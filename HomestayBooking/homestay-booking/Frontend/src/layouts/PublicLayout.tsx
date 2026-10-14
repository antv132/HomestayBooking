import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import CartButton from '../components/ui/CartButton';

const NAV_LINKS = [
  { label: 'Rooms', path: '/rooms' },
  { label: 'Home', path: '/' },
  { label: 'About', path: '/about' },
];

const FOOTER_COLUMNS = [
  {
    title: 'Explore',
    links: [
      { label: 'Rooms', to: '/rooms' },
      { label: 'Homestay', to: '/rooms' },
      { label: 'Resort & Villa', to: '/rooms' },
      { label: 'Best Deals', to: '/rooms' },
    ],
  },
  {
    title: 'Support',
    links: [
      { label: 'Help Center', to: '/about' },
      { label: 'Contact Us', to: '/about' },
      { label: 'Report Issue', to: '/about' },
      { label: 'FAQ', to: '/about' },
    ],
  },
  {
    title: 'Company',
    links: [
      { label: 'About', to: '/about' },
      { label: 'Privacy Policy', to: '/about' },
      { label: 'Terms of Service', to: '/about' },
      { label: 'Careers', to: '/about' },
    ],
  },
];

const FOOTER_BOTTOM_LINKS = [
  { label: 'Privacy Policy', to: '/about' },
  { label: 'Terms', to: '/about' },
  { label: 'Sitemap', to: '/' },
];

function Logo() {
  return (
    <div className="flex items-center gap-2">
      <div style={{
        width: 32, height: 32,
        background: 'var(--primary)',
        borderRadius: 8,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95" />
          <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6" />
        </svg>
      </div>
      <span className="display-md" style={{ fontSize: 18, letterSpacing: '-0.5px', color: 'var(--ink)' }}>
        Homestay<span style={{ color: 'var(--primary)' }}>&</span>Resort
      </span>
    </div>
  );
}

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated, role, logout } = useAuthStore();

  const handleLogout = () => { logout(); navigate('/'); };
  const dashboardLink = role === 'MANAGER' ? '/manager/dashboard' : '/customer/dashboard';
  // Giỏ phòng chỉ dành cho khách (chưa đăng nhập hoặc CUSTOMER)
  const showCart = !isAuthenticated || role === 'CUSTOMER';

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--canvas)' }}>

      {/* ── Navbar ── */}
      <nav
        className="sticky top-0 z-50"
        style={{
          height: 60,
          background: 'rgba(249,247,243,0.92)',
          backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          borderBottom: '1px solid var(--hairline)',
        }}
      >
        <div className="container-wide h-full flex items-center justify-between">
          {/* Logo */}
          <Link to="/" style={{ textDecoration: 'none' }}>
            <Logo />
          </Link>

          {/* Desktop Nav Links */}
          <div className="hidden md:flex items-center gap-1">
            {NAV_LINKS.map((link) => {
              const isHash = link.path.startsWith('/#');
              const active = isHash
                ? location.pathname === '/' && location.hash === link.path.slice(1)
                : location.pathname === link.path || location.pathname.startsWith(link.path + '/');
              return (
                <Link
                  key={link.path}
                  to={link.path}
                  style={{
                    display: 'inline-flex', alignItems: 'center',
                    padding: '6px 14px',
                    borderRadius: 9999,
                    fontSize: 14, fontWeight: 600,
                    textDecoration: 'none',
                    color: active ? 'var(--primary)' : 'var(--charcoal)',
                    background: active ? 'rgba(15,118,110,0.08)' : 'transparent',
                    transition: 'all 0.15s',
                  }}
                  onMouseEnter={e => { if (!active) (e.currentTarget as HTMLElement).style.color = 'var(--ink)'; }}
                  onMouseLeave={e => { if (!active) (e.currentTarget as HTMLElement).style.color = 'var(--charcoal)'; }}
                >
                  {link.label}
                </Link>
              );
            })}
          </div>

          {/* Desktop CTA */}
          <div className="hidden md:flex items-center gap-2">
            {showCart && <CartButton />}
            {isAuthenticated ? (
              <>
                <Link to={dashboardLink} className="btn-outline btn-sm">Dashboard</Link>
                <button onClick={handleLogout} className="btn-ghost btn-sm">Logout</button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-ghost btn-sm">Log In</Link>
                <Link to="/register" className="btn-primary btn-sm">Register</Link>
              </>
            )}
          </div>

          {/* Mobile cart + hamburger */}
          <div className="md:hidden flex items-center gap-1">
            {showCart && <CartButton onClick={() => setMenuOpen(false)} />}
            <button
              onClick={() => setMenuOpen(!menuOpen)}
              aria-label="Toggle menu"
              style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 8, color: 'var(--ink)' }}
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                {menuOpen ? (
                  <><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></>
                ) : (
                  <><line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" /></>
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div className="md:hidden animate-fade-in" style={{
            background: 'var(--surface-card)',
            borderTop: '1px solid var(--hairline)',
            padding: '12px 24px 20px',
            boxShadow: '0 8px 24px rgba(32,32,32,0.10)',
          }}>
            {NAV_LINKS.map((link) => (
              <Link
                key={link.path}
                to={link.path}
                onClick={() => setMenuOpen(false)}
                style={{
                  display: 'block', padding: '12px 0', fontSize: 15, fontWeight: 600,
                  textDecoration: 'none',
                  color: location.pathname === link.path ? 'var(--primary)' : 'var(--ink)',
                  borderBottom: '1px solid var(--hairline)',
                }}
              >
                {link.label}
              </Link>
            ))}
            <div className="flex gap-3 pt-4">
              {isAuthenticated ? (
                <>
                  <Link to={dashboardLink} className="btn-primary flex-1" style={{ justifyContent: 'center' }}>Dashboard</Link>
                  <button onClick={handleLogout} className="btn-outline flex-1">Logout</button>
                </>
              ) : (
                <>
                  <Link to="/login" className="btn-outline flex-1" style={{ justifyContent: 'center' }}>Log In</Link>
                  <Link to="/register" className="btn-primary flex-1" style={{ justifyContent: 'center' }}>Register</Link>
                </>
              )}
            </div>
          </div>
        )}
      </nav>

      {/* ── Main Content ── */}
      <main className="flex-1">{children}</main>

      {/* ── Footer ── */}
      <footer style={{ background: 'var(--surface-deep)', color: 'var(--on-dark)', padding: '64px 32px 32px' }}>
        <div className="container-wide">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-10 pb-10" style={{ borderBottom: '1px solid var(--divider-dark)' }}>
            {/* Brand */}
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div style={{ width: 30, height: 30, background: 'var(--primary)', borderRadius: 7, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95" />
                    <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6" />
                  </svg>
                </div>
                <span className="font-display" style={{ fontWeight: 700, fontSize: 15, color: 'var(--on-dark)' }}>
                  Homestay&Resort
                </span>
              </div>
              <p className="body-sm" style={{ color: 'var(--on-dark-mute)', lineHeight: 1.75, marginBottom: 20 }}>
                Nền tảng đặt phòng homestay &amp; resort trực tuyến — tìm kiếm, đặt phòng và thanh toán nhanh chóng, an toàn.
              </p>
              <div className="flex gap-3">
                {[
                  { label: 'Facebook', href: 'https://facebook.com' },
                  { label: 'Instagram', href: 'https://instagram.com' },
                  { label: 'YouTube', href: 'https://youtube.com' },
                ].map((s) => (
                  <a
                    key={s.label}
                    href={s.href}
                    target="_blank"
                    rel="noopener noreferrer"
                    aria-label={s.label}
                    className="caption"
                    style={{
                      color: 'var(--on-dark-mute)',
                      textDecoration: 'none',
                      padding: '6px 10px',
                      borderRadius: 9999,
                      border: '1px solid var(--divider-dark)',
                      transition: 'color 0.15s, border-color 0.15s',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.color = 'var(--on-dark)';
                      e.currentTarget.style.borderColor = 'rgba(252,252,252,0.4)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.color = 'var(--on-dark-mute)';
                      e.currentTarget.style.borderColor = 'var(--divider-dark)';
                    }}
                  >
                    {s.label}
                  </a>
                ))}
              </div>
            </div>
            {/* Link columns */}
            {FOOTER_COLUMNS.map((col) => (
              <div key={col.title}>
                <h4 className="label-sm mb-5" style={{ color: 'rgba(252,252,252,0.4)' }}>{col.title}</h4>
                <ul className="space-y-3">
                  {col.links.map((l) => (
                    <li key={l.label}>
                      <Link
                        to={l.to}
                        className="body-sm"
                        style={{ color: 'var(--on-dark-mute)', textDecoration: 'none', transition: 'color 0.15s' }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--on-dark)')}
                        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--on-dark-mute)')}
                      >
                        {l.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          {/* Bottom bar */}
          <div className="flex flex-col md:flex-row justify-between items-center pt-8 gap-4">
            <p className="caption" style={{ color: 'var(--on-dark-mute)' }}>© 2026 Homestay &amp; Resort. All rights reserved.</p>
            <div className="flex gap-5">
              {FOOTER_BOTTOM_LINKS.map((t) => (
                <Link
                  key={t.label}
                  to={t.to}
                  className="caption"
                  style={{ color: 'var(--on-dark-mute)', textDecoration: 'none' }}
                  onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--on-dark)')}
                  onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--on-dark-mute)')}
                >
                  {t.label}
                </Link>
              ))}
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
