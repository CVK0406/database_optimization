package models;

import java.time.LocalDateTime;

public class Order {
    private String orderId;
    private String customerId;
    private String status;
    private LocalDateTime orderPurchaseTimestamp;

    public Order() {
    }

    public Order(String orderId, String customerId, String status, LocalDateTime orderPurchaseTimestamp) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.orderPurchaseTimestamp = orderPurchaseTimestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOrderPurchaseTimestamp() {
        return orderPurchaseTimestamp;
    }

    public void setOrderPurchaseTimestamp(LocalDateTime orderPurchaseTimestamp) {
        this.orderPurchaseTimestamp = orderPurchaseTimestamp;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", status='" + status + '\'' +
                ", orderPurchaseTimestamp=" + orderPurchaseTimestamp +
                '}';
    }
}
