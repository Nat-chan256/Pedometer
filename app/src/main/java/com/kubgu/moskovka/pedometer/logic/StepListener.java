package com.kubgu.moskovka.pedometer.logic;

// Will listen to step alerts
public interface StepListener {

    public void step(long timeNs);

}