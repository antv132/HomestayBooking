package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PropertyResponse;
import com.homestay.entity.Property;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.PropertyRepository;
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
class PropertyServiceTest {

    @Mock PropertyRepository propertyRepository;

    private PropertyService service;
    private Property property;

    @BeforeEach
    void setUp() {
        service = new PropertyService(propertyRepository);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        property.setAddress("12 Nguyen Hue");
    }

    @Test
    void getFeatured_capsLimit() {
        when(propertyRepository.findByStatus(eq(Property.Status.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(property)));
        assertEquals(1, service.getFeatured(50).size());
        verify(propertyRepository).findByStatus(eq(Property.Status.ACTIVE), any(PageRequest.class));
    }

    @Test
    void getAllPublic_searchPath() {
        when(propertyRepository.searchByNameOrAddressAndStatus(
                eq("hue"), eq(Property.Status.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(property)));
        PageResponse<PropertyResponse> page =
                service.getAllPublic(" hue ", "INACTIVE", PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getAllPublic_noSearch() {
        when(propertyRepository.findByStatus(eq(Property.Status.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(property)));
        assertEquals(1, service.getAllPublic(null, "bogus", PageRequest.of(0, 5)).getContent().size());
    }

    @Test
    void getPublicById_rejectsInactive() {
        property.setStatus(Property.Status.INACTIVE);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        assertThrows(ResourceNotFoundException.class, () -> service.getPublicById(property.getId()));
    }

    @Test
    void getPublicById_ok() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        assertEquals(property.getId(), service.getPublicById(property.getId()).getId());
    }

    @Test
    void getPublicById_missing() {
        when(propertyRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getPublicById(UUID.randomUUID()));
    }
}
