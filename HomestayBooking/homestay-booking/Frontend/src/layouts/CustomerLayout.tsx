import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { notificationApi } from '../api/notificationApi';
import { useNotificationWebSocket } from '../hooks/useNotificationWebSocket';
import CartButton from '../components/ui/CartButton';

const NAV_ITEMS = [
  { icon: IconDashboard,   label: 'Dashboard',     path: '/customer/dashboard' },
  { icon: IconBooking,     label: 'My Bookings',   path: '/customer/bookings' },
  { icon: IconContract,    label: 'Contracts',     path: '/customer/contracts' },
  { icon: IconPayment,     label: 'Payments',      path: '/customer/payments' },
  { icon: IconMaintenance, label: 'Maintenance',   path: '/customer/maintenance' },
  { icon: IconReview,      label: 'My Reviews',    path: '/customer/reviews' },
  { icon: IconComplaint,   label: 'My Complaints', path: '/customer/complaints' },
  { icon: IconNotif,       label: 'Notifications', path: '/customer/notifications' },
  { icon: IconProfile,     label: 'Profile',       path: '/customer/profile' },
];

function IconDashboard() { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>; }
function IconBooking()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>; }
function IconContract()   { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14,2 14,8 20,8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10,9 9,9 8,9"/></svg>; }
function IconPayment()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>; }
function IconMaintenance(){ return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>; }
function IconReview()     { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21.02 12,17.77 5.82,21.02 7,14.14 2,9.27 8.91,8.26 12,2"/></svg>; }
function IconComplaint()  { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>; }
function IconNotif()      { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>; }
function IconProfile()    { return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>; }

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { fullName, email, avatarUrl, logout } = useAuthStore();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  useNotificationWebSocket();

  const fetchUnreadCount = async () => {
    try {
      const res = await notificationApi.getUnreadCount();
      if (res.success) {
        setUnreadCount(res.data.count);
      }
    } catch (err) {
      console.error('Failed to fetch unread count in layout', err);
    }
  };

  useEffect(() => {
    fetchUnreadCount();

    const handleUpdate = () => {
      fetchUnreadCount();
    };

    window.addEventListener('unreadCountChanged', handleUpdate);
    return () => {
      window.removeEventListener('unreadCountChanged', handleUpdate);
    };
  }, [location.pathname]);

  function handleLogout() { logout(); navigate('/login'); }

  const currentItem = NAV_ITEMS.find(n => location.pathname.startsWith(n.path));
  const initials = (fullName || 'U').split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();

  function SidebarContent() {
    return (
      <div className="flex flex-col h-full">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-3 px-6 py-5 flex-shrink-0" style={{
          borderBottom: '1px solid rgba(255,255,255,0.08)',
          textDecoration: 'none',
        }}>
          <div style={{ width: 32, height: 32, background: 'var(--primary)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95"/>
              <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6"/>
            </svg>
          </div>
          <div>
            <p style={{ fontFamily: 'Bricolage Grotesque', fontWeight: 700, fontSize: 14, color: 'var(--on-dark)', lineHeight: 1.2 }}>Homestay&Resort</p>
            <p style={{ fontSize: 11, color: 'var(--primary)', fontWeight: 600, marginTop: 1 }}>Customer Portal</p>
          </div>
        </Link>

        {/* User chip */}
        <div className="px-4 pt-5 flex-shrink-0">
          <div className="flex items-center gap-3 px-4 py-3 rounded-xl" style={{ background: 'rgba(255,255,255,0.06)' }}>
            {avatarUrl ? (
              <img src={avatarUrl} alt={fullName || ''} style={{ width: 38, height: 38, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }} />
            ) : (
              <div style={{
                width: 38, height: 38, borderRadius: '50%', background: 'var(--primary)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 14, fontWeight: 700, color: '#fff', flexShrink: 0,
              }}>{initials}</div>
            )}
            <div className="min-w-0">
              <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--on-dark)', lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{fullName || 'Customer'}</p>
              <p style={{ fontSize: 11, color: 'var(--on-dark-mute)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginTop: 1 }}>{email}</p>
            </div>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto px-3 py-4">
          {NAV_ITEMS.map((item) => {
            const active = location.pathname.startsWith(item.path);
            const Icon = item.icon;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setSidebarOpen(false)}
                className="flex items-center gap-3 px-4 py-2.5 rounded-xl transition-all duration-150 mb-0.5"
                style={{
                  textDecoration: 'none',
                  fontSize: 14, fontWeight: 500,
                  color: active ? 'var(--primary-light)' : 'rgba(255,255,255,0.65)',
                  background: active ? 'rgba(15,118,110,0.18)' : 'transparent',
                  borderLeft: active ? '3px solid var(--primary-light)' : '3px solid transparent',
                }}
                onMouseEnter={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'rgba(255,255,255,0.06)'; }}
                onMouseLeave={e => { if (!active) (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
              >
                <span style={{ width: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><Icon /></span>
                <span style={{ flex: 1 }}>{item.label}</span>
                {item.label === 'Notifications' && unreadCount > 0 && (
                  <span style={{
                    background: 'var(--primary)',
                    color: '#fff',
                    fontSize: 10,
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: 99,
                    lineHeight: 1,
                  }}>{unreadCount}</span>
                )}
              </Link>
            );
          })}
        </nav>

        {/* Logout */}
        <div className="px-3 pb-5 flex-shrink-0" style={{ borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: 16 }}>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-4 py-2.5 rounded-xl transition-all duration-150"
            style={{ color: '#f87171', background: 'rgba(248,113,113,0.10)', border: 'none', cursor: 'pointer', fontSize: 14, fontWeight: 500 }}
            onMouseEnter={e => (e.currentTarget.style.background = 'rgba(248,113,113,0.18)')}
            onMouseLeave={e => (e.currentTarget.style.background = 'rgba(248,113,113,0.10)')}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
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

      {/* Mobile Sidebar Overlay */}
      {sidebarOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex" onClick={() => setSidebarOpen(false)}>
          <div className="absolute inset-0" style={{ background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(4px)' }} />
          <aside className="relative flex flex-col w-3/4 max-w-xs h-full animate-fade-in"
            style={{ background: 'var(--surface-dark)' }}
            onClick={e => e.stopPropagation()}>
            <SidebarContent />
          </aside>
        </div>
      )}

      {/* Main area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Topbar */}
        <header className="sticky top-0 z-40 flex items-center justify-between px-5 lg:px-8"
          style={{ height: 60, background: 'rgba(249,247,243,0.95)', backdropFilter: 'blur(8px)', borderBottom: '1px solid var(--hairline)' }}>
          <div className="flex items-center gap-4">
            <button className="lg:hidden" onClick={() => setSidebarOpen(true)}
              style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 6, color: 'var(--ink)' }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
              </svg>
            </button>
            {/* Breadcrumb */}
            <div className="flex items-center gap-2 body-sm" style={{ color: 'var(--charcoal)' }}>
              <span className="hidden sm:inline">Customer</span>
              {currentItem && <>
                <span className="hidden sm:inline" style={{ color: 'var(--stone)' }}>/</span>
                <span style={{ fontWeight: 600, color: 'var(--ink)' }}>{currentItem.label}</span>
              </>}
            </div>
          </div>
          <div className="flex items-center gap-4">
            <CartButton />
            {/* Notification bell */}
            <Link to="/customer/notifications" className="btn-icon" style={{ border: 'none', background: 'transparent', color: 'var(--charcoal)', position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
              {unreadCount > 0 && (
                <span style={{
                  position: 'absolute',
                  top: -2,
                  right: -2,
                  background: 'var(--primary)',
                  color: '#fff',
                  fontSize: 9,
                  fontWeight: 700,
                  width: 16,
                  height: 16,
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  lineHeight: 1,
                  border: '1.5px solid rgba(249,247,243,0.95)'
                }}>{unreadCount}</span>
              )}
            </Link>
            {/* Avatar */}
            <Link to="/customer/profile" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 16, borderLeft: '1px solid var(--hairline)' }}>
              {avatarUrl ? (
                <img src={avatarUrl} alt="" style={{ width: 34, height: 34, borderRadius: '50%', objectFit: 'cover', border: '1px solid var(--hairline)' }} />
              ) : (
                <div style={{ width: 34, height: 34, borderRadius: '50%', background: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#fff' }}>{initials}</div>
              )}
              <div className="hidden md:block">
                <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)', lineHeight: 1.3 }}>{fullName?.split(' ').pop() || 'User'}</p>
                <p style={{ fontSize: 11, color: 'var(--charcoal)' }}>Customer</p>
              </div>
            </Link>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 p-5 lg:p-8" style={{ maxWidth: 1400, width: '100%', margin: '0 auto' }}>
          {children}
        </main>
      </div>
    </div>
  );
}
