package com.ingestion.pe.mscore.bridge.pub.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EventCreateBridgeService {
    public void createEvent(Object event) {
        log.info("Event published: {}", event);
    }
}
