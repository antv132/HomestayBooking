import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

export default function UnauthorizedPage() {
  const navigate = useNavigate();
  const { isAuthenticated, role } = useAuthStore();

  const dashboardPath = role === 'MANAGER' ? '/manager/dashboard' : '/customer/dashboard';

  return (
    <div style={{
      minHeight: '100vh',
      background: 'var(--canvas)',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px 16px',
      textAlign: 'center',
    }}>
      {/* Icon */}
      <div style={{
        width: 80, height: 80,
        background: 'linear-gradient(135deg, #fee2e2, #fecaca)',
        borderRadius: 20,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        marginBottom: 24,
      }}>
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>

      {/* Badge */}
      <span style={{
        display: 'inline-block',
        background: '#fee2e2',
        color: '#ef4444',
        fontWeight: 700,
        fontSize: 12,
        letterSpacing: '0.08em',
        padding: '4px 12px',
        borderRadius: 9999,
        marginBottom: 16,
        textTransform: 'uppercase',
      }}>
        403 — Forbidden
      </span>

      <h1 className="display-lg" style={{ marginBottom: 12 }}>Truy cập bị từ chối</h1>
      <p className="body-md text-charcoal" style={{ maxWidth: 420, lineHeight: 1.7, marginBottom: 32 }}>
        Bạn không có quyền truy cập trang này.<br/>
        Vui lòng liên hệ quản trị viên nếu bạn cho rằng đây là lỗi.
      </p>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', justifyContent: 'center' }}>
        <button
          onClick={() => navigate(-1)}
          className="btn-outline"
          id="unauthorized-go-back-btn"
        >
          ← Quay lại
        </button>

        {isAuthenticated ? (
          <Link to={dashboardPath} className="btn-primary" id="unauthorized-dashboard-btn">
            Về Dashboard
          </Link>
        ) : (
          <Link to="/login" className="btn-primary" id="unauthorized-login-btn">
            Đăng nhập
          </Link>
        )}
      </div>
    </div>
  );
}
