import api from './axiosInstance';

export interface FeaturedRoom {
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

export interface FeaturedProperty {
  id: string;
  name: string;
  address: string;
  roomCount: number;
  availableRoomCount: number;
  coverImageUrl?: string | null;
}

export interface PlatformStats {
  totalProperties: number;
  totalRooms: number;
  totalAvailableRooms: number;
  averageRating: number;
  totalReviews: number;
}

export interface Promotion {
  id: string;
  subtitle: string;
  title: string;
  description?: string;
  ctaText: string;
  ctaUrl: string;
  imageUrl?: string;
  colorTheme: string;
  isActive: boolean;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SearchSuggestion {
  type: 'location' | 'property';
  label: string;
}



export async function fetchFeaturedProperties(limit = 6): Promise<FeaturedProperty[]> {
  const res = await api.get(`/api/v1/properties/featured?limit=${limit}`);
  return res.data.data ?? [];
}

export async function fetchFeaturedRooms(limit = 8): Promise<FeaturedRoom[]> {
  const res = await api.get(`/api/v1/rooms/featured?limit=${limit}`);
  return res.data.data ?? [];
}

export async function fetchPlatformStats(): Promise<PlatformStats> {
  const res = await api.get('/api/v1/public/stats');
  return res.data.data;
}

export async function fetchSearchSuggestions(q = ''): Promise<SearchSuggestion[]> {
  const res = await api.get('/api/v1/public/search-suggestions', { params: { q } });
  return res.data.data;
}

export async function fetchPromotions(): Promise<Promotion[]> {
  const res = await api.get('/api/v1/public/promotions');
  return res.data.data ?? [];
}
