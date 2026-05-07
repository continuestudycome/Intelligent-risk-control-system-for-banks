package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.credit.CreditLimitRuleHelper;
import com.system.domain.CrdLimit;
import com.system.domain.CrdLimitAdjustRequest;
import com.system.domain.CrdLimitHistory;
import com.system.domain.CrdScore;
import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.exception.ApiException;
import com.system.mapper.CrdLimitAdjustRequestMapper;
import com.system.mapper.CrdLimitHistoryMapper;
import com.system.mapper.CrdLimitMapper;
import com.system.mapper.CrdScoreMapper;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.CreditLimitAiClient;
import com.system.service.CreditLimitManagementService;
import com.system.vo.CustomerLimitSummaryVO;
import com.system.vo.LimitAdjustPendingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditLimitManagementServiceImpl implements CreditLimitManagementService {

    private static final int TXN_DAYS = 90;

    private record ResolvedLimits(BigDecimal total, BigDecimal single, BigDecimal daily) {}

    private final CreditLimitAiClient creditLimitAiClient;
    private final CrdLimitMapper crdLimitMapper;
    private final CrdLimitHistoryMapper crdLimitHistoryMapper;
    private final CrdLimitAdjustRequestMapper crdLimitAdjustRequestMapper;
    private final CrdScoreMapper crdScoreMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final TxnTransactionMapper txnTransactionMapper;
    private final UserMapper userMapper;

    @Value("${credit.limit.min-total:10000}")
    private BigDecimal minTotal;

    @Value("${credit.limit.max-total:2000000}")
    private BigDecimal maxTotal;

    /** 建议额度高于当前超过该比例才发起上调复核 */
    @Value("${credit.limit.up-ratio:0.02}")
    private double upRatio;

    /** 建议额度低于当前超过该比例才自动下调 */
    @Value("${credit.limit.down-ratio:0.02}")
    private double downRatio;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureLimitAccountAfterScoreEvaluation(Long customerId) {
        CrdLimit exists = crdLimitMapper.selectOne(
                new LambdaQueryWrapper<CrdLimit>()
                        .eq(CrdLimit::getCustomerId, customerId)
                        .eq(CrdLimit::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (exists != null) {
            return;
        }
        CrdScore latest = latestScore(customerId);
        if (latest == null) {
            return;
        }
        CustCustomer c = custCustomerMapper.selectById(customerId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            return;
        }
        insertInitialLimit(customerId, latest.getRiskLevel(), c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void runPeriodicAdjustment() {
        List<CustCustomer> customers = custCustomerMapper.selectList(
                new LambdaQueryWrapper<CustCustomer>()
                        .eq(CustCustomer::getIsDeleted, 0));
        int ok = 0;
        for (CustCustomer c : customers) {
            try {
                adjustSingleCustomer(c.getId());
                ok++;
            } catch (Exception e) {
                log.warn("授信动态调整跳过 customerId={}: {}", c.getId(), e.getMessage());
            }
        }
        log.info("授信动态调整批跑结束，客户数={}, 处理尝试={}", customers.size(), ok);
    }

    @Override
    public void triggerManualRiskBatch() {
        ensureRiskOfficer();
        runPeriodicAdjustment();
    }

    private void adjustSingleCustomer(Long customerId) {
        CrdScore latest = latestScore(customerId);
        if (latest == null) {
            return;
        }
        CustCustomer c = custCustomerMapper.selectById(customerId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            return;
        }
        CrdLimit lim = crdLimitMapper.selectOne(
                new LambdaQueryWrapper<CrdLimit>()
                        .eq(CrdLimit::getCustomerId, customerId)
                        .eq(CrdLimit::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (lim == null) {
            insertInitialLimit(customerId, latest.getRiskLevel(), c);
            return;
        }

        ResolvedLimits res = resolveLimits(latest.getRiskLevel(), c);
        BigDecimal recommended = res.total();

        BigDecimal currentTotal = lim.getTotalLimit() != null ? lim.getTotalLimit() : BigDecimal.ZERO;
        BigDecimal used = lim.getUsedLimit() != null ? lim.getUsedLimit() : BigDecimal.ZERO;

        BigDecimal downTrigger = currentTotal.multiply(BigDecimal.valueOf(1.0 - downRatio));
        BigDecimal upTrigger = currentTotal.multiply(BigDecimal.valueOf(1.0 + upRatio));

        if (recommended.compareTo(downTrigger) < 0) {
            BigDecimal newTotal = recommended.max(used).setScale(2, RoundingMode.HALF_UP);
            if (newTotal.compareTo(currentTotal) < 0) {
                applyLimitChange(lim, newTotal, res.single(), res.daily(), 2,
                        "系统定期重算：信用分与交易行为建议下调授信（评分 "
                                + latest.getScore() + " / " + latest.getRiskLevel() + "）");
            }
        } else if (recommended.compareTo(upTrigger) > 0) {
            boolean hasPending = crdLimitAdjustRequestMapper.selectCount(
                    new LambdaQueryWrapper<CrdLimitAdjustRequest>()
                            .eq(CrdLimitAdjustRequest::getCustomerId, customerId)
                            .eq(CrdLimitAdjustRequest::getStatus, "PENDING")) > 0;
            if (!hasPending) {
                CrdLimitAdjustRequest req = new CrdLimitAdjustRequest();
                req.setCustomerId(customerId);
                req.setCurrentTotalLimit(currentTotal);
                req.setProposedTotalLimit(recommended.setScale(2, RoundingMode.HALF_UP));
                req.setTriggerScore(latest.getScore());
                req.setTriggerRiskLevel(latest.getRiskLevel());
                req.setReason("系统定期评估建议上调授信：评分 "
                        + latest.getScore() + "，建议总额 "
                        + req.getProposedTotalLimit() + " 元（待人工复核）");
                req.setStatus("PENDING");
                req.setCreateTime(LocalDateTime.now());
                crdLimitAdjustRequestMapper.insert(req);
                log.info("已生成授信上调工单 customerId={} proposed={}", customerId, req.getProposedTotalLimit());
            }
        }
    }

    private void insertInitialLimit(Long customerId, String riskLevel, CustCustomer c) {
        ResolvedLimits res = resolveLimits(riskLevel, c);
        BigDecimal total = res.total();
        BigDecimal used = BigDecimal.ZERO;
        CrdLimit row = new CrdLimit();
        row.setCustomerId(customerId);
        row.setTotalLimit(total);
        row.setUsedLimit(used);
        row.setAvailableLimit(total.subtract(used));
        row.setSingleLimit(res.single());
        row.setDailyLimit(res.daily());
        row.setLastAdjustTime(LocalDateTime.now());
        row.setRemark("首次授信初始化（依据信用评分结果）");
        row.setIsDeleted(0);
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(LocalDateTime.now());
        crdLimitMapper.insert(row);

        CrdLimitHistory h = new CrdLimitHistory();
        h.setCustomerId(customerId);
        h.setAdjustType(1);
        h.setOldLimit(BigDecimal.ZERO);
        h.setNewLimit(total);
        h.setReason("初始化授信账户");
        h.setOperatorName("系统");
        h.setCreateTime(LocalDateTime.now());
        crdLimitHistoryMapper.insert(h);
    }

    private void applyLimitChange(
            CrdLimit lim,
            BigDecimal newTotal,
            BigDecimal singleLimit,
            BigDecimal dailyLimit,
            int adjustType,
            String reason) {
        BigDecimal oldTotal = lim.getTotalLimit();
        BigDecimal used = lim.getUsedLimit() != null ? lim.getUsedLimit() : BigDecimal.ZERO;
        BigDecimal avail = newTotal.subtract(used).max(BigDecimal.ZERO);

        lim.setTotalLimit(newTotal);
        lim.setAvailableLimit(avail);
        lim.setSingleLimit(singleLimit);
        lim.setDailyLimit(dailyLimit);
        lim.setLastAdjustTime(LocalDateTime.now());
        lim.setRemark(reason);
        lim.setUpdateTime(LocalDateTime.now());
        crdLimitMapper.updateById(lim);

        CrdLimitHistory h = new CrdLimitHistory();
        h.setCustomerId(lim.getCustomerId());
        h.setAdjustType(adjustType);
        h.setOldLimit(oldTotal);
        h.setNewLimit(newTotal);
        h.setReason(reason);
        h.setOperatorName("系统批调");
        h.setCreateTime(LocalDateTime.now());
        crdLimitHistoryMapper.insert(h);
    }

    /**
     * 优先调用 ai_services 授信推荐；不可达时使用 {@link CreditLimitRuleHelper} 与旧逻辑一致。
     */
    private ResolvedLimits resolveLimits(String riskLevel, CustCustomer c) {
        Long cnt = txnTransactionMapper.countSuccessfulSinceDays(c.getId(), TXN_DAYS);
        long txnCnt = cnt == null ? 0L : cnt;
        boolean bl = c.getIsBlacklist() != null && c.getIsBlacklist() == 1;
        var r = creditLimitAiClient.recommend(
                riskLevel,
                bl,
                txnCnt,
                c.getAssetAmount(),
                minTotal,
                maxTotal);
        if (r.available()
                && r.recommendedTotal() != null
                && r.singleLimit() != null
                && r.dailyLimit() != null) {
            return new ResolvedLimits(r.recommendedTotal(), r.singleLimit(), r.dailyLimit());
        }
        return new ResolvedLimits(
                CreditLimitRuleHelper.computeRecommendedTotal(
                        riskLevel, bl, txnCnt, c.getAssetAmount(), minTotal, maxTotal),
                CreditLimitRuleHelper.singleLimitByRisk(riskLevel),
                CreditLimitRuleHelper.dailyLimitByRisk(riskLevel));
    }

    private CrdScore latestScore(Long customerId) {
        return crdScoreMapper.selectOne(
                new LambdaQueryWrapper<CrdScore>()
                        .eq(CrdScore::getCustomerId, customerId)
                        .orderByDesc(CrdScore::getCreateTime)
                        .last("LIMIT 1"));
    }

    @Override
    public CustomerLimitSummaryVO getCustomerLimitSummary(Long customerId) {
        CrdLimit lim = crdLimitMapper.selectOne(
                new LambdaQueryWrapper<CrdLimit>()
                        .eq(CrdLimit::getCustomerId, customerId)
                        .eq(CrdLimit::getIsDeleted, 0)
                        .last("LIMIT 1"));
        boolean pending = crdLimitAdjustRequestMapper.selectCount(
                new LambdaQueryWrapper<CrdLimitAdjustRequest>()
                        .eq(CrdLimitAdjustRequest::getCustomerId, customerId)
                        .eq(CrdLimitAdjustRequest::getStatus, "PENDING")) > 0;

        CrdLimitAdjustRequest pReq = crdLimitAdjustRequestMapper.selectOne(
                new LambdaQueryWrapper<CrdLimitAdjustRequest>()
                        .eq(CrdLimitAdjustRequest::getCustomerId, customerId)
                        .eq(CrdLimitAdjustRequest::getStatus, "PENDING")
                        .orderByDesc(CrdLimitAdjustRequest::getCreateTime)
                        .last("LIMIT 1"));

        if (lim == null) {
            return CustomerLimitSummaryVO.builder()
                    .hasLimitAccount(false)
                    .pendingIncreaseReview(pending)
                    .pendingProposedTotal(pReq != null ? pReq.getProposedTotalLimit() : null)
                    .lastAdjustHint("完成信用评分后将自动初始化授信额度")
                    .build();
        }

        return CustomerLimitSummaryVO.builder()
                .hasLimitAccount(true)
                .totalLimit(lim.getTotalLimit())
                .usedLimit(lim.getUsedLimit())
                .availableLimit(lim.getAvailableLimit())
                .singleLimit(lim.getSingleLimit())
                .dailyLimit(lim.getDailyLimit())
                .pendingIncreaseReview(pending)
                .pendingProposedTotal(pReq != null ? pReq.getProposedTotalLimit() : null)
                .lastAdjustHint(lim.getLastAdjustTime() != null
                        ? "上次额度相关更新时间：" + lim.getLastAdjustTime()
                        : null)
                .build();
    }

    @Override
    public List<LimitAdjustPendingVO> listPendingIncreaseRequests() {
        ensureRiskOfficer();
        List<CrdLimitAdjustRequest> list = crdLimitAdjustRequestMapper.selectList(
                new LambdaQueryWrapper<CrdLimitAdjustRequest>()
                        .eq(CrdLimitAdjustRequest::getStatus, "PENDING")
                        .orderByDesc(CrdLimitAdjustRequest::getCreateTime));
        return list.stream().map(r -> {
            CustCustomer c = custCustomerMapper.selectById(r.getCustomerId());
            String name = c != null ? c.getRealName() : "";
            return LimitAdjustPendingVO.builder()
                    .id(r.getId())
                    .customerId(r.getCustomerId())
                    .customerName(name)
                    .currentTotalLimit(r.getCurrentTotalLimit())
                    .proposedTotalLimit(r.getProposedTotalLimit())
                    .triggerScore(r.getTriggerScore())
                    .triggerRiskLevel(r.getTriggerRiskLevel())
                    .reason(r.getReason())
                    .status(r.getStatus())
                    .createTime(r.getCreateTime())
                    .build();
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveIncrease(Long requestId, String comment) {
        SysUser reviewer = ensureRiskOfficer();
        CrdLimitAdjustRequest req = crdLimitAdjustRequestMapper.selectById(requestId);
        if (req == null || !"PENDING".equals(req.getStatus())) {
            throw new ApiException(400, "工单不存在或已处理");
        }
        CrdLimit lim = crdLimitMapper.selectOne(
                new LambdaQueryWrapper<CrdLimit>()
                        .eq(CrdLimit::getCustomerId, req.getCustomerId())
                        .eq(CrdLimit::getIsDeleted, 0)
                        .last("LIMIT 1"));
        if (lim == null) {
            throw new ApiException(400, "客户授信账户不存在");
        }
        CustCustomer cust = custCustomerMapper.selectById(req.getCustomerId());
        if (cust == null || (cust.getIsDeleted() != null && cust.getIsDeleted() == 1)) {
            throw new ApiException(400, "客户不存在");
        }
        BigDecimal newTotal = req.getProposedTotalLimit();
        BigDecimal used = lim.getUsedLimit() != null ? lim.getUsedLimit() : BigDecimal.ZERO;
        if (newTotal.compareTo(used) < 0) {
            throw new ApiException(400, "建议额度不能低于已用额度");
        }
        BigDecimal oldTotal = lim.getTotalLimit();
        lim.setTotalLimit(newTotal);
        lim.setAvailableLimit(newTotal.subtract(used).max(BigDecimal.ZERO));
        String rk = req.getTriggerRiskLevel() != null ? req.getTriggerRiskLevel() : "C";
        ResolvedLimits res = resolveLimits(rk, cust);
        lim.setSingleLimit(res.single());
        lim.setDailyLimit(res.daily());
        lim.setLastAdjustTime(LocalDateTime.now());
        lim.setRemark("人工复核通过授信上调");
        lim.setUpdateTime(LocalDateTime.now());
        crdLimitMapper.updateById(lim);

        CrdLimitHistory h = new CrdLimitHistory();
        h.setCustomerId(req.getCustomerId());
        h.setAdjustType(1);
        h.setOldLimit(oldTotal);
        h.setNewLimit(newTotal);
        h.setReason("授信上调（人工通过）：" + Objects.toString(comment, ""));
        h.setOperatorId(reviewer.getId());
        h.setOperatorName(reviewer.getRealName() != null ? reviewer.getRealName() : reviewer.getUsername());
        h.setCreateTime(LocalDateTime.now());
        crdLimitHistoryMapper.insert(h);

        req.setStatus("APPROVED");
        req.setReviewerId(reviewer.getId());
        req.setReviewComment(comment);
        req.setReviewTime(LocalDateTime.now());
        crdLimitAdjustRequestMapper.updateById(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectIncrease(Long requestId, String comment) {
        SysUser reviewer = ensureRiskOfficer();
        CrdLimitAdjustRequest req = crdLimitAdjustRequestMapper.selectById(requestId);
        if (req == null || !"PENDING".equals(req.getStatus())) {
            throw new ApiException(400, "工单不存在或已处理");
        }
        req.setStatus("REJECTED");
        req.setReviewerId(reviewer.getId());
        req.setReviewComment(comment);
        req.setReviewTime(LocalDateTime.now());
        crdLimitAdjustRequestMapper.updateById(req);
    }

    private SysUser ensureRiskOfficer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(principal.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        String code = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可操作");
        }
        return user;
    }
}
