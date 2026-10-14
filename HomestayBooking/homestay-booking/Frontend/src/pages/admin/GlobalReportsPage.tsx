import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../layouts/AdminLayout';
import {
  getAdminProperties,
  getGlobalRevenueReport,
  getAdminOccupancyReport,
  getAdminBookingTrends,
  type AdminProperty,
  type MonthlyRevenue,
} from '../../api/adminApi';
import type { OccupancyReportData, BookingTrendData } from '../../api/reportApi';
import { DataTable } from '../../components/ui';
import { fmtVnd, extractApiError, Spinner, ErrorBanner } from './_adminShared';

const MONTHS_VI = ['Th1','Th2','Th3','Th4','Th5','Th6','Th7','Th8','Th9','Th10','Th11','Th12'];

/** Compact VND for KPI cards — tránh tràn số lớn (tỷ/triệu). */
function fmtVndCompact(n: number): string {
  const abs = Math.abs(n);
  if (abs >= 1_000_000_000) {
    return `${(n / 1_000_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tỷ ₫`;
  }
  if (abs >= 1_000_000) {
    return `${(n / 1_000_000).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} triệu ₫`;
  }
  return fmtVnd(n);
}

type TabKey = 'revenue' | 'occupancy' | 'bookings';

export function GlobalReportsPage() {
  const [year, setYear] = useState(new Date().getFullYear());
  const [propertyId, setPropertyId] = useState('');
  const [properties, setProperties] = useState<AdminProperty[]>([]);
  const [tab, setTab] = useState<TabKey>('revenue');

  const [data, setData] = useState<MonthlyRevenue[]>([]);
  const [occupancy, setOccupancy] = useState<OccupancyReportData | null>(null);
  const [bookings, setBookings] = useState<BookingTrendData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getAdminProperties({ size: 100 })
      .then(res => { if (res.success) setProperties(res.data.content ?? []); })
      .catch(() => { /* non-blocking */ });
  }, []);

  const load = useCallback(async (y: number, pid: string, t: TabKey) => {
    setLoading(true);
    setError(null);
    try {
      if (t === 'revenue') {
        const res = await getGlobalRevenueReport(y, pid || undefined);
        if (res.success) setData(res.data?.monthlyData ?? []);
        else setError('Không tải được báo cáo.');
      } else if (!pid) {
        setError('Chọn property để xem Occupancy / Booking Trends.');
        setOccupancy(null);
        setBookings(null);
      } else if (t === 'occupancy') {
        const res = await getAdminOccupancyReport(y, pid);
        if (res.success) setOccupancy(res.data);
        else setError('Không tải được occupancy.');
      } else {
        const res = await getAdminBookingTrends(y, pid);
        if (res.success) setBookings(res.data);
        else setError('Không tải được booking trends.');
      }
    } catch (err) {
      setError(extractApiError(err, 'Không tải được báo cáo.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(year, propertyId, tab); }, [load, year, propertyId, tab]);

  const totalRevenue = data.reduce((s, d) => s + d.revenue, 0);
  const maxRevenue = Math.max(...data.map(d => d.revenue), 1);

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24, flexWrap: 'wrap', gap: 12 }}>
          <div>
            <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)', marginBottom: 4 }}>Global Reports</h1>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <label className="form-label" style={{ margin: 0 }} htmlFor="report-year">Năm:</label>
            <select id="report-year" className="input" style={{ width: 100 }}
              value={year} onChange={e => setYear(Number(e.target.value))}>
              {[2024, 2025, 2026, 2027].map(y => <option key={y} value={y}>{y}</option>)}
            </select>
            <label className="form-label" style={{ margin: 0 }} htmlFor="report-prop">Property:</label>
            <select id="report-prop" className="input" style={{ minWidth: 200 }}
              value={propertyId} onChange={e => setPropertyId(e.target.value)}>
              <option value="">Tất cả (revenue)</option>
              {properties.map(p => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid var(--hairline)', marginBottom: 24 }}>
          {([
            { key: 'revenue' as const, label: 'Doanh thu' },
            { key: 'occupancy' as const, label: 'Lấp đầy' },
            { key: 'bookings' as const, label: 'Xu hướng booking' },
          ]).map(t => (
            <button key={t.key} type="button" onClick={() => setTab(t.key)}
              style={{
                padding: '10px 20px', fontSize: 14, fontWeight: 600, cursor: 'pointer',
                background: 'none', border: 'none',
                color: tab === t.key ? 'var(--primary)' : 'var(--charcoal)',
                borderBottom: tab === t.key ? '2px solid var(--primary)' : '2px solid transparent',
                marginBottom: -1,
              }}>
              {t.label}
            </button>
          ))}
        </div>

        {error && <ErrorBanner msg={error} />}

        {tab === 'revenue' && (
          <>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 14, marginBottom: 24 }}>
              <div className="kpi-card">
                <div
                  className="kpi-value"
                  title={fmtVnd(totalRevenue)}
                  style={{ color: 'var(--primary)', fontSize: 24, lineHeight: 1.2, letterSpacing: '-0.5px', overflowWrap: 'anywhere' }}
                >
                  {fmtVndCompact(totalRevenue)}
                </div>
                <div className="kpi-label">Tổng doanh thu {year}</div>
              </div>
              <div className="kpi-card">
                <div className="kpi-value">{data.filter(d => d.revenue > 0).length}</div>
                <div className="kpi-label">Tháng có dữ liệu</div>
              </div>
              <div className="kpi-card">
                <div
                  className="kpi-value"
                  title={data.length > 0 ? fmtVnd(totalRevenue / 12) : undefined}
                  style={{ fontSize: 24, lineHeight: 1.2, letterSpacing: '-0.5px', overflowWrap: 'anywhere' }}
                >
                  {data.length > 0 ? fmtVndCompact(totalRevenue / 12) : '—'}
                </div>
                <div className="kpi-label">TB/tháng (12 tháng)</div>
              </div>
            </div>

            <div className="card" style={{ padding: 24 }}>
              <h2 style={{ fontFamily: 'Outfit', fontSize: 16, fontWeight: 700, marginBottom: 20 }}>
                Doanh thu theo tháng — {year}
              </h2>
              {loading ? <div style={{ height: 160 }}><Spinner /></div> : data.length === 0 ? (
                <p className="body-sm text-charcoal" style={{ textAlign: 'center', padding: 40 }}>Chưa có dữ liệu</p>
              ) : (
                <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 180, padding: '0 4px' }}>
                  {data.map(d => {
                    const pct = Math.round((d.revenue / maxRevenue) * 100);
                    return (
                      <div key={d.month} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
                        <span style={{ fontSize: 10, color: 'var(--charcoal)', fontWeight: 600 }}>
                          {d.revenue > 0 ? `${(d.revenue / 1_000_000).toFixed(0)}M` : ''}
                        </span>
                        <div
                          title={`${MONTHS_VI[d.month - 1]}: ${fmtVnd(d.revenue)}`}
                          style={{
                            width: '100%', height: `${Math.max(pct, 4)}%`, background: 'var(--primary)',
                            borderRadius: '4px 4px 0 0', transition: 'height 0.4s ease', cursor: 'default',
                          }}
                        />
                        <span style={{ fontSize: 11, color: 'var(--charcoal)' }}>{MONTHS_VI[d.month - 1]}</span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {!loading && data.length > 0 && (
              <div style={{ marginTop: 20 }}>
                <DataTable
                  columns={[
                    { header: 'Tháng', accessor: (d: MonthlyRevenue) => `${MONTHS_VI[d.month - 1]} ${year}` },
                    { header: 'Doanh thu', accessor: (d: MonthlyRevenue) => (
                      <span style={{ fontWeight: 600, color: 'var(--primary)' }}>{fmtVnd(d.revenue)}</span>
                    ) },
                    { header: '% Tổng', accessor: (d: MonthlyRevenue) =>
                      `${totalRevenue > 0 ? ((d.revenue / totalRevenue) * 100).toFixed(1) : 0}%` },
                  ]}
                  data={data}
                  keyExtractor={(d) => d.month.toString()}
                />
              </div>
            )}
          </>
        )}

        {tab === 'occupancy' && (
          <div className="card" style={{ padding: 24 }}>
            {loading ? <Spinner /> : occupancy ? (
              <>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 12, marginBottom: 20 }}>
                  <div className="kpi-card">
                    <div className="kpi-value">{occupancy.avgOccupancyRate.toFixed(1)}%</div>
                    <div className="kpi-label">Lấp đầy TB</div>
                  </div>
                  <div className="kpi-card">
                    <div className="kpi-value">{occupancy.totalRooms}</div>
                    <div className="kpi-label">Số phòng</div>
                  </div>
                  <div className="kpi-card">
                    <div className="kpi-value">{occupancy.totalOccupiedRoomNights}</div>
                    <div className="kpi-label">Đêm đã bán</div>
                  </div>
                </div>
                <DataTable
                  columns={[
                    { header: 'Kỳ', accessor: (r) => r.period },
                    { header: 'Lấp đầy %', accessor: (r) => `${r.occupancyRate}%` },
                    { header: 'Đêm bán', accessor: (r) => r.occupiedRoomNights },
                    { header: 'Đêm khả dụng', accessor: (r) => r.availableRoomNights },
                  ]}
                  data={occupancy.byPeriod}
                  keyExtractor={(r) => r.period}
                />
              </>
            ) : (
              <p className="body-sm text-charcoal">Chọn property để xem occupancy.</p>
            )}
          </div>
        )}

        {tab === 'bookings' && (
          <div className="card" style={{ padding: 24 }}>
            {loading ? <Spinner /> : bookings ? (
              <>
                <div className="kpi-card" style={{ marginBottom: 20, maxWidth: 220 }}>
                  <div className="kpi-value">{bookings.totalBookings}</div>
                  <div className="kpi-label">Booking counted (năm)</div>
                </div>
                <DataTable
                  columns={[
                    { header: 'Kỳ', accessor: (r) => r.period },
                    { header: 'Số booking', accessor: (r) => r.bookingCount },
                  ]}
                  data={bookings.byPeriod}
                  keyExtractor={(r) => r.period}
                />
              </>
            ) : (
              <p className="body-sm text-charcoal">Chọn property để xem booking trends.</p>
            )}
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
