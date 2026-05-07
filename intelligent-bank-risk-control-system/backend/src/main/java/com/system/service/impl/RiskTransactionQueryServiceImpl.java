package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.domain.TxnTransaction;
import com.system.exception.ApiException;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.RiskTransactionQueryService;
import com.system.vo.TransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskTransactionQueryServiceImpl implements RiskTransactionQueryService {

    private static final int MAX_ROWS = 500;

    private final UserMapper userMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final TxnTransactionMapper txnTransactionMapper;

    @Override
    public List<TransactionVO> listTransactions(
            Long customerId,
            String customerKeyword,
            Integer transactionType,
            String riskStatus,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        ensureRiskStaff();
        LambdaQueryWrapper<TxnTransaction> w = new LambdaQueryWrapper<TxnTransaction>()
                .orderByDesc(TxnTransaction::getCreateTime)
                .last("LIMIT " + MAX_ROWS);
        if (customerId != null) {
            w.eq(TxnTransaction::getCustomerId, customerId);
        } else if (customerKeyword != null && !customerKeyword.isBlank()) {
            List<Long> ids = resolveCustomerIds(customerKeyword.trim());
            if (ids.isEmpty()) {
                return List.of();
            }
            w.in(TxnTransaction::getCustomerId, ids);
        }
        if (transactionType != null) {
            w.eq(TxnTransaction::getTransactionType, transactionType);
        }
        if (riskStatus != null && !riskStatus.isBlank()) {
            w.eq(TxnTransaction::getRiskStatus, riskStatus.trim().toUpperCase());
        }
        if (minAmount != null) {
            w.ge(TxnTransaction::getAmount, minAmount);
        }
        if (maxAmount != null) {
            w.le(TxnTransaction::getAmount, maxAmount);
        }
        if (startTime != null) {
            w.ge(TxnTransaction::getCreateTime, startTime);
        }
        if (endTime != null) {
            w.le(TxnTransaction::getCreateTime, endTime);
        }
        List<TxnTransaction> rows = txnTransactionMapper.selectList(w);
        Map<Long, CustCustomer> custMap = loadCustomerMap(rows);
        return rows.stream()
                .map(t -> enrichWithCustomer(toVO(t, null), t.getCustomerId(), custMap))
                .toList();
    }

    @Override
    public List<TransactionVO> listRecentRiskEvents(LocalDateTime since, int limit) {
        ensureRiskStaff();
        int lim = Math.min(Math.max(limit, 1), 200);
        LambdaQueryWrapper<TxnTransaction> w = new LambdaQueryWrapper<TxnTransaction>()
                .in(TxnTransaction::getRiskStatus, "MEDIUM", "HIGH", "INTERCEPTED", "CONFIRMED_FRAUD")
                .apply("(COALESCE(transaction_time, create_time) >= {0})", since)
                .orderByDesc(TxnTransaction::getCreateTime)
                .last("LIMIT " + lim);
        List<TxnTransaction> rows = txnTransactionMapper.selectList(w);
        Map<Long, CustCustomer> custMap = loadCustomerMap(rows);
        return rows.stream()
                .map(t -> enrichWithCustomer(toVO(t, null), t.getCustomerId(), custMap))
                .toList();
    }

    @Override
    public TransactionVO getTransactionDetail(Long id) {
        ensureRiskStaff();
        TxnTransaction t = txnTransactionMapper.selectById(id);
        if (t == null) {
            throw new ApiException(404, "交易记录不存在");
        }
        Map<Long, CustCustomer> map = loadCustomerMap(List.of(t));
        return enrichWithCustomer(toVO(t, null), t.getCustomerId(), map);
    }

    private List<Long> resolveCustomerIds(String keyword) {
        Long idExact;
        try {
            idExact = Long.parseLong(keyword);
        } catch (NumberFormatException e) {
            idExact = null;
        }
        List<CustCustomer> list = idExact != null
                ? custCustomerMapper.listCreditBriefByKeywordWithId(keyword, idExact, 200)
                : custCustomerMapper.listCreditBriefByKeywordNoId(keyword, 200);
        return list.stream().map(CustCustomer::getId).toList();
    }

    private Map<Long, CustCustomer> loadCustomerMap(List<TxnTransaction> rows) {
        Set<Long> ids = rows.stream()
                .map(TxnTransaction::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<CustCustomer> list = custCustomerMapper.selectBatchIds(ids);
        return list.stream().collect(Collectors.toMap(CustCustomer::getId, c -> c, (a, b) -> a));
    }

    private TransactionVO enrichWithCustomer(
            TransactionVO vo, Long customerId, Map<Long, CustCustomer> custMap) {
        if (customerId == null) {
            return vo;
        }
        vo.setCustomerId(customerId);
        CustCustomer c = custMap.get(customerId);
        if (c != null) {
            vo.setCustomerName(c.getRealName());
            vo.setCustomerPhone(c.getPhone());
        }
        return vo;
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

    private TransactionVO toVO(TxnTransaction t, String riskMessage) {
        return TransactionVO.builder()
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
                .riskMessage(riskMessage)
                .build();
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

    private static String handleResultName(Integer result) {
        if (result == null) {
            return "未处理";
        }
        return switch (result) {
            case 0 -> "已拦截";
            case 1 -> "正常放行";
            case 2 -> "二次验证";
            default -> "未知";
        };
    }
}
