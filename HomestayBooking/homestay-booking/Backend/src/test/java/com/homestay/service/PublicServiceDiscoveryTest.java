package com.homestay.service;

import com.homestay.dto.response.SearchSuggestionResponse;
import com.homestay.entity.Property;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicServiceDiscoveryTest {

    @Mock PropertyRepository propertyRepository;
    @Mock RoomRepository roomRepository;
    @Mock ReviewRepository reviewRepository;

    private PublicService service;

    @BeforeEach
    void setUp() {
        service = new PublicService(propertyRepository, roomRepository, reviewRepository);
    }

    @Test
    void getSearchSuggestions_shortQuery_returnsPopularWithoutNameFilter() {
        Property p = new Property();
        p.setName("Homestay Đà Nẵng");
        p.setAddress("12 Trần Phú, Đà Nẵng");
        p.setStatus(Property.Status.ACTIVE);
        when(propertyRepository.findByStatus(eq(Property.Status.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        List<SearchSuggestionResponse> result = service.getSearchSuggestions("D");

        assertFalse(result.isEmpty());
        verify(propertyRepository).findByStatus(eq(Property.Status.ACTIVE), any(Pageable.class));
    }

    @Test
    void getSearchSuggestions_validQuery_filtersByName() {
        when(propertyRepository.findByNameContainingIgnoreCaseAndStatus(
                eq("Đà"), eq(Property.Status.ACTIVE), any(Pageable.class)))
                .thenReturn(Page.empty());

        List<SearchSuggestionResponse> result = service.getSearchSuggestions("Đà");

        assertTrue(result.stream().anyMatch(s -> "location".equals(s.getType())));
        verify(propertyRepository).findByNameContainingIgnoreCaseAndStatus(
                eq("Đà"), eq(Property.Status.ACTIVE), any(Pageable.class));
    }
}
