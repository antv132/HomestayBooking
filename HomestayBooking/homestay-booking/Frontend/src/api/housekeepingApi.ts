import api from './axiosInstance';
export interface HousekeepingTaskSummary {
  id: string;
  propertyId?: string;
  propertyName?: string;
  roomId: string;
  roomNumber: string;
  bookingId?: string | null;
  status: string;
  assigneeId?: string | null;
  assigneeName?: string | null;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  note?: string | null;
}
export interface HousekeepingSchedule {
  date: string;
  summary: {
    pending: number;
    inProgress: number;
    completed: number;
    unassigned: number;
  };
  unassigned: HousekeepingTaskSummary[];
  employees: {
    employeeId: string;
    employeeName: string;
    tasks: HousekeepingTaskSummary[];
  }[];
}
export interface HousekeepingBoardParams {
  propertyId: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export async function fetchHousekeepingScheduleV1(
  propertyId: string,
  date?: string,
): Promise<HousekeepingSchedule> {
  const res = await api.get('/api/v1/manager/housekeeping/schedule', {
    params: { propertyId, date: date || undefined },
  });
  return res.data.data;
}
export async function fetchManagerHousekeepingTasksV1(
  params: HousekeepingBoardParams,
): Promise<PageResponse<HousekeepingTaskSummary>> {
  const res = await api.get('/api/v1/manager/housekeeping-tasks', { params });
  return res.data.data;
}
export async function createHousekeepingTaskV1(payload: {
  roomId: string;
  assigneeId?: string;
}): Promise<HousekeepingTaskSummary> {
  const res = await api.post('/api/v1/manager/housekeeping-tasks', payload);
  return res.data.data;
}
export async function assignHousekeepingTaskV1(
  taskId: string,
  assigneeId: string,
): Promise<HousekeepingTaskSummary> {
  const res = await api.patch(`/api/v1/manager/housekeeping-tasks/${taskId}/assign`, {
    assigneeId,
  });
  return res.data.data;
}
export async function cancelHousekeepingTaskV1(
  taskId: string,
  note?: string,
): Promise<HousekeepingTaskSummary> {
  const res = await api.patch(`/api/v1/manager/housekeeping-tasks/${taskId}/cancel`, {
    note: note || undefined,
  });
  return res.data.data;
}
/** SCR-60 — Employee assigned housekeeping task */
export interface EmployeeHousekeepingTask {
  id: string;
  roomId?: string;
  roomNumber?: string;
  floorName?: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  note?: string | null;
  createdAt?: string;
  startedAt?: string | null;
  completedAt?: string | null;
}
export async function fetchEmployeeHousekeepingTasks(
  params?: { page?: number; size?: number; status?: string },
): Promise<PageResponse<EmployeeHousekeepingTask>> {
  const res = await api.get('/api/v1/employees/housekeeping', { params });
  return res.data.data;
}
export async function startEmployeeHousekeepingTask(taskId: string): Promise<void> {
  await api.post(`/api/v1/employees/housekeeping/${taskId}/start`);
}
export async function finishEmployeeHousekeepingTask(taskId: string): Promise<void> {
  await api.post(`/api/v1/employees/housekeeping/${taskId}/finish`);
}

/** FR-012 — Admin read-only housekeeping list */
export interface AdminHousekeepingParams {
  propertyId?: string;
  status?: string;
  page?: number;
  size?: number;
}

export async function fetchAdminHousekeepingTasks(
  params?: AdminHousekeepingParams,
): Promise<PageResponse<HousekeepingTaskSummary>> {
  const res = await api.get('/api/v1/admin/housekeeping-tasks', { params });
  return res.data.data;
}
