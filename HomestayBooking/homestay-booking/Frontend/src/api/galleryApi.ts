import api from './axiosInstance';
import { fetchManagerRoomV1, RoomDetail } from './roomsApi';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface GalleryImage {
  id: string;
  imageUrl: string;
  isPrimary: boolean;
  sortOrder: number;
}

function mapImages(images: RoomDetail['images']): GalleryImage[] {
  return (images ?? []).map(img => ({
    id: String(img.id),
    imageUrl: img.imageUrl,
    isPrimary: img.isPrimary,
    sortOrder: img.sortOrder,
  }));
}

interface RawGalleryImage {
  id: string;
  imageUrl: string;
  isPrimary: boolean;
  sortOrder: number;
}

function mapImageList(data: unknown[]): GalleryImage[] {
  return (data ?? []).map((raw) => {
    const img = raw as RawGalleryImage;
    return {
      id: String(img.id),
      imageUrl: img.imageUrl,
      isPrimary: img.isPrimary,
      sortOrder: img.sortOrder,
    };
  });
}

// ── v1 API (SCR-32) ───────────────────────────────────────────────────────────

export async function fetchGalleryImagesV1(
  roomId: string,
): Promise<{ roomName: string; images: GalleryImage[] }> {
  const room = await fetchManagerRoomV1(roomId);
  return {
    roomName: room.roomNumber,
    images: mapImages(room.images),
  };
}

export async function uploadRoomImagesV1(
  roomId: string,
  files: File[],
  setPrimary = false,
): Promise<GalleryImage[]> {
  const formData = new FormData();
  files.forEach(f => formData.append('files', f));

  const res = await api.post(`/api/v1/manager/rooms/${roomId}/images`, formData, {
    params: { setPrimary },
  });
  return mapImageList(res.data.data ?? []);
}

export async function setPrimaryImageV1(roomId: string, imageId: string): Promise<void> {
  await api.patch(`/api/v1/manager/rooms/${roomId}/images/${imageId}/primary`);
}

export async function deleteRoomImageV1(roomId: string, imageId: string): Promise<void> {
  await api.delete(`/api/v1/manager/rooms/${roomId}/images/${imageId}`);
}

export async function reorderRoomImagesV1(
  roomId: string,
  imageIds: string[],
): Promise<GalleryImage[]> {
  const res = await api.put(`/api/v1/manager/rooms/${roomId}/images/reorder`, { imageIds });
  return mapImageList(res.data.data ?? []);
}

// ── Legacy API ────────────────────────────────────────────────────────────────

/** @deprecated Use uploadRoomImagesV1 */
export async function uploadRoomImages(
  roomId: string,
  files: File[],
  setPrimary = false,
): Promise<GalleryImage[]> {
  const formData = new FormData();
  files.forEach(f => formData.append('files', f));

  const res = await api.post(`/api/rooms/${roomId}/images`, formData, {
    params: { setPrimary },
  });
  return mapImageList(res.data.data ?? []);
}

/** @deprecated Use fetchGalleryImagesV1 */
export async function fetchGalleryImages(roomId: string): Promise<{ roomName: string; images: GalleryImage[] }> {
  const res = await api.get(`/api/rooms/${roomId}`);
  const room: RoomDetail = res.data.data;
  return {
    roomName: room.roomNumber,
    images: mapImages(room.images),
  };
}

/** @deprecated Use setPrimaryImageV1 */
export async function setPrimaryImage(imageId: string): Promise<void> {
  await api.patch(`/api/room-images/${imageId}/set-primary`);
}

/** @deprecated Use deleteRoomImageV1 */
export async function deleteRoomImage(imageId: string): Promise<void> {
  await api.delete(`/api/room-images/${imageId}`);
}
