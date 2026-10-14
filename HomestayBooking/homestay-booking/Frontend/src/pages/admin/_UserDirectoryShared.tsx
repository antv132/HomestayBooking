import { useState, useEffect, useCallback, useRef } from 'react';
import AdminLayout from '../../layouts/AdminLayout';
import {
  getManagers, getCustomers, updateAdminUser, updateCustomerStatus, createManager, updateManager,
  getCustomerBookings,
  type AdminUser,
  type AdminCustomerBooking,
} from '../../api/adminApi';
import { DataTable, StatusBadge as UIStatusBadge, Modal } from '../../components/ui';
import { fmtDate, fmtVnd, extractApiError, Spinner, ErrorBanner, SuccessBanner, StatusBadge, Drawer, ConfirmModal, Pagination } from './_adminShared';

const EMPTY_MANAGER_FORM = { fullName: '', email: '', phone: '', password: '' };

function CreateManagerModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  onCreated: (msg: string) => void;
}) {
  const [form, setForm] = useState(EMPTY_MANAGER_FORM);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setForm(EMPTY_MANAGER_FORM);
      setFieldErrors({});
      setSubmitError(null);
      setSubmitting(false);
    }
  }, [open]);

  function set(field: keyof typeof EMPTY_MANAGER_FORM, value: string) {
    setForm(f => ({ ...f, [field]: value }));
    setFieldErrors(e => ({ ...e, [field]: '' }));
  }

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!form.fullName.trim()) errs.fullName = 'Vui lòng nhập họ tên.';
    if (!form.email.trim()) errs.email = 'Vui lòng nhập email.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errs.email = 'Email không hợp lệ.';
    if (!form.password) errs.password = 'Vui lòng nhập mật khẩu.';
    else if (form.password.length < 8) errs.password = 'Mật khẩu phải có ít nhất 8 ký tự.';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit() {
    if (submitting || !validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      await createManager({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        password: form.password,
      });
      onCreated(`Đã tạo tài khoản Manager cho ${form.email.trim()}.`);
      onClose();
    } catch (err) {
      setSubmitError(extractApiError(err, 'Tạo tài khoản thất bại.'));
    } finally {
      setSubmitting(false);
    }
  }

  const fields: {
    key: keyof typeof EMPTY_MANAGER_FORM;
    label: string;
    type: string;
    placeholder: string;
    required?: boolean;
  }[] = [
    { key: 'fullName', label: 'Họ tên', type: 'text', placeholder: 'Nguyễn Văn A', required: true },
    { key: 'email', label: 'Email', type: 'email', placeholder: 'manager@example.com', required: true },
    { key: 'phone', label: 'Số điện thoại', type: 'tel', placeholder: '0901234567' },
    { key: 'password', label: 'Mật khẩu', type: 'password', placeholder: 'Tối thiểu 8 ký tự', required: true },
  ];

  return (
    <Modal isOpen={open} onClose={onClose} title="Tạo Manager" size="sm">
      {submitError && <ErrorBanner msg={submitError} />}
      <form
        onSubmit={e => { e.preventDefault(); void handleSubmit(); }}
        style={{ display: 'grid', gap: 14 }}
      >
        {fields.map(f => (
          <div key={f.key}>
            <label
              htmlFor={`create-manager-${f.key}`}
              style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 6 }}
            >
              {f.label}
              {f.required && <span style={{ color: 'var(--danger, #d93025)' }}> *</span>}
            </label>
            <input
              id={`create-manager-${f.key}`}
              className="input"
              type={f.type}
              placeholder={f.placeholder}
              value={form[f.key]}
              onChange={e => set(f.key, e.target.value)}
              autoComplete={f.key === 'password' ? 'new-password' : 'off'}
              style={{ width: '100%' }}
            />
            {fieldErrors[f.key] && (
              <p style={{ color: 'var(--danger, #d93025)', fontSize: 12, marginTop: 4 }}>
                {fieldErrors[f.key]}
              </p>
            )}
          </div>
        ))}
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 6 }}>
          <button type="button" className="btn-secondary" onClick={onClose} disabled={submitting}>
            Hủy
          </button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Đang tạo...' : 'Tạo Manager'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function EditManagerModal({
  open,
  user,
  onClose,
  onUpdated,
}: {
  open: boolean;
  user: AdminUser | null;
  onClose: () => void;
  onUpdated: (msg: string, updated: AdminUser) => void;
}) {
  const [form, setForm] = useState({ ...EMPTY_MANAGER_FORM, status: 'ACTIVE' });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open && user) {
      setForm({
        fullName: user.fullName || '',
        email: user.email || '',
        phone: user.phone || '',
        password: '',
        status: user.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE',
      });
      setFieldErrors({});
      setSubmitError(null);
      setSubmitting(false);
    }
  }, [open, user]);

  function set(field: keyof typeof form, value: string) {
    setForm(f => ({ ...f, [field]: value }));
    setFieldErrors(e => ({ ...e, [field]: '' }));
  }

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!form.fullName.trim()) errs.fullName = 'Vui lòng nhập họ tên.';
    if (!form.email.trim()) errs.email = 'Vui lòng nhập email.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errs.email = 'Email không hợp lệ.';
    if (form.password && form.password.length < 8) errs.password = 'Mật khẩu phải có ít nhất 8 ký tự.';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit() {
    if (!user || submitting || !validate()) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      const res = await updateManager(user.id, {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        password: form.password || undefined,
        status: form.status,
      });
      onUpdated(`Đã cập nhật tài khoản Manager ${form.email.trim()}.`, res.data);
      onClose();
    } catch (err) {
      setSubmitError(extractApiError(err, 'Cập nhật tài khoản thất bại.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal isOpen={open} onClose={onClose} title="Sửa Manager" size="sm">
      {submitError && <ErrorBanner msg={submitError} />}
      <form
        onSubmit={e => { e.preventDefault(); void handleSubmit(); }}
        style={{ display: 'grid', gap: 14 }}
      >
        {([
          { key: 'fullName' as const, label: 'Họ tên', type: 'text', placeholder: 'Nguyễn Văn A', required: true },
          { key: 'email' as const, label: 'Email', type: 'email', placeholder: 'manager@example.com', required: true },
          { key: 'phone' as const, label: 'Số điện thoại', type: 'tel', placeholder: '0901234567' },
          {
            key: 'password' as const,
            label: 'Mật khẩu mới',
            type: 'password',
            placeholder: 'Để trống nếu không đổi',
          },
        ]).map(f => (
          <div key={f.key}>
            <label
              htmlFor={`edit-manager-${f.key}`}
              style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 6 }}
            >
              {f.label}
              {'required' in f && f.required && (
                <span style={{ color: 'var(--danger, #d93025)' }}> *</span>
              )}
            </label>
            <input
              id={`edit-manager-${f.key}`}
              className="input"
              type={f.type}
              placeholder={f.placeholder}
              value={form[f.key]}
              onChange={e => set(f.key, e.target.value)}
              autoComplete={f.key === 'password' ? 'new-password' : 'off'}
              style={{ width: '100%' }}
            />
            {fieldErrors[f.key] && (
              <p style={{ color: 'var(--danger, #d93025)', fontSize: 12, marginTop: 4 }}>
                {fieldErrors[f.key]}
              </p>
            )}
          </div>
        ))}
        <div>
          <label
            htmlFor="edit-manager-status"
            style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 6 }}
          >
            Trạng thái
          </label>
          <select
            id="edit-manager-status"
            className="input"
            value={form.status}
            onChange={e => set('status', e.target.value)}
            style={{ width: '100%' }}
          >
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 6 }}>
          <button type="button" className="btn-secondary" onClick={onClose} disabled={submitting}>
            Hủy
          </button>
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function UserAvatar({ name, url, size = 36 }: { name: string; url?: string | null; size?: number }) {
  const initial = (name || '?').charAt(0).toUpperCase();
  if (url) {
    return (
      <img
        src={url}
        alt=""
        style={{
          width: size,
          height: size,
          borderRadius: '50%',
          objectFit: 'cover',
          border: '1px solid var(--hairline)',
          flexShrink: 0,
        }}
      />
    );
  }
  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: 'rgba(15,118,110,0.12)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: size * 0.4,
        fontWeight: 700,
        color: 'var(--primary)',
        flexShrink: 0,
      }}
    >
      {initial}
    </div>
  );
}

function statusVariant(status: string): 'success' | 'neutral' | 'danger' {
  if (status === 'ACTIVE') return 'success';
  if (status === 'SUSPENDED') return 'danger';
  return 'neutral';
}

function UserDirectoryPage({ role, title }: { role: 'MANAGER' | 'CUSTOMER'; title: string; scr: string }) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pageSuccess, setPageSuccess] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMsg, setActionMsg] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [confirmUser, setConfirmUser] = useState<AdminUser | null>(null);
  const [bookings, setBookings] = useState<AdminCustomerBooking[]>([]);
  const [bookingsLoading, setBookingsLoading] = useState(false);
  const [bookingsError, setBookingsError] = useState<string | null>(null);

  const fetchUsers = useCallback(async (p = 0, kw = keyword, status = statusFilter) => {
    setLoading(true);
    setError(null);
    try {
      const fn = role === 'MANAGER' ? getManagers : getCustomers;
      const res = await fn({
        page: p,
        size: 10,
        keyword: kw.trim() || undefined,
        status: status || undefined,
      });
      if (res.success) {
        setUsers(res.data.content);
        setTotalPages(res.data.totalPages);
        setPage(p);
      }
    } catch (err) {
      setError(extractApiError(err, 'Không tải được danh sách.'));
    } finally {
      setLoading(false);
    }
  }, [role, keyword, statusFilter]);

  useEffect(() => {
    fetchUsers(0, keyword, statusFilter);
  }, [statusFilter]); // eslint-disable-line react-hooks/exhaustive-deps -- keyword debounced

  useEffect(() => {
    fetchUsers(0, '', '');
  }, [role]); // eslint-disable-line react-hooks/exhaustive-deps

  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  function handleSearch(v: string) {
    setKeyword(v);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => fetchUsers(0, v, statusFilter), 400);
  }

  async function applyStatusChange(user: AdminUser, newStatus: string) {
    setActionLoading(true);
    setActionMsg(null);
    setPageSuccess(null);
    setError(null);
    try {
      if (role === 'CUSTOMER') {
        await updateCustomerStatus(user.id, newStatus as 'ACTIVE' | 'SUSPENDED');
      } else {
        await updateAdminUser(user.id, { status: newStatus });
      }
      const msg =
        role === 'CUSTOMER'
          ? `Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'đình chỉ'} tài khoản Customer.`
          : `Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'vô hiệu hóa'} tài khoản.`;
      setActionMsg({ type: 'success', msg });
      if (!drawerOpen) setPageSuccess(msg);
      setSelectedUser(u => (u && u.id === user.id ? { ...u, status: newStatus } : u));
      setUsers(list => list.map(u => (u.id === user.id ? { ...u, status: newStatus } : u)));
    } catch (err) {
      const msg = extractApiError(err, 'Thao tác thất bại.');
      setActionMsg({ type: 'error', msg });
      if (!drawerOpen) setError(msg);
    } finally {
      setActionLoading(false);
      setConfirmUser(null);
    }
  }

  function requestToggleStatus(user: AdminUser) {
    if (role === 'CUSTOMER' && user.status === 'INACTIVE') return;
    setConfirmUser(user);
  }

  function nextStatusFor(user: AdminUser): string {
    if (role === 'CUSTOMER') {
      return user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    }
    return user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  }

  function statusLabel(status: string): string {
    if (status === 'ACTIVE') return 'Hoạt động';
    if (status === 'INACTIVE') return 'Ngừng hoạt động';
    if (status === 'SUSPENDED') return 'Đình chỉ';
    return status;
  }

  async function loadCustomerBookings(userId: string) {
    setBookingsLoading(true);
    setBookingsError(null);
    try {
      const res = await getCustomerBookings(userId, { page: 0, size: 20 });
      if (res.success) setBookings(res.data.content);
      else setBookings([]);
    } catch (err) {
      setBookings([]);
      setBookingsError(extractApiError(err, 'Không tải được lịch sử đặt phòng.'));
    } finally {
      setBookingsLoading(false);
    }
  }

  function openDetail(u: AdminUser) {
    setSelectedUser(u);
    setDrawerOpen(true);
    setActionMsg(null);
    setBookings([]);
    setBookingsError(null);
    if (role === 'CUSTOMER') {
      void loadCustomerBookings(u.id);
    }
  }

  function openEdit(u: AdminUser) {
    setEditingUser(u);
    setEditOpen(true);
  }

  const managerColumns = [
    {
      header: '',
      accessor: (u: AdminUser) => <UserAvatar name={u.fullName} url={u.avatarUrl} size={36} />,
      className: 'w-14',
    },
    {
      header: 'Họ tên',
      accessor: (u: AdminUser) => <span className="font-semibold">{u.fullName}</span>,
    },
    { header: 'Email', accessor: (u: AdminUser) => u.email },
    {
      header: 'Properties Assigned',
      accessor: (u: AdminUser) => (
        <span style={{ fontWeight: 600 }}>{u.propertiesAssigned ?? 0}</span>
      ),
    },
    {
      header: 'Trạng thái',
      accessor: (u: AdminUser) => (
        <UIStatusBadge
          status={u.status === 'ACTIVE' ? 'Hoạt động' : u.status === 'INACTIVE' ? 'Ngừng hoạt động' : u.status}
          variant={statusVariant(u.status)}
        />
      ),
    },
  ];

  const customerColumns = [
    {
      header: '',
      accessor: (u: AdminUser) => <UserAvatar name={u.fullName} url={u.avatarUrl} size={36} />,
      className: 'w-14',
    },
    {
      header: 'Họ tên',
      accessor: (u: AdminUser) => <span className="font-semibold">{u.fullName}</span>,
    },
    { header: 'Email', accessor: (u: AdminUser) => u.email },
    {
      header: 'Total Bookings',
      accessor: (u: AdminUser) => <span style={{ fontWeight: 600 }}>{u.totalBookings ?? 0}</span>,
    },
    {
      header: 'Total Spend',
      accessor: (u: AdminUser) => (
        <span style={{ fontWeight: 600 }}>
          {u.totalSpend != null ? fmtVnd(Number(u.totalSpend)) : '—'}
        </span>
      ),
    },
    {
      header: 'Trạng thái',
      accessor: (u: AdminUser) => (
        <UIStatusBadge
          status={statusLabel(u.status)}
          variant={statusVariant(u.status)}
        />
      ),
    },
  ];

  const columns = role === 'MANAGER' ? managerColumns : customerColumns;

  const actions = [
    { label: 'Xem chi tiết', onClick: (u: AdminUser) => openDetail(u) },
    ...(role === 'MANAGER'
      ? [
          { label: 'Sửa', onClick: (u: AdminUser) => openEdit(u) },
          {
            label: 'Kích hoạt / Vô hiệu hóa',
            onClick: (u: AdminUser) => { requestToggleStatus(u); },
          },
        ]
      : [
          {
            label: 'Suspend / Activate',
            onClick: (u: AdminUser) => {
              if (u.status === 'INACTIVE') return;
              requestToggleStatus(u);
            },
          },
        ]),
  ];

  return (
    <AdminLayout>
      <div className="animate-fade-in">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
          <h1 style={{ fontFamily: 'Outfit', fontSize: 26, fontWeight: 700, color: 'var(--ink)' }}>
            {title}
          </h1>
          {role === 'MANAGER' && (
            <button className="btn-primary" onClick={() => setCreateOpen(true)}>
              + Tạo Manager
            </button>
          )}
        </div>

        {error && <ErrorBanner msg={error} />}
        {pageSuccess && <SuccessBanner msg={pageSuccess} />}

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
          <input
            id={`${role}-search`}
            className="input"
            placeholder="Tìm theo tên hoặc email..."
            value={keyword}
            onChange={e => handleSearch(e.target.value)}
            style={{ maxWidth: 320, flex: '1 1 200px' }}
          />
          <select
            id={`${role}-status-filter`}
            className="input"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            style={{ maxWidth: 180 }}
            aria-label="Lọc theo trạng thái"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
            {role === 'CUSTOMER' && <option value="SUSPENDED">Đình chỉ</option>}
          </select>
        </div>

        {loading ? (
          <Spinner />
        ) : (
          <>
            <DataTable
              columns={columns}
              data={users}
              keyExtractor={u => u.id}
              actions={actions}
              onRowClick={openDetail}
            />
            <Pagination
              page={page}
              totalPages={totalPages}
              onPage={p => fetchUsers(p, keyword, statusFilter)}
            />
          </>
        )}

        <Drawer
          open={drawerOpen}
          onClose={() => {
            setDrawerOpen(false);
            setSelectedUser(null);
            setActionMsg(null);
            setBookings([]);
            setBookingsError(null);
          }}
          title="Chi tiết tài khoản"
        >
          {selectedUser && (
            <div>
              {actionMsg &&
                (actionMsg.type === 'success' ? (
                  <SuccessBanner msg={actionMsg.msg} />
                ) : (
                  <ErrorBanner msg={actionMsg.msg} />
                ))}
              <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 24 }}>
                <UserAvatar name={selectedUser.fullName} url={selectedUser.avatarUrl} size={56} />
                <div>
                  <p style={{ fontWeight: 700, fontSize: 16 }}>{selectedUser.fullName}</p>
                  <p className="body-sm text-charcoal">{selectedUser.email}</p>
                  <StatusBadge status={selectedUser.status} />
                </div>
              </div>
              <div style={{ display: 'grid', gap: 12, marginBottom: 24 }}>
                {[
                  { label: 'ID', value: selectedUser.id.slice(0, 8).toUpperCase() },
                  { label: 'Vai trò', value: selectedUser.role },
                  { label: 'Điện thoại', value: selectedUser.phone || '—' },
                  ...(role === 'MANAGER'
                    ? [{ label: 'Properties Assigned', value: String(selectedUser.propertiesAssigned ?? 0) }]
                    : [
                        { label: 'Total Bookings', value: String(selectedUser.totalBookings ?? 0) },
                        {
                          label: 'Total Spend',
                          value:
                            selectedUser.totalSpend != null
                              ? fmtVnd(Number(selectedUser.totalSpend))
                              : '—',
                        },
                      ]),
                  { label: 'Ngày tạo', value: fmtDate(selectedUser.createdAt) },
                  { label: 'Cập nhật', value: fmtDate(selectedUser.updatedAt) },
                ].map(r => (
                  <div
                    key={r.label}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      padding: '8px 0',
                      borderBottom: '1px solid var(--hairline)',
                    }}
                  >
                    <span className="body-sm text-charcoal">{r.label}</span>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{r.value}</span>
                  </div>
                ))}
              </div>

              {role === 'CUSTOMER' && (
                <div style={{ marginBottom: 24 }}>
                  <h3 style={{ fontFamily: 'Outfit', fontWeight: 700, fontSize: 15, marginBottom: 12 }}>
                    Lịch sử đặt phòng
                  </h3>
                  {bookingsError && <ErrorBanner msg={bookingsError} />}
                  {bookingsLoading ? (
                    <Spinner />
                  ) : bookings.length === 0 ? (
                    <p className="body-sm text-charcoal">Chưa có booking nào.</p>
                  ) : (
                    <div style={{ display: 'grid', gap: 10 }}>
                      {bookings.map(b => (
                        <div
                          key={b.id}
                          style={{
                            padding: '10px 12px',
                            border: '1px solid var(--hairline)',
                            borderRadius: 8,
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                            <span style={{ fontWeight: 600, fontSize: 13 }}>
                              {b.id.slice(0, 8).toUpperCase()}
                            </span>
                            <StatusBadge status={b.status} />
                          </div>
                          <p className="body-sm text-charcoal" style={{ marginTop: 4 }}>
                            {b.propertyName} · Phòng {b.roomNumber}
                          </p>
                          <p className="body-sm" style={{ marginTop: 2 }}>
                            {b.checkInDate} → {b.checkOutDate} · {fmtVnd(Number(b.totalAmount))}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                {role === 'MANAGER' && (
                  <button
                    className="btn-secondary"
                    onClick={() => openEdit(selectedUser)}
                  >
                    Sửa
                  </button>
                )}
                <button
                  className={
                    selectedUser.status === 'ACTIVE' ? 'btn-danger' : 'btn-primary'
                  }
                  disabled={
                    actionLoading
                    || (role === 'CUSTOMER' && selectedUser.status === 'INACTIVE')
                  }
                  title={
                    role === 'CUSTOMER' && selectedUser.status === 'INACTIVE'
                      ? 'Tài khoản chưa xác thực email — không Suspend/Activate'
                      : undefined
                  }
                  onClick={() => requestToggleStatus(selectedUser)}
                >
                  {actionLoading
                    ? 'Đang xử lý...'
                    : role === 'CUSTOMER'
                      ? selectedUser.status === 'ACTIVE'
                        ? 'Đình chỉ'
                        : selectedUser.status === 'SUSPENDED'
                          ? 'Kích hoạt'
                          : 'Không khả dụng'
                      : selectedUser.status === 'ACTIVE'
                        ? 'Vô hiệu hóa'
                        : 'Kích hoạt'}
                </button>
              </div>
            </div>
          )}
        </Drawer>

        <ConfirmModal
          open={!!confirmUser}
          title={
            role === 'CUSTOMER'
              ? confirmUser?.status === 'ACTIVE'
                ? 'Đình chỉ Customer?'
                : 'Kích hoạt Customer?'
              : confirmUser?.status === 'ACTIVE'
                ? 'Vô hiệu hóa tài khoản?'
                : 'Kích hoạt tài khoản?'
          }
          message={
            confirmUser
              ? role === 'CUSTOMER' && confirmUser.status === 'ACTIVE'
                ? `Đình chỉ ${confirmUser.fullName}? Họ sẽ không đăng nhập/refresh được; booking hiện tại không tự hủy.`
                : `Xác nhận đổi trạng thái tài khoản ${confirmUser.fullName}?`
              : ''
          }
          confirmLabel={
            role === 'CUSTOMER'
              ? confirmUser?.status === 'ACTIVE'
                ? 'Đình chỉ'
                : 'Kích hoạt'
              : 'Xác nhận'
          }
          danger={confirmUser?.status === 'ACTIVE'}
          onCancel={() => setConfirmUser(null)}
          onConfirm={() => {
            if (confirmUser) void applyStatusChange(confirmUser, nextStatusFor(confirmUser));
          }}
        />

        {role === 'MANAGER' && (
          <>
            <CreateManagerModal
              open={createOpen}
              onClose={() => setCreateOpen(false)}
              onCreated={msg => {
                setPageSuccess(msg);
                setError(null);
                void fetchUsers(0, keyword, statusFilter);
              }}
            />
            <EditManagerModal
              open={editOpen}
              user={editingUser}
              onClose={() => {
                setEditOpen(false);
                setEditingUser(null);
              }}
              onUpdated={(msg, updated) => {
                setPageSuccess(msg);
                setError(null);
                setUsers(list => list.map(u => (u.id === updated.id ? { ...u, ...updated } : u)));
                setSelectedUser(u => (u && u.id === updated.id ? { ...u, ...updated } : u));
                void fetchUsers(page, keyword, statusFilter);
              }}
            />
          </>
        )}
      </div>
    </AdminLayout>
  );
}

export { UserDirectoryPage };
