import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faDoorOpen,
  faChartLine,
  faCalendarCheck,
  faClipboardCheck,
  faChartPie,
  faChartArea,
} from '@fortawesome/free-solid-svg-icons';
import ManagerLayout from '../../layouts/ManagerLayout';
import Alert from '../../components/ui/Alert';
import { KpiCard } from '../../components/ui/KpiCard';
import { managerApi } from '../../api/managerApi';
import { reportApi, type PropertyKpis, type AssignedProperty } from '../../api/reportApi';

// ── Helpers ───────────────────────────────────────────────────────────────────

const PIE_COLORS = ['#0F766E', '#2563EB', '#F59E0B', '#7C3AED', '#EF4444', '#64748B', '#0891B2'];

const BOOKING_STATUS_VI: Record<string, string> = {
  PENDING_DEPOSIT: 'Chờ cọc',
  CONFIRMED: 'Đã xác nhận',
  CHECKED_IN: 'Đang ở',
  PENDING_INSPECTION: 'Chờ kiểm tra',
  PENDING_DAMAGE_PAYMENT: 'Chờ thanh toán hư hại',
  CHECKED_OUT: 'Đã trả phòng',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Không đến',
};

function formatVnd(value: number): string {
  return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
}

function sixMonthsRange(): { from: string; to: string } {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth() - 5, 1);
  const to = now;
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  return { from: fmt(from), to: fmt(to) };
}

// ── Sub-components ────────────────────────────────────────────────────────────

function ChartSkeleton() {
  return (
    <div className="h-[280px] bg-[#F1F5F9] rounded-[16px] animate-pulse" />
  );
}

function EmptyChart({ message }: { message: string }) {
  return (
    <div className="h-[280px] flex flex-col items-center justify-center text-[#94A3B8] gap-2">
      <FontAwesomeIcon icon={faChartArea} style={{ fontSize: 42 }} />
      <p className="text-sm">{message}</p>
    </div>
  );
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function ManagerDashboardPage() {
  const navigate = useNavigate();

  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propertyId, setPropertyId] = useState('');
  const [kpis, setKpis] = useState<PropertyKpis | null>(null);
  const [revenueData, setRevenueData] = useState<{ period: string; revenue: number }[]>([]);
  const [pieData, setPieData] = useState<{ name: string; value: number }[]>([]);

  const [loadingProps, setLoadingProps] = useState(true);
  const [loadingData, setLoadingData] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load assigned properties
  useEffect(() => {
    setLoadingProps(true);
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data) {
          setProperties(res.data);
          if (res.data.length > 0) {
            setPropertyId(res.data[0].id);
          }
        }
      })
      .catch(() => setError('Không thể tải danh sách homestay.'))
      .finally(() => setLoadingProps(false));
  }, []);

  const fetchDashboard = useCallback(async (pid: string) => {
    if (!pid) return;
    setLoadingData(true);
    setError(null);
    try {
      const { from, to } = sixMonthsRange();
      const [kpiRes, revRes, pieRes] = await Promise.all([
        reportApi.getPropertyKpis(pid),
        reportApi.getRevenue({ propertyId: pid, from, to, groupBy: 'month' }),
        reportApi.getBookingStatusBreakdown(pid),
      ]);

      if (kpiRes.success && kpiRes.data) {
        setKpis(kpiRes.data);
      } else {
        setError(kpiRes.message || 'Không thể tải KPI.');
      }

      if (revRes.success && revRes.data) {
        setRevenueData(
          revRes.data.byPeriod.map(p => ({ period: p.period, revenue: p.revenue }))
        );
      } else {
        setRevenueData([]);
      }

      if (pieRes.success && pieRes.data) {
        setPieData(
          pieRes.data.map(item => ({
            name: BOOKING_STATUS_VI[item.status] || item.status,
            value: item.count,
          }))
        );
      } else {
        setPieData([]);
      }
    } catch (err: unknown) {
      const ax = err as { response?: { status?: number; data?: { message?: string } } };
      if (ax?.response?.status === 403) {
        setError('Bạn không có quyền xem homestay này.');
      } else {
        setError(ax?.response?.data?.message || 'Lỗi kết nối. Vui lòng thử lại.');
      }
    } finally {
      setLoadingData(false);
    }
  }, []);

  useEffect(() => {
    if (propertyId) fetchDashboard(propertyId);
  }, [propertyId, fetchDashboard]);

  const isLoading = loadingProps || loadingData;

  return (
    <ManagerLayout>
      <div className="space-y-6">
        {/* Header + Property Selector */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <h1 className="font-display text-[28px] font-bold text-[#1E293B]">
            Bảng điều khiển
          </h1>
          {properties.length > 0 && (
            <select
              className="border border-[#E2E8F0] rounded-md px-4 py-2 text-sm bg-white text-[#1E293B] min-w-[220px]"
              value={propertyId}
              onChange={e => setPropertyId(e.target.value)}
              disabled={loadingData}
            >
              {properties.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          )}
        </div>

        {error && <Alert variant="error" message={error} closeable onClose={() => setError(null)} />}

        {/* Empty: no assigned properties */}
        {!loadingProps && properties.length === 0 && (
          <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-12 text-center">
            <p className="text-[#64748B]">Bạn chưa được gán homestay nào.</p>
            <p className="text-sm text-[#94A3B8] mt-2">Liên hệ Admin để được phân công quản lý.</p>
          </div>
        )}

        {/* KPI Cards */}
        {properties.length > 0 && (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
              {isLoading && !kpis ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <div key={i} className="h-[120px] bg-[#F1F5F9] rounded-[16px] animate-pulse" />
                ))
              ) : kpis ? (
                <>
                  <button type="button" onClick={() => navigate('/manager/rooms')} className="text-left">
                    <KpiCard title="Tổng phòng" value={kpis.totalRooms} icon={faDoorOpen} />
                  </button>
                  <button type="button" onClick={() => navigate('/manager/rooms')} className="text-left">
                    <KpiCard title="Tỷ lệ lấp đầy" value={`${kpis.occupancyRate}%`} icon={faChartLine} />
                  </button>
                  <button type="button" onClick={() => navigate('/manager/bookings')} className="text-left">
                    <KpiCard title="Check-in hôm nay" value={kpis.pendingCheckIns} icon={faCalendarCheck} />
                  </button>
                  <button type="button" onClick={() => navigate('/manager/payments')} className="text-left">
                    <KpiCard title="Chờ duyệt" value={kpis.pendingApprovals} icon={faClipboardCheck} />
                  </button>
                </>
              ) : null}
            </div>

            {/* Revenue summary inline */}
            {kpis && (
              <p className="text-sm text-[#64748B]">
                Doanh thu tháng này:{' '}
                <span className="font-semibold text-[#0F766E]">{formatVnd(kpis.revenue)}</span>
              </p>
            )}

            {/* Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Revenue Line Chart */}
              <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6 shadow-[0_1px_2px_rgba(0,0,0,0.05)]">
                <h2 className="text-[16px] font-semibold text-[#1E293B] mb-4">Doanh thu 6 tháng gần nhất</h2>
                {loadingData ? (
                  <ChartSkeleton />
                ) : revenueData.length === 0 ? (
                  <EmptyChart message="Chưa có dữ liệu doanh thu" />
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <LineChart data={revenueData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                      <XAxis dataKey="period" tick={{ fontSize: 12, fill: '#64748B' }} />
                      <YAxis tick={{ fontSize: 12, fill: '#64748B' }} tickFormatter={v => `${(v / 1_000_000).toFixed(0)}M`} />
                      <Tooltip formatter={(v) => formatVnd(Number(v ?? 0))} />
                      <Line type="monotone" dataKey="revenue" stroke="#0F766E" strokeWidth={2} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </div>

              {/* Booking Status Pie Chart */}
              <div className="bg-white rounded-[16px] border border-[#E2E8F0] p-6 shadow-[0_1px_2px_rgba(0,0,0,0.05)]">
                <h2 className="text-[16px] font-semibold text-[#1E293B] mb-4">Phân bổ đặt phòng tháng này</h2>
                {loadingData ? (
                  <ChartSkeleton />
                ) : pieData.length === 0 ? (
                  <EmptyChart message="Chưa có đặt phòng trong tháng" />
                ) : (
                  <ResponsiveContainer width="100%" height={280}>
                    <PieChart>
                      <Pie
                        data={pieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={90}
                        label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                      >
                        {pieData.map((_, i) => (
                          <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </ManagerLayout>
  );
}
