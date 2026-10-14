package com.homestay.repository;

import com.homestay.entity.BookingCheckVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingCheckVerificationRepository extends JpaRepository<BookingCheckVerification, UUID> {
}
