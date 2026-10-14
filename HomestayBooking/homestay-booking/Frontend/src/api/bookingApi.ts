import api from './axiosInstance';

export interface BookingSummary {
  bookingId: string;
  roomId: string;
  roomNumber: string;
  propertyId: string;
  propertyName: string;
  checkInDate: string;
  checkOutDate: string;
  status: string;
}

export interface BookingSummaryResponse {
  id: string;
  customerName: string;
  customerEmail: string;
  roomNumber: string;
  roomType: string;
  propertyName: string;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalAmount: number;
  status: string;
  createdAt: string;
  isReviewed?: boolean;
}

export interface BookingPaymentInfo {
  id: string;
  type: string;
  amount: number;
  method: string;
  status: string;
  paidAt: string | null;
  verificationNote?: string | null;
}

export interface BookingDetailResponse {
  id: string;
  customerId: string;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  roomNumber: string;
  roomType: string;
  propertyName: string;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalAmount: number;
  depositAmount: number;
  remainingAmount: number;
  damageFeeAmount?: number;
  status: string;
  specialRequests: string;
  /** Deposit hold deadline (ISO); present when PENDING_DEPOSIT */
  holdExpiresAt?: string | null;
  createdAt: string;
  isReviewed?: boolean;
  payments?: BookingPaymentInfo[];
  invoiceBreakdown?: {
    invoices: Array<{
      id: string;
      bookingId: string;
      type: string;
      amount: number;
      status: string;
      dueDate: string;
      paidAt: string | null;
      overdue: boolean;
      createdAt: string;
    }>;
    totalAmount: number;
    paidAmount: number;
    remainingBalance: number;
  };
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateBookingPayload {
  roomId: string;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  specialRequests?: string;
}

export interface CancellationPreview {
  daysUntilCheckIn: number;
  refundPercent: number;
  refundAmount: number;
  forfeitAmount: number;
  policyText: string;
}

export const bookingApi = {
  getMyActiveBookings: async (): Promise<{ success: boolean; data: BookingSummary[] }> => {
    const res = await api.get('/api/bookings/my-active');
    return res.data;
  },
  createBooking: async (payload: CreateBookingPayload): Promise<{ success: boolean; message: string; data: BookingDetailResponse }> => {
    const res = await api.post('/api/v1/bookings', payload);
    return res.data;
  },
  getMyBookings: async (params: { page?: number; size?: number; status?: string; sort?: string }): Promise<{ success: boolean; data: PageResponse<BookingSummaryResponse> }> => {
    const res = await api.get('/api/v1/bookings/me', { params });
    return res.data;
  },
  /** @deprecated Use fetchManagerBookingsV1 for manager list (SCR-34) */
  getAllBookings: async (params: { page?: number; size?: number; status?: string; search?: string; sort?: string }): Promise<{ success: boolean; data: PageResponse<BookingSummaryResponse> }> => {
    const res = await api.get('/api/bookings', { params });
    return res.data;
  },
  /** @deprecated Use fetchManagerBookingV1 for manager detail (SCR-35) */
  getBookingDetail: async (id: string): Promise<{ success: boolean; data: BookingDetailResponse }> => {
    const res = await api.get(`/api/bookings/${id}`);
    return res.data;
  },
  getMyBookingDetail: async (id: string): Promise<{ success: boolean; data: BookingDetailResponse; message?: string }> => {
    const res = await api.get(`/api/v1/bookings/me/${id}`);
    return res.data;
  },
  /** Prefer cancelMyBooking — customer cancel must use v1 path. */
  cancelBooking: async (id: string, reason?: string): Promise<{ success: boolean; message?: string }> => {
    const res = await api.patch(`/api/v1/bookings/me/${id}/cancel`, reason ? { reason } : {});
    return res.data;
  },
  getCancellationPreview: async (id: string): Promise<{ success: boolean; data: CancellationPreview }> => {
    const res = await api.get(`/api/v1/bookings/me/${id}/cancel/preview`);
    return res.data;
  },
  cancelMyBooking: async (id: string, reason?: string): Promise<{ success: boolean; message: string }> => {
    const res = await api.patch(`/api/v1/bookings/me/${id}/cancel`, reason ? { reason } : {});
    return res.data;
  },
};

export interface ManagerBookingsParams {
  propertyId?: string;
  page?: number;
  size?: number;
  status?: string;
  search?: string;
  checkInFrom?: string;
  checkInTo?: string;
  sort?: string;
}

export async function fetchManagerBookingsV1(
  params: ManagerBookingsParams,
): Promise<PageResponse<BookingSummaryResponse>> {
  const res = await api.get('/api/v1/manager/bookings', { params });
  return res.data.data;
}

export interface ManagerBookingDetail extends BookingDetailResponse {
  roomId?: string;
  damageFeeAmount?: number;
  canCheckIn?: boolean;
  canCheckOut?: boolean;
  canCancel?: boolean;
  canModify?: boolean;
  checkInBlockedReason?: string | null;
  checkOutBlockedReason?: string | null;
}

export interface ModifyBookingRequest {
  checkIn?: string;
  checkOut?: string;
  roomId?: string;
}

export interface ModifyBookingResponse {
  booking: ManagerBookingDetail;
  priceDelta: number;
  message: string;
}

export async function fetchManagerBookingV1(id: string): Promise<ManagerBookingDetail> {
  const res = await api.get(`/api/v1/manager/bookings/${id}`);
  return res.data.data;
}

/** FR-015 — Modify Confirmed booking dates/room. */
export async function modifyManagerBookingV1(
  id: string,
  body: ModifyBookingRequest,
): Promise<ModifyBookingResponse> {
  const res = await api.patch(`/api/v1/manager/bookings/${id}`, body);
  return res.data.data;
}

export async function getManagerCancellationPreview(
  id: string,
): Promise<{ success: boolean; data: CancellationPreview }> {
  const res = await api.get(`/api/v1/manager/bookings/${id}/cancel/preview`);
  return res.data;
}

/** SCR-69 — Manager cancel with required cancelReason (100% refund). */
export async function cancelManagerBookingV1(
  id: string,
  cancelReason: string,
): Promise<ManagerBookingDetail> {
  const res = await api.post(`/api/v1/manager/bookings/${id}/cancel`, { cancelReason });
  return res.data.data;
}

export async function checkInBookingV1(
  id: string,
  payload: ManagerCheckInPayload,
): Promise<ManagerBookingDetail> {
  const res = await api.patch(`/api/v1/manager/bookings/${id}/check-in`, payload);
  return res.data.data;
}

export async function checkOutBookingV1(
  id: string,
  payload: ManagerCheckOutPayload,
): Promise<ManagerBookingDetail> {
  const res = await api.patch(`/api/v1/manager/bookings/${id}/check-out`, payload);
  return res.data.data;
}

export interface ManagerCheckInPayload {
  idDocumentUrls: string[];
  keyHandedOver: boolean;
  remainingCollected?: boolean;
  note?: string;
}

export interface ManagerCheckOutPayload {
  keyReturned: boolean;
  note?: string;
}

export async function uploadBookingIdDocumentsV1(
  bookingId: string,
  files: File[],
): Promise<string[]> {
  const formData = new FormData();
  files.forEach(f => formData.append('files', f));
  const res = await api.post(
    `/api/v1/manager/bookings/${bookingId}/id-documents`,
    formData,
  );
  return res.data.data ?? [];
}
