package com.homestay.service;

import com.homestay.entity.Room;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * FR-14 — Maintain denormalized {@code rooms.average_rating} / {@code total_reviews}
 * from Published reviews only.
 */
@Service
public class RoomRatingAggregateService {

    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;

    public RoomRatingAggregateService(ReviewRepository reviewRepository, RoomRepository roomRepository) {
        this.reviewRepository = reviewRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public void recalculate(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng"));

        Double avg = reviewRepository.averageRatingByRoomId(roomId);
        long count = reviewRepository.countPublishedByRoomId(roomId);

        if (count == 0 || avg == null) {
            room.setAverageRating(null);
            room.setTotalReviews(0);
        } else {
            room.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            room.setTotalReviews((int) count);
        }
        roomRepository.save(room);
    }
}
