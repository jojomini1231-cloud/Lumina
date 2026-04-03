import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { CheckCircle2, AlertCircle, Activity, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const useToast = (): ToastContextType => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

interface ToastProviderProps {
  children: ReactNode;
}

export const ToastProvider: React.FC<ToastProviderProps> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const showToast = useCallback((message: string, type: ToastType = 'success') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, message, type }]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 3000);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {/* Toast Container */}
      <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={`pointer-events-auto px-4 py-3 rounded-xl shadow-float border flex items-center justify-between min-w-[300px] max-w-md animate-in slide-in-from-right duration-300 backdrop-blur-md ${
              toast.type === 'success'
                ? 'bg-white border-green-200 text-green-700 dark:bg-[#1a1a1a] dark:border-green-900/50 dark:text-green-400'
                : toast.type === 'error'
                ? 'bg-white border-red-200 text-red-700 dark:bg-[#1a1a1a] dark:border-red-900/50 dark:text-red-400'
                : 'bg-white border-blue-200 text-blue-700 dark:bg-[#1a1a1a] dark:border-blue-900/50 dark:text-blue-400'
            }`}
          >
            <div className="flex items-center">
              {toast.type === 'success' ? (
                <CheckCircle2 size={18} className="mr-2 flex-shrink-0" />
              ) : toast.type === 'error' ? (
                <AlertCircle size={18} className="mr-2 flex-shrink-0" />
              ) : (
                <Activity size={18} className="mr-2 flex-shrink-0" />
              )}
              <span className="text-sm font-medium leading-tight">{toast.message}</span>
            </div>
            <button
              onClick={() => removeToast(toast.id)}
              className="ml-4 text-current opacity-60 hover:opacity-100 transition-opacity focus:outline-none"
            >
              <X size={16} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};
