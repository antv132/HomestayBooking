/** Danh sách tiện nghi chuẩn (UTF-8) dùng chung Manager Add/Edit Room */
export const AMENITY_OPTIONS = [
  'WiFi',
  'Điều hòa',
  'Hồ bơi riêng',
  'Bếp',
  'Bếp nhỏ',
  'View biển',
  'Bãi đỗ xe',
  'Minibar',
  'Ban công',
  'Smart TV',
  'TV',
  'Room service',
  'Tủ lạnh',
  'Bàn làm việc',
  'Máy giặt',
  'Nước nóng',
  'Tủ quần áo',
] as const;

export type AmenityOption = (typeof AMENITY_OPTIONS)[number];

/**
 * Sửa nhãn tiện nghi bị mất dấu (thường thành `?`) khi lưu/đọc DB sai encoding.
 * VD: "B?p nh?" → "Bếp nhỏ", "Đi?u hòa" → "Điều hòa"
 */
export function fixAmenityLabel(raw: string): string {
  if (!raw) return raw;
  if ((AMENITY_OPTIONS as readonly string[]).includes(raw)) return raw;

  // `?` thay cho 1 ký tự có dấu → match bằng regex wildcard
  const escaped = raw
    .replace(/[.*+^${}()|[\]\\]/g, '\\$&')
    .replace(/\?/g, '.');
  const re = new RegExp(`^${escaped}$`, 'i');
  const matched = AMENITY_OPTIONS.find((opt) => re.test(opt));
  if (matched) return matched;

  // Fallback map tường minh (phòng khi regex không khớp)
  const FALLBACK: Record<string, string> = {
    'B?p nh?': 'Bếp nhỏ',
    'B?p': 'Bếp',
    'T? l?nh': 'Tủ lạnh',
    'T? qu?n áo': 'Tủ quần áo',
    'Đi?u hòa': 'Điều hòa',
    'Dieu?u hòa': 'Điều hòa',
    'View bi?n': 'View biển',
    'H? b?i riêng': 'Hồ bơi riêng',
    'Bãi đ? xe': 'Bãi đỗ xe',
    'Bàn làm vi?c': 'Bàn làm việc',
    'Máy gi?t': 'Máy giặt',
    'Nư?c nóng': 'Nước nóng',
  };
  return FALLBACK[raw] ?? raw;
}

export function fixAmenityLabels(list: string[] | null | undefined): string[] {
  if (!list?.length) return [];
  return list.map(fixAmenityLabel);
}
