import React from 'react';
import { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import {
  getAdminProperties, createAdminProperty, updateAdminProperty,
  assignManagerToProperty,
  getManagers, getCustomers, updateAdminUser,
  getPaymentReconciliation,
  getEscalatedDamageReports, coApproveDamageReport,
  getAdminComplaints, resolveComplaint,
  getGlobalRevenueReport,
  getSystemSettings, updateSystemSettings,
  getAdminPromotions, createPromotion, updatePromotion, deletePromotion,
  type AdminUser, type AdminProperty, type AdminDamageReport,
  type AdminComplaint, type PaymentReconciliationItem,
  type Promotion, type SystemSettings, type MonthlyRevenue,
} from '../../api/adminApi';
import { DataTable, StatusBadge as UIStatusBadge } from '../../components/ui';

// ── Shared Helpers ─────────────────────────────────────────────────────────────

const fmtVnd = (n: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(n);

const fmtDate = (s: string) => {
  if (!s) return '—';
  return new Date(s).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
};

function extractApiError(err: unknown, fallback: string): string {
  const d = (err as { response?: { data?: { message?: string } } })?.response?.data;
  return d?.message || fallback;
}

// ── Shared UI Components ───────────────────────────────────────────────────────

function Spinner() {
  return (
    <div style={{ textAlign: 'center', padding: 48 }}>
      <div style={{ width: 32, height: 32, border: '3px solid var(--hairline)', borderTopColor: 'var(--primary)', borderRadius: '50%', animation: 'spin 0.8s linear infinite', margin: '0 auto' }} />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

function ErrorBanner({ msg }: { msg: string }) {
  return (
    <div className="alert alert-error" style={{ marginBottom: 16 }}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
        <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
      </svg>
      {msg}
    </div>
  );
}

function SuccessBanner({ msg }: { msg: string }) {
  return (
    <div className="alert alert-success" style={{ marginBottom: 16 }}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
        <circle cx="12" cy="12" r="10"/><polyline points="9 12 11 14 15 10"/>
      </svg>
      {msg}
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const MAP: Record<string, { cls: string; label: string }> = {
    ACTIVE:         { cls: 'badge-success', label: 'Active' },
    INACTIVE:       { cls: 'badge-error',   label: 'Inactive' },
    SUSPENDED:      { cls: 'badge-error',   label: 'Suspended' },
    OPEN:           { cls: 'badge-warning', label: 'Open' },
    INVESTIGATING:  { cls: 'badge-info',    label: 'Investigating' },
    RESOLVED:       { cls: 'badge-success', label: 'Resolved' },
    CLOSED:         { cls: 'badge-neutral', label: 'Closed' },
    ESCALATED:      { cls: 'badge-error',   label: 'Escalated' },
    APPROVED:       { cls: 'badge-success', label: 'Approved' },
    PENDING_REVIEW: { cls: 'badge-warning', label: 'Pending Review' },
    DISCREPANCY:    { cls: 'badge-error',   label: 'Discrepancy' },
    SUCCESS:        { cls: 'badge-success', label: 'Success' },
    PENDING:        { cls: 'badge-warning', label: 'Pending' },
    FAILED:         { cls: 'badge-error',   label: 'Failed' },
  };
  const v = MAP[status] ?? { cls: 'badge-neutral', label: status };
  return <span className={`badge ${v.cls}`}>{v.label}</span>;
}

function Drawer({ open, onClose, title, children }: {
  open: boolean; onClose: () => void; title: string; children: React.ReactNode;
}) {
  return (
    <>
      {/* Backdrop */}
      {open && (
        <div
          onClick={onClose}
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', backdropFilter: 'blur(2px)', zIndex: 998 }}
        />
      )}
      {/* Panel */}
      <div style={{
        position: 'fixed', top: 0, right: 0, height: '100%',
        width: 480, maxWidth: '92vw',
        background: 'var(--surface-card)',
        boxShadow: '-8px 0 32px rgba(0,0,0,0.12)',
        zIndex: 999,
        transform: open ? 'translateX(0)' : 'translateX(100%)',
        transition: 'transform 0.28s cubic-bezier(0.4,0,0.2,1)',
        display: 'flex', flexDirection: 'column',
        overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{ padding: '18px 24px', borderBottom: '1px solid var(--hairline)', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0 }}>
          <h3 style={{ fontFamily: 'Outfit', fontWeight: 700, fontSize: 16, color: 'var(--ink)' }}>{title}</h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--charcoal)', padding: 4 }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
        {/* Body */}
        <div style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
          {children}
        </div>
      </div>
    </>
  );
}

function ConfirmModal({ open, title, message, confirmLabel, onConfirm, onCancel, danger = false }: {
  open: boolean; title: string; message: string; confirmLabel: string;
  onConfirm: () => void; onCancel: () => void; danger?: boolean;
}) {
  if (!open) return null;
  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
      <div className="card" style={{ maxWidth: 440, width: '100%', padding: 28 }}>
        <h3 style={{ fontFamily: 'Outfit', fontWeight: 700, fontSize: 17, marginBottom: 10, color: 'var(--ink)' }}>{title}</h3>
        <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>{message}</p>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
          <button className="btn-ghost" onClick={onCancel}>Hủy</button>
          <button className={danger ? 'btn-danger' : 'btn-primary'} onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}

function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div style={{ display: 'flex', gap: 6, justifyContent: 'center', marginTop: 20 }}>
      <button className="btn-ghost btn-sm" disabled={page === 0} onClick={() => onPage(page - 1)}>‹ Trước</button>
      <span className="body-sm" style={{ padding: '4px 12px', alignSelf: 'center' }}>Trang {page + 1} / {totalPages}</span>
      <button className="btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => onPage(page + 1)}>Sau ›</button>
    </div>
  );
}


export { fmtVnd, fmtDate, extractApiError, Spinner, ErrorBanner, SuccessBanner, StatusBadge, Drawer, ConfirmModal, Pagination };
