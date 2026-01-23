import { api } from '../utils/request';
import { DashboardOverview } from '../types';

export interface TrafficDataPoint {
  hour: number;
  requestCount: number;
  timestamp: number;
}

export interface ModelTokenUsageData {
  modelName: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  requestCount: number;
  percentage: number;
}

export interface ProviderStats {
  rank: number;
  providerId: number;
  providerName: string;
  callCount: number;
  estimatedCost: number;
  avgLatency: number;
  successRate: number;
}

export const dashboardService = {
  async getOverview(): Promise<DashboardOverview> {
    const response = await api.get<any>('/dashboard/overview');
    if (response.code === 200 && response.data) {
      return response.data;
    }
    // Return safe default if request fails
    return {
        totalRequests: 0,
        requestGrowthRate: 0,
        totalCost: 0,
        costGrowthRate: 0,
        avgLatency: 0,
        latencyChange: 0,
        successRate: 0,
        successRateChange: 0
    };
  },

  async getTraffic(): Promise<TrafficDataPoint[]> {
    const response = await api.get<any>('/dashboard/traffic');
    if (response.code === 200 && Array.isArray(response.data)) {
      return response.data;
    }
    return [];
  },

  async getModelTokenUsage(): Promise<ModelTokenUsageData[]> {
    const response = await api.get<any>('/dashboard/model-token-usage');
    if (response.code === 200 && Array.isArray(response.data)) {
      return response.data;
    }
    return [];
  },

  async getProviderStats(): Promise<ProviderStats[]> {
    const response = await api.get<any>('/dashboard/provider-stats');
    if (response.code === 200 && Array.isArray(response.data)) {
      return response.data;
    }
    return [];
  }
};