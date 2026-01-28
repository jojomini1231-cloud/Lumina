import React from 'react';

// Skeleton base component with shimmer animation
const SkeletonBase: React.FC<{
  className?: string;
  children?: React.ReactNode;
}> = ({ className = '', children }) => (
  <div className={`relative overflow-hidden bg-slate-100 dark:bg-slate-800 rounded ${className}`}>
    {/* Shimmer effect */}
    <div className="absolute inset-0 -translate-x-full animate-[shimmer_1.5s_infinite]">
      <div className="w-full h-full bg-gradient-to-r from-transparent via-white/50 to-transparent dark:via-white/10" />
    </div>
    {children}
  </div>
);

// Text skeleton (single line)
export const SkeletonText: React.FC<{
  width?: string;
  height?: string;
  className?: string;
}> = ({ width = '100%', height = '16px', className = '' }) => (
  <SkeletonBase className={`${className}`} style={{ width, height }} />
);

// Text skeleton (multiple lines)
export const SkeletonTextLines: React.FC<{
  lines?: number;
  className?: string;
  lastLineWidth?: string;
}> = ({ lines = 3, className = '', lastLineWidth = '70%' }) => (
  <div className={`space-y-2 ${className}`}>
    {Array.from({ length: lines }).map((_, i) => (
      <SkeletonText
        key={i}
        width={i === lines - 1 ? lastLineWidth : '100%'}
        height="14px"
      />
    ))}
  </div>
);

// Avatar skeleton
export const SkeletonAvatar: React.FC<{
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}> = ({ size = 'md', className = '' }) => {
  const sizeClasses = {
    sm: 'w-8 h-8',
    md: 'w-10 h-10',
    lg: 'w-12 h-12'
  };
  return <SkeletonBase className={`${sizeClasses[size]} rounded-full ${className}`} />;
};

// Card skeleton
export const SkeletonCard: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 p-5 ${className}`}>
    <div className="flex items-start gap-4 mb-4">
      <SkeletonAvatar size="md" />
      <div className="flex-1 space-y-2">
        <SkeletonText width="60%" height="20px" />
        <SkeletonText width="40%" height="14px" />
      </div>
    </div>
    <div className="space-y-2">
      <SkeletonText height="14px" />
      <SkeletonText height="14px" />
      <SkeletonText width="80%" height="14px" />
    </div>
  </div>
);

// Table row skeleton
export const SkeletonTableRow: React.FC<{
  columns?: number;
  className?: string;
}> = ({ columns = 5, className = '' }) => (
  <tr className={`animate-in fade-in duration-300 ${className}`}>
    {Array.from({ length: columns }).map((_, i) => (
      <td key={i} className="px-6 py-4">
        <SkeletonText width={i === 0 ? '60%' : '80%'} height="16px" />
      </td>
    ))}
  </tr>
);

// Stat card skeleton
export const SkeletonStatCard: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`bg-white dark:bg-slate-800 rounded-xl p-6 shadow-sm border border-slate-100 dark:border-slate-700 ${className}`}>
    <div className="flex items-center justify-between mb-4">
      <SkeletonBase className="w-10 h-10 rounded-lg" />
      <SkeletonText width="60px" height="20px" />
    </div>
    <SkeletonText width="40%" height="14px" className="mb-2" />
    <SkeletonText width="80%" height="28px" />
  </div>
);

// Provider card skeleton
export const SkeletonProviderCard: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`bg-white border border-slate-200 rounded-xl p-4 sm:p-5 shadow-sm ${className}`}>
    <div className="flex flex-col sm:flex-row justify-between items-start gap-4">
      <div className="flex items-start gap-3 sm:gap-4 w-full overflow-hidden">
        <SkeletonBase className="w-10 h-10 rounded-lg flex-shrink-0" />
        <div className="flex-1 min-w-0">
          <div className="flex flex-wrap items-center gap-x-3 gap-y-2 mb-1">
            <SkeletonText width="150px" height="20px" />
            <div className="flex items-center space-x-2 pl-2">
              <SkeletonBase className="w-6 h-3 rounded-full" />
              <SkeletonText width="50px" height="12px" />
            </div>
          </div>
          <SkeletonText width="200px" height="14px" className="mb-3" />
          <div className="flex flex-wrap items-center gap-2">
            <SkeletonBase className="w-24 h-6 rounded" />
            <SkeletonBase className="w-20 h-6 rounded" />
            <SkeletonBase className="w-32 h-6 rounded" />
          </div>
        </div>
      </div>
      <div className="flex items-center gap-2 w-full sm:w-auto">
        <SkeletonBase className="flex-1 sm:flex-none h-9 w-20 rounded-lg" />
        <SkeletonBase className="w-9 h-9 rounded-lg" />
        <SkeletonBase className="w-9 h-9 rounded-lg" />
      </div>
    </div>
  </div>
);

// Group card skeleton
export const SkeletonGroupCard: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col ${className}`}>
    <div className="p-6 flex-1">
      <div className="flex justify-between items-start mb-4">
        <SkeletonBase className="w-10 h-10 rounded-lg" />
        <div className="flex space-x-1">
          <SkeletonBase className="w-8 h-8 rounded-lg" />
          <SkeletonBase className="w-8 h-8 rounded-lg" />
        </div>
      </div>
      <SkeletonText width="60%" height="24px" className="mb-4" />
      <div className="flex items-center space-x-2 mb-6">
        <SkeletonBase className="w-20 h-6 rounded-md" />
        <SkeletonBase className="w-16 h-6 rounded" />
      </div>
      <div className="space-y-2">
        <SkeletonText width="30%" height="12px" />
        <div className="space-y-1.5">
          {Array.from({ length: 3 }).map((_, i) => (
            <SkeletonBase key={i} className="h-8 rounded" />
          ))}
        </div>
      </div>
    </div>
    <div className="p-4 bg-slate-50 border-t border-slate-100 rounded-b-xl flex justify-between items-center">
      <SkeletonText width="80px" height="12px" />
      <SkeletonText width="60px" height="12px" />
    </div>
  </div>
);

// Pricing card skeleton
export const SkeletonPricingCard: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`bg-white rounded-xl border border-slate-200 shadow-sm flex flex-col h-full overflow-hidden ${className}`}>
    <div className="p-5 flex flex-col h-full">
      <div className="flex justify-between items-start mb-3">
        <div className="flex-1 pr-2">
          <SkeletonText width="80%" height="20px" className="mb-2" />
          <div className="mt-1.5 flex items-center gap-2">
            <SkeletonBase className="w-12 h-5 rounded" />
            <div className="flex gap-1">
              <SkeletonBase className="w-4 h-4 rounded" />
              <SkeletonBase className="w-4 h-4 rounded" />
            </div>
          </div>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-px bg-slate-100 border border-slate-100 rounded-lg overflow-hidden mb-4">
        <div className="bg-slate-50 p-2.5">
          <SkeletonText width="100%" height="10px" className="mb-1" />
          <SkeletonText width="60%" height="16px" />
          <SkeletonText width="60%" height="10px" />
        </div>
        <div className="bg-slate-50 p-2.5">
          <SkeletonText width="100%" height="10px" className="mb-1" />
          <SkeletonText width="60%" height="16px" />
          <SkeletonText width="60%" height="10px" />
        </div>
      </div>
      <div className="space-y-2 mb-4">
        {Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="flex justify-between items-center">
            <div className="flex items-center">
              <SkeletonBase className="w-4 h-4 rounded mr-1.5" />
              <SkeletonText width="50px" height="14px" />
            </div>
            <SkeletonText width="40px" height="14px" />
          </div>
        ))}
      </div>
      <div className="mt-auto">
        <div className="flex flex-wrap gap-1.5">
          <SkeletonBase className="w-16 h-6 rounded" />
          <SkeletonBase className="w-12 h-6 rounded" />
        </div>
      </div>
    </div>
    <div className="bg-slate-50 px-5 py-2.5 border-t border-slate-100 flex justify-between items-center">
      <SkeletonBase className="w-16 h-4 rounded" />
    </div>
  </div>
);

// Chart skeleton
export const SkeletonChart: React.FC<{
  className?: string;
}> = ({ className = '' }) => (
  <div className={`bg-white dark:bg-slate-800 p-6 rounded-xl shadow-sm border border-slate-100 dark:border-slate-700 ${className}`}>
    <SkeletonText width="40%" height="24px" className="mb-6" />
    <div className="h-72 w-full">
      {/* Chart area skeleton */}
      <div className="h-full flex items-end justify-between gap-2 px-4">
        {Array.from({ length: 12 }).map((_, i) => (
          <SkeletonBase
            key={i}
            className="w-full rounded-t"
            style={{ height: `${30 + Math.random() * 70}%` }}
          />
        ))}
      </div>
    </div>
  </div>
);

// Table skeleton with multiple rows
export const SkeletonTable: React.FC<{
  rows?: number;
  columns?: number;
  className?: string;
}> = ({ rows = 5, columns = 5, className = '' }) => (
  <div className={`bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden ${className}`}>
    <div className="h-12 bg-slate-50 border-b border-slate-200" />
    <table className="w-full">
      <tbody>
        {Array.from({ length: rows }).map((_, i) => (
          <SkeletonTableRow key={i} columns={columns} />
        ))}
      </tbody>
    </table>
  </div>
);