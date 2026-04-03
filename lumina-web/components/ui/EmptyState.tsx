import React, { ReactNode } from 'react';
import { Button } from './Button';

export interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  actionIcon?: ReactNode;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  actionIcon,
  className = '',
}) => {
  return (
    <div className={`flex flex-col items-center justify-center p-12 text-center ${className}`}>
      {icon && (
        <div className="text-gray-300 dark:text-gray-700 mb-4 bg-gray-50 dark:bg-gray-800/50 p-4 rounded-full">
          {icon}
        </div>
      )}
      <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-2">{title}</h3>
      {description && (
        <p className="text-gray-500 dark:text-gray-400 max-w-sm mb-6 leading-relaxed">
          {description}
        </p>
      )}
      {actionLabel && onAction && (
        <Button
          onClick={onAction}
          variant="primary"
          leftIcon={actionIcon}
          className="mt-2"
        >
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
