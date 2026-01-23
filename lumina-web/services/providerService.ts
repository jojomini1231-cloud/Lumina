import { api } from '../utils/request';
import { Channel, ProviderType } from '../types';

interface ProviderDTO {
  id: number;
  name: string;
  type: number;
  isEnabled: boolean;
  baseUrl: string;
  modelName: string;
  autoSync: boolean;
  apiKey?: string;
  createdAt?: string;
  updatedAt?: string;
}

export const providerService = {
  // Fetch paginated list of providers
  async getList(current = 1, size = 100): Promise<Channel[]> {
    const response = await api.get<any>('/providers/page', { params: { current, size } });
    
    if (response.code === 200 && response.data && Array.isArray(response.data.records)) {
      return response.data.records.map((item: any) => ({
        id: String(item.id),
        name: item.name,
        type: item.type as ProviderType,
        baseUrl: item.baseUrl,
        // Map directly to string, default to empty
        apiKey: item.apiKey || '', 
        models: item.modelName ? item.modelName.split(',') : [],
        latency: 0, // Latency is not provided in the basic CRUD API
        status: item.isEnabled ? 'active' : 'inactive',
        autoSync: item.autoSync
      }));
    }
    return [];
  },

  // Create a new provider
  async create(channel: Partial<Channel>): Promise<any> {
    const data = {
      name: channel.name,
      type: channel.type,
      isEnabled: channel.status === 'active' ? 1 : 0,
      baseUrl: channel.baseUrl,
      modelName: channel.models?.join(','),
      autoSync: channel.autoSync ? 1 : 0,
      apiKey: channel.apiKey // Send as string
    };
    return api.post('/providers', data);
  },

  // Update an existing provider
  async update(id: string, channel: Partial<Channel>): Promise<any> {
    const data = {
      name: channel.name,
      type: channel.type,
      isEnabled: channel.status === 'active' ? 1 : 0,
      baseUrl: channel.baseUrl,
      modelName: channel.models?.join(','),
      autoSync: channel.autoSync ? 1 : 0,
      apiKey: channel.apiKey // Send as string
    };
    return api.put(`/providers/${id}`, data);
  },

  // Delete a provider
  async delete(id: string): Promise<any> {
    return api.delete(`/providers/${id}`);
  },

  // Sync models from provider
  async syncModels(baseUrl: string, apiKey: string): Promise<string[]> {
    const response = await api.post<any>('/providers/models', { baseUrl, apiKey });
    if (response.code === 200 && Array.isArray(response.data)) {
      return response.data;
    }
    throw new Error(response.message || 'Failed to sync models');
  }
};