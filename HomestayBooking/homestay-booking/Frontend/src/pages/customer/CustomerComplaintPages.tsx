// ─── CustomerComplaintPages.tsx — SCR-67, SCR-68 ─────────────────────────────
// Exports: CustomerComplaintListPage (SCR-67), CreateComplaintPage (SCR-68)

import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { complaintsApi, type ComplaintStatus } from '../../api/complaintsApi';

const STATUS_META: Record<string, { cls: string; label: string }> = {
  OPEN: { cls: 'badge-warning', label: 'Chờ xử lý' },
  INVESTIGATING: { cls: 'badge-info', label: 'Đang điều tra' },
  RESOLVED: { cls: 'badge-success', label: 'Đã giải quyết' },
  CLOSED: { cls: 'badge-error', label: 'Đã đóng' },
};

function ComplaintStatusBadge({ status }: { status: string }) {
  const s = STATUS_META[status] ?? { cls: 'badge-neutral', label: status };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${day}/${month}/${d.getFullYear()} ${hours}:${minutes}`;
}

const FILTERS: { key: string; label: string }[] = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'OPEN', label: 'Chờ xử lý' },
  { key: 'INVESTIGATING', label: 'Đang điều tra' },
  { key: 'RESOLVED', label: 'Đã giải quyết' },
  { key: 'CLOSED', label: 'Đã đóng' },
];

// ── SCR-67: My Complaint List ────────────────────────────────────────────────
export function CustomerComplaintListPage() {
  const [filter, setFilter] = useState<string>('ALL');

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['customer_complaints'],
    queryFn: async () => {
      const res = await complaintsApi.getMyComplaints({ page: 0, size: 50 });
      return res;
    },
  });

  const all = useMemo(() => data?.data?.content ?? [], [data]);
  const list = useMemo(
    () => (filter === 'ALL' ? all : all.filter((c) => c.status === filter)),
    [all, filter],
  );

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 780, margin: '0 auto' }}>
        <div className="flex items-center justify-between" style={{ marginBottom: 20, gap: 16, flexWrap: 'wrap' }}>
          <div>
            <h1 className="heading-md" style={{ marginBottom: 4 }}>Khiếu nại của tôi</h1>
            <p className="body-sm text-charcoal">Gửi và theo dõi tiến độ xử lý khiếu nại của bạn.</p>
          </div>
          <Link to="/customer/complaints/create" className="btn-primary">Gửi khiếu nại</Link>
        </div>

        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
          {FILTERS.map((f) => (
            <button
              key={f.key}
              type="button"
              className={filter === f.key ? 'btn-primary btn-sm' : 'btn-outline btn-sm'}
              onClick={() => setFilter(f.key)}
            >
              {f.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="card" style={{ textAlign: 'center', padding: 60 }}>
            <p className="body-md text-charcoal">Đang tải khiếu nại...</p>
          </div>
        ) : isError ? (
          <div>
            <Alert variant="error" message="Không tải được danh sách khiếu nại. Vui lòng thử lại." />
            <button type="button" className="btn-outline btn-sm" style={{ marginTop: 12 }} onClick={() => refetch()}>
              Thử lại
            </button>
          </div>
        ) : list.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: 60 }}>
            <div style={{ fontSize: 40, marginBottom: 12 }}>📋</div>
            <h3 className="heading-sm" style={{ marginBottom: 8 }}>
              {filter === 'ALL' ? 'Chưa có khiếu nại nào' : 'Không có khiếu nại ở trạng thái này'}
            </h3>
            <p className="body-md text-charcoal" style={{ marginBottom: 20 }}>
              Nếu gặp bất kỳ vấn đề nào trong quá trình lưu trú, hãy gửi khiếu nại cho chúng tôi.
            </p>
            <Link to="/customer/complaints/create" className="btn-primary">Gửi khiếu nại ngay</Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {list.map((c) => (
              <div key={c.id} className="card" style={{ padding: 24 }}>
                <div className="flex items-start justify-between" style={{ marginBottom: 12, gap: 12 }}>
                  <div style={{ minWidth: 0 }}>
                    <h3 className="heading-sm" style={{ margin: '0 0 4px 0' }}>{c.subject}</h3>
                    <p style={{ fontSize: 11, color: 'var(--ash)' }}>Gửi lúc {formatDateTime(c.createdAt)}</p>
                  </div>
                  <ComplaintStatusBadge status={c.status} />
                </div>

                <p
                  className="body-md text-body"
                  style={{ background: 'var(--surface-bone)', padding: '12px 16px', borderRadius: 8, margin: '0 0 16px 0', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}
                >
                  {c.description}
                </p>

                {c.resolutionNotes && (
                  <div style={{ borderTop: '1px solid var(--hairline)', paddingTop: 16 }}>
                    <p style={{ fontWeight: 700, fontSize: 13, color: '#166534', margin: '0 0 6px 0' }}>
                      Phản hồi xử lý{c.resolvedAt ? ` · ${formatDateTime(c.resolvedAt)}` : ''}:
                    </p>
                    <p
                      className="body-sm"
                      style={{ background: '#f0fdf4', padding: '10px 14px', borderRadius: 8, color: '#166534', margin: 0, lineHeight: 1.5, whiteSpace: 'pre-wrap' }}
                    >
                      {c.resolutionNotes}
                    </p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </CustomerLayout>
  );
}

// ── SCR-68: Create Complaint ─────────────────────────────────────────────────
export function CreateComplaintPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState<{ subject?: string; description?: string }>({});
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (payload: { subject: string; description: string }) => complaintsApi.submitComplaint(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customer_complaints'] });
      queryClient.invalidateQueries({ queryKey: ['manager_complaints'] });
      navigate('/customer/complaints');
    },
    onError: (err: unknown) => {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setErrorMsg(message || 'Gửi khiếu nại thất bại. Vui lòng thử lại.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setErrorMsg(null);

    const next: { subject?: string; description?: string } = {};
    if (!subject.trim()) next.subject = 'Vui lòng nhập tiêu đề';
    else if (subject.trim().length < 5) next.subject = 'Tiêu đề ít nhất 5 ký tự';
    else if (subject.trim().length > 200) next.subject = 'Tiêu đề tối đa 200 ký tự';
    if (!description.trim()) next.description = 'Vui lòng mô tả chi tiết vấn đề';
    else if (description.trim().length < 20) next.description = 'Mô tả ít nhất 20 ký tự';
    else if (description.trim().length > 2000) next.description = 'Mô tả tối đa 2000 ký tự';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    mutation.mutate({ subject: subject.trim(), description: description.trim() });
  }

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 620, margin: '0 auto' }}>
        <nav className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/customer/complaints" className="text-primary" style={{ textDecoration: 'none' }}>Khiếu nại</Link>
          <span aria-hidden="true">›</span>
          <span style={{ fontWeight: 600 }}>Gửi khiếu nại</span>
        </nav>

        <h1 className="heading-md" style={{ marginBottom: 24 }}>Gửi khiếu nại</h1>

        {errorMsg && (
          <div style={{ marginBottom: 20 }}>
            <Alert variant="error" message={errorMsg} closeable onClose={() => setErrorMsg(null)} />
          </div>
        )}

        <form onSubmit={handleSubmit} className="card" style={{ padding: 28 }}>
          <div style={{ marginBottom: 20 }}>
            <label className="form-label form-label-required" htmlFor="complaint-subject">Tiêu đề</label>
            <input
              id="complaint-subject"
              className={`input ${errors.subject ? 'input-error' : ''}`}
              type="text"
              placeholder="VD: Phòng bẩn, hàng xóm ồn ào, thái độ nhân viên..."
              value={subject}
              onChange={(e) => { setSubject(e.target.value); if (errors.subject) setErrors((p) => ({ ...p, subject: undefined })); }}
              maxLength={255}
            />
            {errors.subject && <p className="form-error">{errors.subject}</p>}
          </div>

          <div style={{ marginBottom: 24 }}>
            <label className="form-label form-label-required" htmlFor="complaint-description">Mô tả chi tiết</label>
            <textarea
              id="complaint-description"
              className={`textarea ${errors.description ? 'input-error' : ''}`}
              rows={6}
              placeholder="Vui lòng mô tả rõ vấn đề để chúng tôi hỗ trợ bạn tốt hơn..."
              value={description}
              onChange={(e) => { setDescription(e.target.value); if (errors.description) setErrors((p) => ({ ...p, description: undefined })); }}
            />
            {errors.description && <p className="form-error">{errors.description}</p>}
          </div>

          <div style={{ display: 'flex', gap: 12 }}>
            <button type="submit" className="btn-primary" disabled={mutation.isPending}>
              {mutation.isPending ? 'Đang gửi...' : 'Gửi khiếu nại'}
            </button>
            <Link to="/customer/complaints" className="btn-ghost">Hủy</Link>
          </div>
        </form>
      </div>
    </CustomerLayout>
  );
}

export type { ComplaintStatus };
