package com.municipal.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.municipal.dashboard.dto.HeightDetectionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MqttMessageHandler {
    
    private final ViolationService violationService;
    private final DetectorService detectorService;
    private final ObjectMapper objectMapper;
    
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = message.getPayload().toString();
            
            log.info("Received message from topic: {}", topic);
            log.debug("Payload: {}", payload);
            
            if (topic.contains("/height")) {
                handleHeightMessage(payload);
            } else if (topic.contains("/violation")) {
                handleViolationMessage(payload);
            } else if (topic.contains("/heartbeat")) {
                handleHeartbeatMessage(payload);
            }
            
        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }
    
    private void handleHeightMessage(String payload) {
        try {
            HeightDetectionMessage message = objectMapper.readValue(payload, HeightDetectionMessage.class);
            
            detectorService.updateHeartbeat(message.getDeviceId());
            
            violationService.processHeightDetection(message);
            
        } catch (Exception e) {
            log.error("Error processing height message", e);
        }
    }
    
    private void handleViolationMessage(String payload) {
        log.info("Received violation message: {}", payload);
        handleHeightMessage(payload);
    }
    
    private void handleHeartbeatMessage(String payload) {
        try {
            HeightDetectionMessage message = objectMapper.readValue(payload, HeightDetectionMessage.class);
            detectorService.updateHeartbeat(message.getDeviceId());
            log.debug("Heartbeat received from: {}", message.getDeviceId());
        } catch (Exception e) {
            log.error("Error processing heartbeat message", e);
        }
    }
}