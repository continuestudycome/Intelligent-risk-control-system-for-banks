package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.BotKnowledge;
import com.system.domain.BotMessage;
import com.system.domain.BotSession;
import com.system.domain.SysUser;
import com.system.dto.BotKnowledgeSaveDTO;
import com.system.exception.ApiException;
import com.system.mapper.BotKnowledgeMapper;
import com.system.mapper.BotMessageMapper;
import com.system.mapper.BotSessionMapper;
import com.system.mapper.UserMapper;
import com.system.service.BotKnowledgeIndexSync;
import com.system.service.RiskBotAdminService;
import com.system.vo.BotKnowledgeVO;
import com.system.vo.BotSessionAdminVO;
import com.system.vo.BotStatsSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskBotAdminServiceImpl implements RiskBotAdminService {

    private final BotKnowledgeMapper botKnowledgeMapper;
    private final BotKnowledgeIndexSync botKnowledgeIndexSync;
    private final BotSessionMapper botSessionMapper;
    private final BotMessageMapper botMessageMapper;
    private final UserMapper userMapper;

    @Override
    public List<BotKnowledgeVO> listKnowledge(String keyword, String category, String docType) {
        ensureRiskStaff();
        LambdaQueryWrapper<BotKnowledge> w = new LambdaQueryWrapper<BotKnowledge>()
                .eq(BotKnowledge::getIsDeleted, 0)
                .orderByDesc(BotKnowledge::getUpdateTime);
        if (category != null && !category.isBlank()) {
            w.eq(BotKnowledge::getCategory, category.trim().toUpperCase(Locale.ROOT));
        }
        if (docType != null && !docType.isBlank()) {
            w.eq(BotKnowledge::getDocType, docType.trim().toUpperCase(Locale.ROOT));
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(x -> x.like(BotKnowledge::getQuestion, kw)
                    .or()
                    .like(BotKnowledge::getKeywords, kw)
                    .or()
                    .like(BotKnowledge::getAnswer, kw));
        }
        return botKnowledgeMapper.selectList(w).stream().map(this::toKnowVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BotKnowledgeVO createKnowledge(BotKnowledgeSaveDTO dto) {
        ensureRiskStaff();
        BotKnowledge k = new BotKnowledge();
        fillKnowledge(k, dto);
        k.setHitCount(0);
        k.setIsDeleted(0);
        k.setCreateBy(currentUserId());
        botKnowledgeMapper.insert(k);
        botKnowledgeIndexSync.upsertKnowledge(k);
        return toKnowVo(k);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BotKnowledgeVO updateKnowledge(Long id, BotKnowledgeSaveDTO dto) {
        ensureRiskStaff();
        BotKnowledge k = botKnowledgeMapper.selectById(id);
        if (k == null || (k.getIsDeleted() != null && k.getIsDeleted() == 1)) {
            throw new ApiException(404, "知识不存在");
        }
        fillKnowledge(k, dto);
        k.setUpdateBy(currentUserId());
        botKnowledgeMapper.updateById(k);
        botKnowledgeIndexSync.upsertKnowledge(k);
        return toKnowVo(k);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledge(Long id) {
        ensureRiskStaff();
        BotKnowledge k = botKnowledgeMapper.selectById(id);
        if (k == null) {
            throw new ApiException(404, "知识不存在");
        }
        k.setIsDeleted(1);
        k.setUpdateBy(currentUserId());
        botKnowledgeMapper.updateById(k);
        botKnowledgeIndexSync.removeKnowledgeFromIndex(id);
    }

    @Override
    public List<BotSessionAdminVO> listSessions(String topic, Integer status, Integer satisfaction, Integer limit) {
        ensureRiskStaff();
        int lim = limit == null ? 80 : Math.min(Math.max(limit, 1), 200);
        LambdaQueryWrapper<BotSession> w = new LambdaQueryWrapper<BotSession>().orderByDesc(BotSession::getCreateTime);
        if (topic != null && !topic.isBlank()) {
            w.eq(BotSession::getSessionTopic, topic.trim());
        }
        if (status != null) {
            w.eq(BotSession::getStatus, status);
        }
        if (satisfaction != null) {
            w.eq(BotSession::getSatisfaction, satisfaction);
        }
        w.last("LIMIT " + lim);
        List<BotSession> sessions = botSessionMapper.selectList(w);
        return sessions.stream().map(s -> {
            long cnt = botMessageMapper.selectCount(
                    new LambdaQueryWrapper<BotMessage>().eq(BotMessage::getSessionId, s.getSessionId()));
            return BotSessionAdminVO.builder()
                    .id(s.getId())
                    .sessionId(s.getSessionId())
                    .userId(s.getUserId())
                    .channel(s.getChannel())
                    .sessionTopic(s.getSessionTopic())
                    .status(s.getStatus())
                    .satisfaction(s.getSatisfaction())
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .createTime(s.getCreateTime())
                    .messageCount(cnt)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public BotStatsSummaryVO statsSummary() {
        ensureRiskStaff();
        long total = botSessionMapper.selectCount(new LambdaQueryWrapper<BotSession>());
        long active = botSessionMapper.selectCount(
                new LambdaQueryWrapper<BotSession>().eq(BotSession::getStatus, 1));
        long transferred = botSessionMapper.selectCount(
                new LambdaQueryWrapper<BotSession>().eq(BotSession::getStatus, 2));
        long sat3 = botSessionMapper.selectCount(
                new LambdaQueryWrapper<BotSession>().eq(BotSession::getSatisfaction, 3));
        long sat2 = botSessionMapper.selectCount(
                new LambdaQueryWrapper<BotSession>().eq(BotSession::getSatisfaction, 2));
        long sat1 = botSessionMapper.selectCount(
                new LambdaQueryWrapper<BotSession>().eq(BotSession::getSatisfaction, 1));
        return BotStatsSummaryVO.builder()
                .totalSessions(total)
                .activeSessions(active)
                .transferredSessions(transferred)
                .satisfiedCount(sat3)
                .neutralCount(sat2)
                .dissatisfiedCount(sat1)
                .build();
    }

    private void fillKnowledge(BotKnowledge k, BotKnowledgeSaveDTO dto) {
        k.setCategory(dto.getCategory().trim().toUpperCase(Locale.ROOT));
        k.setDocType(dto.getDocType().trim().toUpperCase(Locale.ROOT));
        k.setQuestion(dto.getQuestion().trim());
        k.setAnswer(dto.getAnswer().trim());
        k.setSimilarQuestions(dto.getSimilarQuestions());
        k.setKeywords(dto.getKeywords());
        k.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        if (dto.getSourceType() != null && !dto.getSourceType().isBlank()) {
            k.setSourceType(dto.getSourceType().trim().toUpperCase(Locale.ROOT));
        } else if (k.getId() == null) {
            k.setSourceType("MANUAL");
        }
        if (dto.getSourceFilename() != null && !dto.getSourceFilename().isBlank()) {
            k.setSourceFilename(dto.getSourceFilename().trim());
        }
    }

    private BotKnowledgeVO toKnowVo(BotKnowledge k) {
        return BotKnowledgeVO.builder()
                .id(k.getId())
                .category(k.getCategory())
                .docType(k.getDocType())
                .question(k.getQuestion())
                .answer(k.getAnswer())
                .similarQuestions(k.getSimilarQuestions())
                .keywords(k.getKeywords())
                .hitCount(k.getHitCount())
                .status(k.getStatus())
                .updateTime(k.getUpdateTime())
                .sourceType(k.getSourceType())
                .sourceFilename(k.getSourceFilename())
                .vectorIndexedAt(k.getVectorIndexedAt())
                .build();
    }

    private Long currentUserId() {
        SysUser u = getCurrentUser();
        return u.getId();
    }

    private void ensureRiskStaff() {
        SysUser user = getCurrentUser();
        String code = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可操作");
        }
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
}
