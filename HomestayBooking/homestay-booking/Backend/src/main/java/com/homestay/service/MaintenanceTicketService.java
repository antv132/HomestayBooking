package com.homestay.service;

import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.entity.*;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import com.homestay.util.UploadLimits;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenanceTicketService {

    private final MaintenanceTicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final ActivityLogService activityLogService;
    private final MaintenanceTaskManagerService maintenanceTaskManagerService;

    public MaintenanceTicketService(MaintenanceTicketRepository ticketRepository,
                                    BookingRepository bookingRepository,
                                    UserRepository userRepository,
                                    RoomRepository roomRepository,
                                    ReportPropertyScopeValidator scopeValidator,
                                    ActivityLogService activityLogService,
                                    MaintenanceTaskManagerService maintenanceTaskManagerService) {
        this.ticketRepository = ticketRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.scopeValidator = scopeValidator;
        this.activityLogService = activityLogService;
        this.maintenanceTaskManagerService = maintenanceTaskManagerService;
    }

    private Map<String, Object> ticketToMap(MaintenanceTicket ticket) {
        return ticketToMap(ticket, true);
    }

    /** @param forCustomer when true, hide resolutionNote until CLOSED and never expose workNote */
    private Map<String, Object> ticketToMap(MaintenanceTicket ticket, boolean forCustomer) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ticket.getId().toString());
        m.put("customerId", ticket.getCustomer().getId().toString());
        m.put("customerName", ticket.getCustomer().getFullName());
        m.put("customerEmail", ticket.getCustomer().getEmail());
        m.put("roomId", ticket.getRoom().getId().toString());
        m.put("roomNumber", ticket.getRoom().getRoomNumber());
        m.put("propertyId", ticket.getRoom().getProperty().getId().toString());
        m.put("propertyName", ticket.getRoom().getProperty().getName());
        m.put("title", ticket.getTitle());
        m.put("description", ticket.getDescription());

        List<String> photos = parsePhotoUrls(ticket.getPhotoUrls());
        m.put("photoUrls", photos);

        m.put("status", ticket.getStatus().name());
        if (forCustomer) {
            m.put("resolutionNote",
                    ticket.getStatus() == MaintenanceTicket.Status.CLOSED
                            ? ticket.getResolutionNote()
                            : null);
        } else {
            m.put("resolutionNote", ticket.getResolutionNote());
            m.put("workNote", ticket.getWorkNote());
        }
        m.put("createdAt", ticket.getCreatedAt().toString());
        m.put("updatedAt", ticket.getUpdatedAt().toString());
        return m;
    }

    private static List<String> parsePhotoUrls(String photoUrls) {
        List<String> photos = new ArrayList<>();
        if (photoUrls == null || photoUrls.isBlank()) {
            return photos;
        }
        String raw = photoUrls.trim();
        if (raw.startsWith("[")) {
            raw = raw.substring(1, raw.length() - 1);
            for (String s : raw.split(",")) {
                s = s.trim().replaceAll("^\"|\"$", "");
                if (!s.isEmpty()) {
                    photos.add(s);
                }
            }
        } else {
            photos.add(raw);
        }
        return photos;
    }

    private static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "photo.jpg";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCustomerTicketsPaged(UUID customerId, String status, int page, int size) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        MaintenanceTicket.Status ticketStatus = parseStatusFilter(status);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<MaintenanceTicket> result = ticketRepository.findByCustomerPaged(customer, ticketStatus, pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getContent().stream()
                .map(t -> ticketToMap(t, true))
                .collect(Collectors.toList()));
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        return response;
    }

    private static MaintenanceTicket.Status parseStatusFilter(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return MaintenanceTicket.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Map<String, Object> createTicketFromForm(UUID customerId, UUID roomId, String title,
                                                    String description, List<MultipartFile> photos) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<Booking> bookings = bookingRepository.findByCustomerOrderByCreatedAtDesc(customer);
        Booking matchingBooking = bookings.stream()
                .filter(b -> b.getRoom().getId().equals(roomId))
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED
                        || b.getStatus() == Booking.Status.CHECKED_IN)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "No active booking found for this room. You can only submit maintenance requests for Confirmed or Checked-in stays."));

        if (title == null || title.isBlank()) {
            throw new BusinessException("Tiêu đề không được để trống");
        }
        if (description == null || description.length() < 20) {
            throw new BusinessException("Mô tả phải có ít nhất 20 ký tự");
        }

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setCustomer(customer);
        ticket.setRoom(room);
        ticket.setBooking(matchingBooking);
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setStatus(MaintenanceTicket.Status.OPEN);

        savePhotosToTicket(ticket, photos);

        MaintenanceTicket saved = ticketRepository.saveAndFlush(ticket);
        saved = ticketRepository.findActiveById(saved.getId()).orElse(saved);
        activityLogService.log(customer, ActivityLogService.TICKET_CREATED, "MaintenanceTicket",
                saved.getId(), "Created OPEN ticket: " + saved.getTitle());
        return ticketToMap(saved, true);
    }

    public Map<String, Object> createTicketFromBooking(UUID customerId, UUID bookingId, String title,
                                                       String description, List<MultipartFile> files) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Tiêu đề không được để trống");
        }
        if (description == null || description.length() < 20) {
            throw new BusinessException("Mô tả phải có ít nhất 20 ký tự");
        }
        if (files != null && files.size() > 5) {
            throw new BusinessException("Chỉ được tải lên tối đa 5 ảnh");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Booking booking = bookingRepository.findByIdWithRoomAndCustomer(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Đặt phòng không tồn tại"));

        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Bạn không có quyền tạo yêu cầu cho đặt phòng này");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED
                && booking.getStatus() != Booking.Status.CHECKED_IN) {
            throw new BusinessException("Chỉ có thể báo cáo sự cố cho đặt phòng đang hiệu lực");
        }

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setCustomer(customer);
        ticket.setRoom(booking.getRoom());
        ticket.setBooking(booking);
        ticket.setTitle(title.trim());
        ticket.setDescription(description.trim());
        ticket.setStatus(MaintenanceTicket.Status.OPEN);

        savePhotosToTicket(ticket, files);

        MaintenanceTicket saved = ticketRepository.saveAndFlush(ticket);
        saved = ticketRepository.findActiveById(saved.getId()).orElse(saved);
        activityLogService.log(customer, ActivityLogService.TICKET_CREATED, "MaintenanceTicket",
                saved.getId(), "Created OPEN ticket: " + saved.getTitle());
        return ticketToMap(saved, true);
    }

    private void savePhotosToTicket(MaintenanceTicket ticket, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        if (photos.size() > 5) {
            throw new BusinessException("Chỉ được tải lên tối đa 5 ảnh");
        }

        List<String> photoUrls = new ArrayList<>();
        java.nio.file.Path ticketUploadDir = java.nio.file.Paths.get("uploads", "tickets").toAbsolutePath();
        try {
            java.nio.file.Files.createDirectories(ticketUploadDir);
        } catch (java.io.IOException e) {
            throw new BusinessException("Failed to create upload directory: " + e.getMessage());
        }

        for (MultipartFile photo : photos) {
            if (photo.isEmpty()) {
                continue;
            }
            UploadLimits.validateMaintenanceImage(photo);
            String savedName = UUID.randomUUID() + "_" + sanitizeFilename(photo.getOriginalFilename());
            java.nio.file.Path targetPath = ticketUploadDir.resolve(savedName);
            try {
                java.nio.file.Files.copy(photo.getInputStream(), targetPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.io.IOException e) {
                throw new BusinessException("Failed to save photo: " + e.getMessage());
            }
            photoUrls.add("/uploads/tickets/" + savedName);
        }
        if (!photoUrls.isEmpty()) {
            ticket.setPhotoUrls(photoUrls.toString());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTicketDetail(User user, UUID ticketId) {
        MaintenanceTicket ticket = ticketRepository.findActiveById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        boolean forCustomer = true;
        if (user.getRole() == User.Role.MANAGER || user.getRole() == User.Role.ADMIN) {
            if (user.getRole() == User.Role.MANAGER) {
                scopeValidator.validateManagerAccess(user, ticket.getRoom().getProperty().getId());
            }
            forCustomer = false;
        } else if (!ticket.getCustomer().getId().equals(user.getId())) {
            throw new BusinessException("You do not have permission to view this ticket");
        }

        return ticketToMap(ticket, forCustomer);
    }

    public Map<String, Object> updateTicketContent(UUID customerId, UUID ticketId, String title, String description,
                                                    List<String> existingPhotoUrls, List<MultipartFile> newPhotos) {
        MaintenanceTicket ticket = ticketRepository.findActiveById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (!ticket.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You do not have permission to update this ticket");
        }
        if (ticket.getStatus() != MaintenanceTicket.Status.OPEN) {
            throw new BusinessException("Cannot update ticket because it is already being processed");
        }

        if (title != null && !title.isBlank()) {
            ticket.setTitle(title);
        }
        if (description != null && !description.isBlank()) {
            if (description.length() < 20) {
                throw new BusinessException("Mô tả phải có ít nhất 20 ký tự");
            }
            ticket.setDescription(description);
        }

        List<String> currentPhotos = parsePhotoUrls(ticket.getPhotoUrls());
        List<String> keptPhotos = new ArrayList<>();
        if (existingPhotoUrls != null) {
            keptPhotos.addAll(existingPhotoUrls);
        }

        for (String oldUrl : currentPhotos) {
            if (!keptPhotos.contains(oldUrl)) {
                try {
                    java.nio.file.Path filePath = java.nio.file.Paths.get("uploads").toAbsolutePath()
                            .resolve(oldUrl.replaceFirst("^/uploads/", ""));
                    java.nio.file.Files.deleteIfExists(filePath);
                } catch (java.io.IOException ignored) {
                }
            }
        }

        List<String> newPhotoUrls = new ArrayList<>();
        if (newPhotos != null && !newPhotos.isEmpty()) {
            java.nio.file.Path ticketUploadDir = java.nio.file.Paths.get("uploads", "tickets").toAbsolutePath();
            try {
                java.nio.file.Files.createDirectories(ticketUploadDir);
            } catch (java.io.IOException e) {
                throw new BusinessException("Failed to create upload directory: " + e.getMessage());
            }
            for (MultipartFile photo : newPhotos) {
                if (photo.isEmpty()) {
                    continue;
                }
                UploadLimits.validateMaintenanceImage(photo);
                String savedName = UUID.randomUUID() + "_" + sanitizeFilename(photo.getOriginalFilename());
                java.nio.file.Path targetPath = ticketUploadDir.resolve(savedName);
                try {
                    java.nio.file.Files.copy(photo.getInputStream(), targetPath,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.io.IOException e) {
                    throw new BusinessException("Failed to save photo: " + e.getMessage());
                }
                newPhotoUrls.add("/uploads/tickets/" + savedName);
            }
        }

        List<String> finalPhotos = new ArrayList<>(keptPhotos);
        finalPhotos.addAll(newPhotoUrls);
        if (finalPhotos.size() > 5) {
            throw new BusinessException("Chỉ được tải lên tối đa 5 ảnh");
        }

        ticket.setPhotoUrls(finalPhotos.isEmpty() ? null : finalPhotos.toString());

        MaintenanceTicket updated = ticketRepository.save(ticket);
        User customer = updated.getCustomer();
        activityLogService.log(customer, ActivityLogService.TICKET_UPDATED, "MaintenanceTicket",
                updated.getId(), "Updated OPEN ticket content");
        return ticketToMap(updated, true);
    }

    public void deleteTicket(UUID customerId, UUID ticketId) {
        MaintenanceTicket ticket = ticketRepository.findActiveById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (!ticket.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You do not have permission to delete this ticket");
        }

        if (ticket.getStatus() != MaintenanceTicket.Status.OPEN) {
            throw new BusinessException("Cannot delete ticket because it is already being processed");
        }

        ticket.setDeletedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        activityLogService.log(ticket.getCustomer(), ActivityLogService.TICKET_DELETED, "MaintenanceTicket",
                ticket.getId(), "Soft-deleted OPEN ticket");
    }

    /**
     * Legacy global manager list — blocked (FR-013 property scope).
     * Use {@code GET /api/v1/manager/maintenance-tasks?propertyId=}.
     */
    public Map<String, Object> getAllTicketsPaged(String status, int page, int size) {
        throw new BusinessException(
                "Danh sách bảo trì toàn cục đã tắt. Dùng GET /api/v1/manager/maintenance-tasks?propertyId=");
    }

    /**
     * Legacy free-form status update — only Verify&Close (RESOLVED→CLOSED) via manager service.
     */
    public Map<String, Object> updateTicketStatusAndReturn(User manager, UUID ticketId,
                                                           UpdateMaintenanceStatusRequest request) {
        if (request.getStatus() != MaintenanceTicket.Status.CLOSED) {
            throw new BusinessException(
                    "Manager chỉ được đóng yêu cầu đã xử lý (RESOLVED→CLOSED). "
                            + "Dùng PATCH /api/v1/manager/maintenance-tasks/{id}/status");
        }
        maintenanceTaskManagerService.updateStatusForManager(manager, ticketId, request);
        MaintenanceTicket ticket = ticketRepository.findActiveById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return ticketToMap(ticket, false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveBookingsForCustomer(UUID customerId) {
        if (!userRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found");
        }

        List<Booking.Status> activeStatuses = List.of(
                Booking.Status.CONFIRMED,
                Booking.Status.CHECKED_IN
        );
        List<Booking> activeBookings = bookingRepository.findActiveWithRoomPropertyByCustomerId(
                customerId, activeStatuses);

        return activeBookings.stream()
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bookingId", b.getId().toString());
                    m.put("roomId", b.getRoom().getId().toString());
                    m.put("roomNumber", b.getRoom().getRoomNumber());
                    m.put("propertyId", b.getRoom().getProperty().getId().toString());
                    m.put("propertyName", b.getRoom().getProperty().getName());
                    m.put("checkInDate", b.getCheckInDate().toString());
                    m.put("checkOutDate", b.getCheckOutDate().toString());
                    m.put("status", b.getStatus().name());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listForAdmin(String status, int page, int size) {
        MaintenanceTicket.Status ticketStatus = parseStatusFilter(status);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<MaintenanceTicket> result = ticketRepository.findAllActive(ticketStatus, pageable);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getContent().stream()
                .map(t -> ticketToMap(t, false))
                .collect(Collectors.toList()));
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        return response;
    }
}
