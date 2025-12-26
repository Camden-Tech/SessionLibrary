package me.BaddCamden.SessionLibrary.events;


import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionEndSequenceEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    /**
     * Create an event indicating the grace period has completed.
     *
     * @param session session finishing its end sequence.
     */
    public SessionEndSequenceEndEvent(Session session) {
        this.session = session;
    }

    /**
     * Access the session that has fully ended.
     *
     * @return session completing shutdown.
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

