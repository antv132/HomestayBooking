package com.homestay.service;

import com.homestay.dto.request.CreateRoomRequest;
import com.homestay.dto.request.ReorderImagesRequest;
import com.homestay.dto.request.UpdateRoomRequest;
import com.homestay.dto.request.UpdateRoomStatusRequest;
import com.homestay.dto.response.AvailabilityResponse;
import com.homestay.dto.response.BookingSummaryResponse;
import com.homestay.dto.response.CalendarDayStatusResponse;
import com.homestay.dto.response.MonthAvailabilityResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.RoomCalendarResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.dto.response.RoomStatusBlockResponse;
import com.homestay.dto.response.RoomSummaryResponse;
import com.homestay.dto.response.StaffRoomMonthCalendarResponse;
import com.homestay.entity.Floor;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.RoomStatusBlock;
import com.homestay.entity.User;
import com.homestay.entity.RoomImage;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomImageRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.RoomStatusBlockRepository;
import com.homestay.util.SearchKeywordExpander;
import com.homestay.util.UploadLimits;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {

    /** Property-local calendar day (005 Assumptions / edge cases). */
    static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Set<Room.Status> MAINTENANCE_ROOM_STATUSES = Set.of(
            Room.Status.MAINTENANCE,
            Room.Status.OUT_OF_SERVICE,
            Room.Status.PENDING_CLEANING,
            Room.Status.CLEANING_IN_PROGRESS
    );

    private static final Set<Room.Status> MANAGER_SETTABLE_STATUSES = Set.of(
            Room.Status.AVAILABLE,
            Room.Status.MAINTENANCE,
            Room.Status.OUT_OF_SERVICE
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;
    private final FloorRepository floorRepository;
    private final RoomImageRepository roomImageRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final RoomStatusBlockRepository roomStatusBlockRepository;
    private final HousekeepingTaskRepository housekeepingTaskRepository;
    private final ActivityLogService activityLogService;

    public RoomService(RoomRepository roomRepository,
                       PropertyRepository propertyRepository,
                       FloorRepository floorRepository,
                       RoomImageRepository roomImageRepository,
                       BookingRepository bookingRepository,
                       ReviewRepository reviewRepository,
                       ReportPropertyScopeValidator scopeValidator,
                       ManagerPropertyAssignmentRepository assignmentRepository,
                       RoomStatusBlockRepository roomStatusBlockRepository,
                       HousekeepingTaskRepository housekeepingTaskRepository,
                       ActivityLogService activityLogService) {
        this.roomRepository = roomRepository;
        this.propertyRepository = propertyRepository;
        this.floorRepository = floorRepository;
        this.roomImageRepository = roomImageRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.scopeValidator = scopeValidator;
        this.assignmentRepository = assignmentRepository;
        this.roomStatusBlockRepository = roomStatusBlockRepository;
        this.housekeepingTaskRepository = housekeepingTaskRepository;
        this.activityLogService = activityLogService;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    // Lấy danh sách phòng (public listing - SCR-07/SCR-09) với full filter
    public PageResponse<RoomSummaryResponse> getAll(
            String search, String location, String status, String propertyIdStr,
            String roomType, BigDecimal minPrice, BigDecimal maxPrice,
            Integer capacity, LocalDate checkIn, LocalDate checkOut,
            Pageable pageable) {

        validatePublicSearchDates(checkIn, checkOut);

        Room.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Room.Status.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        UUID propertyId = (propertyIdStr != null && !propertyIdStr.isBlank())
                ? UUID.fromString(propertyIdStr) : null;

        String cleanSearch   = (search   != null && !search.isBlank())   ? search.trim()   : null;
        String cleanLocation = (location != null && !location.isBlank()) ? location.trim() : null;
        String keyword = cleanSearch != null ? cleanSearch : cleanLocation;
        List<String> keywordTerms = SearchKeywordExpander.expand(keyword);
        String cleanRoomType = (roomType != null && !roomType.isBlank()) ? roomType.trim() : null;

        Page<Room> page = roomRepository.findPublicWithFilters(
                getKeywordTerm(keywordTerms, 0),
                getKeywordTerm(keywordTerms, 1),
                getKeywordTerm(keywordTerms, 2),
                getKeywordTerm(keywordTerms, 3),
                getKeywordTerm(keywordTerms, 4),
                statusEnum, propertyId, cleanRoomType,
                minPrice, maxPrice, capacity, checkIn, checkOut,
                pageable);

        return toPageResponse(page);
    }

    private static String getKeywordTerm(List<String> terms, int index) {
        return index < terms.size() ? terms.get(index) : null;
    }

    // Khoảng giá thực tế cho slider bộ lọc
    public java.util.Map<String, java.math.BigDecimal> getPriceStats() {
        java.math.BigDecimal min = roomRepository.findMinPrice(Room.Status.AVAILABLE);
        java.math.BigDecimal max = roomRepository.findMaxPrice(Room.Status.AVAILABLE);
        return java.util.Map.of(
                "minPrice", min != null ? min : java.math.BigDecimal.ZERO,
                "maxPrice", max != null ? max : new java.math.BigDecimal("5000000"));
    }

    // Lấy danh sách phòng nổi bật cho trang chủ (public - SCR-01)
    public List<RoomSummaryResponse> getFeatured(int limit) {
        Page<Room> page = roomRepository.findByStatusAndActiveProperty(
                Room.Status.AVAILABLE,
                org.springframework.data.domain.PageRequest.of(0, limit,
                        org.springframework.data.domain.Sort.by("createdAt").descending()));
        return page.getContent().stream().map(RoomSummaryResponse::fromEntity).toList();
    }

    // Lấy chi tiết phòng (public - SCR-08) — ẩn property INACTIVE
    public RoomDetailResponse getById(UUID id) {
        Room room = findPublicDiscoverableRoom(id);
        return RoomDetailResponse.fromEntity(room);
    }

    /** SCR-31 — Manager-scoped room detail (v1). */
    @Transactional(readOnly = true)
    public RoomDetailResponse getByIdForManager(User manager, UUID id) {
        Room room = findActiveById(id);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
        return RoomDetailResponse.fromEntity(room);
    }

    // Đánh giá công khai của phòng (SCR-08) — chỉ PUBLISHED
    public PageResponse<RoomDetailResponse.ReviewInfo> getPublishedReviews(UUID roomId, Pageable pageable) {
        findPublicDiscoverableRoom(roomId);
        Page<Review> page = reviewRepository.findByRoom_IdAndStatusOrderByCreatedAtDesc(
                roomId, Review.Status.PUBLISHED, pageable);
        List<RoomDetailResponse.ReviewInfo> content = page.getContent().stream()
                .map(this::toReviewInfo)
                .collect(Collectors.toList());
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private RoomDetailResponse.ReviewInfo toReviewInfo(Review review) {
        RoomDetailResponse.ReviewInfo info = new RoomDetailResponse.ReviewInfo();
        info.setId(review.getId());
        info.setCustomerName(review.getCustomer().getFullName());
        info.setRating(review.getRating());
        info.setComment(review.getComment());
        info.setCreatedAt(review.getCreatedAt());
        return info;
    }

    // Lấy lịch trống phòng cho SCR-10 calendar
    public RoomCalendarResponse getCalendar(UUID roomId) {
        Room room = findPublicDiscoverableRoom(roomId);
        List<Object[]> ranges = roomRepository.findBookedDateRanges(roomId);
        List<RoomCalendarResponse.BookedRange> bookedRanges = ranges.stream()
                .map(r -> new RoomCalendarResponse.BookedRange(
                        (LocalDate) r[0],
                        (LocalDate) r[1],
                        r[2].toString()))
                .collect(Collectors.toList());
        return new RoomCalendarResponse(room.getStatus().name(), bookedRanges);
    }

    // Kiểm tra phòng còn trống không (public - SCR-10 / FR-05 T048)
    public AvailabilityResponse checkAvailability(UUID roomId, LocalDate checkIn, LocalDate checkOut) {
        validatePublicSearchDates(checkIn, checkOut);

        Room room = findPublicDiscoverableRoom(roomId);
        List<Object[]> ranges = roomRepository.findBookedDateRanges(roomId);
        List<AvailabilityResponse.DateRange> bookedRanges = ranges.stream()
                .map(r -> new AvailabilityResponse.DateRange((LocalDate) r[0], (LocalDate) r[1]))
                .collect(Collectors.toList());

        if (MAINTENANCE_ROOM_STATUSES.contains(room.getStatus())) {
            return new AvailabilityResponse(false, bookedRanges);
        }

        if (roomStatusBlockRepository.existsActiveOverlappingStay(roomId, checkIn, checkOut)) {
            return new AvailabilityResponse(false, bookedRanges);
        }

        boolean hasOverlap = roomRepository.existsOverlapBooking(roomId, checkIn, checkOut);
        return new AvailabilityResponse(!hasOverlap, bookedRanges);
    }

    // SCR-09 — month view: flat lists + FR-05 days[] (8 statuses)
    public MonthAvailabilityResponse getMonthAvailability(UUID roomId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException("startDate và endDate không hợp lệ");
        }
        Room room = findPublicDiscoverableRoom(roomId);

        List<Object[]> ranges = roomRepository.findBlockingBookingsInRange(roomId, startDate, endDate);
        java.util.Map<LocalDate, String> bookingStatusByDate = new java.util.HashMap<>();
        Set<String> bookedDates = new LinkedHashSet<>();
        for (Object[] row : ranges) {
            LocalDate checkIn = (LocalDate) row[0];
            LocalDate checkOut = (LocalDate) row[1];
            String bookingStatus = row[2].toString();
            String calendarStatus = mapBookingToCalendarStatus(bookingStatus);
            // Half-open stay: [checkIn, checkOut)
            LocalDate cursor = checkIn;
            while (cursor.isBefore(checkOut)) {
                if (!cursor.isBefore(startDate) && !cursor.isAfter(endDate)) {
                    bookedDates.add(cursor.toString());
                    bookingStatusByDate.merge(cursor, calendarStatus, RoomService::higherPriorityStatus);
                }
                cursor = cursor.plusDays(1);
            }
        }

        java.util.Map<LocalDate, String> blockStatusByDate = new java.util.HashMap<>();
        Set<String> maintenanceDates = new LinkedHashSet<>();
        List<RoomStatusBlock> blocks =
                roomStatusBlockRepository.findActiveOverlappingRange(roomId, startDate, endDate);
        for (RoomStatusBlock block : blocks) {
            String blockStatus = block.getStatus().name();
            LocalDate cursor = block.getStartDate();
            while (!cursor.isAfter(block.getEndDate())) {
                if (!cursor.isBefore(startDate) && !cursor.isAfter(endDate)) {
                    maintenanceDates.add(cursor.toString());
                    blockStatusByDate.merge(cursor, blockStatus, RoomService::higherPriorityStatus);
                }
                cursor = cursor.plusDays(1);
            }
        }

        // HK / operational room snapshot applies to "today" only (not whole month)
        LocalDate today = LocalDate.now(VN_ZONE);
        String todayRoomStatus = null;
        if (!today.isBefore(startDate) && !today.isAfter(endDate)) {
            Room.Status rs = room.getStatus();
            if (rs == Room.Status.PENDING_CLEANING || rs == Room.Status.CLEANING_IN_PROGRESS) {
                todayRoomStatus = mapRoomStatusToCalendar(rs);
            } else if ((rs == Room.Status.MAINTENANCE || rs == Room.Status.OUT_OF_SERVICE)
                    && !blockStatusByDate.containsKey(today)) {
                todayRoomStatus = mapRoomStatusToCalendar(rs);
            }
        }

        List<CalendarDayStatusResponse> days = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String status = "AVAILABLE";
            String fromBooking = bookingStatusByDate.get(cursor);
            if (fromBooking != null) {
                status = higherPriorityStatus(status, fromBooking);
            }
            String fromBlock = blockStatusByDate.get(cursor);
            if (fromBlock != null) {
                status = higherPriorityStatus(status, fromBlock);
            }
            if (cursor.equals(today) && todayRoomStatus != null) {
                status = higherPriorityStatus(status, todayRoomStatus);
            }
            boolean bookable = "AVAILABLE".equals(status);
            days.add(new CalendarDayStatusResponse(cursor.toString(), status, bookable));
            cursor = cursor.plusDays(1);
        }

        return new MonthAvailabilityResponse(
                new ArrayList<>(bookedDates),
                new ArrayList<>(maintenanceDates),
                days);
    }

    /**
     * FR-05 US2 — Manager month calendar (same days[] as SCR-09) with property scope + booking refs.
     */
    @Transactional(readOnly = true)
    public StaffRoomMonthCalendarResponse getMonthAvailabilityForManager(
            User manager, UUID roomId, LocalDate startDate, LocalDate endDate) {
        Room room = findActiveById(roomId);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
        return buildStaffMonthCalendar(room, startDate, endDate);
    }

    /** Package-visible for EmployeeRoomService — builds staff calendar after caller validates scope. */
    @Transactional(readOnly = true)
    public StaffRoomMonthCalendarResponse buildStaffMonthCalendar(
            Room room, LocalDate startDate, LocalDate endDate) {
        MonthAvailabilityResponse month = getMonthAvailability(room.getId(), startDate, endDate);
        List<Object[]> ranges =
                roomRepository.findBlockingBookingsWithIdInRange(room.getId(), startDate, endDate);
        List<StaffRoomMonthCalendarResponse.BookingRangeRef> refs = ranges.stream()
                .map(r -> new StaffRoomMonthCalendarResponse.BookingRangeRef(
                        (UUID) r[0],
                        ((LocalDate) r[1]).toString(),
                        ((LocalDate) r[2]).toString(),
                        r[3].toString()))
                .collect(Collectors.toList());
        return new StaffRoomMonthCalendarResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getStatus().name(),
                month.getDays(),
                month.getBookedDates(),
                month.getMaintenanceDates(),
                refs);
    }

    /** Priority (high→low): OCCUPIED > PENDING_DEPOSIT > RESERVED > CLEANING_IN_PROGRESS > PENDING_CLEANING > MAINTENANCE > OUT_OF_SERVICE > AVAILABLE */
    static String higherPriorityStatus(String a, String b) {
        return calendarPriority(a) <= calendarPriority(b) ? a : b;
    }

    private static int calendarPriority(String status) {
        if (status == null) return 99;
        return switch (status) {
            case "OCCUPIED" -> 1;
            case "PENDING_DEPOSIT" -> 2;
            case "RESERVED" -> 3;
            case "CLEANING_IN_PROGRESS" -> 4;
            case "PENDING_CLEANING" -> 5;
            case "MAINTENANCE" -> 6;
            case "OUT_OF_SERVICE" -> 7;
            case "AVAILABLE" -> 8;
            default -> 50;
        };
    }

    private static String mapBookingToCalendarStatus(String bookingStatus) {
        if (bookingStatus == null) return "RESERVED";
        return switch (bookingStatus) {
            case "PENDING_DEPOSIT" -> "PENDING_DEPOSIT";
            case "CONFIRMED" -> "RESERVED";
            case "CHECKED_IN", "PENDING_INSPECTION", "PENDING_DAMAGE_PAYMENT" -> "OCCUPIED";
            default -> "RESERVED";
        };
    }

    private static String mapRoomStatusToCalendar(Room.Status roomStatus) {
        if (roomStatus == null) return "AVAILABLE";
        return switch (roomStatus) {
            case MAINTENANCE -> "MAINTENANCE";
            case OUT_OF_SERVICE -> "OUT_OF_SERVICE";
            case PENDING_CLEANING -> "PENDING_CLEANING";
            case CLEANING_IN_PROGRESS -> "CLEANING_IN_PROGRESS";
            case AVAILABLE, PENDING_DEPOSIT, RESERVED, OCCUPIED -> "AVAILABLE";
        };
    }

    // ── Manager API ────────────────────────────────────────────────

    // SCR-40: Lịch sử booking của một phòng cụ thể (Manager)
    public PageResponse<BookingSummaryResponse> getRoomBookings(UUID roomId, Pageable pageable) {
        findById(roomId); // validates room exists
        var page = bookingRepository.findByRoomIdOrderByCheckInDateDesc(roomId, pageable);
        return new PageResponse<>(
                page.getContent().stream()
                        .map(BookingSummaryResponse::fromEntity)
                        .collect(Collectors.toList()),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }

    // SCR-39: Manager room list với combined filter
    public PageResponse<RoomSummaryResponse> getAllForManager(
            String search, String status, String propertyIdStr,
            String floorIdStr, String roomType, Pageable pageable) {

        Room.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = Room.Status.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        UUID propertyId = (propertyIdStr != null && !propertyIdStr.isBlank())
                ? UUID.fromString(propertyIdStr) : null;
        UUID floorId = (floorIdStr != null && !floorIdStr.isBlank())
                ? UUID.fromString(floorIdStr) : null;
        String cleanSearch    = (search   != null && !search.isBlank())    ? search.trim()    : null;
        String cleanRoomType  = (roomType != null && !roomType.isBlank())  ? roomType.trim()  : null;

        Page<Room> page = roomRepository.findWithFilters(
                cleanSearch, statusEnum, propertyId, floorId, cleanRoomType, pageable);
        return toPageResponse(page);
    }

    /** SCR-29 — Manager room list with property assignment scope (v1). */
    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> getAllForManagerScoped(
            User manager,
            String search, String status, String propertyIdStr,
            String floorIdStr, String roomType, Pageable pageable) {

        Room.Status statusEnum = parseStatus(status);
        UUID floorId = parseUuid(floorIdStr);
        String cleanSearch   = blankToNull(search);
        String cleanRoomType = blankToNull(roomType);

        Page<Room> page;
        if (propertyIdStr != null && !propertyIdStr.isBlank()) {
            UUID propertyId = UUID.fromString(propertyIdStr);
            scopeValidator.validateManagerAccess(manager, propertyId);
            page = roomRepository.findWithFilters(
                    cleanSearch, statusEnum, propertyId, floorId, cleanRoomType, pageable);
        } else {
            List<UUID> assignedIds = assignmentRepository.findActivePropertiesByManagerId(manager.getId())
                    .stream().map(Property::getId).toList();
            if (assignedIds.isEmpty()) {
                page = Page.empty(pageable);
            } else {
                page = roomRepository.findWithFiltersInProperties(
                        cleanSearch, statusEnum, assignedIds, floorId, cleanRoomType, pageable);
            }
        }
        return toPageResponse(page);
    }

    private static Room.Status parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return Room.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        return UUID.fromString(value);
    }

    private static String blankToNull(String value) {
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    // SCR-39 / FR-08: Soft-delete phòng — chỉ khi không có booking blocking
    @Transactional
    public void deleteRoom(UUID id) {
        softDeleteRoom(id, null);
    }

    /** SCR-29 DELETE — Manager soft-delete with property scope. */
    @Transactional
    public void deleteRoomForManager(User manager, UUID id) {
        Room room = findActiveById(id);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
        softDeleteRoom(id, manager);
    }

    private void softDeleteRoom(UUID id, User actor) {
        Room room = findActiveById(id);

        if (roomRepository.hasBlockingBookings(id)) {
            throw new ConflictException(
                    "Không thể xóa phòng đang có booking. Hãy hủy hoặc hoàn thành các booking trước.");
        }

        room.setDeletedAt(java.time.LocalDateTime.now());
        roomRepository.save(room);

        if (actor != null) {
            activityLogService.log(actor, "ROOM_DELETED", "Room", room.getId(),
                    "Soft-deleted room " + room.getRoomNumber());
        }
    }

    // Tạo phòng mới
    @Transactional
    public RoomDetailResponse create(CreateRoomRequest request) {
        UUID propertyId = UUID.fromString(request.getPropertyId());
        UUID floorId = UUID.fromString(request.getFloorId());

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property"));
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tầng"));

        // Floor phải thuộc property
        if (!floor.getProperty().getId().equals(propertyId)) {
            throw new BusinessException("Tầng không thuộc property này");
        }

        if (roomRepository.existsByPropertyIdAndRoomNumberAndDeletedAtIsNull(
                propertyId, request.getRoomNumber().trim())) {
            throw new ConflictException("Số phòng đã tồn tại trong property này");
        }

        Room room = new Room();
        room.setProperty(property);
        room.setFloor(floor);
        room.setRoomNumber(request.getRoomNumber().trim());
        room.setRoomType(request.getRoomType());
        room.setPricePerNight(request.getPricePerNight());
        room.setCapacity(request.getCapacity());
        room.setArea(request.getArea());
        room.setDescription(request.getDescription());
        room.setAmenities(request.getAmenities() != null
                ? new ArrayList<>(request.getAmenities())
                : new ArrayList<>());
        room.setStatus(Room.Status.AVAILABLE);

        roomRepository.save(room);
        return RoomDetailResponse.fromEntity(room);
    }

    /** SCR-30 — Manager-scoped room create (v1). */
    @Transactional
    public RoomDetailResponse createForManager(User manager, CreateRoomRequest request) {
        UUID propertyId = UUID.fromString(request.getPropertyId());
        scopeValidator.validateManagerAccess(manager, propertyId);
        RoomDetailResponse created = create(request);
        activityLogService.log(manager, "ROOM_CREATED", "Room",
                created.getId(), "Created room " + created.getRoomNumber());
        return created;
    }

    // Cập nhật thông tin phòng
    @Transactional
    public RoomDetailResponse update(UUID id, UpdateRoomRequest request) {
        Room room = findActiveById(id);

        if (request.getFloorId() != null && !request.getFloorId().isBlank()) {
            UUID floorId = UUID.fromString(request.getFloorId());
            Floor floor = floorRepository.findById(floorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tầng"));
            if (!floor.getProperty().getId().equals(room.getProperty().getId())) {
                throw new BusinessException("Tầng không thuộc property này");
            }
            room.setFloor(floor);
        }

        if (request.getRoomNumber() != null) {
            String newNumber = request.getRoomNumber().trim();
            if (!newNumber.equals(room.getRoomNumber())
                    && roomRepository.existsByPropertyIdAndRoomNumberAndDeletedAtIsNullAndIdNot(
                    room.getProperty().getId(), newNumber, room.getId())) {
                throw new ConflictException("Số phòng đã tồn tại trong property này");
            }
            room.setRoomNumber(newNumber);
        }
        if (request.getRoomType() != null) room.setRoomType(request.getRoomType());
        if (request.getPricePerNight() != null) room.setPricePerNight(request.getPricePerNight());
        if (request.getCapacity() != null) room.setCapacity(request.getCapacity());
        if (request.getArea() != null) room.setArea(request.getArea());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (request.getAmenities() != null) {
            room.setAmenities(new ArrayList<>(request.getAmenities()));
        }

        roomRepository.save(room);
        return RoomDetailResponse.fromEntity(room);
    }

    /** SCR-31 — Manager-scoped room update (v1). */
    @Transactional
    public RoomDetailResponse updateForManager(User manager, UUID id, UpdateRoomRequest request) {
        Room room = findActiveById(id);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
        RoomDetailResponse updated = update(id, request);
        activityLogService.log(manager, "ROOM_UPDATED", "Room", room.getId(),
                "Updated room " + room.getRoomNumber());
        return updated;
    }

    // Cập nhật trạng thái phòng (Manager — SCR-33)
    @Transactional
    public RoomDetailResponse updateStatus(UUID id, UpdateRoomStatusRequest request) {
        return updateStatus(id, request, null);
    }

    /** SCR-33 — Manager-scoped status update (v1). */
    @Transactional
    public RoomDetailResponse updateStatusForManager(User manager, UUID id, UpdateRoomStatusRequest request) {
        Room room = findActiveById(id);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
        return updateStatus(id, request, manager);
    }

    private RoomDetailResponse updateStatus(UUID id, UpdateRoomStatusRequest request, User actor) {
        Room room = findActiveById(id);

        Room.Status newStatus;
        try {
            newStatus = Room.Status.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái không hợp lệ: " + request.getStatus());
        }

        if (!MANAGER_SETTABLE_STATUSES.contains(newStatus)) {
            throw new BusinessException("Manager chỉ được đặt trạng thái AVAILABLE, MAINTENANCE hoặc OUT_OF_SERVICE");
        }

        String reasonText = resolveStatusReason(request);

        if (newStatus == Room.Status.AVAILABLE) {
            boolean openHk = housekeepingTaskRepository.existsByRoom_IdAndStatusIn(
                    room.getId(),
                    List.of(HousekeepingTask.Status.PENDING, HousekeepingTask.Status.IN_PROGRESS));
            if (openHk) {
                throw new ConflictException(
                        "Không thể đặt Available khi HousekeepingTask chưa Completed");
            }
            for (RoomStatusBlock block : roomStatusBlockRepository.findByRoom_IdAndActiveTrue(room.getId())) {
                block.setActive(false);
                roomStatusBlockRepository.save(block);
            }
            room.setStatus(Room.Status.AVAILABLE);
            roomRepository.save(room);
            if (actor != null) {
                activityLogService.log(
                        actor,
                        "ROOM_STATUS_CHANGED",
                        "Room",
                        room.getId(),
                        "AVAILABLE — cleared status blocks");
            }
            return RoomDetailResponse.fromEntity(room);
        }

        // MAINTENANCE / OUT_OF_SERVICE — date-scoped block (FR-005/007)
        if (reasonText == null || reasonText.isBlank()) {
            throw new BusinessException("Vui lòng nhập lý do");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BusinessException("Vui lòng chọn từ ngày và đến ngày");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("Đến ngày phải sau hoặc bằng từ ngày");
        }
        if (roomRepository.existsActiveBookingOverlapHalfOpen(
                room.getId(), request.getStartDate(), request.getEndDate())) {
            throw new ConflictException(
                    "Không thể khóa phòng: có booking active trùng khoảng ngày");
        }

        RoomStatusBlock block = new RoomStatusBlock();
        block.setRoom(room);
        block.setProperty(room.getProperty());
        block.setStatus(RoomStatusBlock.Status.valueOf(newStatus.name()));
        block.setStartDate(request.getStartDate());
        block.setEndDate(request.getEndDate());
        block.setReason(reasonText);
        block.setCreatedBy(actor);
        block.setActive(true);
        roomStatusBlockRepository.save(block);

        LocalDate today = LocalDate.now(VN_ZONE);
        if (!today.isBefore(request.getStartDate()) && !today.isAfter(request.getEndDate())) {
            room.setStatus(newStatus);
        }
        roomRepository.save(room);

        if (actor != null) {
            activityLogService.log(
                    actor,
                    "ROOM_STATUS_CHANGED",
                    "RoomStatusBlock",
                    block.getId(),
                    newStatus.name() + " " + request.getStartDate() + ".." + request.getEndDate()
                            + " — " + reasonText);
        }
        return RoomDetailResponse.fromEntity(room);
    }

    /** FR-05 T036 — list active Maintenance/OOS blocks (property-scoped). */
    @Transactional(readOnly = true)
    public List<RoomStatusBlockResponse> listStatusBlocksForManager(User manager, UUID roomId) {
        Room room = findActiveById(roomId);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
        return roomStatusBlockRepository.findByRoom_IdAndActiveTrue(room.getId()).stream()
                .map(RoomStatusBlockResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private static String resolveStatusReason(UpdateRoomStatusRequest request) {
        if (request.getReason() != null && !request.getReason().isBlank()) {
            return request.getReason().trim();
        }
        if (request.getNote() != null && !request.getNote().isBlank()) {
            return request.getNote().trim();
        }
        return null;
    }

    // Upload ảnh cho phòng — trả về list ảnh mới (SCR-43 / FR-08 gallery)
    @Transactional
    public List<RoomDetailResponse.RoomImageInfo> uploadImages(UUID roomId, List<MultipartFile> files, boolean setPrimary) {
        Room room = findActiveById(roomId);

        if (files == null || files.isEmpty()) {
            throw new BusinessException("Chưa chọn file ảnh");
        }

        long existingCount = roomImageRepository.countByRoomId(roomId);
        if (existingCount + files.size() > UploadLimits.MAX_ROOM_IMAGES) {
            throw new BusinessException(
                    "Mỗi phòng tối đa " + UploadLimits.MAX_ROOM_IMAGES + " ảnh (hiện có "
                            + existingCount + ")");
        }

        // Validate MIME + size before any disk write (LESSON-007)
        for (MultipartFile file : files) {
            UploadLimits.validateRoomImageMime(file);
            UploadLimits.validateSize(file, "Ảnh phòng");
        }

        // Tạo thư mục nếu chưa có
        String roomUploadDir = uploadDir + "rooms/" + roomId + "/";
        Path dirPath = Paths.get(roomUploadDir);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new BusinessException("Không thể tạo thư mục upload");
        }

        // Lấy sortOrder lớn nhất hiện tại
        List<RoomImage> existingImages = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
        int nextSort = existingImages.isEmpty() ? 0 : existingImages.get(existingImages.size() - 1).getSortOrder() + 1;
        boolean isFirstEver = existingImages.isEmpty(); // ảnh đầu tiên của phòng → tự set primary

        // Nếu setPrimary → bỏ primary cũ trước
        if (setPrimary || isFirstEver) {
            roomImageRepository.clearPrimaryByRoomId(roomId);
        }

        List<RoomDetailResponse.RoomImageInfo> result = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) originalName = "image.png";
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = dirPath.resolve(fileName);

            try {
                Files.write(filePath, file.getBytes());
            } catch (IOException e) {
                throw new BusinessException("Lưu file thất bại: " + file.getOriginalFilename());
            }

            boolean makePrimary = (setPrimary || isFirstEver) && i == 0;
            RoomImage image = new RoomImage();
            image.setRoom(room);
            image.setImageUrl("/" + roomUploadDir + fileName);
            image.setIsPrimary(makePrimary);
            image.setSortOrder(nextSort + i);
            RoomImage saved = roomImageRepository.save(image);

            // Build response DTO
            RoomDetailResponse.RoomImageInfo info = new RoomDetailResponse.RoomImageInfo();
            info.setId(saved.getId());
            info.setImageUrl(saved.getImageUrl());
            info.setIsPrimary(saved.getIsPrimary());
            info.setSortOrder(saved.getSortOrder());
            result.add(info);
        }
        return result;
    }

    @Transactional
    public List<RoomDetailResponse.RoomImageInfo> reorderImages(UUID roomId, List<UUID> imageIds) {
        List<RoomImage> existing = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
        Set<UUID> existingIds = existing.stream().map(RoomImage::getId).collect(Collectors.toSet());
        Set<UUID> requestedIds = new LinkedHashSet<>(imageIds);

        if (existing.isEmpty() || existingIds.size() != requestedIds.size() || !existingIds.equals(requestedIds)) {
            throw new BusinessException("Danh sách ảnh không hợp lệ");
        }

        Map<UUID, RoomImage> byId = existing.stream()
                .collect(Collectors.toMap(RoomImage::getId, Function.identity()));

        for (int i = 0; i < imageIds.size(); i++) {
            RoomImage img = byId.get(imageIds.get(i));
            img.setSortOrder(i);
            roomImageRepository.save(img);
        }

        return roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId).stream()
                .map(this::toRoomImageInfo)
                .collect(Collectors.toList());
    }

    // Set ảnh làm primary (SCR-43)
    @Transactional
    public void setPrimaryImage(UUID imageId) {
        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));
        // Bỏ primary cũ của phòng này
        roomImageRepository.clearPrimaryByRoomId(image.getRoom().getId());
        // Set ảnh này là primary
        image.setIsPrimary(true);
        roomImageRepository.save(image);
    }

    // Xóa ảnh — xóa file vật lý + DB record (SCR-43)
    @Transactional
    public void deleteImage(UUID imageId) {
        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        UUID roomId = image.getRoom().getId();
        // Xóa file vật lý nếu tồn tại
        try {
            Path filePath = Paths.get(image.getImageUrl().replaceFirst("^/", ""));
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}
        roomImageRepository.delete(image);

        if (wasPrimary) {
            List<RoomImage> remaining = roomImageRepository.findByRoomIdOrderBySortOrderAsc(roomId);
            if (!remaining.isEmpty()) {
                RoomImage next = remaining.get(0);
                roomImageRepository.clearPrimaryByRoomId(roomId);
                next.setIsPrimary(true);
                roomImageRepository.save(next);
            }
        }
    }

    // ── SCR-32 v1: Manager-scoped gallery ─────────────────────────────────────

    @Transactional
    public List<RoomDetailResponse.RoomImageInfo> uploadImagesForManager(
            User manager, UUID roomId, List<MultipartFile> files, boolean setPrimary) {
        validateManagerRoomAccess(manager, roomId);
        return uploadImages(roomId, files, setPrimary);
    }

    @Transactional
    public void deleteImageForManager(User manager, UUID roomId, UUID imageId) {
        validateManagerRoomAccess(manager, roomId);
        assertImageBelongsToRoom(roomId, imageId);
        deleteImage(imageId);
    }

    @Transactional
    public void setPrimaryImageForManager(User manager, UUID roomId, UUID imageId) {
        validateManagerRoomAccess(manager, roomId);
        assertImageBelongsToRoom(roomId, imageId);
        setPrimaryImage(imageId);
    }

    @Transactional
    public List<RoomDetailResponse.RoomImageInfo> reorderImagesForManager(
            User manager, UUID roomId, ReorderImagesRequest request) {
        validateManagerRoomAccess(manager, roomId);
        List<UUID> imageIds = request.getImageIds().stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
        return reorderImages(roomId, imageIds);
    }

    private void validateManagerRoomAccess(User manager, UUID roomId) {
        Room room = findActiveById(roomId);
        scopeValidator.validateManagerAccess(manager, room.getProperty().getId());
    }

    private void assertImageBelongsToRoom(UUID roomId, UUID imageId) {
        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ảnh"));
        if (!image.getRoom().getId().equals(roomId)) {
            throw new BusinessException("Ảnh không thuộc phòng này");
        }
    }

    private RoomDetailResponse.RoomImageInfo toRoomImageInfo(RoomImage saved) {
        RoomDetailResponse.RoomImageInfo info = new RoomDetailResponse.RoomImageInfo();
        info.setId(saved.getId());
        info.setImageUrl(saved.getImageUrl());
        info.setIsPrimary(saved.getIsPrimary());
        info.setSortOrder(saved.getSortOrder());
        return info;
    }

    // ── Private helper ────────────────────────────────────────────────────────

    /** FR-010 — public discovery date rules. */
    private void validatePublicSearchDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null && checkOut == null) {
            return;
        }
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Cần cả ngày check-in và check-out");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException("Ngày check-out phải sau ngày check-in");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException("Ngày check-in không được trong quá khứ");
        }
    }

    /** Public discovery: room must exist, not soft-deleted, ACTIVE property. */
    private Room findPublicDiscoverableRoom(UUID id) {
        Room room = findActiveById(id);
        if (room.getProperty() == null || room.getProperty().getStatus() != Property.Status.ACTIVE) {
            throw new ResourceNotFoundException("Không tìm thấy phòng với ID: " + id);
        }
        return room;
    }

    /** Manager / gallery / booking inventory — exclude soft-deleted. */
    private Room findActiveById(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng với ID: " + id));
        if (room.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng với ID: " + id);
        }
        return room;
    }

    private Room findById(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng với ID: " + id));
    }

    private PageResponse<RoomSummaryResponse> toPageResponse(Page<Room> page) {
        return new PageResponse<>(
                page.getContent().stream().map(RoomSummaryResponse::fromEntity).collect(Collectors.toList()),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }
}
