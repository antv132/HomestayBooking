package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {

    private long totalProperties;
    private long totalRooms;
    private long totalAvailableRooms;
    private double averageRating;
    private long totalReviews;
}
