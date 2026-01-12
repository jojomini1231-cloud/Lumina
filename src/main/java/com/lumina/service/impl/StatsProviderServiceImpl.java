package com.lumina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.entity.StatsProvider;
import com.lumina.mapper.StatsProviderMapper;
import com.lumina.service.StatsProviderService;
import org.springframework.stereotype.Service;

@Service
public class StatsProviderServiceImpl extends ServiceImpl<StatsProviderMapper, StatsProvider> implements StatsProviderService {
}
