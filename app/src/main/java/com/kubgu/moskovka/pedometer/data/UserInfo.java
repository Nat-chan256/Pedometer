package com.kubgu.moskovka.pedometer.data;

public class UserInfo {

    private int growth;
    private int stepLength;
    private int weight;

    public UserInfo(int growth, int stepLength, int weight)
    {
        this.growth = growth;
        this.stepLength = stepLength;
        this.weight = weight;
    }

    public int getGrowth()
    {
        return growth;
    }

    public int getStepLength()
    {
        return stepLength;
    }

    public int getWeight()
    {
        return weight;
    }

    public void setGrowth(int growth) {
        this.growth = growth;
    }

    public void setStepLength(int stepLength) {
        this.stepLength = stepLength;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
