package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CustCustomer;
import com.system.domain.LoanApplication;
import com.system.domain.LoanApprovalRecord;
import com.system.domain.SysUser;
import com.system.dto.LoanApplicationCreateDTO;
import com.system.dto.LoanApplicationReviewDTO;
import com.system.exception.ApiException;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.LoanApplicationMapper;
import com.system.mapper.LoanApprovalRecordMapper;
import com.system.mapper.UserMapper;
import com.system.service.LoanApplicationService;
import com.system.vo.LoanApplicationVO;
import com.system.vo.LoanApprovalRecordVO;
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
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final UserMapper userMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanApprovalRecordMapper loanApprovalRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanApplicationVO createApplication(LoanApplicationCreateDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());
        ensureCanApply(customer);

        if (dto.getApplyType() != null && dto.getApplyType() != 3) {
            if (dto.getApplyTerm() == null) {
                throw new ApiException(400, "贷款类申请需要填写申请期限(月)");
            }
            if (dto.getApplyTerm() < 1 || dto.getApplyTerm() > 360) {
                throw new ApiException(400, "申请期限应在 1～360 个月之间");
            }
        } else if (dto.getApplyType() != null && dto.getApplyType() == 3) {
            if (dto.getApplyTerm() == null || dto.getApplyTerm() == 0) {
                dto.setApplyTerm(0);
            }
        }

        LoanApplication app = new LoanApplication();
        app.setApplicationNo(generateAppNo());
        app.setCustomerId(customer.getId());
        app.setApplyType(dto.getApplyType());
        app.setApplyAmount(dto.getApplyAmount());
        app.setApplyTerm(dto.getApplyTerm());
        app.setApplyPurpose(dto.getApplyPurpose());
        app.setCurrentStatus(STATUS_PENDING);
        app.setCreateBy(user.getId());
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        app.setIsDeleted(0);
        loanApplicationMapper.insert(app);

        return toVO(app, List.of());
    }

    @Override
    public List<LoanApplicationVO> listMyApplications() {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());

        List<LoanApplication> list = loanApplicationMapper.selectList(
                new LambdaQueryWrapper<LoanApplication>()
                        .eq(LoanApplication::getCustomerId, customer.getId())
                        .eq(LoanApplication::getIsDeleted, 0)
                        .orderByDesc(LoanApplication::getCreateTime)
        );
        return list.stream().map(a -> toVO(a, loadApprovalRecords(a.getId()))).toList();
    }

    @Override
    public LoanApplicationVO getMyApplicationDetail(Long id) {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());

        LoanApplication app = loanApplicationMapper.selectById(id);
        if (app == null || app.getIsDeleted() != null && app.getIsDeleted() == 1) {
            throw new ApiException(404, "申请不存在");
        }
        if (!customer.getId().equals(app.getCustomerId())) {
            throw new ApiException(403, "无权查看该申请");
        }
        return toVO(app, loadApprovalRecords(id));
    }

    @Override
    public List<LoanApplicationVO> listForRisk(String currentStatus, Integer applyType) {
        SysUser user = getCurrentUser();
        ensureRiskOfficer(user.getId());

        LambdaQueryWrapper<LoanApplication> w = new LambdaQueryWrapper<LoanApplication>()
                .eq(LoanApplication::getIsDeleted, 0)
                .orderByDesc(LoanApplication::getCreateTime);
        if (currentStatus != null && !currentStatus.isBlank()) {
            w.eq(LoanApplication::getCurrentStatus, currentStatus.trim().toUpperCase(Locale.ROOT));
        }
        if (applyType != null) {
            w.eq(LoanApplication::getApplyType, applyType);
        }
        return loanApplicationMapper.selectList(w).stream()
                .map(a -> toVO(a, loadApprovalRecords(a.getId())))
                .toList();
    }

    @Override
    public LoanApplicationVO getRiskApplicationDetail(Long id) {
        SysUser user = getCurrentUser();
        ensureRiskOfficer(user.getId());

        LoanApplication app = loanApplicationMapper.selectById(id);
        if (app == null || app.getIsDeleted() != null && app.getIsDeleted() == 1) {
            throw new ApiException(404, "申请不存在");
        }
        return toVO(app, loadApprovalRecords(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoanApplicationVO review(Long applicationId, LoanApplicationReviewDTO dto) {
        SysUser user = getCurrentUser();
        ensureRiskOfficer(user.getId());

        LoanApplication app = loanApplicationMapper.selectById(applicationId);
        if (app == null || app.getIsDeleted() != null && app.getIsDeleted() == 1) {
            throw new ApiException(404, "申请不存在");
        }
        if (!STATUS_PENDING.equals(app.getCurrentStatus())) {
            throw new ApiException(400, "该申请已审批结束，无法重复操作");
        }

        String action = dto.getAction().trim().toUpperCase(Locale.ROOT);
        if (!"PASS".equals(action) && !"REJECT".equals(action)) {
            throw new ApiException(400, "审批结论只能为 PASS 或 REJECT");
        }

        LoanApprovalRecord record = new LoanApprovalRecord();
        record.setApplicationId(applicationId);
        record.setReviewLevel(1);
        record.setReviewerId(user.getId());
        record.setReviewerName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        record.setAction(action);
        record.setComment(dto.getComment());
        record.setCreateTime(LocalDateTime.now());

        if ("PASS".equals(action)) {
            record.setNextLevel(null);
            app.setCurrentStatus(STATUS_APPROVED);
            app.setFinalResult(1);
            BigDecimal approvedAmount = dto.getFinalAmount() != null ? dto.getFinalAmount() : app.getApplyAmount();
            app.setFinalAmount(approvedAmount);
            Integer approvedTerm = dto.getFinalTerm() != null ? dto.getFinalTerm() : app.getApplyTerm();
            app.setFinalTerm(approvedTerm);
        } else {
            record.setNextLevel(null);
            app.setCurrentStatus(STATUS_REJECTED);
            app.setFinalResult(0);
            app.setFinalAmount(null);
            app.setFinalTerm(null);
        }

        loanApprovalRecordMapper.insert(record);
        app.setUpdateBy(user.getId());
        app.setUpdateTime(LocalDateTime.now());
        loanApplicationMapper.updateById(app);

        return toVO(app, loadApprovalRecords(applicationId));
    }

    private List<LoanApprovalRecordVO> loadApprovalRecords(Long applicationId) {
        List<LoanApprovalRecord> records = loanApprovalRecordMapper.selectList(
                new LambdaQueryWrapper<LoanApprovalRecord>()
                        .eq(LoanApprovalRecord::getApplicationId, applicationId)
                        .orderByAsc(LoanApprovalRecord::getCreateTime)
        );
        return records.stream().map(r -> LoanApprovalRecordVO.builder()
                .id(r.getId())
                .reviewLevel(r.getReviewLevel())
                .reviewerName(r.getReviewerName())
                .action(r.getAction())
                .comment(r.getComment())
                .createTime(r.getCreateTime())
                .build()).toList();
    }

    private LoanApplicationVO toVO(LoanApplication a, List<LoanApprovalRecordVO> records) {
        return LoanApplicationVO.builder()
                .id(a.getId())
                .applicationNo(a.getApplicationNo())
                .applyType(a.getApplyType())
                .applyTypeName(applyTypeName(a.getApplyType()))
                .applyAmount(a.getApplyAmount())
                .applyTerm(a.getApplyTerm())
                .applyPurpose(a.getApplyPurpose())
                .currentStatus(a.getCurrentStatus())
                .currentStatusName(statusName(a.getCurrentStatus()))
                .finalResult(a.getFinalResult())
                .finalResultName(finalResultName(a.getFinalResult()))
                .finalAmount(a.getFinalAmount())
                .finalTerm(a.getFinalTerm())
                .remark(a.getRemark())
                .createTime(a.getCreateTime())
                .updateTime(a.getUpdateTime())
                .approvalRecords(records)
                .build();
    }

    private String applyTypeName(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "信用贷款";
            case 2 -> "抵押贷款";
            case 3 -> "信用卡";
            default -> "未知";
        };
    }

    private String statusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case STATUS_PENDING -> "待审批";
            case STATUS_APPROVED -> "已通过";
            case STATUS_REJECTED -> "已拒绝";
            case "FIRST_REVIEW" -> "初审中";
            case "SECOND_REVIEW" -> "复审中";
            case "FINAL_REVIEW" -> "终审中";
            default -> status;
        };
    }

    private String finalResultName(Integer r) {
        if (r == null) return "—";
        return switch (r) {
            case 0 -> "拒绝";
            case 1 -> "通过";
            default -> "—";
        };
    }

    private String generateAppNo() {
        String t = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "APP" + t + rand;
    }

    private void ensureCanApply(CustCustomer customer) {
        if (customer.getProfileCompleted() == null || customer.getProfileCompleted() != 1) {
            throw new ApiException(400, "请先完善个人中心资料后再提交申请");
        }
        if (customer.getIsBlacklist() != null && customer.getIsBlacklist() == 1) {
            throw new ApiException(400, "您已被列入黑名单，无法提交申请");
        }
    }

    private void ensureCustomer(Long userId) {
        String roleCode = userMapper.selectPrimaryRoleCode(userId);
        if (!"CUSTOMER".equalsIgnoreCase(roleCode)) {
            throw new ApiException(403, "仅客户可提交或查看本人申请");
        }
    }

    private void ensureRiskOfficer(Long userId) {
        String roleCode = userMapper.selectPrimaryRoleCode(userId);
        if (!"RISK_OFFICER".equalsIgnoreCase(roleCode) && !"RISK_MANAGER".equalsIgnoreCase(roleCode)) {
            throw new ApiException(403, "仅风控人员可进行审批");
        }
    }

    private CustCustomer getCustomerOrThrow(Long userId) {
        CustCustomer customer = custCustomerMapper.selectByUserId(userId);
        if (customer == null || customer.getIsDeleted() != null && customer.getIsDeleted() == 1) {
            throw new ApiException(400, "客户档案不存在，请先完善个人中心");
        }
        return customer;
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
