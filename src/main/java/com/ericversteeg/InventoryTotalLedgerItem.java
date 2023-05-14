package com.ericversteeg;

public class InventoryTotalLedgerItem {
    private final String description;
    private int qty;
    private final int amount;

    public InventoryTotalLedgerItem(String description, int qty, int amount)
    {
        this.description = description;
        this.qty = qty;
        this.amount = amount;
    }

    public String getDescription()
    {
        return description;
    }

    public int getQty()
    {
        return qty;
    }

    public int getAmount()
    {
        return amount;
    }

    public void addQuantityDifference(int qtyDifference)
    {
        qty += qtyDifference;
    }
}
