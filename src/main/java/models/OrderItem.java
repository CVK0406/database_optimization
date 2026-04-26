package models;

import java.math.BigDecimal;

public class OrderItem {
    private String orderId;
    private Integer orderItemId;
    private String productId;
    private BigDecimal price;
    private BigDecimal freightValue;

    public OrderItem() {
    }

    public OrderItem(String orderId, Integer orderItemId, String productId, BigDecimal price, BigDecimal freightValue) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.price = price;
        this.freightValue = freightValue;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Integer orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getFreightValue() {
        return freightValue;
    }

    public void setFreightValue(BigDecimal freightValue) {
        this.freightValue = freightValue;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "orderId='" + orderId + '\'' +
                ", orderItemId=" + orderItemId +
                ", productId='" + productId + '\'' +
                ", price=" + price +
                ", freightValue=" + freightValue +
                '}';
    }
}
