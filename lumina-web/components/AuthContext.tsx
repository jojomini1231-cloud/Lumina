import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '../utils/request';
import { LoginResponse, User } from '../types';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  login: (username: string, passsword: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Check for existing token on mount
  useEffect(() => {
    const token = localStorage.getItem('lumina_token');
    const username = localStorage.getItem('lumina_user');
    
    if (token && username) {
      setIsAuthenticated(true);
      setUser({ 
        username, 
        token, 
        expiresIn: 0 // We don't persist expiration strictly in state for restore
      });
    }
    setIsLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    try {
      // Use skipAuth: true to avoid sending potentially stale/invalid tokens to the login endpoint
      const response = await api.post<LoginResponse>('/auth/login', {
        username,
        password
      }, { skipAuth: true });

      if (response.code === 200) {
        const { token, expiresIn, username: returnedUsername } = response.data;
        
        localStorage.setItem('lumina_token', token);
        localStorage.setItem('lumina_user', returnedUsername);
        
        setIsAuthenticated(true);
        setUser({
          token,
          expiresIn,
          username: returnedUsername
        });
      } else {
        throw new Error(response.message || 'Login failed');
      }
    } catch (error: any) {
      console.error('Login error:', error);
      throw error;
    }
  };

  const logout = async () => {
    try {
        await api.post('/auth/logout');
    } catch (error) {
        console.error("Logout failed:", error);
    } finally {
        localStorage.removeItem('lumina_token');
        localStorage.removeItem('lumina_user');
        setIsAuthenticated(false);
        setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};