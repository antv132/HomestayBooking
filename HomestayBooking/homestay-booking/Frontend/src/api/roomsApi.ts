import api from './axiosInstance';

export interface RoomListItem {
  id: string;
  roomNumber: string;
  roomType: string;
  pricePerNight: number;
  capacity: number;
  area: number;
  status: string;
  primaryImageUrl: string | null;
  propertyId: string;
  propertyName: string;
  floorId?: string;
  floorNumber?: number | null;
  averageRating?: number | null;
  totalReviews?: number | null;
}

export interface RoomsPageResponse {
  content: RoomListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PropertyOption {
  id: string;
  name: string;
  address: string;
}

export interface FetchRoomsParams {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  location?: string;
  propertyId?: string;
  floorId?: string;      // SCR-39: filter by floor
  roomType?: string;
  minPrice?: number;
  maxPrice?: number;
  capacity?: number;
  checkIn?: string;
  checkOut?: string;
  status?: string;
}

export const ROOM_TYPES = ['Studio', 'Standard', 'Deluxe', 'Suite', 'Villa'] as const;

export function sortToApi(sort: string): string {
  switch (sort) {
    case 'price-asc':
      return 'pricePerNight,asc';
    case 'price-desc':
      return 'pricePerNight,desc';
    default:
      return 'createdAt,desc';
  }
}

/** SCR-07 — public room search (contract: GET /api/v1/rooms) */
export async function fetchRooms(params: FetchRoomsParams): Promise<RoomsPageResponse> {
  const { capacity, status, ...rest } = params;
  const res = await api.get('/api/v1/rooms', {
    params: {
      ...rest,
      guests: capacity,
      status: status ?? 'AVAILABLE',
    },
  });
  return res.data.data;
}

// SCR-39: Manager-only room list with full filter support
/** @deprecated SCR-29 — dùng fetchManagerRoomsV1 thay thế */
export async function fetchRoomsManager(params: FetchRoomsParams): Promise<RoomsPageResponse> {
  const res = await api.get('/api/rooms/manager', { params });
  return res.data.data;
}

/** SCR-29 — Manager room list (v1, property-scoped) */
export async function fetchManagerRoomsV1(params: FetchRoomsParams): Promise<RoomsPageResponse> {
  const clean: Record<string, string | number> = {};
  if (params.page != null) clean.page = params.page;
  if (params.size != null) clean.size = params.size;
  if (params.search) clean.search = params.search;
  if (params.propertyId) clean.propertyId = params.propertyId;
  if (params.floorId) clean.floorId = params.floorId;
  if (params.status) clean.status = params.status;
  if (params.roomType) clean.roomType = params.roomType;
  const res = await api.get('/api/v1/manager/rooms', { params: clean });
  return res.data.data;
}

// SCR-39: Delete room (only allowed if no active bookings)
/** @deprecated SCR-29 — dùng deleteRoomV1 (soft-delete) */
export async function deleteRoom(id: string): Promise<void> {
  await api.delete(`/api/rooms/${id}`);
}

/** SCR-29 — soft-delete room (v1, property-scoped) */
export async function deleteRoomV1(id: string): Promise<void> {
  await api.delete(`/api/v1/manager/rooms/${id}`);
}

export async function fetchPropertyOptions(): Promise<PropertyOption[]> {
  const res = await api.get('/api/properties', {
    params: { page: 0, size: 100, status: 'ACTIVE' },
  });
  return res.data.data.content.map((p: { id: string; name: string; address: string }) => ({
    id: p.id,
    name: p.name,
    address: p.address,
  }));
}

export interface RoomImageInfo {
  id: string;
  imageUrl: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface RoomReviewInfo {
  id: string;
  customerName: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface RoomDetail {
  id: string;
  propertyId: string;
  propertyName: string;
  propertyAddress: string;
  floorId: string;
  floorNumber: number;
  roomNumber: string;
  roomType: string;
  pricePerNight: number;
  capacity: number;
  area: number;
  description: string;
  status: string;
  images: RoomImageInfo[];
  amenities: string[];
  averageRating: number;
  totalReviews: number;
  reviews: RoomReviewInfo[];
  createdAt: string;
}

export async function fetchRoomById(id: string): Promise<RoomDetail> {
  const res = await api.get(`/api/v1/rooms/${id}`);
  return res.data.data;
}

export interface BookedRange {
  checkIn: string;
  checkOut: string;
  bookingStatus: string;
}

export interface RoomCalendarData {
  roomStatus: string;
  bookedRanges: BookedRange[];
}

export async function fetchRoomCalendar(id: string): Promise<RoomCalendarData> {
  const res = await api.get(`/api/v1/rooms/${id}/calendar`);
  return res.data.data;
}

export interface MonthAvailabilityDay {
  date: string;
  status: string;
  bookable: boolean;
}

export interface MonthAvailabilityData {
  bookedDates: string[];
  maintenanceDates: string[];
  days?: MonthAvailabilityDay[];
}

/** SCR-09 — month view availability */
export async function fetchRoomMonthAvailability(
  id: string,
  startDate: string,
  endDate: string,
): Promise<MonthAvailabilityData> {
  const res = await api.get(`/api/v1/rooms/${id}/availability`, {
    params: { startDate, endDate },
  });
  return res.data.data;
}

export interface RoomReviewsPage {
  content: RoomReviewInfo[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function fetchRoomReviews(id: string, page = 0, size = 5): Promise<RoomReviewsPage> {
  const res = await api.get(`/api/v1/rooms/${id}/reviews`, { params: { page, size } });
  return res.data.data;
}

export async function fetchPriceStats(): Promise<{ minPrice: number; maxPrice: number }> {
  const res = await api.get('/api/v1/rooms/price-stats');
  return res.data.data;
}

export async function checkRoomAvailability(
  id: string,
  checkIn: string,
  checkOut: string,
): Promise<{ available: boolean; bookedRanges: BookedRange[] }> {
  const res = await api.get(`/api/v1/rooms/${id}/availability`, { params: { checkIn, checkOut } });
  return res.data.data;
}

// ── SCR-40: Booking history for a room (Manager) ─────────────────────────────

export interface RoomBookingSummary {
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
}

export interface RoomBookingsPage {
  content: RoomBookingSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export async function fetchRoomBookings(
  roomId: string,
  page = 0,
  size = 5,
): Promise<RoomBookingsPage> {
  const res = await api.get(`/api/rooms/${roomId}/bookings`, {
    params: { page, size, sort: 'checkInDate,desc' },
  });
  return res.data.data;
}

// ── Manager room write operations ─────────────────────────────────────────────

export interface CreateRoomPayload {
  propertyId: string;
  floorId: string;
  roomNumber: string;
  roomType?: string;
  pricePerNight: number;
  capacity: number;
  area?: number;
  description?: string;
  amenities?: string[];
}

export interface UpdateRoomPayload {
  floorId?: string;
  roomNumber?: string;
  roomType?: string;
  pricePerNight?: number;
  capacity?: number;
  area?: number | null;
  description?: string | null;
  amenities?: string[];
}

/** @deprecated Use createRoomV1 */
export async function createRoom(payload: CreateRoomPayload): Promise<RoomDetail> {
  const res = await api.post('/api/rooms', payload);
  return res.data.data;
}

export async function createRoomV1(payload: CreateRoomPayload): Promise<RoomDetail> {
  const res = await api.post('/api/v1/manager/rooms', payload);
  return res.data.data;
}

export async function fetchManagerRoomV1(id: string): Promise<RoomDetail> {
  const res = await api.get(`/api/v1/manager/rooms/${id}`);
  return res.data.data;
}

/** @deprecated Use updateRoomV1 */
export async function updateRoom(id: string, payload: UpdateRoomPayload): Promise<RoomDetail> {
  const res = await api.put(`/api/rooms/${id}`, payload);
  return res.data.data;
}

export async function updateRoomV1(id: string, payload: UpdateRoomPayload): Promise<RoomDetail> {
  const res = await api.put(`/api/v1/manager/rooms/${id}`, payload);
  return res.data.data;
}

export interface UpdateRoomStatusPayload {
  status: 'AVAILABLE' | 'MAINTENANCE' | 'OUT_OF_SERVICE';
  startDate?: string;
  endDate?: string;
  reason?: string;
  note?: string;
}

/** @deprecated Use updateRoomStatusV1 */
export async function updateRoomStatus(
  id: string,
  payload: UpdateRoomStatusPayload,
): Promise<RoomDetail> {
  const res = await api.patch(`/api/rooms/${id}/status`, payload);
  return res.data.data;
}

export async function updateRoomStatusV1(
  id: string,
  payload: UpdateRoomStatusPayload,
): Promise<RoomDetail> {
  const res = await api.patch(`/api/v1/manager/rooms/${id}/status`, payload);
  return res.data.data;
}

/** FR-05 US2 — Manager property-scoped month calendar (same days[] as SCR-09). */
export interface StaffBookingRangeRef {
  bookingId: string;
  checkIn: string;
  checkOut: string;
  bookingStatus: string;
}

export interface StaffRoomMonthCalendar {
  roomId: string;
  roomNumber: string;
  roomStatus: string;
  days: MonthAvailabilityDay[];
  bookedDates: string[];
  maintenanceDates: string[];
  bookedRanges: StaffBookingRangeRef[];
}

export async function fetchManagerRoomCalendar(
  id: string,
  params: { month: number; year: number } | { startDate: string; endDate: string },
): Promise<StaffRoomMonthCalendar> {
  const res = await api.get(`/api/v1/manager/rooms/${id}/calendar`, { params });
  return res.data.data;
}

export interface RoomStatusBlockItem {
  id: string;
  status: string;
  startDate: string;
  endDate: string;
  reason: string | null;
}

/** FR-05 T036 — active Maintenance / OOS blocks. */
export async function fetchRoomStatusBlocks(id: string): Promise<RoomStatusBlockItem[]> {
  const res = await api.get(`/api/v1/manager/rooms/${id}/status-blocks`);
  return res.data.data;
}


