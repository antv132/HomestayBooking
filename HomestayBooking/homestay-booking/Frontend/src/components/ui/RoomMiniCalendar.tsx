import { useEffect, useMemo, useState } from 'react';
import RoomAvailabilityCalendar, { RoomCalendarLegend } from '../rooms/RoomAvailabilityCalendar';
import { fetchRoomMonthAvailability } from '../../api/roomsApi';
import {
  addMonths,
  apiStatusToDayStatus,
  dateKey,
  type BookedRange,
  type DayStatus,
} from '../../utils/roomCalendar';

export type { BookedRange, DayStatus } from '../../utils/roomCalendar';
export {
  DAY_STYLES,
  isRangeAvailable,
  isRoomBookingBlocked,
  parseLocalDate,
  statusForDay,
} from '../../utils/roomCalendar';

interface RoomMiniCalendarProps {
  roomId?: string;
  roomStatus: string;
  bookedRanges: BookedRange[];
  /** Optional preloaded FR-05 days[] map; if omitted and roomId set, fetches month API. */
  dayStatuses?: Record<string, DayStatus>;
  onDayStatusesLoaded?: (map: Record<string, DayStatus>) => void;
}

function monthBounds(year: number, month: number) {
  const lastDay = new Date(year, month + 1, 0).getDate();
  return {
    start: dateKey(year, month, 1),
    end: dateKey(year, month, lastDay),
  };
}

/**
 * SCR-09 mini dual-month calendar. Prefers API days[] (same endpoint as full calendar)
 * over snapshot room.status + bookedRanges for occupancy coloring.
 */
export default function RoomMiniCalendar({
  roomId,
  roomStatus,
  bookedRanges,
  dayStatuses: dayStatusesProp,
  onDayStatusesLoaded,
}: RoomMiniCalendarProps) {
  const today = useMemo(() => new Date(), []);
  const thisYear = today.getFullYear();
  const thisMonth = today.getMonth();
  const next = addMonths(thisYear, thisMonth, 1);

  const [fetchedStatuses, setFetchedStatuses] = useState<Record<string, DayStatus> | undefined>();

  useEffect(() => {
    if (!roomId || dayStatusesProp) return;
    let cancelled = false;
    const first = monthBounds(thisYear, thisMonth);
    const second = monthBounds(next.year, next.month);

    fetchRoomMonthAvailability(roomId, first.start, second.end)
      .then((data) => {
        if (cancelled || !data.days?.length) return;
        const map: Record<string, DayStatus> = {};
        for (const d of data.days) {
          map[d.date] = apiStatusToDayStatus(d.status);
        }
        setFetchedStatuses(map);
        onDayStatusesLoaded?.(map);
      })
      .catch(() => {
        // Fall back to bookedRanges + roomStatus via statusForDayWithApi
      });

    return () => {
      cancelled = true;
    };
  }, [roomId, dayStatusesProp, thisYear, thisMonth, next.year, next.month, onDayStatusesLoaded]);

  const dayStatuses = dayStatusesProp ?? fetchedStatuses;

  return (
    <div className="card" style={{ padding: 20 }}>
      <div className="room-mini-calendar-dual">
        <RoomAvailabilityCalendar
          roomStatus={roomStatus}
          bookedRanges={bookedRanges}
          dayStatuses={dayStatuses}
          year={thisYear}
          month={thisMonth}
          showNavigation={false}
          cellSize={36}
          showLegend={false}
        />
        <RoomAvailabilityCalendar
          roomStatus={roomStatus}
          bookedRanges={bookedRanges}
          dayStatuses={dayStatuses}
          year={next.year}
          month={next.month}
          showNavigation={false}
          cellSize={36}
          showLegend={false}
        />
      </div>
      <RoomCalendarLegend />
    </div>
  );
}
