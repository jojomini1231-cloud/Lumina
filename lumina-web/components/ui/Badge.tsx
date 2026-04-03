import React from 'react';

type BadgeTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'purple';

type BadgeSize = 'xs' | 'sm' | 'md';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone;
  size?: BadgeSize;
}

const cx = (...parts: Array<string | undefined | false>) => parts.filter(Boolean).join(' ');

const toneClasses: Record<BadgeTone, string> = {
  neutral: 'bg-gray-100 text-gray-700 border-gray-200 dark:bg-gray-800/60 dark:text-gray-300 dark:border-gray-700',
  info: 'bg-indigo-100 text-indigo-700 border-indigo-200 dark:bg-indigo-900/20 dark:text-indigo-300 dark:border-indigo-800/50',
  success: 'bg-emerald-100 text-emerald-700 border-emerald-200 dark:bg-emerald-900/20 dark:text-emerald-300 dark:border-emerald-800/50',
  warning: 'bg-amber-100 text-amber-700 border-amber-200 dark:bg-amber-900/20 dark:text-amber-300 dark:border-amber-800/50',
  danger: 'bg-red-100 text-red-700 border-red-200 dark:bg-red-900/20 dark:text-red-300 dark:border-red-800/50',
  purple: 'bg-purple-100 text-purple-700 border-purple-200 dark:bg-purple-900/20 dark:text-purple-300 dark:border-purple-800/50',
};

const sizeClasses: Record<BadgeSize, string> = {
  xs: 'px-1.5 py-0.5 text-[10px] rounded-md',
  sm: 'px-2.5 py-0.5 text-xs rounded-full',
  md: 'px-3 py-1 text-sm rounded-full',
};

export const Badge: React.FC<BadgeProps> = ({ tone = 'neutral', size = 'sm', className, ...props }) => {
  return (
    <span
      {...props}
      className={cx(
        'inline-flex items-center font-bold uppercase tracking-wide border',
        toneClasses[tone],
        sizeClasses[size],
        className,
      )}
    />
  );
};

