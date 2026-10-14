// ─── SCR-59: Reports Dashboard ────────────────────────────────────────────────
import { Link } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';

export function ReportsDashboardPage() {
  return (
    <ManagerLayout>
      <h1 className="heading-md" style={{ marginBottom: 8 }}>Reports &amp; Analytics</h1>
      <p className="body-md text-charcoal" style={{ marginBottom: 28 }}>Comprehensive overview of your business performance.</p>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {[
          { to: '/manager/reports/revenue', icon: '📈', title: 'Revenue Report', desc: 'Monthly and annual revenue breakdown', color: 'var(--primary)' },
          { to: '/manager/reports/occupancy', icon: '🏠', title: 'Occupancy Rate', desc: 'Room occupancy trends by property', color: 'var(--success)' },
          { to: '/manager/reports/bookings', icon: '📋', title: 'Booking Trends', desc: 'Booking volume over time', color: 'var(--info)' },
        ].map(r => (
          <Link key={r.to} to={r.to} style={{ textDecoration: 'none' }}>
            <div className="card" style={{ padding: 28, transition: 'transform 0.2s, box-shadow 0.2s', cursor: 'pointer' }}
              onMouseEnter={e => { (e.currentTarget as HTMLElement).style.transform = 'translateY(-3px)'; (e.currentTarget as HTMLElement).style.boxShadow = '0 8px 24px rgba(32,32,32,0.1)'; }}
              onMouseLeave={e => { (e.currentTarget as HTMLElement).style.transform = ''; (e.currentTarget as HTMLElement).style.boxShadow = ''; }}>
              <div style={{ fontSize: 36, marginBottom: 16 }}>{r.icon}</div>
              <h3 className="heading-sm" style={{ marginBottom: 6, color: r.color }}>{r.title}</h3>
              <p className="body-sm text-charcoal">{r.desc}</p>
            </div>
          </Link>
        ))}
      </div>
    </ManagerLayout>
  );
}
