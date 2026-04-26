package models;

public class Product {
    private String productId;
    private String categoryName;
    private Integer weightG;

    public Product() {
    }

    public Product(String productId, String categoryName, Integer weightG) {
        this.productId = productId;
        this.categoryName = categoryName;
        this.weightG = weightG;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getWeightG() {
        return weightG;
    }

    public void setWeightG(Integer weightG) {
        this.weightG = weightG;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", weightG=" + weightG +
                '}';
    }
}
