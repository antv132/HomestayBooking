import { ReactNode } from 'react';

type AlertVariant = 'error' | 'success' | 'warning' | 'info';

interface AlertProps {
  variant: AlertVariant;
  message: string;
  icon?: ReactNode;
  closeable?: boolean;
  onClose?: () => void;
}

// Per component-library.md:
// Alert: radius-md, background 10% opacity of semantic color, text 100% opacity
// Non-blocking alerts: used for policy notices (SCR-19), VNPay discrepancies (SCR-52)
const variantClasses: Record<AlertVariant, string> = {
  error:   'bg-[rgba(239,68,68,0.08)]   border border-[rgba(239,68,68,0.20)]   text-[#EF4444]',
  success: 'bg-[rgba(16,185,129,0.08)]  border border-[rgba(16,185,129,0.20)]  text-[#10B981]',
  warning: 'bg-[rgba(245,158,11,0.08)]  border border-[rgba(245,158,11,0.20)]  text-[#B45309]',
  info:    'bg-[rgba(59,130,246,0.08)]   border border-[rgba(59,130,246,0.20)]   text-[#1D4ED8]',
};

const iconMap: Record<AlertVariant, ReactNode> = {
  error: (
    <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>
  ),
  success: (
    <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
    </svg>
  ),
  warning: (
    <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
    </svg>
  ),
  info: (
    <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  ),
};

export default function Alert({ variant, message, closeable = false, onClose, icon }: AlertProps) {
  return (
    <div
      className={`${variantClasses[variant]} p-4 rounded-md flex items-start gap-3 animate-fade-in`}
      role="alert"
    >
      <div className="mt-0.5">
        {icon || iconMap[variant]}
      </div>
      <p className="flex-1 text-sm leading-relaxed">{message}</p>
      {closeable && (
        <button
          onClick={onClose}
          className="text-current opacity-70 hover:opacity-100 transition-opacity flex-shrink-0 ml-1"
          aria-label="Close alert"
          type="button"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      )}
    </div>
  );
}
