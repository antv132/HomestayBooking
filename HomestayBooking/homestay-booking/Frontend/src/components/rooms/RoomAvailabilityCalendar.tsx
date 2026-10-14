import {
  DAY_STYLES,
  LEGEND_STATUSES,
  MONTH_NAMES_VI,
  WEEKDAY_LABELS,
  addMonths,
  dateKey,
  statusForDayWithApi,
  type BookedRange,
  type DayStatus,
} from '../../utils/roomCalendar';

export interface SelectedRange {
  start: string;
  end: string;
}

interface RoomAvailabilityCalendarProps {
  roomStatus: string;
  bookedRanges: BookedRange[];
  /** FR-05 days[] from month availability API — preferred over local range paint */
  dayStatuses?: Record<string, DayStatus>;
  /** Optional per-day tooltip (e.g. booking ref for manager) */
  dayTitles?: Record<string, string>;
  year: number;
  month: number;
  onMonthChange?: (year: number, month: number) => void;
  showNavigation?: boolean;
  selectable?: boolean;
  selectedRange?: SelectedRange;
  onDateClick?: (isoDate: string) => void;
  cellSize?: number;
  showLegend?: boolean;
}

function inSelectedRange(key: string, selectedRange?: SelectedRange) {
  if (!selectedRange?.start) return false;
  if (!selectedRange.end) return key === selectedRange.start;
  return key >= selectedRange.start && key <= selectedRange.end;
}

export function RoomCalendarLegend() {
  return (
    <div className="room-calendar-legend">
      {LEGEND_STATUSES.map((st) => (
        <span key={st} className="badge badge-tag room-calendar-legend-chip">
          <span
            className="room-calendar-legend-dot"
            style={{ background: DAY_STYLES[st].bg, borderColor: DAY_STYLES[st].color }}
          />
          {DAY_STYLES[st].label}
        </span>
      ))}
    </div>
  );
}

function MonthGrid({
  year,
  month,
  ranges,
  roomStatus,
  dayStatuses,
  dayTitles,
  selectable,
  selectedRange,
  onDateClick,
  cellSize,
}: {
  year: number;
  month: number;
  ranges: BookedRange[];
  roomStatus: string;
  dayStatuses?: Record<string, DayStatus>;
  dayTitles?: Record<string, string>;
  selectable?: boolean;
  selectedRange?: SelectedRange;
  onDateClick?: (isoDate: string) => void;
  cellSize: number;
}) {
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells: (number | null)[] = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  function dayStatus(key: string): DayStatus {
    return statusForDayWithApi(key, dayStatuses, ranges, roomStatus);
  }

  function cellStyle(day: number | null): React.CSSProperties {
    if (!day) return { minHeight: cellSize };
    const key = dateKey(year, month, day);
    const st: DayStatus = dayStatus(key);
    const base = DAY_STYLES[st];
    const selected = selectable && inSelectedRange(key, selectedRange);

    if (st !== 'available') {
      return {
        minHeight: cellSize,
        background: base.bg,
        color: base.color,
        cursor: 'not-allowed',
        fontWeight: 600,
      };
    }
    if (selected) {
      return {
        minHeight: cellSize,
        background: 'var(--primary)',
        color: '#fff',
        cursor: 'pointer',
        fontWeight: 700,
        boxShadow: '0 0 0 2px rgba(234,40,4,0.25)',
      };
    }
    return {
      minHeight: cellSize,
      background: base.bg,
      color: base.color,
      cursor: selectable ? 'pointer' : 'default',
      fontWeight: 600,
    };
  }

  function handleClick(day: number | null) {
    if (!day || !selectable || !onDateClick) return;
    const key = dateKey(year, month, day);
    if (dayStatus(key) !== 'available') return;
    onDateClick(key);
  }

  return (
    <div className="room-calendar-grid">
      {WEEKDAY_LABELS.map((d) => (
        <div key={d} className="room-calendar-weekday">
          {d}
        </div>
      ))}
      {cells.map((day, i) => (
        <div
          key={i}
          role={day && selectable ? 'button' : undefined}
          tabIndex={day && selectable ? 0 : undefined}
          className="room-calendar-cell"
          onClick={() => handleClick(day)}
          onKeyDown={(e) => day && selectable && (e.key === 'Enter' || e.key === ' ') && handleClick(day)}
          style={cellStyle(day)}
          title={
            day
              ? dayTitles?.[dateKey(year, month, day)]
                ?? DAY_STYLES[dayStatus(dateKey(year, month, day))].label
              : undefined
          }
        >
          {day || ''}
        </div>
      ))}
    </div>
  );
}

export default function RoomAvailabilityCalendar({
  roomStatus,
  bookedRanges,
  dayStatuses,
  dayTitles,
  year,
  month,
  onMonthChange,
  showNavigation = true,
  selectable = false,
  selectedRange,
  onDateClick,
  cellSize = 48,
  showLegend = true,
}: RoomAvailabilityCalendarProps) {
  const today = new Date();
  const minNav = { year: today.getFullYear(), month: today.getMonth() };
  const canGoPrev =
    year > minNav.year || (year === minNav.year && month > minNav.month);

  function goPrev() {
    if (!canGoPrev || !onMonthChange) return;
    const next = addMonths(year, month, -1);
    onMonthChange(next.year, next.month);
  }

  function goNext() {
    if (!onMonthChange) return;
    const next = addMonths(year, month, 1);
    onMonthChange(next.year, next.month);
  }

  return (
    <div>
      <div className="room-calendar-header">
        {showNavigation ? (
          <button
            type="button"
            className="room-calendar-nav-btn"
            onClick={goPrev}
            disabled={!canGoPrev}
            aria-label="Tháng trước"
          >
            ‹
          </button>
        ) : (
          <span style={{ width: 36 }} />
        )}
        <h3 className="heading-sm font-display room-calendar-title">
          {MONTH_NAMES_VI[month]} {year}
        </h3>
        {showNavigation ? (
          <button
            type="button"
            className="room-calendar-nav-btn"
            onClick={goNext}
            aria-label="Tháng sau"
          >
            ›
          </button>
        ) : (
          <span style={{ width: 36 }} />
        )}
      </div>

      <MonthGrid
        year={year}
        month={month}
        ranges={bookedRanges}
        roomStatus={roomStatus}
        dayStatuses={dayStatuses}
        dayTitles={dayTitles}
        selectable={selectable}
        selectedRange={selectedRange}
        onDateClick={onDateClick}
        cellSize={cellSize}
      />

      {showLegend && <RoomCalendarLegend />}
    </div>
  );
}
