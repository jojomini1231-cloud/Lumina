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
  }
};