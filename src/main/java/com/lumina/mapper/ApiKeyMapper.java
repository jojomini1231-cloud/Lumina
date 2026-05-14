package com.lumina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumina.dto.ApiKeyUsageDto;
import com.lumina.entity.ApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {

    List<ApiKeyUsageDto> selectApiKeyUsageList();

    ApiKeyUsageDto selectApiKeyUsageByKey(@Param("apiKey") String apiKey);
}
