import { api } from '../utils/request';
import { LogEntry } from '../types';

export interface LogDTO {
  id: string;
  requestTime: number; // Seconds timestamp
  requestModelName: string;
  actualModelName?: string;
  inputTokens: number;
  outputTokens: number;
  firstTokenMs: number;
  retryCount: number;
  status: string; // SUCCESS or FAIL
  cost: number; // Added cost field
  providerName: string;
}

export interface LogDetail {
  id: string;
  requestId: string;
  requestTime: number;
  requestType: string;
  requestModelName: string;
  actualModelName?: string;
  providerId: number;
  providerName: string;
  isStream: boolean;
  inputTokens: number;
  outputTokens: number;
  firstTokenTime: number;
  firstTokenMs: number;
  totalTimeMs: number; // Changed from totalTime to totalTimeMs
  cost: number;
  status: string;
  retryCount: number;
  requestContent: string;
  responseContent?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface LogPageResponse {
  records: LogEntry[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export const logService = {
  // Fetch paginated list of logs
  async getPage(current = 1, size = 10): Promise<LogPageResponse> {
    const response = await api.get<any>('/request-logs/page', { params: { current, size } });
    
    if (response.code === 200 && response.data && Array.isArray(response.data.records)) {
      const records = response.data.records.map((item: LogDTO) => ({
        id: String(item.id),
        timestamp: new Date(item.requestTime * 1000).toLocaleString(),
        method: 'POST', // Default as usually it's chat completions
        path: '/v1/chat/completions', // Default
        status: item.status, // Use status directly from API (SUCCESS/FAIL)
        latency: item.firstTokenMs,
        requestModel: item.requestModelName || 'Unknown',
        actualModel: item.actualModelName || '-',
        tokens: (item.inputTokens || 0) + (item.outputTokens || 0),
        cost: item.cost || 0, // Map cost from API response
        providerName: item.providerName || '-'
      }));

      return {
        records,
        total: response.data.total,
        size: response.data.size,
        current: response.data.current,
        pages: response.data.pages
      };
    }
    
    return {
      records: [],
      total: 0,
      size,
      current: 1,
      pages: 0
    };
  },

  // Fetch log detail
  async getDetail(id: string): Promise<LogDetail> {
    const response = await api.get<any>(`/request-logs/${id}`);
    if (response.code === 200 && response.data) {
        return {
            ...response.data,
            id: String(response.data.id)
        };
    }
    throw new Error(response.message || 'Failed to load log details');
  }
};