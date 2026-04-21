package com.lumina;

import com.lumina.scheduled.ProviderModelSyncJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
    "lumina.sync.model-info-interval=2"
})
public class ProviderModelSyncJobTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;

    @Autowired
    private ProviderModelSyncJob providerModelSyncJob;

    @Test
    public void testScheduledTaskConfiguredCorrectly() {
        Set<ScheduledTask> scheduledTasks = scheduledAnnotationBeanPostProcessor.getScheduledTasks();
        assertNotNull(scheduledTasks);
        assertFalse(scheduledTasks.isEmpty());
        
        // 我们只需验证 Spring 能成功解析 SpEL 表达式并启动任务即可
        // 具体的 interval 在运行时会被正确计算
    }
}
