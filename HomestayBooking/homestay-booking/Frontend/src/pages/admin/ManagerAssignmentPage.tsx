import { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
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
import { fmtVnd, fmtDate, extractApiError, Spinner, ErrorBanner, SuccessBanner, StatusBadge, Drawer, ConfirmModal, Pagination } from './_adminShared';

export function ManagerAssignmentPage() {
  const { id } = useParams<{ id: string }>();
  const [managers, setManagers] = useState<AdminUser[]>([]);
  const [selectedId, setSelectedId] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [assignedOk, setAssignedOk] = useState(false);

  useEffect(() => {
    async function load() {
      setFetching(true);
      try {
        const res = await getManagers({ size: 100, status: 'ACTIVE' });
        if (res.success) setManagers(res.data.content);
      } catch { /* silent */ }
      finally { setFetching(false); }
    }
    load();
  }, []);

  const filtered = managers.filter(m =>
    !keyword || m.fullName.toLowerCase().includes(keyword.toLowerCase()) || m.email.toLowerCase().includes(keyword.toLowerCase())
  );

  async function handleSave() {
    if (!id || !selectedId) return;
    setLoading(true); setError(null);
    try {
      const res = await assignManagerToProperty(id, selectedId);
      if (res.success) setAssignedOk(true);
      else setError('Gán manager thất bại.');
    } catch (err) { setError(extractApiError(err, 'Gán manager thất bại.')); }
    finally { setLoading(false); }
  }

  return (
    <AdminLayout>
      <div className="animate-fade-in" style={{ maxWidth: 560, margin: '0 auto' }}>
        <div style={{ marginBottom: 24 }}>
          <Link to="/admin/properties" className="body-sm text-primary" style={{ textDecoration: 'none' }}>← Properties</Link>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)', marginTop: 8, marginBottom: 4 }}>Manager Assignment</h1>
        </div>
        {error && <ErrorBanner msg={error} />}
        {assignedOk && (
          <div className="card" style={{ padding: 20, marginBottom: 16 }}>
            <p style={{ margin: '0 0 12px', fontWeight: 600 }}>Đã gán Manager thành công.</p>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <Link to={`/admin/properties/${id}/edit`} className="btn-primary">
                Activate property
              </Link>
              <Link to="/admin/properties" className="btn-ghost">Về danh sách</Link>
            </div>
          </div>
        )}
        {!assignedOk && (
        <div className="card" style={{ padding: 24 }}>
          <label className="form-label" htmlFor="manager-search">Tìm Manager</label>
          <input id="manager-search" className="input" placeholder="Tìm theo tên hoặc email..."
            value={keyword} onChange={e => setKeyword(e.target.value)} style={{ marginBottom: 16 }} />
          {fetching ? <Spinner /> : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 320, overflowY: 'auto', marginBottom: 20 }}>
              {filtered.length === 0 ? (
                <p className="body-sm text-charcoal" style={{ textAlign: 'center', padding: 20 }}>Không tìm thấy manager</p>
              ) : filtered.map(m => (
                <label key={m.id} style={{
                  display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px',
                  border: `1.5px solid ${selectedId === m.id ? 'var(--primary)' : 'var(--hairline)'}`,
                  borderRadius: 10, cursor: 'pointer',
                  background: selectedId === m.id ? 'rgba(15,118,110,0.08)' : 'var(--surface-card)',
                  transition: 'all 0.15s',
                }}>
                  <input type="radio" name="manager" value={m.id} checked={selectedId === m.id}
                    onChange={() => setSelectedId(m.id)} style={{ accentColor: 'var(--primary)' }} />
                  <div style={{ width: 36, height: 36, borderRadius: '50%', background: 'var(--surface-bone)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 14, flexShrink: 0, color: 'var(--primary)' }}>
                    {m.fullName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p style={{ fontWeight: 600, fontSize: 14 }}>{m.fullName}</p>
                    <p className="body-sm text-charcoal">{m.email}</p>
                  </div>
                  <StatusBadge status={m.status} />
                </label>
              ))}
            </div>
          )}
          <div style={{ display: 'flex', gap: 10 }}>
            <button className="btn-primary" disabled={!selectedId || loading} onClick={handleSave}>
              {loading ? 'Đang lưu...' : 'Save Assignment'}
            </button>
            <Link to="/admin/properties" className="btn-ghost">Hủy</Link>
          </div>
        </div>
        )}
      </div>
    </AdminLayout>
  );
}

