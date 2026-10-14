package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PlatformStatsResponse;
import com.homestay.dto.response.SearchSuggestionResponse;
import com.homestay.service.PublicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/public", "/api/v1/public"})
public class PublicController {

    private final PublicService publicService;

    public PublicController(PublicService publicService) {
        this.publicService = publicService;
    }

    // Thống kê nền tảng cho stats band trang chủ (SCR-01)
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.ok(publicService.getPlatformStats()));
    }

    // Gợi ý tìm kiếm cho hero search bar (SCR-01)
    @GetMapping("/search-suggestions")
    public ResponseEntity<ApiResponse<List<SearchSuggestionResponse>>> getSearchSuggestions(
            @RequestParam(required = false) String q) {

        return ResponseEntity.ok(ApiResponse.ok(publicService.getSearchSuggestions(q)));
    }
}
