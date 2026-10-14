export interface BookedRange {
  checkIn: string;
  checkOut: string;
  bookingStatus: string;
}

/** FR-05 — 8 day statuses (+ past for UI). */
export type DayStatus =
  | 'past'
  | 'available'
  | 'pending'
  | 'reserved'
  | 'occupied'
  | 'pending_cleaning'
  | 'cleaning_in_progress'
  | 'maintenance'
  | 'out_of_service';

/** Màu ô lịch theo SCR-08 / SCR-10 + FR-05 8 statuses */
export const DAY_STYLES: Record<DayStatus, { bg: string; color: string; label: string }> = {
  past: { bg: 'transparent', color: 'var(--stone)', label: 'Đã qua' },
  available: { bg: '#dcfce7', color: '#2b9a66', label: 'Còn trống' },
  pending: { bg: '#fef3c7', color: '#d97706', label: 'Chờ cọc' },
  reserved: { bg: '#dbeafe', color: '#2563EB', label: 'Đã đặt' },
  occupied: { bg: '#fee2e2', color: '#DC2626', label: 'Đang ở' },
  pending_cleaning: { bg: '#ffedd5', color: '#ea580c', label: 'Chờ dọn' },
  cleaning_in_progress: { bg: '#fce7f3', color: '#db2777', label: 'Đang dọn' },
  maintenance: { bg: '#f3f4f6', color: '#6B7280', label: 'Bảo trì' },
  out_of_service: { bg: '#e5e7eb', color: '#374151', label: 'Ngưng phục vụ' },
};

export const LEGEND_STATUSES: DayStatus[] = [
  'available',
  'pending',
  'reserved',
  'occupied',
  'pending_cleaning',
  'cleaning_in_progress',
  'maintenance',
  'out_of_service',
];

export const MONTH_NAMES_VI = [
  'Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
  'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12',
];

export const WEEKDAY_LABELS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

export function parseLocalDate(iso: string) {
  const [y, m, day] = iso.split('-').map(Number);
  return new Date(y, m - 1, day);
}

export function dateKey(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/**
 * Trạng thái vận hành chặn đặt phòng — khớp BOOKING_BLOCKED_ROOM_STATUSES phía backend.
 * RESERVED/OCCUPIED KHÔNG chặn: tồn kho tính theo khoảng ngày (Spec FR-04).
 */
export const BOOKING_BLOCKED_ROOM_STATUSES = [
  'MAINTENANCE',
  'OUT_OF_SERVICE',
  'PENDING_CLEANING',
  'CLEANING_IN_PROGRESS',
] as const;

export function isRoomBookingBlocked(roomStatus: string): boolean {
  return (BOOKING_BLOCKED_ROOM_STATUSES as readonly string[]).includes(roomStatus);
}

function roomStatusToDayStatus(roomStatus: string): DayStatus | null {
  switch (roomStatus?.toUpperCase()) {
    case 'MAINTENANCE':
      return 'maintenance';
    case 'OUT_OF_SERVICE':
      return 'out_of_service';
    case 'PENDING_CLEANING':
      return 'pending_cleaning';
    case 'CLEANING_IN_PROGRESS':
      return 'cleaning_in_progress';
    default:
      return null;
  }
}

function bookingStatusToDayStatus(bookingStatus: string): DayStatus {
  const st = bookingStatus?.toUpperCase() ?? '';
  if (st === 'PENDING_DEPOSIT') return 'pending';
  if (st === 'CHECKED_IN' || st === 'PENDING_INSPECTION' || st === 'PENDING_DAMAGE_PAYMENT') {
    return 'occupied';
  }
  if (st === 'CONFIRMED' || st === 'RESERVED') return 'reserved';
  return 'reserved';
}

/** Priority high→low matching 005 Assumptions. */
const DAY_PRIORITY: Record<DayStatus, number> = {
  occupied: 1,
  pending: 2,
  reserved: 3,
  cleaning_in_progress: 4,
  pending_cleaning: 5,
  maintenance: 6,
  out_of_service: 7,
  available: 8,
  past: 99,
};

function higherPriority(a: DayStatus, b: DayStatus): DayStatus {
  return DAY_PRIORITY[a] <= DAY_PRIORITY[b] ? a : b;
}

export function statusForDay(dateKeyStr: string, ranges: BookedRange[], roomStatus: string): DayStatus {
  const day = parseLocalDate(dateKeyStr);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (day < today) return 'past';

  let status: DayStatus = 'available';
  const roomDay = roomStatusToDayStatus(roomStatus);
  // Room-level MAINTENANCE/OOS without days[] — treat as operational snapshot (caller should prefer days[])
  if (roomDay) status = roomDay;

  for (const range of ranges) {
    const start = parseLocalDate(range.checkIn);
    const end = parseLocalDate(range.checkOut);
    // Half-open stay [checkIn, checkOut)
    if (day >= start && day < end) {
      status = higherPriority(status, bookingStatusToDayStatus(range.bookingStatus));
    }
  }
  return status;
}

/** Map BE CalendarDayStatusResponse.status → DayStatus */
export function apiStatusToDayStatus(apiStatus: string): DayStatus {
  switch (apiStatus?.toUpperCase()) {
    case 'PENDING_DEPOSIT':
      return 'pending';
    case 'RESERVED':
      return 'reserved';
    case 'OCCUPIED':
      return 'occupied';
    case 'PENDING_CLEANING':
      return 'pending_cleaning';
    case 'CLEANING_IN_PROGRESS':
      return 'cleaning_in_progress';
    case 'MAINTENANCE':
      return 'maintenance';
    case 'OUT_OF_SERVICE':
      return 'out_of_service';
    case 'AVAILABLE':
    default:
      return 'available';
  }
}

export function statusForDayWithApi(
  dateKeyStr: string,
  dayStatuses: Record<string, DayStatus> | undefined,
  ranges: BookedRange[],
  roomStatus: string,
): DayStatus {
  const day = parseLocalDate(dateKeyStr);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (day < today) return 'past';
  if (dayStatuses && dayStatuses[dateKeyStr]) {
    return dayStatuses[dateKeyStr];
  }
  return statusForDay(dateKeyStr, ranges, roomStatus);
}

export function isRangeAvailable(
  checkIn: string,
  checkOut: string,
  ranges: BookedRange[],
  roomStatus: string,
  dayStatuses?: Record<string, DayStatus>,
): boolean {
  if (!checkIn || !checkOut || checkOut <= checkIn) return false;
  const start = parseLocalDate(checkIn);
  const end = parseLocalDate(checkOut);
  // Half-open: nights [checkIn, checkOut)
  for (let d = new Date(start); d < end; d.setDate(d.getDate() + 1)) {
    const key = dateKey(d.getFullYear(), d.getMonth(), d.getDate());
    if (statusForDayWithApi(key, dayStatuses, ranges, roomStatus) !== 'available') return false;
  }
  // Room-level block only when no days[] (legacy) — if days[] present, per-day already checked
  if (!dayStatuses && isRoomBookingBlocked(roomStatus)) return false;
  return true;
}

export function addMonths(year: number, month: number, delta: number) {
  const d = new Date(year, month + delta, 1);
  return { year: d.getFullYear(), month: d.getMonth() };
}
