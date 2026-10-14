import React from 'react';
import { Link } from 'react-router-dom';

// ── Constants ──────────────────────────────────────────────────────────────────

const TOUCH = { minHeight: 48, minWidth: 48 }; // touch target constraint

const fmtVnd = (n: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(n);

const fmtDate = (s: string) =>
  !s ? '—' : new Date(s).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });

function extractErr(err: unknown, fallback: string) {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback;
}

// ── Shared UI ─────────────────────────────────────────────────────────────────

function Spinner() {
  return (
    <div style={{ textAlign: 'center', padding: 40 }}>
      <div style={{
        width: 36, height: 36,
        border: '3px solid var(--hairline)', borderTopColor: 'var(--primary)',
        borderRadius: '50%', animation: 'spin 0.8s linear infinite', margin: '0 auto',
      }} />
      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  );
}

function ErrBanner({ msg }: { msg: string }) {
  return (
    <div className="alert alert-error" style={{ marginBottom: 12 }}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      {msg}
    </div>
  );
}

function OkBanner({ msg }: { msg: string }) {
  return (
    <div className="alert alert-success" style={{ marginBottom: 12 }}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
        <circle cx="12" cy="12" r="10"/><polyline points="9 12 11 14 15 10"/>
      </svg>
      {msg}
    </div>
  );
}

/** Status badge — colour by value */
function StatusBadge({ status }: { status: string }) {
  const MAP: Record<string, { cls: string; label: string }> = {
    PENDING:         { cls: 'badge-warning', label: 'Pending' },
    IN_PROGRESS:     { cls: 'badge-info',    label: 'In Progress' },
    COMPLETED:       { cls: 'badge-success', label: 'Completed' },
    CANCELLED:       { cls: 'badge-neutral', label: 'Cancelled' },
    ASSIGNED:        { cls: 'badge-warning', label: 'Assigned' },
    RESOLVED:        { cls: 'badge-success', label: 'Resolved' },
    CLEAN:           { cls: 'badge-success', label: 'Clean' },
    AVAILABLE:       { cls: 'badge-success', label: 'Available' },
    OCCUPIED:        { cls: 'badge-info',    label: 'Occupied' },
    MAINTENANCE:     { cls: 'badge-warning', label: 'Maintenance' },
    PENDING_CLEANING:{ cls: 'badge-warning', label: 'Needs Cleaning' },
    PENDING_REVIEW:  { cls: 'badge-warning', label: 'Pending Review' },
    // SCR-63 DamageReport statuses
    DRAFT:              { cls: 'badge-neutral', label: 'Draft' },
    PENDING_APPROVAL:   { cls: 'badge-warning', label: 'Pending Approval' },
    APPROVED:           { cls: 'badge-success', label: 'Approved' },
    DISPUTED:           { cls: 'badge-warning', label: 'Disputed' },
    PAID:               { cls: 'badge-success', label: 'Paid' },
    ESCALATED:          { cls: 'badge-error',   label: 'Escalated' },
    REJECTED:           { cls: 'badge-error',   label: 'Rejected' },
    PASS:               { cls: 'badge-success', label: '✓ Pass' },
    FAIL:               { cls: 'badge-error',   label: '✗ Fail' },
  };
  const v = MAP[status] ?? { cls: 'badge-neutral', label: status };
  return <span className={`badge ${v.cls}`}>{v.label}</span>;
}

/** Slide-in drawer from right */
function Drawer({ open, onClose, title, children }: {
  open: boolean; onClose: () => void; title: string; children: React.ReactNode;
}) {
  return (
    <>
      {open && (
        <div onClick={onClose} style={{
          position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
          backdropFilter: 'blur(2px)', zIndex: 998,
        }} />
      )}
      <div style={{
        position: 'fixed', top: 0, right: 0, height: '100%',
        width: 420, maxWidth: '95vw',
        background: 'var(--surface-card)',
        boxShadow: '-6px 0 28px rgba(0,0,0,0.14)',
        zIndex: 999,
        transform: open ? 'translateX(0)' : 'translateX(100%)',
        transition: 'transform 0.28s cubic-bezier(0.4,0,0.2,1)',
        display: 'flex', flexDirection: 'column',
      }}>
        <div style={{ padding: '18px 20px', borderBottom: '1px solid var(--hairline)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0 }}>
          <h3 style={{ fontFamily: 'Outfit', fontWeight: 700, fontSize: 16, color: 'var(--ink)' }}>{title}</h3>
          <button onClick={onClose} style={{ ...TOUCH, background: 'none', border: 'none', cursor: 'pointer', color: 'var(--charcoal)', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 8 }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: 20 }}>{children}</div>
      </div>
    </>
  );
}

/** FAB (Floating Action Button) */
function FAB({ to, label }: { to: string; label: string }) {
  return (
    <Link to={to} style={{
      position: 'fixed', bottom: 24, right: 20,
      width: 56, height: 56, borderRadius: '50%',
      background: 'var(--primary)', color: '#fff',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: 24, fontWeight: 700,
      boxShadow: '0 4px 18px rgba(15,118,110,0.45)',
      textDecoration: 'none', zIndex: 100,
      transition: 'transform 0.15s, box-shadow 0.15s',
    }}
      title={label}
      onMouseEnter={e => { (e.currentTarget as HTMLAnchorElement).style.transform = 'scale(1.1)'; }}
      onMouseLeave={e => { (e.currentTarget as HTMLAnchorElement).style.transform = ''; }}
    >
      +
    </Link>
  );
}

// ── SCR-59: Employee Dashboard ─────────────────────────────────────────────────
export { TOUCH, fmtVnd, fmtDate, extractErr, Spinner, ErrBanner, OkBanner, StatusBadge, Drawer, FAB };