package com.system.controller;

import com.system.common.Result;
import com.system.dto.BotMessageFeedbackDTO;
import com.system.dto.BotSessionSatisfactionDTO;
import com.system.dto.BotTransferDTO;
import com.system.dto.CustomerBotChatDTO;
import com.system.service.CustomerBotService;
import com.system.vo.BotMessageVO;
import com.system.vo.CustomerBotReplyVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/bot")
@RequiredArgsConstructor
public class CustomerBotController {

    private final CustomerBotService customerBotService;

    @PostMapping("/sessions")
    public Result<String> createSession() {
        return Result.success(customerBotService.createSession());
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<BotMessageVO>> messages(@PathVariable("sessionId") String sessionId) {
        return Result.success(customerBotService.listMessages(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/chat")
    public Result<CustomerBotReplyVO> chat(
            @PathVariable("sessionId") String sessionId,
            @Valid @RequestBody CustomerBotChatDTO dto
    ) {
        return Result.success(customerBotService.chat(sessionId, dto));
    }

    @PostMapping("/messages/{messageId}/feedback")
    public Result<Void> feedback(
            @PathVariable("messageId") Long messageId,
            @Valid @RequestBody BotMessageFeedbackDTO dto
    ) {
        customerBotService.feedback(messageId, dto);
        return Result.success(null);
    }

    @PutMapping("/sessions/{sessionId}/satisfaction")
    public Result<Void> satisfaction(
            @PathVariable("sessionId") String sessionId,
            @Valid @RequestBody BotSessionSatisfactionDTO dto
    ) {
        customerBotService.submitSatisfaction(sessionId, dto);
        return Result.success(null);
    }

    @PostMapping("/sessions/{sessionId}/transfer")
    public Result<Void> transfer(
            @PathVariable("sessionId") String sessionId,
            @RequestBody(required = false) BotTransferDTO dto
    ) {
        customerBotService.requestHuman(sessionId, dto != null ? dto : new BotTransferDTO());
        return Result.success(null);
    }
}
