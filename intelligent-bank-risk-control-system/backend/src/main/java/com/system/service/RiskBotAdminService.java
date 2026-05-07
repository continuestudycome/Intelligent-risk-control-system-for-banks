package com.system.service;

import com.system.dto.BotKnowledgeSaveDTO;
import com.system.vo.BotKnowledgeVO;
import com.system.vo.BotSessionAdminVO;
import com.system.vo.BotStatsSummaryVO;

import java.util.List;

public interface RiskBotAdminService {

    List<BotKnowledgeVO> listKnowledge(String keyword, String category, String docType);

    BotKnowledgeVO createKnowledge(BotKnowledgeSaveDTO dto);

    BotKnowledgeVO updateKnowledge(Long id, BotKnowledgeSaveDTO dto);

    void deleteKnowledge(Long id);

    List<BotSessionAdminVO> listSessions(String topic, Integer status, Integer satisfaction, Integer limit);

    BotStatsSummaryVO statsSummary();
}
