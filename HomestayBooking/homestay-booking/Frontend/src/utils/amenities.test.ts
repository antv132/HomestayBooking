import { describe, expect, it } from 'vitest';
import { fixAmenityLabel, fixAmenityLabels, AMENITY_OPTIONS } from './amenities';

describe('amenities utils', () => {
  describe('fixAmenityLabel', () => {
    it('returns unchanged if already in AMENITY_OPTIONS', () => {
      expect(fixAmenityLabel('WiFi')).toBe('WiFi');
      expect(fixAmenityLabel('Điều hòa')).toBe('Điều hòa');
      expect(fixAmenityLabel('Bếp')).toBe('Bếp');
      expect(fixAmenityLabel('Smart TV')).toBe('Smart TV');
    });

    it('returns empty/null input unchanged', () => {
      expect(fixAmenityLabel('')).toBe('');
    });

    it('fixes labels with ? encoding using regex', () => {
      expect(fixAmenityLabel('B?p')).toBe('Bếp');
      expect(fixAmenityLabel('B?p nh?')).toBe('Bếp nhỏ');
      expect(fixAmenityLabel('Đi?u hòa')).toBe('Điều hòa');
      expect(fixAmenityLabel('T? l?nh')).toBe('Tủ lạnh');
      expect(fixAmenityLabel('T? qu?n áo')).toBe('Tủ quần áo');
    });

    it('uses fallback map when regex does not match', () => {
      // Testing explicit fallback entries
      expect(fixAmenityLabel('Dieu?u hòa')).toBe('Điều hòa');
      expect(fixAmenityLabel('View bi?n')).toBe('View biển');
      expect(fixAmenityLabel('H? b?i riêng')).toBe('Hồ bơi riêng');
      expect(fixAmenityLabel('Bãi đ? xe')).toBe('Bãi đỗ xe');
      expect(fixAmenityLabel('Bàn làm vi?c')).toBe('Bàn làm việc');
      expect(fixAmenityLabel('Máy gi?t')).toBe('Máy giặt');
      expect(fixAmenityLabel('Nư?c nóng')).toBe('Nước nóng');
    });

    it('returns original if no match found', () => {
      expect(fixAmenityLabel('Unknown amenity')).toBe('Unknown amenity');
      expect(fixAmenityLabel('Some random text')).toBe('Some random text');
    });

    it('is case-insensitive for regex matching', () => {
      expect(fixAmenityLabel('b?p')).toBe('Bếp');
      expect(fixAmenityLabel('WIFI')).toBe('WiFi');
    });

    it('handles multiple ? characters', () => {
      expect(fixAmenityLabel('H? b?i riêng')).toBe('Hồ bơi riêng');
    });

    it('escapes regex special characters properly', () => {
      // Testing that special characters are escaped
      const specialLabel = 'Test*Label+Special';
      expect(fixAmenityLabel(specialLabel)).toBe(specialLabel);
    });
  });

  describe('fixAmenityLabels', () => {
    it('fixes all labels in array', () => {
      const input = ['B?p', 'WiFi', 'Đi?u hòa', 'T? l?nh'];
      const expected = ['Bếp', 'WiFi', 'Điều hòa', 'Tủ lạnh'];
      expect(fixAmenityLabels(input)).toEqual(expected);
    });

    it('returns empty array for null input', () => {
      expect(fixAmenityLabels(null)).toEqual([]);
    });

    it('returns empty array for undefined input', () => {
      expect(fixAmenityLabels(undefined)).toEqual([]);
    });

    it('returns empty array for empty array', () => {
      expect(fixAmenityLabels([])).toEqual([]);
    });

    it('handles mixed corrupted and valid labels', () => {
      const input = ['WiFi', 'B?p nh?', 'Smart TV', 'Đi?u hòa'];
      const expected = ['WiFi', 'Bếp nhỏ', 'Smart TV', 'Điều hòa'];
      expect(fixAmenityLabels(input)).toEqual(expected);
    });

    it('preserves unknown labels', () => {
      const input = ['WiFi', 'Unknown', 'B?p'];
      const expected = ['WiFi', 'Unknown', 'Bếp'];
      expect(fixAmenityLabels(input)).toEqual(expected);
    });
  });

  describe('AMENITY_OPTIONS', () => {
    it('contains expected amenities', () => {
      expect(AMENITY_OPTIONS).toContain('WiFi');
      expect(AMENITY_OPTIONS).toContain('Điều hòa');
      expect(AMENITY_OPTIONS).toContain('Bếp');
      expect(AMENITY_OPTIONS).toContain('Bếp nhỏ');
      expect(AMENITY_OPTIONS).toContain('Smart TV');
    });

    it('has expected length', () => {
      expect(AMENITY_OPTIONS.length).toBeGreaterThan(0);
    });
  });
});
