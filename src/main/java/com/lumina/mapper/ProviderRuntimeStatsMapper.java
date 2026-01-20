package com.lumina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumina.entity.ProviderRuntimeStats;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

@Mapper
public interface ProviderRuntimeStatsMapper extends BaseMapper<ProviderRuntimeStats> {

    @Insert("INSERT INTO provider_runtime_stats (provider_id, provider_name, success_rate_ema, latency_ema_ms, score, total_requests, success_requests, failure_requests, circuit_state, circuit_opened_at, updated_at) " +
            "VALUES (#{providerId}, #{providerName}, #{successRateEma}, #{latencyEmaMs}, #{score}, #{totalRequests}, #{successRequests}, #{failureRequests}, #{circuitState}, #{circuitOpenedAt}, #{updatedAt}) " +
            "ON DUPLICATE KEY UPDATE " +
            "provider_name = VALUES(provider_name), " +
            "success_rate_ema = VALUES(success_rate_ema), " +
            "latency_ema_ms = VALUES(latency_ema_ms), " +
            "score = VALUES(score), " +
            "total_requests = VALUES(total_requests), " +
            "success_requests = VALUES(success_requests), " +
            "failure_requests = VALUES(failure_requests), " +
            "circuit_state = VALUES(circuit_state), " +
            "circuit_opened_at = VALUES(circuit_opened_at), " +
            "updated_at = VALUES(updated_at)")
    int upsert(ProviderRuntimeStats stats);

    /**
     * 删除指定的 Provider 运行态数据
     * @param providerId Provider ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM provider_runtime_stats WHERE provider_id = #{providerId}")
    int deleteByProviderId(@Param("providerId") String providerId);

    /**
     * 批量删除不在指定列表中的 Provider 运行态数据
     * @param validProviderIds 有效的 Provider ID 列表
     * @return 删除的记录数
     */
    @Delete("<script>" +
            "DELETE FROM provider_runtime_stats " +
            "<where>" +
            "  <if test='validProviderIds != null and validProviderIds.size() > 0'>" +
            "    provider_id NOT IN " +
            "    <foreach collection='validProviderIds' item='id' open='(' separator=',' close=')'>" +
            "      #{id}" +
            "    </foreach>" +
            "  </if>" +
            "</where>" +
            "</script>")
    int deleteNotInProviderIds(@Param("validProviderIds") Collection<String> validProviderIds);
}
