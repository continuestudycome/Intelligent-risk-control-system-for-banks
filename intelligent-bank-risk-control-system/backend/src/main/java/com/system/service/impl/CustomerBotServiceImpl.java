package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.system.domain.BotHumanTransfer;
import com.system.domain.BotKnowledge;
import com.system.domain.BotMessage;
import com.system.domain.BotSession;
import com.system.domain.SysUser;
import com.system.dto.BotMessageFeedbackDTO;
import com.system.dto.BotSessionSatisfactionDTO;
import com.system.dto.BotTransferDTO;
import com.system.dto.CustomerBotChatDTO;
import com.system.exception.ApiException;
import com.system.mapper.BotHumanTransferMapper;
import com.system.mapper.BotKnowledgeMapper;
import com.system.mapper.BotMessageMapper;
import com.system.mapper.BotSessionMapper;
import com.system.mapper.UserMapper;
import com.system.service.BotKnowledgeIndexSync;
import com.system.service.BotRagClient;
import com.system.service.CustomerBotService;
import com.system.vo.BotMessageVO;
import com.system.vo.CustomerBotReplyVO;
import com.system.vo.RagCitationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerBotServiceImpl implements CustomerBotService {

    private final BotSessionMapper botSessionMapper;
    private final BotMessageMapper botMessageMapper;
    private final BotKnowledgeMapper botKnowledgeMapper;
    private final BotHumanTransferMapper botHumanTransferMapper;
    private final UserMapper userMapper;
    private final BotRagClient botRagClient;
    private final BotKnowledgeIndexSync botKnowledgeIndexSync;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSession() {
        SysUser user = getCurrentUser();
        ensureCustomerRole(user.getId());
        String sid = UUID.randomUUID().toString().replace("-", "");
        BotSession s = new BotSession();
        s.setSessionId(sid);
        s.setUserId(user.getId());
        s.setUserType(2);
        s.setChannel("WEB");
        s.setStatus(1);
        s.setStartTime(LocalDateTime.now());
        botSessionMapper.insert(s);
        return sid;
    }

    @Override
    public List<BotMessageVO> listMessages(String sessionId) {
        SysUser user = getCurrentUser();
        ensureCustomerRole(user.getId());
        BotSession session = findSession(sessionId);
        if (!session.getUserId().equals(user.getId())) {
            throw new ApiException(403, "无权访问该会话");
        }
        List<BotMessage> rows = botMessageMapper.selectList(
                new LambdaQueryWrapper<BotMessage>()
                        .eq(BotMessage::getSessionId, sessionId)
                        .orderByAsc(BotMessage::getId));
        return rows.stream().map(this::toMsgVo).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerBotReplyVO chat(String sessionId, CustomerBotChatDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomerRole(user.getId());
        BotSession session = findSession(sessionId);
        if (!session.getUserId().equals(user.getId())) {
            throw new ApiException(403, "无权访问该会话");
        }
        if (session.getStatus() != null && session.getStatus() == 0) {
            throw new ApiException(400, "会话已结束");
        }

        botKnowledgeIndexSync.ensureIndexedWithDb();

        List<BotMessage> prior = botMessageMapper.selectList(
                new LambdaQueryWrapper<BotMessage>()
                        .eq(BotMessage::getSessionId, sessionId)
                        .orderByAsc(BotMessage::getId)
                        .last("LIMIT 80"));
        ArrayNode historyNode = objectMapper.createArrayNode();
        for (BotMessage m : prior) {
            if (m.getMessageType() == null) {
                continue;
            }
            ObjectNode turn = objectMapper.createObjectNode();
            if (m.getMessageType() == 1) {
                turn.put("role", "user");
            } else if (m.getMessageType() == 2) {
                turn.put("role", "assistant");
            } else {
                continue;
            }
            turn.put("content", m.getContent() != null ? m.getContent() : "");
            historyNode.add(turn);
        }

        BotMessage userMsg = new BotMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setMessageType(1);
        userMsg.setContent(dto.getContent().trim());
        userMsg.setCreateTime(LocalDateTime.now());
        botMessageMapper.insert(userMsg);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("question", dto.getContent().trim());
        body.set("history", historyNode);
        String hint = session.getSessionTopic();
        if (hint == null || hint.isBlank()) {
            hint = dto.getScopeCategory();
        }
        if (hint != null && !hint.isBlank()) {
            body.put("category_hint", hint);
        }
        if (dto.getScopeCategory() != null && !dto.getScopeCategory().isBlank()) {
            String sc = dto.getScopeCategory().trim().toUpperCase(Locale.ROOT);
            long inScope = botKnowledgeMapper.selectCount(
                    new LambdaQueryWrapper<BotKnowledge>()
                            .eq(BotKnowledge::getStatus, 1)
                            .eq(BotKnowledge::getIsDeleted, 0)
                            .eq(BotKnowledge::getCategory, sc));
            if (inScope > 0) {
                body.put("category_scope", sc);
            }
        }

        JsonNode ragResult = botRagClient.ragChat(body);
        String answer = ragResult.path("answer").asText("");
        JsonNode cites = ragResult.path("citations");
        String mode = ragResult.path("mode").asText("");
        String model = ragResult.path("model").asText("");

        List<Long> citeIds = new ArrayList<>();
        List<RagCitationVO> citationVos = new ArrayList<>();
        BigDecimal topConf = null;
        if (cites.isArray()) {
            for (JsonNode c : cites) {
                long kid = c.path("id").asLong(0);
                BigDecimal sc = BigDecimal.valueOf(c.path("score").asDouble(0)).setScale(4, RoundingMode.HALF_UP);
                citeIds.add(kid);
                citationVos.add(RagCitationVO.builder().id(kid).score(sc).build());
                if (topConf == null) {
                    topConf = sc;
                }
            }
        }

        Long matchedKid = citeIds.isEmpty() ? null : citeIds.get(0);

        BotMessage botMsg = new BotMessage();
        botMsg.setSessionId(sessionId);
        botMsg.setMessageType(2);
        botMsg.setContent(answer);
        botMsg.setMatchedKnowledgeId(matchedKid);
        botMsg.setConfidence(topConf);
        botMsg.setModelName(model);
        try {
            botMsg.setRagSources(objectMapper.writeValueAsString(citeIds));
        } catch (Exception e) {
            botMsg.setRagSources("[]");
        }
        botMsg.setCreateTime(LocalDateTime.now());
        botMessageMapper.insert(botMsg);

        if (matchedKid != null) {
            BotKnowledge top = botKnowledgeMapper.selectById(matchedKid);
            if (top != null) {
                top.setHitCount((top.getHitCount() == null ? 0 : top.getHitCount()) + 1);
                botKnowledgeMapper.updateById(top);
            }
        }

        if (matchedKid != null) {
            BotKnowledge mk = botKnowledgeMapper.selectById(matchedKid);
            if (mk != null && mk.getCategory() != null
                    && (session.getSessionTopic() == null || session.getSessionTopic().isBlank())) {
                session.setSessionTopic(mk.getCategory());
                botSessionMapper.updateById(session);
            }
        }

        return CustomerBotReplyVO.builder()
                .assistantMessageId(botMsg.getId())
                .answer(answer)
                .citations(citationVos)
                .mode(mode)
                .model(model)
                .build();
    }

    @Override
    public void feedback(Long messageId, BotMessageFeedbackDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomerRole(user.getId());
        BotMessage m = botMessageMapper.selectById(messageId);
        if (m == null) {
            throw new ApiException(404, "消息不存在");
        }
        if (m.getMessageType() == null || m.getMessageType() != 2) {
            throw new ApiException(400, "仅能对客服回复进行反馈");
        }
        BotSession session = findSession(m.getSessionId());
        if (!session.getUserId().equals(user.getId())) {
            throw new ApiException(403, "无权操作");
        }
        m.setIsHelpful(Boolean.TRUE.equals(dto.getHelpful()) ? 1 : 0);
        botMessageMapper.updateById(m);
    }

    @Override
    public void submitSatisfaction(String sessionId, BotSessionSatisfactionDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomerRole(user.getId());
        BotSession session = findSession(sessionId);
        if (!session.getUserId().equals(user.getId())) {
            throw new ApiException(403, "无权操作");
        }
        session.setSatisfaction(dto.getSatisfaction());
        session.setStatus(0);
        session.setEndTime(LocalDateTime.now());
        botSessionMapper.updateById(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestHuman(String sessionId, BotTransferDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomerRole(user.getId());
        BotSession session = findSession(sessionId);
        if (!session.getUserId().equals(user.getId())) {
            throw new ApiException(403, "无权操作");
        }
        session.setStatus(2);
        botSessionMapper.updateById(session);

        BotHumanTransfer t = new BotHumanTransfer();
        t.setSessionId(sessionId);
        t.setTransferReason(dto.getReason() != null ? dto.getReason() : "用户请求人工");
        t.setStatus(0);
        t.setCreateTime(LocalDateTime.now());
        botHumanTransferMapper.insert(t);

        BotMessage sys = new BotMessage();
        sys.setSessionId(sessionId);
        sys.setMessageType(4);
        sys.setContent("已为您登记转人工诉求，客服将在工作时间回访。");
        sys.setCreateTime(LocalDateTime.now());
        botMessageMapper.insert(sys);
    }

    private BotSession findSession(String sessionId) {
        BotSession s = botSessionMapper.selectOne(
                new LambdaQueryWrapper<BotSession>().eq(BotSession::getSessionId, sessionId));
        if (s == null) {
            throw new ApiException(404, "会话不存在");
        }
        return s;
    }

    private BotMessageVO toMsgVo(BotMessage m) {
        return BotMessageVO.builder()
                .id(m.getId())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .matchedKnowledgeId(m.getMatchedKnowledgeId())
                .confidence(m.getConfidence())
                .isHelpful(m.getIsHelpful())
                .createTime(m.getCreateTime())
                .build();
    }

    private void ensureCustomerRole(Long userId) {
        if (userMapper.countUserRole(userId, "CUSTOMER") > 0) {
            return;
        }
        throw new ApiException(403, "仅客户可使用智能客服");
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
