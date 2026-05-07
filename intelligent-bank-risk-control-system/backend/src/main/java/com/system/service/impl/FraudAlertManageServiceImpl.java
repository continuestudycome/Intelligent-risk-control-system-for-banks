package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.FrdAlert;
import com.system.domain.FrdCase;
import com.system.domain.SysUser;
import com.system.domain.TxnTransaction;
import com.system.dto.FraudAlertReviewDTO;
import com.system.exception.ApiException;
import com.system.mapper.FrdAlertMapper;
import com.system.mapper.FrdCaseMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.FraudAlertManageService;
import com.system.vo.FraudAlertVO;
import com.system.vo.FraudCaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FraudAlertManageServiceImpl implements FraudAlertManageService {

    private final FrdAlertMapper frdAlertMapper;
    private final FrdCaseMapper frdCaseMapper;
    private final TxnTransactionMapper txnTransactionMapper;
    private final UserMapper userMapper;

    @Override
    public List<FraudAlertVO> listAlerts(String status) {
        ensureRisk();
        LambdaQueryWrapper<FrdAlert> w = new LambdaQueryWrapper<FrdAlert>().orderByDesc(FrdAlert::getCreateTime);
        if (status != null && !status.isBlank()) {
            w.eq(FrdAlert::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        return frdAlertMapper.selectList(w).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FraudAlertVO review(Long alertId, FraudAlertReviewDTO dto) {
        SysUser reviewer = getCurrentUser();
        ensureRisk();

        FrdAlert alert = frdAlertMapper.selectById(alertId);
        if (alert == null) {
            throw new ApiException(404, "预警不存在");
        }
        if (!"PENDING".equals(alert.getStatus())) {
            throw new ApiException(400, "该预警已处理，无法重复复核");
        }

        String dec = dto.getDecision().trim().toUpperCase(Locale.ROOT);
        if (!"CONFIRM_FRAUD".equals(dec) && !"FALSE_POSITIVE".equals(dec)) {
            throw new ApiException(400, "decision 仅支持 CONFIRM_FRAUD 或 FALSE_POSITIVE");
        }

        alert.setReviewerId(reviewer.getId());
        alert.setReviewComment(dto.getComment());
        alert.setReviewTime(LocalDateTime.now());

        if ("FALSE_POSITIVE".equals(dec)) {
            alert.setStatus("IGNORED");
            frdAlertMapper.updateById(alert);
            return toVO(alert);
        }

        alert.setStatus("CONFIRMED");
        frdAlertMapper.updateById(alert);

        TxnTransaction txn = txnTransactionMapper.selectById(alert.getTransactionId());
        if (txn != null) {
            txn.setRiskStatus("CONFIRMED_FRAUD");
            txn.setHandleResult(0);
            txnTransactionMapper.updateById(txn);
        }

        String fraudType = dto.getFraudType() != null && !dto.getFraudType().isBlank()
                ? dto.getFraudType().trim() : "THEFT";

        FrdCase existing = frdCaseMapper.selectOne(
                new LambdaQueryWrapper<FrdCase>().eq(FrdCase::getTransactionId, alert.getTransactionId()).last("LIMIT 1"));
        if (existing == null) {
            FrdCase c = new FrdCase();
            c.setTransactionId(alert.getTransactionId());
            c.setAlertId(alert.getId());
            c.setCustomerId(alert.getCustomerId());
            c.setFraudType(fraudType);
            c.setConfirmedResult(1);
            c.setFeatureSnapshot(alert.getFeatureSnapshot());
            c.setLabelSource(1);
            c.setCreateTime(LocalDateTime.now());
            frdCaseMapper.insert(c);
        }

        return toVO(frdAlertMapper.selectById(alertId));
    }

    @Override
    public List<FraudCaseVO> listConfirmedCases() {
        ensureRisk();
        return frdCaseMapper.selectList(
                new LambdaQueryWrapper<FrdCase>().orderByDesc(FrdCase::getCreateTime)
        ).stream().map(c -> FraudCaseVO.builder()
                .id(c.getId())
                .transactionId(c.getTransactionId())
                .alertId(c.getAlertId())
                .customerId(c.getCustomerId())
                .fraudType(c.getFraudType())
                .confirmedResult(c.getConfirmedResult())
                .labelSource(c.getLabelSource())
                .createTime(c.getCreateTime())
                .build()).toList();
    }

    private FraudAlertVO toVO(FrdAlert a) {
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

    private void ensureRisk() {
        SysUser u = getCurrentUser();
        String code = userMapper.selectPrimaryRoleCode(u.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可访问");
        }
    }

    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(principal.getUsername());
        if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
            throw new ApiException(401, "用户不存在");
        }
        return user;
    }
}
