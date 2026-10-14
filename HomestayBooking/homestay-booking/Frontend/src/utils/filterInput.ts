import type { KeyboardEvent } from 'react';

export const MAX_FILTER_PRICE = 500_000_000;
export const MAX_FILTER_GUESTS = 100;

/** Chỉ giữ chữ số; chuỗi rỗng được phép */
export function digitsOnly(raw: string): string {
  return raw.replace(/\D/g, '');
}

/** Ép giá trị thành số nguyên dương trong [min, max]; rỗng → rỗng */
export function clampPositiveIntString(raw: string, max: number, min = 1): string {
  const digits = digitsOnly(raw);
  if (!digits) return '';
  const n = Math.min(max, Math.max(min, parseInt(digits, 10)));
  return String(n);
}

export function blockNonDigitKey(e: KeyboardEvent<HTMLInputElement>) {
  if (e.ctrlKey || e.metaKey || e.altKey) return;
  const allowed = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
  if (allowed.includes(e.key)) return;
  if (!/^\d$/.test(e.key)) e.preventDefault();
}
