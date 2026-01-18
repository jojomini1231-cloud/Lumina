package com.lumina.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求流量数据点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestTrafficDto {
    /**
     * 时间点（小时：0-23）
     */
    private Integer hour;

    /**
     * 请求次数
     */
    private Long requestCount;

    /**
     * 时间戳
     */
    private Long timestamp;
}
