import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import {
  getGlobalKpis,
  type GlobalKpis,
  getGlobalRevenueReport,
  type MonthlyRevenue,
} from '../../api/adminApi';

// ── Helpers ────────────────────────────────────────────────────────────────────
const fmtVnd = (n: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(n);

const MONTHS_VI = ['Th1','Th2','Th3','Th4','Th5','Th6','Th7','Th8','Th9','Th10','Th11','Th12'];

// ── KPI Card ──────────────────────────────────────────────────────────────────
interface KpiCardProps {
  label: string;
  value: string | number;
  sub?: string;
  link?: string;
  color?: string;
}

function KpiCard({ label, value, sub, link, color }: KpiCardProps) {
  const inner = (
    <div
      className="kpi-card"
      style={{
        cursor: link ? 'pointer' : 'default',
        padding: '20px 24px',
        borderLeft: color ? `4px solid ${color}` : '1px solid var(--hairline)',
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
        background: 'var(--surface-card)',
        borderRadius: 12,
        boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
        transition: 'all 0.2s ease',
      }}
      onMouseEnter={e => {
        if (link) {
          (e.currentTarget as HTMLDivElement).style.transform = 'translateY(-2px)';
          (e.currentTarget as HTMLDivElement).style.boxShadow = '0 8px 24px rgba(0,0,0,0.06)';
        }
      }}
      onMouseLeave={e => {
        if (link) {
          (e.currentTarget as HTMLDivElement).style.transform = '';
          (e.currentTarget as HTMLDivElement).style.boxShadow = '0 1px 3px rgba(0,0,0,0.02)';
        }
      }}
    >
      <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--mute)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        {label}
      </div>
      <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--ink)' }}>
        {value}
      </div>
      {sub && <div style={{ fontSize: 12, color: 'var(--charcoal)', marginTop: 2 }}>{sub}</div>}
    </div>
  );
  if (link) return <Link to={link} style={{ textDecoration: 'none', display: 'block' }}>{inner}</Link>;
  return inner;
}

// ── Inline Bar Chart ──────────────────────────────────────────────────────────
function RevenueBarChart({ data }: { data: MonthlyRevenue[] }) {
  if (!data.length) return null;
  const max = Math.max(...data.map(d => d.revenue), 1);
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 140, padding: '10px 4px 0 4px' }}>
      {data.map((d) => {
        const pct = Math.round((d.revenue / max) * 100);
        return (
          <div key={d.month} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
            <div
              title={`${MONTHS_VI[d.month - 1]}: ${fmtVnd(d.revenue)}`}
              style={{
                width: '100%', height: `${Math.max(pct, 6)}%`,
                background: 'var(--primary)', borderRadius: '6px 6px 0 0',
                transition: 'height 0.4s ease',
                cursor: 'default',
              }}
            />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--mute)' }}>{MONTHS_VI[d.month - 1]}</span>
          </div>
        );
      })}
    </div>
  );
}

// ── Quick Link Card ───────────────────────────────────────────────────────────
interface QuickLinkProps {
  to: string;
  label: string;
  desc: string;
  icon: React.ReactNode;
}

function QuickLink({ to, label, desc, icon }: QuickLinkProps) {
  return (
    <Link to={to} style={{ textDecoration: 'none' }}>
      <div
        className="card"
        style={{
          padding: '24px',
          display: 'flex',
          gap: 16,
          alignItems: 'flex-start',
          borderRadius: 16,
          background: 'var(--surface-card)',
          border: '1px solid var(--hairline)',
          boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
          transition: 'all 0.2s ease',
          height: '100%',
        }}
        onMouseEnter={e => {
          (e.currentTarget as HTMLDivElement).style.transform = 'translateY(-2px)';
          (e.currentTarget as HTMLDivElement).style.boxShadow = '0 12px 30px rgba(0,0,0,0.06)';
          (e.currentTarget as HTMLDivElement).style.borderColor = 'var(--primary)';
        }}
        onMouseLeave={e => {
          (e.currentTarget as HTMLDivElement).style.transform = '';
          (e.currentTarget as HTMLDivElement).style.boxShadow = '0 1px 3px rgba(0,0,0,0.02)';
          (e.currentTarget as HTMLDivElement).style.borderColor = 'var(--hairline)';
        }}
      >
        <div style={{
          width: 44,
          height: 44,
          borderRadius: 12,
          background: 'var(--primary-light)',
          color: 'var(--primary)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}>
          {icon}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, flexGrow: 1 }}>
          <h3 style={{ fontSize: 15, fontWeight: 700, color: 'var(--ink)' }}>
            {label}
          </h3>
          <p style={{ fontSize: 13, color: 'var(--mute)', lineHeight: 1.4 }}>
            {desc}
          </p>
        </div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--mute)" strokeWidth="2.5" style={{ alignSelf: 'center', flexShrink: 0 }}>
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
    </Link>
  );
}

// ── Icons ─────────────────────────────────────────────────────────────────────
const Icons = {
  Property: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M3 21h18M3 7v1a3 3 0 0 0 6 0V7m0 1a3 3 0 0 0 6 0V7m0 1a3 3 0 0 0 6 0V7M4 21V10m16 11V10M9 21V14h6v7"/></svg>,
  Manager: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>,
  Customer: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>,
  Promotion: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82zM7 7h.01"/></svg>,
  Damage: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0zM12 9v4M12 17h.01"/></svg>,
  Complaint: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>,
  Report: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>,
  Setting: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>,
};

// ── Main Component ────────────────────────────────────────────────────────────
export default function AdminDashboardPage() {
  const [kpis, setKpis] = useState<GlobalKpis | null>(null);
  const [revenue, setRevenue] = useState<MonthlyRevenue[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'management' | 'operations'>('overview');

  const currentYear = new Date().getFullYear();

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [kpiRes, revRes] = await Promise.allSettled([
          getGlobalKpis(),
          getGlobalRevenueReport(currentYear),
        ]);
        if (!cancelled) {
          if (kpiRes.status === 'fulfilled' && kpiRes.value.success) {
            setKpis(kpiRes.value.data);
          }
          if (revRes.status === 'fulfilled' && revRes.value.success) {
            setRevenue(revRes.value.data?.monthlyData ?? []);
          }
        }
      } catch {
        if (!cancelled) setError('Không thể tải dữ liệu dashboard. Vui lòng thử lại.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [currentYear]);

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        {/* Header */}
        <div style={{ marginBottom: 28, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <h1 style={{ fontFamily: 'Outfit', fontSize: 30, fontWeight: 800, color: 'var(--ink)', letterSpacing: '-0.02em', marginBottom: 2 }}>
                Admin Dashboard
              </h1>
              <p className="body-md text-charcoal" style={{ fontSize: 14 }}>Tổng quan hoạt động & cấu hình toàn hệ thống — {currentYear}</p>
            </div>

            {/* Premium Pill Tab Header */}
            <div style={{ display: 'inline-flex', background: 'var(--surface-bone)', padding: 4, borderRadius: 12, border: '1px solid var(--hairline)' }}>
              <button
                onClick={() => setActiveTab('overview')}
                style={{
                  background: activeTab === 'overview' ? 'var(--surface-card)' : 'transparent',
                  border: 'none',
                  padding: '8px 18px',
                  fontWeight: 700,
                  fontSize: 13,
                  cursor: 'pointer',
                  color: activeTab === 'overview' ? 'var(--primary)' : 'var(--charcoal)',
                  borderRadius: 8,
                  transition: 'all 0.2s ease',
                  boxShadow: activeTab === 'overview' ? '0 1px 3px rgba(0,0,0,0.06)' : 'none',
                  outline: 'none',
                }}
              >
                Thống kê & Báo cáo
              </button>
              <button
                onClick={() => setActiveTab('management')}
                style={{
                  background: activeTab === 'management' ? 'var(--surface-card)' : 'transparent',
                  border: 'none',
                  padding: '8px 18px',
                  fontWeight: 700,
                  fontSize: 13,
                  cursor: 'pointer',
                  color: activeTab === 'management' ? 'var(--primary)' : 'var(--charcoal)',
                  borderRadius: 8,
                  transition: 'all 0.2s ease',
                  boxShadow: activeTab === 'management' ? '0 1px 3px rgba(0,0,0,0.06)' : 'none',
                  outline: 'none',
                }}
              >
                Danh mục quản lý
              </button>
              <button
                onClick={() => setActiveTab('operations')}
                style={{
                  background: activeTab === 'operations' ? 'var(--surface-card)' : 'transparent',
                  border: 'none',
                  padding: '8px 18px',
                  fontWeight: 700,
                  fontSize: 13,
                  cursor: 'pointer',
                  color: activeTab === 'operations' ? 'var(--primary)' : 'var(--charcoal)',
                  borderRadius: 8,
                  transition: 'all 0.2s ease',
                  boxShadow: activeTab === 'operations' ? '0 1px 3px rgba(0,0,0,0.06)' : 'none',
                  outline: 'none',
                }}
              >
                Vận hành & Hệ thống
              </button>
            </div>
          </div>
        </div>

        {/* Error */}
        {error && (
          <div className="alert alert-error" style={{ marginBottom: 20 }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {error}
          </div>
        )}

        {/* Tab 1: Thống kê & Báo cáo */}
        {activeTab === 'overview' && (
          <div>
            {/* Top Row: Revenue & Primary Stats */}
            {loading ? (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
                <div style={{ background: 'var(--surface-bone)', height: 140, borderRadius: 16 }} className="animate-pulse" />
                <div style={{ background: 'var(--surface-bone)', height: 140, borderRadius: 16 }} className="animate-pulse" />
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 20, marginBottom: 20 }}>
                {/* Revenue Hero Card */}
                <div
                  style={{
                    background: 'linear-gradient(135deg, #0F766E 0%, #0D9488 100%)',
                    color: 'white',
                    padding: '24px 30px',
                    borderRadius: 16,
                    boxShadow: '0 8px 30px rgba(15,118,110,0.15)',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    gap: 8,
                    position: 'relative',
                    overflow: 'hidden',
                  }}
                >
                  <div style={{ position: 'absolute', right: '-20px', bottom: '-20px', width: 140, height: 140, background: 'rgba(255,255,255,0.08)', borderRadius: '50%' }} />
                  <span style={{ fontSize: 11, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.1em', opacity: 0.8 }}>
                    Doanh thu tháng này
                  </span>
                  <span style={{ fontSize: 34, fontWeight: 800 }}>
                    {kpis ? fmtVnd(kpis.monthlyRevenue) : '—'}
                  </span>
                  <Link to="/admin/reports" style={{ color: 'white', fontSize: 13, fontWeight: 700, display: 'inline-flex', alignItems: 'center', gap: 6, textDecoration: 'underline', width: 'fit-content', marginTop: 4 }}>
                    Xem báo cáo chi tiết
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                  </Link>
                </div>

                {/* Core Properties/Scale Card */}
                <div
                  style={{
                    background: 'var(--surface-card)',
                    border: '1px solid var(--hairline)',
                    padding: '24px 30px',
                    borderRadius: 16,
                    boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    gap: 12,
                  }}
                >
                  <span style={{ fontSize: 11, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.1em', color: 'var(--mute)' }}>
                    Quy mô hệ thống
                  </span>
                  <div style={{ display: 'flex', gap: 40, flexWrap: 'wrap' }}>
                    <div>
                      <div style={{ fontSize: 11, color: 'var(--mute)', fontWeight: 700, marginBottom: 2 }}>PROPERTIES</div>
                      <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--ink)' }}>{kpis ? kpis.totalProperties : '—'}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: 11, color: 'var(--mute)', fontWeight: 700, marginBottom: 2 }}>TẦNG</div>
                      <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--ink)' }}>{kpis ? kpis.totalFloors : '—'}</div>
                    </div>
                    <div>
                      <div style={{ fontSize: 11, color: 'var(--mute)', fontWeight: 700, marginBottom: 2 }}>PHÒNG</div>
                      <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--ink)' }}>{kpis ? kpis.totalRooms : '—'}</div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Status KPI Grid (5 Columns) */}
            {loading ? (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 16, marginBottom: 24 }}>
                {[1,2,3,4,5].map(i => (
                  <div key={i} style={{ background: 'var(--surface-bone)', height: 100, borderRadius: 12 }} className="animate-pulse" />
                ))}
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16, marginBottom: 24 }}>
                <KpiCard label="Phòng trống" value={kpis ? kpis.availableRooms : '—'} color="var(--success)" />
                <KpiCard label="Phòng có khách" value={kpis ? kpis.occupiedRooms : '—'} color="var(--error)" />
                <KpiCard label="Sắp nhận phòng" value={kpis ? kpis.upcomingCheckIns : '—'} color="var(--warning)" />
                <KpiCard label="Sắp trả phòng" value={kpis ? kpis.upcomingCheckOuts : '—'} color="var(--purple)" />
                <KpiCard label="Khách hàng" value={kpis ? kpis.totalCustomers.toLocaleString('vi-VN') : '—'} color="var(--info)" link="/admin/customers" />
              </div>
            )}

            {/* Revenue Chart (Spacious Full Width) */}
            <div className="card" style={{ padding: 24, borderRadius: 16, border: '1px solid var(--hairline)', boxShadow: '0 1px 3px rgba(0,0,0,0.02)', marginBottom: 28 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
                <h2 style={{ fontFamily: 'Outfit', fontSize: 16, fontWeight: 700, color: 'var(--ink)' }}>
                  Doanh thu theo tháng — {currentYear}
                </h2>
              </div>
              {revenue.length > 0 ? (
                <RevenueBarChart data={revenue} />
              ) : (
                <div style={{ height: 140, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <p className="body-sm text-charcoal">{loading ? 'Đang tải...' : 'Chưa có dữ liệu doanh thu'}</p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tab 2: Danh mục quản lý */}
        {activeTab === 'management' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20, marginBottom: 28 }}>
            <QuickLink to="/admin/properties"   label="Quản lý Properties"  desc="Danh sách, chỉnh sửa & phân công manager cho property" icon={<Icons.Property />} />
            <QuickLink to="/admin/managers"      label="Manager Directory"   desc="Quản lý thông tin và tài khoản của các manager" icon={<Icons.Manager />} />
            <QuickLink to="/admin/customers"     label="Customer Directory"  desc="Xem danh sách và chi tiết thông tin khách hàng" icon={<Icons.Customer />} />
            <QuickLink to="/admin/promotions"   label="Promotions"     desc="Quản lý banner marketing trang chủ (FR-18)" icon={<Icons.Promotion />} />
          </div>
        )}

        {/* Tab 3: Vận hành & Hệ thống */}
        {activeTab === 'operations' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 20, marginBottom: 28 }}>
            <QuickLink to="/admin/damage-escalation" label="Damage Escalation"  desc="Xét duyệt báo cáo thiệt hại phòng trên 5,000,000 VND" icon={<Icons.Damage />} />
            <QuickLink to="/admin/complaints"   label="Complaints"     desc="Danh sách và hướng giải quyết khiếu nại của khách hàng" icon={<Icons.Complaint />} />
            <QuickLink to="/admin/reports"      label="Global Reports" desc="Báo cáo doanh thu và xu hướng chi tiết toàn hệ thống" icon={<Icons.Report />} />
            <QuickLink to="/admin/settings"     label="System Admin"   desc="Cấu hình hệ thống & theo dõi nhật ký hoạt động (Logs)" icon={<Icons.Setting />} />
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
