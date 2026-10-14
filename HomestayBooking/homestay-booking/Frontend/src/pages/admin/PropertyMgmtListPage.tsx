import { useState, useEffect, useCallback, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import {
  getAdminProperties,
  updateAdminProperty,
  getManagers,
  type AdminProperty,
  type AdminUser,
} from '../../api/adminApi';
import { DataTable, StatusBadge as UIStatusBadge } from '../../components/ui';
import { extractApiError, Spinner, ErrorBanner, SuccessBanner, ConfirmModal, Pagination } from './_adminShared';

export function PropertyMgmtListPage() {
  const [items, setItems] = useState<AdminProperty[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [managerId, setManagerId] = useState('');
  const [sort, setSort] = useState('createdAt,desc');
  const [managers, setManagers] = useState<AdminUser[]>([]);
  const [confirmTarget, setConfirmTarget] = useState<AdminProperty | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const navigate = useNavigate();
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = useCallback(async (
    p = 0,
    kw = keyword,
    status = statusFilter,
    mgr = managerId,
    sortValue = sort,
  ) => {
    setLoading(true);
    setError(null);
    try {
      const res = await getAdminProperties({
        page: p,
        size: 10,
        keyword: kw.trim() || undefined,
        status: status || undefined,
        managerId: mgr || undefined,
        sort: sortValue,
      });
      if (res.success) {
        setItems(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(p);
      }
    } catch (err) {
      setError(extractApiError(err, 'Không tải được danh sách properties.'));
    } finally {
      setLoading(false);
    }
  }, [keyword, statusFilter, managerId, sort]);

  useEffect(() => {
    load(0);
  }, [statusFilter, managerId, sort]); // eslint-disable-line react-hooks/exhaustive-deps -- keyword handled by debounce

  useEffect(() => {
    getManagers({ page: 0, size: 100, status: 'ACTIVE' })
      .then(res => { if (res.success) setManagers(res.data.content); })
      .catch(() => { /* optional filter; ignore load errors */ });
  }, []);

  function handleSearch(v: string) {
    setKeyword(v);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => load(0, v, statusFilter, managerId, sort), 400);
  }

  async function handleToggleStatus() {
    if (!confirmTarget) return;
    const nextStatus = confirmTarget.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setActionLoading(true);
    setSuccess(null);
    setError(null);
    try {
      const res = await updateAdminProperty(confirmTarget.id, { status: nextStatus });
      if (res.success) {
        setSuccess(nextStatus === 'ACTIVE' ? 'Đã kích hoạt property.' : 'Đã vô hiệu hóa property.');
        setConfirmTarget(null);
        await load(page);
      }
    } catch (err) {
      setError(extractApiError(err, 'Không cập nhật được trạng thái property.'));
      setConfirmTarget(null);
    } finally {
      setActionLoading(false);
    }
  }

  const columns = [
    { header: 'Tên Property', accessor: (p: AdminProperty) => <span className="font-semibold">{p.name}</span> },
    { header: 'Địa điểm', accessor: (p: AdminProperty) => p.location || '—' },
    { header: 'Manager', accessor: (p: AdminProperty) => p.managerName || <span className="text-charcoal">Chưa gán</span> },
    {
      header: 'Trạng thái',
      accessor: (p: AdminProperty) => (
        <UIStatusBadge
          status={p.status === 'ACTIVE' ? 'Hoạt động' : 'Ngừng hoạt động'}
          variant={p.status === 'ACTIVE' ? 'success' : 'neutral'}
        />
      ),
    },
  ];

  const actions = [
    { label: 'Sửa', onClick: (p: AdminProperty) => navigate(`/admin/properties/${p.id}/edit`) },
    { label: 'Gán Manager', onClick: (p: AdminProperty) => navigate(`/admin/properties/${p.id}/manager`) },
    { label: 'Kích hoạt / Vô hiệu hóa', onClick: (p: AdminProperty) => setConfirmTarget(p) },
  ];

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)' }}>
            Danh sách Property
          </h1>
          <Link to="/admin/properties/create" className="btn-primary">+ Tạo Property</Link>
        </div>

        {error && <ErrorBanner msg={error} />}
        {success && <SuccessBanner msg={success} />}

        <div className="card" style={{ padding: '16px 20px', marginBottom: 16, display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
          <input
            id="property-search"
            className="input"
            placeholder="Tìm theo tên hoặc địa chỉ..."
            value={keyword}
            onChange={e => handleSearch(e.target.value)}
            style={{ maxWidth: 280, flex: '1 1 200px' }}
          />
          <select
            id="property-status-filter"
            className="input"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            style={{ maxWidth: 180 }}
            aria-label="Lọc theo trạng thái"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
          <select
            id="property-manager-filter"
            className="input"
            value={managerId}
            onChange={e => setManagerId(e.target.value)}
            style={{ maxWidth: 220 }}
            aria-label="Lọc theo Manager"
          >
            <option value="">Tất cả Manager</option>
            {managers.map(m => (
              <option key={m.id} value={m.id}>{m.fullName}</option>
            ))}
          </select>
          <select
            id="property-sort"
            className="input"
            value={sort}
            onChange={e => setSort(e.target.value)}
            style={{ maxWidth: 200 }}
            aria-label="Sắp xếp danh sách"
          >
            <option value="createdAt,desc">Mới nhất</option>
            <option value="createdAt,asc">Cũ nhất</option>
            <option value="name,asc">Tên A–Z</option>
            <option value="name,desc">Tên Z–A</option>
          </select>
        </div>

        {loading ? <Spinner /> : (
          <>
            <DataTable
              columns={columns}
              data={items}
              keyExtractor={(p) => p.id}
              actions={actions}
            />
            <Pagination
              page={page}
              totalPages={totalPages}
              onPage={p => load(p)}
            />
          </>
        )}
      </div>

      <ConfirmModal
        open={!!confirmTarget}
        title={confirmTarget?.status === 'ACTIVE' ? 'Vô hiệu hóa Property' : 'Kích hoạt Property'}
        message={
          confirmTarget
            ? `Bạn chắc chắn muốn ${confirmTarget.status === 'ACTIVE' ? 'vô hiệu hóa' : 'kích hoạt'} "${confirmTarget.name}"?`
            : ''
        }
        confirmLabel={confirmTarget?.status === 'ACTIVE' ? 'Vô hiệu hóa' : 'Kích hoạt'}
        danger={confirmTarget?.status === 'ACTIVE'}
        onConfirm={actionLoading ? () => undefined : handleToggleStatus}
        onCancel={() => !actionLoading && setConfirmTarget(null)}
      />
    </AdminLayout>
  );
}
