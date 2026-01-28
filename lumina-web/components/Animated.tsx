import React from 'react';

// Staggered animation wrapper for list items
interface StaggeredListProps {
  children: React.ReactNode;
  delay?: number; // Delay between items in ms
  className?: string;
  animation?: 'fade' | 'slide' | 'zoom' | 'scale';
}

export const StaggeredList: React.FC<StaggeredListProps> = ({
  children,
  delay = 50,
  className = '',
  animation = 'fade'
}) => {
  const animationClasses = {
    fade: 'animate-in fade-in',
    slide: 'animate-in slide-in-from-bottom',
    zoom: 'animate-in zoom-in-95',
    scale: 'animate-in scale-in'
  };

  return (
    <div className={className}>
      {React.Children.map(children, (child, index) => {
        if (React.isValidElement(child)) {
          return React.cloneElement(child as React.ReactElement<any>, {
            style: {
              animationDelay: `${index * delay}ms`,
              animationDuration: '300ms',
              ...((child as React.ReactElement<any>).props.style || {})
            },
            className: `${animationClasses[animation]} duration-300 ease-out-smooth ${
              (child as React.ReactElement<any>).props.className || ''
            }`
          });
        }
        return child;
      })}
    </div>
  );
};

// Individual staggered item component
interface StaggeredItemProps {
  children: React.ReactNode;
  delay?: number;
  animation?: 'fade' | 'slide' | 'zoom' | 'scale';
  className?: string;
}

export const StaggeredItem: React.FC<StaggeredItemProps> = ({
  children,
  delay = 0,
  animation = 'fade',
  className = ''
}) => {
  const animationClasses = {
    fade: 'animate-in fade-in',
    slide: 'animate-in slide-in-from-bottom',
    zoom: 'animate-in zoom-in-95',
    scale: 'animate-in scale-in'
  };

  return (
    <div
      className={`${animationClasses[animation]} duration-300 ease-out-smooth ${className}`}
      style={{ animationDelay: `${delay}ms` }}
    >
      {children}
    </div>
  );
};

// Fade in staggered animation
export const FadeInList: React.FC<{
  children: React.ReactNode;
  delay?: number;
  className?: string;
}> = ({ children, delay = 50, className = '' }) => (
  <StaggeredList children={children} delay={delay} className={className} animation="fade" />
);

// Slide up staggered animation
export const SlideUpList: React.FC<{
  children: React.ReactNode;
  delay?: number;
  className?: string;
}> = ({ children, delay = 50, className = '' }) => (
  <StaggeredList children={children} delay={delay} className={className} animation="slide" />
);

// Zoom in staggered animation
export const ZoomInList: React.FC<{
  children: React.ReactNode;
  delay?: number;
  className?: string;
}> = ({ children, delay = 50, className = '' }) => (
  <StaggeredList children={children} delay={delay} className={className} animation="zoom" />
);

// Table row with staggered animation
export const AnimatedTableRow: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 30, className = '' }) => (
  <tr
    className={`animate-in fade-in slide-in-from-left duration-300 ease-out-smooth hover:bg-slate-50 dark:hover:bg-slate-700/30 transition-colors ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </tr>
);

// Card with staggered animation
export const AnimatedCard: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 50, className = '' }) => (
  <div
    className={`animate-in fade-in zoom-in-95 duration-300 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </div>
);

// Grid item with staggered animation
export const AnimatedGridItem: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 40, className = '' }) => (
  <div
    className={`animate-in fade-in slide-in-from-bottom duration-300 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </div>
);

// Stat card with staggered animation
export const AnimatedStatCard: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 60, className = '' }) => (
  <div
    className={`animate-in fade-in slide-in-from-top duration-400 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </div>
);

// List item with hover animation
export const AnimatedListItem: React.FC<{
  children: React.ReactNode;
  index?: number;
  delay?: number;
  className?: string;
  onClick?: () => void;
}> = ({ children, index = 0, delay = 30, className = '', onClick }) => (
  <div
    className={`animate-in fade-in duration-300 ease-out-smooth group ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
    onClick={onClick}
  >
    {children}
  </div>
);

// Provider card with animation
export const AnimatedProviderCard: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 50, className = '' }) => (
  <div
    className={`bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-4 sm:p-5 shadow-sm hover:shadow-md hover:border-indigo-300 dark:hover:border-indigo-600 transition-all duration-300 animate-in fade-in slide-in-from-left duration-300 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </div>
);

// Group card with animation
export const AnimatedGroupCard: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 50, className = '' }) => (
  <div
    className={`bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm hover:border-indigo-300 dark:hover:border-indigo-600 hover:shadow-md transition-all duration-300 animate-in fade-in zoom-in-95 duration-300 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </div>
);

// Pricing card with animation
export const AnimatedPricingCard: React.FC<{
  children: React.ReactNode;
  index: number;
  delay?: number;
  className?: string;
}> = ({ children, index, delay = 40, className = '' }) => (
  <div
    className={`bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm hover:shadow-md hover:border-indigo-300 dark:hover:border-indigo-600 transition-all duration-300 animate-in fade-in slide-in-from-bottom duration-300 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </div>
);

// Chip/tag with animation
export const AnimatedChip: React.FC<{
  children: React.ReactNode;
  index?: number;
  delay?: number;
  className?: string;
}> = ({ children, index = 0, delay = 20, className = '' }) => (
  <span
    className={`animate-in scale-in duration-200 ease-out-smooth ${className}`}
    style={{ animationDelay: `${index * delay}ms` }}
  >
    {children}
  </span>
);

// Button with click animation
export const AnimatedButton: React.FC<{
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
}> = ({ children, className = '', onClick, disabled, variant = 'primary' }) => {
  const variantClasses = {
    primary: 'bg-indigo-600 hover:bg-indigo-700 text-white',
    secondary: 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-50',
    danger: 'bg-red-600 hover:bg-red-700 text-white'
  };

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`flex items-center justify-center px-4 py-2 rounded-lg font-medium transition-all duration-150 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed ${variantClasses[variant]} ${className}`}
    >
      {children}
    </button>
  );
};

// Icon with hover animation
export const AnimatedIcon: React.FC<{
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  size?: 'sm' | 'md' | 'lg';
}> = ({ children, className = '', onClick, size = 'md' }) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6'
  };

  return (
    <button
      onClick={onClick}
      className={`${sizeClasses[size]} p-1 rounded-md transition-all duration-150 hover:scale-110 hover:bg-slate-100 dark:hover:bg-slate-700 ${className}`}
    >
      {children}
    </button>
  );
};