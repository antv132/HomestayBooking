import { describe, expect, it, vi } from 'vitest';
import { digitsOnly, clampPositiveIntString, blockNonDigitKey } from './filterInput';

describe('filterInput utils', () => {
  describe('digitsOnly', () => {
    it('removes non-digit characters', () => {
      expect(digitsOnly('abc123def')).toBe('123');
      expect(digitsOnly('12-34-56')).toBe('123456');
      expect(digitsOnly('12.34')).toBe('1234');
    });

    it('returns empty string when no digits', () => {
      expect(digitsOnly('abc')).toBe('');
      expect(digitsOnly('')).toBe('');
      expect(digitsOnly('!@#$%')).toBe('');
    });

    it('preserves digits only', () => {
      expect(digitsOnly('1234567890')).toBe('1234567890');
    });
  });

  describe('clampPositiveIntString', () => {
    it('clamps value within min and max', () => {
      expect(clampPositiveIntString('150', 100, 1)).toBe('100');
      expect(clampPositiveIntString('50', 100, 1)).toBe('50');
      expect(clampPositiveIntString('0', 100, 1)).toBe('1');
    });

    it('returns empty string for empty input', () => {
      expect(clampPositiveIntString('', 100, 1)).toBe('');
    });

    it('removes non-digits before clamping', () => {
      expect(clampPositiveIntString('abc50xyz', 100, 1)).toBe('50');
      expect(clampPositiveIntString('12.34', 100, 1)).toBe('100'); // '1234' -> clamped to 100
    });

    it('uses default min of 1', () => {
      expect(clampPositiveIntString('0', 100)).toBe('1');
      expect(clampPositiveIntString('1', 100)).toBe('1');
    });

    it('handles edge cases with max', () => {
      expect(clampPositiveIntString('999', 100, 1)).toBe('100');
      expect(clampPositiveIntString('100', 100, 1)).toBe('100');
      expect(clampPositiveIntString('101', 100, 1)).toBe('100');
    });

    it('works with custom min value', () => {
      expect(clampPositiveIntString('5', 100, 10)).toBe('10');
      expect(clampPositiveIntString('10', 100, 10)).toBe('10');
      expect(clampPositiveIntString('50', 100, 10)).toBe('50');
    });
  });

  describe('blockNonDigitKey', () => {
    it('allows digit keys', () => {
      for (let i = 0; i <= 9; i++) {
        const event = {
          key: String(i),
          ctrlKey: false,
          metaKey: false,
          altKey: false,
          preventDefault: vi.fn(),
        } as any;
        blockNonDigitKey(event);
        expect(event.preventDefault).not.toHaveBeenCalled();
      }
    });

    it('allows navigation keys', () => {
      const allowedKeys = ['Backspace', 'Delete', 'ArrowLeft', 'ArrowRight', 'Tab', 'Home', 'End'];
      allowedKeys.forEach((key) => {
        const event = {
          key,
          ctrlKey: false,
          metaKey: false,
          altKey: false,
          preventDefault: vi.fn(),
        } as any;
        blockNonDigitKey(event);
        expect(event.preventDefault).not.toHaveBeenCalled();
      });
    });

    it('blocks non-digit, non-navigation keys', () => {
      const blockedKeys = ['a', 'A', 'Enter', ' ', '.', '-', 'e', 'E'];
      blockedKeys.forEach((key) => {
        const event = {
          key,
          ctrlKey: false,
          metaKey: false,
          altKey: false,
          preventDefault: vi.fn(),
        } as any;
        blockNonDigitKey(event);
        expect(event.preventDefault).toHaveBeenCalled();
      });
    });

    it('allows modifier key combinations (ctrl/meta/alt)', () => {
      const event1 = {
        key: 'a',
        ctrlKey: true,
        metaKey: false,
        altKey: false,
        preventDefault: vi.fn(),
      } as any;
      blockNonDigitKey(event1);
      expect(event1.preventDefault).not.toHaveBeenCalled();

      const event2 = {
        key: 'v',
        metaKey: true,
        ctrlKey: false,
        altKey: false,
        preventDefault: vi.fn(),
      } as any;
      blockNonDigitKey(event2);
      expect(event2.preventDefault).not.toHaveBeenCalled();

      const event3 = {
        key: 'x',
        altKey: true,
        ctrlKey: false,
        metaKey: false,
        preventDefault: vi.fn(),
      } as any;
      blockNonDigitKey(event3);
      expect(event3.preventDefault).not.toHaveBeenCalled();
    });
  });
});
