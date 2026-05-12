package com.lumina.controller;

import com.lumina.dto.ApiResponse;
import com.lumina.stats.StatsRebuildJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    @Autowired
    private StatsRebuildJob statsRebuildJob;

    @PostMapping("/rebuild")
    public ApiResponse<String> rebuild() {
        if (statsRebuildJob.isRunning()) {
            return ApiResponse.success("统计重建任务已在运行中");
        }
        statsRebuildJob.rebuildAll();
        return ApiResponse.success("统计重建任务已启动，请稍后查看日志确认完成");
    }

    @GetMapping("/rebuild/status")
    public ApiResponse<Boolean> rebuildStatus() {
        return ApiResponse.success(statsRebuildJob.isRunning());
    }
}
