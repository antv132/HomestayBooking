import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../layouts/AdminLayout';
import { DataTable } from '../../components/ui';
import { extractApiError, Spinner, ErrorBanner, Pagination } from './_adminShared';
import { reviewApi, type ModerationReview } from '../../api/reviewApi';
import { activityLogApi, type ActivityLogEntry, type ActivityLogParams } from '../../api/activityLogApi';

function ContentModerationPanel() {
  const [reviews, setReviews] = useState<ModerationReview[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError(null);
    try {
      const res = await reviewApi.getAdminReviews({
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
  }, [statusFilter]);

  useEffect(() => {
    void load(0);
  }, [load]);

  async function toggle(review: ModerationReview) {
    const next = review.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED';
    const ok = window.confirm(
      next === 'HIDDEN'
        ? `Ẩn đánh giá của ${review.customerName}?`
        : `Hiện lại đánh giá của ${review.customerName}?`,
    );
    if (!ok) return;
    setActionLoading(true);
    try {
      await reviewApi.moderateAdminReview(review.id, next);
      await load(page);
    } catch (err) {
      setError(extractApiError(err, 'Cập nhật thất bại.'));
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div>
      <div className="alert alert-info" style={{ marginBottom: 16 }}>
        Kiểm duyệt nội dung reviews toàn hệ thống. Ẩn reviews vi phạm chính sách.
      </div>
      <div style={{ marginBottom: 12 }}>
        <select
          className="input"
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ maxWidth: 200 }}
        >
          <option value="">Tất cả trạng thái</option>
          <option value="PUBLISHED">Published</option>
          <option value="HIDDEN">Hidden</option>
        </select>
      </div>
      {error && <ErrorBanner msg={error} />}
      {loading ? (
        <Spinner />
      ) : (
        <>
          <DataTable
            columns={[
              { header: 'Review ID', accessor: (r: ModerationReview) => <span className="code-sm">{r.id.slice(0, 8)}</span> },
              { header: 'Khách hàng', accessor: (r: ModerationReview) => r.customerName },
              { header: 'Property', accessor: (r: ModerationReview) => r.propertyName },
              { header: 'Rating', accessor: (r: ModerationReview) => `${r.rating}★` },
              {
                header: 'Nội dung',
                accessor: (r: ModerationReview) => (
                  <span style={{ maxWidth: 200, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 13 }}>
                    {r.comment}
                  </span>
                ),
              },
              {
                header: 'Trạng thái',
                accessor: (r: ModerationReview) => (
                  <span className={`badge ${r.status === 'PUBLISHED' ? 'badge-success' : 'badge-warning'}`}>
                    {r.status}
                  </span>
                ),
              },
            ]}
            data={reviews}
            keyExtractor={r => r.id}
            actions={[
              {
                label: 'Toggle Hide/Show',
                onClick: (r: ModerationReview) => {
                  if (!actionLoading) void toggle(r);
                },
              },
            ]}
          />
          <Pagination page={page} totalPages={totalPages} onPage={p => void load(p)} />
        </>
      )}
    </div>
  );
}

function ActivityLogsTab() {
  const [logs, setLogs] = useState<ActivityLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError(null);
    try {
      const params: ActivityLogParams = { page: p, size: 20 };
      if (dateFrom) params.from = new Date(dateFrom).toISOString();
      if (dateTo) params.to = new Date(dateTo + 'T23:59:59').toISOString();
      if (searchTerm.trim()) params.search = searchTerm.trim();
      const res = await activityLogApi.getLogs(params);
      if (res.success) {
        setLogs(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(p);
      }
    } catch (err) {
      setError(extractApiError(err, 'Không tải được activity logs.'));
    } finally {
      setLoading(false);
    }
  }, [dateFrom, dateTo, searchTerm]);

  useEffect(() => {
    void load(0);
  }, [load]);

  return (
    <div>
      <div className="card" style={{ padding: '14px 18px', marginBottom: 16 }}>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <div>
            <label className="form-label" style={{ fontSize: 12 }}>Từ ngày</label>
            <input type="date" className="input" value={dateFrom}
              onChange={e => setDateFrom(e.target.value)} style={{ width: 160 }} />
          </div>
          <div>
            <label className="form-label" style={{ fontSize: 12 }}>Đến ngày</label>
            <input type="date" className="input" value={dateTo}
              onChange={e => setDateTo(e.target.value)} style={{ width: 160 }} />
          </div>
          <div>
            <label className="form-label" style={{ fontSize: 12 }}>Tìm kiếm</label>
            <input type="text" className="input" placeholder="Action hoặc mô tả..."
              value={searchTerm} onChange={e => setSearchTerm(e.target.value)} style={{ width: 200 }} />
          </div>
        </div>
      </div>
      {error && <ErrorBanner msg={error} />}
      {loading ? <Spinner /> : (
        <>
          <DataTable
            columns={[
              { header: 'Thời gian', accessor: (l: ActivityLogEntry) => <span className="code-sm">{l.createdAt ? new Date(l.createdAt).toLocaleString('vi-VN') : '—'}</span> },
              { header: 'User', accessor: (l: ActivityLogEntry) => l.userFullName || '—' },
              { header: 'Action', accessor: (l: ActivityLogEntry) => <span className="badge badge-info">{l.action}</span> },
              { header: 'Entity', accessor: (l: ActivityLogEntry) => <span className="body-sm text-charcoal">{l.entityType ? `${l.entityType}${l.entityId ? '#' + String(l.entityId).slice(0, 8) : ''}` : '—'}</span> },
              { header: 'Mô tả', accessor: (l: ActivityLogEntry) => <span style={{ maxWidth: 240, display: 'inline-block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 13 }}>{l.description || '—'}</span> },
            ]}
            data={logs}
            keyExtractor={l => l.id}
          />
          <Pagination page={page} totalPages={totalPages} onPage={p => void load(p)} />
        </>
      )}
      {!loading && logs.length === 0 && <p className="body-sm text-charcoal">Không có activity log nào.</p>}
    </div>
  );
}

export function SystemAdminPage() {
  const [tab, setTab] = useState<'logs' | 'moderation'>('logs');

  const TABS: { key: typeof tab; label: string }[] = [
    { key: 'logs',       label: 'Activity Logs' },
    { key: 'moderation', label: 'Content Moderation' },
  ];

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ marginBottom: 24 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)', marginBottom: 4 }}>System Administration</h1>
        </div>

        <div style={{ display: 'flex', gap: 0, borderBottom: '2px solid var(--hairline)', marginBottom: 24 }}>
          {TABS.map(t => (
            <button key={t.key} onClick={() => setTab(t.key)} style={{
              padding: '10px 18px', border: 'none', background: 'none', cursor: 'pointer',
              fontFamily: 'Outfit', fontWeight: tab === t.key ? 700 : 500, fontSize: 14,
              color: tab === t.key ? 'var(--primary)' : 'var(--charcoal)',
              borderBottom: `2px solid ${tab === t.key ? 'var(--primary)' : 'transparent'}`,
              marginBottom: -2, transition: 'all 0.15s',
            }}>
              {t.label}
            </button>
          ))}
        </div>

        {tab === 'logs' && <ActivityLogsTab />}
        {tab === 'moderation' && <ContentModerationPanel />}
      </div>
    </AdminLayout>
  );
}
