package com.homestay.service;

import com.homestay.dto.request.CreateReviewRequest;
import com.homestay.dto.request.ModerateReviewRequest;
import com.homestay.dto.response.ModerationReviewResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceFr14Test {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock RoomRepository roomRepository;
    @Mock ActivityLogService activityLogService;
    @Mock ReportPropertyScopeValidator scopeValidator;

    private RoomRatingAggregateService aggregateService;
    private ReviewService reviewService;
    private ReviewModerationService moderationService;

    private User customer;
    private User manager;
    private Property property;
    private Room room;
    private Booking booking;

    @BeforeEach
    void setUp() {
        aggregateService = new RoomRatingAggregateService(reviewRepository, roomRepository);
        reviewService = new ReviewService(
                reviewRepository, bookingRepository, aggregateService, activityLogService);
        moderationService = new ReviewModerationService(
                reviewRepository, aggregateService, scopeValidator, activityLogService);

        customer = user(User.Role.CUSTOMER);
        manager = user(User.Role.MANAGER);
        property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Homestay A");
        room = new Room();
        room.setId(UUID.randomUUID());
        room.setProperty(property);
        room.setRoomNumber("101");
        room.setTotalReviews(0);
        booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setStatus(Booking.Status.CHECKED_OUT);
    }

    private static User user(User.Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFullName(role.name());
        u.setEmail(role.name().toLowerCase() + "@test.local");
        u.setRole(role);
        u.setStatus(User.Status.ACTIVE);
        return u;
    }

    @Test
    void submitReview_duplicate_throwsConflict() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(booking.getId());
        req.setRating(5);
        req.setComment("Phòng rất sạch sẽ và nhân viên thân thiện.");
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> reviewService.submitReview(req, customer));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void recalculate_publishedOnly() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(reviewRepository.averageRatingByRoomId(room.getId())).thenReturn(4.5);
        when(reviewRepository.countPublishedByRoomId(room.getId())).thenReturn(2L);

        aggregateService.recalculate(room.getId());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("4.50").compareTo(captor.getValue().getAverageRating()));
        assertEquals(2, captor.getValue().getTotalReviews());
    }

    @Test
    void moderateForManager_crossProperty_forbidden() {
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setRoom(room);
        review.setBooking(booking);
        review.setCustomer(customer);
        review.setStatus(Review.Status.PUBLISHED);
        when(reviewRepository.findWithDetailsById(review.getId())).thenReturn(Optional.of(review));
        org.mockito.Mockito.doThrow(new ForbiddenException("no access"))
                .when(scopeValidator).validateManagerAccess(manager, property.getId());

        ModerateReviewRequest req = new ModerateReviewRequest();
        req.setStatus("HIDDEN");

        assertThrows(ForbiddenException.class,
                () -> moderationService.moderateForManager(review.getId(), req, manager));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void moderateForManager_hide_logsAndRecalculates() {
        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setRoom(room);
        review.setBooking(booking);
        review.setCustomer(customer);
        review.setRating(4);
        review.setStatus(Review.Status.PUBLISHED);
        when(reviewRepository.findWithDetailsById(review.getId())).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(reviewRepository.averageRatingByRoomId(room.getId())).thenReturn(null);
        when(reviewRepository.countPublishedByRoomId(room.getId())).thenReturn(0L);

        ModerateReviewRequest req = new ModerateReviewRequest();
        req.setStatus("HIDDEN");

        ModerationReviewResponse res = moderationService.moderateForManager(review.getId(), req, manager);

        assertEquals("HIDDEN", res.getStatus());
        verify(activityLogService).log(
                eq(manager),
                eq(ActivityLogService.REVIEW_HIDDEN),
                eq("Review"),
                eq(review.getId()),
                eq("PUBLISHED → HIDDEN"));
        verify(roomRepository).save(any(Room.class));
    }
}
