import api from './axiosInstance';
import type { ApiResponse } from './authApi';

// ── Types ─────────────────────────────────────────────────────────────────────

/** Params cho GET /api/reports/revenue (SCR-59) */
export interface RevenueReportParams {
  /** UUID property, undefined = tất cả */
  propertyId?: string;
  /** yyyy-MM-dd — ngày bắt đầu */
  from?: string;
  /** yyyy-MM-dd — ngày kết thúc */
  to?: string;
  /** "month" (default) | "week" */
  groupBy?: 'month' | 'week';
}

/** Doanh thu theo 1 kỳ (tháng: "2026-01" | tuần: "2026-W22") */
export interface PeriodRevenue {
  period: string;
  revenue: number;
  bookingCount: number;
}

/** Doanh thu tổng hợp theo property */
export interface PropertyRevenue {
  propertyId: string;
  propertyName: string;
  revenue: number;
  bookingCount: number;
}

/** Response data từ GET /api/reports/revenue */
export interface RevenueReportData {
  /** Tổng doanh thu (DEPOSIT + REMAINING_BALANCE, PAID) */
  totalRevenue: number;
  /** Doanh thu đặt cọc 40% */
  depositRevenue: number;
  /** Doanh thu phần còn lại 60% */
  balanceRevenue: number;
  /** Số booking phân biệt có payment PAID */
  totalBookingCount: number;
  /** Doanh thu theo kỳ — dùng cho Bar Chart */
  byPeriod: PeriodRevenue[];
  /** Doanh thu theo property — dùng cho Horizontal Bar */
  byProperty: PropertyRevenue[];
}

/** SCR-27 — KPI dashboard theo property */
export interface PropertyKpis {
  propertyId: string;
  totalRooms: number;
  occupancyRate: number;
  revenue: number;
  pendingCheckIns: number;
  pendingApprovals: number;
}

/** SCR-27 — Phân bổ booking theo trạng thái (pie chart) */
export interface BookingStatusCount {
  status: string;
  count: number;
}

/** Property được gán cho Manager (selector) */
export interface AssignedProperty {
  id: string;
  name: string;
  address?: string | null;
  status: string;
}

// ── SCR-44: Property Reports (Occupancy + Booking Trend) ────────────────────────

/** Params chung cho báo cáo theo property + khoảng ngày (SCR-44) */
export interface PropertyReportParams {
  propertyId: string;
  /** yyyy-MM-dd */
  from: string;
  /** yyyy-MM-dd */
  to: string;
  groupBy?: 'month' | 'week';
}

/** Tỷ lệ lấp đầy theo 1 kỳ */
export interface PeriodOccupancy {
  period: string;
  occupancyRate: number;
  occupiedRoomNights: number;
  availableRoomNights: number;
}

/** Response GET /api/v1/reports/occupancy */
export interface OccupancyReportData {
  totalRooms: number;
  avgOccupancyRate: number;
  totalOccupiedRoomNights: number;
  totalAvailableRoomNights: number;
  byPeriod: PeriodOccupancy[];
}

/** Số booking theo 1 kỳ */
export interface PeriodBookingCount {
  period: string;
  bookingCount: number;
}

/** Response GET /api/v1/reports/booking-trends */
export interface BookingTrendData {
  totalBookings: number;
  byPeriod: PeriodBookingCount[];
  byStatus: { status: string; count: number }[];
}

// ── API ───────────────────────────────────────────────────────────────────────

export const reportApi = {
  /**
   * GET /api/reports/revenue
   * SCR-59 — Báo cáo doanh thu cho Manager.
   * Yêu cầu role MANAGER.
   */
  getRevenue: async (params: RevenueReportParams = {}): Promise<ApiResponse<RevenueReportData>> => {
    const cleanParams: Record<string, string> = {};
    if (params.propertyId) cleanParams.propertyId = params.propertyId;
    if (params.from)       cleanParams.from       = params.from;
    if (params.to)         cleanParams.to         = params.to;
    if (params.groupBy)    cleanParams.groupBy    = params.groupBy;

    // Prefer v1 when propertyId present (scoped); legacy allows “all assigned”
    const path = params.propertyId ? '/api/v1/reports/revenue' : '/api/reports/revenue';
    const res = await api.get(path, { params: cleanParams });
    return res.data;
  },

  /** SCR-27 — KPI dashboard theo property */
  getPropertyKpis: async (propertyId: string): Promise<ApiResponse<PropertyKpis>> => {
    const res = await api.get('/api/v1/reports/property-kpis', { params: { propertyId } });
    return res.data;
  },

  /** SCR-27 — Phân bổ booking theo trạng thái tháng hiện tại */
  getBookingStatusBreakdown: async (propertyId: string): Promise<ApiResponse<BookingStatusCount[]>> => {
    const res = await api.get('/api/v1/reports/booking-status-breakdown', { params: { propertyId } });
    return res.data;
  },

  /** SCR-44 — Tỷ lệ lấp đầy theo kỳ cho 1 property */
  getOccupancy: async (params: PropertyReportParams): Promise<ApiResponse<OccupancyReportData>> => {
    const res = await api.get('/api/v1/reports/occupancy', {
      params: {
        propertyId: params.propertyId,
        from: params.from,
        to: params.to,
        groupBy: params.groupBy ?? 'month',
      },
    });
    return res.data;
  },

  /** SCR-44 — Xu hướng đặt phòng theo kỳ cho 1 property */
  getBookingTrends: async (params: PropertyReportParams): Promise<ApiResponse<BookingTrendData>> => {
    const res = await api.get('/api/v1/reports/booking-trends', {
      params: {
        propertyId: params.propertyId,
        from: params.from,
        to: params.to,
        groupBy: params.groupBy ?? 'month',
      },
    });
    return res.data;
  },
};
