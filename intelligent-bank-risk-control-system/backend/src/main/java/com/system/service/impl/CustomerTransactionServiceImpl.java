package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.domain.TxnTransaction;
import com.system.dto.TransactionCreateDTO;
import com.system.exception.ApiException;
import com.system.domain.AcctAccount;
import com.system.domain.FrdAlert;
import com.system.fraud.FraudAssessmentResult;
import com.system.fraud.FraudRiskLevel;
import com.system.mapper.AcctAccountMapper;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.FrdAlertMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.CustomerTransactionService;
import com.system.service.FraudRiskAssessmentService;
import com.system.vo.TransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerTransactionServiceImpl implements CustomerTransactionService {

    private final UserMapper userMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final TxnTransactionMapper txnTransactionMapper;
    private final AcctAccountMapper acctAccountMapper;
    private final FraudRiskAssessmentService fraudRiskAssessmentService;
    private final FrdAlertMapper frdAlertMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO createTransaction(TransactionCreateDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());
        ensureCustomerCanTrade(customer);
        AcctAccount fromAccount = ensureFromAccountValid(customer, dto.getFromAccount());
        AcctAccount toAccount = ensureToAccountValid(dto.getToAccount());
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new ApiException(400, "付款账户与收款账户不能相同");
        }
        validateLimit(customer, dto.getAmount());

        FraudAssessmentResult fraud = fraudRiskAssessmentService.assess(customer, dto);
        FraudRiskLevel level = fraud.level();

        TxnTransaction transaction = new TxnTransaction();
        transaction.setTransactionNo(generateTxnNo());
        transaction.setCustomerId(customer.getId());
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setFromAccount(dto.getFromAccount());
        transaction.setToAccount(dto.getToAccount());
        transaction.setAmount(dto.getAmount());
        transaction.setCurrency("CNY");
        transaction.setTransactionProvince(dto.getTransactionProvince());
        transaction.setTransactionCity(dto.getTransactionCity());
        transaction.setRemark(dto.getPurpose());
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setCreateTime(LocalDateTime.now());

        switch (level) {
            case LOW -> {
                transferWithBalanceCheck(fromAccount, toAccount, dto.getAmount());
                transaction.setRiskStatus("LOW");
                transaction.setHandleResult(1);
            }
            case MEDIUM -> {
                transaction.setRiskStatus("MEDIUM");
                transaction.setHandleResult(2);
            }
            case HIGH -> {
                transaction.setRiskStatus("HIGH");
                transaction.setHandleResult(0);
            }
        }

        txnTransactionMapper.insert(transaction);

        if (level != FraudRiskLevel.LOW) {
            insertFraudAlert(transaction, fraud, level);
        }

        String riskHint = buildRiskHint(level, fraud);
        return toVO(transaction, riskHint);
    }

    private void insertFraudAlert(TxnTransaction tx, FraudAssessmentResult fraud, FraudRiskLevel level) {
        FrdAlert alert = new FrdAlert();
        alert.setTransactionId(tx.getId());
        alert.setTransactionNo(tx.getTransactionNo());
        alert.setCustomerId(tx.getCustomerId());
        alert.setAlertLevel(level == FraudRiskLevel.HIGH ? "HIGH" : "MEDIUM");
        alert.setHitRules(String.join(",", fraud.hitRuleCodes()));
        alert.setMlScore(fraud.mlScore());
        alert.setMlModelVersion(fraud.mlModelVersion());
        alert.setFeatureSnapshot(fraud.featureSnapshotJson());
        alert.setStatus("PENDING");
        alert.setCreateTime(LocalDateTime.now());
        frdAlertMapper.insert(alert);
    }

    private String buildRiskHint(FraudRiskLevel level, FraudAssessmentResult fraud) {
        return switch (level) {
            case LOW -> null;
            case MEDIUM -> {
                StringJoiner j = new StringJoiner("；");
                j.add("该笔交易触发中风险策略，未划转资金");
                j.add("请完成二次验证（短信/人脸识别）或联系客服");
                if (!fraud.hitRuleCodes().isEmpty()) {
                    j.add("命中规则：" + String.join(",", fraud.hitRuleCodes()));
                }
                if (fraud.mlScore() != null) {
                    j.add("孤立森林异常度：" + fraud.mlScore());
                }
                yield j.toString();
            }
            case HIGH -> {
                StringJoiner j = new StringJoiner("；");
                j.add("该笔交易已被实时风控拦截，资金未划出");
                j.add("已生成人工复核工单");
                if (!fraud.hitRuleCodes().isEmpty()) {
                    j.add("命中规则：" + String.join(",", fraud.hitRuleCodes()));
                }
                if (fraud.mlScore() != null) {
                    j.add("孤立森林异常度：" + fraud.mlScore());
                }
                yield j.toString();
            }
        };
    }

    private void transferWithBalanceCheck(AcctAccount fromAccount, AcctAccount toAccount, BigDecimal amount) {
        int debitRows = acctAccountMapper.debitWithCheck(fromAccount.getId(), amount);
        if (debitRows <= 0) {
            throw new ApiException(400, "付款账户余额不足或账户不可用");
        }
        int creditRows = acctAccountMapper.credit(toAccount.getId(), amount);
        if (creditRows <= 0) {
            throw new ApiException(400, "收款账户不可用，交易已回滚");
        }
    }

    private AcctAccount ensureFromAccountValid(CustCustomer customer, String accountNo) {
        AcctAccount account = acctAccountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new ApiException(400, "付款账户不存在");
        }
        if (!customer.getId().equals(account.getCustomerId())) {
            throw new ApiException(400, "付款账户不属于当前客户");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new ApiException(400, "付款账户不可用");
        }
        return account;
    }

    private AcctAccount ensureToAccountValid(String accountNo) {
        AcctAccount account = acctAccountMapper.selectByAccountNo(accountNo);
        if (account == null) {
            throw new ApiException(400, "收款账户不存在");
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            throw new ApiException(400, "收款账户不可用");
        }
        return account;
    }

    @Override
    public List<TransactionVO> queryMyTransactions(
            Integer transactionType,
            String riskStatus,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());

        LambdaQueryWrapper<TxnTransaction> wrapper = new LambdaQueryWrapper<TxnTransaction>()
                .eq(TxnTransaction::getCustomerId, customer.getId())
                .orderByDesc(TxnTransaction::getCreateTime);

        if (transactionType != null) {
            wrapper.eq(TxnTransaction::getTransactionType, transactionType);
        }
        if (riskStatus != null && !riskStatus.isBlank()) {
            wrapper.eq(TxnTransaction::getRiskStatus, riskStatus.trim().toUpperCase());
        }
        if (minAmount != null) {
            wrapper.ge(TxnTransaction::getAmount, minAmount);
        }
        if (maxAmount != null) {
            wrapper.le(TxnTransaction::getAmount, maxAmount);
        }
        if (startTime != null) {
            wrapper.ge(TxnTransaction::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(TxnTransaction::getCreateTime, endTime);
        }

        return txnTransactionMapper.selectList(wrapper).stream().map(t -> toVO(t, null)).toList();
    }

    @Override
    public TransactionVO getMyTransactionDetail(Long id) {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());

        TxnTransaction transaction = txnTransactionMapper.selectById(id);
        if (transaction == null || !customer.getId().equals(transaction.getCustomerId())) {
            throw new ApiException(404, "交易记录不存在");
        }
        return toVO(transaction, null);
    }

    private void validateLimit(CustCustomer customer, BigDecimal amount) {
        String level = customer.getCreditLevel();
        if (level == null || level.isBlank()) {
            level = "C";
        }
        LimitPair limit = limitByCredit(level.trim().toUpperCase());
        if (amount.compareTo(limit.singleLimit()) > 0) {
            throw new ApiException(400, "超出单笔交易限额，当前等级单笔限额为 " + limit.singleLimit());
        }

        BigDecimal todayTotal = txnTransactionMapper.selectTodayTotalAmount(customer.getId());
        if (todayTotal == null) {
            todayTotal = BigDecimal.ZERO;
        }
        BigDecimal afterTotal = todayTotal.add(amount);
        if (afterTotal.compareTo(limit.dailyLimit()) > 0) {
            throw new ApiException(400, "超出日累计限额，当前等级日累计限额为 " + limit.dailyLimit());
        }
    }

    private LimitPair limitByCredit(String level) {
        return switch (level) {
            case "A" -> new LimitPair(new BigDecimal("50000"), new BigDecimal("200000"));
            case "B" -> new LimitPair(new BigDecimal("30000"), new BigDecimal("100000"));
            case "C" -> new LimitPair(new BigDecimal("10000"), new BigDecimal("50000"));
            default -> new LimitPair(new BigDecimal("3000"), new BigDecimal("10000"));
        };
    }

    private void ensureCustomerCanTrade(CustCustomer customer) {
        if (customer.getProfileCompleted() == null || customer.getProfileCompleted() != 1) {
            throw new ApiException(400, "客户资料未完善，暂不允许发起交易");
        }
        if (customer.getIsBlacklist() != null && customer.getIsBlacklist() == 1) {
            throw new ApiException(400, "客户已在黑名单中，交易已拦截");
        }
    }

    private String generateTxnNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TXN" + timePart + rand;
    }

    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(principal.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        return user;
    }

    private void ensureCustomer(Long userId) {
        String roleCode = userMapper.selectPrimaryRoleCode(userId);
        if (!"CUSTOMER".equalsIgnoreCase(roleCode)) {
            throw new ApiException(403, "仅客户可发起交易");
        }
    }

    private CustCustomer getCustomerOrThrow(Long userId) {
        CustCustomer customer = custCustomerMapper.selectByUserId(userId);
        if (customer == null || customer.getIsDeleted() != null && customer.getIsDeleted() == 1) {
            throw new ApiException(400, "客户档案不存在，请先完善个人中心信息");
        }
        return customer;
    }

    private TransactionVO toVO(TxnTransaction t, String riskMessage) {
        TransactionVO.TransactionVOBuilder b = TransactionVO.builder()
                .id(t.getId())
                .transactionNo(t.getTransactionNo())
                .transactionType(t.getTransactionType())
                .transactionTypeName(typeName(t.getTransactionType()))
                .fromAccount(t.getFromAccount())
                .toAccount(t.getToAccount())
                .amount(t.getAmount())
                .transactionProvince(t.getTransactionProvince())
                .transactionCity(t.getTransactionCity())
                .purpose(t.getRemark())
                .riskStatus(t.getRiskStatus())
                .riskStatusName(riskStatusName(t.getRiskStatus()))
                .handleResult(t.getHandleResult())
                .handleResultName(handleResultName(t.getHandleResult()))
                .transactionTime(t.getTransactionTime() != null ? t.getTransactionTime() : t.getCreateTime())
                .riskMessage(riskMessage);
        return b.build();
    }

    private String typeName(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "转账";
            case 2 -> "消费";
            case 3 -> "取现";
            case 4 -> "还款";
            default -> "未知";
        };
    }

    private String riskStatusName(String status) {
        if (status == null) return "未评估";
        return switch (status) {
            case "LOW" -> "低风险";
            case "MEDIUM" -> "中风险";
            case "HIGH" -> "高风险";
            case "INTERCEPTED" -> "已拦截";
            case "CONFIRMED_FRAUD" -> "已确认欺诈";
            default -> status;
        };
    }

    private String handleResultName(Integer result) {
        if (result == null) return "未处理";
        return switch (result) {
            case 0 -> "已拦截";
            case 1 -> "正常放行";
            case 2 -> "二次验证";
            default -> "未知";
        };
    }

    private record LimitPair(BigDecimal singleLimit, BigDecimal dailyLimit) {
    }
}
