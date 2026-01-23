import { api } from '../utils/request';

export const userService = {
  // Update user profile (username and/or password)
  async updateProfile(data: { username: string; password?: string; originalPassword?: string }): Promise<void> {
    const response = await api.put<any>('/user/profile', data);
    if (response.code !== 200) {
        throw new Error(response.message || 'Failed to update profile');
    }
  }
};