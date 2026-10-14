/**
 * adminApi.ts — REST API calls for Admin Portal (SCR-45 to SCR-58)
 * Source of truth: docs/api-spec-by-screen.md
 */
import api from './axiosInstance';
import type { BookingTrendData, OccupancyReportData } from './reportApi';

// ── Shared types ───────────────────────────────────────────────────────────────

export interface AdminUser {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  status: string;
  avatarUrl: string | null;
  createdAt: string;
  updatedAt: string;
  /** SCR-50 — ACTIVE property assignments (managers only) */
  propertiesAssigned?: number | null;
  /** SCR-51 — booking count (customers only) */
  totalBookings?: number | null;
  /** SCR-51 — paid spend (customers only) */
  totalSpend?: number | null;
}

/** SCR-51 — Customer booking history row */
export interface AdminCustomerBooking {
  id: string;
  roomNumber: string;
  propertyName: string;
  checkInDate: string;
  checkOutDate: string;
  status: string;
  totalAmount: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminProperty {
  id: string;
  name: string;
  location: string;
  description?: string | null;
  status: 'ACTIVE' | 'INACTIVE';
  managerId?: string;
  managerName?: string;
  createdAt: string;
}

export interface GlobalKpis {
  totalRevenue: number;
  totalBookings: number;
  totalProperties: number;
  totalCustomers: number;
  totalFloors: number;
  totalRooms: number;
  availableRooms: number;
  occupiedRooms: number;
  upcomingCheckIns: number;
  upcomingCheckOuts: number;
  monthlyRevenue: number;
}

export interface PaymentReconciliationItem {
  id: string;
  bookingId: string;
  amount: number;
  vnpayStatus: string;
  systemStatus: string;
  discrepancyReason?: string;
  createdAt: string;
}

export interface AdminDamageReport {
  id: string;
  roomId: string;
  roomName: string;
  propertyName: string;
  reportedBy: string;
  totalFee: number;
  status: 'ESCALATED' | 'APPROVED' | 'PENDING_REVIEW';
  items: { name: string; estimatedCost: number }[];
  attachments: { url: string; type: string }[];
  createdAt: string;
}

export interface AdminComplaint {
  id: string;
  customerId: string;
  customerName: string;
  subject?: string | null;
  bookingId?: string | null;
  description: string;
  status: 'OPEN' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';
  resolution?: string;
  createdAt: string;
}

/** Banner promotion — khop entity Promotion / Manager PromotionItem (SCR-57/58) */
export interface Promotion {
  id: string;
  subtitle: string;
  title: string;
  description?: string;
  ctaText: string;
  ctaUrl: string;
  colorTheme: string;
  isActive: boolean;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface PromotionPayload {
  subtitle: string;
  title: string;
  description?: string;
  ctaText: string;
  ctaUrl: string;
  colorTheme: string;
  isActive: boolean;
  sortOrder: number;
}

export interface SystemSettings {
  depositPercentage: number;
  cancelTimeoutHours: number;
  systemName?: string;
  supportEmail?: string;
  bankAccountNumber?: string;
  bankAccountName?: string;
  bankName?: string;
  updatedAt?: string | null;
}

export interface MonthlyRevenue {
  month: number;
  revenue: number;
}

// ── SCR-45: Admin Dashboard ────────────────────────────────────────────────────

/** GET /api/reports/global-kpis */
export async function getGlobalKpis(): Promise<{ success: boolean; data: GlobalKpis }> {
  const res = await api.get('/api/reports/global-kpis');
  return res.data;
}

// ── SCR-46: Property List ──────────────────────────────────────────────────────

/** GET /api/v1/admin/properties/{id}/tree — FR-07 Admin read-only structure */
export async function getAdminPropertyStructure(propertyId: string): Promise<{
  success: boolean;
  data: {
    propertyId: string;
    propertyName: string;
    floors: Array<{
      id: string;
      floorNumber: number;
      description: string | null;
      roomCount: number;
      rooms: Array<{
        id: string;
        roomNumber: string;
        roomType?: string;
        status: string;
      }>;
    }>;
  };
}> {
  const res = await api.get(`/api/v1/admin/properties/${propertyId}/tree`);
  return res.data;
}

/** GET /api/v1/admin/properties — SCR-46 list (keyword|search, status, managerId, sort) */
export async function getAdminProperties(params?: {
  page?: number;
  size?: number;
  keyword?: string;
  search?: string;
  status?: string;
  managerId?: string;
  sort?: string;
}): Promise<{ success: boolean; data: PageResponse<AdminProperty> }> {
  const res = await api.get('/api/v1/admin/properties', { params });
  return res.data;
}

// ── SCR-47: Create Property ────────────────────────────────────────────────────

/** POST /api/v1/admin/properties — SCR-47 create (default INACTIVE) */
export async function createAdminProperty(payload: {
  name: string;
  address: string;
  description?: string;
  status?: 'ACTIVE' | 'INACTIVE';
}): Promise<{ success: boolean; data: AdminProperty }> {
  const res = await api.post('/api/v1/admin/properties', payload);
  return res.data;
}

// ── SCR-48: Edit Property ──────────────────────────────────────────────────────

/** GET /api/v1/admin/properties/{id} — SCR-48 pre-fill */
export async function getAdminProperty(id: string): Promise<{ success: boolean; data: AdminProperty }> {
  const res = await api.get(`/api/v1/admin/properties/${id}`);
  return res.data;
}

/** PUT /api/v1/admin/properties/{id} — SCR-48 update */
export async function updateAdminProperty(
  id: string,
  payload: {
    name?: string;
    address?: string;
    description?: string | null;
    status?: 'ACTIVE' | 'INACTIVE';
  },
): Promise<{ success: boolean; data: AdminProperty }> {
  const res = await api.put(`/api/v1/admin/properties/${id}`, payload);
  return res.data;
}

// ── SCR-49: Manager Assignment ─────────────────────────────────────────────────

/** PATCH /api/v1/admin/properties/{id}/manager */
export async function assignManagerToProperty(propertyId: string, managerId: string): Promise<{ success: boolean }> {
  const res = await api.patch(`/api/v1/admin/properties/${propertyId}/manager`, { managerId });
  return res.data;
}

/** GET /api/v1/admin/properties/{id}/assignments — history */
export async function getPropertyAssignmentHistory(
  propertyId: string,
  params?: { page?: number; size?: number },
): Promise<{ success: boolean; data: PageResponse<{ id: string; managerName: string; status: string; assignedAt: string }> }> {
  const res = await api.get(`/api/v1/admin/properties/${propertyId}/assignments`, { params });
  return res.data;
}

// ── SCR-50: Manager Directory ──────────────────────────────────────────────────

/** GET /api/admin/users?role=MANAGER */
export async function getManagers(params?: { page?: number; size?: number; keyword?: string; status?: string }): Promise<{ success: boolean; data: PageResponse<AdminUser> }> {
  const res = await api.get('/api/admin/users', { params: { role: 'MANAGER', ...params } });
  return res.data;
}

/** POST /api/admin/users/managers — SCR-50 Create Manager */
export async function createManager(payload: {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
}): Promise<{ success: boolean; message?: string; data: AdminUser }> {
  const res = await api.post('/api/admin/users/managers', payload);
  return res.data;
}

/** PUT /api/admin/users/managers/{id} — SCR-50 Edit Manager */
export async function updateManager(
  id: string,
  payload: {
    fullName: string;
    email: string;
    phone?: string;
    password?: string;
    status?: string;
  },
): Promise<{ success: boolean; message?: string; data: AdminUser }> {
  const res = await api.put(`/api/admin/users/managers/${id}`, payload);
  return res.data;
}

// ── SCR-51: Customer Directory ─────────────────────────────────────────────────

/** GET /api/admin/users?role=CUSTOMER */
export async function getCustomers(params?: { page?: number; size?: number; keyword?: string; status?: string }): Promise<{ success: boolean; data: PageResponse<AdminUser> }> {
  const res = await api.get('/api/admin/users', { params: { role: 'CUSTOMER', ...params } });
  return res.data;
}

/** GET /api/admin/users/{id} */
export async function getAdminUserById(id: string): Promise<{ success: boolean; data: AdminUser }> {
  const res = await api.get(`/api/admin/users/${id}`);
  return res.data;
}

/** GET /api/admin/users/{id}/bookings — SCR-51 drawer history */
export async function getCustomerBookings(
  id: string,
  params?: { page?: number; size?: number },
): Promise<{ success: boolean; data: PageResponse<AdminCustomerBooking> }> {
  const res = await api.get(`/api/admin/users/${id}/bookings`, { params });
  return res.data;
}

/** PUT /api/admin/users/{id} — Manager ACTIVE/INACTIVE or Customer ACTIVE/SUSPENDED */
export async function updateAdminUser(
  id: string,
  payload: { status: string },
): Promise<{ success: boolean; data?: AdminUser }> {
  const res = await api.put(`/api/admin/users/${id}`, payload);
  return res.data;
}

/** PATCH /api/admin/users/{id}/status — FR-09 Customer Suspend/Activate */
export async function updateCustomerStatus(
  id: string,
  status: 'ACTIVE' | 'SUSPENDED',
): Promise<{ success: boolean; data: AdminUser }> {
  const res = await api.patch(`/api/admin/users/${id}/status`, { status });
  return res.data;
}

// ── SCR-52: Payment Reconciliation ────────────────────────────────────────────

/** GET /api/admin/payments/reconciliation?status=DISCREPANCY */
export async function getPaymentReconciliation(params?: { status?: string; page?: number; size?: number }): Promise<{ success: boolean; data: PageResponse<PaymentReconciliationItem> }> {
  const res = await api.get('/api/admin/payments/reconciliation', { params });
  return res.data;
}

// ── SCR-53: Damage Escalation ──────────────────────────────────────────────────

/** GET /api/admin/damage-reports?status=ESCALATED */
export async function getEscalatedDamageReports(params?: { page?: number; size?: number }): Promise<{ success: boolean; data: PageResponse<AdminDamageReport> }> {
  const res = await api.get('/api/admin/damage-reports', { params: { status: 'ESCALATED', ...params } });
  return res.data;
}

/** PATCH /api/admin/damage-reports/{id}/co-approve */
export async function coApproveDamageReport(id: string, approvedFee: number): Promise<{ success: boolean }> {
  const res = await api.patch(`/api/admin/damage-reports/${id}/co-approve`, { approvedFee });
  return res.data;
}

// ── SCR-54: Complaint Management ──────────────────────────────────────────────

/** GET /api/v1/admin/complaints */
export async function getAdminComplaints(params?: { page?: number; size?: number; status?: string }): Promise<{ success: boolean; data: PageResponse<AdminComplaint> }> {
  const res = await api.get('/api/v1/admin/complaints', { params });
  return res.data;
}

/** GET /api/v1/admin/complaints/{id} */
export async function getAdminComplaintDetail(id: string): Promise<{ success: boolean; data: AdminComplaint }> {
  const res = await api.get(`/api/v1/admin/complaints/${id}`);
  return res.data;
}

/** PUT /api/v1/admin/complaints/{id}/status */
export async function updateAdminComplaintStatus(id: string, payload: { status: string; resolution?: string }): Promise<{ success: boolean }> {
  const res = await api.put(`/api/v1/admin/complaints/${id}/status`, payload);
  return res.data;
}

/** PATCH /api/v1/admin/complaints/{id}/resolve */
export async function resolveComplaint(id: string, resolution: string): Promise<{ success: boolean }> {
  const res = await api.patch(`/api/v1/admin/complaints/${id}/resolve`, { resolution });
  return res.data;
}

// ── SCR-55: Global Reports ─────────────────────────────────────────────────────

/** GET /api/admin/reports/revenue?year=&propertyId= */
export async function getGlobalRevenueReport(
  year: number,
  propertyId?: string,
): Promise<{ success: boolean; data: { monthlyData: MonthlyRevenue[] } }> {
  const res = await api.get('/api/admin/reports/revenue', {
    params: { year, ...(propertyId ? { propertyId } : {}) },
  });
  return res.data;
}

/** GET /api/admin/reports/occupancy?year=&propertyId= */
export async function getAdminOccupancyReport(
  year: number,
  propertyId: string,
): Promise<{ success: boolean; data: OccupancyReportData }> {
  const res = await api.get('/api/admin/reports/occupancy', { params: { year, propertyId } });
  return res.data;
}

/** GET /api/admin/reports/booking-trends?year=&propertyId= */
export async function getAdminBookingTrends(
  year: number,
  propertyId: string,
): Promise<{ success: boolean; data: BookingTrendData }> {
  const res = await api.get('/api/admin/reports/booking-trends', { params: { year, propertyId } });
  return res.data;
}

// ── SCR-56: System Administration ─────────────────────────────────────────────

/** GET /api/v1/admin/settings */
export async function getSystemSettings(): Promise<{ success: boolean; data: SystemSettings }> {
  const res = await api.get('/api/v1/admin/settings');
  return res.data;
}

/** PUT /api/v1/admin/settings */
export async function updateSystemSettings(payload: Partial<SystemSettings>): Promise<{ success: boolean; data: SystemSettings }> {
  const res = await api.put('/api/v1/admin/settings', payload);
  return res.data;
}

// ── SCR-57: Promotion Management ──────────────────────────────────────────────

/** GET /api/v1/admin/promotions (also /api/admin/promotions) */
export async function getAdminPromotions(params?: { page?: number; size?: number }): Promise<{ success: boolean; data: PageResponse<Promotion> }> {
  const res = await api.get('/api/v1/admin/promotions', { params });
  return res.data;
}

/** POST /api/v1/admin/promotions — SCR-58 */
export async function createPromotion(payload: PromotionPayload): Promise<{ success: boolean; data: Promotion }> {
  const res = await api.post('/api/v1/admin/promotions', payload);
  return res.data;
}

/** PUT /api/v1/admin/promotions/{id} — SCR-58 */
export async function updatePromotion(id: string, payload: Partial<PromotionPayload>): Promise<{ success: boolean }> {
  const res = await api.put(`/api/v1/admin/promotions/${id}`, payload);
  return res.data;
}

/** DELETE /api/v1/admin/promotions/{id} */
export async function deletePromotion(id: string): Promise<{ success: boolean }> {
  const res = await api.delete(`/api/v1/admin/promotions/${id}`);
  return res.data;
}

// ── Legacy export (backward compat with any existing imports) ──────────────────
export const adminApi = {
  searchUsers:  (params: { page?: number; size?: number; role?: string; status?: string; keyword?: string }) =>
    getCustomers(params),
  getUserById:  getAdminUserById,
  updateUser:   (id: string, payload: { status: string }) => updateAdminUser(id, payload),
  updateCustomerStatus,
  getCustomerBookings,
};
