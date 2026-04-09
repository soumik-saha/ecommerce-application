package com.app.ecom.repository;

import com.app.ecom.model.Order;
import com.app.ecom.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
}
