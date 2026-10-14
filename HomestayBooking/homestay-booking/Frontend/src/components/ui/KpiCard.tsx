import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconDefinition } from '@fortawesome/fontawesome-svg-core';

// Component library definition:
// Anatomy: Icon, Title, Large Number Metric.
// Visuals: color-surface-card, radius-lg, shadow-sm.
// Touch-Friendly Variant: Min-height 100px for Employee Dashboard (Mobile-first).

interface KpiCardProps {
  title: string;
  value: string | number;
  icon: IconDefinition;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  isTouchFriendly?: boolean;
  className?: string;
}

export const KpiCard: React.FC<KpiCardProps> = ({ title, value, icon, trend, isTouchFriendly = false, className = '' }) => {
  return (
    <div className={`bg-white rounded-[16px] p-6 shadow-[0_1px_2px_rgba(0,0,0,0.05)] border border-[#E2E8F0] flex flex-col justify-center gap-3 ${isTouchFriendly ? 'min-h-[100px]' : ''} ${className}`}>
      <div className="flex items-center justify-between">
        <span className="text-[14px] font-normal text-[#64748B]">{title}</span>
        <div className="w-10 h-10 rounded-full bg-[#CCFBF1] text-[#0F766E] flex items-center justify-center">
          <FontAwesomeIcon icon={icon} />
        </div>
      </div>
      <div className="flex items-end gap-3">
        <span className="font-display text-[36px] font-bold leading-[1.2] tracking-tight text-[#1E293B]">
          {value}
        </span>
        {trend && (
          <span className={`text-[12px] font-semibold mb-1 ${trend.isPositive ? 'text-[#10B981]' : 'text-[#EF4444]'}`}>
            {trend.isPositive ? '+' : '-'}{Math.abs(trend.value)}%
          </span>
        )}
      </div>
    </div>
  );
};
