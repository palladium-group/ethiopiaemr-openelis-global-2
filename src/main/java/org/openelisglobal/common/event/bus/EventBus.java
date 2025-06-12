package org.openelisglobal.common.event.bus;

/**
 * Generic event bus interface for handling any type of event. This provides a
 * centralized way to publish and subscribe to events.
 * 
 * @param <T> The type of event this bus handles
 */
public interface EventBus<T> {

    /**
     * Publish an event
     * 
     * @param event The event to publish
     */
    void publish(T event);

    /**
     * Subscribe to events
     * 
     * @param subscriber The subscriber that will handle the events
     */
    void subscribe(EventSubscriber<T> subscriber);
}
