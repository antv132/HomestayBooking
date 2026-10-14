import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import {
  getEmployeeDashboard,
  type EmployeeDashboardData,
  type EmployeeTaskSummary,
  type HousekeepingTask,
} from '../../api/employeeApi';
import { ErrBanner } from './EmployeeShared';

// ── KPI Card ──────────────────────────────────────────────────────────────────
interface KpiCardProps {
  label: string;
  value: string | number;
  sub?: string;
  link?: string;
  color?: string;
  icon?: React.ReactNode;
}

function KpiCard({ label, value, sub, link, color, icon }: KpiCardProps) {
  const inner = (
    <div
      className="kpi-card"
      style={{
        cursor: link ? 'pointer' : 'default',
        padding: '24px',
        borderLeft: color ? `4px solid ${color}` : '1px solid var(--hairline)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        height: '100%',
        minHeight: 100,
        background: 'var(--surface-card)',
        borderRadius: 16,
        boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
        borderTop: '1px solid var(--hairline)',
        borderRight: '1px solid var(--hairline)',
        borderBottom: '1px solid var(--hairline)',
        transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
      }}
      onMouseEnter={e => {
        if (link) {
          (e.currentTarget as HTMLDivElement).style.transform = 'translateY(-4px)';
          (e.currentTarget as HTMLDivElement).style.boxShadow = '0 12px 30px rgba(0,0,0,0.06)';
          if (color) (e.currentTarget as HTMLDivElement).style.borderColor = color;
        }
      }}
      onMouseLeave={e => {
        if (link) {
          (e.currentTarget as HTMLDivElement).style.transform = '';
          (e.currentTarget as HTMLDivElement).style.boxShadow = '0 1px 3px rgba(0,0,0,0.02)';
          (e.currentTarget as HTMLDivElement).style.borderColor = 'var(--hairline)';
        }
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--mute)', textTransform: 'uppercase', letterSpacing: '0.08em', lineHeight: 1.3 }}>
          {label}
        </div>
        {icon && (
          <div style={{
            color: color || 'var(--mute)',
            background: 'var(--surface-bone)',
            width: 32,
            height: 32,
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}>
            {icon}
          </div>
        )}
      </div>

      <div style={{ margin: '14px 0 6px 0' }}>
        <div style={{ fontSize: 32, fontWeight: 800, color: 'var(--ink)', lineHeight: 1 }}>
          {value}
        </div>
      </div>

      {sub && (
        <div style={{ fontSize: 12, color: 'var(--mute)', lineHeight: 1.4 }}>
          {sub}
        </div>
      )}
    </div>
  );
  if (link) return <Link to={link} style={{ textDecoration: 'none', display: 'block' }}>{inner}</Link>;
  return inner;
}

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
          <h3 style={{ fontSize: 15, fontWeight: 700, color: 'var(--ink)' }}>{label}</h3>
          <p style={{ fontSize: 13, color: 'var(--mute)', lineHeight: 1.4 }}>{desc}</p>
        </div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--mute)" strokeWidth="2.5" style={{ alignSelf: 'center', flexShrink: 0 }}>
          <polyline points="9 18 15 12 9 6"/>
        </svg>
      </div>
    </Link>
  );
}

const Icons = {
  Housekeeping: () => <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3v3M9 6h6M6 9l-2 11h16L18 9M8 9V7a4 4 0 0 1 8 0v2"/></svg>,
  Maintenance: () => <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>,
  Inspection: () => <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>,
  Damage: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>,
  CreateDamage: () => <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>,
};

function statusBadge(status: string) {
  const colors: Record<string, string> = {
    PENDING: 'var(--warning)',
    ASSIGNED: 'var(--info)',
    IN_PROGRESS: 'var(--primary)',
    COMPLETED: 'var(--success)',
    RESOLVED: 'var(--success)',
    PASSED: 'var(--success)',
  };
  return (
    <span style={{
      fontSize: 11,
      fontWeight: 700,
      color: colors[status] || 'var(--mute)',
      background: 'var(--surface-bone)',
      padding: '2px 8px',
      borderRadius: 6,
    }}>
      {status}
    </span>
  );
}

function SectionHeader({ title, link, linkLabel }: { title: string; link: string; linkLabel: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
      <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--ink)', margin: 0 }}>{title}</h2>
      <Link to={link} style={{ fontSize: 13, fontWeight: 600, color: 'var(--primary)', textDecoration: 'none' }}>
        {linkLabel}
      </Link>
    </div>
  );
}

function EmptyState({ msg }: { msg: string }) {
  return (
    <div style={{
      padding: '20px 16px',
      background: 'var(--surface-bone)',
      borderRadius: 12,
      color: 'var(--mute)',
      fontSize: 13,
      textAlign: 'center',
    }}>
      {msg}
    </div>
  );
}

function TaskRow({ task, href }: { task: EmployeeTaskSummary; href: string }) {
  return (
    <Link to={href} style={{ textDecoration: 'none', color: 'inherit' }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: 12,
        padding: '12px 14px',
        borderBottom: '1px solid var(--hairline)',
      }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--ink)' }}>
            {task.label || task.type} · Phòng {task.roomNumber || '—'}
          </div>
          <div style={{ fontSize: 12, color: 'var(--mute)', marginTop: 2 }}>
            {task.propertyName || '—'}
            {task.checkOutDate ? ` · Checkout ${task.checkOutDate}` : ''}
          </div>
        </div>
        {statusBadge(task.status)}
      </div>
    </Link>
  );
}

function typeHref(type: EmployeeTaskSummary['type']) {
  if (type === 'HOUSEKEEPING') return '/employee/housekeeping';
  if (type === 'MAINTENANCE') return '/employee/maintenance';
  return '/employee/inspections';
}

export default function EmployeeDashboardPage() {
  const [data, setData] = useState<EmployeeDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'tasks' | 'operations'>('tasks');
  const fallbackName = sessionStorage.getItem('fullName') || 'Nhân viên';

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const res = await getEmployeeDashboard();
        if (!cancelled && res.success) setData(res.data);
      } catch {
        if (!cancelled) setError('Không tải được dashboard. Vui lòng thử lại.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, []);

  const displayName = data?.fullName || fallbackName;

  return (
    <EmployeeLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 32, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <span style={{ fontSize: 11, fontWeight: 800, color: 'var(--primary)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
              Employee Portal
            </span>
            <h1 style={{ fontFamily: 'Outfit', fontSize: 32, fontWeight: 800, color: 'var(--ink)', letterSpacing: '-0.02em', margin: '4px 0' }}>
              {displayName}
            </h1>
            <p className="body-sm text-charcoal" style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6, margin: 0 }}>
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" strokeWidth="2.5" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              {new Date().toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' })}
            </p>
          </div>

          <div style={{ display: 'inline-flex', background: 'var(--surface-bone)', padding: 4, borderRadius: 12, border: '1px solid var(--hairline)', height: 'fit-content' }}>
            <button
              type="button"
              onClick={() => setActiveTab('tasks')}
              style={{
                background: activeTab === 'tasks' ? 'var(--surface-card)' : 'transparent',
                border: 'none',
                padding: '8px 18px',
                fontWeight: 700,
                fontSize: 13,
                cursor: 'pointer',
                color: activeTab === 'tasks' ? 'var(--primary)' : 'var(--charcoal)',
                borderRadius: 8,
                boxShadow: activeTab === 'tasks' ? '0 1px 3px rgba(0,0,0,0.06)' : 'none',
              }}
            >
              Công việc của tôi
            </button>
            <button
              type="button"
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
                boxShadow: activeTab === 'operations' ? '0 1px 3px rgba(0,0,0,0.06)' : 'none',
              }}
            >
              Vận hành & Tra cứu
            </button>
          </div>
        </div>

        {error && <ErrBanner msg={error} />}

        {activeTab === 'tasks' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 28 }}>
            {data?.noPropertyAssignment && (
              <div style={{
                padding: '14px 16px',
                borderRadius: 12,
                background: 'rgba(245, 158, 11, 0.12)',
                color: 'var(--ink)',
                fontSize: 13,
                border: '1px solid rgba(245, 158, 11, 0.35)',
              }}>
                Bạn chưa được gán property. Liên hệ Manager để nhận phân công (FR-20).
              </div>
            )}

            {loading ? (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 20 }}>
                <div style={{ background: 'var(--surface-bone)', height: 120, borderRadius: 16 }} className="animate-pulse" />
                <div style={{ background: 'var(--surface-bone)', height: 120, borderRadius: 16 }} className="animate-pulse" />
                <div style={{ background: 'var(--surface-bone)', height: 120, borderRadius: 16 }} className="animate-pulse" />
              </div>
            ) : (
              <>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 20 }}>
                  <KpiCard
                    label="Dọn dẹp (Housekeeping)"
                    value={data?.pendingHousekeeping ?? 0}
                    sub={`${data?.pendingHousekeeping ?? 0} pending tasks`}
                    link="/employee/housekeeping"
                    color="var(--primary)"
                    icon={<Icons.Housekeeping />}
                  />
                  <KpiCard
                    label="Sửa chữa (Maintenance)"
                    value={data?.pendingMaintenance ?? 0}
                    sub={`${data?.pendingMaintenance ?? 0} pending tasks`}
                    link="/employee/maintenance"
                    color="var(--info)"
                    icon={<Icons.Maintenance />}
                  />
                  <KpiCard
                    label="Kiểm tra (Inspections)"
                    value={data?.pendingInspections ?? 0}
                    sub={`${data?.pendingInspections ?? 0} pending tasks`}
                    link="/employee/inspections"
                    color="var(--purple)"
                    icon={<Icons.Inspection />}
                  />
                </div>

                <section>
                  <SectionHeader title="Tác vụ hôm nay" link="/employee/housekeeping" linkLabel="Xem tất cả" />
                  <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, overflow: 'hidden' }}>
                    {!data?.todayTasks?.length ? (
                      <EmptyState msg="Không có tác vụ hôm nay" />
                    ) : (
                      [...data.todayTasks]
                        .sort((a, b) => {
                          const today = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' });
                          const aPri = a.type === 'INSPECTION' && a.checkOutDate === today ? 0 : 1;
                          const bPri = b.type === 'INSPECTION' && b.checkOutDate === today ? 0 : 1;
                          return aPri - bPri;
                        })
                        .map(t => (
                          <TaskRow key={`${t.type}-${t.id}`} task={t} href={typeHref(t.type)} />
                        ))
                    )}
                  </div>
                </section>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 20 }}>
                  <section>
                    <SectionHeader title="Đang chờ xử lý" link="/employee/housekeeping" linkLabel="Chi tiết" />
                    <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, padding: 12, marginBottom: 8, fontSize: 12, color: 'var(--mute)' }}>
                      HK {data?.awaiting?.housekeepingCount ?? 0} · MT {data?.awaiting?.maintenanceCount ?? 0} · RI {data?.awaiting?.inspectionCount ?? 0}
                    </div>
                    <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, overflow: 'hidden' }}>
                      {!data?.awaiting?.previews?.length ? (
                        <EmptyState msg="Không có tác vụ chờ" />
                      ) : (
                        data.awaiting.previews.map(t => (
                          <TaskRow key={`await-${t.type}-${t.id}`} task={t} href={typeHref(t.type)} />
                        ))
                      )}
                    </div>
                  </section>

                  <section>
                    <SectionHeader title="Hoàn thành hôm nay" link="/employee/housekeeping" linkLabel="Chi tiết" />
                    <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, padding: 12, marginBottom: 8, fontSize: 12, color: 'var(--mute)' }}>
                      HK {data?.completedToday?.housekeepingCount ?? 0} · MT {data?.completedToday?.maintenanceCount ?? 0} · RI {data?.completedToday?.inspectionCount ?? 0}
                    </div>
                    <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, overflow: 'hidden' }}>
                      {!data?.completedToday?.previews?.length ? (
                        <EmptyState msg="Chưa hoàn thành tác vụ nào hôm nay" />
                      ) : (
                        data.completedToday.previews.map(t => (
                          <TaskRow key={`done-${t.type}-${t.id}`} task={t} href={typeHref(t.type)} />
                        ))
                      )}
                    </div>
                  </section>
                </div>

                <section>
                  <SectionHeader title="Housekeeping được gán" link="/employee/housekeeping" linkLabel="Xem tất cả" />
                  <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, overflow: 'hidden' }}>
                    {!data?.housekeepingTasks?.length ? (
                      <EmptyState msg="Không có tác vụ dọn phòng" />
                    ) : (
                      data.housekeepingTasks.map(t => (
                        <TaskRow
                          key={t.id}
                          href="/employee/housekeeping"
                          task={{
                            type: 'HOUSEKEEPING',
                            id: t.id,
                            roomNumber: t.roomNumber,
                            propertyName: t.propertyName,
                            status: t.status,
                            label: 'Dọn phòng',
                          }}
                        />
                      ))
                    )}
                  </div>
                </section>

                <section>
                  <SectionHeader title="Maintenance được gán" link="/employee/maintenance" linkLabel="Xem tất cả" />
                  <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, overflow: 'hidden' }}>
                    {!data?.maintenanceTickets?.length ? (
                      <EmptyState msg="Không có ticket bảo trì" />
                    ) : (
                      data.maintenanceTickets.map(t => (
                        <TaskRow
                          key={t.id}
                          href="/employee/maintenance"
                          task={{
                            type: 'MAINTENANCE',
                            id: t.id,
                            roomNumber: t.roomNumber,
                            propertyName: t.propertyName,
                            status: t.status,
                            label: t.title || 'Bảo trì',
                          }}
                        />
                      ))
                    )}
                  </div>
                </section>

                <section>
                  <SectionHeader title="Inspections" link="/employee/inspections" linkLabel="Xem tất cả" />
                  <div style={{ background: 'var(--surface-card)', border: '1px solid var(--hairline)', borderRadius: 14, overflow: 'hidden' }}>
                    {!data?.inspections?.length ? (
                      <EmptyState msg="Không có kiểm tra phòng" />
                    ) : (
                      data.inspections.map(t => (
                        <TaskRow
                          key={t.id}
                          href="/employee/inspections"
                          task={{
                            type: 'INSPECTION',
                            id: t.id,
                            roomNumber: t.roomNumber,
                            propertyName: t.propertyName,
                            status: t.status,
                            label: 'Kiểm tra phòng',
                            checkOutDate: t.checkOutDate,
                          }}
                        />
                      ))
                    )}
                  </div>
                </section>
              </>
            )}
          </div>
        )}

        {activeTab === 'operations' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 20 }}>
            <QuickLink
              to="/employee/damage"
              label="Lịch sử báo cáo hư hại"
              desc="Danh sách các báo cáo hư hỏng tài sản phòng của bạn"
              icon={<Icons.Damage />}
            />
            <QuickLink
              to="/employee/damage/create"
              label="Báo cáo hư hại mới"
              desc="Khai báo tài sản hỏng hóc phát hiện khi dọn phòng"
              icon={<Icons.CreateDamage />}
            />
          </div>
        )}
      </div>
    </EmployeeLayout>
  );
}

/** Next status in housekeeping flow */
export function hkNext(status: HousekeepingTask['status']): 'IN_PROGRESS' | 'COMPLETED' | null {
  if (status === 'PENDING') return 'IN_PROGRESS';
  if (status === 'IN_PROGRESS') return 'COMPLETED';
  return null;
}

export function hkButtonLabel(status: HousekeepingTask['status']) {
  if (status === 'PENDING') return 'Bắt đầu';
  if (status === 'IN_PROGRESS') return 'Hoàn thành';
  return null;
}

export function hkButtonClass(status: HousekeepingTask['status']) {
  if (status === 'IN_PROGRESS') return 'btn-primary';
  return 'btn-outline';
}
