import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEllipsisVertical } from '@fortawesome/free-solid-svg-icons';

// Component library definition:
// Header: bg-surface-canvas (#F8FAFC), text-caption (12px, font-medium, uppercase, tracking-wide), color-text-secondary (#64748B)
// Rows: bg-surface-card (#FFFFFF), bottom border color-border-subtle (#F1F5F9)
// Interaction: Row hover changes background to color-surface-canvas (#F8FAFC)
// Actions: Kebab menu (Three dots "...") dropdown at the end of each row

export interface Column<T> {
  header: string;
  accessor: keyof T | ((row: T) => React.ReactNode);
  className?: string;
}

export interface Action<T> {
  label: React.ReactNode;
  onClick: (row: T) => void;
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string | number;
  actions?: Action<T>[];
  className?: string;
  footer?: React.ReactNode;
  getRowClassName?: (row: T) => string;
  /** Click row body (not actions cell) — e.g. open Detail */
  onRowClick?: (row: T) => void;
}

export function DataTable<T>({
  columns,
  data,
  keyExtractor,
  actions,
  className = '',
  footer,
  getRowClassName,
  onRowClick,
}: DataTableProps<T>) {
  const [openDropdownId, setOpenDropdownId] = useState<string | number | null>(null);

  const toggleDropdown = (id: string | number) => {
    setOpenDropdownId(openDropdownId === id ? null : id);
  };

  return (
    <div className={`w-full overflow-x-auto bg-white rounded-xl border border-[#E2E8F0] ${className}`}>
      <table className="w-full border-collapse">
        <thead>
          <tr>
            {columns.map((col, idx) => (
              <th 
                key={idx}
                className="text-left px-4 py-3 text-[12px] font-semibold uppercase tracking-wider text-[#64748B] bg-[#F8FAFC] border-b border-[#E2E8F0]"
              >
                {col.header}
              </th>
            ))}
            {actions && actions.length > 0 && (
              <th className="text-right px-4 py-3 text-[12px] font-semibold uppercase tracking-wider text-[#64748B] bg-[#F8FAFC] border-b border-[#E2E8F0]">
                Thao tác
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {data.map((row) => {
            const rowKey = keyExtractor(row);
            return (
              <tr
                key={rowKey}
                className={`group transition-colors duration-150 bg-white hover:bg-[#F8FAFC] ${onRowClick ? 'cursor-pointer' : ''} ${getRowClassName?.(row) ?? ''}`}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
              >
                {columns.map((col, idx) => (
                  <td key={idx} className={`px-4 py-3 text-sm text-[#334155] border-b border-[#F1F5F9] group-last:border-none ${col.className || ''}`}>
                    {typeof col.accessor === 'function' ? col.accessor(row) : (row[col.accessor] as React.ReactNode)}
                  </td>
                ))}
                
                {actions && actions.length > 0 && (
                  <td
                    className="px-4 py-3 border-b border-[#F1F5F9] group-last:border-none text-right relative"
                    onMouseLeave={() => setOpenDropdownId(null)}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button 
                      className="w-8 h-8 inline-flex items-center justify-center rounded-full text-[#64748B] hover:bg-[#E2E8F0] transition-colors"
                      onClick={() => toggleDropdown(rowKey)}
                      aria-label="Actions"
                    >
                      <FontAwesomeIcon icon={faEllipsisVertical} />
                    </button>
                    
                    {openDropdownId === rowKey && (
                      <div className="absolute right-8 top-10 w-44 bg-white border border-[#E2E8F0] rounded-md shadow-[0_4px_6px_-1px_rgba(0,0,0,0.1)] z-10 py-1">
                        {actions.map((action, actIdx) => (
                          <button
                            key={actIdx}
                            className={`w-full text-left px-4 py-2 text-sm hover:bg-[#F8FAFC] transition-colors ${action.className || 'text-[#1E293B]'}`}
                            onClick={() => {
                              setOpenDropdownId(null);
                              action.onClick(row);
                            }}
                          >
                            {action.label}
                          </button>
                        ))}
                      </div>
                    )}
                  </td>
                )}
              </tr>
            );
          })}
          {data.length === 0 && (
            <tr>
              <td colSpan={columns.length + (actions?.length ? 1 : 0)} className="px-4 py-8 text-center text-[#64748B] text-sm">
                Không có dữ liệu
              </td>
            </tr>
          )}
        </tbody>
        {footer && (
          <tfoot>
            {footer}
          </tfoot>
        )}
      </table>
    </div>
  );
}
export default DataTable;
