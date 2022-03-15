package com.kubgu.moskovka.pedometer.adapter;

public class StatsItem {
    private String itemName;
    private double itemValue;

    public StatsItem(String itemName, double itemValue) {
        this.itemName = itemName;
        this.itemValue = itemValue;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getItemValue() {
        return (int)itemValue;
    }

    public double getAccurateItemValue()
    {
        return itemValue;
    }

    public void setItemValue(double itemValue) {
        this.itemValue = itemValue;
    }

    public StatsItem clone()
    {
        return new StatsItem(itemName, itemValue);
    }
}
