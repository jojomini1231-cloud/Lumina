import { api } from '../utils/request';
import { ModelPrice } from '../types';

export interface ModelPageResponse {
  records: ModelPrice[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export const modelService = {
  // Fetch paginated list of models
  async getPage(current = 1, size = 10, keyword = ''): Promise<ModelPageResponse> {
    const params: any = { current, size };
    if (keyword) {
        params.modelName = keyword;
    }
    const response = await api.get<any>('/llm-models/page', { params });
    
    if (response.code === 200 && response.data) {
      return response.data;
    }
    
    return {
      records: [],
      total: 0,
      size,
      current,
      pages: 0
    };
  },

  // Sync models from upstream
  async sync(): Promise<boolean> {
      const response = await api.post<any>('/llm-models/sync');
      return response.code === 200;
  },

  // Get all upstream providers for a model
  async getProviders(modelName: string): Promise<ModelPrice[]> {
      const response = await api.get<any>(`/llm-models/${encodeURIComponent(modelName)}/providers`);
      if (response.code === 200 && response.data) {
          return response.data;
      }
      return [];
  },

  // Set active upstream provider for a model
  async setActiveProvider(modelName: string, provider: string): Promise<boolean> {
      const response = await api.put<any>(`/llm-models/${encodeURIComponent(modelName)}/active-provider`, null, {
          params: { provider }
      });
      return response.code === 200;
  }
};