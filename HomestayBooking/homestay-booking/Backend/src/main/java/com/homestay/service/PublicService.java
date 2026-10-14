package com.homestay.service;

import com.homestay.dto.response.PlatformStatsResponse;
import com.homestay.dto.response.SearchSuggestionResponse;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PublicService {

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final ReviewRepository reviewRepository;

    public PublicService(PropertyRepository propertyRepository,
                         RoomRepository roomRepository,
                         ReviewRepository reviewRepository) {
        this.propertyRepository = propertyRepository;
        this.roomRepository = roomRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        long totalProperties = propertyRepository.countByStatus(Property.Status.ACTIVE);
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.countByStatus(Room.Status.AVAILABLE);

        Double avg = reviewRepository.averagePublishedRating();
        double averageRating = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
        long totalReviews = reviewRepository.countPublished();

        return new PlatformStatsResponse(
                totalProperties,
                totalRooms,
                availableRooms,
                averageRating,
                totalReviews
        );
    }

    // Danh sách fallback khi DB chưa có property nào
    private static final List<String> FALLBACK_LOCATIONS = List.of(
            "Đà Lạt", "Hội An", "Đà Nẵng", "Phú Quốc", "Nha Trang", "Hà Nội", "Sapa", "Vũng Tàu"
    );

    @Transactional(readOnly = true)
    public List<SearchSuggestionResponse> getSearchSuggestions(String q) {
        String raw = q == null ? "" : q.trim();
        // FR-004: <2 chars → popular suggestions only (no filtered query)
        boolean filterQuery = raw.length() >= 2;
        String query = filterQuery ? raw.toLowerCase() : "";
        Set<SearchSuggestionResponse> results = new LinkedHashSet<>();

        Page<Property> propertyPage = filterQuery
                ? propertyRepository.findByNameContainingIgnoreCaseAndStatus(
                        raw, Property.Status.ACTIVE, PageRequest.of(0, 10))
                : propertyRepository.findByStatus(Property.Status.ACTIVE, PageRequest.of(0, 20));

        Set<String> dbCities = new LinkedHashSet<>();
        for (Property property : propertyPage.getContent()) {
            if (!filterQuery || property.getAddress().toLowerCase().contains(query)) {
                extractLocationFromAddress(property.getAddress()).ifPresent(dbCities::add);
            }
        }

        for (String city : dbCities) {
            results.add(new SearchSuggestionResponse("location", city));
        }

        for (String loc : FALLBACK_LOCATIONS) {
            if (!filterQuery || loc.toLowerCase().contains(query)) {
                results.add(new SearchSuggestionResponse("location", loc));
            }
        }

        for (Property property : propertyPage.getContent()) {
            if (!filterQuery || property.getName().toLowerCase().contains(query)) {
                results.add(new SearchSuggestionResponse("property", property.getName()));
            }
        }

        return new ArrayList<>(results).stream().limit(15).toList();
    }

    private java.util.Optional<String> extractLocationFromAddress(String address) {
        if (address == null || address.isBlank()) return java.util.Optional.empty();
        String[] parts = address.split(",");
        if (parts.length == 0) return java.util.Optional.empty();
        String city = parts[parts.length - 1].trim();
        return city.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(city);
    }
}
