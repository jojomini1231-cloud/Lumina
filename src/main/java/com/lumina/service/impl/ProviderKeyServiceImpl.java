package com.lumina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumina.entity.ProviderKey;
import com.lumina.mapper.ProviderKeyMapper;
import com.lumina.service.ProviderKeyService;
import org.springframework.stereotype.Service;

@Service
public class ProviderKeyServiceImpl extends ServiceImpl<ProviderKeyMapper, ProviderKey> implements ProviderKeyService {
}
