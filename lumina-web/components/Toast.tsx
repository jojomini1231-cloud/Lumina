import React, { useEffect, useState } from 'react';
import { CheckCircle2, AlertCircle, Activity, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastProps {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
  onClose: (id: string) => void;
}

const toastConfig = {
  success: {
    icon: CheckCircle2,
    bgClass: 'bg-white dark:bg-slate-800',
    borderClass: 'border-green-200 dark:border-green-900',
    textClass: 'text-green-700 dark:text-green-400',
    iconBgClass: 'bg-green-100 dark:bg-green-900/30'
  },
  error: {
    icon: AlertCircle,
    bgClass: 'bg-white dark:bg-slate-800',
    borderClass: 'border-red-200 dark:border-red-900',
    textClass: 'text-red-700 dark:text-red-400',
    iconBgClass: 'bg-red-100 dark:bg-red-900/30'
  },
  info: {
    icon: Activity,
    bgClass: 'bg-white dark:bg-slate-800',
    borderClass: 'border-blue-200 dark:border-blue-900',
    textClass: 'text-blue-700 dark:text-blue-400',
    iconBgClass: 'bg-blue-100 dark:bg-blue-900/30'
  },
  warning: {
    icon: AlertCircle,
    bgClass: 'bg-white dark:bg-slate-800',
    borderClass: 'border-amber-200 dark:border-amber-900',
    textClass: 'text-amber-700 dark:text-amber-400',
    iconBgClass: 'bg-amber-100 dark:bg-amber-900/30'
  }
};

export const Toast: React.FC<ToastProps> = ({
  id,
  type,
  message,
  duration = 3000,
  onClose
}) => {
  const [isVisible, setIsVisible] = useState(true);
  const [isExiting, setIsExiting] = useState(false);
  const [progress, setProgress] = useState(100);

  const config = toastConfig[type];
  const Icon = config.icon;

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsExiting(true);
      setTimeout(() => {
        onClose(id);
      }, 200);
    }, duration);

    // Progress bar animation
    const progressInterval = setInterval(() => {
      setProgress((prev) => {
        if (prev <= 0) {
          clearInterval(progressInterval);
          return 0;
        }
        return prev - (100 / (duration / 100));
      });
    }, 100);

    return () => {
      clearTimeout(timer);
      clearInterval(progressInterval);
    };
  }, [id, duration, onClose]);

  const handleClose = () => {
    setIsExiting(true);
    setTimeout(() => {
      onClose(id);
    }, 200);
  };

  return (
    <div
      className={`fixed top-4 right-4 z-[100] flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg border ${config.bgClass} ${config.borderClass} ${
        isExiting
          ? 'animate-out slide-out-to-right fade-out duration-200'
          : 'animate-in slide-in-from-right fade-in duration-300 ease-out-smooth'
      }`}
      style={{
        opacity: isExiting ? 0 : 1
      }}
    >
      {/* Icon */}
      <div className={`p-1.5 rounded-lg ${config.iconBgClass}`}>
        <Icon size={18} className={config.textClass} />
      </div>

      {/* Message */}
      <span className="text-sm font-medium text-slate-900 dark:text-slate-100">
        {message}
      </span>

      {/* Close button */}
      <button
        onClick={handleClose}
        className="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors"
        aria-label="Close"
      >
        <X size={16} />
      </button>

      {/* Progress bar */}
      <div className="absolute bottom-0 left-0 h-0.5 bg-slate-200 dark:bg-slate-700 rounded-b-lg overflow-hidden">
        <div
          className={`h-full ${config.textClass.replace('text-', 'bg-').replace('dark:text-', 'dark:bg-')} transition-all duration-100 ease-linear`}
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
};

// Toast container component
interface ToastContainerProps {
  toasts: Array<{
    id: string;
    type: ToastType;
    message: string;
    duration?: number;
  }>;
  onClose: (id: string) => void;
}

export const ToastContainer: React.FC<ToastContainerProps> = ({ toasts, onClose }) => {
  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          id={toast.id}
          type={toast.type}
          message={toast.message}
          duration={toast.duration}
          onClose={onClose}
        />
      ))}
    </div>
  );
};

// Toast hook for easy usage
interface ToastState {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
}

export const useToast = () => {
  const [toasts, setToasts] = useState<ToastState[]>([]);

  const addToast = (message: string, type: ToastType = 'info', duration?: number) => {
    const id = Date.now().toString();
    setToasts((prev) => [...prev, { id, type, message, duration }]);
    return id;
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  const success = (message: string, duration?: number) => addToast(message, 'success', duration);
  const error = (message: string, duration?: number) => addToast(message, 'error', duration);
  const info = (message: string, duration?: number) => addToast(message, 'info', duration);
  const warning = (message: string, duration?: number) => addToast(message, 'warning', duration);

  return {
    toasts,
    addToast,
    removeToast,
    success,
    error,
    info,
    warning
  };
};