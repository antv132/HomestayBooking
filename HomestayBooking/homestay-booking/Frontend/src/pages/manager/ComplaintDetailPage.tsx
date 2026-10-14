// ─── SCR-56: Complaint Detail ─────────────────────────────────────────────────
import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import ManagerLayout from '../../layouts/ManagerLayout';
import { Badge } from './_sharedAdminData';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { complaintsApi, ComplaintStatus } from '../../api/complaintsApi';
import Modal from '../../components/ui/Modal';

export function ComplaintDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ['manager_complaint', id],
    queryFn: () => complaintsApi.getComplaintDetail(id!),
    enabled: !!id
  });

  const [selectedStatus, setSelectedStatus] = useState<ComplaintStatus | ''>('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const c = data?.data;

  // Initialize form when data loads
  useEffect(() => {
    if (c) {
      setResolutionNotes(c.resolutionNotes || '');
    }
  }, [c]);

  const updateMutation = useMutation({
    mutationFn: (payload: { status: ComplaintStatus; resolutionNotes: string }) =>
      complaintsApi.updateComplaintStatus(id!, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['manager_complaint', id] });
      queryClient.invalidateQueries({ queryKey: ['manager_complaints'] });
      setSelectedStatus('');
      setErrorMsg(null);
      alert("Cập nhật trạng thái khiếu nại thành công");
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Cập nhật thất bại";
      setErrorMsg(msg);
    }
  });

  if (isLoading) return <ManagerLayout><div style={{ padding: 40 }}>Loading...</div></ManagerLayout>;
  if (isError || !c) return <ManagerLayout><div style={{ padding: 40, color: 'var(--error)' }}>Error or complaint not found</div></ManagerLayout>;

  const getValidNextStatuses = (status: ComplaintStatus): ComplaintStatus[] => {
    const all: ComplaintStatus[] = ['OPEN', 'INVESTIGATING', 'RESOLVED', 'CLOSED'];
    return all.filter(s => s !== status);
  };

  const nextStatuses = getValidNextStatuses(c.status);

  const handleUpdateClick = () => {
    setErrorMsg(null);
    if (!selectedStatus) {
      setErrorMsg("Vui lòng chọn trạng thái mới");
      return;
    }

    if (selectedStatus === 'RESOLVED' || selectedStatus === 'CLOSED') {
      if (!resolutionNotes.trim()) {
        setErrorMsg("Ghi chú xử lý là bắt buộc khi giải quyết hoặc đóng khiếu nại");
        return;
      }
    }

    if (selectedStatus === 'CLOSED') {
      setShowConfirmModal(true);
    } else {
      executeStatusUpdate();
    }
  };

  const executeStatusUpdate = () => {
    if (!selectedStatus) return;
    setShowConfirmModal(false);
    updateMutation.mutate({
      status: selectedStatus,
      resolutionNotes: resolutionNotes.trim()
    });
  };

  return (
    <ManagerLayout>
      <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
        <Link to="/manager/complaints" className="text-primary" style={{ textDecoration: 'none' }}>Complaints</Link>
        <span>›</span>
        <span style={{ fontWeight: 600 }}>#{c.id.substring(0, 8).toUpperCase()}</span>
      </div>

      <h1 className="heading-md" style={{ marginBottom: 24 }}>{c.subject}</h1>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: 20 }}>
        <div>
          <div className="card" style={{ padding: 24, marginBottom: 16 }}>
            <h2 className="heading-sm" style={{ marginBottom: 16 }}>Complaint Details</h2>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 20 }}>
              <div>
                <p className="body-sm text-charcoal" style={{ margin: 0 }}>Customer</p>
                <p style={{ fontWeight: 600, fontSize: 14, marginTop: 4 }}>
                  {c.customer ? `${c.customer.fullName} (${c.customer.email})` : 'Anonymous Guest'}
                </p>
              </div>
              <div>
                <p className="body-sm text-charcoal" style={{ margin: 0 }}>Status</p>
                <div style={{ marginTop: 4 }}><Badge s={c.status} /></div>
              </div>
              <div>
                <p className="body-sm text-charcoal" style={{ margin: 0 }}>Submitted At</p>
                <p style={{ fontWeight: 600, fontSize: 14, marginTop: 4 }}>
                  {new Date(c.createdAt).toLocaleString('en-US')}
                </p>
              </div>
              <div>
                <p className="body-sm text-charcoal" style={{ margin: 0 }}>Last Updated</p>
                <p style={{ fontWeight: 600, fontSize: 14, marginTop: 4 }}>
                  {new Date(c.updatedAt).toLocaleString('en-US')}
                </p>
              </div>
            </div>

            <div style={{ marginBottom: 20 }}>
              <p className="body-sm text-charcoal" style={{ marginBottom: 6 }}>Description</p>
              <p className="body-md" style={{ padding: '14px 18px', background: 'var(--surface-bone)', borderRadius: 8, lineHeight: 1.6, color: 'var(--ink)' }}>
                {c.description}
              </p>
            </div>

            {c.resolutionNotes && (
              <div>
                <p className="body-sm text-charcoal" style={{ marginBottom: 6 }}>Resolution Notes</p>
                <p className="body-md" style={{ padding: '14px 18px', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, lineHeight: 1.6, color: '#166534' }}>
                  {c.resolutionNotes}
                </p>
                {c.resolvedAt && (
                  <p style={{ fontSize: 11, color: 'var(--ash)', marginTop: 4 }}>
                    Resolved at: {new Date(c.resolvedAt).toLocaleString('en-US')}
                  </p>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Status Update Card */}
        <div>
          <div className="card-lg" style={{ padding: 24 }}>
            <h3 className="heading-sm" style={{ marginBottom: 14 }}>Update Status</h3>
            
            {errorMsg && (
              <div className="alert alert-error" style={{ padding: '8px 12px', fontSize: 13, marginBottom: 12 }}>
                {errorMsg}
              </div>
            )}

            <div style={{ marginBottom: 16 }}>
              <label className="form-label" htmlFor="status-select">Next Status *</label>
              <select
                id="status-select"
                className="select"
                value={selectedStatus}
                onChange={e => {
                  setSelectedStatus(e.target.value as ComplaintStatus);
                  setErrorMsg(null);
                }}
                style={{ width: '100%', height: 42, padding: '0 12px', borderRadius: 8, border: '1.5px solid var(--hairline)', background: '#fff', fontSize: 14 }}
              >
                <option value="">-- Choose Status --</option>
                {nextStatuses.map(s => (
                  <option key={s} value={s}>{s.replace('_', ' ')}</option>
                ))}
              </select>
            </div>

            {(selectedStatus === 'RESOLVED' || selectedStatus === 'CLOSED' || c.status === 'RESOLVED') && (
              <div style={{ marginBottom: 16 }}>
                <label className="form-label" htmlFor="resolution-notes">Resolution Notes *</label>
                <textarea
                  id="resolution-notes"
                  className="textarea"
                  rows={4}
                  value={resolutionNotes}
                  onChange={e => {
                    setResolutionNotes(e.target.value);
                    setErrorMsg(null);
                  }}
                  placeholder="Enter resolution notes here..."
                />
              </div>
            )}

            <button
              className="btn-primary"
              style={{ width: '100%' }}
              onClick={handleUpdateClick}
              disabled={updateMutation.isPending || !selectedStatus}
            >
              {updateMutation.isPending ? 'Updating...' : 'Update Status'}
            </button>
          </div>
        </div>
      </div>

      {/* Confirmation Modal for CLOSED */}
      <Modal
        isOpen={showConfirmModal}
        onClose={() => setShowConfirmModal(false)}
        title="Confirm Close Complaint"
        size="sm"
        actions={[
          {
            label: 'Hủy',
            onClick: () => setShowConfirmModal(false),
            variant: 'secondary',
          },
          {
            label: 'Đóng Khiếu Nại',
            onClick: executeStatusUpdate,
            variant: 'primary',
          },
        ]}
      >
        <p className="body-md text-charcoal" style={{ margin: 0 }}>
          Bạn có chắc chắn muốn đóng khiếu nại này không? Một khi đã đóng, trạng thái sẽ là vĩnh viễn và không thể thay đổi lại.
        </p>
      </Modal>
    </ManagerLayout>
  );
}
