package me.BaddCamden.SessionLibrary.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionEndSequenceStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    /**
     * Create an event marking the start of a session's shutdown grace period.
     *
     * @param session session entering its end sequence.
     */
    public SessionEndSequenceStartEvent(Session session) {
        this.session = session;
    }

    /**
     * Access the session that is ending.
     *
     * @return session in the end sequence.
     */
    public Session getSession() { return session; }

    /**
     * Required Bukkit handler list accessor.
     *
     * @return handler registry for this event.
     */
    public HandlerList getHandlers() { return handlers; }

    /**
     * Static accessor used by Bukkit registration.
     *
     * @return handler registry for this event type.
     */
    public static HandlerList getHandlerList() { return handlers; }
}

