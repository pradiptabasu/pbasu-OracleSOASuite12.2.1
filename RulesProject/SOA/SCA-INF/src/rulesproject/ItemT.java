package rulesproject;

import java.math.BigDecimal;

/*
 *  A simple Java bean class to be used as a Java Fact in the rule engine.
 *
 *  @author bob Webster   Feb 2013
 */

public class ItemT {

    String productId;
    int    quantity;
    BigDecimal unitPrice;
    boolean extraCharge;
    String productWeight;
    BigDecimal shipSurcharge;


    public ItemT() {
        super();
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setExtraCharge(boolean extraCharge) {
        this.extraCharge = extraCharge;
    }

    public boolean isExtraCharge() {
        return extraCharge;
    }

    public void setProductWeight(String productWeight) {
        this.productWeight = productWeight;
    }

    public String getProductWeight() {
        return productWeight;
    }

    public void setShipSurcharge(BigDecimal shipSurcharge) {
        this.shipSurcharge = shipSurcharge;
    }

    public BigDecimal getShipSurcharge() {
        return shipSurcharge;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ItemT)) {
            return false;
        }
        final ItemT other = (ItemT)object;
        if (!(productId == null ? other.productId == null : productId.equals(other.productId))) {
            return false;
        }
        if (quantity != other.quantity) {
            return false;
        }
        if (!(unitPrice == null ? other.unitPrice == null : unitPrice.equals(other.unitPrice))) {
            return false;
        }
        if (extraCharge != other.extraCharge) {
            return false;
        }
        if (!(productWeight == null ? other.productWeight == null : productWeight.equals(other.productWeight))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 37;
        int result = 1;
        result = PRIME * result + ((productId == null) ? 0 : productId.hashCode());
        result = PRIME * result + quantity;
        result = PRIME * result + ((unitPrice == null) ? 0 : unitPrice.hashCode());
        result = PRIME * result + (extraCharge ? 0 : 1);
        result = PRIME * result + ((productWeight == null) ? 0 : productWeight.hashCode());
        return result;
    }
}
