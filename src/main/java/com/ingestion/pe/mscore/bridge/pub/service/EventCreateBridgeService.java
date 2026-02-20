package com.ingestion.pe.mscore.bridge.pub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EventCreateBridgeService {
    public void createEvent(Object event) {
        log.info("Event published: {}", event);
    }
}
