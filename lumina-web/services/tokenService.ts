import { api } from '../utils/request';
import { AccessToken } from '../types';

interface ApiKeyDTO {
  id: number;
  name: string;
  apiKey: string;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export const tokenService = {
  // Fetch list of tokens
  async getList(): Promise<AccessToken[]> {
    const response = await api.get<any>('/api-keys');
    
    if (response.code === 200) {
      // Handle case where data might be null, a single object, or an array
      let items: ApiKeyDTO[] = [];
      
      if (Array.isArray(response.data)) {
        items = response.data;
      } else if (response.data) {
        items = [response.data];
      }

      return items.map((item) => ({
        id: String(item.id),
        name: item.name,
        // Populate token so it can be copied from the list
        token: item.apiKey,
        // Generate a masked token from the real one if present, otherwise placeholders
        maskedToken: item.apiKey 
          ? `${item.apiKey.substring(0, 3)}...${item.apiKey.substring(item.apiKey.length - 4)}` 
          : '******',
        createdAt: item.createdAt,
        status: item.isEnabled ? 'active' : 'revoked',
        lastUsedAt: item.updatedAt
      }));
    }
    return [];
  },

  // Create a new token
  async create(name: string): Promise<AccessToken> {
    const response = await api.post<any>('/api-keys/generate', { name });
    
    if (response.code === 200 && response.data) {
      const item = response.data as ApiKeyDTO;
      return {
        id: String(item.id),
        name: item.name,
        token: item.apiKey, // Expose full token on creation
        maskedToken: item.apiKey 
          ? `${item.apiKey.substring(0, 3)}...${item.apiKey.substring(item.apiKey.length - 4)}` 
          : '******',
        createdAt: item.createdAt,
        status: item.isEnabled ? 'active' : 'revoked',
        lastUsedAt: item.updatedAt
      };
    }
    throw new Error(response.message || 'Failed to create token');
  },

  // Delete/Revoke a token
  async delete(id: string): Promise<void> {
    const response = await api.delete<any>(`/api-keys/${id}`);
    if (response.code !== 200) {
        throw new Error(response.message || 'Failed to delete token');
    }
  }
};