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

    List<ApiKeyUsageDto> selectApiKeyUsagePage(
            @Param("isEnabled") Boolean isEnabled,
            @Param("keyGroup") String keyGroup,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("size") long size);

    Long countApiKeyUsage(
            @Param("isEnabled") Boolean isEnabled,
            @Param("keyGroup") String keyGroup,
            @Param("keyword") String keyword);

    List<String> selectApiKeyGroups(@Param("isEnabled") Boolean isEnabled);

    ApiKeyUsageDto selectApiKeyUsageByKey(@Param("apiKey") String apiKey);
}
