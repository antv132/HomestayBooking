import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import Alert from '../../components/ui/Alert';
import Checkbox from '../../components/ui/Checkbox';

function PasswordStrength({ password }: { password: string }) {
  const score = [/.{8,}/, /[A-Za-z]/, /[0-9]/, /[^A-Za-z0-9]/].filter((r) => r.test(password)).length;
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const colors = ['', '#dc2626', '#f59e0b', '#2563eb', '#2b9a66'];
  if (!password) return null;
  return (
    <div style={{ marginTop: 6 }}>
      <div style={{ display: 'flex', gap: 3, marginBottom: 4 }}>
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            style={{
              flex: 1,
              height: 3,
              borderRadius: 9999,
              background: i <= score ? colors[score] : 'var(--hairline)',
              transition: 'background 0.3s',
            }}
          />
        ))}
      </div>
      <p style={{ fontSize: 11, color: colors[score], fontWeight: 600 }}>{labels[score]}</p>
    </div>
  );
}

function ShowPasswordButton({ show, onToggle }: { show: boolean; onToggle: () => void }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={show ? 'Hide password' : 'Show password'}
      style={{
        position: 'absolute',
        right: 14,
        top: '50%',
        transform: 'translateY(-50%)',
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        color: 'var(--ash)',
        padding: 4,
      }}
    >
      {show ? (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
          <line x1="1" y1="1" x2="23" y2="23" />
        </svg>
      ) : (
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      )}
    </button>
  );
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
    terms: false,
  });
  const [showPw, setShowPw] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function validate() {
    const e: Record<string, string> = {};
    const fullName = form.fullName.trim();

    if (!fullName) e.fullName = 'Full name is required';
    else if (fullName.length > 200) e.fullName = 'Name must be under 200 characters';

    if (!form.email.trim()) e.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = 'Invalid email format';

    const phone = form.phone.trim();
    if (!phone) e.phone = 'Phone number is required';
    else if (!/^(\+84|0)[0-9]{9,10}$/.test(phone)) {
      e.phone = 'Invalid phone number (e.g. 0912345678 or +84912345678)';
    }

    if (!form.password) e.password = 'Password is required';
    else if (form.password.length < 8) e.password = 'Password must be at least 8 characters';
    else if (!/[A-Za-z]/.test(form.password)) e.password = 'Password must contain at least 1 letter';
    else if (!/[0-9]/.test(form.password)) e.password = 'Password must contain at least 1 number';

    if (!form.confirmPassword) e.confirmPassword = 'Please confirm your password';
    else if (form.password !== form.confirmPassword) e.confirmPassword = 'Passwords do not match';

    if (!form.terms) e.terms = 'You must accept the terms';

    return e;
  }

  function handleApiError(err: unknown) {
    const axiosError = err as {
      response?: {
        data?: {
          message?: string;
          data?: Record<string, string>;
        };
      };
    };

    if (!axiosError.response) {
      setAlert('Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.');
      return;
    }

    const responseData = axiosError.response.data;
    if (responseData?.data && Object.keys(responseData.data).length > 0) {
      setFieldErrors(responseData.data);
      return;
    }

    setAlert(responseData?.message || 'Registration failed. Please try again.');
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAlert(null);

    const errs = validate();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      return;
    }

    setFieldErrors({});
    setLoading(true);

    const email = form.email.trim();
    const phone = form.phone.trim();

    try {
      const res = await authApi.register({
        fullName: form.fullName.trim(),
        email,
        password: form.password,
        phone,
      });

      if (!res.success) {
        setAlert(res.message || 'Registration failed. Please try again.');
        return;
      }

      navigate('/verify-email?email=' + encodeURIComponent(email), { replace: true });
    } catch (err: unknown) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  }

  const setField = (key: keyof typeof form, val: string | boolean) => {
    setForm((p) => ({ ...p, [key]: val }));
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'var(--canvas)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px 16px',
      }}
    >
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, textDecoration: 'none', marginBottom: 28 }}>
        <div
          style={{
            width: 34,
            height: 34,
            background: 'var(--primary)',
            borderRadius: 9,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95" />
            <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6" />
          </svg>
        </div>
        <span className="font-display" style={{ fontWeight: 700, fontSize: 18, color: 'var(--ink)', letterSpacing: '-0.5px' }}>
          Homestay<span style={{ color: 'var(--primary)' }}>&</span>Resort
        </span>
      </Link>

      <div className="card-lg animate-fade-up" style={{ width: '100%', maxWidth: 520, padding: 40 }}>
        <h1 className="display-md" style={{ marginBottom: 6, textAlign: 'center' }}>
          Create your account
        </h1>
        <p className="body-md text-charcoal" style={{ textAlign: 'center', marginBottom: 28 }}>
          Join thousands of satisfied guests
        </p>

        {alert && (
          <div style={{ marginBottom: 20 }}>
            <Alert variant="error" message={alert} />
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ marginBottom: 16 }}>
            <label className="form-label form-label-required" htmlFor="reg-name">
              Full Name
            </label>
            <input
              id="reg-name"
              className={`input ${fieldErrors.fullName ? 'input-error' : ''}`}
              placeholder="Nguyễn Văn An"
              value={form.fullName}
              onChange={(e) => setField('fullName', e.target.value)}
              disabled={loading}
              maxLength={200}
            />
            {fieldErrors.fullName && <p className="form-error">{fieldErrors.fullName}</p>}
          </div>

          <div style={{ marginBottom: 16 }}>
            <label className="form-label form-label-required" htmlFor="reg-email">
              Email
            </label>
            <input
              id="reg-email"
              type="email"
              className={`input ${fieldErrors.email ? 'input-error' : ''}`}
              placeholder="you@example.com"
              value={form.email}
              onChange={(e) => setField('email', e.target.value)}
              autoComplete="email"
              disabled={loading}
            />
            {fieldErrors.email && <p className="form-error">{fieldErrors.email}</p>}
          </div>

          <div style={{ marginBottom: 16 }}>
            <label className="form-label form-label-required" htmlFor="reg-phone">
              Phone Number
            </label>
            <input
              id="reg-phone"
              type="tel"
              className={`input ${fieldErrors.phone ? 'input-error' : ''}`}
              placeholder="0901234567"
              value={form.phone}
              onChange={(e) => setField('phone', e.target.value)}
              autoComplete="tel"
              disabled={loading}
            />
            {fieldErrors.phone && <p className="form-error">{fieldErrors.phone}</p>}
          </div>

          <div style={{ marginBottom: 16 }}>
            <label className="form-label form-label-required" htmlFor="reg-pw">
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <input
                id="reg-pw"
                type={showPw ? 'text' : 'password'}
                className={`input ${fieldErrors.password ? 'input-error' : ''}`}
                placeholder="At least 8 characters"
                value={form.password}
                onChange={(e) => setField('password', e.target.value)}
                autoComplete="new-password"
                style={{ paddingRight: 50 }}
                disabled={loading}
              />
              <ShowPasswordButton show={showPw} onToggle={() => setShowPw(!showPw)} />
            </div>
            <PasswordStrength password={form.password} />
            {fieldErrors.password && <p className="form-error">{fieldErrors.password}</p>}
          </div>

          <div style={{ marginBottom: 20 }}>
            <label className="form-label form-label-required" htmlFor="reg-confirm">
              Confirm Password
            </label>
            <div style={{ position: 'relative' }}>
              <input
                id="reg-confirm"
                type={showConfirm ? 'text' : 'password'}
                className={`input ${fieldErrors.confirmPassword ? 'input-error' : ''}`}
                placeholder="Repeat your password"
                value={form.confirmPassword}
                onChange={(e) => setField('confirmPassword', e.target.value)}
                autoComplete="new-password"
                style={{ paddingRight: 50 }}
                disabled={loading}
              />
              <ShowPasswordButton show={showConfirm} onToggle={() => setShowConfirm(!showConfirm)} />
            </div>
            {fieldErrors.confirmPassword && <p className="form-error">{fieldErrors.confirmPassword}</p>}
          </div>

          <div style={{ marginBottom: 24 }}>
            <Checkbox
              id="reg-terms"
              checked={form.terms}
              onChange={(e) => setField('terms', e.target.checked)}
              disabled={loading}
              error={fieldErrors.terms}
              label={
                <span className="body-sm text-charcoal">
                  I agree to the{' '}
                  <Link to="/about" className="text-primary" style={{ fontWeight: 600, textDecoration: 'none' }}>
                    Terms of Service
                  </Link>{' '}
                  and{' '}
                  <Link to="/about" className="text-primary" style={{ fontWeight: 600, textDecoration: 'none' }}>
                    Privacy Policy
                  </Link>
                </span>
              }
            />
          </div>

          <button type="submit" className="btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? 'Creating account...' : 'Create Account'}
          </button>
        </form>

        <p className="body-sm text-charcoal" style={{ textAlign: 'center', marginTop: 24 }}>
          Already have an account?{' '}
          <Link to="/login" className="text-primary" style={{ fontWeight: 600, textDecoration: 'none' }}>
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
