import api from './axiosInstance';

export interface InvoiceSummary {
  id: string;
  bookingId: string;
  type: string;
  amount: number;
  status: string;
  dueDate: string;
  paidAt: string | null;
  overdue: boolean;
  createdAt: string;
}

export interface InvoiceBreakdown {
  invoices: InvoiceSummary[];
  totalAmount: number;
  paidAmount: number;
  remainingBalance: number;
}

export interface InvoicePage {
  content: InvoiceSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function getMyInvoices(params?: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<InvoicePage> {
  const res = await api.get('/api/v1/invoices/me', { params });
  return res.data.data;
}

export async function getMyInvoiceDetail(id: string) {
  const res = await api.get(`/api/v1/invoices/me/${id}`);
  return res.data.data;
}

export async function getManagerInvoices(params: {
  propertyId: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<InvoicePage> {
  const res = await api.get('/api/v1/manager/invoices', { params });
  return res.data.data;
}

export const invoiceApi = {
  getMyInvoices,
  getMyInvoiceDetail,
  getManagerInvoices,
};
