import api from './axiosInstance';
import type { BookingSummaryResponse } from './bookingApi';

export interface UpcomingEvent {
  bookingId: string;
  roomNumber: string;
  propertyName: string;
  date: string;
  daysUntil: number;
}

export interface PaymentSummary {
  id: string;
  type: string;
  amount: number;
  status: string;
  createdAt: string;
}

export interface NotificationSummary {
  id: string;
  title: string;
  content: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

export interface CustomerDashboardData {
  activeBookings: number;
  pendingPayments: number;
  openTickets: number;
  unreadNotifications: number;
  upcomingCheckIn: UpcomingEvent | null;
  upcomingCheckOut: UpcomingEvent | null;
  upcomingBookings: BookingSummaryResponse[];
  recentPayments: PaymentSummary[];
  recentNotifications: NotificationSummary[];
}

export async function fetchCustomerDashboard(): Promise<CustomerDashboardData> {
  const res = await api.get('/api/v1/customer/dashboard');
  return res.data.data;
}
