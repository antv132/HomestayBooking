import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import Alert from '../../components/ui/Alert';

function isValidEmail(value: string): boolean {
  return /^\S+@\S+\.\S+$/.test(value);
}

function PasswordStrength({ password }: { password: string }) {
  const score = [/.{8,}/, /[A-Za-z]/, /[0-9]/, /[^A-Za-z0-9]/].filter(r => r.test(password)).length;
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const colors = ['', '#dc2626', '#f59e0b', '#0D9488', '#2b9a66'];
  if (!password) return null;
  return (
    <div style={{ marginTop: 6 }}>
      <div style={{ display: 'flex', gap: 3, marginBottom: 4 }}>
        {[1, 2, 3, 4].map(i => (
          <div key={i} style={{ flex: 1, height: 3, borderRadius: 9999, background: i <= score ? colors[score] : 'var(--hairline)', transition: 'background 0.3s' }} />
        ))}
      </div>
      <p style={{ fontSize: 11, color: colors[score], fontWeight: 600 }}>{labels[score]}</p>
    </div>
  );
}

const CHECKLIST = [
  { label: 'At least 8 characters', test: (p: string) => p.length >= 8 },
  { label: 'One letter', test: (p: string) => /[A-Za-z]/.test(p) },
  { label: 'One number', test: (p: string) => /[0-9]/.test(p) },
];

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const email = (params.get('email') ?? '').trim();
  const otpCode = (params.get('otpCode') ?? params.get('token') ?? '').trim();
  const hasValidParams = isValidEmail(email) && otpCode.length === 6;

  const [form, setForm] = useState({ password: '', confirm: '' });
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForgotLink, setShowForgotLink] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ password?: string; confirm?: string }>({});
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!hasValidParams) {
      navigate('/forgot-password', { replace: true });
    }
  }, [hasValidParams, navigate]);

  useEffect(() => {
    if (!done) return;
    const timer = setTimeout(() => navigate('/login', { replace: true }), 2500);
    return () => clearTimeout(timer);
  }, [done, navigate]);

  function validate(): boolean {
    const errs: { password?: string; confirm?: string } = {};
    if (!form.password) errs.password = 'Password is required';
    else if (form.password.length < 8) errs.password = 'Password must be at least 8 characters';
    else if (!/[A-Za-z]/.test(form.password)) errs.password = 'Password must contain at least 1 letter';
    else if (!/[0-9]/.test(form.password)) errs.password = 'Password must contain at least 1 number';
    if (!form.confirm) errs.confirm = 'Please confirm your password';
    else if (form.password !== form.confirm) errs.confirm = 'Passwords do not match';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleApiError(err: unknown) {
    const axiosError = err as {
      response?: { status?: number; data?: { message?: string } };
    };

    if (!axiosError.response) {
      setError('Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.');
      setShowForgotLink(false);
      return;
    }

    const status = axiosError.response.status;
    const msg = axiosError.response.data?.message ?? 'Invalid or expired verification code. Please request a new one.';
    const isOtpIssue = status === 410
      || (status === 400 && (msg.toLowerCase().includes('otp') || msg.toLowerCase().includes('mã')));

    if (isOtpIssue) {
      setError(status === 410
        ? `${msg} Please request a new verification code.`
        : msg);
      setShowForgotLink(true);
      return;
    }

    setError(msg);
    setShowForgotLink(false);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setShowForgotLink(false);
    if (!validate()) return;

    setLoading(true);
    try {
      const res = await authApi.resetPassword(email, otpCode, form.password);
      if (!res.success) {
        setError(res.message || 'Invalid or expired verification code. Please request a new one.');
        return;
      }
      setDone(true);
    } catch (err: unknown) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  }

  if (!hasValidParams) return null;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--canvas)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '24px 16px' }}>
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, textDecoration: 'none', marginBottom: 32 }}>
        <div style={{ width: 36, height: 36, background: 'var(--primary)', borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95" />
            <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6" />
          </svg>
        </div>
        <span className="font-display" style={{ fontWeight: 700, fontSize: 18, color: 'var(--ink)', letterSpacing: '-0.5px' }}>
          Homestay<span style={{ color: 'var(--primary)' }}>&</span>Resort
        </span>
      </Link>

      <div className="card-lg animate-fade-up" style={{ width: '100%', maxWidth: 440, padding: 40 }}>
        <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'rgba(15, 118, 110, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </svg>
        </div>

        <h1 className="heading-md" style={{ marginBottom: 8 }}>Set new password</h1>
        <p className="body-md text-charcoal" style={{ marginBottom: 24 }}>Choose a strong password for your account.</p>

        {done ? (
          <Alert variant="success" message="Password reset successfully. Redirecting to login..." />
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            {error && (
              <div style={{ marginBottom: 20 }}>
                <Alert variant="error" message={error} />
                {showForgotLink && (
                  <p className="body-sm" style={{ marginTop: 12 }}>
                    <Link to="/forgot-password" className="text-primary" style={{ fontWeight: 600, textDecoration: 'none' }}>
                      Request a new verification code →
                    </Link>
                  </p>
                )}
              </div>
            )}

            <div style={{ marginBottom: 16 }}>
              <label className="form-label form-label-required" htmlFor="reset-pw">New Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  id="reset-pw"
                  type={showPw ? 'text' : 'password'}
                  className={`input ${fieldErrors.password ? 'input-error' : ''}`}
                  placeholder="Enter new password"
                  value={form.password}
                  onChange={e => {
                    setForm(p => ({ ...p, password: e.target.value }));
                    setFieldErrors(p => ({ ...p, password: undefined }));
                    setError(null);
                  }}
                  autoComplete="new-password"
                  disabled={loading}
                  style={{ paddingRight: 50 }}
                />
                <button type="button" onClick={() => setShowPw(!showPw)} aria-label={showPw ? 'Hide' : 'Show'}
                  style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ash)', padding: 4 }}>
                  {showPw
                    ? <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
                    : <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
                  }
                </button>
              </div>
              <PasswordStrength password={form.password} />
              {fieldErrors.password && (
                <p className="form-error" style={{ marginTop: 6 }}>{fieldErrors.password}</p>
              )}
            </div>

            <div style={{ marginBottom: 20 }}>
              <label className="form-label form-label-required" htmlFor="reset-confirm">Confirm Password</label>
              <div style={{ position: 'relative' }}>
                <input
                  id="reset-confirm"
                  type={showConfirm ? 'text' : 'password'}
                  className={`input ${fieldErrors.confirm ? 'input-error' : ''}`}
                  placeholder="Repeat new password"
                  value={form.confirm}
                  onChange={e => {
                    setForm(p => ({ ...p, confirm: e.target.value }));
                    setFieldErrors(p => ({ ...p, confirm: undefined }));
                    setError(null);
                  }}
                  autoComplete="new-password"
                  disabled={loading}
                  style={{ paddingRight: 50 }}
                />
                <button type="button" onClick={() => setShowConfirm(!showConfirm)} aria-label={showConfirm ? 'Hide' : 'Show'}
                  style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ash)', padding: 4 }}>
                  {showConfirm
                    ? <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" /></svg>
                    : <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
                  }
                </button>
              </div>
              {fieldErrors.confirm && (
                <p className="form-error" style={{ marginTop: 6 }}>{fieldErrors.confirm}</p>
              )}
            </div>

            <div style={{ marginBottom: 24, display: 'flex', flexDirection: 'column', gap: 6 }}>
              {CHECKLIST.map(item => {
                const ok = item.test(form.password);
                return (
                  <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
                    <div style={{ width: 16, height: 16, borderRadius: '50%', background: ok ? '#dcfce7' : 'var(--surface-bone)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      {ok && <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="3"><polyline points="20,6 9,17 4,12" /></svg>}
                    </div>
                    <span style={{ color: ok ? 'var(--success)' : 'var(--charcoal)' }}>{item.label}</span>
                  </div>
                );
              })}
            </div>

            <button type="submit" className="btn-primary" style={{ width: '100%', marginBottom: 12 }} disabled={loading}>
              {loading ? 'Resetting...' : 'Reset Password'}
            </button>

            <Link to="/login" className="btn-ghost" style={{ width: '100%', justifyContent: 'center', display: 'flex' }}>
              ← Back to Login
            </Link>
          </form>
        )}

        {done && (
          <Link to="/login" className="btn-primary" style={{ display: 'inline-flex', width: '100%', justifyContent: 'center', marginTop: 20 }}>
            Go to Login
          </Link>
        )}
      </div>
    </div>
  );
}
