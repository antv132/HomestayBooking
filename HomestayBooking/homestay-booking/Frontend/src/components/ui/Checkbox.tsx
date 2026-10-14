import { InputHTMLAttributes, ReactNode } from 'react';

interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: ReactNode;
  error?: string;
}

export default function Checkbox({ label, error, id, ...props }: CheckboxProps) {
  const checkboxId = id || (typeof props.name === 'string' ? props.name : undefined);

  return (
    <div className="space-y-1">
      <div className="flex items-start gap-3">
        <div className="relative flex items-center mt-[2px]">
          <input
            id={checkboxId}
            type="checkbox"
            className={`
              peer w-[20px] h-[20px] appearance-none rounded-[4px] border
              ${error ? 'border-[#c13515]' : 'border-[#dddddd]'}
              bg-white checked:bg-[#222222] checked:border-[#222222]
              hover:border-[#222222] transition-colors duration-200 cursor-pointer
              focus:outline-none focus:ring-2 focus:ring-[#222222] focus:ring-offset-1
            `}
            {...props}
          />
          {/* Custom SVG Checkmark */}
          <svg 
            className="absolute w-[12px] h-[12px] top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 pointer-events-none opacity-0 peer-checked:opacity-100 text-white stroke-[3px]"
            fill="none" 
            stroke="currentColor" 
            viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </div>

        {label && (
          <label htmlFor={checkboxId} className="text-body-sm text-[#222222] cursor-pointer pt-[2px]">
            {label}
          </label>
        )}
      </div>

      {error && (
        <p className="text-caption-sm text-[#c13515] ml-[32px]" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
