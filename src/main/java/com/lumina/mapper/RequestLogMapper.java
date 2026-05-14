package com.lumina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumina.entity.RequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RequestLogMapper extends BaseMapper<RequestLog> {

    @Update("UPDATE request_logs SET request_content = NULL, response_content = NULL " +
            "WHERE request_time < #{timestamp} AND (request_content IS NOT NULL OR response_content IS NOT NULL)")
    int clearContentBefore(@Param("timestamp") long timestamp);
}
