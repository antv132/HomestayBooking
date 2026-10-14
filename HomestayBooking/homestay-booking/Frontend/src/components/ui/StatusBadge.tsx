import React from 'react';

export type StatusVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'primary';

interface StatusBadgeProps {
  status: string;
  variant: StatusVariant;
  className?: string;
}

// Per component-library.md:
// Visuals: radius-full, padding 4px 12px, text-caption uppercase tracking-wide.
// Colors: ALWAYS uses a 10% opacity background of the semantic color, with 100% opacity text.
export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, variant, className = '' }) => {
  const variantClasses = {
    success: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-500/20 shadow-sm',
    warning: 'bg-amber-50 text-amber-700 ring-1 ring-amber-500/30 shadow-sm',
    danger: 'bg-rose-50 text-rose-700 ring-1 ring-rose-500/20 shadow-sm',
    info: 'bg-blue-50 text-blue-700 ring-1 ring-blue-500/20 shadow-sm',
    neutral: 'bg-slate-50 text-slate-700 ring-1 ring-slate-500/20 shadow-sm',
    primary: 'bg-teal-50 text-teal-700 ring-1 ring-teal-500/20 shadow-sm'
  };

  const dotClasses = {
    success: 'bg-emerald-500',
    warning: 'bg-amber-500',
    danger: 'bg-rose-500',
    info: 'bg-blue-500',
    neutral: 'bg-slate-400',
    primary: 'bg-teal-500'
  };

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-bold uppercase tracking-wider whitespace-nowrap transition-all hover:scale-105 ${variantClasses[variant]} ${className}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dotClasses[variant]} animate-pulse`} />
      {status}
    </span>
  );
};
