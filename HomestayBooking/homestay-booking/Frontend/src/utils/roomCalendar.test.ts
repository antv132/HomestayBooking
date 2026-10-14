import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest';
import {
  parseLocalDate,
  dateKey,
  isRoomBookingBlocked,
  DAY_STYLES,
  LEGEND_STATUSES,
  statusForDay,
  apiStatusToDayStatus,
  statusForDayWithApi,
  isRangeAvailable,
  addMonths,
  BOOKING_BLOCKED_ROOM_STATUSES,
  type BookedRange,
  type DayStatus,
} from './roomCalendar';

describe('roomCalendar utils', () => {
  beforeEach(() => {
    // Mock today to a fixed date for consistent tests
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-26T00:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('parseLocalDate', () => {
    it('parses ISO date string correctly', () => {
      const date = parseLocalDate('2026-07-26');
      expect(date.getFullYear()).toBe(2026);
      expect(date.getMonth()).toBe(6); // 0-indexed, July = 6
      expect(date.getDate()).toBe(26);
    });

    it('handles different months', () => {
      const jan = parseLocalDate('2026-01-15');
      expect(jan.getMonth()).toBe(0);

      const dec = parseLocalDate('2026-12-31');
      expect(dec.getMonth()).toBe(11);
    });

    it('handles leap year dates', () => {
      const leapDay = parseLocalDate('2024-02-29');
      expect(leapDay.getFullYear()).toBe(2024);
      expect(leapDay.getMonth()).toBe(1);
      expect(leapDay.getDate()).toBe(29);
    });
  });

  describe('dateKey', () => {
    it('formats date key with zero padding', () => {
      expect(dateKey(2026, 0, 5)).toBe('2026-01-05');
      expect(dateKey(2026, 11, 25)).toBe('2026-12-25');
    });

    it('handles single-digit days and months', () => {
      expect(dateKey(2026, 0, 1)).toBe('2026-01-01');
      expect(dateKey(2026, 8, 9)).toBe('2026-09-09');
    });

    it('handles double-digit days and months', () => {
      expect(dateKey(2026, 9, 15)).toBe('2026-10-15');
      expect(dateKey(2026, 11, 31)).toBe('2026-12-31');
    });
  });

  describe('isRoomBookingBlocked', () => {
    it('returns true for blocked statuses', () => {
      expect(isRoomBookingBlocked('MAINTENANCE')).toBe(true);
      expect(isRoomBookingBlocked('OUT_OF_SERVICE')).toBe(true);
      expect(isRoomBookingBlocked('PENDING_CLEANING')).toBe(true);
      expect(isRoomBookingBlocked('CLEANING_IN_PROGRESS')).toBe(true);
    });

    it('returns false for non-blocked statuses', () => {
      expect(isRoomBookingBlocked('AVAILABLE')).toBe(false);
      expect(isRoomBookingBlocked('RESERVED')).toBe(false);
      expect(isRoomBookingBlocked('OCCUPIED')).toBe(false);
      expect(isRoomBookingBlocked('CONFIRMED')).toBe(false);
    });

    it('returns false for empty or invalid statuses', () => {
      expect(isRoomBookingBlocked('')).toBe(false);
      expect(isRoomBookingBlocked('INVALID')).toBe(false);
    });
  });

  describe('BOOKING_BLOCKED_ROOM_STATUSES', () => {
    it('contains exactly 4 blocking statuses', () => {
      expect(BOOKING_BLOCKED_ROOM_STATUSES).toHaveLength(4);
      expect(BOOKING_BLOCKED_ROOM_STATUSES).toContain('MAINTENANCE');
      expect(BOOKING_BLOCKED_ROOM_STATUSES).toContain('OUT_OF_SERVICE');
      expect(BOOKING_BLOCKED_ROOM_STATUSES).toContain('PENDING_CLEANING');
      expect(BOOKING_BLOCKED_ROOM_STATUSES).toContain('CLEANING_IN_PROGRESS');
    });
  });

  describe('DAY_STYLES', () => {
    it('has styles for all day statuses', () => {
      const statuses: DayStatus[] = [
        'past',
        'available',
        'pending',
        'reserved',
        'occupied',
        'pending_cleaning',
        'cleaning_in_progress',
        'maintenance',
        'out_of_service',
      ];

      statuses.forEach((status) => {
        expect(DAY_STYLES[status]).toBeDefined();
        expect(DAY_STYLES[status].bg).toBeDefined();
        expect(DAY_STYLES[status].color).toBeDefined();
        expect(DAY_STYLES[status].label).toBeDefined();
      });
    });
  });

  describe('LEGEND_STATUSES', () => {
    it('contains 8 statuses (excludes past)', () => {
      expect(LEGEND_STATUSES).toHaveLength(8);
      expect(LEGEND_STATUSES).not.toContain('past');
    });

    it('contains all relevant statuses', () => {
      expect(LEGEND_STATUSES).toContain('available');
      expect(LEGEND_STATUSES).toContain('pending');
      expect(LEGEND_STATUSES).toContain('reserved');
      expect(LEGEND_STATUSES).toContain('occupied');
      expect(LEGEND_STATUSES).toContain('pending_cleaning');
      expect(LEGEND_STATUSES).toContain('cleaning_in_progress');
      expect(LEGEND_STATUSES).toContain('maintenance');
      expect(LEGEND_STATUSES).toContain('out_of_service');
    });
  });

  describe('apiStatusToDayStatus', () => {
    it('maps PENDING_DEPOSIT to pending', () => {
      expect(apiStatusToDayStatus('PENDING_DEPOSIT')).toBe('pending');
    });

    it('maps RESERVED to reserved', () => {
      expect(apiStatusToDayStatus('RESERVED')).toBe('reserved');
    });

    it('maps OCCUPIED to occupied', () => {
      expect(apiStatusToDayStatus('OCCUPIED')).toBe('occupied');
    });

    it('maps cleaning statuses correctly', () => {
      expect(apiStatusToDayStatus('PENDING_CLEANING')).toBe('pending_cleaning');
      expect(apiStatusToDayStatus('CLEANING_IN_PROGRESS')).toBe('cleaning_in_progress');
    });

    it('maps maintenance statuses correctly', () => {
      expect(apiStatusToDayStatus('MAINTENANCE')).toBe('maintenance');
      expect(apiStatusToDayStatus('OUT_OF_SERVICE')).toBe('out_of_service');
    });

    it('maps AVAILABLE and unknown to available', () => {
      expect(apiStatusToDayStatus('AVAILABLE')).toBe('available');
      expect(apiStatusToDayStatus('UNKNOWN')).toBe('available');
      expect(apiStatusToDayStatus('')).toBe('available');
    });

    it('is case-insensitive', () => {
      expect(apiStatusToDayStatus('pending_deposit')).toBe('pending');
      expect(apiStatusToDayStatus('reserved')).toBe('reserved');
    });
  });

  describe('statusForDay', () => {
    it('returns past for dates before today', () => {
      const status = statusForDay('2026-07-25', [], 'AVAILABLE');
      expect(status).toBe('past');
    });

    it('returns available for future date with no bookings', () => {
      const status = statusForDay('2026-07-27', [], 'AVAILABLE');
      expect(status).toBe('available');
    });

    it('returns maintenance when room status is MAINTENANCE', () => {
      const status = statusForDay('2026-07-27', [], 'MAINTENANCE');
      expect(status).toBe('maintenance');
    });

    it('returns out_of_service when room status is OUT_OF_SERVICE', () => {
      const status = statusForDay('2026-07-27', [], 'OUT_OF_SERVICE');
      expect(status).toBe('out_of_service');
    });

    it('returns pending for PENDING_DEPOSIT booking', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'PENDING_DEPOSIT' },
      ];
      const status = statusForDay('2026-07-27', ranges, 'AVAILABLE');
      expect(status).toBe('pending');
    });

    it('returns reserved for CONFIRMED booking', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      const status = statusForDay('2026-07-27', ranges, 'AVAILABLE');
      expect(status).toBe('reserved');
    });

    it('returns occupied for CHECKED_IN booking', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CHECKED_IN' },
      ];
      const status = statusForDay('2026-07-27', ranges, 'AVAILABLE');
      expect(status).toBe('occupied');
    });

    it('returns occupied for PENDING_INSPECTION booking', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'PENDING_INSPECTION' },
      ];
      const status = statusForDay('2026-07-27', ranges, 'AVAILABLE');
      expect(status).toBe('occupied');
    });

    it('returns occupied for PENDING_DAMAGE_PAYMENT booking', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'PENDING_DAMAGE_PAYMENT' },
      ];
      const status = statusForDay('2026-07-27', ranges, 'AVAILABLE');
      expect(status).toBe('occupied');
    });

    it('uses half-open range [checkIn, checkOut)', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      expect(statusForDay('2026-07-25', ranges, 'AVAILABLE')).toBe('past'); // Before today (2026-07-26)
      expect(statusForDay('2026-07-27', ranges, 'AVAILABLE')).toBe('reserved'); // checkIn included
      expect(statusForDay('2026-07-28', ranges, 'AVAILABLE')).toBe('reserved');
      expect(statusForDay('2026-07-29', ranges, 'AVAILABLE')).toBe('reserved');
      expect(statusForDay('2026-07-30', ranges, 'AVAILABLE')).toBe('available'); // checkOut excluded
    });

    it('applies priority: occupied > pending > reserved', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-29', bookingStatus: 'CONFIRMED' },
        { checkIn: '2026-07-28', checkOut: '2026-07-30', bookingStatus: 'CHECKED_IN' },
      ];
      // 2026-07-28 has both CONFIRMED and CHECKED_IN; occupied wins
      const status = statusForDay('2026-07-28', ranges, 'AVAILABLE');
      expect(status).toBe('occupied');
    });

    it('applies priority: room status < booking status', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CHECKED_IN' },
      ];
      // occupied (1) beats maintenance (6)
      const status = statusForDay('2026-07-27', ranges, 'MAINTENANCE');
      expect(status).toBe('occupied');
    });

    it('applies priority: cleaning_in_progress > pending_cleaning', () => {
      const ranges: BookedRange[] = [];
      // When no bookings, room status determines
      expect(statusForDay('2026-07-27', ranges, 'CLEANING_IN_PROGRESS')).toBe('cleaning_in_progress');
      expect(statusForDay('2026-07-27', ranges, 'PENDING_CLEANING')).toBe('pending_cleaning');
    });

    it('handles multiple overlapping bookings correctly', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-29', bookingStatus: 'PENDING_DEPOSIT' },
        { checkIn: '2026-07-28', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      expect(statusForDay('2026-07-27', ranges, 'AVAILABLE')).toBe('pending'); // only pending
      expect(statusForDay('2026-07-28', ranges, 'AVAILABLE')).toBe('pending'); // pending > reserved
      expect(statusForDay('2026-07-29', ranges, 'AVAILABLE')).toBe('reserved'); // only reserved
    });
  });

  describe('statusForDayWithApi', () => {
    it('returns past for dates before today', () => {
      const status = statusForDayWithApi('2026-07-25', {}, [], 'AVAILABLE');
      expect(status).toBe('past');
    });

    it('uses dayStatuses when available', () => {
      const dayStatuses = { '2026-07-27': 'maintenance' as DayStatus };
      const status = statusForDayWithApi('2026-07-27', dayStatuses, [], 'AVAILABLE');
      expect(status).toBe('maintenance');
    });

    it('falls back to statusForDay when dayStatuses is undefined', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      const status = statusForDayWithApi('2026-07-27', undefined, ranges, 'AVAILABLE');
      expect(status).toBe('reserved');
    });

    it('falls back to statusForDay when date not in dayStatuses', () => {
      const dayStatuses = { '2026-07-28': 'maintenance' as DayStatus };
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      const status = statusForDayWithApi('2026-07-27', dayStatuses, ranges, 'AVAILABLE');
      expect(status).toBe('reserved');
    });
  });

  describe('isRangeAvailable', () => {
    it('returns false for invalid date range (checkOut <= checkIn)', () => {
      expect(isRangeAvailable('2026-07-27', '2026-07-27', [], 'AVAILABLE')).toBe(false);
      expect(isRangeAvailable('2026-07-28', '2026-07-27', [], 'AVAILABLE')).toBe(false);
    });

    it('returns false for empty checkIn or checkOut', () => {
      expect(isRangeAvailable('', '2026-07-27', [], 'AVAILABLE')).toBe(false);
      expect(isRangeAvailable('2026-07-27', '', [], 'AVAILABLE')).toBe(false);
    });

    it('returns true for available range with no bookings', () => {
      expect(isRangeAvailable('2026-07-27', '2026-07-30', [], 'AVAILABLE')).toBe(true);
    });

    it('returns false when any night is not available', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-28', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      expect(isRangeAvailable('2026-07-27', '2026-07-29', ranges, 'AVAILABLE')).toBe(false);
    });

    it('returns true when checking nights do not overlap (half-open)', () => {
      const ranges: BookedRange[] = [
        { checkIn: '2026-07-27', checkOut: '2026-07-30', bookingStatus: 'CONFIRMED' },
      ];
      // Range ends on 2026-07-30, so 2026-07-30 is available
      expect(isRangeAvailable('2026-07-30', '2026-08-02', ranges, 'AVAILABLE')).toBe(true);
    });

    it('returns false when room is blocked without dayStatuses', () => {
      expect(isRangeAvailable('2026-07-27', '2026-07-30', [], 'MAINTENANCE')).toBe(false);
      expect(isRangeAvailable('2026-07-27', '2026-07-30', [], 'OUT_OF_SERVICE')).toBe(false);
    });

    it('does not check room block when dayStatuses provided', () => {
      const dayStatuses = {
        '2026-07-27': 'available' as DayStatus,
        '2026-07-28': 'available' as DayStatus,
        '2026-07-29': 'available' as DayStatus,
      };
      // Room is blocked but dayStatuses override
      expect(isRangeAvailable('2026-07-27', '2026-07-30', [], 'MAINTENANCE', dayStatuses)).toBe(true);
    });

    it('returns false when dayStatuses indicate unavailable days', () => {
      const dayStatuses = {
        '2026-07-27': 'available' as DayStatus,
        '2026-07-28': 'maintenance' as DayStatus,
        '2026-07-29': 'available' as DayStatus,
      };
      expect(isRangeAvailable('2026-07-27', '2026-07-30', [], 'AVAILABLE', dayStatuses)).toBe(false);
    });

    it('checks all nights in half-open range', () => {
      const dayStatuses = {
        '2026-07-27': 'available' as DayStatus,
        '2026-07-28': 'available' as DayStatus,
        '2026-07-29': 'available' as DayStatus,
        '2026-07-30': 'maintenance' as DayStatus, // Not checked, it's checkOut
      };
      expect(isRangeAvailable('2026-07-27', '2026-07-30', [], 'AVAILABLE', dayStatuses)).toBe(true);
    });
  });

  describe('addMonths', () => {
    it('adds positive months correctly', () => {
      const result = addMonths(2026, 6, 1); // July + 1
      expect(result.year).toBe(2026);
      expect(result.month).toBe(7); // August
    });

    it('subtracts negative months correctly', () => {
      const result = addMonths(2026, 6, -1); // July - 1
      expect(result.year).toBe(2026);
      expect(result.month).toBe(5); // June
    });

    it('handles year rollover forward', () => {
      const result = addMonths(2026, 11, 1); // December + 1
      expect(result.year).toBe(2027);
      expect(result.month).toBe(0); // January
    });

    it('handles year rollover backward', () => {
      const result = addMonths(2026, 0, -1); // January - 1
      expect(result.year).toBe(2025);
      expect(result.month).toBe(11); // December
    });

    it('handles zero delta', () => {
      const result = addMonths(2026, 6, 0);
      expect(result.year).toBe(2026);
      expect(result.month).toBe(6);
    });

    it('handles large positive delta', () => {
      const result = addMonths(2026, 0, 25); // January + 25 months
      expect(result.year).toBe(2028);
      expect(result.month).toBe(1); // February
    });

    it('handles large negative delta', () => {
      const result = addMonths(2026, 11, -25); // December - 25 months
      expect(result.year).toBe(2024);
      expect(result.month).toBe(10); // November
    });
  });
});
