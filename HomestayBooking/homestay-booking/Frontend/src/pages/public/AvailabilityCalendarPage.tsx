import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import PublicLayout from '../../layouts/PublicLayout';
import Alert from '../../components/ui/Alert';
import RoomAvailabilityCalendar, { RoomCalendarLegend } from '../../components/rooms/RoomAvailabilityCalendar';
import {
  fetchRoomById,
  fetchRoomCalendar,
  fetchRoomMonthAvailability,
  type BookedRange,
  type RoomDetail,
} from '../../api/roomsApi';
import { useAuthStore } from '../../store/authStore';
import {
  addMonths,
  apiStatusToDayStatus,
  dateKey,
  isRangeAvailable,
  isRoomBookingBlocked,
  MONTH_NAMES_VI,
  type DayStatus,
} from '../../utils/roomCalendar';

function monthBounds(year: number, month: number) {
  const lastDay = new Date(year, month + 1, 0).getDate();
  return {
    start: dateKey(year, month, 1),
    end: dateKey(year, month, lastDay),
  };
}

function visibleWindow(year: number, month: number, dualMonth: boolean) {
  const first = monthBounds(year, month);
  if (!dualMonth) return first;
  const second = addMonths(year, month, 1);
  const last = monthBounds(second.year, second.month);
  return { start: first.start, end: last.end };
}

export default function AvailabilityCalendarPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isAuthenticated, role } = useAuthStore();

  const today = useMemo(() => new Date(), []);
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());
  const [isWide, setIsWide] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(min-width: 1024px)').matches : false,
  );

  const [range, setRange] = useState({
    start: searchParams.get('checkIn') || '',
    end: searchParams.get('checkOut') || '',
  });
  const [guests] = useState(Math.max(1, Number(searchParams.get('guests')) || 1));

  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [roomStatus, setRoomStatus] = useState('AVAILABLE');
  const [bookedRanges, setBookedRanges] = useState<BookedRange[]>([]);
  const [dayStatuses, setDayStatuses] = useState<Record<string, DayStatus> | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [monthLoading, setMonthLoading] = useState(false);
  const [error, setError] = useState('');
  const [rangeError, setRangeError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  const secondMonth = addMonths(viewYear, viewMonth, 1);
  const windowRange = visibleWindow(viewYear, viewMonth, isWide);

  useEffect(() => {
    const mq = window.matchMedia('(min-width: 1024px)');
    const onChange = () => setIsWide(mq.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  const loadMonthAvailability = useCallback(async (roomId: string, start: string, end: string) => {
    setMonthLoading(true);
    try {
      const data = await fetchRoomMonthAvailability(roomId, start, end);
      if (data.days && data.days.length > 0) {
        const map: Record<string, DayStatus> = {};
        for (const d of data.days) {
          map[d.date] = apiStatusToDayStatus(d.status);
        }
        setDayStatuses(map);
      }
    } catch {
      // Month API optional — fall back to bookedRanges + roomStatus
    } finally {
      setMonthLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError('');
      try {
        const [roomData, calendarData] = await Promise.all([
          fetchRoomById(id!),
          fetchRoomCalendar(id!),
        ]);
        if (cancelled) return;
        setRoom(roomData);
        setRoomStatus(calendarData.roomStatus);
        setBookedRanges(calendarData.bookedRanges);
      } catch {
        if (!cancelled) setError('Không thể tải dữ liệu phòng. Vui lòng thử lại.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => { cancelled = true; };
  }, [id, reloadKey]);

  useEffect(() => {
    if (!id || loading) return;
    loadMonthAvailability(id, windowRange.start, windowRange.end);
  }, [id, loading, windowRange.start, windowRange.end, loadMonthAvailability]);

  // FR-012: refresh month days within 30s until FR-15 WebSocket
  useEffect(() => {
    if (!id || loading) return;
    const timer = window.setInterval(() => {
      void loadMonthAvailability(id, windowRange.start, windowRange.end);
    }, 30_000);
    return () => window.clearInterval(timer);
  }, [id, loading, windowRange.start, windowRange.end, loadMonthAvailability]);

  useEffect(() => {
    if (!range.start || !range.end) {
      setRangeError('');
      return;
    }
    if (new Date(range.end) <= new Date(range.start)) {
      setRangeError('Ngày check-out phải sau ngày check-in.');
      return;
    }
    if (!isRangeAvailable(range.start, range.end, bookedRanges, roomStatus, dayStatuses)) {
      setRangeError('Phòng không còn trống trong khoảng ngày đã chọn.');
      return;
    }
    setRangeError('');
  }, [range, bookedRanges, roomStatus, dayStatuses]);

  const minNav = { year: today.getFullYear(), month: today.getMonth() };
  const canGoPrev =
    viewYear > minNav.year || (viewYear === minNav.year && viewMonth > minNav.month);

  function goPrev() {
    if (!canGoPrev) return;
    const prev = addMonths(viewYear, viewMonth, -1);
    setViewYear(prev.year);
    setViewMonth(prev.month);
  }

  function goNext() {
    const next = addMonths(viewYear, viewMonth, 1);
    setViewYear(next.year);
    setViewMonth(next.month);
  }

  function handleDateClick(date: string) {
    setRangeError('');
    if (!range.start || (range.start && range.end)) {
      setRange({ start: date, end: '' });
    } else if (date < range.start) {
      setRange({ start: date, end: range.start });
    } else {
      setRange({ start: range.start, end: date });
    }
  }

  const nights =
    range.start && range.end && !rangeError
      ? Math.max(0, Math.ceil((new Date(range.end).getTime() - new Date(range.start).getTime()) / 86400000))
      : 0;

  const pricePerNight = room?.pricePerNight ?? 0;
  const totalAmount = nights * pricePerNight;
  const depositAmount = Math.round(totalAmount * 0.4);
  const canBookRoom = !isRoomBookingBlocked(roomStatus);
  const hasValidRange = nights > 0 && !rangeError;

  function buildQuery() {
    return `checkIn=${range.start}&checkOut=${range.end}&guests=${guests}`;
  }

  function handleApplyDates() {
    if (!id || !hasValidRange) return;
    navigate(`/rooms/${id}?${buildQuery()}`);
  }

  function handleBook() {
    if (!id || !hasValidRange) return;
    const qs = buildQuery();
    if (!isAuthenticated || role !== 'CUSTOMER') {
      navigate(`/login?redirect=${encodeURIComponent(`/request-booking/${id}?${qs}`)}`);
      return;
    }
    navigate(`/request-booking/${id}?${qs}`);
  }

  if (loading) {
    return (
      <PublicLayout>
        <div className="container-wide" style={{ paddingTop: 64, textAlign: 'center' }}>
          <p className="body-lg text-charcoal">Đang tải lịch trống...</p>
        </div>
      </PublicLayout>
    );
  }

  if (error || !room) {
    return (
      <PublicLayout>
        <div className="container-wide" style={{ padding: '80px 0', maxWidth: 560, margin: '0 auto' }}>
          <Alert variant="error" message={error || 'Không tìm thấy phòng.'} />
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap', marginTop: 20 }}>
            <button type="button" className="btn-primary" onClick={() => { setError(''); setReloadKey((k) => k + 1); }}>
              Thử lại
            </button>
            <Link to="/rooms" className="btn-outline">Quay lại danh sách phòng</Link>
          </div>
        </div>
      </PublicLayout>
    );
  }

  const titleSuffix = isWide
    ? `${MONTH_NAMES_VI[viewMonth]} ${viewYear} – ${MONTH_NAMES_VI[secondMonth.month]} ${secondMonth.year}`
    : `${MONTH_NAMES_VI[viewMonth]} ${viewYear}`;

  return (
    <PublicLayout>
      <div className="container-wide" style={{ paddingTop: 32, paddingBottom: 64 }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 24, flexWrap: 'wrap' }}>
          <Link to="/" className="text-primary" style={{ textDecoration: 'none' }}>Trang chủ</Link>
          <span>›</span>
          <Link to="/rooms" className="text-primary" style={{ textDecoration: 'none' }}>Phòng</Link>
          <span>›</span>
          <Link to={`/rooms/${id}`} className="text-primary" style={{ textDecoration: 'none' }}>{room.roomNumber}</Link>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600 }}>Lịch trống</span>
        </div>

        <div
          className="card"
          style={{
            padding: '16px 20px',
            marginBottom: 28,
            display: 'flex',
            flexWrap: 'wrap',
            gap: 16,
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <h1 className="heading-md">{room.roomNumber} — Lịch trống</h1>
            <p className="body-sm text-charcoal">{room.propertyName}</p>
          </div>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            <div>
              <p className="body-sm text-charcoal">Loại phòng</p>
              <p style={{ fontWeight: 600 }}>{room.roomType}</p>
            </div>
            <div>
              <p className="body-sm text-charcoal">Sức chứa</p>
              <p style={{ fontWeight: 600 }}>{room.capacity} khách</p>
            </div>
            <div>
              <p className="body-sm text-charcoal">Diện tích</p>
              <p style={{ fontWeight: 600 }}>{room.area} m²</p>
            </div>
            <div>
              <p className="body-sm text-charcoal">Giá</p>
              <p className="text-primary" style={{ fontWeight: 700 }}>
                ₫{Number(pricePerNight).toLocaleString('vi-VN')}/đêm
              </p>
            </div>
            {!canBookRoom && (
              <div>
                <p className="body-sm text-charcoal">Trạng thái</p>
                <span className="badge badge-warning">{room.status}</span>
              </div>
            )}
          </div>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: isWide ? '1fr 320px' : '1fr',
            gap: 28,
            alignItems: 'flex-start',
          }}
        >
          <div className="card-lg" style={{ padding: 28, boxShadow: '0 4px 16px rgba(32,32,32,0.06)' }}>
            <div className="room-calendar-header" style={{ marginBottom: 20 }}>
              <button
                type="button"
                className="room-calendar-nav-btn"
                onClick={goPrev}
                disabled={!canGoPrev}
                aria-label="Tháng trước"
              >
                ‹
              </button>
              <h2 className="heading-sm font-display room-calendar-title">{titleSuffix}</h2>
              <button
                type="button"
                className="room-calendar-nav-btn"
                onClick={goNext}
                aria-label="Tháng sau"
              >
                ›
              </button>
            </div>

            {monthLoading && (
              <p className="body-sm text-charcoal" style={{ marginBottom: 12 }}>Đang cập nhật lịch...</p>
            )}

            <div className={isWide ? 'room-mini-calendar-dual' : ''}>
              <RoomAvailabilityCalendar
                roomStatus={roomStatus}
                bookedRanges={bookedRanges}
                dayStatuses={dayStatuses}
                year={viewYear}
                month={viewMonth}
                showNavigation={false}
                selectable
                selectedRange={range}
                onDateClick={handleDateClick}
                cellSize={isWide ? 48 : 44}
                showLegend={false}
              />
              {isWide && (
                <RoomAvailabilityCalendar
                  roomStatus={roomStatus}
                  bookedRanges={bookedRanges}
                  dayStatuses={dayStatuses}
                  year={secondMonth.year}
                  month={secondMonth.month}
                  showNavigation={false}
                  selectable
                  selectedRange={range}
                  onDateClick={handleDateClick}
                  cellSize={48}
                  showLegend={false}
                />
              )}
            </div>

            <div style={{ marginTop: 24, paddingTop: 20, borderTop: '1px solid var(--hairline)' }}>
              <RoomCalendarLegend />
            </div>
          </div>

          <div className="room-detail-booking">
            <div className="card-lg" style={{ padding: 24, boxShadow: '0 8px 24px rgba(32,32,32,0.08)' }}>
              <h3 className="heading-sm" style={{ marginBottom: 16 }}>Tóm tắt đặt phòng</h3>

              {range.start ? (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                    <span className="body-sm text-charcoal">Check-in</span>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{range.start}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                    <span className="body-sm text-charcoal">Check-out</span>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>
                      {range.end || 'Chọn ngày trả phòng'}
                    </span>
                  </div>
                  {nights > 0 && (
                    <>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                        <span className="body-sm text-charcoal">Số đêm</span>
                        <span style={{ fontWeight: 600, fontSize: 14 }}>{nights}</span>
                      </div>
                      <div style={{ borderTop: '1px solid var(--hairline)', paddingTop: 12, marginTop: 12 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                          <span className="body-sm text-charcoal">Tổng cộng</span>
                          <span style={{ fontWeight: 700 }}>₫{totalAmount.toLocaleString('vi-VN')}</span>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span className="body-sm text-charcoal">Cọc yêu cầu (40%)</span>
                          <span className="text-primary" style={{ fontWeight: 700 }}>
                            ₫{depositAmount.toLocaleString('vi-VN')}
                          </span>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '20px 0', color: 'var(--charcoal)' }}>
                  <div style={{ fontSize: 32, marginBottom: 8 }}>📅</div>
                  <p className="body-sm">Chọn ngày check-in trên lịch</p>
                </div>
              )}

              {rangeError && (
                <div style={{ marginTop: 16 }}>
                  <Alert variant="warning" message={rangeError} />
                </div>
              )}

              {!canBookRoom && (
                <div style={{ marginTop: 16 }}>
                  <Alert variant="warning" message="Phòng hiện không khả dụng để đặt." />
                </div>
              )}

              <div style={{ marginTop: 20, display: 'flex', flexDirection: 'column', gap: 8 }}>
                <button
                  type="button"
                  className="btn-primary"
                  style={{ width: '100%', height: 44 }}
                  onClick={handleApplyDates}
                  disabled={!hasValidRange}
                >
                  Áp dụng ngày
                </button>

                {canBookRoom && (
                  !isAuthenticated || role !== 'CUSTOMER' ? (
                    <button
                      type="button"
                      className="btn-outline"
                      style={{ width: '100%', height: 44 }}
                      onClick={handleBook}
                      disabled={!hasValidRange}
                    >
                      Đăng nhập để đặt
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn-outline"
                      style={{ width: '100%', height: 44 }}
                      onClick={handleBook}
                      disabled={!hasValidRange}
                    >
                      Đặt phòng
                    </button>
                  )
                )}

                <Link
                  to={`/rooms/${id}`}
                  className="btn-ghost"
                  style={{ width: '100%', justifyContent: 'center', display: 'flex' }}
                >
                  ← Quay lại chi tiết phòng
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PublicLayout>
  );
}
