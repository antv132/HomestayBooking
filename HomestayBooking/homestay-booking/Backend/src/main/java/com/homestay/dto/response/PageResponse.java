package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Wrapper cho response có phân trang
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;     // danh sách dữ liệu trang hiện tại
    private int page;            // trang hiện tại (bắt đầu từ 0)
    private int size;            // số item mỗi trang
    private long totalElements;  // tổng số item
    private int totalPages;      // tổng số trang
}
