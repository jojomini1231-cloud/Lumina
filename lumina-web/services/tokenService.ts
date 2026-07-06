import { api } from '../utils/request';
import { AccessToken } from '../types';

interface ApiKeyDTO {
  id: number;
  name: string;
  keyGroup: string | null;
  apiKey: string;
  isEnabled: boolean;
  expiredAt: number | null;
  maxAmount: number | null;
  maxRequests: number | null;
  maxConcurrentRequests: number | null;
  requestLimitResetAt: number | null;
  supportedModels: string | null;
  createdAt: string;
  updatedAt: string;
}

interface ApiKeyUsageDTO {
  id: number;
  name: string;
  keyGroup: string | null;
  apiKey: string;
  isEnabled: boolean;
  expiredAt: number | null;
  maxAmount: number | null;
  maxRequests: number | null;
  maxConcurrentRequests: number | null;
  requestLimitResetAt: number | null;
  supportedModels: string | null;
  totalRequests: number;
  requestLimitUsed: number;
  successRequests: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalCost: number;
}

interface PageDTO<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}

const mapApiKeyToToken = (item: ApiKeyDTO | ApiKeyUsageDTO): AccessToken => ({
  id: String(item.id),
  name: item.name,
  keyGroup: item.keyGroup || '自用',
  token: item.apiKey,
  maskedToken: item.apiKey
    ? `${item.apiKey.substring(0, 3)}...${item.apiKey.substring(item.apiKey.length - 4)}`
    : '******',
  createdAt: 'createdAt' in item ? item.createdAt || '' : '',
  status: item.isEnabled ? 'active' as const : 'revoked' as const,
  expiredAt: item.expiredAt,
  maxAmount: item.maxAmount,
  maxRequests: item.maxRequests,
  maxConcurrentRequests: item.maxConcurrentRequests,
  requestLimitResetAt: item.requestLimitResetAt,
  supportedModels: parseSupportedModels(item.supportedModels),
  totalRequests: 'totalRequests' in item ? item.totalRequests : undefined,
  requestLimitUsed: 'requestLimitUsed' in item ? item.requestLimitUsed : undefined,
  successRequests: 'successRequests' in item ? item.successRequests : undefined,
  totalInputTokens: 'totalInputTokens' in item ? item.totalInputTokens : undefined,
  totalOutputTokens: 'totalOutputTokens' in item ? item.totalOutputTokens : undefined,
  totalCost: 'totalCost' in item ? item.totalCost : undefined,
});

const parseSupportedModels = (value: string | null): string[] => {
  if (!value) return [];
  return value.split(',').map((item) => item.trim()).filter(Boolean);
};

export const tokenService = {
  // Fetch list of tokens with usage stats
  async getList(): Promise<AccessToken[]> {
    const response = await api.get<any>('/api-keys/usage');

    if (response.code === 200) {
      let items: ApiKeyUsageDTO[] = [];

      if (Array.isArray(response.data)) {
        items = response.data;
      } else if (response.data) {
        items = [response.data];
      }

      return items.map(mapApiKeyToToken);
    }
    return [];
  },

  async getPage(current: number, size: number, isEnabled: boolean, keyGroup?: string, keyword?: string): Promise<{ items: AccessToken[]; total: number; current: number; size: number }> {
    const params = new URLSearchParams({
      current: String(current),
      size: String(size),
      isEnabled: String(isEnabled),
    });
    if (keyGroup) {
      params.set('keyGroup', keyGroup);
    }
    if (keyword?.trim()) {
      params.set('keyword', keyword.trim());
    }
    const response = await api.get<any>(`/api-keys/usage/page?${params.toString()}`);

    if (response.code === 200 && response.data) {
      const page = response.data as PageDTO<ApiKeyUsageDTO>;
      return {
        items: (page.records || []).map(mapApiKeyToToken),
        total: page.total || 0,
        current: page.current || current,
        size: page.size || size,
      };
    }

    return { items: [], total: 0, current, size };
  },

  // Create a new token
  async create(name: string, keyGroup: string): Promise<AccessToken> {
    const response = await api.post<any>('/api-keys/generate', { name, keyGroup });

    if (response.code === 200 && response.data) {
      const item = response.data as ApiKeyDTO;
      return mapApiKeyToToken(item);
    }
    throw new Error(response.message || 'Failed to create token');
  },

  // Toggle enable/disable a token
  async toggle(id: string): Promise<AccessToken> {
    const response = await api.put<any>(`/api-keys/${id}/toggle`);
    if (response.code === 200 && response.data) {
      const item = response.data as ApiKeyDTO;
      return mapApiKeyToToken(item);
    }
    throw new Error(response.message || 'Failed to toggle token');
  },

  // Update token spending quota. null means unlimited.
  async updateQuota(id: string, maxAmount: number | null, maxRequests: number | null, maxConcurrentRequests: number | null, supportedModels: string[], keyGroup: string): Promise<AccessToken> {
    const response = await api.put<any>(`/api-keys/${id}/quota`, { maxAmount, maxRequests, maxConcurrentRequests, supportedModels, keyGroup });
    if (response.code === 200 && response.data) {
      const item = response.data as ApiKeyDTO;
      return mapApiKeyToToken(item);
    }
    throw new Error(response.message || 'Failed to update token quota');
  },

  async getGroups(isEnabled?: boolean): Promise<string[]> {
    const query = typeof isEnabled === 'boolean' ? `?isEnabled=${isEnabled}` : '';
    const response = await api.get<any>(`/api-keys/groups${query}`);
    if (response.code === 200 && Array.isArray(response.data)) {
      return response.data;
    }
    return [];
  },

  async resetRequestLimit(id: string): Promise<AccessToken> {
    const response = await api.put<any>(`/api-keys/${id}/request-limit/reset`);
    if (response.code === 200 && response.data) {
      return mapApiKeyToToken(response.data as ApiKeyDTO);
    }
    throw new Error(response.message || 'Failed to reset request limit');
  },

  // Delete/Revoke a token
  async delete(id: string): Promise<void> {
    const response = await api.delete<any>(`/api-keys/${id}`);
    if (response.code !== 200) {
        throw new Error(response.message || 'Failed to delete token');
    }
  }
};
