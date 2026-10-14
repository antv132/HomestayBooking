// ─── ProfilePages.tsx — SCR-10, 11, 12 ───────────────────────────────────────
// Exports: UserProfilePage, EditProfilePage, ChangePasswordPage

import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import ManagerLayout from '../../layouts/ManagerLayout';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import AdminLayout from '../../layouts/AdminLayout';
import Alert from '../../components/ui/Alert';
import { StatusBadge, type StatusVariant } from '../../components/ui/StatusBadge';
import { useAuthStore } from '../../store/authStore';
import { fetchMyProfile, updateMyProfile, changeMyPassword, type UserProfile } from '../../api/usersApi';

function nameInitials(name: string) {
  return name.trim().split(/\s+/).map(w => w[0]).slice(-2).join('').toUpperCase() || '?';
}

/** Avatar with initials fallback when URL is missing or fails to load (FR-02 US3). */
function ProfileAvatar({
  name,
  url,
  size,
  fontSize,
}: {
  name: string;
  url?: string | null;
  size: number;
  fontSize: number;
}) {
  const [broken, setBroken] = useState(false);
  const showImg = !!url && !broken;

  if (showImg) {
    return (
      <img
        src={url}
        alt={name}
        style={{ width: size, height: size, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
        onError={() => setBroken(true)}
      />
    );
  }

  return (
    <div
      style={{
        width: size,
        height: size,
        borderRadius: '50%',
        background: 'var(--primary)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize,
        fontWeight: 700,
        color: '#fff',
        flexShrink: 0,
      }}
    >
      {nameInitials(name)}
    </div>
  );
}

function getProfileBase(role: string | null): string {
  switch (role) {
    case 'MANAGER':
      return '/manager/profile';
    case 'EMPLOYEE':
      return '/employee/profile';
    case 'ADMIN':
      return '/admin/profile';
    default:
      return '/customer/profile';
  }
}

function roleLabel(role: string): string {
  switch (role) {
    case 'MANAGER':
      return 'Quản lý';
    case 'EMPLOYEE':
      return 'Nhân viên';
    case 'ADMIN':
      return 'Quản trị viên';
    default:
      return 'Khách hàng';
  }
}

function roleBadgeVariant(role: string): StatusVariant {
  switch (role) {
    case 'MANAGER':
      return 'warning';
    case 'EMPLOYEE':
      return 'info';
    case 'ADMIN':
      return 'primary';
    default:
      return 'info';
  }
}

function statusLabel(status: string): string {
  switch (status) {
    case 'ACTIVE':
      return 'Hoạt động';
    case 'INACTIVE':
      return 'Chưa kích hoạt';
    case 'SUSPENDED':
      return 'Đình chỉ';
    default:
      return status;
  }
}

function statusVariant(status: string): StatusVariant {
  switch (status) {
    case 'ACTIVE':
      return 'success';
    case 'SUSPENDED':
      return 'danger';
    default:
      return 'neutral';
  }
}

function formatJoinedDate(iso: string) {
  return new Date(iso).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

function RoleLayout({ children }: { children: React.ReactNode }) {
  const role = useAuthStore(s => s.role);
  if (role === 'MANAGER') return <ManagerLayout>{children}</ManagerLayout>;
  if (role === 'EMPLOYEE') return <EmployeeLayout>{children}</EmployeeLayout>;
  if (role === 'ADMIN') return <AdminLayout>{children}</AdminLayout>;
  return <CustomerLayout>{children}</CustomerLayout>;
}

// ── SCR-10: User Profile (read-only) ───────────────────────────────────────────
export function UserProfilePage() {
  const role = useAuthStore(s => s.role);
  const updateAuthProfile = useAuthStore(s => s.updateProfile);
  const profileBase = getProfileBase(role);

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError('');
      try {
        const data = await fetchMyProfile();
        if (cancelled) return;
        setProfile(data);
        updateAuthProfile({
          fullName: data.fullName,
          phone: data.phone ?? '',
          avatarUrl: data.avatarUrl ?? '',
        });
      } catch {
        if (!cancelled) setError('Không thể tải thông tin hồ sơ. Vui lòng thử lại.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [reloadKey, updateAuthProfile]);

  return (
    <RoleLayout>
      <div style={{ maxWidth: 680, margin: '0 auto' }}>
        <h1 className="heading-md" style={{ marginBottom: 24 }}>Hồ sơ của tôi</h1>

        {loading ? (
          <div className="card-lg" style={{ padding: 48, textAlign: 'center' }}>
            <p className="body-md text-charcoal">Đang tải hồ sơ...</p>
          </div>
        ) : error || !profile ? (
          <div style={{ maxWidth: 480, margin: '0 auto' }}>
            <Alert variant="error" message={error || 'Không tìm thấy hồ sơ.'} />
            <button
              type="button"
              className="btn-primary btn-sm"
              style={{ marginTop: 16 }}
              onClick={() => { setError(''); setReloadKey((k) => k + 1); }}
            >
              Thử lại
            </button>
          </div>
        ) : (
          <>
            <div className="card-lg" style={{ padding: 32, marginBottom: 20, boxShadow: '0 4px 16px rgba(32,32,32,0.06)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 32, paddingBottom: 24, borderBottom: '1px solid var(--hairline)', flexWrap: 'wrap' }}>
                <ProfileAvatar name={profile.fullName} url={profile.avatarUrl} size={120} fontSize={36} />
                <div style={{ flex: 1, minWidth: 200 }}>
                  <h2 className="heading-sm" style={{ marginBottom: 6 }}>{profile.fullName}</h2>
                  <p className="body-sm text-charcoal" style={{ marginBottom: 4 }}>{profile.email}</p>
                  <p className="body-sm text-charcoal">
                    Tham gia từ {formatJoinedDate(profile.createdAt)}
                  </p>
                </div>
                <StatusBadge status={roleLabel(profile.role)} variant={roleBadgeVariant(profile.role)} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 20 }}>
                <div>
                  <p className="form-label">Họ tên</p>
                  <p style={{ fontSize: 15, color: 'var(--ink)', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    {profile.fullName || '—'}
                  </p>
                </div>
                <div>
                  <p className="form-label">Email</p>
                  <p style={{ fontSize: 15, color: 'var(--ink)', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    {profile.email || '—'}
                  </p>
                </div>
                <div>
                  <p className="form-label">Số điện thoại</p>
                  <p style={{ fontSize: 15, color: 'var(--ink)', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    {profile.phone || '—'}
                  </p>
                </div>
                <div>
                  <p className="form-label">Vai trò</p>
                  <div style={{ padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    <StatusBadge status={roleLabel(profile.role)} variant={roleBadgeVariant(profile.role)} />
                  </div>
                </div>
                <div>
                  <p className="form-label">Trạng thái</p>
                  <div style={{ padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    <StatusBadge status={statusLabel(profile.status)} variant={statusVariant(profile.status)} />
                  </div>
                </div>
                <div>
                  <p className="form-label">Ngày tham gia</p>
                  <p style={{ fontSize: 15, color: 'var(--ink)', padding: '10px 0', borderBottom: '1px solid var(--hairline)' }}>
                    {formatJoinedDate(profile.createdAt)}
                  </p>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <Link to={`${profileBase}/edit`} className="btn-secondary">Chỉnh sửa hồ sơ</Link>
              <Link to={`${profileBase}/change-password`} className="btn-ghost">Đổi mật khẩu</Link>
            </div>
          </>
        )}
      </div>
    </RoleLayout>
  );
}

// ── SCR-11: Edit Profile ───────────────────────────────────────────────────────
export function EditProfilePage() {
  const navigate = useNavigate();
  const role = useAuthStore(s => s.role);
  const updateAuthProfile = useAuthStore(s => s.updateProfile);
  const profileBase = getProfileBase(role);

  const [form, setForm] = useState({ fullName: '', phone: '', avatarUrl: '' });
  const [email, setEmail] = useState('');
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState('');
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setInitialLoading(true);
      setLoadError('');
      try {
        const data = await fetchMyProfile();
        if (cancelled) return;
        setForm({
          fullName: data.fullName ?? '',
          phone: data.phone ?? '',
          avatarUrl: data.avatarUrl ?? '',
        });
        setEmail(data.email ?? '');
      } catch {
        if (!cancelled) setLoadError('Không thể tải thông tin hồ sơ. Vui lòng thử lại.');
      } finally {
        if (!cancelled) setInitialLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [reloadKey]);

  function validate() {
    const e: Record<string, string> = {};
    if (!form.fullName.trim()) e.fullName = 'Họ tên không được để trống';
    else if (form.fullName.trim().length > 255) e.fullName = 'Họ tên tối đa 255 ký tự';

    const cleanedPhone = form.phone.replace(/[\s\-()]/g, '');
    if (cleanedPhone && !/^(\+84|0)[0-9]{9,10}$/.test(cleanedPhone)) {
      e.phone = 'Số điện thoại không hợp lệ (VD: 0901234567)';
    }

    const avatarTrim = form.avatarUrl.trim();
    if (avatarTrim) {
      if (avatarTrim.length > 512) e.avatarUrl = 'URL avatar tối đa 512 ký tự';
      else if (!/^https?:\/\/.+/i.test(avatarTrim)) {
        e.avatarUrl = 'URL phải bắt đầu bằng http:// hoặc https://';
      }
    }
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setErrors({});
    setSubmitError('');
    setLoading(true);
    try {
      const cleanedPhone = form.phone.replace(/[\s\-()]/g, '');
      const avatarTrim = form.avatarUrl.trim();
      const updated = await updateMyProfile({
        fullName: form.fullName.trim(),
        phone: cleanedPhone || null,
        avatarUrl: avatarTrim || null,
      });
      updateAuthProfile({
        fullName: updated.fullName,
        phone: updated.phone ?? '',
        avatarUrl: updated.avatarUrl ?? '',
      });
      setSuccess(true);
      setTimeout(() => navigate(profileBase), 1500);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } };
      const data = axiosErr?.response?.data;
      if (data?.errors && typeof data.errors === 'object') {
        setErrors(data.errors);
      }
      setSubmitError(data?.message ?? 'Cập nhật hồ sơ thất bại. Vui lòng thử lại.');
      setLoading(false);
    }
  }

  const avatarPreview = form.avatarUrl.trim() && /^https?:\/\/.+/i.test(form.avatarUrl.trim())
    ? form.avatarUrl.trim()
    : null;

  return (
    <RoleLayout>
      <div style={{ maxWidth: 560, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to={profileBase} className="text-primary" style={{ textDecoration: 'none' }}>Hồ sơ</Link>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600 }}>Chỉnh sửa</span>
        </div>
        <h1 className="heading-md" style={{ marginBottom: 24 }}>Chỉnh sửa hồ sơ</h1>

        {success && (
          <div style={{ marginBottom: 16 }}>
            <Alert variant="success" message="Cập nhật hồ sơ thành công! Đang chuyển về trang hồ sơ..." />
          </div>
        )}

        {initialLoading ? (
          <div className="card-lg" style={{ padding: 48, textAlign: 'center' }}>
            <p className="body-md text-charcoal">Đang tải hồ sơ...</p>
          </div>
        ) : loadError ? (
          <div>
            <Alert variant="error" message={loadError} />
            <button
              type="button"
              className="btn-primary btn-sm"
              style={{ marginTop: 16 }}
              onClick={() => { setLoadError(''); setReloadKey((k) => k + 1); }}
            >
              Thử lại
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="card-lg" style={{ padding: 32, boxShadow: '0 4px 16px rgba(32,32,32,0.06)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 28, paddingBottom: 24, borderBottom: '1px solid var(--hairline)' }}>
              <ProfileAvatar name={form.fullName || '?'} url={avatarPreview} size={72} fontSize={24} />
              <div>
                <button type="button" className="btn-outline btn-sm" disabled title="Tính năng upload sẽ có sau">
                  Tải ảnh lên
                </button>
                <p className="form-hint" style={{ marginTop: 4 }}>Tính năng upload sẽ có sau — dùng URL bên dưới</p>
              </div>
            </div>

            {submitError && (
              <div style={{ marginBottom: 16 }}>
                <Alert variant="error" message={submitError} />
              </div>
            )}

            <div style={{ marginBottom: 16 }}>
              <label className="form-label">Email</label>
              <input type="email" className="input" value={email} disabled />
              <p className="form-hint">Email không thể thay đổi</p>
            </div>

            <div style={{ marginBottom: 16 }}>
              <label className="form-label form-label-required" htmlFor="editName">Họ tên</label>
              <input
                id="editName"
                className={`input ${errors.fullName ? 'input-error' : ''}`}
                value={form.fullName}
                onChange={(e) => setForm((p) => ({ ...p, fullName: e.target.value }))}
              />
              {errors.fullName && <p className="form-error">{errors.fullName}</p>}
            </div>

            <div style={{ marginBottom: 16 }}>
              <label className="form-label" htmlFor="editPhone">Số điện thoại</label>
              <input
                id="editPhone"
                type="tel"
                className={`input ${errors.phone ? 'input-error' : ''}`}
                value={form.phone}
                onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))}
                placeholder="0901234567"
              />
              {errors.phone && <p className="form-error">{errors.phone}</p>}
            </div>

            <div style={{ marginBottom: 28 }}>
              <label className="form-label" htmlFor="editAvatar">URL ảnh đại diện</label>
              <input
                id="editAvatar"
                type="url"
                className={`input ${errors.avatarUrl ? 'input-error' : ''}`}
                value={form.avatarUrl}
                onChange={(e) => setForm((p) => ({ ...p, avatarUrl: e.target.value }))}
                placeholder="https://..."
              />
              {errors.avatarUrl && <p className="form-error">{errors.avatarUrl}</p>}
            </div>

            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <button type="submit" className="btn-primary" disabled={loading || success}>
                {loading ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
              <Link to={profileBase} className="btn-ghost">Hủy</Link>
            </div>
          </form>
        )}
      </div>
    </RoleLayout>
  );
}

// ── SCR-12: Change Password ────────────────────────────────────────────────────
export function ChangePasswordPage() {
  const navigate = useNavigate();
  const role = useAuthStore(s => s.role);
  const logout = useAuthStore(s => s.logout);
  const profileBase = getProfileBase(role);

  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [show, setShow] = useState({ current: false, new: false, confirm: false });
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState('');
  const [success, setSuccess] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!form.currentPassword) e.currentPassword = 'Mật khẩu hiện tại không được để trống';
    if (!form.newPassword) e.newPassword = 'Mật khẩu mới không được để trống';
    else if (form.newPassword.length < 8) e.newPassword = 'Mật khẩu tối thiểu 8 ký tự';
    if (!form.confirmPassword) e.confirmPassword = 'Xác nhận mật khẩu không được để trống';
    else if (form.newPassword !== form.confirmPassword) e.confirmPassword = 'Mật khẩu xác nhận không khớp';
    if (form.currentPassword && form.newPassword && form.currentPassword === form.newPassword) {
      e.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại';
    }
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setErrors({});
    setSubmitError('');
    setLoading(true);
    try {
      await changeMyPassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword,
      });
      setSuccess(true);
      setTimeout(() => {
        logout();
        navigate('/login');
      }, 1500);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const msg = axiosErr?.response?.data?.message ?? 'Đổi mật khẩu thất bại. Kiểm tra lại mật khẩu hiện tại.';
      setSubmitError(msg);
      setLoading(false);
    }
  }

  function EyeBtn({ field }: { field: keyof typeof show }) {
    return (
      <button type="button" onClick={() => setShow(p => ({ ...p, [field]: !p[field] }))}
        style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ash)' }}>
        {show[field]
          ? <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
          : <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
        }
      </button>
    );
  }

  return (
    <RoleLayout>
      <div style={{ maxWidth: 480, margin: '0 auto' }}>
        <div className="flex items-center gap-2 body-sm text-charcoal" style={{ marginBottom: 20 }}>
          <Link to={profileBase} className="text-primary" style={{ textDecoration: 'none' }}>Hồ sơ</Link>
          <span>›</span>
          <span className="text-ink" style={{ fontWeight: 600 }}>Đổi mật khẩu</span>
        </div>
        <h1 className="heading-md" style={{ marginBottom: 24 }}>Đổi mật khẩu</h1>

        {success && (
          <div style={{ marginBottom: 16 }}>
            <Alert variant="success" message="Đổi mật khẩu thành công! Các phiên đăng nhập khác đã bị đăng xuất. Đang chuyển đến trang đăng nhập..." />
          </div>
        )}

        <form onSubmit={handleSubmit} className="card-lg" style={{ padding: 32, boxShadow: '0 4px 16px rgba(32,32,32,0.06)' }}>
          {submitError && (
            <div style={{ marginBottom: 16 }}>
              <Alert variant="error" message={submitError} />
            </div>
          )}

          {[
            { id: 'curr', label: 'Mật khẩu hiện tại', field: 'currentPassword', showKey: 'current' as keyof typeof show, placeholder: 'Nhập mật khẩu hiện tại' },
            { id: 'new',  label: 'Mật khẩu mới',     field: 'newPassword',     showKey: 'new'     as keyof typeof show, placeholder: 'Nhập mật khẩu mới (tối thiểu 8 ký tự)' },
            { id: 'conf', label: 'Xác nhận mật khẩu mới', field: 'confirmPassword', showKey: 'confirm' as keyof typeof show, placeholder: 'Nhập lại mật khẩu mới' },
          ].map(({ id, label, field, showKey, placeholder }) => (
            <div key={id} style={{ marginBottom: 20 }}>
              <label className="form-label form-label-required" htmlFor={id}>{label}</label>
              <div style={{ position: 'relative' }}>
                <input id={id} type={show[showKey] ? 'text' : 'password'}
                  className={`input ${errors[field] ? 'input-error' : ''}`}
                  placeholder={placeholder}
                  value={(form as Record<string, string>)[field]}
                  onChange={e => setForm(p => ({ ...p, [field]: e.target.value }))}
                  style={{ paddingRight: 50 }}
                  autoComplete={field === 'currentPassword' ? 'current-password' : 'new-password'}
                />
                <EyeBtn field={showKey} />
              </div>
              {errors[field] && <p className="form-error">{errors[field]}</p>}
            </div>
          ))}

          <div style={{ display: 'flex', gap: 12 }}>
            <button type="submit" className="btn-primary" disabled={loading || success}>
              {loading ? 'Đang xử lý...' : 'Cập nhật mật khẩu'}
            </button>
            <Link to={profileBase} className="btn-ghost">Hủy</Link>
          </div>
        </form>
      </div>
    </RoleLayout>
  );
}
