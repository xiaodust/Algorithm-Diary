package com.algodiary.controller;

import com.algodiary.config.LeetCodeCredentials;
import com.algodiary.service.DailyPlanService;
import com.algodiary.dto.SyncResult;
import com.algodiary.dto.SyncResponse;
import com.algodiary.service.SyncService;
import com.algodiary.service.TopicService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final TopicService topicService;
    private final LeetCodeCredentials credentials;
    private final DailyPlanService dailyPlanService;

    public SyncController(
            SyncService syncService,
            TopicService topicService,
            LeetCodeCredentials credentials,
            DailyPlanService dailyPlanService
    ) {
        this.syncService = syncService;
        this.topicService = topicService;
        this.credentials = credentials;
        this.dailyPlanService = dailyPlanService;
    }

    @PostMapping
    public SyncResponse sync() {
        if (!credentials.isConfigured()) {
            return new SyncResponse(false, 0, 0, "未配置 LeetCode 登录态，请先配置后再同步");
        }
        SyncResult result = syncService.sync();
        topicService.enrichAll();
        dailyPlanService.autoCompleteIfDone(Instant.now());
        return new SyncResponse(false, result.problems(), result.submissions(), "同步完成");
    }

}
