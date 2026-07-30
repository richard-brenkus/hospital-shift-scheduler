package com.richardbrenkus.hospitalshiftscheduler.activity.kafka;

import com.richardbrenkus.hospitalshiftscheduler.activity.ActivityEvent;

/**
 * Outbound adapter boundary for publishing activity events.
 * Application components depend on this interface rather than directly
 * depending on KafkaTemplate.
 */
public interface ActivityKafkaProducer {

    void publish(ActivityEvent event);
}