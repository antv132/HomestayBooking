// ─── SCR-44: Property Reports (gộp Doanh thu · Tỷ lệ lấp đầy · Xu hướng — Tabs) ──
import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import ManagerLayout from '../../layouts/ManagerLayout';
import DataTable from '../../components/ui/DataTable';
import { managerApi } from '../../api/managerApi';
import {
  reportApi,
  type AssignedProperty,
  type RevenueReportData,
  type OccupancyReportData,
  type BookingTrendData,
} from '../../api/reportApi';

// ── Types ───────────────────────────────────────────────────────────────────

type TabKey = 'revenue' | 'occupancy' | 'bookings';

function parseTab(raw: string | null): TabKey {
  if (raw === 'occupancy' || raw === 'bookings' || raw === 'revenue') return raw;
  return 'revenue';
}

const TABS: { key: TabKey; label: string }[] = [
  { key: 'revenue', label: 'Doanh thu' },
  { key: 'occupancy', label: 'Tỷ lệ lấp đầy' },
  { key: 'bookings', label: 'Xu hướng đặt phòng' },
];

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtVnd(v: number): string {
  return `₫${Math.round(v).toLocaleString('vi-VN')}`;
}
function fmtM(v: number): string {
  if (v >= 1_000_000_000) return `${(v / 1_000_000_000).toFixed(1)}B`;
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`;
  if (v >= 1_000) return `${(v / 1_000).toFixed(0)}K`;
  return `${v}`;
}
function fmtPct(v: number): string {
  return `${v.toFixed(1)}%`;
}
function defaultDateRange(): { from: string; to: string } {
  const to = new Date();
  const from = new Date();
  from.setMonth(from.getMonth() - 5);
  from.setDate(1);
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  return { from: fmt(from), to: fmt(to) };
}
function extractError(err: unknown): string {
  const ax = err as { response?: { status?: number; data?: { message?: string } } };
  if (ax?.response?.status === 403) return 'Bạn không có quyền xem homestay này.';
  return ax?.response?.data?.message || 'Lỗi kết nối. Vui lòng thử lại.';
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function Skeleton({ height = 120 }: { height?: number }) {
  return (
    <div style={{
      background: 'var(--surface-card)', borderRadius: 14, height,
      animation: 'pulse 1.5s ease-in-out infinite',
    }} />
  );
}

// ── KPI Card ──────────────────────────────────────────────────────────────────

function KpiCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="kpi-card">
      <div className="body-sm text-charcoal" style={{ fontWeight: 500, marginBottom: 8 }}>{label}</div>
      <div className="kpi-value" style={{ fontSize: 26, lineHeight: 1.1 }}>{value}</div>
      {sub && <div className="body-sm text-charcoal" style={{ marginTop: 4 }}>{sub}</div>}
    </div>
  );
}

// ── Chart Tooltip ───────────────────────────────────────────────────────────

function ChartTooltip({ active, payload, label, formatter }: {
  active?: boolean;
  payload?: readonly { value?: unknown }[];
  label?: string;
  formatter: (v: number) => string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: '#fff', border: '1px solid var(--hairline)', borderRadius: 8,
      padding: '10px 16px', boxShadow: '0 4px 16px rgba(0,0,0,0.08)', fontSize: 13,
    }}>
      <p style={{ fontWeight: 700, marginBottom: 4, color: 'var(--ink)' }}>{label}</p>
      <p style={{ color: 'var(--primary)', fontWeight: 600 }}>{formatter(Number(payload[0].value))}</p>
    </div>
  );
}

// ── Empty State ───────────────────────────────────────────────────────────────

function EmptyState({ message = 'Không có dữ liệu cho kỳ này' }: { message?: string }) {
  return (
    <div style={{
      height: 260, display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', gap: 8, opacity: 0.4,
    }}>
      <p className="body-md">{message}</p>
      <p className="body-sm">Hãy thử điều chỉnh bộ lọc</p>
    </div>
  );
}

// ── Main Component ─────────────────────────────────────────────────────────────

export function PropertyReportsPage() {
  const defaults = defaultDateRange();
  const [searchParams, setSearchParams] = useSearchParams();

  // Filters
  const [properties, setProperties] = useState<AssignedProperty[]>([]);
  const [propertyId, setPropertyId] = useState<string>('');
  const [from, setFrom] = useState<string>(defaults.from);
  const [to, setTo] = useState<string>(defaults.to);
  const [groupBy, setGroupBy] = useState<'month' | 'week'>('month');
  const [activeTab, setActiveTab] = useState<TabKey>(() => parseTab(searchParams.get('tab')));

  // Applied filters (fetch khi Áp dụng / đổi tab / đổi property)
  const [applied, setApplied] = useState({ from: defaults.from, to: defaults.to, groupBy: 'month' as 'month' | 'week' });

  // Data per tab
  const [revenue, setRevenue] = useState<RevenueReportData | null>(null);
  const [occupancy, setOccupancy] = useState<OccupancyReportData | null>(null);
  const [bookings, setBookings] = useState<BookingTrendData | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function selectTab(tab: TabKey) {
    setActiveTab(tab);
    setSearchParams(prev => {
      const next = new URLSearchParams(prev);
      next.set('tab', tab);
      return next;
    }, { replace: true });
  }

  // ── Load assigned properties ──────────────────────────────────────────────
  useEffect(() => {
    managerApi.getMyAssignedProperties()
      .then(res => {
        if (res.success && res.data.length) {
          setProperties(res.data);
          setPropertyId(res.data[0].id);
        }
      })
      .catch(() => { /* selector lỗi không block UI */ });
  }, []);

  // ── Fetch active tab data ───────────────────────────────────────────────────
  const fetchTab = useCallback(async (
    tab: TabKey,
    pid: string,
    params: { from: string; to: string; groupBy: 'month' | 'week' },
  ) => {
    if (!pid) return;
    setLoading(true);
    setError(null);
    try {
      if (tab === 'revenue') {
        const res = await reportApi.getRevenue({ propertyId: pid, ...params });
        if (res.success && res.data) setRevenue(res.data);
        else setError(res.message || 'Không thể tải dữ liệu.');
      } else if (tab === 'occupancy') {
        const res = await reportApi.getOccupancy({ propertyId: pid, ...params });
        if (res.success && res.data) setOccupancy(res.data);
        else setError(res.message || 'Không thể tải dữ liệu.');
      } else {
        const res = await reportApi.getBookingTrends({ propertyId: pid, ...params });
        if (res.success && res.data) setBookings(res.data);
        else setError(res.message || 'Không thể tải dữ liệu.');
      }
    } catch (err: unknown) {
      setError(extractError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch khi đổi tab / property / applied filters
  useEffect(() => {
    if (propertyId) fetchTab(activeTab, propertyId, applied);
  }, [activeTab, propertyId, applied, fetchTab]);

  function handleApply() {
    setApplied({ from, to, groupBy });
  }

  const selectedPropName = properties.find(p => p.id === propertyId)?.name || '—';

  // ── Chart data theo tab ───────────────────────────────────────────────────
  const chart = (() => {
    if (activeTab === 'revenue') {
      return {
        data: revenue?.byPeriod ?? [],
        dataKey: 'revenue',
        yFmt: fmtM,
        tipFmt: fmtVnd,
        title: `Doanh thu theo ${groupBy === 'month' ? 'tháng' : 'tuần'}`,
        empty: (revenue?.byPeriod?.length ?? 0) === 0,
      };
    }
    if (activeTab === 'occupancy') {
      return {
        data: occupancy?.byPeriod ?? [],
        dataKey: 'occupancyRate',
        yFmt: (v: number) => `${v}%`,
        tipFmt: fmtPct,
        title: `Tỷ lệ lấp đầy theo ${groupBy === 'month' ? 'tháng' : 'tuần'}`,
        empty: (occupancy?.byPeriod?.length ?? 0) === 0,
      };
    }
    return {
      data: bookings?.byPeriod ?? [],
      dataKey: 'bookingCount',
      yFmt: (v: number) => `${v}`,
      tipFmt: (v: number) => `${v} booking`,
      title: `Số booking theo ${groupBy === 'month' ? 'tháng' : 'tuần'}`,
      empty: (bookings?.byPeriod?.length ?? 0) === 0,
    };
  })();

  // ── KPIs theo tab ───────────────────────────────────────────────────────────
  const kpis: { label: string; value: string; sub?: string }[] = (() => {
    if (activeTab === 'revenue' && revenue) {
      return [
        { label: 'Tổng doanh thu', value: fmtVnd(revenue.totalRevenue) },
        { label: 'Đặt cọc (40%)', value: fmtVnd(revenue.depositRevenue) },
        { label: 'Phần còn lại (60%)', value: fmtVnd(revenue.balanceRevenue) },
        { label: 'Số booking', value: String(revenue.totalBookingCount) },
      ];
    }
    if (activeTab === 'occupancy' && occupancy) {
      return [
        { label: 'Lấp đầy trung bình', value: fmtPct(occupancy.avgOccupancyRate) },
        { label: 'Tổng số phòng', value: String(occupancy.totalRooms) },
        { label: 'Đêm-phòng đã bán', value: occupancy.totalOccupiedRoomNights.toLocaleString('vi-VN') },
        { label: 'Đêm-phòng khả dụng', value: occupancy.totalAvailableRoomNights.toLocaleString('vi-VN') },
      ];
    }
    if (activeTab === 'bookings' && bookings) {
      return [{ label: 'Tổng số booking', value: String(bookings.totalBookings) }];
    }
    return [];
  })();

  // ── Bảng tổng hợp theo tab ──────────────────────────────────────────────────
  const periodLabel = groupBy === 'month' ? 'Tháng' : 'Tuần';

  function renderTable() {
    if (activeTab === 'revenue') {
      const rows = revenue?.byPeriod ?? [];
      return (
        <DataTable
          columns={[
            { header: periodLabel, accessor: (r) => <span style={{ fontWeight: 600 }}>{r.period}</span> },
            { header: 'Doanh thu', className: 'text-right', accessor: (r) => <span style={{ fontWeight: 700, color: 'var(--primary)' }}>{fmtVnd(r.revenue)}</span> },
            { header: 'Số booking', className: 'text-right', accessor: (r) => r.bookingCount },
          ]}
          data={rows}
          keyExtractor={(r) => r.period}
          className="border-0 !shadow-none"
        />
      );
    }
    if (activeTab === 'occupancy') {
      const rows = occupancy?.byPeriod ?? [];
      return (
        <DataTable
          columns={[
            { header: periodLabel, accessor: (r) => <span style={{ fontWeight: 600 }}>{r.period}</span> },
            { header: 'Tỷ lệ lấp đầy', className: 'text-right', accessor: (r) => <span style={{ fontWeight: 700, color: 'var(--primary)' }}>{fmtPct(r.occupancyRate)}</span> },
            { header: 'Đêm-phòng đã bán', className: 'text-right', accessor: (r) => r.occupiedRoomNights.toLocaleString('vi-VN') },
            { header: 'Đêm-phòng khả dụng', className: 'text-right', accessor: (r) => r.availableRoomNights.toLocaleString('vi-VN') },
          ]}
          data={rows}
          keyExtractor={(r) => r.period}
          className="border-0 !shadow-none"
        />
      );
    }
    const rows = bookings?.byPeriod ?? [];
    return (
      <DataTable
        columns={[
          { header: periodLabel, accessor: (r) => <span style={{ fontWeight: 600 }}>{r.period}</span> },
          { header: 'Số booking', className: 'text-right', accessor: (r) => <span style={{ fontWeight: 700, color: 'var(--primary)' }}>{r.bookingCount}</span> },
        ]}
        data={rows}
        keyExtractor={(r) => r.period}
        className="border-0 !shadow-none"
      />
    );
  }

  // ── Render ──────────────────────────────────────────────────────────────────
  return (
    <ManagerLayout>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <h1 className="heading-md" style={{ marginBottom: 4 }}>Báo cáo tổng hợp</h1>
        <p className="body-sm text-charcoal">
          {selectedPropName} · {applied.from} → {applied.to}
        </p>
      </div>

      {/* Filter Bar */}
      <div className="card-lg" style={{
        padding: '16px 20px', marginBottom: 20,
        display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'flex-end',
        background: 'var(--surface-bone)',
      }}>
        <div style={{ flex: '1 1 200px', minWidth: 160 }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Homestay</label>
          <select
            value={propertyId}
            onChange={e => setPropertyId(e.target.value)}
            style={{ width: '100%', height: 40, padding: '0 14px', borderRadius: 9999, border: '1px solid var(--hairline)', background: 'var(--surface-card)', fontSize: 14, cursor: 'pointer' }}
          >
            {properties.length === 0 && <option value="">Chưa có homestay</option>}
            {properties.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        <div style={{ flex: '1 1 140px', minWidth: 120 }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Từ ngày</label>
          <input type="date" value={from} max={to} onChange={e => setFrom(e.target.value)}
            style={{ width: '100%', height: 40, padding: '0 14px', borderRadius: 9999, border: '1px solid var(--hairline)', background: 'var(--surface-card)', fontSize: 14 }} />
        </div>
        <div style={{ flex: '1 1 140px', minWidth: 120 }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Đến ngày</label>
          <input type="date" value={to} min={from} onChange={e => setTo(e.target.value)}
            style={{ width: '100%', height: 40, padding: '0 14px', borderRadius: 9999, border: '1px solid var(--hairline)', background: 'var(--surface-card)', fontSize: 14 }} />
        </div>
        <div style={{ flex: '0 0 auto' }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Nhóm theo</label>
          <div style={{ display: 'flex', gap: 4 }}>
            {(['month', 'week'] as const).map(g => (
              <button key={g} onClick={() => setGroupBy(g)}
                className={groupBy === g ? 'btn-primary btn-sm' : 'btn-outline btn-sm'} style={{ minWidth: 60 }}>
                {g === 'month' ? 'Tháng' : 'Tuần'}
              </button>
            ))}
          </div>
        </div>
        <button onClick={handleApply} disabled={loading || !propertyId}
          className="btn-primary btn-sm" style={{ flex: '0 0 auto', height: 40, padding: '0 20px' }}>
          Áp dụng
        </button>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid var(--hairline)', marginBottom: 24 }}>
        {TABS.map(t => (
          <button key={t.key} onClick={() => selectTab(t.key)}
            style={{
              padding: '10px 20px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
              background: 'none', border: 'none',
              color: activeTab === t.key ? 'var(--primary)' : 'var(--charcoal)',
              borderBottom: activeTab === t.key ? '2px solid var(--primary)' : '2px solid transparent',
              marginBottom: -1,
            }}>
            {t.label}
          </button>
        ))}
      </div>

      {/* Error */}
      {error && !loading && (
        <div className="alert alert-error" style={{ marginBottom: 24, alignItems: 'flex-start' }}>
          <div style={{ flex: 1 }}>
            <p style={{ fontWeight: 600, marginBottom: 4 }}>Không thể tải báo cáo</p>
            <p style={{ fontSize: 13 }}>{error}</p>
          </div>
          <button onClick={() => fetchTab(activeTab, propertyId, applied)} className="btn-primary btn-sm">Thử lại</button>
        </div>
      )}

      {/* KPI Row */}
      {kpis.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: `repeat(${Math.min(kpis.length, 4)}, 1fr)`, gap: 16, marginBottom: 24 }}>
          {loading
            ? Array(kpis.length).fill(0).map((_, i) => <Skeleton key={i} height={100} />)
            : kpis.map(k => <KpiCard key={k.label} {...k} />)}
        </div>
      )}

      {/* Line Chart */}
      <div className="card-lg" style={{ padding: 24, marginBottom: 20 }}>
        <h2 className="heading-sm" style={{ marginBottom: 20 }}>{chart.title}</h2>
        {loading ? <Skeleton height={260} /> : chart.empty ? <EmptyState /> : (
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={chart.data as unknown as Record<string, string | number>[]} margin={{ top: 4, right: 12, bottom: 0, left: 8 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" vertical={false} />
              <XAxis dataKey="period" tick={{ fontSize: 11, fill: '#888' }} />
              <YAxis tickFormatter={chart.yFmt} tick={{ fontSize: 11, fill: '#888' }} width={56} />
              <Tooltip content={(props) => (
                <ChartTooltip active={props.active} payload={props.payload} label={props.label as string} formatter={chart.tipFmt} />
              )} />
              <Line type="monotone" dataKey={chart.dataKey} stroke="var(--primary)" strokeWidth={2.5}
                dot={{ r: 3 }} activeDot={{ r: 5 }} />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Summary Table */}
      <div className="card-lg" style={{ padding: 24 }}>
        <h2 className="heading-sm" style={{ marginBottom: 18 }}>Bảng tổng hợp</h2>
        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {Array(5).fill(0).map((_, i) => <Skeleton key={i} height={40} />)}
          </div>
        ) : renderTable()}
      </div>

      <style>{`@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }`}</style>
    </ManagerLayout>
  );
}

export default PropertyReportsPage;
