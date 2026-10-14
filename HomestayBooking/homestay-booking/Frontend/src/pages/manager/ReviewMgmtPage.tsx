// ─── SCR-65: Review Management (FR-14) ───────────────────────────────────────
import { useCallback, useEffect, useState } from 'react';
import ManagerLayout from '../../layouts/ManagerLayout';
import { StarDisplay } from './_sharedAdminData';
import DataTable from '../../components/ui/DataTable';
import { propertyApi, type PropertySummary } from '../../api/propertyApi';
import { reviewApi, type ModerationReview } from '../../api/reviewApi';
import { extractApiError } from '../admin/_adminShared';

export function ReviewMgmtPage() {
  const [properties, setProperties] = useState<PropertySummary[]>([]);
  const [propertyId, setPropertyId] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [reviews, setReviews] = useState<ModerationReview[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    void (async () => {
      try {
        const res = await propertyApi.getAll({ page: 0, size: 50 });
        if (res.success) {
          setProperties(res.data.content);
          if (res.data.content.length > 0) {
            setPropertyId(res.data.content[0].id);
          }
        }
      } catch (err) {
        setError(extractApiError(err, 'Không tải được danh sách property.'));
      }
    })();
  }, []);

  const loadReviews = useCallback(async (p = 0) => {
    if (!propertyId) {
      setReviews([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await reviewApi.getManagerReviews({
        propertyId,
        status: statusFilter || undefined,
        page: p,
        size: 20,
      });
      if (res.success) {
        setReviews(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(p);
      }
    } catch (err) {
      setError(extractApiError(err, 'Không tải được danh sách đánh giá.'));
    } finally {
      setLoading(false);
    }
  }, [propertyId, statusFilter]);

  useEffect(() => {
    void loadReviews(0);
  }, [loadReviews]);

  async function toggleVisibility(review: ModerationReview) {
    const next = review.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED';
    const ok = window.confirm(
      next === 'HIDDEN'
        ? `Ẩn đánh giá của ${review.customerName}? Đánh giá sẽ không còn công khai.`
        : `Hiện lại đánh giá của ${review.customerName}?`,
    );
    if (!ok) return;
    setActionLoading(true);
    setError(null);
    try {
      await reviewApi.moderateManagerReview(review.id, next);
      await loadReviews(page);
    } catch (err) {
      setError(extractApiError(err, 'Cập nhật trạng thái thất bại.'));
    } finally {
      setActionLoading(false);
    }
  }

  const columns = [
    {
      header: 'Customer',
      accessor: (r: ModerationReview) => <span style={{ fontWeight: 600 }}>{r.customerName}</span>,
    },
    {
      header: 'Room',
      accessor: (r: ModerationReview) => <span className="text-charcoal">{r.roomNumber}</span>,
    },
    {
      header: 'Rating',
      accessor: (r: ModerationReview) => <StarDisplay rating={r.rating} />,
    },
    {
      header: 'Comment',
      accessor: (r: ModerationReview) => (
        <span
          className="text-charcoal"
          style={{
            display: 'inline-block',
            maxWidth: 220,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {r.comment}
        </span>
      ),
    },
    {
      header: 'Date',
      accessor: (r: ModerationReview) => (
        <span className="text-charcoal">{new Date(r.createdAt).toLocaleDateString('vi-VN')}</span>
      ),
    },
    {
      header: 'Status',
      accessor: (r: ModerationReview) => (
        <span className={`badge ${r.status === 'PUBLISHED' ? 'badge-success' : 'badge-neutral'}`}>
          {r.status === 'PUBLISHED' ? 'Published' : 'Hidden'}
        </span>
      ),
    },
    {
      header: 'Actions',
      accessor: (r: ModerationReview) => (
        <button
          className="btn-ghost btn-sm"
          disabled={actionLoading}
          onClick={() => void toggleVisibility(r)}
          style={{ color: r.status === 'PUBLISHED' ? 'var(--error)' : 'var(--success)' }}
        >
          {r.status === 'PUBLISHED' ? 'Hide' : 'Show'}
        </button>
      ),
    },
  ];

  return (
    <ManagerLayout>
      <h1 className="heading-md" style={{ marginBottom: 24 }}>Review Management</h1>

      <div
        className="card"
        style={{
          padding: '14px 18px',
          marginBottom: 16,
          display: 'flex',
          flexWrap: 'wrap',
          gap: 12,
          alignItems: 'center',
        }}
      >
        <select
          className="input"
          value={propertyId}
          onChange={e => setPropertyId(e.target.value)}
          style={{ maxWidth: 280 }}
          aria-label="Property"
        >
          {properties.length === 0 && <option value="">Không có property</option>}
          {properties.map(p => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
        <select
          className="input"
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ maxWidth: 180 }}
          aria-label="Status filter"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="PUBLISHED">Published</option>
          <option value="HIDDEN">Hidden</option>
        </select>
      </div>

      {error && (
        <div className="alert alert-error" style={{ marginBottom: 16 }}>{error}</div>
      )}

      {loading ? (
        <p className="body-sm text-charcoal">Đang tải...</p>
      ) : (
        <>
          <DataTable columns={columns} data={reviews} keyExtractor={r => r.id} />
          {totalPages > 1 && (
            <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
              <button className="btn-ghost btn-sm" disabled={page === 0} onClick={() => void loadReviews(page - 1)}>
                ‹ Trước
              </button>
              <span className="body-sm">Trang {page + 1} / {totalPages}</span>
              <button
                className="btn-ghost btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => void loadReviews(page + 1)}
              >
                Sau ›
              </button>
            </div>
          )}
        </>
      )}
    </ManagerLayout>
  );
}
