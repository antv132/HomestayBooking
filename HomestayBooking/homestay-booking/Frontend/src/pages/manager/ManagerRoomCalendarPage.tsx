import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import StaffRoomMonthCalendar from '../../components/rooms/StaffRoomMonthCalendar';
import {
  fetchManagerRoomCalendar,
  type StaffBookingRangeRef,
  type StaffRoomMonthCalendar as StaffCalendarData,
} from '../../api/roomsApi';

function monthBounds(year: number, month: number) {
  const last = new Date(year, month + 1, 0).getDate();
  const pad = (n: number) => String(n).padStart(2, '0');
  return {
    startDate: `${year}-${pad(month + 1)}-01`,
    endDate: `${year}-${pad(month + 1)}-${pad(last)}`,
  };
}

/** FR-05 US2 / SCR-33 — Manager month view (8 statuses from days[]). */
export default function ManagerRoomCalendarPage() {
  const { id } = useParams<{ id: string }>();
  const today = useMemo(() => new Date(), []);
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());
  const [data, setData] = useState<StaffCalendarData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const bounds = monthBounds(viewYear, viewMonth);
      const calendar = await fetchManagerRoomCalendar(id, bounds);
      setData(calendar);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      setError(
        status === 403
          ? 'Bạn không được gán property của phòng này.'
          : 'Không tải được lịch phòng. Vui lòng thử lại.',
      );
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [id, viewYear, viewMonth]);

  useEffect(() => {
    load();
  }, [load]);

  // FR-012: refresh calendar within 30s until FR-15 WebSocket
  useEffect(() => {
    const timer = window.setInterval(() => {
      void load();
    }, 30_000);
    return () => window.clearInterval(timer);
  }, [load]);

  const bookingRows: StaffBookingRangeRef[] = data?.bookedRanges ?? [];

  return (
    <ManagerLayout>
      <div className="animate-fade-in" style={{ maxWidth: 720 }}>
        <div style={{ marginBottom: 16 }}>
          <Link
            to={id ? `/manager/rooms/${id}` : '/manager/rooms'}
            className="body-sm"
            style={{ color: 'var(--primary)', textDecoration: 'none' }}
          >
            ← Chi tiết phòng
          </Link>
          <h1 className="heading-md font-display" style={{ margin: '8px 0 4px' }}>
            Lịch availability
          </h1>
          <p className="body-sm text-charcoal">
            {data ? `Phòng ${data.roomNumber}` : '…'} — 8 trạng thái theo ngày (FR-05). Chỉ xem;
            cập nhật Maintenance/OOS tại tab Trạng thái.
          </p>
        </div>

        {error && <Alert variant="error" message={error} />}

        <div className="card" style={{ padding: 20, marginBottom: 16 }}>
          {loading && !data ? (
            <p className="body-sm text-charcoal">Đang tải lịch…</p>
          ) : data ? (
            <StaffRoomMonthCalendar
              roomStatus={data.roomStatus}
              days={data.days}
              bookedRanges={data.bookedRanges}
              year={viewYear}
              month={viewMonth}
              onMonthChange={(y, m) => {
                setViewYear(y);
                setViewMonth(m);
              }}
              showBookingRefs
            />
          ) : null}
        </div>

        {bookingRows.length > 0 && (
          <div className="card" style={{ padding: 16 }}>
            <h2 className="heading-sm" style={{ marginBottom: 12 }}>
              Booking trong tháng
            </h2>
            <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
              {bookingRows.map((b) => (
                <li
                  key={b.bookingId}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 12,
                    fontSize: 13,
                    borderBottom: '1px solid var(--hairline)',
                    paddingBottom: 8,
                  }}
                >
                  <Link
                    to={`/manager/bookings/${b.bookingId}`}
                    style={{ color: 'var(--primary)', fontWeight: 600, textDecoration: 'none' }}
                  >
                    #{b.bookingId.slice(0, 8).toUpperCase()}
                  </Link>
                  <span className="text-charcoal">
                    {b.checkIn} → {b.checkOut} · {b.bookingStatus}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div style={{ marginTop: 16, display: 'flex', gap: 10 }}>
          <Link to={`/manager/rooms/${id}/edit?tab=status`} className="btn-outline btn-sm">
            Cập nhật trạng thái
          </Link>
          <button type="button" className="btn-ghost btn-sm" onClick={load} disabled={loading}>
            Làm mới
          </button>
        </div>
      </div>
    </ManagerLayout>
  );
}
