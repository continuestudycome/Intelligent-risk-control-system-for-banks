package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CustCustomer;
import com.system.domain.FrdAlert;
import com.system.domain.SysUser;
import com.system.domain.TxnTransaction;
import com.system.exception.ApiException;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.FrdAlertMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.RiskTransactionQueryService;
import com.system.service.RiskWarningService;
import com.system.vo.FraudAlertVO;
import com.system.vo.RiskWarningOverviewVO;
import com.system.vo.RiskWarningStatsVO;
import com.system.vo.TransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskWarningServiceImpl implements RiskWarningService {

    private static final int FRAUD_ALERT_LIMIT = 14;
    private static final int TXN_LIMIT = 22;
    private static final int TXN_LOOKBACK_DAYS = 7;

    private final UserMapper userMapper;
    private final FrdAlertMapper frdAlertMapper;
    private final TxnTransactionMapper txnTransactionMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final RiskTransactionQueryService riskTransactionQueryService;

    @Override
    public RiskWarningOverviewVO getOverview() {
        ensureRiskStaff();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since24h = now.minusHours(24);
        LocalDateTime since7d = now.minusDays(7);
        LocalDateTime txnSince = now.minusDays(TXN_LOOKBACK_DAYS);

        long pending = frdAlertMapper.selectCount(
                new LambdaQueryWrapper<FrdAlert>().eq(FrdAlert::getStatus, "PENDING"));

        long medium24 = txnTransactionMapper.selectCount(buildTxnSince(since24h)
                .eq(TxnTransaction::getRiskStatus, "MEDIUM"));

        long critical24 = txnTransactionMapper.selectCount(buildTxnSince(since24h)
                .in(TxnTransaction::getRiskStatus, "HIGH", "INTERCEPTED", "CONFIRMED_FRAUD"));

        long intercepted7d = txnTransactionMapper.selectCount(buildTxnSince(since7d)
                .eq(TxnTransaction::getRiskStatus, "INTERCEPTED"));

        long blacklist = custCustomerMapper.selectCount(
                new LambdaQueryWrapper<CustCustomer>()
                        .eq(CustCustomer::getIsBlacklist, 1)
                        .eq(CustCustomer::getIsDeleted, 0));

        RiskWarningStatsVO stats = RiskWarningStatsVO.builder()
                .pendingFraudAlerts(pending)
                .mediumRiskTransactions24h(medium24)
                .criticalRiskTransactions24h(critical24)
                .interceptedTransactions7d(intercepted7d)
                .blacklistCustomers(blacklist)
                .build();

        List<FrdAlert> alerts = frdAlertMapper.selectList(
                new LambdaQueryWrapper<FrdAlert>()
                        .orderByDesc(FrdAlert::getCreateTime)
                        .last("LIMIT " + FRAUD_ALERT_LIMIT));
        List<FraudAlertVO> alertVos = alerts.stream().map(this::toAlertVo).toList();

        List<TransactionVO> txns = riskTransactionQueryService.listRecentRiskEvents(txnSince, TXN_LIMIT);

        return RiskWarningOverviewVO.builder()
                .stats(stats)
                .fraudAlerts(alertVos)
                .riskTransactions(txns)
                .build();
    }

    private LambdaQueryWrapper<TxnTransaction> buildTxnSince(LocalDateTime since) {
        return new LambdaQueryWrapper<TxnTransaction>()
                .apply("(COALESCE(transaction_time, create_time) >= {0})", since);
    }

    private FraudAlertVO toAlertVo(FrdAlert a) {
        return FraudAlertVO.builder()
                .id(a.getId())
                .transactionId(a.getTransactionId())
                .transactionNo(a.getTransactionNo())
                .customerId(a.getCustomerId())
                .alertLevel(a.getAlertLevel())
                .hitRules(a.getHitRules())
                .mlScore(a.getMlScore())
                .mlModelVersion(a.getMlModelVersion())
                .featureSnapshot(a.getFeatureSnapshot())
                .status(a.getStatus())
                .reviewerId(a.getReviewerId())
                .reviewComment(a.getReviewComment())
                .reviewTime(a.getReviewTime())
                .createTime(a.getCreateTime())
                .build();
    }

    private void ensureRiskStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails ud)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(ud.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        String code = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可操作");
        }
    }
}
