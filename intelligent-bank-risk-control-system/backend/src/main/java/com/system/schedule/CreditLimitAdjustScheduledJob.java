package com.system.schedule;

import com.system.service.CreditLimitManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时授信动态调整：下调自动落地，上调仅生成复核工单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLimitAdjustScheduledJob {

    private final CreditLimitManagementService creditLimitManagementService;

    @Scheduled(cron = "${credit.limit.adjust-cron:0 0 2 * * ?}")
    public void runDailyAdjustment() {
        log.info("定时任务：开始授信动态调整批跑");
        creditLimitManagementService.runPeriodicAdjustment();
    }
}
