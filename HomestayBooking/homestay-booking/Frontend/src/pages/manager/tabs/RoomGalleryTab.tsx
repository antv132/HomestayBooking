import { useState, useEffect, useRef, useCallback, DragEvent } from 'react';
import Alert from '../../../components/ui/Alert';
import {
  GalleryImage,
  fetchGalleryImagesV1,
  uploadRoomImagesV1,
  setPrimaryImageV1,
  deleteRoomImageV1,
  reorderRoomImagesV1,
} from '../../../api/galleryApi';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_SIZE_BYTES = 10 * 1024 * 1024;
const MAX_ROOM_IMAGES = 10;

function validateFiles(files: File[], currentCount: number): string | null {
  if (currentCount + files.length > MAX_ROOM_IMAGES) {
    return `Mỗi phòng tối đa ${MAX_ROOM_IMAGES} ảnh (hiện có ${currentCount}).`;
  }
  for (const file of files) {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return `"${file.name}" không đúng định dạng. Chỉ chấp nhận JPG, PNG hoặc WebP.`;
    }
    if (file.size > MAX_SIZE_BYTES) {
      return `"${file.name}" vượt quá 10 MB (${(file.size / 1024 / 1024).toFixed(1)} MB).`;
    }
  }
  return null;
}

function ConfirmModal({
  open,
  title,
  message,
  onConfirm,
  onCancel,
  loading,
}: {
  open: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="card p-5 max-w-sm w-full" style={{ boxShadow: '0 12px 40px rgba(0,0,0,0.18)' }}>
        <h3 className="font-semibold text-[#0F172A] mb-2">{title}</h3>
        <p className="text-sm text-[#64748B] mb-5">{message}</p>
        <div className="flex gap-2 justify-end">
          <button type="button" className="btn-ghost btn-sm" onClick={onCancel} disabled={loading}>
            Hủy
          </button>
          <button
            type="button"
            className="btn-primary btn-sm"
            style={{ background: '#DC2626', borderColor: '#DC2626' }}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Đang xóa…' : 'Xóa'}
          </button>
        </div>
      </div>
    </div>
  );
}

function ImageCard({
  image,
  index,
  onSetPrimary,
  onDelete,
  onDragStart,
  onDragOver,
  onDrop,
  settingPrimaryId,
  deletingId,
  dragOverIndex,
}: {
  image: GalleryImage;
  index: number;
  onSetPrimary: (id: string) => void;
  onDelete: (id: string) => void;
  onDragStart: (index: number) => void;
  onDragOver: (e: DragEvent, index: number) => void;
  onDrop: (index: number) => void;
  settingPrimaryId: string | null;
  deletingId: string | null;
  dragOverIndex: number | null;
}) {
  const [hovered, setHovered] = useState(false);
  const isProcessing = settingPrimaryId === image.id || deletingId === image.id;

  return (
    <div
      draggable={!isProcessing}
      onDragStart={() => onDragStart(index)}
      onDragOver={(e) => onDragOver(e, index)}
      onDrop={() => onDrop(index)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      className={`relative aspect-square rounded-lg overflow-hidden border border-[#E2E8F0] cursor-grab active:cursor-grabbing ${
        dragOverIndex === index ? 'ring-2 ring-[var(--primary)]' : ''
      }`}
    >
      <img
        src={image.imageUrl}
        alt="Phòng"
        className="w-full h-full object-cover block"
        loading="lazy"
        draggable={false}
        onError={(e) => {
          const target = e.currentTarget;
          target.onerror = null;
          target.src =
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='200'%3E%3Crect width='200' height='200' fill='%23f1f5f9'/%3E%3Ctext x='50%25' y='50%25' dominant-baseline='middle' text-anchor='middle' fill='%2394a3b8' font-size='12'%3EẢnh lỗi%3C/text%3E%3C/svg%3E";
        }}
      />

      {image.isPrimary && (
        <span className="absolute top-1.5 left-1.5 bg-[#0F766E] text-white text-[10px] font-bold rounded px-1.5 py-0.5">
          Đại diện
        </span>
      )}

      {isProcessing && (
        <div className="absolute inset-0 bg-black/45 flex items-center justify-center">
          <span className="spinner w-6 h-6 border-white/30 border-t-white" />
        </div>
      )}

      {hovered && !isProcessing && (
        <div className="absolute inset-0 bg-black/50 flex flex-col items-center justify-center gap-1.5 p-2">
          {!image.isPrimary && (
            <button
              type="button"
              onClick={() => onSetPrimary(image.id)}
              className="bg-white/95 border-0 rounded-md px-2 py-1 text-[11px] font-semibold cursor-pointer w-full"
            >
              Đặt đại diện
            </button>
          )}
          <button
            type="button"
            onClick={() => onDelete(image.id)}
            className="absolute top-1.5 right-1.5 bg-red-600 border-0 rounded-full w-6 h-6 text-white text-sm font-bold cursor-pointer flex items-center justify-center leading-none"
            title="Xóa ảnh"
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}

/** SCR-31 tab — Gallery (formerly SCR-32) */
export default function RoomGalleryTab({ roomId }: { roomId: string }) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [images, setImages] = useState<GalleryImage[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [uploading, setUploading] = useState(false);
  const [uploadingCount, setUploadingCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [fileDragOver, setFileDragOver] = useState(false);

  const [settingPrimaryId, setSettingPrimaryId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const [reordering, setReordering] = useState(false);

  const loadImages = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await fetchGalleryImagesV1(roomId);
      setImages(data.images.sort((a, b) => a.sortOrder - b.sortOrder));
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setLoadError(axiosErr?.response?.data?.message ?? 'Không tải được thư viện ảnh.');
    } finally {
      setLoading(false);
    }
  }, [roomId]);

  useEffect(() => {
    loadImages();
  }, [loadImages]);

  async function handleUpload(files: File[]) {
    if (files.length === 0) return;
    setError(null);
    const validationError = validateFiles(files, images.length);
    if (validationError) {
      setError(validationError);
      return;
    }

    setUploading(true);
    setUploadingCount(files.length);
    try {
      const newImages = await uploadRoomImagesV1(roomId, files);
      setImages((prev) => {
        const hasPrimaryInNew = newImages.some((i) => i.isPrimary);
        const updated = hasPrimaryInNew ? prev.map((i) => ({ ...i, isPrimary: false })) : [...prev];
        return [...updated, ...newImages].sort((a, b) => a.sortOrder - b.sortOrder);
      });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message ?? 'Tải ảnh thất bại. Vui lòng thử lại.');
    } finally {
      setUploading(false);
      setUploadingCount(0);
    }
  }

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length > 0) handleUpload(files);
    e.target.value = '';
  }

  function handleFileDragOver(e: DragEvent) {
    e.preventDefault();
    setFileDragOver(true);
  }

  function handleFileDrop(e: DragEvent) {
    e.preventDefault();
    setFileDragOver(false);
    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) handleUpload(files);
  }

  async function handleSetPrimary(imageId: string) {
    setError(null);
    setSettingPrimaryId(imageId);
    try {
      await setPrimaryImageV1(roomId, imageId);
      setImages((prev) => prev.map((img) => ({ ...img, isPrimary: img.id === imageId })));
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message ?? 'Không thể đặt ảnh đại diện.');
    } finally {
      setSettingPrimaryId(null);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setError(null);
    setDeletingId(deleteTarget);
    try {
      await deleteRoomImageV1(roomId, deleteTarget);
      const remaining = images.filter((img) => img.id !== deleteTarget);
      const hadPrimary = images.find((img) => img.id === deleteTarget)?.isPrimary;
      if (hadPrimary && remaining.length > 0 && !remaining.some((i) => i.isPrimary)) {
        remaining[0] = { ...remaining[0], isPrimary: true };
      }
      setImages(remaining);
      setDeleteTarget(null);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message ?? 'Xóa ảnh thất bại.');
    } finally {
      setDeletingId(null);
    }
  }

  function handleImageDragStart(index: number) {
    setDragIndex(index);
  }

  function handleImageDragOver(e: DragEvent, index: number) {
    e.preventDefault();
    setDragOverIndex(index);
  }

  async function handleImageDrop(targetIndex: number) {
    if (dragIndex === null || dragIndex === targetIndex) {
      setDragIndex(null);
      setDragOverIndex(null);
      return;
    }

    const prev = [...images];
    const reordered = [...images];
    const [moved] = reordered.splice(dragIndex, 1);
    reordered.splice(targetIndex, 0, moved);
    const withOrder = reordered.map((img, i) => ({ ...img, sortOrder: i }));

    setImages(withOrder);
    setDragIndex(null);
    setDragOverIndex(null);
    setReordering(true);

    try {
      const result = await reorderRoomImagesV1(
        roomId,
        withOrder.map((img) => img.id),
      );
      setImages(result.sort((a, b) => a.sortOrder - b.sortOrder));
    } catch (err: unknown) {
      setImages(prev);
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr?.response?.data?.message ?? 'Sắp xếp ảnh thất bại.');
    } finally {
      setReordering(false);
    }
  }

  const primaryCount = images.filter((i) => i.isPrimary).length;

  if (loadError) {
    return (
      <div className="text-center py-6">
        <Alert variant="error" message={loadError} />
        <button type="button" className="btn-primary btn-sm mt-3" onClick={loadImages}>
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="w-full min-w-0">
      <ConfirmModal
        open={deleteTarget !== null}
        title="Xóa ảnh?"
        message="Hành động này không thể hoàn tác. Ảnh sẽ bị xóa vĩnh viễn."
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deletingId !== null}
      />

      <input
        ref={fileInputRef}
        type="file"
        accept=".jpg,.jpeg,.png,.webp"
        multiple
        className="hidden"
        onChange={handleFileInput}
      />

      <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
        <p className="text-sm text-[#64748B] m-0">
          {loading
            ? 'Đang tải…'
            : `${images.length} ảnh${primaryCount > 0 ? ' · Có ảnh đại diện' : ' · Chưa có đại diện'}${reordering ? ' · Đang sắp xếp…' : ''}`}
        </p>
        <button
          type="button"
          className="btn-primary btn-sm"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
        >
          + Tải ảnh
        </button>
      </div>

      {error && (
        <div className="mb-3">
          <Alert variant="error" message={error} closeable onClose={() => setError(null)} />
        </div>
      )}

      <div
        role="button"
        tabIndex={0}
        onClick={() => !uploading && fileInputRef.current?.click()}
        onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
        onDragOver={handleFileDragOver}
        onDragLeave={() => setFileDragOver(false)}
        onDrop={handleFileDrop}
        className={`border border-dashed rounded-lg px-4 py-5 mb-4 text-center cursor-pointer transition-colors ${
          fileDragOver ? 'border-[var(--primary)] bg-[rgba(15,118,110,0.06)]' : 'border-[#CBD5E1] bg-[#F8FAFC]'
        } ${uploading ? 'opacity-60 cursor-not-allowed' : ''}`}
      >
        {uploading ? (
          <p className="text-sm font-medium text-[#64748B] m-0">Đang tải {uploadingCount} ảnh…</p>
        ) : (
          <>
            <p className="text-sm font-medium text-[#334155] m-0 mb-0.5">
              Kéo thả JPG / PNG / WebP, hoặc bấm để chọn
            </p>
            <p className="text-xs text-[#94A3B8] m-0">JPEG/PNG/WebP · tối đa 10 MB / ảnh · tối đa 10 ảnh</p>
          </>
        )}
      </div>

      {loading ? (
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-2.5">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="aspect-square rounded-lg bg-[#F1F5F9] animate-pulse" />
          ))}
        </div>
      ) : images.length === 0 ? (
        <div className="text-center py-8 rounded-lg bg-[#F8FAFC] border border-[#E2E8F0]">
          <p className="font-semibold text-[#0F172A] mb-1">Chưa có ảnh</p>
          <p className="text-sm text-[#64748B] mb-3">Tải ảnh để giới thiệu phòng.</p>
          <button type="button" className="btn-primary btn-sm" onClick={() => fileInputRef.current?.click()}>
            + Tải ảnh đầu tiên
          </button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-2.5">
            {images.map((img, index) => (
              <ImageCard
                key={img.id}
                image={img}
                index={index}
                onSetPrimary={handleSetPrimary}
                onDelete={setDeleteTarget}
                onDragStart={handleImageDragStart}
                onDragOver={handleImageDragOver}
                onDrop={handleImageDrop}
                settingPrimaryId={settingPrimaryId}
                deletingId={deletingId}
                dragOverIndex={dragOverIndex}
              />
            ))}
          </div>
          {primaryCount === 0 && (
            <p className="text-xs text-amber-800 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-3 mb-0">
              Chưa có ảnh đại diện — di chuột vào ảnh và chọn &quot;Đặt đại diện&quot;.
            </p>
          )}
          <p className="text-xs text-[#94A3B8] mt-2 mb-0">Kéo thả để đổi thứ tự hiển thị.</p>
        </>
      )}
    </div>
  );
}
