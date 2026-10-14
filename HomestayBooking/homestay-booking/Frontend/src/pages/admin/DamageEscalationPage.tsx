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
import { fmtVnd, fmtDate, extractApiError, Spinner, ErrorBanner, SuccessBanner, StatusBadge, Drawer, ConfirmModal, Pagination } from './_adminShared';

const ESCALATION_THRESHOLD = 5000000;

export function DamageEscalationPage() {
  const [items, setItems] = useState<AdminDamageReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selected, setSelected] = useState<AdminDamageReport | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [feeInput, setFeeInput] = useState('');
  const [approveLoading, setApproveLoading] = useState(false);
  const [approveMsg, setApproveMsg] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const load = useCallback(async (p = 0) => {
    setLoading(true); setError(null);
    try {
      const res = await getEscalatedDamageReports({ page: p, size: 10 });
      if (res.success) { setItems(res.data.content); setTotalPages(res.data.totalPages); setPage(p); }
    } catch (err) { setError(extractApiError(err, 'Không tải được danh sách.')); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(0); }, [load]);

  const columns = [
    { header: 'Property', accessor: (item: AdminDamageReport) => <span style={{ fontWeight: 600 }}>{item.propertyName}</span> },
    { header: 'Phòng', accessor: (item: AdminDamageReport) => item.roomName },
    { header: 'Báo cáo bởi', accessor: (item: AdminDamageReport) => item.reportedBy },
    { header: 'Phí ước tính', accessor: (item: AdminDamageReport) => <span style={{ fontWeight: 700, color: '#dc2626' }}>{fmtVnd(item.totalFee)}</span> },
    { header: 'Trạng thái', accessor: (item: AdminDamageReport) => <StatusBadge status={item.status} /> },
    { header: 'Ngày tạo', accessor: (item: AdminDamageReport) => fmtDate(item.createdAt) }
  ];

  const actions = [
    { label: 'Xem & Duyệt', onClick: (item: AdminDamageReport) => { setSelected(item); setFeeInput(String(item.totalFee)); setApproveMsg(null); setDrawerOpen(true); } }
  ];

  async function handleCoApprove() {
    if (!selected) return;
    const fee = parseFloat(feeInput.replace(/[^0-9]/g, ''));
    if (isNaN(fee) || fee <= 0) { setApproveMsg({ type: 'error', msg: 'Vui lòng nhập phí hợp lệ.' }); return; }
    setApproveLoading(true); setApproveMsg(null); setConfirmOpen(false);
    try {
      const res = await coApproveDamageReport(selected.id, fee);
      if (res.success) {
        setApproveMsg({ type: 'success', msg: 'Co-approve thành công!' });
        load(page);
        setTimeout(() => setDrawerOpen(false), 1200);
      } else { setApproveMsg({ type: 'error', msg: 'Co-approve thất bại.' }); }
    } catch (err) { setApproveMsg({ type: 'error', msg: extractApiError(err, 'Co-approve thất bại.') }); }
    finally { setApproveLoading(false); }
  }

  const parsedFee = parseFloat(feeInput.replace(/[^0-9]/g, '')) || 0;

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)', marginBottom: 4 }}>Damage Escalation</h1>
        </div>

        {items.length > 0 && (
          <div className="alert alert-error" style={{ marginBottom: 16 }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <strong>{items.length}</strong> báo cáo hư hại cần Admin co-approve — phí &gt; {fmtVnd(ESCALATION_THRESHOLD)}
          </div>
        )}

        {error && <ErrorBanner msg={error} />}
        {loading ? <Spinner /> : (
          <>
            <div style={{ marginBottom: 20 }}>
              {items.length === 0 ? (
                <div style={{ textAlign: 'center', color: 'var(--charcoal)', padding: 32 }}>✅ Không có damage report nào cần escalation</div>
              ) : (
                <DataTable
                  columns={columns}
                  data={items}
                  keyExtractor={(item) => item.id}
                  actions={actions}
                />
              )}
            </div>
            <Pagination page={page} totalPages={totalPages} onPage={p => load(p)} />
          </>
        )}

        {/* Drawer */}
        <Drawer open={drawerOpen} onClose={() => { setDrawerOpen(false); setSelected(null); }} title="Co-Approve Damage Report">
          {selected && (
            <div>
              {approveMsg && (approveMsg.type === 'success' ? <SuccessBanner msg={approveMsg.msg} /> : <ErrorBanner msg={approveMsg.msg} />)}

              {/* Details */}
              <div style={{ marginBottom: 20 }}>
                {[
                  { label: 'Property', value: selected.propertyName },
                  { label: 'Phòng', value: selected.roomName },
                  { label: 'Báo cáo bởi', value: selected.reportedBy },
                  { label: 'Phí tổng ước tính', value: fmtVnd(selected.totalFee) },
                ].map(r => (
                  <div key={r.label} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--hairline)' }}>
                    <span className="body-sm text-charcoal">{r.label}</span>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{r.value}</span>
                  </div>
                ))}
              </div>

              {/* Damage items */}
              {selected.items.length > 0 && (
                <div style={{ marginBottom: 20 }}>
                  <p style={{ fontWeight: 700, marginBottom: 10 }}>Chi tiết hư hại:</p>
                  {selected.items.map((item, i) => (
                    <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 10px', background: 'var(--surface-bone)', borderRadius: 6, marginBottom: 4 }}>
                      <span className="body-sm">{item.name}</span>
                      <span style={{ fontWeight: 600, fontSize: 13, color: '#dc2626' }}>{fmtVnd(item.estimatedCost)}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Damage photos */}
              {selected.attachments.filter(a => a.type === 'IMAGE').length > 0 && (
                <div style={{ marginBottom: 20 }}>
                  <p style={{ fontWeight: 700, marginBottom: 10 }}>Ảnh hư hại:</p>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    {selected.attachments.filter(a => a.type === 'IMAGE').map((att, i) => (
                      <img key={i} src={att.url} alt={`Damage ${i + 1}`}
                        style={{ width: '100%', aspectRatio: '4/3', objectFit: 'cover', borderRadius: 8, border: '1px solid var(--hairline)' }} />
                    ))}
                  </div>
                </div>
              )}

              {/* Fee input */}
              <div style={{ marginBottom: 16 }}>
                <label className="form-label form-label-required" htmlFor="approved-fee">Phí được duyệt (VND)</label>
                <input id="approved-fee" className="input" type="number" min={0}
                  value={feeInput} onChange={e => setFeeInput(e.target.value)}
                  placeholder="Nhập phí chính thức..." />
                {parsedFee > ESCALATION_THRESHOLD && (
                  <p className="body-sm" style={{ color: '#dc2626', marginTop: 4 }}>
                    ⚠️ Phí &gt; {fmtVnd(ESCALATION_THRESHOLD)} — yêu cầu co-approval từ Admin
                  </p>
                )}
              </div>

              <button
                className="btn-primary"
                style={{ width: '100%' }}
                disabled={approveLoading || !feeInput}
                onClick={() => setConfirmOpen(true)}
              >
                {approveLoading ? 'Đang xử lý...' : '✓ Co-Approve'}
              </button>
            </div>
          )}
        </Drawer>

        <ConfirmModal
          open={confirmOpen}
          title="Xác nhận Co-Approve"
          message={`Bạn xác nhận duyệt phí ${parsedFee > 0 ? fmtVnd(parsedFee) : '?'} cho báo cáo hư hại này?`}
          confirmLabel="Xác nhận Co-Approve"
          onConfirm={handleCoApprove}
          onCancel={() => setConfirmOpen(false)}
        />
      </div>
    </AdminLayout>
  );
}

