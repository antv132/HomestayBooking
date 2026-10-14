// ─── MaintenancePages.tsx — SCR-22, 23, 29 ───────────────────────────────────
// Exports: MaintenanceListPage, CreateMaintenancePage, MaintenanceDetailPage

import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import Alert from '../../components/ui/Alert';
import { maintenanceApi, MaintenanceTicket } from '../../api/maintenanceApi';
import { bookingApi, BookingSummary } from '../../api/bookingApi';
import { PhotoLightbox } from '../../components/PhotoLightbox';

// Helper to format static image URLs from the backend upload directory
const getPhotoUrl = (url: string) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  const backendBase = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  return `${backendBase}${url.startsWith('/') ? '' : '/'}${url}`;
};

const STATUS_CONFIG: Record<string, { cls: string; label: string }> = {
  OPEN:        { cls: 'badge-warning', label: 'Chờ xử lý' },
  ASSIGNED:    { cls: 'badge-info',    label: 'Đã phân công' },
  IN_PROGRESS: { cls: 'badge-info',    label: 'Đang xử lý' },
  RESOLVED:    { cls: 'badge-success', label: 'Đã xử lý' },
  CLOSED:      { cls: 'badge-neutral', label: 'Đã đóng' },
};

const TAB_LABELS: Record<string, string> = {
  ALL: 'Tất cả',
  OPEN: 'Chờ xử lý',
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang xử lý',
  RESOLVED: 'Đã xử lý',
  CLOSED: 'Đã đóng',
};

function StatusBadge({ s }: { s: string }) {
  const v = STATUS_CONFIG[s] ?? { cls: 'badge-neutral', label: s };
  return <span className={`badge ${v.cls}`}>{v.label}</span>;
}

function StatusTimeline({ status }: { status: string }) {
  const steps = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
  const curIdx = steps.indexOf(status);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 0 }}>
      {steps.map((step, i) => {
        const done = i <= curIdx;
        const isLast = i === steps.length - 1;
        return (
          <div key={step} style={{ display: 'flex', alignItems: 'center', flex: isLast ? 'none' : 1 }}>
            <div style={{ textAlign: 'center' }}>
              <div style={{ width: 28, height: 28, borderRadius: '50%', background: done ? 'var(--primary)' : 'var(--stone)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 4px' }}>
                {done ? <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5"><polyline points="20,6 9,17 4,12"/></svg> : <span style={{ fontSize: 11, fontWeight: 700, color: '#fff' }}>{i+1}</span>}
              </div>
              <p style={{ fontSize: 10, fontWeight: 600, color: done ? 'var(--primary)' : 'var(--charcoal)', whiteSpace: 'nowrap' }}>{step.replace('_',' ')}</p>
            </div>
            {!isLast && <div style={{ flex: 1, height: 2, background: i < curIdx ? 'var(--primary)' : 'var(--stone)', margin: '0 4px', marginBottom: 16 }} />}
          </div>
        );
      })}
    </div>
  );
}

// ── SCR-22: Maintenance Ticket List ─────────────────────────────────────────
const LIST_TABS = ['ALL', 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'] as const;
const PAGE_SIZE = 10;

function TableSkeleton() {
  return (
    <div className="table-wrap card">
      <table className="data-table">
        <thead>
          <tr>
            <th>Mã ticket</th>
            <th>Vấn đề</th>
            <th>Trạng thái</th>
            <th>Ngày gửi</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {[1, 2, 3].map(i => (
            <tr key={i}>
              {Array.from({ length: 5 }).map((_, j) => (
                <td key={j}>
                  <div className="h-4 rounded bg-[var(--surface-bone)] animate-pulse" style={{ width: j === 1 ? '80%' : '55%' }} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function MaintenanceListPage() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [list, setList] = useState<MaintenanceTicket[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadTickets() {
      setLoading(true);
      setError(null);
      try {
        const res = await maintenanceApi.getCustomerTickets({
          status: filter,
          page,
          size: PAGE_SIZE,
        });
        if (res.success && res.data) {
          setList(res.data.content);
          setTotalPages(res.data.totalPages ?? 0);
        } else {
          setError('Không thể tải danh sách yêu cầu.');
        }
      } catch (err) {
        console.error('Error loading tickets:', err);
        setError('Lỗi kết nối máy chủ. Vui lòng thử lại.');
      } finally {
        setLoading(false);
      }
    }
    loadTickets();
  }, [filter, page]);

  const handleFilterChange = (tab: string) => {
    setFilter(tab);
    setPage(0);
  };

  return (
    <CustomerLayout>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 className="heading-md">Yêu cầu bảo trì</h1>
        <Link to="/customer/maintenance/create" className="btn-primary btn-sm">
          Tạo yêu cầu
        </Link>
      </div>

      <div className="flex gap-1 flex-wrap p-1 mb-5 bg-[var(--surface-bone)] rounded-full w-fit">
        {LIST_TABS.map(tab => (
          <button
            key={tab}
            type="button"
            className={`tab-pill ${filter === tab ? 'active' : ''}`}
            onClick={() => handleFilterChange(tab)}
          >
            {TAB_LABELS[tab] ?? tab}
          </button>
        ))}
      </div>

      {loading ? (
        <TableSkeleton />
      ) : error ? (
        <div className="card p-6 max-w-lg space-y-4">
          <Alert variant="error" message={error} />
          <button
            type="button"
            className="btn-primary"
            onClick={() => {
              setError(null);
              setLoading(true);
              maintenanceApi.getCustomerTickets({ status: filter, page, size: PAGE_SIZE })
                .then(res => {
                  if (res.success && res.data) {
                    setList(res.data.content);
                    setTotalPages(res.data.totalPages ?? 0);
                  } else {
                    setError('Không thể tải danh sách yêu cầu.');
                  }
                })
                .catch(() => setError('Lỗi kết nối máy chủ. Vui lòng thử lại.'))
                .finally(() => setLoading(false));
            }}
          >
            Thử lại
          </button>
        </div>
      ) : list.length === 0 ? (
        <div className="card text-center py-16 px-6">
          <div className="text-5xl mb-4">🔧</div>
          <h3 className="heading-sm mb-2">Chưa có yêu cầu bảo trì</h3>
          <p className="body-md text-charcoal mb-5">
            Báo cáo sự cố tại phòng bạn đang lưu trú để được hỗ trợ nhanh chóng.
          </p>
          <Link to="/customer/maintenance/create" className="btn-primary">
            Tạo yêu cầu
          </Link>
        </div>
      ) : (
        <>
          <div className="table-wrap card">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã ticket</th>
                  <th>Vấn đề</th>
                  <th>Trạng thái</th>
                  <th>Ngày gửi</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {list.map(t => (
                  <tr
                    key={t.id}
                    className="cursor-pointer hover:bg-[var(--surface-bone)]"
                    onClick={() => navigate(`/customer/maintenance/${t.id}`)}
                  >
                    <td>
                      <span className="code-md font-semibold">
                        {t.id.slice(0, 8).toUpperCase()}
                      </span>
                    </td>
                    <td>
                      <span className="font-medium">{t.title}</span>
                      <span className="body-sm text-charcoal block">
                        {t.roomNumber} · {t.propertyName}
                      </span>
                    </td>
                    <td>
                      <StatusBadge s={t.status} />
                    </td>
                    <td className="body-sm text-charcoal whitespace-nowrap">
                      {new Date(t.createdAt).toLocaleString('vi-VN')}
                    </td>
                    <td onClick={e => e.stopPropagation()}>
                      <Link
                        to={`/customer/maintenance/${t.id}`}
                        className="btn-outline btn-sm no-underline"
                      >
                        Xem
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 mt-6">
              <button
                type="button"
                className="btn-outline btn-sm"
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >
                ← Trước
              </button>
              <span className="body-sm text-charcoal">
                Trang {page + 1} / {totalPages}
              </span>
              <button
                type="button"
                className="btn-outline btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Tiếp →
              </button>
            </div>
          )}
        </>
      )}
    </CustomerLayout>
  );
}

// ── SCR-23: Create Maintenance Ticket ───────────────────────────────────────
function formatBookingDate(d: string) {
  return new Date(d + 'T00:00:00').toLocaleDateString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

export function CreateMaintenancePage() {
  const navigate = useNavigate();
  const [bookings, setBookings] = useState<BookingSummary[]>([]);
  const [bookingsLoading, setBookingsLoading] = useState(true);
  const [form, setForm] = useState({ bookingId: '', title: '', description: '' });
  const [photos, setPhotos] = useState<File[]>([]);
  const [photoPreviews, setPhotoPreviews] = useState<string[]>([]);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    async function loadBookings() {
      setBookingsLoading(true);
      try {
        const res = await bookingApi.getMyActiveBookings();
        if (res.success && res.data) {
          setBookings(res.data);
        }
      } catch (err) {
        console.error('Failed to load active bookings', err);
      } finally {
        setBookingsLoading(false);
      }
    }
    loadBookings();
  }, []);

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files) return;
    const selectedFiles = Array.from(e.target.files);
    if (photos.length + selectedFiles.length > 5) {
      setPhotoError('Chỉ được tải lên tối đa 5 ảnh.');
      return;
    }
    setPhotoError(null);
    setPhotos(prev => [...prev, ...selectedFiles]);
    setPhotoPreviews(prev => [
      ...prev,
      ...selectedFiles.map(file => URL.createObjectURL(file)),
    ]);
    e.target.value = '';
  };

  const removePhoto = (index: number) => {
    setPhotos(prev => {
      const next = [...prev];
      next.splice(index, 1);
      return next;
    });
    setPhotoPreviews(prev => {
      const next = [...prev];
      URL.revokeObjectURL(next[index]);
      next.splice(index, 1);
      return next;
    });
    setPhotoError(null);
  };

  function validate() {
    const e: Record<string, string> = {};
    if (!form.bookingId.trim()) e.bookingId = 'Vui lòng chọn đặt phòng liên quan';
    if (!form.title.trim()) e.title = 'Tiêu đề không được để trống';
    if (!form.description.trim()) e.description = 'Mô tả không được để trống';
    else if (form.description.length < 20) e.description = 'Mô tả phải có ít nhất 20 ký tự';
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setLoading(true);

    try {
      const formData = new FormData();
      formData.append('bookingId', form.bookingId);
      formData.append('title', form.title);
      formData.append('description', form.description);
      photos.forEach(photo => {
        formData.append('files', photo);
      });

      const res = await maintenanceApi.createTicket(formData);
      if (res.success) {
        navigate('/customer/maintenance');
      } else {
        setErrors({ submit: 'Không thể gửi yêu cầu bảo trì.' });
        setLoading(false);
      }
    } catch (err: unknown) {
      console.error(err);
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setErrors({ submit: message || 'Lỗi máy chủ khi gửi yêu cầu.' });
      setLoading(false);
    }
  }

  return (
    <CustomerLayout>
      <div className="max-w-xl mx-auto">
        <nav className="flex items-center gap-2 body-sm text-charcoal mb-5">
          <Link to="/customer/maintenance" className="text-primary no-underline">Yêu cầu bảo trì</Link>
          <span aria-hidden="true">›</span>
          <span className="font-semibold">Tạo mới</span>
        </nav>

        <h1 className="heading-md mb-6">Báo cáo sự cố</h1>

        {errors.submit && (
          <div className="mb-4">
            <Alert variant="error" message={errors.submit} />
          </div>
        )}

        {!bookingsLoading && bookings.length === 0 && (
          <div className="mb-4">
            <Alert
              variant="info"
              message="Bạn chưa có đặt phòng đang hiệu lực để báo cáo sự cố."
            />
            <Link to="/customer/bookings" className="btn-outline btn-sm mt-3 inline-flex">
              Xem đặt phòng
            </Link>
          </div>
        )}

        <form onSubmit={handleSubmit} className="card p-7">
          <div className="mb-4">
            <label className="form-label form-label-required" htmlFor="bookingId">
              Đặt phòng liên quan
            </label>
            <select
              id="bookingId"
              className={`select w-full ${errors.bookingId ? 'input-error' : ''}`}
              value={form.bookingId}
              onChange={ev => setForm(p => ({ ...p, bookingId: ev.target.value }))}
              disabled={bookingsLoading || bookings.length === 0}
            >
              <option value="">
                {bookingsLoading ? 'Đang tải...' : 'Chọn đặt phòng'}
              </option>
              {bookings.map(b => (
                <option key={b.bookingId} value={b.bookingId}>
                  {b.roomNumber} – {b.propertyName} ({formatBookingDate(b.checkInDate)} → {formatBookingDate(b.checkOutDate)})
                </option>
              ))}
            </select>
            {errors.bookingId && <p className="form-error">{errors.bookingId}</p>}
          </div>

          <div className="mb-4">
            <label className="form-label form-label-required" htmlFor="title">
              Tiêu đề sự cố
            </label>
            <input
              id="title"
              className={`input w-full ${errors.title ? 'input-error' : ''}`}
              placeholder="VD: Điều hòa không hoạt động"
              value={form.title}
              onChange={ev => setForm(p => ({ ...p, title: ev.target.value }))}
            />
            {errors.title && <p className="form-error">{errors.title}</p>}
          </div>

          <div className="mb-4">
            <label className="form-label form-label-required" htmlFor="description">
              Mô tả chi tiết
            </label>
            <textarea
              id="description"
              className={`textarea w-full ${errors.description ? 'input-error' : ''}`}
              rows={5}
              placeholder="Mô tả chi tiết sự cố để chúng tôi hỗ trợ bạn nhanh hơn..."
              value={form.description}
              onChange={ev => setForm(p => ({ ...p, description: ev.target.value }))}
            />
            <div className="flex justify-between mt-1">
              {errors.description ? <p className="form-error">{errors.description}</p> : <span />}
              <span className="form-hint">{form.description.length} ký tự</span>
            </div>
          </div>

          <div className="mb-6">
            <label className="form-label">Ảnh minh họa (tùy chọn, tối đa 5)</label>
            <div className="flex items-center gap-3 flex-wrap">
              <input
                type="file"
                multiple
                accept="image/*"
                onChange={handlePhotoChange}
                disabled={loading || photos.length >= 5}
                className="hidden"
                id="photo-upload"
              />
              <label
                htmlFor="photo-upload"
                className="btn-outline btn-sm inline-flex items-center gap-1.5 cursor-pointer"
              >
                📁 Chọn ảnh
              </label>
              <span className="body-xs text-charcoal">{photos.length}/5 ảnh</span>
            </div>
            {photoError && <p className="form-error mt-2">{photoError}</p>}
            {photoPreviews.length > 0 && (
              <div className="flex gap-2.5 flex-wrap mt-3">
                {photoPreviews.map((preview, idx) => (
                  <div key={preview} className="relative w-[70px] h-[70px]">
                    <img
                      src={preview}
                      alt={`Ảnh ${idx + 1}`}
                      className="w-full h-full object-cover rounded-lg border border-[var(--hairline)]"
                    />
                    <button
                      type="button"
                      onClick={() => removePhoto(idx)}
                      className="absolute -top-1.5 -right-1.5 w-[18px] h-[18px] rounded-full bg-[var(--error)] text-white border-0 text-xs font-bold cursor-pointer flex items-center justify-center"
                      aria-label="Xóa ảnh"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="mb-5">
            <Alert variant="info" message="Đội ngũ sẽ xem xét trong vòng 24 giờ." />
          </div>

          <div className="flex gap-3">
            <button type="submit" className="btn-primary" disabled={loading || bookings.length === 0}>
              {loading ? 'Đang gửi...' : 'Gửi yêu cầu'}
            </button>
            <Link to="/customer/maintenance" className="btn-ghost">
              Hủy
            </Link>
          </div>
        </form>
      </div>
    </CustomerLayout>
  );
}

// ── SCR-29: Detail ────────────────────────────────────────────────────────────
export function MaintenanceDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [ticket, setTicket] = useState<MaintenanceTicket | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ title: '', description: '' });
  
  // Photo editing state
  const [editExistingPhotos, setEditExistingPhotos] = useState<string[]>([]);
  const [editNewPhotos, setEditNewPhotos] = useState<File[]>([]);
  
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);
  
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    async function loadTicket() {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const res = await maintenanceApi.getTicketDetail(id);
        if (res.success && res.data) {
          setTicket(res.data);
          setEditForm({ title: res.data.title, description: res.data.description });
        } else {
          setError("Maintenance request not found.");
        }
      } catch (err: any) {
        console.error(err);
        setError(err.response?.data?.message || "Failed to load maintenance request details.");
      } finally {
        setLoading(false);
      }
    }
    loadTicket();
  }, [id]);

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !ticket) return;
    if (!editForm.title.trim()) { setEditError('Title is required'); return; }
    if (!editForm.description.trim()) { setEditError('Description is required'); return; }
    if (editForm.description.length < 20) { setEditError('Description must be at least 20 characters'); return; }
    
    const totalPhotos = editExistingPhotos.length + editNewPhotos.length;
    if (totalPhotos > 5) { setEditError('Maximum 5 photos allowed'); return; }
    
    setEditLoading(true);
    setEditError(null);
    try {
      const formData = new FormData();
      formData.append('title', editForm.title.trim());
      formData.append('description', editForm.description.trim());
      
      // Send which existing photos to keep
      editExistingPhotos.forEach(url => {
        formData.append('existingPhotoUrls', url);
      });
      
      // Send new photos
      editNewPhotos.forEach(file => {
        formData.append('photos', file);
      });
      
      const res = await maintenanceApi.updateTicket(id, formData as any);
      if (res.success && res.data) {
        setTicket(res.data);
        setIsEditing(false);
        setEditNewPhotos([]);
      } else {
        setEditError('Update failed.');
      }
    } catch (err: any) {
      console.error(err);
      setEditError(err.response?.data?.message || 'Server error occurred during update.');
    } finally {
      setEditLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    setDeleteLoading(true);
    try {
      const res = await maintenanceApi.deleteTicket(id);
      if (res.success) {
        navigate('/customer/maintenance');
      } else {
        alert('Failed to delete ticket.');
        setDeleteLoading(false);
      }
    } catch (err: any) {
      console.error(err);
      alert(err.response?.data?.message || 'Server error occurred during deletion.');
      setDeleteLoading(false);
    }
  };

  if (loading) {
    return (
      <CustomerLayout>
        <div style={{ textAlign: 'center', padding: 60 }}>
          <p className="body-md text-charcoal">Loading ticket details...</p>
        </div>
      </CustomerLayout>
    );
  }

  if (error || !ticket) {
    return (
      <CustomerLayout>
        <div style={{ maxWidth: 720, margin: '0 auto' }}>
          <div className="alert alert-error" style={{ marginBottom: 20 }}>
            {error || 'Maintenance ticket details could not be found.'}
          </div>
          <Link to="/customer/maintenance" className="btn-outline">← Back to List</Link>
        </div>
      </CustomerLayout>
    );
  }

  return (
    <CustomerLayout>
      <div style={{ maxWidth: 720, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to="/customer/maintenance" className="text-primary" style={{ textDecoration: 'none' }}>Maintenance</Link>
          <span>›</span>
          <span style={{ fontWeight: 600 }}>#{ticket.id.substring(0, 8)}...</span>
        </div>

        {isEditing ? (
          <div className="card" style={{ padding: 28, marginBottom: 20 }}>
            <h2 className="heading-sm" style={{ marginBottom: 20 }}>Edit Maintenance Request</h2>
            {editError && <div className="alert alert-error" style={{ marginBottom: 16 }}>{editError}</div>}
            
            <form onSubmit={handleEditSubmit}>
              <div style={{ marginBottom: 16 }}>
                <label className="form-label form-label-required" htmlFor="edit-title">Issue Title</label>
                <input id="edit-title" className="input" value={editForm.title}
                  onChange={e => setEditForm(p => ({ ...p, title: e.target.value }))} disabled={editLoading} />
              </div>
              <div style={{ marginBottom: 24 }}>
                <label className="form-label form-label-required" htmlFor="edit-description">Description</label>
                <textarea id="edit-description" className="textarea" rows={5} value={editForm.description}
                  onChange={e => setEditForm(p => ({ ...p, description: e.target.value }))} disabled={editLoading} />
              </div>

              {/* ── Photo Editing Section ── */}
              <div style={{ marginBottom: 24 }}>
                <label className="form-label">Attached Photos <span style={{ fontWeight: 400, color: 'var(--text-muted)' }}>({editExistingPhotos.length + editNewPhotos.length}/5)</span></label>

                {/* Existing photos with remove button */}
                {editExistingPhotos.length > 0 && (
                  <div style={{ marginBottom: 12 }}>
                    <p className="body-sm text-charcoal" style={{ marginBottom: 6 }}>Current photos (click ✕ to remove):</p>
                    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                      {editExistingPhotos.map((url, idx) => (
                        <div key={idx} style={{ position: 'relative', width: 90, height: 90, borderRadius: 8, overflow: 'hidden', border: '2px solid var(--hairline)' }}>
                          <img src={getPhotoUrl(url)} alt={`Photo ${idx+1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                          <button type="button" onClick={() => setEditExistingPhotos(p => p.filter((_, i) => i !== idx))}
                            style={{ position: 'absolute', top: 2, right: 2, background: 'rgba(234,40,4,0.85)', color: '#fff', border: 'none', borderRadius: '50%', width: 22, height: 22, cursor: 'pointer', fontSize: 12, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', lineHeight: 1 }}
                            disabled={editLoading}>✕</button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* New photos with preview and remove */}
                {editNewPhotos.length > 0 && (
                  <div style={{ marginBottom: 12 }}>
                    <p className="body-sm text-charcoal" style={{ marginBottom: 6 }}>New photos to add:</p>
                    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                      {editNewPhotos.map((file, idx) => (
                        <div key={idx} style={{ position: 'relative', width: 90, height: 90, borderRadius: 8, overflow: 'hidden', border: '2px solid #22c55e' }}>
                          <img src={URL.createObjectURL(file)} alt={`New ${idx+1}`} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                          <button type="button" onClick={() => setEditNewPhotos(p => p.filter((_, i) => i !== idx))}
                            style={{ position: 'absolute', top: 2, right: 2, background: 'rgba(234,40,4,0.85)', color: '#fff', border: 'none', borderRadius: '50%', width: 22, height: 22, cursor: 'pointer', fontSize: 12, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', lineHeight: 1 }}
                            disabled={editLoading}>✕</button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Add more button */}
                {(editExistingPhotos.length + editNewPhotos.length) < 5 && (
                  <label style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '8px 16px', background: 'var(--surface-bone)', border: '1.5px dashed var(--stone)', borderRadius: 8, cursor: 'pointer', fontSize: 13, fontWeight: 600, color: 'var(--charcoal)' }}>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="2" /><line x1="12" y1="8" x2="12" y2="16" /><line x1="8" y1="12" x2="16" y2="12" /></svg>
                    Add Photos
                    <input type="file" accept="image/*" multiple hidden
                      onChange={e => {
                        if (!e.target.files) return;
                        const remaining = 5 - editExistingPhotos.length - editNewPhotos.length;
                        const files = Array.from(e.target.files).slice(0, remaining);
                        setEditNewPhotos(p => [...p, ...files]);
                        e.target.value = '';
                      }} disabled={editLoading} />
                  </label>
                )}
              </div>

              <div style={{ display: 'flex', gap: 12 }}>
                <button type="submit" className="btn-primary" disabled={editLoading}>
                  {editLoading ? 'Saving...' : 'Save Changes'}
                </button>
                <button type="button" className="btn-ghost" onClick={() => { setIsEditing(false); setEditError(null); setEditNewPhotos([]); }} disabled={editLoading}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        ) : (
          <>
            <div className="flex items-start justify-between" style={{ marginBottom: 24 }}>
              <div>
                <h1 className="heading-md" style={{ marginBottom: 4 }}>{ticket.title}</h1>
                <p className="body-sm text-charcoal">Ticket #{ticket.id} · {ticket.roomNumber} · {ticket.propertyName}</p>
              </div>
              <StatusBadge s={ticket.status} />
            </div>

            {/* Management Buttons for OPEN tickets */}
            {ticket.status === 'OPEN' && (
              <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
                <button className="btn-outline btn-sm" onClick={() => { setIsEditing(true); setEditExistingPhotos(ticket.photoUrls || []); setEditNewPhotos([]); }}>✏️ Edit Request</button>
                <button className="btn-outline btn-sm" style={{ borderColor: 'var(--error)', color: 'var(--error)' }} onClick={() => setShowDeleteConfirm(true)}>🗑️ Delete Request</button>
              </div>
            )}

            {showDeleteConfirm && (
              <div className="alert alert-error" style={{ marginBottom: 20, padding: 20, display: 'block' }}>
                <h4 style={{ fontWeight: 700, marginBottom: 8, color: '#991b1b' }}>Are you sure you want to delete this request?</h4>
                <p className="body-sm text-charcoal" style={{ marginBottom: 16 }}>This action cannot be undone and will permanently remove this maintenance ticket.</p>
                <div style={{ display: 'flex', gap: 10 }}>
                  <button className="btn-danger" onClick={handleDelete} disabled={deleteLoading}>
                    {deleteLoading ? 'Deleting...' : 'Yes, Delete'}
                  </button>
                  <button className="btn-ghost" onClick={() => setShowDeleteConfirm(false)} disabled={deleteLoading} style={{ background: 'var(--surface-bone)' }}>
                    Cancel
                  </button>
                </div>
              </div>
            )}

            {/* Resolution note from Manager */}
            {ticket.status === 'CLOSED' && ticket.resolutionNote && (
              <div className="card" style={{ padding: 24, marginBottom: 20, borderLeft: '4px solid var(--primary)' }}>
                <h2 className="heading-sm" style={{ marginBottom: 8 }}>Manager Resolution Note</h2>
                <p className="body-md text-charcoal">{ticket.resolutionNote}</p>
              </div>
            )}

            {/* Progress */}
            <div className="card" style={{ padding: 24, marginBottom: 20 }}>
              <h2 className="heading-sm" style={{ marginBottom: 20 }}>Status Progress</h2>
              <StatusTimeline status={ticket.status} />
            </div>

            {/* Info */}
            <div className="card" style={{ padding: 24, marginBottom: 20 }}>
              <h2 className="heading-sm" style={{ marginBottom: 16 }}>Issue Details</h2>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
                <div>
                  <p className="body-sm text-charcoal">Room</p>
                  <p style={{ fontWeight: 600 }}>{ticket.roomNumber}</p>
                </div>
                <div>
                  <p className="body-sm text-charcoal">Property</p>
                  <p style={{ fontWeight: 600 }}>{ticket.propertyName}</p>
                </div>
                <div>
                  <p className="body-sm text-charcoal">Submitted</p>
                  <p style={{ fontWeight: 600 }}>{new Date(ticket.createdAt).toLocaleString('en-US')}</p>
                </div>
                <div>
                  <p className="body-sm text-charcoal">Last Updated</p>
                  <p style={{ fontWeight: 600 }}>{new Date(ticket.updatedAt).toLocaleString('en-US')}</p>
                </div>
              </div>
              
              <div style={{ marginBottom: 16 }}>
                <p className="body-sm text-charcoal" style={{ marginBottom: 6 }}>Description</p>
                <p className="body-md" style={{ padding: '12px 16px', background: 'var(--surface-bone)', borderRadius: 8, whiteSpace: 'pre-wrap' }}>{ticket.description}</p>
              </div>

              {/* Photos rendering */}
              {ticket.photoUrls && ticket.photoUrls.length > 0 && (
                <div>
                  <p className="body-sm text-charcoal" style={{ marginBottom: 8 }}>Attached Photos</p>
                  <PhotoLightbox photoUrls={ticket.photoUrls} getPhotoUrl={getPhotoUrl} />
                </div>
              )}
            </div>
          </>
        )}

        <Link to="/customer/maintenance" className="btn-outline">← Back to List</Link>
      </div>
    </CustomerLayout>
  );
}
