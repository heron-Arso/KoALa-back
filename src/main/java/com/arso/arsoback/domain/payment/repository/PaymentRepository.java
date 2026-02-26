package com.arso.arsoback.domain.payment.repository;

import com.arso.arsoback.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}