import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import axios from 'axios';
import {
  fetchRooms,
  fetchPropertyOptions,
  fetchPriceStats,
  sortToApi,
  ROOM_TYPES,
  type RoomListItem,
  type PropertyOption,
} from '../api/roomsApi';
import {
  MAX_FILTER_GUESTS,
  MAX_FILTER_PRICE,
  clampPositiveIntString,
} from '../utils/filterInput';

const PAGE_SIZE = 12;

export function useRoomSearch() {
  const [searchParams, setSearchParams] = useSearchParams();

  const urlPage = Number(searchParams.get('page') || '0');
  const urlSort = searchParams.get('sort') || 'newest';
  const urlLocation = searchParams.get('location') || '';
  const urlSearch = searchParams.get('search') || '';
  const urlPropertyId = searchParams.get('propertyId') || '';
  const urlPropertyIds = searchParams.get('propertyIds') || '';
  const urlRoomTypesKey = searchParams.get('roomType') || '';
  const urlRoomTypes = useMemo(
    () => (urlRoomTypesKey ? urlRoomTypesKey.split(',').filter(Boolean) : []),
    [urlRoomTypesKey],
  );
  const urlMinPrice = searchParams.get('minPrice') || '';
  const urlMaxPrice = searchParams.get('maxPrice') || '';
  const urlGuests = searchParams.get('guests') || '';
  const urlCheckIn = searchParams.get('checkIn') || '';
  const urlCheckOut = searchParams.get('checkOut') || '';

  const selectedPropertyIds = useMemo(
    () => (urlPropertyIds ? urlPropertyIds.split(',').filter(Boolean) : urlPropertyId ? [urlPropertyId] : []),
    [urlPropertyIds, urlPropertyId],
  );

  const [draft, setDraft] = useState({
    search: urlSearch || urlLocation,
    propertyIds: selectedPropertyIds,
    roomTypes: urlRoomTypes,
    minPrice: urlMinPrice,
    maxPrice: urlMaxPrice,
    guests: urlGuests,
    checkIn: urlCheckIn,
    checkOut: urlCheckOut,
    sort: urlSort,
  });

  const [rooms, setRooms] = useState<RoomListItem[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [properties, setProperties] = useState<PropertyOption[]>([]);
  const [priceMin, setPriceMin] = useState(0);
  const [priceMax, setPriceMax] = useState(5_000_000);
  const sliderMax = Math.min(priceMax, MAX_FILTER_PRICE);

  function sanitizeDraftPrices(minRaw: string, maxRaw: string) {
    const minP = minRaw ? clampPositiveIntString(minRaw, MAX_FILTER_PRICE) : '';
    let maxP = maxRaw ? clampPositiveIntString(maxRaw, MAX_FILTER_PRICE) : '';
    if (minP && maxP && Number(maxP) < Number(minP)) maxP = minP;
    return { minP, maxP };
  }

  useEffect(() => {
    setDraft({
      search: urlSearch || urlLocation,
      propertyIds: selectedPropertyIds,
      roomTypes: urlRoomTypes,
      minPrice: urlMinPrice,
      maxPrice: urlMaxPrice,
      guests: urlGuests,
      checkIn: urlCheckIn,
      checkOut: urlCheckOut,
      sort: urlSort,
    });
  }, [
    urlSearch,
    urlLocation,
    selectedPropertyIds,
    urlRoomTypes,
    urlMinPrice,
    urlMaxPrice,
    urlGuests,
    urlCheckIn,
    urlCheckOut,
    urlSort,
  ]);

  useEffect(() => {
    fetchPropertyOptions()
      .then(setProperties)
      .catch(() => setProperties([]));
    fetchPriceStats()
      .then((s) => {
        setPriceMin(Math.floor(s.minPrice));
        setPriceMax(Math.min(Math.ceil(s.maxPrice), MAX_FILTER_PRICE));
      })
      .catch(() => {});
  }, []);

  const loadRooms = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const propertyId = selectedPropertyIds.length === 1 ? selectedPropertyIds[0] : undefined;
      const data = await fetchRooms({
        page: urlPage,
        size: PAGE_SIZE,
        sort: urlSort === 'rating' ? sortToApi('newest') : sortToApi(urlSort),
        search: urlSearch || undefined,
        location: urlLocation || undefined,
        propertyId,
        roomType: urlRoomTypes.length ? urlRoomTypes.join(',') : undefined,
        minPrice: urlMinPrice
          ? Number(clampPositiveIntString(urlMinPrice, MAX_FILTER_PRICE))
          : undefined,
        maxPrice: urlMaxPrice
          ? Number(clampPositiveIntString(urlMaxPrice, MAX_FILTER_PRICE))
          : undefined,
        capacity: urlGuests
          ? Number(clampPositiveIntString(urlGuests, MAX_FILTER_GUESTS))
          : undefined,
        checkIn: urlCheckIn || undefined,
        checkOut: urlCheckOut || undefined,
        status: 'AVAILABLE',
      });

      let content = data.content;
      if (selectedPropertyIds.length > 1) {
        content = content.filter((r) => selectedPropertyIds.includes(r.propertyId));
      }
      if (urlSort === 'rating') {
        content = [...content].sort((a, b) => (b.averageRating ?? 0) - (a.averageRating ?? 0));
      }

      setRooms(content);
      setTotalElements(selectedPropertyIds.length > 1 ? content.length : data.totalElements);
      setTotalPages(selectedPropertyIds.length > 1 ? Math.ceil(content.length / PAGE_SIZE) || 1 : data.totalPages);
    } catch (err) {
      setRooms([]);
      setTotalElements(0);
      setTotalPages(0);
      const isNetwork =
        !axios.isAxiosError(err) ||
        err.code === 'ERR_NETWORK' ||
        err.message.includes('Network Error');
      setError(
        isNetwork
          ? 'Không kết nối được backend (port 8080). Mở IntelliJ → Run HomestayApplication, rồi bấm Thử lại.'
          : 'Không tải được danh sách phòng. Vui lòng thử lại sau.',
      );
    } finally {
      setLoading(false);
    }
  }, [
    urlPage,
    urlSort,
    urlSearch,
    urlLocation,
    selectedPropertyIds,
    urlRoomTypesKey,
    urlMinPrice,
    urlMaxPrice,
    urlGuests,
    urlCheckIn,
    urlCheckOut,
  ]);

  useEffect(() => {
    loadRooms();
  }, [loadRooms]);

  function applyFilters(resetPage = true) {
    const { minP, maxP } = sanitizeDraftPrices(draft.minPrice, draft.maxPrice);
    const guests = draft.guests ? clampPositiveIntString(draft.guests, MAX_FILTER_GUESTS) : '';

    const next = new URLSearchParams();
    if (draft.search.trim()) {
      next.set('location', draft.search.trim());
    }
    if (draft.propertyIds.length === 1) next.set('propertyId', draft.propertyIds[0]);
    else if (draft.propertyIds.length > 1) next.set('propertyIds', draft.propertyIds.join(','));
    if (draft.roomTypes.length) next.set('roomType', draft.roomTypes.join(','));
    if (minP) next.set('minPrice', minP);
    if (maxP) next.set('maxPrice', maxP);
    if (guests) next.set('guests', guests);
    if (draft.checkIn) next.set('checkIn', draft.checkIn);
    if (draft.checkOut) next.set('checkOut', draft.checkOut);
    if (draft.sort && draft.sort !== 'newest') next.set('sort', draft.sort);
    if (!resetPage && urlPage > 0) next.set('page', String(urlPage));
    setSearchParams(next);
  }

  /** Apply filters with an explicit search/location term (suggestion select). */
  function applySuggestionTerm(term: string) {
    const { minP, maxP } = sanitizeDraftPrices(draft.minPrice, draft.maxPrice);
    const guests = draft.guests ? clampPositiveIntString(draft.guests, MAX_FILTER_GUESTS) : '';
    const next = new URLSearchParams();
    const cleaned = term.trim();
    if (cleaned) next.set('location', cleaned);
    if (draft.propertyIds.length === 1) next.set('propertyId', draft.propertyIds[0]);
    else if (draft.propertyIds.length > 1) next.set('propertyIds', draft.propertyIds.join(','));
    if (draft.roomTypes.length) next.set('roomType', draft.roomTypes.join(','));
    if (minP) next.set('minPrice', minP);
    if (maxP) next.set('maxPrice', maxP);
    if (guests) next.set('guests', guests);
    if (draft.checkIn) next.set('checkIn', draft.checkIn);
    if (draft.checkOut) next.set('checkOut', draft.checkOut);
    if (draft.sort && draft.sort !== 'newest') next.set('sort', draft.sort);
    setDraft((p) => ({ ...p, search: cleaned }));
    setSearchParams(next);
  }

  function clearFilters() {
    setDraft({
      search: '',
      propertyIds: [],
      roomTypes: [],
      minPrice: '',
      maxPrice: '',
      guests: '',
      checkIn: '',
      checkOut: '',
      sort: 'newest',
    });
    setSearchParams({});
  }

  function toggleRoomType(type: string) {
    setDraft((p) => ({
      ...p,
      roomTypes: p.roomTypes.includes(type) ? p.roomTypes.filter((t) => t !== type) : [...p.roomTypes, type],
    }));
  }

  function toggleProperty(id: string) {
    setDraft((p) => ({
      ...p,
      propertyIds: p.propertyIds.includes(id) ? p.propertyIds.filter((x) => x !== id) : [...p.propertyIds, id],
    }));
  }

  function handleSortChange(sort: string) {
    const next = new URLSearchParams(searchParams);
    if (sort === 'newest') next.delete('sort');
    else next.set('sort', sort);
    next.delete('page');
    setSearchParams(next);
  }

  function handlePageChange(page: number) {
    const next = new URLSearchParams(searchParams);
    if (page <= 0) next.delete('page');
    else next.set('page', String(page));
    setSearchParams(next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  const activeChips = useMemo(() => {
    const chips: { label: string; onRemove: () => void }[] = [];
    if (urlLocation) {
      chips.push({
        label: `📍 ${urlLocation}`,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          n.delete('location');
          n.delete('page');
          setSearchParams(n);
        },
      });
    }
    if (urlSearch) {
      chips.push({
        label: `🔍 ${urlSearch}`,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          n.delete('search');
          n.delete('page');
          setSearchParams(n);
        },
      });
    }
    urlRoomTypes.forEach((t) =>
      chips.push({
        label: t,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          const rest = urlRoomTypes.filter((x) => x !== t);
          if (rest.length) n.set('roomType', rest.join(','));
          else n.delete('roomType');
          n.delete('page');
          setSearchParams(n);
        },
      }),
    );
    if (urlMinPrice) {
      chips.push({
        label: `Từ ₫${Number(urlMinPrice).toLocaleString('vi-VN')}`,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          n.delete('minPrice');
          n.delete('page');
          setSearchParams(n);
        },
      });
    }
    if (urlMaxPrice) {
      chips.push({
        label: `Đến ₫${Number(urlMaxPrice).toLocaleString('vi-VN')}`,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          n.delete('maxPrice');
          n.delete('page');
          setSearchParams(n);
        },
      });
    }
    if (urlGuests) {
      chips.push({
        label: `${urlGuests} khách`,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          n.delete('guests');
          n.delete('page');
          setSearchParams(n);
        },
      });
    }
    if (urlCheckIn && urlCheckOut) {
      chips.push({
        label: `${urlCheckIn} → ${urlCheckOut}`,
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          n.delete('checkIn');
          n.delete('checkOut');
          n.delete('page');
          setSearchParams(n);
        },
      });
    }
    selectedPropertyIds.forEach((pid) => {
      const prop = properties.find((p) => p.id === pid);
      chips.push({
        label: prop?.name ?? 'Homestay',
        onRemove: () => {
          const n = new URLSearchParams(searchParams);
          const rest = selectedPropertyIds.filter((x) => x !== pid);
          n.delete('propertyId');
          n.delete('propertyIds');
          if (rest.length === 1) n.set('propertyId', rest[0]);
          else if (rest.length > 1) n.set('propertyIds', rest.join(','));
          n.delete('page');
          setSearchParams(n);
        },
      });
    });
    return chips;
  }, [
    searchParams,
    urlLocation,
    urlSearch,
    urlRoomTypes,
    urlMinPrice,
    urlMaxPrice,
    urlGuests,
    urlCheckIn,
    urlCheckOut,
    selectedPropertyIds,
    properties,
    setSearchParams,
  ]);

  const hasActiveFilters = activeChips.length > 0;

  const detailQuerySuffix = useMemo(() => {
    const q = new URLSearchParams();
    if (urlCheckIn) q.set('checkIn', urlCheckIn);
    if (urlCheckOut) q.set('checkOut', urlCheckOut);
    if (urlGuests) q.set('guests', urlGuests);
    const s = q.toString();
    return s ? `?${s}` : '';
  }, [urlCheckIn, urlCheckOut, urlGuests]);

  return {
    draft,
    setDraft,
    priceMin,
    priceMax,
    sliderMax,
    MAX_FILTER_PRICE,
    MAX_FILTER_GUESTS,
    rooms,
    totalElements,
    totalPages,
    loading,
    error,
    properties,
    urlPage,
    urlSort,
    urlLocation,
    urlCheckIn,
    urlCheckOut,
    urlGuests,
    activeChips,
    hasActiveFilters,
    detailQuerySuffix,
    applyFilters,
    applySuggestionTerm,
    clearFilters,
    toggleRoomType,
    toggleProperty,
    handleSortChange,
    handlePageChange,
    loadRooms,
    ROOM_TYPES,
  };
}

export function buildSearchSummary(
  location: string,
  checkIn: string,
  checkOut: string,
  guests: string,
  hasActiveFilters: boolean,
): string {
  const parts: string[] = [];
  if (location) parts.push(location);
  if (checkIn && checkOut) {
    parts.push(`${formatDateVi(checkIn)} – ${formatDateVi(checkOut)}`);
  } else if (checkIn) {
    parts.push(`từ ${formatDateVi(checkIn)}`);
  }
  if (guests) parts.push(`${guests} khách`);
  if (parts.length === 0) {
    return hasActiveFilters ? 'Kết quả theo bộ lọc của bạn' : 'Tất cả phòng đang trống';
  }
  return `Kết quả cho ${parts.join(' · ')}`;
}

function formatDateVi(iso: string) {
  try {
    return new Date(iso + 'T00:00:00').toLocaleDateString('vi-VN', { day: 'numeric', month: 'short' });
  } catch {
    return iso;
  }
}
