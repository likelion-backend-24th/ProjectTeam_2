package org.example.backend.payment.repository;

import org.example.backend.payment.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// 주문 저장/조회
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}