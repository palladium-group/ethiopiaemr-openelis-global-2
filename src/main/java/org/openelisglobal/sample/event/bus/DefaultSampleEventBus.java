package org.openelisglobal.sample.event.bus;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the sample event bus.
 * This implementation uses an in-memory list of subscribers and synchronously
 * notifies them when events are published.
 */
@Component
public class DefaultSampleEventBus implements SampleEventBus {
    
    private final List<SampleEventSubscriber> subscribers = new ArrayList<>();
    
    @Override
    public void publishSampleCreated(SamplePatientUpdateDataCreatedEvent event) {
        for (SampleEventSubscriber subscriber : subscribers) {
            try {
                subscriber.onSampleCreated(event);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "publishSampleCreated", 
                    "Error notifying subscriber: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void subscribeToSampleCreated(SampleEventSubscriber subscriber) {
        if (subscriber != null && !subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }
} 