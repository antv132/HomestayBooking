import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

const NAV_SECTIONS = [
  {
    label: 'OVERVIEW',
    items: [
      { label: 'Dashboard',       path: '/employee/dashboard',    icon: IconDashboard },
    ],
  },
  {
    label: 'OPERATIONS',
    items: [
      { label: 'Housekeeping',    path: '/employee/housekeeping', icon: IconBroom },
      { label: 'Maintenance',     path: '/employee/maintenance',  icon: IconWrench },
      { label: 'Kiểm tra phòng',  path: '/employee/inspections',  icon: IconClipboard },
      { label: 'Báo cáo hư hại',  path: '/employee/damage',       icon: IconAlert },
    ],
  },
];

function IconDashboard() { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>; }
function IconBroom()     { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 20l2-8h12l2 8H4z"/><path d="M12 4v8"/><path d="M8 8h8"/></svg>; }
function IconWrench()    { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>; }
function IconClipboard() { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="15" y2="16"/></svg>; }
function IconAlert()     { return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>; }

export default function EmployeeLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { fullName, avatarUrl, logout } = useAuthStore();
  const [mobileOpen, setMobileOpen] = useState(false);

  function handleLogout() { logout(); navigate('/login'); }

  const allItems = NAV_SECTIONS.flatMap(s => s.items);
  const currentItem = allItems.find(n => location.pathname.startsWith(n.path));
  const initials = (fullName || 'E').split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();

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
            <p style={{ fontSize: 11, color: 'var(--primary)', fontWeight: 600, marginTop: 1 }}>Employee Portal</p>
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
              <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--on-dark)', lineHeight: 1.3, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{fullName || 'Employee'}</p>
              <p style={{ fontSize: 11, color: 'var(--on-dark-mute)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginTop: 1 }}>Employee</p>
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
              <span className="hidden sm:inline">Employee</span>
              {currentItem && <>
                <span className="hidden sm:inline" style={{ color: 'var(--stone)' }}>/</span>
                <span style={{ fontWeight: 600, color: 'var(--ink)' }}>{currentItem.label}</span>
              </>}
            </div>
          </div>
          <div className="flex items-center gap-4">
            <Link to="/employee/notifications" className="btn-icon" style={{ border: 'none', background: 'transparent', color: 'var(--charcoal)' }}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/>
              </svg>
            </Link>
            <Link to="/employee/profile" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 16, borderLeft: '1px solid var(--hairline)' }}>
              {avatarUrl ? (
                <img src={avatarUrl} alt="" style={{ width: 34, height: 34, borderRadius: '50%', objectFit: 'cover', border: '1px solid var(--hairline)' }} />
              ) : (
                <div style={{ width: 34, height: 34, borderRadius: '50%', background: 'var(--primary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#fff' }}>{initials}</div>
              )}
              <div className="hidden md:block">
                <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink)', lineHeight: 1.3 }}>{fullName?.split(' ').pop() || 'Employee'}</p>
                <p style={{ fontSize: 11, color: 'var(--primary)' }}>Employee</p>
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
