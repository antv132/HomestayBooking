// ─── SCR-59: Revenue Report ────────────────────────────────────────────────────
import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, Cell,
} from 'recharts';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faChartBar, faChartLine, faCoins, faFileInvoice,
  faBookmark, faArrowsRotate, faCircleInfo, faFilePdf, faFileExcel,
  faFilter,
} from '@fortawesome/free-solid-svg-icons';
import ManagerLayout from '../../layouts/ManagerLayout';
import { reportApi, type RevenueReportData, type RevenueReportParams } from '../../api/reportApi';
import { propertyApi, type PropertySummary } from '../../api/propertyApi';
import DataTable from '../../components/ui/DataTable';

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Định dạng số VNĐ rút gọn: 72_000_000 → "72M" */
function fmtM(v: number): string {
  if (v >= 1_000_000_000) return `${(v / 1_000_000_000).toFixed(1)}B`;
  if (v >= 1_000_000)     return `${(v / 1_000_000).toFixed(1)}M`;
  return `₫${v.toLocaleString('vi-VN')}`;
}

/** Định dạng số VNĐ đầy đủ */
function fmtVnd(v: number): string {
  return `₫${v.toLocaleString('vi-VN')}`;
}

/** Tính % của phần so với tổng */
function pct(part: number, total: number): string {
  if (!total) return '—';
  return `${((part / total) * 100).toFixed(1)}%`;
}

/** Ngày mặc định: 6 tháng gần nhất */
function defaultDateRange(): { from: string; to: string } {
  const to   = new Date();
  const from = new Date();
  from.setMonth(from.getMonth() - 5);
  from.setDate(1);
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  return { from: fmt(from), to: fmt(to) };
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

function Skeleton({ height = 120, style }: { height?: number; style?: React.CSSProperties }) {
  return (
    <div style={{
      background: 'var(--surface-card)', borderRadius: 14, height,
      animation: 'pulse 1.5s ease-in-out infinite', ...style,
    }} />
  );
}

// ── Custom Tooltip ────────────────────────────────────────────────────────────

function RevenueTooltip({ active, payload, label }: {
  active?: boolean; payload?: { value: number; name?: string }[]; label?: string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div style={{
      background: '#fff', border: '1px solid var(--hairline)', borderRadius: 8,
      padding: '10px 16px', boxShadow: '0 4px 16px rgba(0,0,0,0.08)', fontSize: 13,
    }}>
      <p style={{ fontWeight: 700, marginBottom: 4, color: 'var(--ink)' }}>{label}</p>
      {payload.map((p, i) => (
        <p key={i} style={{ color: 'var(--primary)', fontWeight: 600 }}>
          {fmtVnd(p.value)}
        </p>
      ))}
    </div>
  );
}

// ── KPI Card ──────────────────────────────────────────────────────────────────

interface KpiProps {
  icon: React.ReactNode;
  iconBg: string;
  iconColor: string;
  label: string;
  value: string;
  sub?: string;
}

function KpiCard({ icon, iconBg, iconColor, label, value, sub }: KpiProps) {
  return (
    <div className="kpi-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <span className="body-sm text-charcoal" style={{ fontWeight: 500 }}>{label}</span>
        <div style={{
          width: 36, height: 36, borderRadius: 10,
          background: iconBg, color: iconColor,
          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        }}>
          {icon}
        </div>
      </div>
      <div className="kpi-value" style={{ fontSize: 26, lineHeight: 1.1, marginBottom: sub ? 4 : 0 }}>
        {value}
      </div>
      {sub && <div className="body-sm text-charcoal">{sub}</div>}
    </div>
  );
}

// ── Empty State ───────────────────────────────────────────────────────────────

function EmptyChart({ message = 'Không có dữ liệu cho kỳ này' }: { message?: string }) {
  return (
    <div style={{
      height: 220, display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', gap: 10, opacity: 0.4,
    }}>
      <FontAwesomeIcon icon={faChartBar} style={{ fontSize: 48 }} />
      <p className="body-md">{message}</p>
      <p className="body-sm">Hãy thử điều chỉnh bộ lọc</p>
    </div>
  );
}

// ── Màu cho property bars ─────────────────────────────────────────────────────
const PROPERTY_COLORS = ['#0F766E','#2563EB','#2b9a66','#7C3AED','#F59E0B','#0891B2','#DC2626'];

// ── Main Component ─────────────────────────────────────────────────────────────

export function RevenueReportPage() {
  const defaults = defaultDateRange();

  // Filter state
  const [propertyId, setPropertyId] = useState<string>('');
  const [from,       setFrom]       = useState<string>(defaults.from);
  const [to,         setTo]         = useState<string>(defaults.to);
  const [groupBy,    setGroupBy]    = useState<'month' | 'week'>('month');

  // Applied params (chỉ fetch khi nhấn Apply)
  const [appliedParams, setAppliedParams] = useState<RevenueReportParams>({
    from: defaults.from,
    to:   defaults.to,
    groupBy: 'month',
  });

  // Data state
  const [data,       setData]       = useState<RevenueReportData | null>(null);
  const [loading,    setLoading]    = useState(false);
  const [error,      setError]      = useState<string | null>(null);
  const [properties, setProperties] = useState<PropertySummary[]>([]);
  const [lastUpdated, setLastUpdated] = useState('');

  // ── Load property list for filter dropdown ──────────────────────────────────
  useEffect(() => {
    propertyApi.getAll({ size: 100 })
      .then(res => { if (res.success) setProperties(res.data.content); })
      .catch(() => { /* Không block UX nếu property list lỗi */ });
  }, []);

  // ── Fetch revenue data ──────────────────────────────────────────────────────
  const fetchData = useCallback(async (params: RevenueReportParams) => {
    setLoading(true);
    setError(null);
    try {
      const res = await reportApi.getRevenue(params);
      if (res.success && res.data) {
        setData(res.data);
        setLastUpdated(new Date().toLocaleTimeString('vi-VN'));
      } else {
        setError(res.message || 'Không thể tải dữ liệu báo cáo.');
      }
    } catch (err: unknown) {
      const ax = err as { response?: { data?: { message?: string } } };
      setError(ax?.response?.data?.message || 'Lỗi kết nối. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch lần đầu khi mount
  useEffect(() => { fetchData(appliedParams); }, []);   // eslint-disable-line

  // ── Apply filter ────────────────────────────────────────────────────────────
  function handleApply() {
    const params: RevenueReportParams = { from, to, groupBy };
    if (propertyId) params.propertyId = propertyId;
    setAppliedParams(params);
    fetchData(params);
  }

  // ── Export PDF (print) ──────────────────────────────────────────────────────
  function handleExportPdf() { window.print(); }

  // ── Export Excel (CSV fallback) ─────────────────────────────────────────────
  function handleExportExcel() {
    if (!data) return;
    const header  = ['Kỳ', 'Doanh thu (VNĐ)', 'Số booking'];
    const rows    = data.byPeriod.map(r => [r.period, r.revenue, r.bookingCount]);
    const csv     = [header, ...rows].map(r => r.join(',')).join('\n');
    const blob    = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
    const url     = URL.createObjectURL(blob);
    const link    = document.createElement('a');
    link.href     = url;
    link.download = `revenue_report_${from}_${to}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

  // ── KPI data ────────────────────────────────────────────────────────────────
  const kpis: KpiProps[] = data ? [
    {
      icon:      <FontAwesomeIcon icon={faChartLine}          style={{ fontSize: 17 }} />,
      iconBg:    'rgba(15,118,110,0.10)', iconColor: 'var(--primary)',
      label:     'Tổng doanh thu',
      value:     fmtVnd(data.totalRevenue),
    },
    {
      icon:      <FontAwesomeIcon icon={faCoins}              style={{ fontSize: 17 }} />,
      iconBg:    '#f0fdf4', iconColor: '#2b9a66',
      label:     'Đặt cọc (40%)',
      value:     fmtVnd(data.depositRevenue),
      sub:       pct(data.depositRevenue, data.totalRevenue) + ' tổng',
    },
    {
      icon:      <FontAwesomeIcon icon={faFileInvoice}        style={{ fontSize: 17 }} />,
      iconBg:    '#eff6ff', iconColor: '#2563EB',
      label:     'Phần còn lại (60%)',
      value:     fmtVnd(data.balanceRevenue),
      sub:       pct(data.balanceRevenue, data.totalRevenue) + ' tổng',
    },
    {
      icon:      <FontAwesomeIcon icon={faBookmark}           style={{ fontSize: 17 }} />,
      iconBg:    '#f5f3ff', iconColor: '#7C3AED',
      label:     'Số booking',
      value:     String(data.totalBookingCount),
    },
  ] : [];

  // ── Property filter label ───────────────────────────────────────────────────
  const selectedPropName = properties.find(p => p.id === appliedParams.propertyId)?.name || 'Tất cả';

  // ── Column Definitions ─────────────────────────────────────────────────────────
  const propertyColumns = data ? [
    { header: 'Property', accessor: (p: any) => {
      const idx = data.byProperty.findIndex(x => x.propertyId === p.propertyId);
      return (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 10, height: 10, borderRadius: '50%', flexShrink: 0, background: PROPERTY_COLORS[idx % PROPERTY_COLORS.length] }} />
          <span style={{ fontWeight: 600, fontSize: 13 }}>{p.propertyName}</span>
        </div>
      );
    }},
    { header: 'Doanh thu', className: 'text-right', accessor: (p: any) => <div style={{ fontWeight: 700, color: 'var(--primary)' }}>{fmtVnd(p.revenue)}</div> },
    { header: 'Booking', className: 'text-right', accessor: (p: any) => <div style={{ color: 'var(--charcoal)' }}>{p.bookingCount}</div> },
    { header: 'Tỉ lệ', className: 'text-right', accessor: (p: any) => <div><span className="badge badge-neutral" style={{ fontSize: 11 }}>{pct(p.revenue, data.totalRevenue)}</span></div> }
  ] : [];

  const propertyFooter = data ? (
    <tr style={{ borderTop: '2px solid var(--hairline)' }}>
      <td className="px-4 py-3" style={{ fontWeight: 700 }}>Tổng</td>
      <td className="px-4 py-3" style={{ textAlign: 'right', fontWeight: 700, color: 'var(--primary)' }}>{fmtVnd(data.totalRevenue)}</td>
      <td className="px-4 py-3" style={{ textAlign: 'right', fontWeight: 700 }}>{data.totalBookingCount}</td>
      <td className="px-4 py-3" style={{ textAlign: 'right' }}><span className="badge badge-success" style={{ fontSize: 11 }}>100%</span></td>
    </tr>
  ) : null;

  const periodColumns = data ? [
    { header: groupBy === 'month' ? 'Tháng' : 'Tuần', accessor: (row: any) => <span style={{ fontWeight: 600 }}>{row.period}</span> },
    { header: 'Doanh thu', className: 'text-right', accessor: (row: any) => <div style={{ fontWeight: 700 }}>{fmtVnd(row.revenue)}</div> },
    { header: 'Số booking', className: 'text-right', accessor: (row: any) => <div style={{ color: 'var(--charcoal)' }}>{row.bookingCount}</div> },
    { header: '% tổng', className: 'text-right', accessor: (row: any) => {
      const percent = data.totalRevenue > 0 ? (row.revenue / data.totalRevenue) * 100 : 0;
      return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 8 }}>
          <div style={{ height: 6, width: `${Math.round(percent)}px`, minWidth: 4, maxWidth: 80, background: 'var(--primary)', borderRadius: 4, opacity: 0.7 }} />
          <span style={{ fontSize: 12, color: 'var(--charcoal)', minWidth: 40, textAlign: 'right' }}>{percent.toFixed(1)}%</span>
        </div>
      );
    }},
    { header: 'Trung bình/booking', className: 'text-right', accessor: (row: any) => {
      const avg = row.bookingCount > 0 ? Math.round(row.revenue / row.bookingCount) : 0;
      return <div style={{ color: 'var(--charcoal)', fontSize: 13 }}>{avg > 0 ? fmtVnd(avg) : '—'}</div>;
    }}
  ] : [];

  const periodFooter = data ? (
    <tr style={{ borderTop: '2px solid var(--hairline)' }}>
      <td className="px-4 py-3" style={{ fontWeight: 700 }}>Tổng</td>
      <td className="px-4 py-3" style={{ textAlign: 'right', fontWeight: 700, color: 'var(--primary)' }}>{fmtVnd(data.totalRevenue)}</td>
      <td className="px-4 py-3" style={{ textAlign: 'right', fontWeight: 700 }}>{data.totalBookingCount}</td>
      <td className="px-4 py-3" style={{ textAlign: 'right' }}><span className="badge badge-success" style={{ fontSize: 11 }}>100%</span></td>
      <td className="px-4 py-3" style={{ textAlign: 'right', color: 'var(--charcoal)', fontSize: 13 }}>
        {data.totalBookingCount > 0 ? fmtVnd(Math.round(data.totalRevenue / data.totalBookingCount)) : '—'}
      </td>
    </tr>
  ) : null;

  // ── Render ──────────────────────────────────────────────────────────────────
  return (
    <ManagerLayout>

      {/* Breadcrumb */}
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
        <Link to="/manager/reports" className="text-primary" style={{ textDecoration: 'none' }}>
          Báo cáo
        </Link>
        <span>›</span>
        <span style={{ fontWeight: 600 }}>Doanh thu</span>
      </div>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 24 }}>
        <div>
          <h1 className="heading-md" style={{ marginBottom: 4 }}>Revenue Report</h1>
          <p className="body-sm text-charcoal">
            {selectedPropName} · {appliedParams.from} → {appliedParams.to}
            {lastUpdated && (
              <span style={{ marginLeft: 10, color: 'var(--ash)', fontSize: 11 }}>
                · Cập nhật {lastUpdated}
              </span>
            )}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <button
            onClick={handleExportPdf}
            disabled={loading || !data}
            className="btn-outline btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <FontAwesomeIcon icon={faFilePdf} />
            PDF
          </button>
          <button
            onClick={handleExportExcel}
            disabled={loading || !data}
            className="btn-outline btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <FontAwesomeIcon icon={faFileExcel} />
            Excel
          </button>
          <button
            onClick={() => fetchData(appliedParams)}
            disabled={loading}
            className="btn-outline btn-sm"
            style={{ display: 'flex', alignItems: 'center', gap: 6 }}
          >
            <span style={{ animation: loading ? 'spin 0.8s linear infinite' : 'none', display: 'flex' }}>
              <FontAwesomeIcon icon={faArrowsRotate} style={{ fontSize: 12 }} />
            </span>
            {loading ? 'Đang tải...' : 'Làm mới'}
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="card-lg" style={{
        padding: '16px 20px', marginBottom: 24,
        display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'flex-end',
        background: 'var(--surface-bone)',
      }}>
        <FontAwesomeIcon icon={faFilter} style={{ color: 'var(--charcoal)', fontSize: 14, marginBottom: 2 }} />

        {/* Property */}
        <div style={{ flex: '1 1 180px', minWidth: 140 }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Property</label>
          <select
            value={propertyId}
            onChange={e => setPropertyId(e.target.value)}
            style={{
              width: '100%', height: 40, padding: '0 14px',
              borderRadius: 9999, border: '1px solid var(--hairline)',
              background: 'var(--surface-card)', fontSize: 14,
              cursor: 'pointer',
            }}
          >
            <option value="">Tất cả property</option>
            {properties.map(p => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
        </div>

        {/* From */}
        <div style={{ flex: '1 1 140px', minWidth: 120 }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Từ ngày</label>
          <input
            type="date"
            value={from}
            max={to}
            onChange={e => setFrom(e.target.value)}
            style={{
              width: '100%', height: 40, padding: '0 14px',
              borderRadius: 9999, border: '1px solid var(--hairline)',
              background: 'var(--surface-card)', fontSize: 14,
            }}
          />
        </div>

        {/* To */}
        <div style={{ flex: '1 1 140px', minWidth: 120 }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Đến ngày</label>
          <input
            type="date"
            value={to}
            min={from}
            onChange={e => setTo(e.target.value)}
            style={{
              width: '100%', height: 40, padding: '0 14px',
              borderRadius: 9999, border: '1px solid var(--hairline)',
              background: 'var(--surface-card)', fontSize: 14,
            }}
          />
        </div>

        {/* GroupBy toggle */}
        <div style={{ flex: '0 0 auto' }}>
          <label className="body-sm text-charcoal" style={{ display: 'block', marginBottom: 4 }}>Nhóm theo</label>
          <div style={{ display: 'flex', gap: 4 }}>
            {(['month', 'week'] as const).map(g => (
              <button
                key={g}
                onClick={() => setGroupBy(g)}
                className={groupBy === g ? 'btn-primary btn-sm' : 'btn-outline btn-sm'}
                style={{ minWidth: 60 }}
              >
                {g === 'month' ? 'Tháng' : 'Tuần'}
              </button>
            ))}
          </div>
        </div>

        {/* Apply */}
        <button
          onClick={handleApply}
          disabled={loading}
          className="btn-primary btn-sm"
          style={{ flex: '0 0 auto', alignSelf: 'flex-end', height: 40, padding: '0 20px' }}
        >
          Áp dụng
        </button>
      </div>

      {/* Error */}
      {error && !loading && (
        <div className="alert alert-error" style={{ marginBottom: 24, alignItems: 'flex-start' }}>
          <FontAwesomeIcon icon={faCircleInfo} style={{ marginTop: 2 }} />
          <div style={{ flex: 1 }}>
            <p style={{ fontWeight: 600, marginBottom: 4 }}>Không thể tải báo cáo</p>
            <p style={{ fontSize: 13 }}>{error}</p>
          </div>
          <button onClick={() => fetchData(appliedParams)} className="btn-primary btn-sm">Thử lại</button>
        </div>
      )}

      {/* KPI Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        {loading
          ? Array(4).fill(0).map((_, i) => <Skeleton key={i} height={108} />)
          : kpis.map(k => <KpiCard key={k.label} {...k} />)
        }
      </div>

      {/* Revenue Bar Chart — byPeriod */}
      <div className="card-lg" style={{ padding: 24, marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
          <div>
            <h2 className="heading-sm" style={{ marginBottom: 2 }}>
              Doanh thu theo {groupBy === 'month' ? 'tháng' : 'tuần'}
            </h2>
            <p className="body-sm text-charcoal">Đơn vị: VNĐ — chỉ tính payment đã xác nhận (PAID)</p>
          </div>
        </div>

        {loading ? <Skeleton height={240} /> : (data?.byPeriod?.length ?? 0) > 0 ? (
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={data!.byPeriod} margin={{ top: 4, right: 4, bottom: 0, left: 16 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" vertical={false} />
              <XAxis dataKey="period" tick={{ fontSize: 11, fill: '#888' }} />
              <YAxis
                tickFormatter={v => fmtM(v)}
                tick={{ fontSize: 11, fill: '#888' }}
                width={56}
              />
              <Tooltip content={<RevenueTooltip />} />
              <Bar dataKey="revenue" name="Doanh thu" fill="var(--primary)" radius={[5, 5, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <EmptyChart />
        )}
      </div>

      {/* Two-col: Property Breakdown + Table */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20, alignItems: 'flex-start' }}>

        {/* Property Breakdown Bar */}
        <div className="card-lg" style={{ padding: 24 }}>
          <h2 className="heading-sm" style={{ marginBottom: 18 }}>Doanh thu theo Property</h2>

          {loading ? <Skeleton height={220} /> : (data?.byProperty?.length ?? 0) > 0 ? (
            <ResponsiveContainer width="100%" height={Math.max(180, data!.byProperty.length * 48)}>
              <BarChart
                data={data!.byProperty}
                layout="vertical"
                margin={{ top: 0, right: 16, bottom: 0, left: 0 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#f0ece6" horizontal={false} />
                <XAxis type="number" tickFormatter={v => fmtM(v)} tick={{ fontSize: 11, fill: '#888' }} />
                <YAxis
                  type="category"
                  dataKey="propertyName"
                  tick={{ fontSize: 12, fill: '#575757' }}
                  width={110}
                />
                <Tooltip content={<RevenueTooltip />} />
                <Bar dataKey="revenue" name="Doanh thu" radius={[0, 5, 5, 0]}>
                  {data!.byProperty.map((_, i) => (
                    <Cell key={i} fill={PROPERTY_COLORS[i % PROPERTY_COLORS.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <EmptyChart message="Không có dữ liệu property" />
          )}
        </div>

        {/* Property Revenue Table */}
        <div className="card-lg" style={{ padding: 24 }}>
          <h2 className="heading-sm" style={{ marginBottom: 18 }}>Chi tiết theo Property</h2>

          {loading ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {Array(4).fill(0).map((_, i) => <Skeleton key={i} height={40} />)}
            </div>
          ) : (data?.byProperty?.length ?? 0) > 0 ? (
            <DataTable 
              columns={propertyColumns}
              data={data!.byProperty}
              keyExtractor={(p) => p.propertyId}
              footer={propertyFooter}
              className="border-0 !shadow-none"
            />
          ) : (
            <div style={{ padding: '40px 0', textAlign: 'center', opacity: 0.4 }}>
              <p className="body-md">Không có dữ liệu</p>
            </div>
          )}
        </div>
      </div>

      {/* Period Detail Table */}
      <div className="card-lg" style={{ padding: 24 }}>
        <h2 className="heading-sm" style={{ marginBottom: 18 }}>
          Chi tiết theo {groupBy === 'month' ? 'tháng' : 'tuần'}
        </h2>

        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {Array(6).fill(0).map((_, i) => <Skeleton key={i} height={40} />)}
          </div>
        ) : (data?.byPeriod?.length ?? 0) > 0 ? (
          <DataTable 
            columns={periodColumns}
            data={data!.byPeriod}
            keyExtractor={(r) => r.period}
            footer={periodFooter}
            className="border-0 !shadow-none"
          />
        ) : (
          <div style={{ padding: '40px 0', textAlign: 'center', opacity: 0.4 }}>
            <FontAwesomeIcon icon={faChartBar} style={{ fontSize: 36, marginBottom: 12 }} />
            <p className="body-md">Không có dữ liệu trong khoảng thời gian này</p>
            <p className="body-sm">Hãy thử điều chỉnh bộ lọc và áp dụng lại</p>
          </div>
        )}
      </div>

      <style>{`
        @keyframes spin  { to { transform: rotate(360deg); } }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
        @media print {
          .manager-sidebar, nav, .btn-outline, .btn-primary, .btn-ghost { display: none !important; }
          .card-lg { box-shadow: none !important; break-inside: avoid; }
        }
      `}</style>

    </ManagerLayout>
  );
}
