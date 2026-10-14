import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';

interface ManagerRedirectRouteProps {
  children: React.ReactNode;
}

/**
 * ManagerRedirectRoute — chặn Manager truy cập các trang public.
 *
 * Logic:
 * 1. Đã đăng nhập + role MANAGER → redirect về /manager/dashboard
 * 2. Guest hoặc Customer → render children bình thường
 *
 * Dùng cho: /, /rooms, /search, /rooms/:id, /rooms/:id/calendar, /about
 */
export default function ManagerRedirectRoute({ children }: ManagerRedirectRouteProps) {
  const { isAuthenticated, role } = useAuthStore();

  if (isAuthenticated && role === 'MANAGER') {
    return <Navigate to="/manager/dashboard" replace />;
  }

  return <>{children}</>;
}
