package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;

/**
 * Outbound adapter boundary for publishing activity events.
 * Application components depend on this interface rather than directly
 * depending on KafkaTemplate.
 */
public interface ActivityKafkaProducer {

    void publish(ActivityEvent event);
}