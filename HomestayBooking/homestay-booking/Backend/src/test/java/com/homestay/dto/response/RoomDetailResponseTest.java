package com.homestay.dto.response;

import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Review;
import com.homestay.entity.Room;
import com.homestay.entity.RoomImage;
import com.homestay.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoomDetailResponseTest {

    @Test
    void fromEntity_mapsAllBasicFields() {
        Room room = createRoomWithBasicDetails();

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getId()).isEqualTo(room.getId());
        assertThat(response.getPropertyId()).isEqualTo(room.getProperty().getId());
        assertThat(response.getPropertyName()).isEqualTo("Test Homestay");
        assertThat(response.getPropertyAddress()).isEqualTo("123 Test St");
        assertThat(response.getFloorId()).isEqualTo(room.getFloor().getId());
        assertThat(response.getFloorNumber()).isEqualTo(1);
        assertThat(response.getRoomNumber()).isEqualTo("101");
        assertThat(response.getRoomType()).isEqualTo("DELUXE");
        assertThat(response.getPricePerNight()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(response.getCapacity()).isEqualTo(2);
        assertThat(response.getArea()).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(response.getDescription()).isEqualTo("Cozy room");
        assertThat(response.getStatus()).isEqualTo("AVAILABLE");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void fromEntity_mapsAmenitiesList() {
        Room room = createRoomWithBasicDetails();
        room.setAmenities(Arrays.asList("WiFi", "AC", "TV"));

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getAmenities()).containsExactly("WiFi", "AC", "TV");
    }

    @Test
    void fromEntity_handlesNullAmenities() {
        Room room = createRoomWithBasicDetails();
        room.setAmenities(null);

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getAmenities()).isEmpty();
    }

    @Test
    void fromEntity_mapsRoomImages() {
        Room room = createRoomWithBasicDetails();
        List<RoomImage> images = new ArrayList<>();
        
        RoomImage img1 = new RoomImage();
        img1.setId(UUID.randomUUID());
        img1.setImageUrl("/images/room1.jpg");
        img1.setIsPrimary(true);
        img1.setSortOrder(1);
        images.add(img1);

        RoomImage img2 = new RoomImage();
        img2.setId(UUID.randomUUID());
        img2.setImageUrl("/images/room2.jpg");
        img2.setIsPrimary(false);
        img2.setSortOrder(2);
        images.add(img2);

        room.setRoomImages(images);

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getImages()).hasSize(2);
        assertThat(response.getImages().get(0).getImageUrl()).isEqualTo("/images/room1.jpg");
        assertThat(response.getImages().get(0).getIsPrimary()).isTrue();
        assertThat(response.getImages().get(0).getSortOrder()).isEqualTo(1);
        assertThat(response.getImages().get(1).getIsPrimary()).isFalse();
    }

    @Test
    void fromEntity_calculatesRatingFromDenormalizedFields() {
        Room room = createRoomWithBasicDetails();
        room.setAverageRating(new BigDecimal("4.5"));
        room.setTotalReviews(10);

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getAverageRating()).isEqualTo(4.5);
        assertThat(response.getTotalReviews()).isEqualTo(10);
    }

    @Test
    void fromEntity_calculatesRatingFromPublishedReviewsWhenNoDenormalizedData() {
        Room room = createRoomWithBasicDetails();
        room.setAverageRating(null);
        room.setTotalReviews(null);

        List<Review> reviews = new ArrayList<>();
        reviews.add(createReview(5, Review.Status.PUBLISHED));
        reviews.add(createReview(4, Review.Status.PUBLISHED));
        reviews.add(createReview(5, Review.Status.PUBLISHED));
        reviews.add(createReview(3, Review.Status.HIDDEN));
        room.setReviews(reviews);

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getTotalReviews()).isEqualTo(3);
        assertThat(response.getAverageRating()).isEqualTo(4.7);
    }

    @Test
    void fromEntity_handlesNoReviews() {
        Room room = createRoomWithBasicDetails();
        room.setReviews(new ArrayList<>());

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getTotalReviews()).isEqualTo(0);
        assertThat(response.getAverageRating()).isEqualTo(0.0);
    }

    @Test
    void fromEntity_mapsPublishedReviews() {
        Room room = createRoomWithBasicDetails();
        
        User customer = new User();
        customer.setFullName("John Doe");

        Review review1 = new Review();
        review1.setId(UUID.randomUUID());
        review1.setCustomer(customer);
        review1.setRating(5);
        review1.setComment("Excellent!");
        review1.setStatus(Review.Status.PUBLISHED);
        review1.setCreatedAt(LocalDateTime.now());

        Review review2 = new Review();
        review2.setId(UUID.randomUUID());
        review2.setCustomer(customer);
        review2.setRating(3);
        review2.setComment("Hidden");
        review2.setStatus(Review.Status.HIDDEN);
        review2.setCreatedAt(LocalDateTime.now());

        room.setReviews(Arrays.asList(review1, review2));

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getReviews()).hasSize(1);
        assertThat(response.getReviews().get(0).getRating()).isEqualTo(5);
        assertThat(response.getReviews().get(0).getComment()).isEqualTo("Excellent!");
        assertThat(response.getReviews().get(0).getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void fromEntity_roundsAverageRatingToOneDecimal() {
        Room room = createRoomWithBasicDetails();
        room.setReviews(Arrays.asList(
                createReview(5, Review.Status.PUBLISHED),
                createReview(5, Review.Status.PUBLISHED),
                createReview(4, Review.Status.PUBLISHED)
        ));

        RoomDetailResponse response = RoomDetailResponse.fromEntity(room);

        assertThat(response.getAverageRating()).isEqualTo(4.7);
    }

    private Room createRoomWithBasicDetails() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Test Homestay");
        property.setAddress("123 Test St");

        Floor floor = new Floor();
        floor.setId(UUID.randomUUID());
        floor.setFloorNumber(1);

        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setProperty(property);
        room.setFloor(floor);
        room.setRoomNumber("101");
        room.setRoomType("DELUXE");
        room.setPricePerNight(new BigDecimal("500000"));
        room.setCapacity(2);
        room.setArea(new BigDecimal("30"));
        room.setDescription("Cozy room");
        room.setStatus(Room.Status.AVAILABLE);
        room.setCreatedAt(LocalDateTime.now());
        room.setAmenities(new ArrayList<>());

        return room;
    }

    private Review createReview(int rating, Review.Status status) {
        User customer = new User();
        customer.setFullName("Reviewer");

        Review review = new Review();
        review.setId(UUID.randomUUID());
        review.setCustomer(customer);
        review.setRating(rating);
        review.setComment("Comment");
        review.setStatus(status);
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }
}
