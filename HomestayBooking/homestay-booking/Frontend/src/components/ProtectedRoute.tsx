import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

type AllowedRole = 'CUSTOMER' | 'MANAGER' | 'ADMIN' | 'EMPLOYEE';

interface ProtectedRouteProps {
  /** Role bắt buộc để truy cập route này */
  role: AllowedRole;
  children: React.ReactNode;
}

/** Bảng fallback dashboard theo role */
const ROLE_DASHBOARD: Record<string, string> = {
  CUSTOMER: '/customer/dashboard',
  MANAGER:  '/manager/dashboard',
  ADMIN:    '/admin/dashboard',
  EMPLOYEE: '/employee/dashboard',
};

/**
 * ProtectedRoute — bảo vệ route theo role.
 *
 * Logic:
 * 1. Chưa đăng nhập → redirect /login?redirect=<currentPath>
 * 2. Đã đăng nhập nhưng sai role → redirect về dashboard của role đó
 * 3. Đúng role → render children
 */
export default function ProtectedRoute({ role, children }: ProtectedRouteProps) {
  const { isAuthenticated, role: userRole } = useAuthStore();
  const location = useLocation();

  // Chưa đăng nhập → redirect /login, giữ lại path để quay lại sau
  if (!isAuthenticated) {
    return (
      <Navigate
        to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`}
        replace
      />
    );
  }

  // Đã đăng nhập nhưng sai role
  if (userRole !== role) {
    const fallback = userRole ? (ROLE_DASHBOARD[userRole] ?? '/') : '/';
    return <Navigate to={fallback} replace />;
  }

  return <>{children}</>;
}
