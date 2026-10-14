import { useEffect, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { GoogleLogin, GoogleOAuthProvider } from '@react-oauth/google';
import { useAuthStore, getRoleDashboardPath } from '../../store/authStore';
import { authApi, type AuthResponse, isAuthSession } from '../../api/authApi';
import Alert from '../../components/ui/Alert';
import Checkbox from '../../components/ui/Checkbox';

const REMEMBER_EMAIL_KEY = 'rememberedLoginEmail';

interface LoginForm {
  email: string;
  password: string;
}

type AlertKind = 'error' | 'warning';

interface LoginAlert {
  kind: AlertKind;
  message: string;
  verifyEmail?: string;
}

function isSuspendedMessage(message: string): boolean {
  const lower = message.toLowerCase();
  return lower.includes('tạm khóa') || lower.includes('suspended');
}

function isGoogleOnlyMessage(message: string): boolean {
  const lower = message.toLowerCase();
  return lower.includes('google');
}

function LoginPageContent() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuthStore();

  const [form, setForm] = useState<LoginForm>({ email: '', password: '' });
  const [rememberMe, setRememberMe] = useState(false);
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [alert, setAlert] = useState<LoginAlert | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});

  const params = new URLSearchParams(location.search);
  const redirectPath = params.get('redirect');
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;

  useEffect(() => {
    const savedEmail = localStorage.getItem(REMEMBER_EMAIL_KEY);
    if (savedEmail) {
      setForm((prev) => ({ ...prev, email: savedEmail }));
      setRememberMe(true);
    }
  }, []);

  function validate(): boolean {
    const errs: typeof fieldErrors = {};
    if (!form.email.trim()) errs.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) errs.email = 'Invalid email format';
    if (!form.password) errs.password = 'Password is required';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function persistRememberMe(email: string) {
    if (rememberMe) {
      localStorage.setItem(REMEMBER_EMAIL_KEY, email);
    } else {
      localStorage.removeItem(REMEMBER_EMAIL_KEY);
    }
  }

  function navigateAfterLogin(role: string) {
    if (redirectPath) {
      navigate(redirectPath, { replace: true });
    } else {
      navigate(getRoleDashboardPath(role), { replace: true });
    }
  }

  function handleAuthSuccess(data: AuthResponse) {
    if (!isAuthSession(data)) {
      setAlert({ kind: 'error', message: 'Login response incomplete. Please try again.' });
      return;
    }
    login(data);
    persistRememberMe(form.email.trim());
    navigateAfterLogin(data.user.role);
  }

  function handleApiError(err: unknown) {
    const axiosError = err as {
      response?: {
        status?: number;
        data?: {
          message?: string;
          data?: { errorCode?: string; email?: string };
        };
      };
      code?: string;
    };

    if (!axiosError.response) {
      setAlert({
        kind: 'error',
        message: 'Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.',
      });
      return;
    }

    const status = axiosError.response.status;
    const body = axiosError.response.data;
    const msg = body?.message ?? 'Invalid email or password';

    if (status === 403 && body?.data?.errorCode === 'ACCOUNT_INACTIVE') {
      setAlert({
        kind: 'warning',
        message: 'Please verify your email first. We sent a new code to your inbox.',
        verifyEmail: body.data.email ?? form.email.trim(),
      });
      return;
    }

    if (isSuspendedMessage(msg)) {
      setAlert({ kind: 'error', message: 'Your account has been suspended. Please contact support.' });
      return;
    }

    if (isGoogleOnlyMessage(msg)) {
      setAlert({
        kind: 'error',
        message: 'This account uses Google sign-in. Please use the Google button below.',
      });
      return;
    }

    setAlert({ kind: 'error', message: msg || 'Invalid email or password' });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAlert(null);
    if (!validate()) return;

    setLoading(true);
    try {
      const res = await authApi.login(form.email.trim(), form.password);

      if (!res.success || !res.data) {
        setAlert({ kind: 'error', message: res.message ?? 'Invalid email or password' });
        return;
      }

      handleAuthSuccess(res.data);
    } catch (err: unknown) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleGoogleSuccess(idToken: string) {
    setAlert(null);
    setLoading(true);
    try {
      const res = await authApi.loginWithGoogle(idToken);
      if (!res.success || !res.data) {
        setAlert({ kind: 'error', message: res.message ?? 'Google sign-in failed' });
        return;
      }
      if (res.data.pendingVerification) {
        const email = res.data.email ?? '';
        if (res.data.verificationToken) {
          sessionStorage.setItem('googleLinkToken', res.data.verificationToken);
        }
        navigate(
          `/verify-email?email=${encodeURIComponent(email)}&purpose=google-link`,
          { replace: true },
        );
        return;
      }
      if (!isAuthSession(res.data)) {
        setAlert({ kind: 'error', message: 'Google sign-in response incomplete.' });
        return;
      }
      login(res.data);
      navigateAfterLogin(res.data.user.role);
    } catch (err: unknown) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  }

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
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 10, textDecoration: 'none', marginBottom: 32 }}>
        <div
          style={{
            width: 36,
            height: 36,
            background: 'var(--primary)',
            borderRadius: 10,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" fill="white" fillOpacity="0.95" />
            <polyline points="9,22 9,12 15,12 15,22" fill="white" fillOpacity="0.6" />
          </svg>
        </div>
        <span className="font-display" style={{ fontWeight: 700, fontSize: 18, color: 'var(--ink)', letterSpacing: '-0.5px' }}>
          Homestay<span style={{ color: 'var(--primary)' }}>&</span>Resort
        </span>
      </Link>

      <div className="card-lg animate-fade-up" style={{ width: '100%', maxWidth: 480, padding: 40 }}>
        <h1 className="display-md" style={{ marginBottom: 6, textAlign: 'center' }}>
          Welcome back
        </h1>
        <p className="body-md text-charcoal" style={{ textAlign: 'center', marginBottom: 28 }}>
          Sign in to your account
        </p>

        {alert && (
          <div style={{ marginBottom: 20 }}>
            <Alert variant={alert.kind} message={alert.message} />
            {alert.kind === 'warning' && alert.verifyEmail && (
              <div style={{ marginTop: 12, textAlign: 'center' }}>
                <Link
                  to={`/verify-email?email=${encodeURIComponent(alert.verifyEmail)}`}
                  className="btn-primary btn-sm"
                  style={{ display: 'inline-flex', textDecoration: 'none' }}
                >
                  Verify email now
                </Link>
              </div>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div style={{ marginBottom: 18 }}>
            <label className="form-label" htmlFor="login-email">
              Email
            </label>
            <input
              id="login-email"
              type="email"
              className={`input ${fieldErrors.email ? 'input-error' : ''}`}
              placeholder="you@example.com"
              value={form.email}
              onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
              autoComplete="email"
              disabled={loading}
            />
            {fieldErrors.email && <p className="form-error">{fieldErrors.email}</p>}
          </div>

          <div style={{ marginBottom: 20 }}>
            <label className="form-label" htmlFor="login-password">
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <input
                id="login-password"
                type={showPw ? 'text' : 'password'}
                className={`input ${fieldErrors.password ? 'input-error' : ''}`}
                placeholder="Enter your password"
                value={form.password}
                onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
                autoComplete="current-password"
                style={{ paddingRight: 50 }}
                disabled={loading}
              />
              <button
                type="button"
                onClick={() => setShowPw(!showPw)}
                aria-label={showPw ? 'Hide password' : 'Show password'}
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
                {showPw ? (
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
            </div>
            {fieldErrors.password && <p className="form-error">{fieldErrors.password}</p>}
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              marginBottom: 24,
              flexWrap: 'wrap',
            }}
          >
            <Checkbox
              id="login-remember"
              label="Remember me"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
              disabled={loading}
            />
            <Link to="/forgot-password" className="body-sm text-primary" style={{ textDecoration: 'none', fontWeight: 600 }}>
              Forgot password?
            </Link>
          </div>

          <button type="submit" id="login-submit-btn" className="btn-primary" style={{ width: '100%' }} disabled={loading}>
            {loading ? (
              <>
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  style={{ animation: 'spin 0.8s linear infinite' }}
                >
                  <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                </svg>
                Signing in...
              </>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        {googleClientId && (
          <>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                margin: '24px 0',
                color: 'var(--ash)',
              }}
            >
              <div style={{ flex: 1, height: 1, background: 'var(--hairline)' }} />
              <span className="caption">or</span>
              <div style={{ flex: 1, height: 1, background: 'var(--hairline)' }} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <GoogleLogin
                onSuccess={(res) => {
                  if (res.credential) void handleGoogleSuccess(res.credential);
                  else setAlert({ kind: 'error', message: 'Google sign-in failed' });
                }}
                onError={() => setAlert({ kind: 'error', message: 'Google sign-in was cancelled or failed' })}
                theme="outline"
                size="large"
                text="signin_with"
                shape="pill"
                width={400}
              />
            </div>
          </>
        )}

        <p className="body-sm text-charcoal" style={{ textAlign: 'center', marginTop: 24 }}>
          Don&apos;t have an account?{' '}
          <Link to="/register" className="text-primary" style={{ fontWeight: 600, textDecoration: 'none' }}>
            Create account
          </Link>
        </p>
      </div>

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

export default function LoginPage() {
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;

  if (googleClientId) {
    return (
      <GoogleOAuthProvider clientId={googleClientId}>
        <LoginPageContent />
      </GoogleOAuthProvider>
    );
  }

  return <LoginPageContent />;
}
