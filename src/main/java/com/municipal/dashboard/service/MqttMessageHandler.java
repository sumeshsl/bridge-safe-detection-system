package com.municipal.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.municipal.dashboard.dto.HeightDetectionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final ViolationService violationService;
    private final DetectorService detectorService;
    private final ObjectMapper objectMapper;

    public void handleMessage(Message<?> message) {
        try {
            String payload = message.getPayload().toString();
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            
            log.info("=== MQTT MESSAGE RECEIVED ===");
            log.info("Topic: {}", topic);
            log.info("Payload: {}", payload);
            
            if (topic == null) {
                log.warn("Received message without topic");
                return;
            }
            
            // Parse topic: detector/{deviceId}/{messageType}
            String[] topicParts = topic.split("/");
            if (topicParts.length < 3) {
                log.warn("Invalid topic format: {}", topic);
                return;
            }
            
            String deviceId = topicParts[1];
            String messageType = topicParts[2];
            
            log.info("Device ID: {}, Message Type: {}", deviceId, messageType);
            
            switch (messageType) {
                case "height":
                    handleHeightDetection(deviceId, payload);
                    break;
                    
                case "violation":
                    handleViolationDetection(deviceId, payload);
                    break;
                    
                case "heartbeat":
                    handleHeartbeat(deviceId, payload);
                    break;
                    
                default:
                    log.warn("Unknown message type: {} on topic: {}", messageType, topic);
            }
            
        } catch (Exception e) {
            log.error("Error handling MQTT message: {}", e.getMessage(), e);
        }
    }
    
    private void handleHeightDetection(String deviceId, String payload) {
        try {
            log.info("Processing height detection for device: {}", deviceId);
            HeightDetectionMessage detection = objectMapper.readValue(payload, HeightDetectionMessage.class);
            log.info("Height detection from {}: {} ft", deviceId, detection.getHeight());
            
            violationService.processHeightDetection(detection);
            detectorService.updateHeartbeat(deviceId);
            
            log.info("Height detection processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing height detection for device {}: {}", deviceId, e.getMessage(), e);
        }
    }
    
    private void handleViolationDetection(String deviceId, String payload) {
        try {
            log.info("Processing violation for device: {}", deviceId);
            HeightDetectionMessage detection = objectMapper.readValue(payload, HeightDetectionMessage.class);
            log.info("Violation detection from {}: {} ft", deviceId, detection.getHeight());
            
            violationService.processHeightDetection(detection);
            detectorService.updateHeartbeat(deviceId);
            
            log.info("Violation processed successfully");
            
        } catch (Exception e) {
            log.error("Error processing violation for device {}: {}", deviceId, e.getMessage(), e);
        }
    }
    
    private void handleHeartbeat(String deviceId, String payload) {
        try {
            log.debug("Processing heartbeat from device: {}", deviceId);
            detectorService.updateHeartbeat(deviceId);
        } catch (Exception e) {
            log.error("Error processing heartbeat for device {}: {}", deviceId, e.getMessage(), e);
        }
    }
}