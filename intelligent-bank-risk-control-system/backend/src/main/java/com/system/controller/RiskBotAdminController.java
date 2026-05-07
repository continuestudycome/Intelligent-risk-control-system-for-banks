package com.system.controller;

import com.system.common.Result;
import com.system.dto.BotKnowledgeSaveDTO;
import com.system.service.RiskBotAdminService;
import com.system.vo.BotKnowledgeVO;
import com.system.vo.BotSessionAdminVO;
import com.system.vo.BotStatsSummaryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/risk/bot")
@RequiredArgsConstructor
public class RiskBotAdminController {

    private final RiskBotAdminService riskBotAdminService;

    @GetMapping("/knowledge")
    public Result<List<BotKnowledgeVO>> listKnowledge(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "docType", required = false) String docType
    ) {
        return Result.success(riskBotAdminService.listKnowledge(keyword, category, docType));
    }

    @PostMapping("/knowledge")
    public Result<BotKnowledgeVO> createKnowledge(@Valid @RequestBody BotKnowledgeSaveDTO dto) {
        return Result.success(riskBotAdminService.createKnowledge(dto));
    }

    @PutMapping("/knowledge/{id}")
    public Result<BotKnowledgeVO> updateKnowledge(
            @PathVariable("id") Long id,
            @Valid @RequestBody BotKnowledgeSaveDTO dto
    ) {
        return Result.success(riskBotAdminService.updateKnowledge(id, dto));
    }

    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable("id") Long id) {
        riskBotAdminService.deleteKnowledge(id);
        return Result.success(null);
    }

    @GetMapping("/sessions")
    public Result<List<BotSessionAdminVO>> listSessions(
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "satisfaction", required = false) Integer satisfaction,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return Result.success(riskBotAdminService.listSessions(topic, status, satisfaction, limit));
    }

    @GetMapping("/stats/summary")
    public Result<BotStatsSummaryVO> statsSummary() {
        return Result.success(riskBotAdminService.statsSummary());
    }
}
