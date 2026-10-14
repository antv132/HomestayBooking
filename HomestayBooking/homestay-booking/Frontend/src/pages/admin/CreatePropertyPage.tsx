import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AdminLayout from '../../layouts/AdminLayout';
import { createAdminProperty } from '../../api/adminApi';
import { extractApiError, ErrorBanner } from './_adminShared';

export function CreatePropertyPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    name: '',
    address: '',
    description: '',
    status: 'INACTIVE' as 'ACTIVE' | 'INACTIVE',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  function validate() {
    const e: Record<string, string> = {};
    if (!form.name.trim()) e.name = 'Tên property không được để trống';
    if (!form.address.trim()) e.address = 'Địa chỉ không được để trống';
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
    setError(null);
    setLoading(true);
    try {
      const res = await createAdminProperty({
        name: form.name.trim(),
        address: form.address.trim(),
        description: form.description.trim() || undefined,
        status: 'INACTIVE',
      });
      if (res.success) {
        navigate('/admin/properties');
      } else {
        setError('Tạo property thất bại.');
      }
    } catch (err) {
      setError(extractApiError(err, 'Tạo property thất bại.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AdminLayout>
      <div className="animate-fade-in" style={{ maxWidth: 680, margin: '0 auto' }}>
        {/* Page header */}
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
            Create Property
          </h1>
        </div>

        {error && <ErrorBanner msg={error} />}

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
            {/* Accent bar */}
            <div
              style={{
                height: 4,
                background: 'linear-gradient(90deg, var(--primary) 0%, #14B8A6 100%)',
              }}
            />

            <div style={{ padding: '28px 32px 8px' }}>
              {/* Name */}
              <div style={{ marginBottom: 22 }}>
                <label className="form-label form-label-required" htmlFor="prop-name">
                  Tên Property
                </label>
                <input
                  id="prop-name"
                  className={`input ${errors.name ? 'input-error' : ''}`}
                  placeholder="VD: Sunset Resort Đà Nẵng"
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  autoFocus
                />
                {errors.name && <p className="form-error">{errors.name}</p>}
              </div>

              {/* Address */}
              <div style={{ marginBottom: 22 }}>
                <label className="form-label form-label-required" htmlFor="prop-address">
                  Địa chỉ
                </label>
                <input
                  id="prop-address"
                  className={`input ${errors.address ? 'input-error' : ''}`}
                  placeholder="VD: 123 Trần Hưng Đạo, Đà Nẵng"
                  value={form.address}
                  onChange={e => setForm(f => ({ ...f, address: e.target.value }))}
                />
                {errors.address && <p className="form-error">{errors.address}</p>}
              </div>

              {/* Description */}
              <div style={{ marginBottom: 22 }}>
                <label className="form-label" htmlFor="prop-description">
                  Mô tả
                </label>
                <textarea
                  id="prop-description"
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

              {/* FR-06: new property always INACTIVE until Manager assigned */}
              <div
                style={{
                  marginBottom: 16,
                  padding: '12px 14px',
                  borderRadius: 10,
                  background: 'rgba(15, 118, 110, 0.06)',
                  border: '1px solid rgba(15, 118, 110, 0.2)',
                }}
              >
                <p style={{ margin: 0, fontSize: 13, fontWeight: 600, color: 'var(--ink)' }}>
                  Trạng thái mặc định: Ngừng hoạt động (INACTIVE)
                </p>
                <p className="body-sm" style={{ margin: '4px 0 0', color: 'var(--charcoal)', fontSize: 12 }}>
                  Gán Manager trước, rồi kích hoạt ACTIVE ở màn Edit / danh sách.
                </p>
              </div>
            </div>

            {/* Footer actions */}
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
                {loading ? 'Đang tạo...' : 'Tạo Property'}
              </button>
            </div>
          </div>
        </form>
      </div>
    </AdminLayout>
  );
}
