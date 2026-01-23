import React, { createContext, useContext, useState, ReactNode } from 'react';

export type Language = 'en' | 'zh';

const translations = {
  zh: {
    common: {
      add: '添加',
      edit: '编辑',
      delete: '删除',
      save: '保存更改',
      cancel: '取消',
      clearAll: '清空全部',
      search: '搜索',
      filter: '筛选',
      export: '导出',
      view: '查看',
      status: '状态',
      latency: '延迟',
      apiKey: 'API 密钥',
      keys: '密钥',
      timeout: '超时',
      details: '详情',
      confirmDelete: '确认删除？此操作无法撤销。',
      active: '正常',
      inactive: '停用',
      enabled: '已开启',
      disabled: '已关闭',
      degraded: '降级',
      logout: '退出登录',
      copy: '复制',
      close: '关闭',
      success: '成功',
      fail: '失败'
    },
    nav: {
      dashboard: '仪表盘',
      channels: '供应商管理',
      groups: '分组管理',
      pricing: '价格管理',
      logs: '日志系统',
      settings: '设置中心',
      systemHealthy: '系统正常',
    },
    dashboard: {
      title: '仪表盘概览',
      subtitle: 'LLM 网关实时监控',
      totalRequests: '总请求数',
      totalCost: '预估总费用',
      avgLatency: '平均延迟',
      successRate: '成功率',
      traffic: '请求流量 (24小时)',
      tokenUsage: '模型 Token 使用分布',
      recentActivity: '近期 API 调用',
      viewLogs: '查看所有日志',
      viewChannels: '管理供应商',
      table: {
        timestamp: '时间戳',
        model: '模型',
        status: '状态',
        latency: '延迟',
        cost: '费用',
      },
      ranking: {
        title: '供应商统计排名',
        options: {
          calls: '调用次数',
          cost: '预估费用',
          latency: '平均延迟',
          successRate: '成功率',
        },
        columns: {
           rank: '排名',
           channel: '供应商名称',
           status: '状态'
        },
        status: {
           observation: '观测中',
           normal: '正常',
           active: '活跃',
           excellent: '优秀',
           slow: '偏慢',
           abnormal: '异常',
           volatile: '波动'
        }
      }
    },
    channels: {
      title: '供应商管理',
      subtitle: '管理上游供应商及 API 密钥',
      addChannel: '添加供应商',
      provider: '供应商',
      baseUrl: 'API 地址',
      modal: {
        titleAdd: '添加供应商',
        titleEdit: '编辑供应商',
        name: '名称',
        type: '类型',
        baseUrl: 'API 基础地址',
        apiKey: 'API 密钥',
        apiKeyPlaceholder: '请输入 API 密钥 (sk-...)',
        models: '可用模型',
        modelsPlaceholder: 'gpt-4, gpt-3.5-turbo (逗号分隔)',
        status: '当前状态',
        autoSync: '模型自动同步'
      },
      more: {
        testConnection: '测试连接',
        syncModels: '同步模型列表'
      },
      validation: {
        name: '请输入供应商名称',
        baseUrl: '请输入API基础地址',
        apiKey: '请输入API密钥',
        models: '请至少输入一个模型名称'
      }
    },
    groups: {
      title: '分组与路由',
      subtitle: '配置负载均衡策略与路由规则',
      createGroup: '新建分组',
      activeChannels: '活跃供应商',
      modal: {
        titleAdd: '新建分组',
        titleEdit: '编辑分组',
        name: '分组名称',
        mode: '负载均衡模式',
        timeout: '超时设置 (ms)',
        selectedProviders: '包含的模型与供应商',
        selectPlaceholder: '请选择...',
        searchModels: '搜索模型...',
        viewSelected: '仅查看已选',
        invalidSelections: '无效或缺失的模型选择'
      },
      modes: {
        roundRobin: '轮询 (Round Robin)',
        random: '随机 (Random)',
        failover: '故障转移 (Failover)',
        weighted: '加权 (Weighted)',
        sapr: '自适应 (SAPR)'
      }
    },
    pricing: {
      title: '模型价格',
      subtitle: '仅显示分组内使用的模型，自动从 models.dev 同步模型价格数据。',
      searchPlaceholder: '搜索模型名称...',
      sync: '同步上游模型',
      syncSuccess: '模型同步成功',
      syncFail: '模型同步失败',
      table: {
        modelName: '模型名称',
        provider: '供应商',
        inputPrice: '输入价格 (1M)',
        outputPrice: '输出价格 (1M)',
        context: '上下文 / 输出限制',
        capabilities: '能力',
        updated: '更新时间',
        inputType: '输入类型'
      },
      capabilities: {
        reasoning: '推理',
        toolCall: '工具调用'
      }
    },
    logs: {
      title: '请求日志',
      subtitle: '搜索与分析 API 请求历史',
      searchPlaceholder: '搜索请求 ID、模型或路径...',
      autoRefresh: '自动刷新',
      refreshInterval: '刷新间隔',
      seconds: '秒',
      table: {
        status: '状态',
        time: '时间',
        model: '模型',
        provider: '供应商',
        latency: '延迟',
        tokens: 'Token数',
        cost: '费用',
      },
      pagination: {
        showing: '显示',
        to: '到',
        of: '共',
        results: '条结果',
        prev: '上一页',
        next: '下一页',
      },
      detail: {
        title: '请求详情',
        info: '基本信息',
        performance: '性能与费用',
        content: '请求内容',
        responseContent: '响应内容',
        error: '错误信息',
        requestId: '请求 ID',
        provider: '供应商',
        type: '请求类型',
        stream: '流式响应',
        created: '创建时间',
        retry: '重试次数',
        duration: '总耗时'
      }
    },
    settings: {
      title: '设置中心',
      subtitle: '系统配置与偏好设置',
      language: '语言设置',
      languageDesc: '选择系统显示语言',
      flushNow: '立即刷新',
      security: '客户端访问令牌',
      securityDesc: '管理应用程序用于通过 Lumina 进行身份验证的令牌。',
      manageTokens: '管理令牌',
      appearance: '外观设置',
      appearanceDesc: '自定义仪表盘的外观风格。',
      light: '浅色',
      dark: '深色',
      account: '账号设置',
      accountDesc: '更新管理员用户名和密码。修改后需要重新登录。',
      username: '用户名',
      originalPassword: '原密码',
      password: '新密码',
      confirmPassword: '确认新密码',
      leaveBlank: '若不修改请留空',
      updateSuccess: '账号信息更新成功，即将跳转登录页...',
      passwordsDoNotMatch: '两次输入的密码不一致',
      update: '更新账号信息',
      tokens: {
        title: 'API 令牌管理',
        create: '创建新令牌',
        name: '令牌名称',
        key: '令牌密钥',
        created: '创建时间',
        lastUsed: '最后使用',
        empty: '暂无令牌',
        copied: '已复制',
        copyWarning: '请务必立即复制此令牌。出于安全原因，它将不会再次显示。',
        confirmRevoke: '确定要撤销此令牌吗？任何使用此令牌的应用程序将立即无法访问。'
      }
    }
  },
  en: {
    common: {
      add: 'Add',
      edit: 'Edit',
      delete: 'Delete',
      save: 'Save Changes',
      cancel: 'Cancel',
      clearAll: 'Clear All',
      search: 'Search',
      filter: 'Filter',
      export: 'Export',
      view: 'View',
      status: 'Status',
      latency: 'Latency',
      apiKey: 'API Key',
      keys: 'Keys',
      timeout: 'timeout',
      details: 'Details',
      confirmDelete: 'Are you sure you want to delete this provider? This action cannot be undone.',
      active: 'Active',
      inactive: 'Inactive',
      enabled: 'Enabled',
      disabled: 'Disabled',
      degraded: 'Degraded',
      logout: 'Logout',
      copy: 'Copy',
      close: 'Close',
      success: 'Success',
      fail: 'Failure'
    },
    nav: {
      dashboard: 'Dashboard',
      channels: 'Providers',
      groups: 'Groups',
      pricing: 'Pricing',
      logs: 'Logs',
      settings: 'Settings',
      systemHealthy: 'System Healthy',
    },
    dashboard: {
      title: 'Dashboard Overview',
      subtitle: 'Real-time monitoring of your LLM gateway.',
      totalRequests: 'Total Requests',
      totalCost: 'Total Cost (Est.)',
      avgLatency: 'Avg. Latency',
      successRate: 'Success Rate',
      traffic: 'Request Traffic (24h)',
      tokenUsage: 'Token Usage by Model',
      recentActivity: 'Recent API Calls',
      viewLogs: 'View All Logs',
      viewChannels: 'Manage Providers',
      table: {
        timestamp: 'Timestamp',
        model: 'Model',
        status: 'Status',
        latency: 'Latency',
        cost: 'Cost',
      },
      ranking: {
        title: 'Provider Statistics Ranking',
        options: {
          calls: 'Calls',
          cost: 'Est. Cost',
          latency: 'Avg. Latency',
          successRate: 'Success Rate',
        },
        columns: {
           rank: 'Rank',
           channel: 'Provider Name',
           status: 'Status'
        },
        status: {
           observation: 'Observation',
           normal: 'Normal',
           active: 'Active',
           excellent: 'Excellent',
           slow: 'Slow',
           abnormal: 'Abnormal',
           volatile: 'Volatile'
        }
      }
    },
    channels: {
      title: 'Providers',
      subtitle: 'Manage upstream providers and API keys.',
      addChannel: 'Add Provider',
      provider: 'Provider',
      baseUrl: 'Base URL',
      modal: {
        titleAdd: 'Add Provider',
        titleEdit: 'Edit Provider',
        name: 'Name',
        type: 'Type',
        baseUrl: 'Base URL',
        apiKey: 'API Key',
        apiKeyPlaceholder: 'Enter API Key (sk-...)',
        models: 'Available Models',
        modelsPlaceholder: 'gpt-4, gpt-3.5-turbo (comma separated)',
        status: 'Status',
        autoSync: 'Model Auto Sync'
      },
      more: {
        testConnection: 'Test Connection',
        syncModels: 'Sync Models'
      },
      validation: {
        name: 'Provider name is required',
        baseUrl: 'Base URL is required',
        apiKey: 'API Key is required',
        models: 'At least one model is required'
      }
    },
    groups: {
      title: 'Groups & Routing',
      subtitle: 'Configure load balancing strategies and routing rules.',
      createGroup: 'Create Group',
      activeChannels: 'Active Providers',
      modal: {
        titleAdd: 'Create Group',
        titleEdit: 'Edit Group',
        name: 'Group Name',
        mode: 'Load Balance Mode',
        timeout: 'Timeout (ms)',
        selectedProviders: 'Included Models & Providers',
        selectPlaceholder: 'Select...',
        searchModels: 'Search models...',
        viewSelected: 'View Selected',
        invalidSelections: 'Invalid / Missing Selections'
      },
      modes: {
        roundRobin: 'Round Robin',
        random: 'Random',
        failover: 'Failover',
        weighted: 'Weighted',
        sapr: 'SAPR'
      }
    },
    pricing: {
      title: 'Model Pricing',
      subtitle: 'Automatically sync model pricing data from models.dev.',
      searchPlaceholder: 'Search model name...',
      sync: 'Sync Upstream Models',
      syncSuccess: 'Models synced successfully',
      syncFail: 'Failed to sync models',
      table: {
        modelName: 'Model Name',
        provider: 'Provider',
        inputPrice: 'Input Price (1M)',
        outputPrice: 'Output Price (1M)',
        context: 'Context / Output Limit',
        capabilities: 'Capabilities',
        updated: 'Updated',
        inputType: 'Input Type'
      },
      capabilities: {
        reasoning: 'Reasoning',
        toolCall: 'Tool Call'
      }
    },
    logs: {
      title: 'Request Logs',
      subtitle: 'Search and analyze API request history.',
      searchPlaceholder: 'Search by Request ID, Model, or Path...',
      autoRefresh: 'Auto Refresh',
      refreshInterval: 'Interval',
      seconds: 's',
      table: {
        status: 'Status',
        time: 'Time',
        model: 'Model',
        provider: 'Provider',
        latency: 'Latency',
        tokens: 'Tokens',
        cost: 'Cost',
      },
      pagination: {
        showing: 'Showing',
        to: 'to',
        of: 'of',
        results: 'results',
        prev: 'Previous',
        next: 'Next',
      },
      detail: {
        title: 'Request Detail',
        info: 'Basic Info',
        performance: 'Performance & Cost',
        content: 'Request Content',
        responseContent: 'Response Content',
        error: 'Error Message',
        requestId: 'Request ID',
        provider: 'Provider',
        type: 'Type',
        stream: 'Stream',
        created: 'Created At',
        retry: 'Retry Count',
        duration: 'Duration'
      }
    },
    settings: {
      title: 'Settings',
      subtitle: 'System configuration and preferences.',
      language: 'Language',
      languageDesc: 'Select system display language.',
      flushNow: 'Flush Now',
      security: 'Client Access Tokens',
      securityDesc: 'Manage tokens used by your applications to authenticate with Lumina.',
      manageTokens: 'Manage Tokens',
      appearance: 'Appearance',
      appearanceDesc: 'Customize the look and feel of the dashboard.',
      light: 'Light',
      dark: 'Dark',
      account: 'Account Settings',
      accountDesc: 'Update administrator username and password. You will need to login again.',
      username: 'Username',
      originalPassword: 'Original Password',
      password: 'New Password',
      confirmPassword: 'Confirm Password',
      leaveBlank: 'Leave blank to keep current',
      updateSuccess: 'Profile updated successfully. Redirecting to login...',
      passwordsDoNotMatch: 'Passwords do not match',
      update: 'Update Profile',
      tokens: {
        title: 'API Token Management',
        create: 'Create New Token',
        name: 'Token Name',
        key: 'Secret Key',
        created: 'Created',
        lastUsed: 'Last Used',
        empty: 'No tokens found',
        copied: 'Copied',
        copyWarning: 'Make sure to copy this token now. You won’t be able to see it again!',
        confirmRevoke: 'Are you sure you want to revoke this token? Any applications using it will lose access immediately.'
      }
    }
  }
};

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string) => string;
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export const LanguageProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // Default to Chinese ('zh')
  const [language, setLanguage] = useState<Language>('zh');

  const t = (key: string) => {
    const keys = key.split('.');
    let value: any = translations[language];
    for (const k of keys) {
      if (value && value[k]) {
        value = value[k];
      } else {
        return key; // Return key if translation not found
      }
    }
    return value as string;
  };

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
};

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
};