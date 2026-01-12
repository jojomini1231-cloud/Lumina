package com.lumina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.entity.Provider;
import com.lumina.mapper.ProviderMapper;
import com.lumina.service.ProviderService;
import org.springframework.stereotype.Service;

@Service
public class ProviderServiceImpl extends ServiceImpl<ProviderMapper, Provider> implements ProviderService {
}
