package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CustCustomer;
import com.system.domain.FrdAlert;
import com.system.domain.LoanApplication;
import com.system.domain.SysUser;
import com.system.domain.TxnTransaction;
import com.system.exception.ApiException;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.FrdAlertMapper;
import com.system.mapper.LoanApplicationMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.CustomerCreditService;
import com.system.service.CustomerRiskProfileService;
import com.system.vo.CreditScoreOverviewVO;
import com.system.vo.CustomerRiskBasicVO;
import com.system.vo.CustomerRiskProfileVO;
import com.system.vo.FraudAlertBriefVO;
import com.system.vo.LoanAppProfileRowVO;
import com.system.vo.LoanApplicationProfileSummaryVO;
import com.system.vo.ProfileTxnBriefVO;
import com.system.vo.TransactionRiskSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerRiskProfileServiceImpl implements CustomerRiskProfileService {

    private static final int LOOKBACK_DAYS = 90;
    private static final int RECENT_RISKY_TXN = 8;
    private static final int FRAUD_ALERT_LIMIT = 15;
    private static final int RECENT_LOAN = 8;
    private static final int HIT_RULES_MAX = 240;

    private static final Set<String> RISKY_TXN = Set.of("HIGH", "MEDIUM", "INTERCEPTED", "CONFIRMED_FRAUD");

    private final CustCustomerMapper custCustomerMapper;
    private final CustomerCreditService customerCreditService;
    private final TxnTransactionMapper txnTransactionMapper;
    private final FrdAlertMapper frdAlertMapper;
    private final LoanApplicationMapper loanApplicationMapper;
    private final UserMapper userMapper;

    @Override
    public CustomerRiskProfileVO getProfile(Long customerId) {
        ensureRiskStaff();
        CustCustomer c = custCustomerMapper.selectById(customerId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            throw new ApiException(404, "客户不存在");
        }

        CreditScoreOverviewVO credit = customerCreditService.getCustomerOverviewForRisk(customerId);
        LocalDateTime since = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<TxnTransaction> txns = txnTransactionMapper.selectList(
                new LambdaQueryWrapper<TxnTransaction>()
                        .eq(TxnTransaction::getCustomerId, customerId)
                        .apply("(COALESCE(transaction_time, create_time) >= {0})", since)
        );

        TransactionRiskSummaryVO txnSummary = buildTxnSummary(txns);
        List<FrdAlert> alerts = frdAlertMapper.selectList(
                new LambdaQueryWrapper<FrdAlert>()
                        .eq(FrdAlert::getCustomerId, customerId)
                        .orderByDesc(FrdAlert::getCreateTime)
                        .last("LIMIT " + FRAUD_ALERT_LIMIT)
        );
        List<FraudAlertBriefVO> fraudVos = alerts.stream().map(this::toFraudBrief).toList();

        List<LoanApplication> apps = loanApplicationMapper.selectList(
                new LambdaQueryWrapper<LoanApplication>()
                        .eq(LoanApplication::getCustomerId, customerId)
                        .eq(LoanApplication::getIsDeleted, 0)
        );
        LoanApplicationProfileSummaryVO loanSummary = buildLoanSummary(apps);

        CustomerRiskBasicVO basic = CustomerRiskBasicVO.builder()
                .customerId(c.getId())
                .customerNo(c.getCustomerNo())
                .realName(c.getRealName())
                .phoneMasked(maskPhone(c.getPhone()))
                .idCardMasked(maskIdCard(c.getIdCardNo()))
                .province(c.getProvince())
                .city(c.getCity())
                .creditLevel(c.getCreditLevel())
                .isBlacklist(c.getIsBlacklist())
                .blacklistReason(c.getBlacklistReason())
                .annualIncome(c.getAnnualIncome())
                .assetAmount(c.getAssetAmount())
                .profileCompleted(c.getProfileCompleted())
                .registerTime(c.getCreateTime())
                .build();

        List<String> tags = buildPortraitTags(c, credit, txnSummary, fraudVos, loanSummary);

        return CustomerRiskProfileVO.builder()
                .basic(basic)
                .portraitTags(tags)
                .credit(credit)
                .transactions(txnSummary)
                .fraudAlerts(fraudVos)
                .loans(loanSummary)
                .build();
    }

    private TransactionRiskSummaryVO buildTxnSummary(List<TxnTransaction> txns) {
        int low = 0, med = 0, high = 0, inter = 0, fraud = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (TxnTransaction t : txns) {
            BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            sum = sum.add(amt);
            String rs = t.getRiskStatus();
            if (rs == null) {
                continue;
            }
            switch (rs) {
                case "LOW" -> low++;
                case "MEDIUM" -> med++;
                case "HIGH" -> high++;
                case "INTERCEPTED" -> inter++;
                case "CONFIRMED_FRAUD" -> fraud++;
                default -> {
                    /* ignore */
                }
            }
        }

        List<ProfileTxnBriefVO> risky = txns.stream()
                .filter(t -> t.getRiskStatus() != null && RISKY_TXN.contains(t.getRiskStatus()))
                .sorted(Comparator.comparing((TxnTransaction t) -> txnTime(t)).reversed())
                .limit(RECENT_RISKY_TXN)
                .map(this::toProfileTxn)
                .toList();

        return TransactionRiskSummaryVO.builder()
                .lookbackDays(LOOKBACK_DAYS)
                .totalCount(txns.size())
                .totalAmount(sum)
                .lowCount(low)
                .mediumCount(med)
                .highCount(high)
                .interceptedCount(inter)
                .confirmedFraudCount(fraud)
                .recentRiskyTransactions(risky)
                .build();
    }

    private LocalDateTime txnTime(TxnTransaction t) {
        return t.getTransactionTime() != null ? t.getTransactionTime() : t.getCreateTime();
    }

    private ProfileTxnBriefVO toProfileTxn(TxnTransaction t) {
        return ProfileTxnBriefVO.builder()
                .id(t.getId())
                .transactionNo(t.getTransactionNo())
                .transactionType(t.getTransactionType())
                .transactionTypeName(typeName(t.getTransactionType()))
                .amount(t.getAmount())
                .riskStatus(t.getRiskStatus())
                .riskStatusName(riskStatusName(t.getRiskStatus()))
                .transactionTime(txnTime(t))
                .build();
    }

    private FraudAlertBriefVO toFraudBrief(FrdAlert a) {
        return FraudAlertBriefVO.builder()
                .id(a.getId())
                .transactionNo(a.getTransactionNo())
                .alertLevel(a.getAlertLevel())
                .mlScore(a.getMlScore())
                .status(a.getStatus())
                .createTime(a.getCreateTime())
                .hitRulesSummary(truncate(a.getHitRules(), HIT_RULES_MAX))
                .build();
    }

    private LoanApplicationProfileSummaryVO buildLoanSummary(List<LoanApplication> apps) {
        int pend = 0, appr = 0, rej = 0, other = 0;
        for (LoanApplication a : apps) {
            String s = a.getCurrentStatus();
            if ("PENDING".equals(s)) {
                pend++;
            } else if ("APPROVED".equals(s)) {
                appr++;
            } else if ("REJECTED".equals(s)) {
                rej++;
            } else {
                other++;
            }
        }
        List<LoanAppProfileRowVO> recent = apps.stream()
                .sorted(Comparator.comparing(LoanApplication::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(RECENT_LOAN)
                .map(a -> LoanAppProfileRowVO.builder()
                        .id(a.getId())
                        .applicationNo(a.getApplicationNo())
                        .applyTypeName(applyTypeName(a.getApplyType()))
                        .currentStatus(a.getCurrentStatus())
                        .currentStatusName(loanStatusName(a.getCurrentStatus()))
                        .applyAmount(a.getApplyAmount())
                        .createTime(a.getCreateTime())
                        .build())
                .toList();

        return LoanApplicationProfileSummaryVO.builder()
                .pendingCount(pend)
                .approvedCount(appr)
                .rejectedCount(rej)
                .otherStatusCount(other)
                .recentApplications(recent)
                .build();
    }

    private List<String> buildPortraitTags(
            CustCustomer c,
            CreditScoreOverviewVO credit,
            TransactionRiskSummaryVO txn,
            List<FraudAlertBriefVO> frauds,
            LoanApplicationProfileSummaryVO loans) {
        List<String> tags = new ArrayList<>();
        if (c.getIsBlacklist() != null && c.getIsBlacklist() == 1) {
            tags.add("黑名单客户");
        }
        if (credit.isEvaluated()) {
            String lv = credit.getRiskLevel();
            Integer sc = credit.getScore();
            if ("D".equalsIgnoreCase(lv) || (sc != null && sc < 520)) {
                tags.add("信用评分偏低");
            }
        } else {
            tags.add("尚未生成信用评分");
        }
        if (txn.getConfirmedFraudCount() > 0) {
            tags.add("近" + LOOKBACK_DAYS + "日存在确认欺诈交易");
        }
        if (txn.getHighCount() >= 5) {
            tags.add("高频高风险交易");
        }
        if (txn.getInterceptedCount() >= 3) {
            tags.add("多次交易拦截");
        }
        long pendingFraud = frauds.stream()
                .filter(f -> "PENDING".equalsIgnoreCase(f.getStatus()))
                .count();
        if (pendingFraud > 0) {
            tags.add("待处理欺诈告警 " + pendingFraud + " 条");
        }
        if (loans.getPendingCount() > 0) {
            tags.add("存在待审批信贷申请");
        }
        return tags;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone != null ? phone : "—";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskIdCard(String id) {
        if (id == null || id.isBlank()) {
            return "—";
        }
        if (id.length() < 10) {
            return "已登记";
        }
        return id.substring(0, 6) + "********" + id.substring(id.length() - 4);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }

    private static String typeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "转账";
            case 2 -> "消费";
            case 3 -> "取现";
            case 4 -> "还款";
            default -> "未知";
        };
    }

    private static String riskStatusName(String status) {
        if (status == null) {
            return "未评估";
        }
        return switch (status) {
            case "LOW" -> "低风险";
            case "MEDIUM" -> "中风险";
            case "HIGH" -> "高风险";
            case "INTERCEPTED" -> "已拦截";
            case "CONFIRMED_FRAUD" -> "已确认欺诈";
            default -> status;
        };
    }

    private static String applyTypeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return switch (type) {
            case 1 -> "信用贷款";
            case 2 -> "抵押贷款";
            case 3 -> "信用卡";
            default -> "未知";
        };
    }

    private static String loanStatusName(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            case "FIRST_REVIEW" -> "初审中";
            case "SECOND_REVIEW" -> "复审中";
            case "FINAL_REVIEW" -> "终审中";
            default -> status;
        };
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
