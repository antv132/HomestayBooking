import { ReactNode, useEffect } from 'react';
import Button from './Button';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  actions?: {
    label: string;
    onClick: () => void;
    variant?: 'primary' | 'secondary' | 'ghost';
  }[];
  size?: 'sm' | 'md' | 'lg';
}

const sizeClasses = {
  sm: 'max-w-[400px]',
  md: 'max-w-[568px]',
  lg: 'max-w-[780px]',
};

export default function Modal({ isOpen, onClose, title, children, actions, size = 'md' }: ModalProps) {
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Scrim Backdrop */}
      <div
        className="absolute inset-0 bg-[rgba(0,0,0,0.5)] transition-opacity"
        onClick={onClose}
        aria-label="Close modal"
      />

      {/* Modal Surface */}
      <div className={`relative w-full ${sizeClasses[size]} bg-white rounded-lg shadow-card-hover animate-scale-up flex flex-col max-h-[90vh]`}>
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-[#ebebeb]">
          <h2 className="text-display-sm text-[#222222]">{title}</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#f7f7f7] transition-colors text-[#222222]"
            aria-label="Close modal"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto">
          {children}
        </div>

        {/* Footer Actions */}
        {actions && actions.length > 0 && (
          <div className="flex items-center gap-4 p-6 border-t border-[#ebebeb] justify-end bg-white rounded-b-lg">
            {actions.map((action, index) => (
              <Button
                key={index}
                onClick={action.onClick}
                variant={action.variant || 'secondary'}
              >
                {action.label}
              </Button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
