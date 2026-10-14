import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';

// ── Route Guards ─────────────────────────────────────────────────────────────
import ProtectedRoute from './components/ProtectedRoute';
import GuestRoute from './components/GuestRoute';
import ManagerRedirectRoute from './components/ManagerRedirectRoute';

// ── Public (SCR-01 → SCR-10) ─────────────────────────────────────────────────
import LandingPage from './pages/public/LandingPage';
import LoginPage from './pages/public/LoginPage';
import RegisterPage from './pages/public/RegisterPage';
import VerifyEmailPage from './pages/public/VerifyEmailPage';
import ForgotPasswordPage from './pages/public/ForgotPasswordPage';
import ResetPasswordPage from './pages/public/ResetPasswordPage';
import RoomListingPage from './pages/public/RoomListingPage';
import SearchResultsPage from './pages/public/SearchResultsPage';
import RoomDetailPage from './pages/public/RoomDetailPage';
import CartPage from './pages/public/CartPage';
import AvailabilityCalendarPage from './pages/public/AvailabilityCalendarPage';
import AboutPage from './pages/public/AboutPage';
import UnauthorizedPage from './pages/public/UnauthorizedPage';

// ── Customer Portal ───────────────────────────────────────────────────────────
import CustomerDashboardPage from './pages/customer/CustomerDashboardPage';
// Profile (SCR-10, 11, 12)
import UserProfilePage from './pages/customer/UserProfilePage';
import EditProfilePage from './pages/customer/EditProfilePage';
import ChangePasswordPage from './pages/customer/ChangePasswordPage';
// Notifications (SCR-13, 14)
import { NotificationCenterPage, NotificationDetailPage }
  from './pages/customer/NotificationPages';
// Bookings (SCR-17, 18, 19, 20)
import BookingFormPage from './pages/customer/BookingFormPage';
import BookingListPage from './pages/customer/BookingListPage';
import BookingDetailPage from './pages/customer/BookingDetailPage';
import BookingCancellationPage from './pages/customer/BookingCancellationPage';
import ContractListPage from './pages/customer/ContractListPage';
import ContractDetailPage from './pages/customer/ContractDetailPage';
// Payments (SCR-21, 22, 23, 24)
import DepositPaymentPage from './pages/customer/DepositPaymentPage';
import RemainingPaymentPage from './pages/customer/RemainingPaymentPage';
import DamageFeePaymentPage from './pages/customer/DamageFeePaymentPage';
import PaymentHistoryPage from './pages/customer/PaymentHistoryPage';
import VNPayResultPage from './pages/customer/VNPayResultPage';
// Maintenance (SCR-27, 28, 29)
import MaintenanceListPage from './pages/customer/MaintenanceListPage';
import CreateMaintenancePage from './pages/customer/CreateMaintenancePage';
import MaintenanceDetailPage from './pages/customer/MaintenanceDetailPage';
// Reviews (SCR-30, 31)
import ReviewRatingPage from './pages/customer/ReviewRatingPage';
import MyReviewsPage from './pages/customer/MyReviewsPage';
// Complaints
import { CustomerComplaintListPage, CreateComplaintPage }
  from './pages/customer/CustomerComplaintPages';

// ── Manager Portal ────────────────────────────────────────────────────────────
import ManagerDashboardPage from './pages/manager/ManagerDashboardPage';
import PropertyDetailPage from './pages/manager/PropertyDetailPage';
import StructureTreePage from './pages/manager/StructureTreePage';
import FloorManagementPage from './pages/manager/FloorManagementPage';
import RoomListPage from './pages/manager/RoomListPage';
import RoomDetailMgmtPage from './pages/manager/RoomDetailMgmtPage';
import AddRoomPage from './pages/manager/AddRoomPage';
import EditRoomPage from './pages/manager/EditRoomPage';
import ManagerRoomCalendarPage from './pages/manager/ManagerRoomCalendarPage';
import BookingMgmtListPage from './pages/manager/BookingMgmtListPage';
import BookingMgmtDetailPage from './pages/manager/BookingMgmtDetailPage';
import BookingCheckInOutPage from './pages/manager/BookingCheckInOutPage';
import BookingMgmtCancelPage from './pages/manager/BookingMgmtCancelPage';
import HousekeepingSchedulePage from './pages/manager/HousekeepingSchedulePage';
import HousekeepingTasksPage from './pages/manager/HousekeepingTasksPage';
import MaintenanceTasksPage from './pages/manager/MaintenanceTasksPage';
import InspectionsPage from './pages/manager/InspectionsPage';
import DamageReportsPage from './pages/manager/DamageReportsPage';
import PropertyReportsPage from './pages/manager/PropertyReportsPage';
import EmployeeMgmtPage from './pages/manager/EmployeeMgmtPage';
import PaymentMgmtListPage from './pages/manager/PaymentMgmtListPage';
import PaymentMgmtDetailPage from './pages/manager/PaymentMgmtDetailPage';
import ContractMgmtListPage from './pages/manager/ContractMgmtListPage';
import ContractMgmtDetailPage from './pages/manager/ContractMgmtDetailPage';
import { CustomerListPage } from './pages/manager/CustomerListPage';
import { CustomerDetailPage } from './pages/manager/CustomerDetailPage';
import { ReportsDashboardPage } from './pages/manager/ReportsDashboardPage';
import { RevenueReportPage } from './pages/manager/RevenueReportPage';
import { OccupancyReportPage } from './pages/manager/OccupancyReportPage';
import { ActivityLogPage } from './pages/manager/ActivityLogPage';
import { ReviewMgmtPage } from './pages/manager/ReviewMgmtPage';
// ── Admin Portal (SCR-45 → SCR-58) ──────────────────────────────────────────
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import { PropertyMgmtListPage } from './pages/admin/PropertyMgmtListPage';
import { CreatePropertyPage } from './pages/admin/CreatePropertyPage';
import { EditPropertyAdminPage } from './pages/admin/EditPropertyAdminPage';
import { ManagerAssignmentPage } from './pages/admin/ManagerAssignmentPage';
import { ManagerDirectoryPage } from './pages/admin/ManagerDirectoryPage';
import { CustomerDirectoryPage } from './pages/admin/CustomerDirectoryPage';
import AdminEmployeeMgmtPage from './pages/admin/AdminEmployeeMgmtPage';
import { DamageEscalationPage } from './pages/admin/DamageEscalationPage';
import { AdminComplaintsPage } from './pages/admin/AdminComplaintsPage';
import { GlobalReportsPage } from './pages/admin/GlobalReportsPage';
import { SystemAdminPage } from './pages/admin/SystemAdminPage';
import PromotionMgmtPage from './pages/admin/PromotionMgmtPage';
import { AdminHousekeepingPage } from './pages/admin/AdminHousekeepingPage';

// ── Employee Portal (SCR-59 → SCR-65) ────────────────────────────────────────
import EmployeeDashboardPage from './pages/employee/EmployeeDashboardPage';
import HousekeepingWorkspacePage from './pages/employee/HousekeepingWorkspacePage';
import MaintenanceWorkspacePage from './pages/employee/MaintenanceWorkspacePage';
import RoomInspectionHubPage from './pages/employee/RoomInspectionHubPage';
import DamageReportListPage from './pages/employee/DamageReportListPage';
import CreateDamageReportPage from './pages/employee/CreateDamageReportPage';
import PropertyRoomListPage from './pages/employee/PropertyRoomListPage';
import EmployeeRoomCalendarPage from './pages/employee/EmployeeRoomCalendarPage';

/** Catch-all: redirect theo role */
function CatchAllRedirect() {
  const role = sessionStorage.getItem('userRole');
  if (role === 'MANAGER')  return <Navigate to="/manager/dashboard"  replace />;
  if (role === 'CUSTOMER') return <Navigate to="/customer/dashboard" replace />;
  if (role === 'ADMIN')    return <Navigate to="/admin/dashboard"    replace />;
  if (role === 'EMPLOYEE') return <Navigate to="/employee/dashboard" replace />;
  return <Navigate to="/" replace />;
}

/** Legacy SCR-32/33 dedicated pages → SCR-31 tabs */
function LegacyRoomTabRedirect({ tab }: { tab: 'gallery' | 'status' }) {
  const { id } = useParams();
  return <Navigate to={`/manager/rooms/${id}/edit?tab=${tab}`} replace />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>

        {/* ─────────────── PUBLIC — Manager bị redirect về dashboard ─────────────── */}
        <Route path="/" element={<ManagerRedirectRoute><LandingPage /></ManagerRedirectRoute>} />
        <Route path="/rooms" element={<ManagerRedirectRoute><RoomListingPage /></ManagerRedirectRoute>} />
        <Route path="/search" element={<ManagerRedirectRoute><SearchResultsPage /></ManagerRedirectRoute>} />
        <Route path="/rooms/:id" element={<ManagerRedirectRoute><RoomDetailPage /></ManagerRedirectRoute>} />
        <Route path="/rooms/:id/calendar" element={<ManagerRedirectRoute><AvailabilityCalendarPage /></ManagerRedirectRoute>} />
        <Route path="/about" element={<ManagerRedirectRoute><AboutPage /></ManagerRedirectRoute>} />
        <Route path="/cart" element={<ManagerRedirectRoute><CartPage /></ManagerRedirectRoute>} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* ─────────────── GUEST ONLY — Redirect nếu đã login ─────────────── */}
        <Route path="/login" element={<GuestRoute><LoginPage /></GuestRoute>} />
        <Route path="/register" element={<GuestRoute><RegisterPage /></GuestRoute>} />
        <Route path="/verify-email" element={<GuestRoute><VerifyEmailPage /></GuestRoute>} />
        <Route path="/forgot-password" element={<GuestRoute><ForgotPasswordPage /></GuestRoute>} />
        <Route path="/reset-password" element={<GuestRoute><ResetPasswordPage /></GuestRoute>} />

        {/* ─────────────── CUSTOMER (SCR-11 to SCR-31) ─────────────── */}
        <Route path="/customer" element={<Navigate to="/customer/dashboard" replace />} />

        <Route path="/customer/dashboard"
          element={<ProtectedRoute role="CUSTOMER"><CustomerDashboardPage /></ProtectedRoute>} />

        {/* Profile (SCR-11,12,13) */}
        <Route path="/customer/profile"
          element={<ProtectedRoute role="CUSTOMER"><UserProfilePage /></ProtectedRoute>} />
        <Route path="/customer/profile/edit"
          element={<ProtectedRoute role="CUSTOMER"><EditProfilePage /></ProtectedRoute>} />
        <Route path="/customer/profile/change-password"
          element={<ProtectedRoute role="CUSTOMER"><ChangePasswordPage /></ProtectedRoute>} />

        {/* Notifications (SCR-13,14) */}
        <Route path="/customer/notifications"
          element={<ProtectedRoute role="CUSTOMER"><NotificationCenterPage /></ProtectedRoute>} />
        <Route path="/customer/notifications/:id"
          element={<ProtectedRoute role="CUSTOMER"><NotificationDetailPage /></ProtectedRoute>} />

        {/* Bookings (SCR-17,18,19,20) */}
        <Route path="/customer/bookings"
          element={<ProtectedRoute role="CUSTOMER"><BookingListPage /></ProtectedRoute>} />
        <Route path="/request-booking/:roomId"
          element={<ProtectedRoute role="CUSTOMER"><BookingFormPage /></ProtectedRoute>} />
        <Route path="/customer/bookings/:id"
          element={<ProtectedRoute role="CUSTOMER"><BookingDetailPage /></ProtectedRoute>} />
        <Route path="/customer/bookings/:id/cancel"
          element={<ProtectedRoute role="CUSTOMER"><BookingCancellationPage /></ProtectedRoute>} />

        {/* Contracts (SCR-25,26) */}
        <Route path="/customer/contracts"
          element={<ProtectedRoute role="CUSTOMER"><ContractListPage /></ProtectedRoute>} />
        <Route path="/customer/contracts/:id"
          element={<ProtectedRoute role="CUSTOMER"><ContractDetailPage /></ProtectedRoute>} />

        {/* Payments (SCR-21,22,23,24) */}
        <Route path="/customer/payments"
          element={<ProtectedRoute role="CUSTOMER"><PaymentHistoryPage /></ProtectedRoute>} />
        <Route path="/customer/payments/:id/pay/deposit"
          element={<ProtectedRoute role="CUSTOMER"><DepositPaymentPage /></ProtectedRoute>} />
        <Route path="/customer/payments/:id/pay"
          element={<ProtectedRoute role="CUSTOMER"><DepositPaymentPage /></ProtectedRoute>} />
        <Route path="/customer/payments/:id/remaining"
          element={<ProtectedRoute role="CUSTOMER"><RemainingPaymentPage /></ProtectedRoute>} />
        <Route path="/customer/payments/:id/damage"
          element={<ProtectedRoute role="CUSTOMER"><DamageFeePaymentPage /></ProtectedRoute>} />
        <Route path="/customer/payments/vnpay-result"
          element={<ProtectedRoute role="CUSTOMER"><VNPayResultPage /></ProtectedRoute>} />

        {/* Maintenance (SCR-27,28,29) */}
        <Route path="/customer/maintenance"
          element={<ProtectedRoute role="CUSTOMER"><MaintenanceListPage /></ProtectedRoute>} />
        <Route path="/customer/maintenance/create"
          element={<ProtectedRoute role="CUSTOMER"><CreateMaintenancePage /></ProtectedRoute>} />
        <Route path="/customer/maintenance/:id"
          element={<ProtectedRoute role="CUSTOMER"><MaintenanceDetailPage /></ProtectedRoute>} />

        {/* Reviews (SCR-30,31) */}
        <Route path="/customer/reviews"
          element={<ProtectedRoute role="CUSTOMER"><MyReviewsPage /></ProtectedRoute>} />
        <Route path="/customer/reviews/create"
          element={<ProtectedRoute role="CUSTOMER"><ReviewRatingPage /></ProtectedRoute>} />

        {/* Complaints */}
        <Route path="/customer/complaints"
          element={<ProtectedRoute role="CUSTOMER"><CustomerComplaintListPage /></ProtectedRoute>} />
        <Route path="/customer/complaints/create"
          element={<ProtectedRoute role="CUSTOMER"><CreateComplaintPage /></ProtectedRoute>} />

        {/* ─────────────── MANAGER (SCR-32 to SCR-65) ─────────────── */}
        <Route path="/manager" element={<Navigate to="/manager/dashboard" replace />} />

        <Route path="/manager/dashboard"
          element={<ProtectedRoute role="MANAGER"><ManagerDashboardPage /></ProtectedRoute>} />

        {/* Profile (SCR-11,12,13) — shared with customer */}
        <Route path="/manager/profile"
          element={<ProtectedRoute role="MANAGER"><UserProfilePage /></ProtectedRoute>} />
        <Route path="/manager/profile/edit"
          element={<ProtectedRoute role="MANAGER"><EditProfilePage /></ProtectedRoute>} />
        <Route path="/manager/profile/change-password"
          element={<ProtectedRoute role="MANAGER"><ChangePasswordPage /></ProtectedRoute>} />

        {/* Notifications (SCR-13,14) */}
        <Route path="/manager/notifications"
          element={<ProtectedRoute role="MANAGER"><NotificationCenterPage /></ProtectedRoute>} />
        <Route path="/manager/notifications/:id"
          element={<ProtectedRoute role="MANAGER"><NotificationDetailPage /></ProtectedRoute>} />

        {/* Property (FR-06 read-only detail + selector khi nhiều property) */}
        <Route path="/manager/properties"
          element={<ProtectedRoute role="MANAGER"><PropertyDetailPage /></ProtectedRoute>} />
        <Route path="/manager/properties/:id"
          element={<ProtectedRoute role="MANAGER"><PropertyDetailPage /></ProtectedRoute>} />

        {/* Structure (SCR-37,38) */}
        <Route path="/manager/structure"
          element={<ProtectedRoute role="MANAGER"><StructureTreePage /></ProtectedRoute>} />
        <Route path="/manager/floors"
          element={<ProtectedRoute role="MANAGER"><FloorManagementPage /></ProtectedRoute>} />

        {/* Rooms (SCR-39 to 44) */}
        <Route path="/manager/rooms"
          element={<ProtectedRoute role="MANAGER"><RoomListPage /></ProtectedRoute>} />
        <Route path="/manager/rooms/add"
          element={<ProtectedRoute role="MANAGER"><AddRoomPage /></ProtectedRoute>} />
        <Route path="/manager/rooms/:id"
          element={<ProtectedRoute role="MANAGER"><RoomDetailMgmtPage /></ProtectedRoute>} />
        <Route path="/manager/rooms/:id/calendar"
          element={<ProtectedRoute role="MANAGER"><ManagerRoomCalendarPage /></ProtectedRoute>} />
        <Route path="/manager/rooms/:id/edit"
          element={<ProtectedRoute role="MANAGER"><EditRoomPage /></ProtectedRoute>} />
        {/* Legacy SCR-32/33 → SCR-31 tabs */}
        <Route path="/manager/rooms/:id/gallery"
          element={<ProtectedRoute role="MANAGER"><LegacyRoomTabRedirect tab="gallery" /></ProtectedRoute>} />
        <Route path="/manager/rooms/:id/status"
          element={<ProtectedRoute role="MANAGER"><LegacyRoomTabRedirect tab="status" /></ProtectedRoute>} />

        {/* Bookings (SCR-45,46) */}
        <Route path="/manager/bookings"
          element={<ProtectedRoute role="MANAGER"><BookingMgmtListPage /></ProtectedRoute>} />
        <Route path="/manager/bookings/:id/check-in"
          element={<ProtectedRoute role="MANAGER"><BookingCheckInOutPage /></ProtectedRoute>} />
        <Route path="/manager/bookings/:id/check-out"
          element={<ProtectedRoute role="MANAGER"><BookingCheckInOutPage /></ProtectedRoute>} />
        <Route path="/manager/bookings/:id/cancel"
          element={<ProtectedRoute role="MANAGER"><BookingMgmtCancelPage /></ProtectedRoute>} />
        <Route path="/manager/bookings/:id"
          element={<ProtectedRoute role="MANAGER"><BookingMgmtDetailPage /></ProtectedRoute>} />

        <Route path="/manager/housekeeping/schedule"
          element={<ProtectedRoute role="MANAGER"><HousekeepingSchedulePage /></ProtectedRoute>} />

        <Route path="/manager/housekeeping/tasks"
          element={<ProtectedRoute role="MANAGER"><HousekeepingTasksPage /></ProtectedRoute>} />
        <Route path="/manager/housekeeping"
          element={<ProtectedRoute role="MANAGER"><Navigate to="/manager/housekeeping/tasks" replace /></ProtectedRoute>} />

        {/* SCR-42 / SCR-43 — Room Inspection & Damage Reports */}
        <Route path="/manager/inspections"
          element={<ProtectedRoute role="MANAGER"><InspectionsPage /></ProtectedRoute>} />
        <Route path="/manager/damage-reports"
          element={<ProtectedRoute role="MANAGER"><DamageReportsPage /></ProtectedRoute>} />

        <Route path="/manager/employees"
          element={<ProtectedRoute role="MANAGER"><EmployeeMgmtPage /></ProtectedRoute>} />

        {/* Payments (SCR-47,48,49) */}
        <Route path="/manager/payments"
          element={<ProtectedRoute role="MANAGER"><PaymentMgmtListPage /></ProtectedRoute>} />
        <Route path="/manager/payments/:id"
          element={<ProtectedRoute role="MANAGER"><PaymentMgmtDetailPage /></ProtectedRoute>} />

        {/* Contracts (SCR-50,51,52) */}
        <Route path="/manager/contracts"
          element={<ProtectedRoute role="MANAGER"><ContractMgmtListPage /></ProtectedRoute>} />
        <Route path="/manager/contracts/:id"
          element={<ProtectedRoute role="MANAGER"><ContractMgmtDetailPage /></ProtectedRoute>} />

        {/* Customers (SCR-53,54) */}
        <Route path="/manager/customers"
          element={<ProtectedRoute role="MANAGER"><CustomerListPage /></ProtectedRoute>} />
        <Route path="/manager/customers/:id"
          element={<ProtectedRoute role="MANAGER"><CustomerDetailPage /></ProtectedRoute>} />

        {/* FR-17: Manager complaints deprecated — redirect */}
        <Route path="/manager/complaints"
          element={<Navigate to="/manager/dashboard" replace />} />
        <Route path="/manager/complaints/:id"
          element={<Navigate to="/manager/dashboard" replace />} />

        {/* Maintenance Tasks (SCR-41) — legacy /manager/maintenance redirects here */}
        <Route path="/manager/maintenance/tasks"
          element={<ProtectedRoute role="MANAGER"><MaintenanceTasksPage /></ProtectedRoute>} />
        <Route path="/manager/maintenance"
          element={<ProtectedRoute role="MANAGER"><Navigate to="/manager/maintenance/tasks" replace /></ProtectedRoute>} />
        <Route path="/manager/maintenance/:id"
          element={<ProtectedRoute role="MANAGER"><Navigate to="/manager/maintenance/tasks" replace /></ProtectedRoute>} />

        {/* Property Reports (SCR-44) — gộp Doanh thu · Lấp đầy · Xu hướng theo Tabs */}
        <Route path="/manager/reports/property"
          element={<ProtectedRoute role="MANAGER"><PropertyReportsPage /></ProtectedRoute>} />

        {/* Reports (SCR-59,60,61) */}
        <Route path="/manager/reports"
          element={<ProtectedRoute role="MANAGER"><ReportsDashboardPage /></ProtectedRoute>} />
        <Route path="/manager/reports/revenue"
          element={<ProtectedRoute role="MANAGER"><RevenueReportPage /></ProtectedRoute>} />
        <Route path="/manager/reports/occupancy"
          element={<ProtectedRoute role="MANAGER"><OccupancyReportPage /></ProtectedRoute>} />
        <Route path="/manager/reports/bookings"
          element={<ProtectedRoute role="MANAGER"><Navigate to="/manager/reports/property?tab=bookings" replace /></ProtectedRoute>} />

        {/* Audit & Reviews (SCR-62,63) */}
        <Route path="/manager/activity-log"
          element={<ProtectedRoute role="MANAGER"><ActivityLogPage /></ProtectedRoute>} />
        <Route path="/manager/reviews"
          element={<ProtectedRoute role="MANAGER"><ReviewMgmtPage /></ProtectedRoute>} />

        {/* ─────────────── ADMIN (SCR-45 to SCR-58) ─────────────── */}
        <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />

        <Route path="/admin/dashboard"
          element={<ProtectedRoute role="ADMIN"><AdminDashboardPage /></ProtectedRoute>} />

        {/* Profile (SCR-10,11,12) */}
        <Route path="/admin/profile"
          element={<ProtectedRoute role="ADMIN"><UserProfilePage /></ProtectedRoute>} />
        <Route path="/admin/profile/edit"
          element={<ProtectedRoute role="ADMIN"><EditProfilePage /></ProtectedRoute>} />
        <Route path="/admin/profile/change-password"
          element={<ProtectedRoute role="ADMIN"><ChangePasswordPage /></ProtectedRoute>} />

        {/* Notifications (SCR-13,14) */}
        <Route path="/admin/notifications"
          element={<ProtectedRoute role="ADMIN"><NotificationCenterPage /></ProtectedRoute>} />
        <Route path="/admin/notifications/:id"
          element={<ProtectedRoute role="ADMIN"><NotificationDetailPage /></ProtectedRoute>} />

        {/* Properties */}
        <Route path="/admin/properties"
          element={<ProtectedRoute role="ADMIN"><PropertyMgmtListPage /></ProtectedRoute>} />
        <Route path="/admin/structure"
          element={<ProtectedRoute role="ADMIN"><StructureTreePage readOnly /></ProtectedRoute>} />
        <Route path="/admin/properties/create"
          element={<ProtectedRoute role="ADMIN"><CreatePropertyPage /></ProtectedRoute>} />
        <Route path="/admin/properties/new"
          element={<Navigate to="/admin/properties/create" replace />} />
        <Route path="/admin/properties/:id/edit"
          element={<ProtectedRoute role="ADMIN"><EditPropertyAdminPage /></ProtectedRoute>} />
        <Route path="/admin/properties/:id/manager"
          element={<ProtectedRoute role="ADMIN"><ManagerAssignmentPage /></ProtectedRoute>} />

        {/* Users */}
        <Route path="/admin/managers"
          element={<ProtectedRoute role="ADMIN"><ManagerDirectoryPage /></ProtectedRoute>} />
        <Route path="/admin/customers"
          element={<ProtectedRoute role="ADMIN"><CustomerDirectoryPage /></ProtectedRoute>} />
        <Route path="/admin/employees"
          element={<ProtectedRoute role="ADMIN"><AdminEmployeeMgmtPage /></ProtectedRoute>} />

        {/* Operations */}
        <Route path="/admin/damage-escalation"
          element={<ProtectedRoute role="ADMIN"><DamageEscalationPage /></ProtectedRoute>} />
        <Route path="/admin/complaints"
          element={<ProtectedRoute role="ADMIN"><AdminComplaintsPage /></ProtectedRoute>} />
        <Route path="/admin/housekeeping"
          element={<ProtectedRoute role="ADMIN"><AdminHousekeepingPage /></ProtectedRoute>} />

        {/* Reports */}
        <Route path="/admin/reports"
          element={<ProtectedRoute role="ADMIN"><GlobalReportsPage /></ProtectedRoute>} />

        {/* Marketing — FR-18 Promotion (SCR-57/58) */}
        <Route path="/admin/promotions"
          element={<ProtectedRoute role="ADMIN"><PromotionMgmtPage /></ProtectedRoute>} />
        <Route path="/admin/promotions/create"
          element={<Navigate to="/admin/promotions" replace />} />
        <Route path="/admin/promotions/:id/edit"
          element={<Navigate to="/admin/promotions" replace />} />

        {/* System */}
        <Route path="/admin/system"
          element={<ProtectedRoute role="ADMIN"><SystemAdminPage /></ProtectedRoute>} />
        <Route path="/admin/settings"
          element={<Navigate to="/admin/system" replace />} />

        {/* ─────────────── EMPLOYEE (SCR-59 to SCR-65) ─────────────── */}
        <Route path="/employee" element={<Navigate to="/employee/dashboard" replace />} />

        <Route path="/employee/dashboard"
          element={<ProtectedRoute role="EMPLOYEE"><EmployeeDashboardPage /></ProtectedRoute>} />

        {/* Profile (SCR-10,11,12) */}
        <Route path="/employee/profile"
          element={<ProtectedRoute role="EMPLOYEE"><UserProfilePage /></ProtectedRoute>} />
        <Route path="/employee/profile/edit"
          element={<ProtectedRoute role="EMPLOYEE"><EditProfilePage /></ProtectedRoute>} />
        <Route path="/employee/profile/change-password"
          element={<ProtectedRoute role="EMPLOYEE"><ChangePasswordPage /></ProtectedRoute>} />

        {/* Notifications (SCR-13,14) */}
        <Route path="/employee/notifications"
          element={<ProtectedRoute role="EMPLOYEE"><NotificationCenterPage /></ProtectedRoute>} />
        <Route path="/employee/notifications/:id"
          element={<ProtectedRoute role="EMPLOYEE"><NotificationDetailPage /></ProtectedRoute>} />

        <Route path="/employee/housekeeping"
          element={<ProtectedRoute role="EMPLOYEE"><HousekeepingWorkspacePage /></ProtectedRoute>} />
        <Route path="/employee/maintenance"
          element={<ProtectedRoute role="EMPLOYEE"><MaintenanceWorkspacePage /></ProtectedRoute>} />
        <Route path="/employee/inspections"
          element={<ProtectedRoute role="EMPLOYEE"><RoomInspectionHubPage /></ProtectedRoute>} />
        <Route path="/employee/damage"
          element={<ProtectedRoute role="EMPLOYEE"><DamageReportListPage /></ProtectedRoute>} />
        <Route path="/employee/damage/create"
          element={<ProtectedRoute role="EMPLOYEE"><CreateDamageReportPage /></ProtectedRoute>} />
        {/* Spec aliases FR-23 */}
        <Route path="/employee/damage-reports"
          element={<ProtectedRoute role="EMPLOYEE"><Navigate to="/employee/damage" replace /></ProtectedRoute>} />
        <Route path="/employee/damage-reports/new"
          element={<ProtectedRoute role="EMPLOYEE"><Navigate to="/employee/damage/create" replace /></ProtectedRoute>} />
        <Route path="/employee/rooms"
          element={<ProtectedRoute role="EMPLOYEE"><PropertyRoomListPage /></ProtectedRoute>} />
        <Route path="/employee/rooms/:id/calendar"
          element={<ProtectedRoute role="EMPLOYEE"><EmployeeRoomCalendarPage /></ProtectedRoute>} />

        {/* Catch-all — redirect theo role */}
        <Route path="*" element={<CatchAllRedirect />} />

      </Routes>
    </BrowserRouter>
  );
}

export default App;
