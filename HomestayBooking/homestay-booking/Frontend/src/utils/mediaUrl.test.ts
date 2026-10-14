import { describe, expect, it, vi } from 'vitest';
import { resolveMediaUrl, formatStatValue, FALLBACK_IMAGE } from './mediaUrl';

describe('mediaUrl utils', () => {
  describe('resolveMediaUrl', () => {
    it('returns fallback for null or undefined', () => {
      expect(resolveMediaUrl(null)).toBe(FALLBACK_IMAGE);
      expect(resolveMediaUrl(undefined)).toBe(FALLBACK_IMAGE);
      expect(resolveMediaUrl('')).toBe(FALLBACK_IMAGE);
    });

    it('returns absolute http URLs unchanged', () => {
      const httpUrl = 'http://example.com/image.jpg';
      expect(resolveMediaUrl(httpUrl)).toBe(httpUrl);
    });

    it('returns absolute https URLs unchanged', () => {
      const httpsUrl = 'https://example.com/image.jpg';
      expect(resolveMediaUrl(httpsUrl)).toBe(httpsUrl);
    });

    it('prepends API base for relative paths when VITE_API_URL is set', () => {
      vi.stubEnv('VITE_API_URL', 'https://api.example.com');
      expect(resolveMediaUrl('/uploads/image.jpg')).toBe('https://api.example.com/uploads/image.jpg');
      vi.unstubAllEnvs();
    });

    it('prepends API base for paths without leading slash', () => {
      vi.stubEnv('VITE_API_URL', 'https://api.example.com');
      expect(resolveMediaUrl('uploads/image.jpg')).toBe('https://api.example.com/uploads/image.jpg');
      vi.unstubAllEnvs();
    });

    it('handles API base with trailing slash', () => {
      vi.stubEnv('VITE_API_URL', 'https://api.example.com/');
      expect(resolveMediaUrl('/uploads/image.jpg')).toBe('https://api.example.com/uploads/image.jpg');
      vi.unstubAllEnvs();
    });

    it('returns relative path when VITE_API_URL is not set', () => {
      vi.stubEnv('VITE_API_URL', '');
      expect(resolveMediaUrl('/uploads/image.jpg')).toBe('/uploads/image.jpg');
      vi.unstubAllEnvs();
    });

    it('adds leading slash to relative paths without one when no API base', () => {
      vi.stubEnv('VITE_API_URL', '');
      expect(resolveMediaUrl('uploads/image.jpg')).toBe('/uploads/image.jpg');
      vi.unstubAllEnvs();
    });
  });

  describe('formatStatValue', () => {
    it('formats values >= 1000 with K suffix', () => {
      expect(formatStatValue(1000)).toBe('1K+');
      expect(formatStatValue(1500)).toBe('1K+');
      expect(formatStatValue(5000)).toBe('5K+');
      expect(formatStatValue(10000)).toBe('10K+');
    });

    it('formats values < 1000 without K suffix', () => {
      expect(formatStatValue(1)).toBe('1+');
      expect(formatStatValue(50)).toBe('50+');
      expect(formatStatValue(999)).toBe('999+');
    });

    it('returns 0 for zero value', () => {
      expect(formatStatValue(0)).toBe('0');
    });

    it('returns 0 for negative value', () => {
      expect(formatStatValue(-5)).toBe('0');
    });

    it('uses custom suffix when provided', () => {
      expect(formatStatValue(1000, ' properties')).toBe('1K properties');
      expect(formatStatValue(500, ' rooms')).toBe('500 rooms');
      expect(formatStatValue(0, ' items')).toBe('0');
    });

    it('handles empty string suffix', () => {
      expect(formatStatValue(1000, '')).toBe('1K');
      expect(formatStatValue(50, '')).toBe('50');
    });
  });
});
