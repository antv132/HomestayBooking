import { useState, useRef, useEffect, useCallback } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi, isAuthSession } from '../../api/authApi';
import { useAuthStore, getRoleDashboardPath } from '../../store/authStore';
import Alert from '../../components/ui/Alert';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN_SEC = 60;
const GOOGLE_LINK_TOKEN_KEY = 'googleLinkToken';

function isValidEmail(value: string): boolean {
  return /^\S+@\S+\.\S+$/.test(value);
}

function isAlreadyActiveMessage(message: string): boolean {
  const lower = message.toLowerCase();
  return lower.includes('kích hoạt') || lower.includes('activated') || lower.includes('already active');
}

export default function VerifyEmailPage() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [params] = useSearchParams();
  const email = (params.get('email') ?? '').trim();
  const purpose = params.get('purpose') ?? '';
  const isGoogleLink = purpose === 'google-link';
  const hasValidEmail = isValidEmail(email);

  const [otp, setOtp] = useState(Array(OTP_LENGTH).fill(''));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [inlineError, setInlineError] = useState<string | null>(null);
  const [showLoginLink, setShowLoginLink] = useState(false);
  const [success, setSuccess] = useState(false);
  const [countdown, setCountdown] = useState(RESEND_COOLDOWN_SEC);
  const [canResend, setCanResend] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
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

  useEffect(() => {
    if (!hasValidEmail) {
      navigate('/register', { replace: true });
      return;
    }

    inputRefs.current[0]?.focus();
    startCountdown();

    return () => {
      if (countdownRef.current) clearInterval(countdownRef.current);
    };
  }, [hasValidEmail, navigate, startCountdown]);

  useEffect(() => {
    if (!success) return;
    if (isGoogleLink) return;
    const timer = setTimeout(() => navigate('/login', { replace: true }), 2000);
    return () => clearTimeout(timer);
  }, [success, navigate, isGoogleLink]);

  function handleOtpChange(index: number, value: string) {
    const val = value.replace(/\D/g, '');
    setInlineError(null);

    if (!val) {
      const newOtp = [...otp];
      newOtp[index] = '';
      setOtp(newOtp);
      if (index > 0) inputRefs.current[index - 1]?.focus();
      return;
    }

    const newOtp = [...otp];
    newOtp[index] = val[val.length - 1];
    setOtp(newOtp);
    if (index < OTP_LENGTH - 1) inputRefs.current[index + 1]?.focus();
  }

  function handleKeyDown(index: number, e: React.KeyboardEvent) {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  }

  function handlePaste(e: React.ClipboardEvent) {
    e.preventDefault();
    setInlineError(null);
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    const newOtp = [...otp];
    for (let i = 0; i < pasted.length; i++) newOtp[i] = pasted[i];
    setOtp(newOtp);
    inputRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus();
  }

  function handleApiError(err: unknown, fallback: string) {
    const axiosError = err as {
      response?: {
        status?: number;
        data?: { message?: string };
      };
    };

    if (!axiosError.response) {
      setError('Cannot reach server. Ensure backend (HomestayApplication) is running on port 8080.');
      setShowLoginLink(false);
      return;
    }

    const status = axiosError.response.status;
    const msg = axiosError.response.data?.message ?? fallback;

    if (status === 410) {
      setError(`${msg} Please tap "Resend code" to get a new one.`);
      setShowLoginLink(false);
      setCanResend(true);
      setCountdown(0);
      if (countdownRef.current) clearInterval(countdownRef.current);
      return;
    }

    if (isAlreadyActiveMessage(msg)) {
      setError(msg);
      setShowLoginLink(true);
      return;
    }

    setError(msg);
    setShowLoginLink(false);
  }

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault();
    const code = otp.join('');

    if (code.length < OTP_LENGTH) {
      setInlineError('Please enter the complete 6-digit code.');
      return;
    }

    setError(null);
    setInlineError(null);
    setShowLoginLink(false);
    setLoading(true);

    try {
      if (isGoogleLink) {
        const verificationToken = sessionStorage.getItem(GOOGLE_LINK_TOKEN_KEY);
        if (!verificationToken) {
          setError('Phiên liên kết Google đã hết. Vui lòng đăng nhập Google lại.');
          return;
        }
        const res = await authApi.completeGoogleLink(verificationToken, code);
        if (!res.success || !res.data || !isAuthSession(res.data)) {
          setError(res.message || 'Không liên kết được Google. Vui lòng thử lại.');
          return;
        }
        sessionStorage.removeItem(GOOGLE_LINK_TOKEN_KEY);
        login(res.data);
        setSuccess(true);
        navigate(getRoleDashboardPath(res.data.user.role), { replace: true });
        return;
      }

      const res = await authApi.verifyOtp({ email, otpCode: code });

      if (!res.success) {
        setError(res.message || 'Invalid code. Please check and try again.');
        return;
      }

      setSuccess(true);
    } catch (err: unknown) {
      handleApiError(err, 'Invalid code. Please check and try again.');
    } finally {
      setLoading(false);
    }
  }

  async function handleResend() {
    if (!canResend || loading) return;

    setError(null);
    setInlineError(null);
    setShowLoginLink(false);
    setLoading(true);

    try {
      const res = await authApi.resendOtp(email);

      if (!res.success) {
        const msg = res.message || 'Failed to resend code.';
        if (isAlreadyActiveMessage(msg)) {
          setError(msg);
          setShowLoginLink(true);
        } else {
          setError(msg);
        }
        setCanResend(true);
        setCountdown(0);
        return;
      }

      setOtp(Array(OTP_LENGTH).fill(''));
      inputRefs.current[0]?.focus();
      startCountdown();
    } catch (err: unknown) {
      handleApiError(err, 'Failed to resend code.');
    } finally {
      setLoading(false);
    }
  }

  if (!hasValidEmail) {
    return null;
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
        <span className="font-display" style={{ fontWeight: 700, fontSize: 18, color: 'var(--ink)' }}>
          Homestay<span style={{ color: 'var(--primary)' }}>&</span>Resort
        </span>
      </Link>

      <div className="card-lg animate-fade-up" style={{ width: '100%', maxWidth: 440, padding: 40, textAlign: 'center' }}>
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            background: 'rgba(15,118,110,0.10)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px',
          }}
        >
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
            <polyline points="22,6 12,13 2,6" />
          </svg>
        </div>

        <h1 className="heading-md" style={{ marginBottom: 8 }}>
          {isGoogleLink ? 'Confirm Google link' : 'Verify your email'}
        </h1>
        <p className="body-md text-charcoal" style={{ marginBottom: 28 }}>
          We sent a 6-digit code to
          <br />
          <span style={{ fontWeight: 600, color: 'var(--ink)' }}>{email}</span>
        </p>

        {success ? (
          <Alert variant="success" message="Email verified! You can now sign in." />
        ) : (
          <form onSubmit={handleVerify}>
            {error && (
              <div style={{ marginBottom: 20, textAlign: 'left' }}>
                <Alert variant="error" message={error} />
                {showLoginLink && (
                  <p className="body-sm text-charcoal" style={{ marginTop: 12, textAlign: 'center' }}>
                    <Link to="/login" className="text-primary" style={{ fontWeight: 600, textDecoration: 'none' }}>
                      Go to Sign in
                    </Link>
                  </p>
                )}
              </div>
            )}

            <div
              style={{
                display: 'flex',
                gap: 8,
                justifyContent: 'center',
                marginBottom: inlineError ? 8 : 24,
                flexWrap: 'wrap',
              }}
              onPaste={handlePaste}
            >
              {otp.map((val, i) => (
                <input
                  key={i}
                  ref={(el) => {
                    inputRefs.current[i] = el;
                  }}
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={1}
                  value={val}
                  disabled={loading}
                  aria-label={`Digit ${i + 1}`}
                  onChange={(e) => handleOtpChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  style={{
                    width: 52,
                    height: 60,
                    border: `1.5px solid ${inlineError ? 'var(--error)' : val ? 'var(--primary)' : 'var(--hairline)'}`,
                    borderRadius: 10,
                    fontSize: 24,
                    fontFamily: 'JetBrains Mono, monospace',
                    fontWeight: 500,
                    textAlign: 'center',
                    outline: 'none',
                    background: val ? 'rgba(15,118,110,0.08)' : 'var(--surface-card)',
                    color: 'var(--ink)',
                    transition: 'all 0.15s',
                    opacity: loading ? 0.7 : 1,
                  }}
                />
              ))}
            </div>

            {inlineError && (
              <p className="form-error" style={{ marginBottom: 20 }}>
                {inlineError}
              </p>
            )}

            <button type="submit" className="btn-primary" style={{ width: '100%', marginBottom: 16 }} disabled={loading}>
              {loading ? 'Verifying...' : 'Verify Email'}
            </button>

            <button
              type="button"
              onClick={handleResend}
              disabled={!canResend || loading}
              className="btn-ghost"
              style={{
                width: '100%',
                cursor: canResend && !loading ? 'pointer' : 'default',
                color: canResend ? 'var(--primary)' : 'var(--ash)',
              }}
            >
              {canResend ? 'Resend code' : `Resend code in ${countdown}s`}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
