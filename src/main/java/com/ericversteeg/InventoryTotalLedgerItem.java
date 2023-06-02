package com.ericversteeg;

public class InventoryTotalLedgerItem {
    private final String description;
    private long qty;
    private final long amount;

    public InventoryTotalLedgerItem(String description, long qty, long amount)
    {
        this.description = description;
        this.qty = qty;
        this.amount = amount;
    }

    public String getDescription()
    {
        return description;
    }

    public long getQty()
    {
        return qty;
    }

    public long getAmount()
    {
        return amount;
    }

    public void addQuantityDifference(long qtyDifference)
    {
        qty += qtyDifference;
    }
}
