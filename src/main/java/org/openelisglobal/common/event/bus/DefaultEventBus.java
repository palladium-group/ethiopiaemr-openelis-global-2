package org.openelisglobal.common.event.bus;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the event bus. This implementation uses an
 * in-memory list of subscribers and synchronously notifies them when events are
 * published.
 * 
 * @param <T> The type of event this bus handles
 */
@Component
public class DefaultEventBus<T> implements EventBus<T> {

    private final List<EventSubscriber<T>> subscribers = new ArrayList<>();

    @Override
    public void publish(T event) {
        for (EventSubscriber<T> subscriber : subscribers) {
            try {
                subscriber.onEvent(event);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "publish",
                        "Error notifying subscriber: " + e.getMessage());
            }
        }
    }

    @Override
    public void subscribe(EventSubscriber<T> subscriber) {
        if (subscriber != null && !subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }
}
