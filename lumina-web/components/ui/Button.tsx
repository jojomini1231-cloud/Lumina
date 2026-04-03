import React from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const cx = (...parts: Array<string | undefined | false>) => parts.filter(Boolean).join(' ');

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'bg-gray-900 hover:bg-black text-white shadow-sm dark:bg-white dark:hover:bg-gray-100 dark:text-black',
  secondary:
    'bg-white hover:bg-gray-50 text-gray-700 border border-gray-200 shadow-sm dark:bg-gray-800 dark:hover:bg-gray-700 dark:text-gray-200 dark:border-gray-700',
  ghost:
    'bg-transparent hover:bg-gray-100 text-gray-700 dark:text-gray-200 dark:hover:bg-gray-800',
  danger:
    'bg-red-600 hover:bg-red-700 text-white shadow-sm dark:bg-red-600 dark:hover:bg-red-700 dark:text-white',
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'h-9 px-3 text-sm rounded-lg',
  md: 'h-10 px-4 text-sm rounded-xl',
  lg: 'h-11 px-5 text-base rounded-xl',
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'secondary',
      size = 'md',
      loading = false,
      leftIcon,
      rightIcon,
      disabled,
      className,
      children,
      type = 'button',
      ...props
    },
    ref,
  ) => {
    const isDisabled = disabled || loading;

    return (
      <button
        {...props}
        ref={ref}
        type={type}
        disabled={isDisabled}
        className={cx(
          'inline-flex items-center justify-center gap-2 font-semibold transition-all active:scale-[0.98] disabled:opacity-70 disabled:cursor-not-allowed outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/30',
          sizeClasses[size],
          variantClasses[variant],
          className,
        )}
      >
        {leftIcon ? <span className="shrink-0">{leftIcon}</span> : null}
        {children ? <span className="truncate">{children}</span> : null}
        {rightIcon ? <span className="shrink-0">{rightIcon}</span> : null}
      </button>
    );
  },
);

Button.displayName = 'Button';

