package com.lumina.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lumina.entity.Provider;
import com.lumina.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provider 模型自动同步定时任务
 * 定期获取开启自动同步的供应商，并更新其模型列表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderModelSyncJob {

    private final ProviderService providerService;

    /**
     * 每小时执行一次模型同步
     * fixedDelay = 3600000ms = 1小时
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void syncProviderModels() {
        log.info("开始执行 Provider 模型自动同步任务");

        try {
            // 查询所有开启自动同步且已启用的供应商
            QueryWrapper<Provider> wrapper = new QueryWrapper<>();
            wrapper.eq("auto_sync", true)
                   .eq("is_enabled", true);

            List<Provider> providers = providerService.list(wrapper);

            if (providers.isEmpty()) {
                log.info("没有需要自动同步的供应商");
                return;
            }

            log.info("找到 {} 个需要自动同步的供应商", providers.size());

            int successCount = 0;
            int failureCount = 0;

            // 遍历每个供应商，获取并更新模型列表
            for (Provider provider : providers) {
                try {
                    log.debug("正在同步供应商 [{}] 的模型列表", provider.getName());

                    // 获取供应商的模型列表
                    List<String> models = providerService.getModels(provider);

                    if (models == null || models.isEmpty()) {
                        log.warn("供应商 [{}] 未返回任何模型", provider.getName());
                        failureCount++;
                        continue;
                    }

                    // 将模型列表转换为逗号分隔的字符串
                    String modelNames = String.join(",", models);

                    // 更新供应商的模型名称
                    provider.setModelName(modelNames);
                    provider.setUpdatedAt(LocalDateTime.now());

                    boolean updated = providerService.updateById(provider);

                    if (updated) {
                        log.info("供应商 [{}] 模型同步成功，共 {} 个模型",
                                provider.getName(), models.size());
                        successCount++;
                    } else {
                        log.error("供应商 [{}] 模型更新失败", provider.getName());
                        failureCount++;
                    }

                } catch (Exception e) {
                    log.error("同步供应商 [{}] 模型时发生异常：{}", provider.getName(), e.getMessage());
                    failureCount++;
                }
            }

            log.info("Provider 模型自动同步任务完成，成功: {}, 失败: {}", successCount, failureCount);

        } catch (Exception e) {
            log.error("执行 Provider 模型自动同步任务时发生异常", e);
        }
    }
}
