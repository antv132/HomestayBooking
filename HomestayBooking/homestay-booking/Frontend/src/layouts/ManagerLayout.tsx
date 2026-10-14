import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { notificationApi } from '../api/notificationApi';
import { useNotificationWebSocket } from '../hooks/useNotificationWebSocket';

const NAV_SECTIONS = [
  {
    label: 'OVERVIEW',
    items: [
      { label: 'Dashboard',        path: '/manager/dashboard',   icon: IconDashboard },
    ],
  },
  {
    label: 'PROPERTY & ROOMS',
    items: [
      { label: 'My Property',      path: '/manager/properties',  icon: IconProperty },
      { label: 'Structure',        path: '/manager/structure',   icon: IconStructure },
      { label: 'Rooms',            path: '/manager/rooms',       icon: IconRoom },
    ],
  },
  {
    label: 'BOOKINGS',
    items: [
      { label: 'Booking List',     path: '/manager/bookings',    icon: IconBooking },
      { label: 'Contracts',        path: '/manager/contracts',   icon: IconContract },
    ],
  },
  {
    label: 'FINANCE',
    items: [
      { label: 'Payments',         path: '/manager/payments',    icon: IconPayment },
    ],
  },
  {
    label: 'OPERATIONS',
    items: [
      { label: 'Lịch dọn phòng',   path: '/manager/housekeeping/schedule', icon: IconHousekeeping },
      { label: 'Tác vụ dọn phòng', path: '/manager/housekeeping/tasks',    icon: IconHousekeeping },
      { label: 'Tác vụ bảo trì',   path: '/manager/maintenance/tasks',     icon: IconMaintenance },
      { label: 'Kiểm tra phòng',   path: '/manager/inspections',           icon: IconInspection },
      { label: 'Báo cáo hư hại',   path: '/manager/damage-reports',        icon: IconDamage },
      { label: 'Nhân viên',        path: '/manager/employees',   icon: IconEmployee },
      { label: 'Customers',        path: '/manager/customers',   icon: IconCustomer },
    ],
  },
  {
    label: 'REPORTING',
    items: [
      { label: 'Báo cáo tổng hợp', path: '/manager/reports/property',      icon: IconTrend },
      { label: 'Revenue',          path: '/manager/reports/revenue',       icon: IconRevenue },
    ],
  },
];

function IconDashboard()   { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>; }
function IconProperty()    { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>; }
function IconStructure()   { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="22,12 18,12 15,21 9,3 6,12 2,12"/></svg>; }
function IconRoom()        { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>; }
function IconBooking()     { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>; }
function IconContract()    { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/></svg>; }
function IconPayment()     { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>; }
function IconCustomer()    { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>; }
function IconMaintenance() { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>; }
function IconHousekeeping() { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3v3M9 6h6M6 9l-2 11h16L18 9M8 9V7a4 4 0 0 1 8 0v2"/></svg>; }
function IconInspection()  { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>; }
function IconDamage()      { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>; }
function IconEmployee()    { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="19" y1="8" x2="19" y2="14"/><line x1="22" y1="11" x2="16" y2="11"/></svg>; }
function IconRevenue()     { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>; }
function IconTrend()       { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="23,6 13.5,15.5 8.5,10.5 1,18"/><polyline points="17,6 23,6 23,12"/></svg>; }

export default function ManagerLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { fullName, email, avatarUrl, logout } = useAuthStore();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  useNotificationWebSocket();

  useEffect(() => {
    const load = () => {
      void notificationApi.getUnreadCount()
        .then(res => { if (res.success) setUnreadCount(res.data.count); })
        .catch(() => undefined);
    };
    load();
    window.addEventListener('unreadCountChanged', load);
    return () => window.removeEventListener('unreadCountChanged', load);
  }, [location.pathname]);

  function handleLogout() { logout(); navigate('/login'); }

  const allItems = NAV_SECTIONS.flatMap(s => s.items);
  const currentItem = allItems.find(n => location.pathname.startsWith(n.path));
  const initials = (fullName || 'M').split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();

  function SidebarContent() {
    return (
      <div className="flex flex-col h-full">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-3 px-5 py-5 flex-shrink-0" style={{ borderBottom: '1px solid rgba(255,255,255,0.08)', textDecoration: 'none' }}>
          <div style={{ width: 32, height: 32, background: 'var(--primary)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95"/>
              <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6"/>
            </svg>
          </div>
          <div>
            <p style={{ fontFamily: 'Bricolage Grotesque', fontWeight: 700, fontSize: 14, color: 'var(--on-dark)', lineHeight: 1.2 }}>Homestay&Resort</p>
            <p style={{ fontSize: 11, color: 'var(--primary)', fontWeight: 600, marginTop: 1 }}>Manager Portal</p>
          </div>
        </Link>

        {/* User chip */}
        <div className="px-3 pt-4 flex-shrink-0">
          <div className="flex items-center gap-3 px-3 py-3 rounded-xl" style={{ background: 'rgba(255,255,255,0.05)' }}>
            {avatarUrl ? (
              <img src={avatarUrl} alt="" style={{ width: 36, height: 36, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }} />
            ) : (
              <div style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#fff', flexShrink: 0 }}>{initials}</div>
            )}
            <div className="min-w-0">
              <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--on-dark)', lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{fullName || 'Manager'}</p>
              <p style={{ fontSize: 11, color: 'var(--on-dark-mute)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginTop: 1 }}>Manager</p>
            </div>
          </div>
        </div>

        {/* Nav sections */}
        <nav className="flex-1 overflow-y-auto px-3 py-3">
          {NAV_SECTIONS.map(section => (
            <div key={section.label} className="mb-4">
              <p style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.08em', color: 'rgba(252,252,252,0.25)', paddingLeft: 12, paddingBottom: 6, textTransform: 'uppercase' }}>
                {section.label}
              </p>
              {section.items.map(item => {
                const active = location.pathname.startsWith(item.path);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    onClick={() => setMobileOpen(false)}
                    className="flex items-center gap-3 px-3 py-2 rounded-lg mb-0.5 transition-all duration-150"
                    style={{
                      textDecoration: 'none',
                      fontSize: 13, fontWeight: 500,
                      color: active ? 'var(--primary-light)' : 'rgba(255,255,255,0.60)',
                      background: active ? 'rgba(15,118,110,0.18)' : 'transparent',
                    }}
                    onMouseEnter={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.06)'; }}
                    onMouseLeave={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                  >
                    <span style={{ width: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><Icon /></span>
                    {item.label}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>

        {/* Logout */}
        <div className="px-3 pb-4 flex-shrink-0" style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 12 }}>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg transition-all duration-150"
            style={{ color: '#f87171', background: 'rgba(248,113,113,0.10)', border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: 500 }}
            onMouseEnter={e => (e.currentTarget.style.background = 'rgba(248,113,113,0.18)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'rgba(248,113,113,0.10)')}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            Sign Out
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex" style={{ background: 'var(--canvas)' }}>
      {/* Desktop Sidebar */}
      <aside className="hidden lg:flex flex-col h-screen sticky top-0 flex-shrink-0"
        style={{ width: 240, background: 'var(--surface-dark)', borderRight: '1px solid rgba(255,255,255,0.06)' }}>
        <SidebarContent />
      </aside>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex" onClick={() => setMobileOpen(false)}>
          <div className="absolute inset-0" style={{ background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)' }} />
          <aside className="relative flex flex-col w-72 h-full animate-fade-in"
            style={{ background: 'var(--surface-dark)' }} onClick={e => e.stopPropagation()}>
            <SidebarContent />
          </aside>
        </div>
      )}

      {/* Main */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Topbar */}
        <header className="sticky top-0 z-40 flex items-center justify-between px-5 lg:px-8"
          style={{ height: 60, background: 'rgba(248,250,252,0.95)', backdropFilter: 'blur(8px)', borderBottom: '1px solid var(--hairline)' }}>
          <div className="flex items-center gap-4">
            <button className="lg:hidden" onClick={() => setMobileOpen(true)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 6, color: 'var(--ink)' }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
              </svg>
            </button>
            {/* Breadcrumb */}
            <div className="flex items-center gap-2 body-sm" style={{ color: 'var(--charcoal)' }}>
              <span className="hidden sm:inline">Manager</span>
              {currentItem && <>
                <span className="hidden sm:inline" style={{ color: 'var(--stone)' }}>/</span>
                <span style={{ fontWeight: 600, color: 'var(--ink)' }}>{currentItem.label}</span>
              </>}
            </div>
          </div>
          <div className="flex items-center gap-4">
            <Link to="/manager/notifications" className="btn-icon" style={{ border: 'none', background: 'transparent', color: 'var(--charcoal)', position: 'relative' }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
              {unreadCount > 0 && (
                <span style={{
                  position: 'absolute', top: -2, right: -2, minWidth: 16, height: 16,
                  borderRadius: 8, background: '#DC2626', color: '#fff', fontSize: 10,
                  fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  padding: '0 4px',
                }}>{unreadCount}</span>
              )}
            </Link>
            <Link to="/manager/profile" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 16, borderLeft: '1px solid var(--hairline)' }}>
              {avatarUrl ? (
                <img src={avatarUrl} alt="" style={{ width: 34, height: 34, borderRadius: '50%', objectFit: 'cover', border: '1px solid var(--hairline)' }} />
              ) : (
                <div style={{ width: 34, height: 34, borderRadius: '50%', background: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#fff' }}>{initials}</div>
              )}
              <div className="hidden md:block">
                <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)', lineHeight: 1.3 }}>{fullName?.split(' ').pop() || 'Manager'}</p>
                <p style={{ fontSize: 11, color: 'var(--primary)' }}>Manager</p>
              </div>
            </Link>
          </div>
        </header>

        {/* Content */}
        <main className="flex-1 p-5 lg:p-8" style={{ maxWidth: 1400, width: '100%', margin: '0 auto' }}>
          {children}
        </main>
      </div>
    </div>
  );
}
