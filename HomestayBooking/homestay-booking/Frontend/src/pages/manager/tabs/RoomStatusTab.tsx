import { useState, useEffect } from 'react';
import Alert from '../../../components/ui/Alert';
import { StatusBadge } from '../../../components/ui';
import type { StatusVariant } from '../../../components/ui/StatusBadge';
import {
  updateRoomStatusV1,
  type UpdateRoomStatusPayload,
  type RoomDetail,
} from '../../../api/roomsApi';

const STATUS_VI: Record<string, { label: string; variant: StatusVariant }> = {
  AVAILABLE:            { label: 'Trống',           variant: 'success' },
  PENDING_DEPOSIT:      { label: 'Chờ cọc',         variant: 'warning' },
  RESERVED:             { label: 'Đã đặt',          variant: 'info' },
  OCCUPIED:             { label: 'Đang ở',          variant: 'primary' },
  PENDING_CLEANING:     { label: 'Chờ dọn',         variant: 'warning' },
  CLEANING_IN_PROGRESS: { label: 'Đang dọn',        variant: 'info' },
  MAINTENANCE:          { label: 'Bảo trì',         variant: 'danger' },
  OUT_OF_SERVICE:       { label: 'Ngưng phục vụ',   variant: 'neutral' },
};

const MANUAL_STATUSES = ['AVAILABLE', 'MAINTENANCE', 'OUT_OF_SERVICE'] as const;
type ManualStatus = (typeof MANUAL_STATUSES)[number];

const AUTO_STATUSES = Object.keys(STATUS_VI).filter(
  (s) => !MANUAL_STATUSES.includes(s as ManualStatus),
);

function needsDatesAndReason(status: ManualStatus): boolean {
  return status === 'MAINTENANCE' || status === 'OUT_OF_SERVICE';
}

function FieldError({ msg }: { msg: string }) {
  return <p className="text-xs text-red-600 mt-1 font-medium">{msg}</p>;
}

/** SCR-31 tab — Status (formerly SCR-33) */
export default function RoomStatusTab({
  room,
  onStatusUpdated,
}: {
  room: RoomDetail;
  onStatusUpdated?: (status: string) => void;
}) {
  const [selectedStatus, setSelectedStatus] = useState<ManualStatus>('AVAILABLE');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (MANUAL_STATUSES.includes(room.status as ManualStatus)) {
      setSelectedStatus(room.status as ManualStatus);
    } else {
      setSelectedStatus('AVAILABLE');
    }
    setStartDate('');
    setEndDate('');
    setReason('');
    setError('');
    setFieldErrors({});
    setSuccess('');
  }, [room.id, room.status]);

  const showDates = needsDatesAndReason(selectedStatus);
  const isDirty = selectedStatus !== room.status;
  const currentMeta = STATUS_VI[room.status];
  const blockAvailableFromCleaning =
    selectedStatus === 'AVAILABLE' &&
    (room.status === 'PENDING_CLEANING' || room.status === 'CLEANING_IN_PROGRESS');

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (showDates) {
      if (!startDate) errs.startDate = 'Vui lòng chọn từ ngày';
      if (!endDate) errs.endDate = 'Vui lòng chọn đến ngày';
      if (startDate && endDate && endDate < startDate) {
        errs.endDate = 'Đến ngày phải sau hoặc bằng từ ngày';
      }
      if (!reason.trim()) errs.reason = 'Vui lòng nhập lý do';
    }
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (!isDirty) return;
    if (blockAvailableFromCleaning) {
      setError(
        'Không thể chuyển sang Trống khi phòng đang chờ/đang dọn. Hoàn thành Housekeeping Task trước.',
      );
      return;
    }
    if (!validate()) return;

    setSaving(true);
    try {
      const payload: UpdateRoomStatusPayload = {
        status: selectedStatus,
        ...(showDates ? { startDate, endDate, reason: reason.trim() } : {}),
      };
      await updateRoomStatusV1(room.id, payload);
      setSuccess('Cập nhật trạng thái thành công');
      onStatusUpdated?.(selectedStatus);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Không thể cập nhật trạng thái. Vui lòng thử lại.';
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="w-full min-w-0">
      <div className="grid grid-cols-1 lg:grid-cols-[240px_minmax(0,1fr)] gap-4 lg:gap-6">
        {/* Current status panel */}
        <aside className="rounded-lg border border-[#E2E8F0] bg-[#F8FAFC] p-4 h-fit">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-[#94A3B8] mb-2">
            Hiện tại
          </p>
          <p className="font-bold text-lg text-[#0F172A] mb-1">{room.roomNumber}</p>
          <p className="text-sm text-[#64748B] mb-3">
            {room.propertyName}
            {room.floorNumber != null ? ` · Tầng ${room.floorNumber}` : ''}
          </p>
          {currentMeta ? (
            <StatusBadge status={currentMeta.label} variant={currentMeta.variant} />
          ) : (
            <StatusBadge status={room.status} variant="neutral" />
          )}
        </aside>

        {/* Form */}
        <div className="flex flex-col gap-4 min-w-0">
          {error && (
            <Alert variant="error" message={error} closeable onClose={() => setError('')} />
          )}
          {success && (
            <Alert variant="success" message={success} closeable onClose={() => setSuccess('')} />
          )}

          <div>
            <label htmlFor="room-status" className="form-label">
              Trạng thái mới <span className="text-red-600">*</span>
            </label>
            <select
              id="room-status"
              className="select w-full"
              value={selectedStatus}
              onChange={(e) => {
                setSelectedStatus(e.target.value as ManualStatus);
                setFieldErrors({});
                setError('');
                setSuccess('');
              }}
            >
              {MANUAL_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_VI[s]?.label ?? s}
                </option>
              ))}
            </select>
            <p className="text-xs text-[#94A3B8] mt-1 mb-0">
              Chỉ đặt: Trống, Bảo trì hoặc Ngưng phục vụ.
            </p>
          </div>

          {showDates && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label htmlFor="start-date" className="form-label">
                  Từ ngày <span className="text-red-600">*</span>
                </label>
                <input
                  id="start-date"
                  type="date"
                  className="input w-full"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    setFieldErrors((prev) => ({ ...prev, startDate: '' }));
                  }}
                />
                {fieldErrors.startDate && <FieldError msg={fieldErrors.startDate} />}
              </div>
              <div>
                <label htmlFor="end-date" className="form-label">
                  Đến ngày <span className="text-red-600">*</span>
                </label>
                <input
                  id="end-date"
                  type="date"
                  className="input w-full"
                  value={endDate}
                  min={startDate || undefined}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    setFieldErrors((prev) => ({ ...prev, endDate: '' }));
                  }}
                />
                {fieldErrors.endDate && <FieldError msg={fieldErrors.endDate} />}
              </div>
            </div>
          )}

          <div>
            <label htmlFor="reason" className="form-label">
              Lý do
              {showDates && <span className="text-red-600"> *</span>}
            </label>
            <textarea
              id="reason"
              className="textarea w-full"
              rows={2}
              maxLength={1000}
              placeholder={
                showDates
                  ? 'VD: Sửa điều hòa, thay ống nước…'
                  : 'Tùy chọn — ghi chú khi đổi trạng thái'
              }
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                setFieldErrors((prev) => ({ ...prev, reason: '' }));
              }}
              style={{ minHeight: 72, resize: 'vertical' }}
            />
            {fieldErrors.reason && <FieldError msg={fieldErrors.reason} />}
            <p className="text-xs text-[#94A3B8] mt-1 text-right mb-0">{reason.length} / 1000</p>
          </div>

          {AUTO_STATUSES.includes(room.status) && (
            <Alert
              variant="warning"
              message={`Phòng đang ở trạng thái hệ thống "${STATUS_VI[room.status]?.label ?? room.status}". Có thể chuyển sang Trống, Bảo trì hoặc Ngưng phục vụ.`}
            />
          )}

          {blockAvailableFromCleaning && (
            <Alert
              variant="warning"
              message="Không được bỏ qua Housekeeping: phòng đang chờ/đang dọn không thể chuyển thẳng sang Trống."
            />
          )}

          <div
            className="flex flex-wrap gap-2 items-center pt-3"
            style={{ borderTop: '1px solid var(--hairline)' }}
          >
            <button
              type="submit"
              className="btn-primary"
              disabled={!isDirty || saving || blockAvailableFromCleaning}
            >
              {saving ? 'Đang cập nhật…' : 'Cập nhật trạng thái'}
            </button>
            {!isDirty && !saving && (
              <span className="text-sm text-[#94A3B8]">Chưa có thay đổi</span>
            )}
          </div>
        </div>
      </div>
    </form>
  );
}
