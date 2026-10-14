package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PaymentReconciliationResponse;
import com.homestay.entity.Payment;
import com.homestay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SCR-52 - Payment Reconciliation (Admin). Read-only: liet ke payment VNPAY bi lech.
 */
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public PageResponse<PaymentReconciliationResponse> listReconciliation(Pageable pageable) {
        Page<Payment> page = paymentRepository.findVnpayDiscrepancies(
                Payment.Method.VNPAY,
                Payment.Status.PAID,
                Payment.Status.PENDING,
                pageable);

        return new PageResponse<>(
                page.getContent().stream().map(PaymentReconciliationResponse::fromEntity).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}