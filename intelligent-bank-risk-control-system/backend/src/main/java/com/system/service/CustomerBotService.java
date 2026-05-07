package com.system.service;

import com.system.dto.BotMessageFeedbackDTO;
import com.system.dto.BotSessionSatisfactionDTO;
import com.system.dto.BotTransferDTO;
import com.system.dto.CustomerBotChatDTO;
import com.system.vo.BotMessageVO;
import com.system.vo.CustomerBotReplyVO;

import java.util.List;

public interface CustomerBotService {

    String createSession();

    List<BotMessageVO> listMessages(String sessionId);

    CustomerBotReplyVO chat(String sessionId, CustomerBotChatDTO dto);

    void feedback(Long messageId, BotMessageFeedbackDTO dto);

    void submitSatisfaction(String sessionId, BotSessionSatisfactionDTO dto);

    void requestHuman(String sessionId, BotTransferDTO dto);
}
