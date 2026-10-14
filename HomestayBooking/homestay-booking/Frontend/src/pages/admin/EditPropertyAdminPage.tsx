import { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import { getAdminProperty, updateAdminProperty } from '../../api/adminApi';
import { extractApiError, Spinner, ErrorBanner, SuccessBanner } from './_adminShared';

export function EditPropertyAdminPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    address: '',
    description: '',
    status: 'ACTIVE' as 'ACTIVE' | 'INACTIVE',
  });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    async function load() {
      setFetching(true);
      setError(null);
      try {
        const res = await getAdminProperty(id!);
        if (cancelled) return;
        if (res.success && res.data) {
          setForm({
            name: res.data.name || '',
            address: res.data.location || '',
            description: res.data.description || '',
            status: res.data.status,
          });
        } else {
          setError('Không tìm thấy property.');
        }
      } catch (err) {
        if (!cancelled) setError(extractApiError(err, 'Không tải được thông tin property.'));
      } finally {
        if (!cancelled) setFetching(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [id]);

  function validate() {
    const e: Record<string, string> = {};
    if (!form.name.trim()) e.name = 'Tên property không được để trống';
    if (!form.address.trim()) e.address = 'Địa chỉ không được để trống';
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!id) return;
    const errs = validate();
    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setError(null);
    setSuccess(null);
    setLoading(true);
    try {
      const res = await updateAdminProperty(id, {
        name: form.name.trim(),
        address: form.address.trim(),
        description: form.description.trim() || null,
        status: form.status,
      });
      if (res.success) {
        setSuccess('Cập nhật thành công!');
        setTimeout(() => navigate('/admin/properties'), 1000);
      } else {
        setError('Cập nhật thất bại.');
      }
    } catch (err) {
      setError(extractApiError(err, 'Cập nhật thất bại.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AdminLayout>
      <div className="animate-fade-in" style={{ maxWidth: 680, margin: '0 auto' }}>
        <div style={{ marginBottom: 28 }}>
          <Link
            to="/admin/properties"
            className="body-sm"
            style={{
              textDecoration: 'none',
              color: 'var(--primary)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              fontWeight: 500,
            }}
          >
            <span aria-hidden>←</span> Danh sách Property
          </Link>
          <h1
            style={{
              fontFamily: 'Outfit',
              fontSize: 28,
              fontWeight: 700,
              color: 'var(--ink)',
              marginTop: 12,
              letterSpacing: '-0.02em',
            }}
          >
            Edit Property
          </h1>
          {id && (
            <Link
              to={`/admin/properties/${id}/manager`}
              className="btn-outline btn-sm"
              style={{ marginTop: 12, display: 'inline-flex' }}
            >
              Assign Manager
            </Link>
          )}
        </div>

        {error && <ErrorBanner msg={error} />}
        {success && <SuccessBanner msg={success} />}

        {fetching ? (
          <Spinner />
        ) : (
          <form onSubmit={handleSubmit}>
            <div
              className="card"
              style={{
                padding: 0,
                overflow: 'hidden',
                border: '1px solid var(--hairline)',
                boxShadow: '0 1px 3px rgba(15, 23, 42, 0.04)',
              }}
            >
              <div
                style={{
                  height: 4,
                  background: 'linear-gradient(90deg, var(--primary) 0%, #14B8A6 100%)',
                }}
              />

              <div style={{ padding: '28px 32px 8px' }}>
                <div style={{ marginBottom: 22 }}>
                  <label className="form-label form-label-required" htmlFor="edit-prop-name">
                    Tên Property
                  </label>
                  <input
                    id="edit-prop-name"
                    className={`input ${errors.name ? 'input-error' : ''}`}
                    placeholder="VD: Sunset Resort Đà Nẵng"
                    value={form.name}
                    onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                    autoFocus
                  />
                  {errors.name && <p className="form-error">{errors.name}</p>}
                </div>

                <div style={{ marginBottom: 22 }}>
                  <label className="form-label form-label-required" htmlFor="edit-prop-address">
                    Địa chỉ
                  </label>
                  <input
                    id="edit-prop-address"
                    className={`input ${errors.address ? 'input-error' : ''}`}
                    placeholder="VD: 123 Trần Hưng Đạo, Đà Nẵng"
                    value={form.address}
                    onChange={e => setForm(f => ({ ...f, address: e.target.value }))}
                  />
                  {errors.address && <p className="form-error">{errors.address}</p>}
                </div>

                <div style={{ marginBottom: 22 }}>
                  <label className="form-label" htmlFor="edit-prop-description">
                    Mô tả
                  </label>
                  <textarea
                    id="edit-prop-description"
                    className="input"
                    rows={4}
                    placeholder="Giới thiệu ngắn về chi nhánh (không bắt buộc)"
                    value={form.description}
                    onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                    style={{
                      resize: 'vertical',
                      minHeight: 108,
                      paddingTop: 12,
                      paddingBottom: 12,
                      lineHeight: 1.55,
                    }}
                  />
                </div>

                <div style={{ marginBottom: 8 }}>
                  <span className="form-label" id="edit-prop-status-label">
                    Trạng thái
                  </span>
                  <div
                    role="group"
                    aria-labelledby="edit-prop-status-label"
                    style={{ display: 'flex', gap: 10, marginTop: 4 }}
                  >
                    {(
                      [
                        { value: 'ACTIVE', label: 'Hoạt động', hint: 'Hiển thị & nhận booking' },
                        { value: 'INACTIVE', label: 'Ngừng hoạt động', hint: 'Ẩn khỏi vận hành' },
                      ] as const
                    ).map(opt => {
                      const selected = form.status === opt.value;
                      return (
                        <button
                          key={opt.value}
                          type="button"
                          onClick={() => setForm(f => ({ ...f, status: opt.value }))}
                          style={{
                            flex: 1,
                            textAlign: 'left',
                            padding: '12px 14px',
                            borderRadius: 10,
                            border: selected
                              ? '1.5px solid var(--primary)'
                              : '1.5px solid var(--hairline)',
                            background: selected ? 'rgba(15, 118, 110, 0.06)' : '#fff',
                            cursor: 'pointer',
                            transition: 'border-color 0.15s, background 0.15s',
                          }}
                        >
                          <div
                            style={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 8,
                              marginBottom: 4,
                            }}
                          >
                            <span
                              style={{
                                width: 8,
                                height: 8,
                                borderRadius: '50%',
                                background: opt.value === 'ACTIVE' ? '#10B981' : '#94A3B8',
                                flexShrink: 0,
                              }}
                            />
                            <span
                              style={{
                                fontFamily: 'Outfit',
                                fontSize: 14,
                                fontWeight: 600,
                                color: selected ? 'var(--primary)' : 'var(--ink)',
                              }}
                            >
                              {opt.label}
                            </span>
                          </div>
                          <p
                            className="body-sm"
                            style={{ margin: 0, color: 'var(--charcoal)', fontSize: 12 }}
                          >
                            {opt.hint}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'flex-end',
                  alignItems: 'center',
                  gap: 12,
                  padding: '20px 32px',
                  marginTop: 16,
                  background: '#F8FAFC',
                  borderTop: '1px solid var(--hairline)',
                }}
              >
                <Link to="/admin/properties" className="btn-ghost">
                  Hủy
                </Link>
                <button type="submit" className="btn-primary" disabled={loading} style={{ minWidth: 140 }}>
                  {loading ? 'Đang lưu...' : 'Lưu thay đổi'}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </AdminLayout>
  );
}
