package com.app.ecom.service;

import com.app.ecom.dto.ReturnCreateRequest;
import com.app.ecom.dto.ReturnResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Order;
import com.app.ecom.model.OrderStatus;
import com.app.ecom.model.OrderStatusHistory;
import com.app.ecom.model.ReturnRequest;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.OrderStatusHistoryRepository;
import com.app.ecom.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional
    public ReturnResponse createReturn(Long userId, ReturnCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to the requesting user");
        }

        if (returnRequestRepository.existsByOrderId(order.getId())) {
            throw new IllegalStateException("Return request already exists for this order");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setUser(order.getUser());
        returnRequest.setReason(request.getReason());

        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);
        orderStatusHistoryRepository.save(buildStatusHistory(order, OrderStatus.RETURN_REQUESTED));

        log.info("Return request created with id={} for orderId={}", saved.getId(), order.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> getReturns(Long userId) {
        return returnRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ReturnResponse mapToResponse(ReturnRequest returnRequest) {
        ReturnResponse response = new ReturnResponse();
        response.setId(returnRequest.getId());
        response.setOrderId(returnRequest.getOrder().getId());
        response.setReason(returnRequest.getReason());
        response.setStatus(returnRequest.getStatus());
        response.setCreatedAt(returnRequest.getCreatedAt());
        return response;
    }

    private OrderStatusHistory buildStatusHistory(Order order, OrderStatus status) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        return history;
    }
}
