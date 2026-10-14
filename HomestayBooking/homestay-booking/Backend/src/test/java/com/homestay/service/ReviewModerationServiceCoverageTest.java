package com.homestay.service;

import com.homestay.dto.request.ModerateReviewRequest;
import com.homestay.dto.response.ModerationReviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.ReviewRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewModerationServiceCoverageTest {

    @Mock ReviewRepository reviewRepository;
    @Mock RoomRatingAggregateService roomRatingAggregateService;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ActivityLogService activityLogService;

    private ReviewModerationService service;
    private User manager;
    private User admin;
    private Property property;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewModerationService(
                reviewRepository, roomRatingAggregateService, scopeValidator, activityLogService);
        manager = TestFixtures.user(User.Role.MANAGER);
        admin = TestFixtures.user(User.Role.ADMIN);
        property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_OUT);
        review = new Review();
        review.setId(UUID.randomUUID());
        review.setRoom(room);
        review.setBooking(booking);
        review.setCustomer(customer);
        review.setRating(5);
        review.setComment("Great stay with friendly staff and clean rooms.");
        review.setStatus(Review.Status.PUBLISHED);
    }

    @Test
    void listForManager_requiresProperty() {
        assertThrows(BusinessException.class, () ->
                service.listForManager(manager, null, null, PageRequest.of(0, 10)));
    }

    @Test
    void listForManager_ok() {
        when(reviewRepository.findByPropertyIdAndOptionalStatus(
                eq(property.getId()), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        PageResponse<ModerationReviewResponse> page =
                service.listForManager(manager, property.getId(), null, PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void listForAdmin_ok() {
        when(reviewRepository.findAllByOptionalStatus(eq(Review.Status.HIDDEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        assertEquals(1, service.listForAdmin("HIDDEN", PageRequest.of(0, 10)).getContent().size());
    }

    @Test
    void moderateForManager_hides() {
        when(reviewRepository.findWithDetailsById(review.getId())).thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerateReviewRequest req = new ModerateReviewRequest();
        req.setStatus("HIDDEN");
        ModerationReviewResponse res = service.moderateForManager(review.getId(), req, manager);
        assertEquals(Review.Status.HIDDEN.name(), res.getStatus());
        verify(roomRatingAggregateService).recalculate(review.getRoom().getId());
    }

    @Test
    void moderateForAdmin_idempotent() {
        when(reviewRepository.findWithDetailsById(review.getId())).thenReturn(Optional.of(review));
        ModerateReviewRequest req = new ModerateReviewRequest();
        req.setStatus("PUBLISHED");
        assertEquals(Review.Status.PUBLISHED.name(),
                service.moderateForAdmin(review.getId(), req, admin).getStatus());
    }
}
