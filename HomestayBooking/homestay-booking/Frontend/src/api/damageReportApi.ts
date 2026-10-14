import api from './axiosInstance';

export interface CustomerDamageReport {
  id: string;
  bookingId: string;
  roomNumber: string;
  propertyName: string;
  status: string;
  totalEstimatedCost: number | null;
  approvedAmount: number | null;
  approvedAt: string | null;
  disputeDeadline: string | null;
  disputeEligible: boolean;
  disputeNote: string | null;
  disputedAt: string | null;
  note: string | null;
  createdAt: string;
}

interface ApiEnvelope<T> {
  success: boolean;
  message?: string;
  data: T;
}

export const damageReportApi = {
  async listMine(): Promise<ApiEnvelope<CustomerDamageReport[]>> {
    const res = await api.get<ApiEnvelope<CustomerDamageReport[]>>(
      '/api/v1/customer/damage-reports',
    );
    return res.data;
  },

  async getMine(id: string): Promise<ApiEnvelope<CustomerDamageReport>> {
    const res = await api.get<ApiEnvelope<CustomerDamageReport>>(
      `/api/v1/customer/damage-reports/${id}`,
    );
    return res.data;
  },

  async dispute(id: string, note: string): Promise<ApiEnvelope<CustomerDamageReport>> {
    const res = await api.patch<ApiEnvelope<CustomerDamageReport>>(
      `/api/v1/customer/damage-reports/${id}/dispute`,
      { note },
    );
    return res.data;
  },
};
