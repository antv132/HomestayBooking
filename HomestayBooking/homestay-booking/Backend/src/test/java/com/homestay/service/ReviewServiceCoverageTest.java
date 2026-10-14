package com.homestay.service;

import com.homestay.dto.request.CreateReviewRequest;
import com.homestay.dto.request.UpdateReviewRequest;
import com.homestay.dto.response.MyReviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
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
class ReviewServiceCoverageTest {

    @Mock ReviewRepository reviewRepository;
    @Mock BookingRepository bookingRepository;
    @Mock RoomRatingAggregateService roomRatingAggregateService;
    @Mock ActivityLogService activityLogService;

    private ReviewService service;
    private User customer;
    private Property property;
    private Room room;
    private Booking booking;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewService(
                reviewRepository, bookingRepository, roomRatingAggregateService, activityLogService);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_OUT);
        review = new Review();
        review.setId(UUID.randomUUID());
        review.setBooking(booking);
        review.setCustomer(customer);
        review.setRoom(room);
        review.setRating(5);
        review.setComment("Great stay with friendly staff and clean rooms.");
        review.setStatus(Review.Status.PUBLISHED);
    }

    @Test
    void submitReview_success() {
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(reviewRepository.findByIdAndCustomer_Id(any(), eq(customer.getId())))
                .thenReturn(Optional.empty());

        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(booking.getId());
        req.setRating(5);
        req.setComment("Great stay with friendly staff and clean rooms.");
        MyReviewResponse res = service.submitReview(req, customer);
        assertEquals(5, res.getRating());
        verify(roomRatingAggregateService).recalculate(room.getId());
        verify(activityLogService).log(eq(customer), eq(ActivityLogService.REVIEW_CREATED),
                eq("Review"), any(), any());
    }

    @Test
    void submitReview_forbiddenOtherCustomer() {
        User other = TestFixtures.user(User.Role.CUSTOMER);
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(booking.getId());
        req.setRating(4);
        req.setComment("Great stay with friendly staff and clean rooms.");
        assertThrows(ForbiddenException.class, () -> service.submitReview(req, other));
    }

    @Test
    void submitReview_notCheckedOut() {
        booking.setStatus(Booking.Status.CONFIRMED);
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(booking.getId());
        req.setRating(4);
        req.setComment("Great stay with friendly staff and clean rooms.");
        assertThrows(BusinessException.class, () -> service.submitReview(req, customer));
    }

    @Test
    void submitReview_duplicate() {
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(true);
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(booking.getId());
        req.setRating(4);
        req.setComment("Great stay with friendly staff and clean rooms.");
        assertThrows(ConflictException.class, () -> service.submitReview(req, customer));
    }

    @Test
    void submitReview_shortComment() {
        when(bookingRepository.findByIdWithRoomAndCustomer(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(booking.getId());
        req.setRating(4);
        req.setComment("too short");
        assertThrows(BusinessException.class, () -> service.submitReview(req, customer));
    }

    @Test
    void getMyReviews() {
        when(reviewRepository.findByCustomer_IdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        PageResponse<MyReviewResponse> page = service.getMyReviews(customer, PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getReviewByIdForCustomer_missing() {
        when(reviewRepository.findByIdAndCustomer_Id(any(), eq(customer.getId()))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.getReviewByIdForCustomer(UUID.randomUUID(), customer));
    }

    @Test
    void updateReview_success() {
        when(reviewRepository.findByIdAndCustomer_Id(review.getId(), customer.getId()))
                .thenReturn(Optional.of(review));
        when(reviewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setRating(4);
        req.setComment("Updated review with enough characters for validation.");
        MyReviewResponse res = service.updateReview(review.getId(), req, customer);
        assertEquals(4, res.getRating());
        verify(roomRatingAggregateService).recalculate(room.getId());
    }

    @Test
    void deleteReview_success() {
        when(reviewRepository.findByIdAndCustomer_Id(review.getId(), customer.getId()))
                .thenReturn(Optional.of(review));
        service.deleteReview(review.getId(), customer);
        verify(reviewRepository).delete(review);
        verify(roomRatingAggregateService).recalculate(room.getId());
        verify(activityLogService).log(eq(customer), eq(ActivityLogService.REVIEW_DELETED),
                eq("Review"), eq(review.getId()), any());
    }
}
