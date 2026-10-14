package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDetailResponse {
    private InvoiceSummaryResponse invoice;
    private String propertyName;
    private String roomNumber;
}
