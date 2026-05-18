package com.lumina;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumina.entity.LlmModel;
import com.lumina.entity.RequestLog;
import com.lumina.service.LlmModelService;
import com.lumina.service.RequestLogService;
import com.lumina.stats.StatsRebuildJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class CostRecalculationTest {

    @Autowired
    private RequestLogService requestLogService;

    @Autowired
    private LlmModelService llmModelService;

    @Autowired
    private StatsRebuildJob statsRebuildJob;

    @Test
    void recalculateCosts() {
        Map<String, LlmModel> priceCache = new HashMap<>();
        int pageSize = 500;
        int pageNum = 1;
        int totalUpdated = 0;
        int totalSkipped = 0;
        int totalMismatch = 0;

        while (true) {
            Page<RequestLog> page = requestLogService.page(
                    new Page<>(pageNum, pageSize),
                    new LambdaQueryWrapper<RequestLog>()
                            .isNotNull(RequestLog::getInputTokens)
                            .isNotNull(RequestLog::getOutputTokens)
                            .orderByAsc(RequestLog::getRequestTime)
            );

            List<RequestLog> records = page.getRecords();
            if (records.isEmpty()) {
                break;
            }

            for (RequestLog log : records) {
                String modelName = log.getActualModelName();
                if (modelName == null || modelName.isBlank()) {
                    modelName = log.getRequestModelName();
                }
                if (modelName == null || modelName.isBlank()) {
                    totalSkipped++;
                    continue;
                }

                LlmModel model = priceCache.computeIfAbsent(modelName, name -> {
                    LlmModel m = llmModelService.findLatestByModelName(name);
                    if (m == null) {
                        m = llmModelService.getOne(new LambdaQueryWrapper<LlmModel>()
                                .eq(LlmModel::getModelName, name)
                                .last("limit 1"));
                    }
                    return m;
                });

                if (model == null || model.getInputPrice() == null || model.getOutputPrice() == null) {
                    totalSkipped++;
                    continue;
                }

                BigDecimal inputCost = BigDecimal.ZERO;
                BigDecimal outputCost = BigDecimal.ZERO;

                if (log.getInputTokens() != null && log.getInputTokens() > 0) {
                    inputCost = model.getInputPrice()
                            .multiply(BigDecimal.valueOf(log.getInputTokens()))
                            .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
                }

                if (log.getOutputTokens() != null && log.getOutputTokens() > 0) {
                    outputCost = model.getOutputPrice()
                            .multiply(BigDecimal.valueOf(log.getOutputTokens()))
                            .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
                }

                BigDecimal newCost = inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
                BigDecimal oldCost = log.getCost() != null ? log.getCost() : BigDecimal.ZERO;

                if (newCost.compareTo(oldCost) != 0) {
                    totalMismatch++;
                    System.out.printf("[MISMATCH] id=%s model=%s input=%d output=%d oldCost=%s newCost=%s%n",
                            log.getId(), modelName,
                            log.getInputTokens(), log.getOutputTokens(),
                            oldCost.toPlainString(), newCost.toPlainString());

                    log.setCost(newCost);
                    requestLogService.updateById(log);
                    totalUpdated++;
                }
            }

            if (!page.hasNext()) {
                break;
            }
            pageNum++;
        }

        System.out.println("========== 费用重算完成 ==========");
        System.out.printf("总计不一致: %d 条%n", totalMismatch);
        System.out.printf("已更新: %d 条%n", totalUpdated);
        System.out.printf("跳过(无模型价格): %d 条%n", totalSkipped);

        // 重建统计聚合表和 Redis 缓存，使首页数据同步
        if (totalUpdated > 0) {
            System.out.println("正在重建统计数据...");
            statsRebuildJob.rebuildAll();
            // rebuildAll 是 @Async 的，等待完成
            while (statsRebuildJob.isRunning()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("统计数据重建完成，首页数据已同步");
        }
    }
}
