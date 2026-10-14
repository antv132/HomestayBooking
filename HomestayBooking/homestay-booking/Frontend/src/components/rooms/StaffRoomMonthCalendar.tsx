import { useMemo } from 'react';
import RoomAvailabilityCalendar from './RoomAvailabilityCalendar';
import {
  apiStatusToDayStatus,
  dateKey,
  parseLocalDate,
  type DayStatus,
} from '../../utils/roomCalendar';

export interface StaffCalendarDay {
  date: string;
  status: string;
  bookable: boolean;
}

export interface StaffBookingRef {
  bookingId: string;
  checkIn: string;
  checkOut: string;
  bookingStatus: string;
}

interface StaffRoomMonthCalendarProps {
  roomStatus: string;
  days: StaffCalendarDay[];
  bookedRanges?: StaffBookingRef[];
  year: number;
  month: number;
  onMonthChange: (year: number, month: number) => void;
  /** When false, no booking links in tooltips (employee RO). */
  showBookingRefs?: boolean;
}

function shortId(id: string) {
  return id.slice(0, 8).toUpperCase();
}

/** Build dayStatuses + tooltips from staff days[] + optional booking refs. */
export function buildStaffDayMaps(
  days: StaffCalendarDay[],
  bookedRanges: StaffBookingRef[] | undefined,
  showBookingRefs: boolean,
): { dayStatuses: Record<string, DayStatus>; dayTitles: Record<string, string> } {
  const dayStatuses: Record<string, DayStatus> = {};
  const dayTitles: Record<string, string> = {};

  for (const d of days) {
    const st = apiStatusToDayStatus(d.status);
    dayStatuses[d.date] = st;
  }

  if (showBookingRefs && bookedRanges) {
    for (const range of bookedRanges) {
      const start = parseLocalDate(range.checkIn);
      const end = parseLocalDate(range.checkOut);
      for (let d = new Date(start); d < end; d.setDate(d.getDate() + 1)) {
        const key = dateKey(d.getFullYear(), d.getMonth(), d.getDate());
        const label = dayStatuses[key]
          ? `${dayStatuses[key] === 'pending' ? 'Chờ cọc' : dayStatuses[key] === 'occupied' ? 'Đang ở' : 'Đã đặt'} · #${shortId(range.bookingId)}`
          : `Booking #${shortId(range.bookingId)}`;
        dayTitles[key] = label;
      }
    }
  }

  return { dayStatuses, dayTitles };
}

/**
 * Shared Manager/Employee month grid — paints exclusively from API days[] (FR-05).
 * Not selectable; status updates live on SCR-33 status tab, not here.
 */
export default function StaffRoomMonthCalendar({
  roomStatus,
  days,
  bookedRanges = [],
  year,
  month,
  onMonthChange,
  showBookingRefs = true,
}: StaffRoomMonthCalendarProps) {
  const { dayStatuses, dayTitles } = useMemo(
    () => buildStaffDayMaps(days, bookedRanges, showBookingRefs),
    [days, bookedRanges, showBookingRefs],
  );

  return (
    <RoomAvailabilityCalendar
      roomStatus={roomStatus}
      bookedRanges={[]}
      dayStatuses={dayStatuses}
      dayTitles={Object.keys(dayTitles).length > 0 ? dayTitles : undefined}
      year={year}
      month={month}
      onMonthChange={onMonthChange}
      showNavigation
      selectable={false}
      cellSize={44}
      showLegend
    />
  );
}
