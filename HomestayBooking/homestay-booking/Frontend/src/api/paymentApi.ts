import api from './axiosInstance';

export interface PaymentSummaryResponse {
  id: string;
  bookingId: string;
  customerName: string;
  type: string;
  method: string;
  amount: number;
  status: string;
  paidAt?: string | null;
  createdAt: string;
}

export interface PaymentPageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PaymentDetailResponse {
  id: string;
  bookingId: string;
  customerId: string;
  customerName: string;
  type: string;
  method: string;
  amount: number;
  status: string;
  orderRef?: string | null;
  gatewayTransactionId?: string | null;
  gatewayResponseCode?: string | null;
  verifiedByName: string | null;
  verifiedAt: string | null;
  paidAt: string | null;
  verificationNote: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ManagerPaymentsParams {
  propertyId?: string;
  page?: number;
  size?: number;
  status?: string;
  type?: string;
  method?: string;
  search?: string;
  sort?: string;
}

export async function fetchManagerPaymentsV1(
  params: ManagerPaymentsParams,
): Promise<PageResponse<PaymentSummaryResponse>> {
  const res = await api.get('/api/v1/manager/payments', { params });
  return res.data.data;
}

export const paymentApi = {
  getPaymentDetail: async (id: string): Promise<{ success: boolean; data: PaymentDetailResponse }> => {
    const res = await api.get(`/api/v1/manager/payments/${id}`);
    return res.data;
  },

  createVnpayUrl: async (
    bookingId: string,
    type: 'DEPOSIT' | 'REMAINING_BALANCE' | 'DAMAGE_FEE',
  ): Promise<{
    success: boolean;
    data: { paymentUrl: string; paymentId?: string; orderRef?: string };
  }> => {
    const res = await api.post(`/api/v1/payments/vnpay/create-url?bookingId=${bookingId}&type=${type}`);
    return res.data;
  },

  getMyPayments: async (params?: {
    page?: number;
    size?: number;
    status?: string;
  }): Promise<{
    success: boolean;
    message?: string;
    data: PaymentPageResponse<PaymentSummaryResponse>;
  }> => {
    const res = await api.get('/api/v1/payments/me', { params });
    return res.data;
  },

  getMyPaymentDetail: async (
    id: string,
  ): Promise<{ success: boolean; data: PaymentDetailResponse }> => {
    const res = await api.get(`/api/v1/payments/me/${id}`);
    return res.data;
  },
};
