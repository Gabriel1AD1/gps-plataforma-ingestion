package com.ingestion.pe.mscore.domain.devices.app.manager;

import lombok.Getter;

@Getter
public class AlertRuntimeState {
    private boolean active;
    private long trueCount;
    private long falseCount;

    public void onTrue() {
        trueCount++;
        falseCount = 0;
    }

    public void onFalse() {
        falseCount++;
        trueCount = 0;
    }

    public void activate() {
        active = true;
        trueCount = 0;
        falseCount = 0;
    }

    public void deactivate() {
        active = false;
        trueCount = 0;
        falseCount = 0;
    }
}
