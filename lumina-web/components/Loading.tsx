import React from 'react';
import { Loader2, Loader, Zap, Activity } from 'lucide-react';

// Spinner loader - classic rotating spinner
export const LoadingSpinner: React.FC<{
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}> = ({ size = 'md', className = '' }) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8',
    xl: 'w-12 h-12'
  };
  return (
    <Loader2 className={`${sizeClasses[size]} text-indigo-600 animate-spin ${className}`} />
  );
};

// Dots loader - animated bouncing dots
export const LoadingDots: React.FC<{
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}> = ({ size = 'md', className = '' }) => {
  const dotSize = {
    sm: 'w-1.5 h-1.5',
    md: 'w-2 h-2',
    lg: 'w-3 h-3'
  };
  return (
    <div className={`flex items-center gap-1 ${className}`}>
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className={`${dotSize[size]} bg-indigo-600 rounded-full animate-bounce`}
          style={{ animationDelay: `${i * 0.15}s`, animationDuration: '0.6s' }}
        />
      ))}
    </div>
  );
};

// Pulse loader - pulsing circle
export const LoadingPulse: React.FC<{
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}> = ({ size = 'md', className = '' }) => {
  const sizeClasses = {
    sm: 'w-6 h-6',
    md: 'w-10 h-10',
    lg: 'w-16 h-16'
  };
  return (
    <div className={`relative ${sizeClasses[size]} ${className}`}>
      <div className="absolute inset-0 bg-indigo-600/30 rounded-full animate-ping" />
      <div className="absolute inset-0 bg-indigo-600/50 rounded-full animate-pulse" />
      <div className="absolute inset-2 bg-indigo-600 rounded-full animate-pulse" />
    </div>
  );
};

// Bar loader - animated progress bars
export const LoadingBar: React.FC<{
  count?: number;
  className?: string;
}> = ({ count = 3, className = '' }) => (
  <div className={`flex items-end gap-1 h-6 ${className}`}>
    {Array.from({ length: count }).map((_, i) => (
      <div
        key={i}
        className="w-1 bg-indigo-600 rounded-full animate-[bounce-soft_1s_ease-in-out_infinite]"
        style={{
          height: '100%',
          animationDelay: `${i * 0.1}s`,
          transformOrigin: 'bottom'
        }}
      />
    ))}
  </div>
);

// Wave loader - wave animation
export const LoadingWave: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`flex items-center gap-0.5 ${className}`}>
    {[0, 1, 2, 3, 4].map((i) => (
      <div
        key={i}
        className="w-1 bg-indigo-600 rounded-full"
        style={{
          height: '20px',
          animation: 'wave 1s ease-in-out infinite',
          animationDelay: `${i * 0.1}s`
        }}
      />
    ))}
  </div>
);

// Full page loader
export const FullPageLoader: React.FC<{
  message?: string;
}> = ({ message = 'Loading...' }) => (
  <div className="h-screen w-screen flex flex-col items-center justify-center bg-slate-50 dark:bg-slate-900">
    <div className="relative">
      {/* Outer ring */}
      <div className="w-16 h-16 border-4 border-indigo-200 dark:border-indigo-900 rounded-full" />
      {/* Inner spinner */}
      <div className="absolute inset-0 w-16 h-16 border-4 border-transparent border-t-indigo-600 rounded-full animate-spin" />
      {/* Center dot */}
      <div className="absolute inset-0 flex items-center justify-center">
        <Zap size={20} className="text-indigo-600 animate-pulse" />
      </div>
    </div>
    <p className="mt-4 text-slate-500 text-sm animate-pulse">{message}</p>
  </div>
);

// Inline loader with text
export const InlineLoader: React.FC<{
  text?: string;
  size?: 'sm' | 'md';
}> = ({ text = 'Loading...', size = 'md' }) => (
  <div className="flex items-center gap-2">
    <LoadingSpinner size={size} />
    <span className="text-slate-500 text-sm">{text}</span>
  </div>
);

// Card loader - loading state for card containers
export const CardLoader: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`flex flex-col items-center justify-center p-12 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 ${className}`}>
    <LoadingPulse size="lg" />
    <p className="mt-4 text-slate-500 text-sm">Loading...</p>
  </div>
);

// Button loader - for buttons with loading state
export const ButtonLoader: React.FC<{
  text?: string;
}> = ({ text = 'Loading...' }) => (
  <div className="flex items-center justify-center gap-2">
    <LoadingSpinner size="sm" />
    <span>{text}</span>
  </div>
);

// Activity loader - animated activity indicator
export const ActivityLoader: React.FC<{
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}> = ({ size = 'md', className = '' }) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-8 h-8'
  };
  return (
    <Activity className={`${sizeClasses[size]} text-indigo-600 animate-pulse-soft ${className}`} />
  );
};

// Three dots loader (horizontal)
export const ThreeDotsLoader: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`flex items-center gap-1 ${className}`}>
    {[0, 1, 2].map((i) => (
      <div
        key={i}
        className="w-2 h-2 bg-indigo-600 rounded-full"
        style={{
          animation: 'threeDots 1.4s infinite ease-in-out both',
          animationDelay: `${i * 0.16}s`
        }}
      />
    ))}
  </div>
);

// Brand loader - animated logo loader
export const BrandLoader: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`flex items-center gap-3 ${className}`}>
    <div className="relative">
      <div className="w-10 h-10 bg-indigo-600 rounded-lg flex items-center justify-center">
        <Zap size={20} className="text-white animate-pulse" />
      </div>
      <div className="absolute inset-0 bg-indigo-600 rounded-lg animate-ping opacity-20" />
    </div>
    <div className="flex flex-col">
      <span className="text-lg font-bold text-slate-900 dark:text-white">Lumina</span>
      <LoadingDots size="sm" className="w-12" />
    </div>
  </div>
);

// Ring loader - animated ring
export const RingLoader: React.FC<{
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}> = ({ size = 'md', className = '' }) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12'
  };
  return (
    <Loader className={`${sizeClasses[size]} text-indigo-600 animate-spin ${className}`} />
  );
};