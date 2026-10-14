import { useState, useRef, useCallback, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import Alert from '../../components/ui/Alert';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN_SEC = 60;

type PageAlert = { variant: 'error' | 'success' | 'warning'; message: string };

function isGoogleOnlyMessage(message: string): boolean {
  const lower = message.toLowerCase();
  return lower.includes('google');
}

/* ── Password strength bar ── */
function PasswordStrength({ password }: { password: string }) {
  const score = [/.{8,}/, /[A-Z]/, /[0-9]/, /[^A-Za-z0-9]/].filter(r => r.test(password)).length;
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const colors = ['', '#dc2626', '#f59e0b', '#2563eb', '#2b9a66'];
  if (!password) return null;
  return (
    <div style={{ marginTop: 6 }}>
      <div style={{ display: 'flex', gap: 3, marginBottom: 4 }}>
        {[1, 2, 3, 4].map(i => (
          <div
            key={i}
            style={{
              flex: 1, height: 3, borderRadius: 9999,
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

/* ── Step types ── */
type Step = 'email' | 'otp' | 'newPassword' | 'done';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();

  /* ── State ── */
  const [step, setStep]         = useState<Step>('email');
  const [email, setEmail]       = useState('');
  const [otp, setOtp]           = useState(['', '', '', '', '', '']);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm]   = useState('');
  const [showPw, setShowPw]     = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState<string | null>(null);
  const [alert, setAlert]       = useState<PageAlert | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string; confirm?: string }>({});
  const [emailSentSuccess, setEmailSentSuccess] = useState(false);
  const [inlineOtpError, setInlineOtpError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(RESEND_COOLDOWN_SEC);
  const [canResend, setCanResend] = useState(false);

  const otpRefs = useRef<(HTMLInputElement | null)[]>([]);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const startCountdown = useCallback(() => {
    if (countdownRef.current) clearInterval(countdownRef.current);
    setCanResend(false);
    setCountdown(RESEND_COOLDOWN_SEC);

    countdownRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          if (countdownRef.current) clearInterval(countdownRef.current);
          setCanResend(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  useEffect(() => () => {
    if (countdownRef.current) clearInterval(countdownRef.current);
  }, []);

  function validateEmailForm(): boolean {
    const errs: { email?: string } = {};
    const trimmed = email.trim();
    if (!trimmed) errs.email = 'Email is required';
    else if (!/^\S+@\S+\.\S+$/.test(trimmed)) errs.email = 'Invalid email format';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleForgotApiError(err: unknown) {
    const axiosError = err as {
      response?: { status?: number; data?: { message?: string } };
    };

    if (!axiosError.response) {
      setAlert({
        variant: 'error',
        message: 'Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.',
      });
      return;
    }

    const status = axiosError.response.status;
    const msg = axiosError.response.data?.message ?? 'Something went wrong. Please try again.';

    if (status === 404) {
      setAlert({ variant: 'error', message: msg });
      return;
    }

    if (status === 400 && isGoogleOnlyMessage(msg)) {
      setAlert({
        variant: 'error',
        message: 'This account uses Google sign-in. Please sign in with Google on the login page.',
      });
      return;
    }

    setAlert({ variant: 'error', message: msg });
  }

  /* ── STEP 1: Send OTP ── */
  async function handleSendOtp(e: React.FormEvent) {
    e.preventDefault();
    setAlert(null);
    setFieldErrors({});
    if (!validateEmailForm()) return;

    const trimmedEmail = email.trim();
    setLoading(true);
    try {
      const res = await authApi.forgotPassword(trimmedEmail);
      if (!res.success) {
        setAlert({ variant: 'error', message: res.message || 'Email not found. Please check and try again.' });
        return;
      }
      setEmail(trimmedEmail);
      setEmailSentSuccess(true);
      setStep('otp');
      startCountdown();
    } catch (err: unknown) {
      handleForgotApiError(err);
    } finally {
      setLoading(false);
    }
  }

  function handleOtpApiError(err: unknown, fallback: string) {
    const axiosError = err as {
      response?: { status?: number; data?: { message?: string } };
    };

    if (!axiosError.response) {
      setError('Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.');
      return;
    }

    const msg = axiosError.response.data?.message ?? fallback;
    setError(msg);
  }

  /* ── STEP 2 Resend ── */
  async function handleResend() {
    if (!canResend || loading) return;

    setError(null);
    setInlineOtpError(null);
    setLoading(true);
    setOtp(Array(OTP_LENGTH).fill(''));

    try {
      const res = await authApi.forgotPassword(email);
      if (!res.success) {
        setError(res.message || 'Something went wrong. Please try again.');
        return;
      }
      setEmailSentSuccess(true);
      startCountdown();
      otpRefs.current[0]?.focus();
    } catch (err: unknown) {
      handleOtpApiError(err, 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  /* ── OTP box input handler ── */
  function handleOtpChange(index: number, value: string) {
    const char = value.replace(/\D/g, '').slice(-1);
    const newOtp = [...otp];
    newOtp[index] = char;
    setOtp(newOtp);
    setError(null);
    setInlineOtpError(null);
    setEmailSentSuccess(false);
    if (char && index < OTP_LENGTH - 1) {
      otpRefs.current[index + 1]?.focus();
    }
  }

  function handleOtpKeyDown(index: number, e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
  }

  function handleOtpPaste(e: React.ClipboardEvent) {
    e.preventDefault();
    setInlineOtpError(null);
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    const newOtp = [...otp];
    for (let i = 0; i < pasted.length; i++) newOtp[i] = pasted[i];
    setOtp(newOtp);
    otpRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus();
  }

  /* ── STEP 2: Verify OTP → go to new password ── */
  function handleVerifyOtp(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const otpCode = otp.join('');
    if (otpCode.length < OTP_LENGTH) {
      setInlineOtpError('Please enter the complete 6-digit code.');
      return;
    }
    setInlineOtpError(null);
    setStep('newPassword');
  }

  function validatePasswordForm(): boolean {
    const errs: { password?: string; confirm?: string } = {};
    if (!password) errs.password = 'Password is required';
    else if (password.length < 8) errs.password = 'Password must be at least 8 characters';
    else if (!/[A-Z]/.test(password)) errs.password = 'Password must contain at least 1 uppercase letter';
    else if (!/[0-9]/.test(password)) errs.password = 'Password must contain at least 1 number';
    if (!confirm) errs.confirm = 'Please confirm your password';
    else if (password !== confirm) errs.confirm = 'Passwords do not match';
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function handleResetApiError(err: unknown) {
    const axiosError = err as {
      response?: { status?: number; data?: { message?: string } };
    };

    if (!axiosError.response) {
      setError('Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.');
      return;
    }

    const status = axiosError.response.status;
    const msg = axiosError.response.data?.message ?? 'Something went wrong. Please try again.';
    const isOtpIssue = status === 410
      || (status === 400 && (msg.toLowerCase().includes('otp') || msg.toLowerCase().includes('mã')));

    if (isOtpIssue) {
      const hint = status === 410
        ? `${msg} Please tap "Resend code" to get a new one.`
        : msg;
      setError(hint);
      setStep('otp');
      setOtp(Array(OTP_LENGTH).fill(''));
      setCanResend(true);
      setCountdown(0);
      if (countdownRef.current) clearInterval(countdownRef.current);
      return;
    }

    setError(msg);
  }

  /* ── STEP 3: Reset Password ── */
  async function handleResetPassword(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    if (!validatePasswordForm()) return;

    setLoading(true);
    try {
      const otpCode = otp.join('');
      const res = await authApi.resetPassword(email, otpCode, password);
      if (!res.success) {
        setError(res.message || 'Invalid or expired code. Please request a new one.');
        return;
      }
      setStep('done');
      setTimeout(() => navigate('/login', { replace: true }), 2500);
    } catch (err: unknown) {
      handleResetApiError(err);
    } finally {
      setLoading(false);
    }
  }

  /* ── Logo ── */
  const Logo = (
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
  );

  /* ── Error alert (OTP / password steps) ── */
  const ErrorAlert = error ? (
    <div style={{ marginBottom: 20 }}>
      <Alert variant="error" message={error} />
    </div>
  ) : null;

  /* ── Spinner icon ── */
  const Spinner = (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ animation: 'spin 0.8s linear infinite' }}>
      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
    </svg>
  );

  return (
    <div style={{ minHeight: '100vh', background: 'var(--canvas)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '24px 16px' }}>
      {Logo}

      <div className="card-lg animate-fade-up" style={{ width: '100%', maxWidth: 460, padding: 40 }}>

        {/* ─────────────────────────────────────────
            STEP 1 — Enter Email
        ───────────────────────────────────────── */}
        {step === 'email' && (
          <>
            {/* Lock icon */}
            <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'rgba(15, 118, 110, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </div>

            <h1 className="display-md" style={{ marginBottom: 8 }}>Forgot your password?</h1>
            <p className="body-md text-charcoal" style={{ marginBottom: 28, lineHeight: 1.6 }}>
              Enter your registered email address and we&apos;ll send you a 6-digit verification code.
            </p>

            {alert && (
              <div style={{ marginBottom: 20 }}>
                <Alert variant={alert.variant} message={alert.message} />
              </div>
            )}

            <form onSubmit={handleSendOtp} noValidate>
              <div style={{ marginBottom: 20 }}>
                <label className="form-label form-label-required" htmlFor="forgot-email">Email Address</label>
                <input
                  id="forgot-email"
                  type="email"
                  className={`input ${fieldErrors.email ? 'input-error' : ''}`}
                  placeholder="you@example.com"
                  value={email}
                  onChange={e => {
                    setEmail(e.target.value);
                    setFieldErrors({});
                    setAlert(null);
                  }}
                  autoComplete="email"
                  disabled={loading}
                />
                {fieldErrors.email && (
                  <p className="form-error" style={{ marginTop: 6 }}>{fieldErrors.email}</p>
                )}
              </div>

              <button id="forgot-password-submit-btn" type="submit" className="btn-primary" style={{ width: '100%', marginBottom: 12 }} disabled={loading}>
                {loading ? <>{Spinner} Sending...</> : 'Send Verification Code'}
              </button>

              <Link to="/login" className="btn-ghost" style={{ width: '100%', justifyContent: 'center', display: 'flex' }}>
                ← Back to Login
              </Link>
            </form>
          </>
        )}

        {/* ─────────────────────────────────────────
            STEP 2 — Enter OTP
        ───────────────────────────────────────── */}
        {step === 'otp' && (
          <>
            {/* Email icon */}
            <div style={{ width: 52, height: 52, borderRadius: '50%', background: '#eff6ff', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#2563eb" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
            </div>

            <h1 className="display-md" style={{ marginBottom: 8 }}>Check Your Email</h1>
            <p className="body-md text-charcoal" style={{ marginBottom: 4, lineHeight: 1.6 }}>
              We sent a 6-digit code to
            </p>
            <p className="body-md" style={{ fontWeight: 600, color: 'var(--ink)', marginBottom: 24, wordBreak: 'break-all' }}>
              {email}
            </p>

            {emailSentSuccess && (
              <div style={{ marginBottom: 20 }}>
                <Alert
                  variant="success"
                  message={`We sent a 6-digit code to ${email}. Check your inbox.`}
                />
              </div>
            )}

            {ErrorAlert}

            <form onSubmit={handleVerifyOtp} noValidate>
              {/* OTP 6-box input */}
              <div style={{ marginBottom: 24 }}>
                <label className="form-label form-label-required" style={{ marginBottom: 12 }}>Verification Code</label>
                <div
                  style={{ display: 'flex', gap: 8, justifyContent: 'center' }}
                  onPaste={handleOtpPaste}
                >
                  {otp.map((digit, i) => (
                    <input
                      key={i}
                      ref={el => { otpRefs.current[i] = el; }}
                      id={`otp-${i}`}
                      type="text"
                      inputMode="numeric"
                      maxLength={1}
                      value={digit}
                      onChange={e => handleOtpChange(i, e.target.value)}
                      onKeyDown={e => handleOtpKeyDown(i, e)}
                      disabled={loading}
                      style={{
                        width: 48,
                        height: 56,
                        textAlign: 'center',
                        fontSize: 22,
                        fontWeight: 700,
                        borderRadius: 10,
                        border: `1.5px solid ${inlineOtpError ? 'var(--error)' : digit ? 'var(--hairline-strong)' : 'var(--hairline)'}`,
                        background: 'var(--surface-card)',
                        color: 'var(--ink)',
                        outline: 'none',
                        transition: 'border-color 0.15s, box-shadow 0.15s',
                        caretColor: 'var(--primary)',
                      }}
                      onFocus={e => { e.target.style.borderColor = 'var(--primary)'; e.target.style.boxShadow = '0 0 0 3px rgba(234,40,4,0.15)'; }}
                      onBlur={e => { e.target.style.borderColor = digit ? 'var(--hairline-strong)' : 'var(--hairline)'; e.target.style.boxShadow = 'none'; }}
                    />
                  ))}
                </div>
                {inlineOtpError && (
                  <p className="form-error" style={{ marginTop: 8, textAlign: 'center' }}>{inlineOtpError}</p>
                )}
              </div>

              {/* Countdown / Resend */}
              <div style={{ textAlign: 'center', marginBottom: 20 }}>
                <button
                  type="button"
                  className="btn-ghost"
                  onClick={handleResend}
                  disabled={!canResend || loading}
                  style={{ margin: '0 auto', cursor: canResend && !loading ? 'pointer' : 'default' }}
                >
                  {loading ? <>{Spinner} Sending...</> : canResend ? '↻ Resend Code' : `Resend code in ${countdown}s`}
                </button>
              </div>

              <button id="otp-verify-btn" type="submit" className="btn-primary" style={{ width: '100%', marginBottom: 12 }} disabled={loading || otp.join('').length < 6}>
                Verify Code
              </button>

              <button type="button" className="btn-ghost" onClick={() => { setStep('email'); setError(null); setAlert(null); setEmailSentSuccess(false); setOtp(['', '', '', '', '', '']); }}
                style={{ width: '100%', justifyContent: 'center', display: 'flex' }}>
                ← Change Email
              </button>
            </form>
          </>
        )}

        {/* ─────────────────────────────────────────
            STEP 3 — New Password
        ───────────────────────────────────────── */}
        {step === 'newPassword' && (
          <>
            {/* Shield icon */}
            <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'rgba(15, 118, 110, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 20 }}>
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              </svg>
            </div>

            <h1 className="heading-md" style={{ marginBottom: 8 }}>Set new password</h1>
            <p className="body-md text-charcoal" style={{ marginBottom: 24, lineHeight: 1.6 }}>
              Choose a strong password for your account.
            </p>

            {ErrorAlert}

            <form onSubmit={handleResetPassword} noValidate>
              {/* New Password */}
              <div style={{ marginBottom: 16 }}>
                <label className="form-label form-label-required" htmlFor="reset-pw">New Password</label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reset-pw"
                    type={showPw ? 'text' : 'password'}
                    className={`input ${fieldErrors.password ? 'input-error' : ''}`}
                    placeholder="Enter new password"
                    value={password}
                    onChange={e => { setPassword(e.target.value); setFieldErrors(p => ({ ...p, password: undefined })); setError(null); }}
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
                <PasswordStrength password={password} />
                {fieldErrors.password && (
                  <p className="form-error" style={{ marginTop: 6 }}>{fieldErrors.password}</p>
                )}
              </div>

              {/* Confirm Password */}
              <div style={{ marginBottom: 20 }}>
                <label className="form-label form-label-required" htmlFor="reset-confirm">Confirm Password</label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="reset-confirm"
                    type={showConfirm ? 'text' : 'password'}
                    className={`input ${fieldErrors.confirm ? 'input-error' : ''}`}
                    placeholder="Repeat new password"
                    value={confirm}
                    onChange={e => { setConfirm(e.target.value); setFieldErrors(p => ({ ...p, confirm: undefined })); setError(null); }}
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

              {/* Password requirements checklist */}
              <div style={{ marginBottom: 24, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {[
                  { label: 'At least 8 characters', test: password.length >= 8 },
                  { label: 'One uppercase letter',  test: /[A-Z]/.test(password) },
                  { label: 'One number',            test: /[0-9]/.test(password) },
                ].map(item => (
                  <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
                    <div style={{ width: 16, height: 16, borderRadius: '50%', background: item.test ? '#dcfce7' : 'var(--surface-bone)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      {item.test && <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="3"><polyline points="20,6 9,17 4,12" /></svg>}
                    </div>
                    <span style={{ color: item.test ? 'var(--success)' : 'var(--charcoal)' }}>{item.label}</span>
                  </div>
                ))}
              </div>

              <button id="reset-password-submit-btn" type="submit" className="btn-primary" style={{ width: '100%', marginBottom: 12 }} disabled={loading}>
                {loading ? <>{Spinner} Resetting...</> : 'Reset Password'}
              </button>

              <button type="button" className="btn-ghost" onClick={() => { setStep('otp'); setError(null); setFieldErrors({}); }}
                style={{ width: '100%', justifyContent: 'center', display: 'flex', marginBottom: 12 }}>
                ← Back
              </button>

              <Link to="/login" className="btn-ghost" style={{ width: '100%', justifyContent: 'center', display: 'flex' }}>
                ← Back to Login
              </Link>
            </form>
          </>
        )}

        {/* ─────────────────────────────────────────
            STEP 4 — Done
        ───────────────────────────────────────── */}
        {step === 'done' && (
          <div style={{ textAlign: 'center' }}>
            <div style={{ width: 64, height: 64, borderRadius: '50%', background: '#f0fdf4', border: '1.5px solid #2b9a66', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
              <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="var(--success)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="20 6 9 17 4 12" />
              </svg>
            </div>
            <h1 className="display-md" style={{ marginBottom: 16 }}>Password Reset!</h1>
            <div style={{ marginBottom: 24 }}>
              <Alert variant="success" message="Password reset successfully. Redirecting to login..." />
            </div>
            <Link to="/login" className="btn-primary" style={{ display: 'inline-flex', width: '100%', justifyContent: 'center' }}>
              Go to Login
            </Link>
          </div>
        )}

      </div>

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
