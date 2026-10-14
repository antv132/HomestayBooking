import api from './axiosInstance';

// ── SCR-59: Employee Dashboard (FR-22 composite) ─────────────────────────────

export interface EmployeeKpis {
  pendingHousekeeping: number;
  pendingMaintenance: number;
  pendingInspections: number;
}

export interface HousekeepingTaskSummary {
  id: string;
  roomNumber?: string;
  propertyName?: string;
  status: string;
  createdAt?: string;
}

export interface MaintenanceTicketSummary {
  id: string;
  title?: string;
  roomNumber?: string;
  propertyName?: string;
  status: string;
  createdAt?: string;
}

export interface RoomInspectionSummary {
  id: string;
  bookingId?: string;
  roomNumber?: string;
  propertyName?: string;
  checkOutDate?: string;
  status: string;
}

export interface EmployeeTaskSummary {
  type: 'HOUSEKEEPING' | 'MAINTENANCE' | 'INSPECTION';
  id: string;
  roomNumber?: string;
  propertyName?: string;
  status: string;
  label?: string;
  checkOutDate?: string | null;
}

export interface AwaitingSummary {
  housekeepingCount: number;
  maintenanceCount: number;
  inspectionCount: number;
  previews: EmployeeTaskSummary[];
}

export interface CompletedTodaySummary {
  housekeepingCount: number;
  maintenanceCount: number;
  inspectionCount: number;
  previews: EmployeeTaskSummary[];
}

export interface EmployeeDashboardData {
  fullName: string;
  pendingHousekeeping: number;
  pendingMaintenance: number;
  pendingInspections: number;
  housekeepingTasks: HousekeepingTaskSummary[];
  maintenanceTickets: MaintenanceTicketSummary[];
  inspections: RoomInspectionSummary[];
  todayTasks: EmployeeTaskSummary[];
  awaiting: AwaitingSummary;
  completedToday: CompletedTodaySummary;
  noPropertyAssignment: boolean;
}

export async function getEmployeeDashboard(): Promise<{ success: boolean; data: EmployeeDashboardData }> {
  const res = await api.get('/api/v1/employee/dashboard');
  return res.data;
}

export async function getEmployeeKpis(): Promise<{ success: boolean; data: EmployeeKpis }> {
  const res = await api.get('/api/v1/employee/kpis');
  return res.data;
}

// ── SCR-60: Housekeeping Workspace ───────────────────────────────────────────

export interface HousekeepingTask {
  id: string;
  roomId?: string;
  roomNumber?: string;
  roomName?: string;
  floorName?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  note?: string | null;
  createdAt?: string;
  startedAt?: string | null;
  completedAt?: string | null;
}

export async function getHousekeepingTasks(params?: {
  page?: number;
  size?: number;
  status?: string;
}): Promise<{ success: boolean; data: { content: HousekeepingTask[]; totalPages: number } }> {
  const res = await api.get('/api/v1/employees/housekeeping', { params });
  return res.data;
}

export async function startHousekeepingTask(id: string): Promise<void> {
  await api.post(`/api/v1/employees/housekeeping/${id}/start`);
}

export async function finishHousekeepingTask(id: string): Promise<void> {
  await api.post(`/api/v1/employees/housekeeping/${id}/finish`);
}

/** @deprecated Prefer startHousekeepingTask / finishHousekeepingTask */
export async function updateHousekeepingTaskStatus(
  id: string,
  status: 'IN_PROGRESS' | 'COMPLETED',
): Promise<void> {
  if (status === 'IN_PROGRESS') {
    await startHousekeepingTask(id);
  } else {
    await finishHousekeepingTask(id);
  }
}

// ── SCR-61: Maintenance Workspace ────────────────────────────────────────────

export interface MaintenanceTicket {
  id: string;
  roomName: string;
  issueType: string;
  description: string;
  status: 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  assignedAt?: string;
  resolvedAt?: string | null;
}

export async function getEmployeeMaintenanceTickets(params?: {
  page?: number;
  size?: number;
  status?: string;
}): Promise<{ success: boolean; data: { content: MaintenanceTicket[]; totalPages: number } }> {
  const res = await api.get('/api/v1/employees/maintenance', { params });
  return res.data;
}

export async function updateMaintenanceTicketStatus(
  id: string,
  status: 'IN_PROGRESS' | 'RESOLVED',
): Promise<void> {
  await api.put(`/api/v1/employees/maintenance/${id}/status`, { status });
}

// ── SCR-62: Room Inspection Hub ──────────────────────────────────────────────

export interface ChecklistItemDefinition {
  id: string;
  code: string;
  label: string;
  icon?: string | null;
  sortOrder: number;
}

export interface ChecklistAnswerInput {
  itemId: string;
  passed: boolean;
}

export interface InspectionChecklistAnswer {
  id: string;
  itemId: string;
  code: string;
  label: string;
  icon?: string | null;
  passed: boolean;
}

/** @deprecated Prefer ChecklistItemDefinition + answers[] */
export interface InspectionChecklist {
  tv: boolean;
  minibar: boolean;
  ac: boolean;
  bathroom: boolean;
  beds: boolean;
}

export interface InspectionSummary {
  id: string;
  roomId: string;
  roomNumber?: string;
  roomName?: string;
  bookingId?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'PASSED' | 'FAILED_WITH_DAMAGE';
  inspectorId?: string | null;
  inspectorName?: string | null;
  createdAt?: string;
  note?: string | null;
}

export async function getEmployeeInspections(params?: {
  page?: number;
  size?: number;
  status?: string;
}): Promise<{ success: boolean; data: { content: InspectionSummary[]; totalPages: number } }> {
  const res = await api.get('/api/v1/employees/inspections', { params });
  return res.data;
}

export async function getChecklistItems(): Promise<ChecklistItemDefinition[]> {
  const res = await api.get('/api/v1/employees/inspections/checklist-items');
  return res.data.data ?? [];
}

export async function passInspection(
  id: string,
  body?: { note?: string; notes?: string; answers?: ChecklistAnswerInput[] },
): Promise<{ success: boolean; data?: InspectionSummary }> {
  const res = await api.post(`/api/v1/employees/inspections/${id}/pass`, body ?? {});
  return res.data;
}

export async function failInspection(
  id: string,
  body: { note: string; notes?: string; answers?: ChecklistAnswerInput[] },
): Promise<{ success: boolean; data?: InspectionSummary }> {
  const res = await api.post(`/api/v1/employees/inspections/${id}/fail`, body);
  return res.data;
}

// ── SCR-63/64: Damage Reports ────────────────────────────────────────────────

export interface DamageItem {
  name: string;
  estimatedCost: number;
}

export interface DamageReport {
  id: string;
  bookingId?: string;
  roomName: string;
  status: string;
  items: DamageItem[];
  itemCount?: number;
  totalCost: number;
  requiresAdminEscalation?: boolean;
  note?: string | null;
  createdAt: string;
}

export async function getEmployeeDamageReports(params?: {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
}): Promise<{ success: boolean; data: { content: DamageReport[]; totalPages: number; totalElements?: number } }> {
  const res = await api.get('/api/v1/employees/damage-reports', { params });
  return res.data;
}

export async function createDamageReport(payload: {
  roomId: string;
  inspectionId?: string;
  items: DamageItem[];
  attachments?: { url: string; type: string }[];
  notes?: string;
}): Promise<{ success: boolean }> {
  const res = await api.post('/api/v1/employees/damage-reports', payload);
  return res.data;
}

/** SCR-64 — Upload evidence photos; returns `/uploads/damage/...` URLs. */
export async function uploadDamagePhotos(
  files: File[],
): Promise<{ success: boolean; data: string[]; message?: string }> {
  const form = new FormData();
  files.forEach((f) => form.append('files', f));
  const res = await api.post('/api/v1/employees/damage-reports/photos', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data;
}

// ── SCR-65: Employee Rooms ─────────────────────────────────────────────────

export interface EmployeeRoom {
  id: string;
  name?: string;
  roomNumber?: string;
  floorName?: string;
  status: string;
}

export async function getEmployeeRooms(params?: {
  page?: number;
  size?: number;
}): Promise<{ success: boolean; data: { content: EmployeeRoom[] } }> {
  const res = await api.get('/api/v1/employees/rooms', { params });
  return res.data;
}

/** FR-05 US4 — Employee read-only month calendar (assigned property only). */
export interface EmployeeRoomCalendarDay {
  date: string;
  status: string;
  bookable: boolean;
}

export interface EmployeeRoomCalendar {
  roomId: string;
  roomNumber: string;
  roomStatus: string;
  days: EmployeeRoomCalendarDay[];
  bookedDates: string[];
  maintenanceDates: string[];
  bookedRanges: {
    bookingId: string;
    checkIn: string;
    checkOut: string;
    bookingStatus: string;
  }[];
}

export async function getEmployeeRoomCalendar(
  id: string,
  params: { month: number; year: number } | { startDate: string; endDate: string },
): Promise<EmployeeRoomCalendar> {
  const res = await api.get(`/api/v1/employees/rooms/${id}/calendar`, { params });
  return res.data.data;
}
