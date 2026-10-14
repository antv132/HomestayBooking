// SCR-12: Change Password
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import CustomerLayout from '../../layouts/CustomerLayout';
import ManagerLayout from '../../layouts/ManagerLayout';
import EmployeeLayout from '../../layouts/EmployeeLayout';
import AdminLayout from '../../layouts/AdminLayout';
import Alert from '../../components/ui/Alert';
import { useAuthStore } from '../../store/authStore';
import { changeMyPassword } from '../../api/usersApi';

function getProfileBase(role: string | null): string {
  switch (role) {
    case 'MANAGER': return '/manager/profile';
    case 'EMPLOYEE': return '/employee/profile';
    case 'ADMIN': return '/admin/profile';
    default: return '/customer/profile';
  }
}

function RoleLayout({ children }: { children: React.ReactNode }) {
  const role = useAuthStore(s => s.role);
  if (role === 'MANAGER') return <ManagerLayout>{children}</ManagerLayout>;
  if (role === 'EMPLOYEE') return <EmployeeLayout>{children}</EmployeeLayout>;
  if (role === 'ADMIN') return <AdminLayout>{children}</AdminLayout>;
  return <CustomerLayout>{children}</CustomerLayout>;
}

export default function ChangePasswordPage() {
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
    else if (!/[A-Za-z]/.test(form.newPassword)) e.newPassword = 'Mật khẩu phải có ít nhất 1 chữ cái';
    else if (!/[0-9]/.test(form.newPassword)) e.newPassword = 'Mật khẩu phải có ít nhất 1 chữ số';
    if (!form.confirmPassword) e.confirmPassword = 'Xác nhận mật khẩu không được để trống';
    else if (form.newPassword !== form.confirmPassword) e.confirmPassword = 'Mật khẩu xác nhận không khớp';
    if (form.currentPassword && form.newPassword && form.currentPassword === form.newPassword)
      e.newPassword = 'Mật khẩu mới phải khác mật khẩu hiện tại';
    return e;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length) { setErrors(errs); return; }
    setErrors({}); setSubmitError(''); setLoading(true);
    try {
      await changeMyPassword({ currentPassword: form.currentPassword, newPassword: form.newPassword, confirmPassword: form.confirmPassword });
      setSuccess(true);
      setTimeout(() => { logout(); navigate('/login'); }, 1500);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setSubmitError(axiosErr?.response?.data?.message ?? 'Đổi mật khẩu thất bại.');
      setLoading(false);
    }
  }

  function EyeBtn({ field }: { field: keyof typeof show }) {
    return (
      <button type="button" onClick={() => setShow(p => ({ ...p, [field]: !p[field] }))}
        style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ash)' }}>
        {show[field]
          ? <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
          : <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>}
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
        {success && (<div style={{ marginBottom: 16 }}><Alert variant="success" message="Đổi mật khẩu thành công! Đang chuyển đến trang đăng nhập..." /></div>)}
        <form onSubmit={handleSubmit} className="card-lg" style={{ padding: 32, boxShadow: '0 4px 16px rgba(32,32,32,0.06)' }}>
          {submitError && (<div style={{ marginBottom: 16 }}><Alert variant="error" message={submitError} /></div>)}
          {[
            { id: 'curr', label: 'Mật khẩu hiện tại', field: 'currentPassword', showKey: 'current' as keyof typeof show, placeholder: 'Nhập mật khẩu hiện tại' },
            { id: 'new', label: 'Mật khẩu mới', field: 'newPassword', showKey: 'new' as keyof typeof show, placeholder: 'Tối thiểu 8 ký tự, có chữ và số' },
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
                  autoComplete={field === 'currentPassword' ? 'current-password' : 'new-password'} />
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