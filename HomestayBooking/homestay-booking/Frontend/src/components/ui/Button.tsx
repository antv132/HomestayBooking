import { ButtonHTMLAttributes, ReactNode } from 'react';

// Button variants aligned with component-library.md
// button-primary: bg-primary-base (#0F766E), text-inverted, radius-md
// button-secondary: bg-transparent, border border-base, text-primary
// button-ghost: no bg, no border
// button-success / button-danger: semantic actions
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'success' | 'danger';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: 'sm' | 'md' | 'lg';
  fullWidth?: boolean;
  isLoading?: boolean;
  children: ReactNode;
}

const variantClasses: Record<ButtonVariant, string> = {
  // button-primary: teal bg, white text, radius-md, no border
  primary: `
    bg-[#0F766E] text-white border-none
    hover:bg-[#0D9488] hover:-translate-y-px
    disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
    focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[#CCFBF1]
  `,
  // button-secondary: transparent bg, border-base, text-primary
  secondary: `
    bg-transparent text-[#1E293B] border border-[#E2E8F0]
    hover:bg-[#F8FAFC]
    disabled:opacity-50 disabled:cursor-not-allowed
    focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[#CCFBF1]
  `,
  // button-ghost: no bg, no border
  ghost: `
    bg-transparent text-[#475569] border-none
    hover:bg-[#F8FAFC] hover:text-[#1E293B]
    disabled:opacity-50 disabled:cursor-not-allowed
    focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[#CCFBF1]
  `,
  // button-success: explicit approvals (green)
  success: `
    bg-[#10B981] text-white border-none
    hover:bg-[#059669] hover:-translate-y-px
    disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
    focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[#CCFBF1]
  `,
  // button-danger: destructive actions (red)
  danger: `
    bg-[#EF4444] text-white border-none
    hover:bg-[#DC2626] hover:-translate-y-px
    disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
    focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-[#CCFBF1]
  `,
};

const sizeClasses = {
  sm: 'h-9 px-4 text-sm',
  md: 'h-11 px-6 text-base',  // 44px height per component-library.md
  lg: 'h-12 px-8 text-base',
};

export default function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  isLoading = false,
  className = '',
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`
        inline-flex items-center justify-center gap-2
        font-semibold rounded-md
        transition-all duration-150 select-none cursor-pointer whitespace-nowrap
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${fullWidth ? 'w-full' : ''}
        ${className}
      `}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && (
        <svg
          className="animate-spin -ml-1 mr-2 h-4 w-4 text-current flex-shrink-0"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
      {children}
    </button>
  );
}
