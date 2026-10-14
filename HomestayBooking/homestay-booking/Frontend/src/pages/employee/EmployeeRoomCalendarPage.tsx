import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import StaffRoomMonthCalendar from '../../components/rooms/StaffRoomMonthCalendar';
import {
  getEmployeeRoomCalendar,
  type EmployeeRoomCalendar,
} from '../../api/employeeApi';
import { ErrBanner, Spinner } from './EmployeeShared';

function monthBounds(year: number, month: number) {
  const last = new Date(year, month + 1, 0).getDate();
  const pad = (n: number) => String(n).padStart(2, '0');
  return {
    startDate: `${year}-${pad(month + 1)}-01`,
    endDate: `${year}-${pad(month + 1)}-${pad(last)}`,
  };
}

/** FR-05 US4 — Employee read-only room calendar (assigned property). */
export default function EmployeeRoomCalendarPage() {
  const { id } = useParams<{ id: string }>();
  const today = useMemo(() => new Date(), []);
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth());
  const [data, setData] = useState<EmployeeRoomCalendar | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const calendar = await getEmployeeRoomCalendar(id, monthBounds(viewYear, viewMonth));
      setData(calendar);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      setError(
        status === 403
          ? 'Bạn không được gán property của phòng này.'
          : 'Không tải được lịch phòng.',
      );
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [id, viewYear, viewMonth]);

  useEffect(() => {
    load();
  }, [load]);

  // FR-012
  useEffect(() => {
    const timer = window.setInterval(() => {
      void load();
    }, 30_000);
    return () => window.clearInterval(timer);
  }, [load]);

  return (
    <EmployeeLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 16 }}>
          <Link to="/employee/rooms" className="body-sm" style={{ color: 'var(--primary)', textDecoration: 'none' }}>
            ← Danh sách phòng
          </Link>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 22, fontWeight: 700, color: 'var(--ink)', margin: '8px 0 4px' }}>
            Lịch phòng
          </h1>
          <p className="body-sm text-charcoal">
            {data ? `Phòng ${data.roomNumber}` : '…'} — chỉ xem (FR-05 US4). Không cập nhật trạng thái từ đây.
          </p>
        </div>

        {error && <ErrBanner msg={error} />}
        {loading && !data ? (
          <Spinner />
        ) : data ? (
          <div className="card" style={{ padding: 16 }}>
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
              showBookingRefs={false}
            />
          </div>
        ) : null}
      </div>
    </EmployeeLayout>
  );
}
