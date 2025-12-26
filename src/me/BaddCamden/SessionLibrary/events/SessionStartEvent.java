package me.BaddCamden.SessionLibrary.events;


import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    /**
     * Create an event indicating that a session has begun.
     *
     * @param session session that just started.
     */
    public SessionStartEvent(Session session) {
        super(false); // not async
        this.session = session;
    }

    /**
     * Access the session associated with this event.
     *
     * @return running session.
     */
    public Session getSession() { return session; }

    @Override
    /**
     * Required Bukkit handler list accessor.
     *
     * @return handler registry for this event.
     */
    public HandlerList getHandlers() { return handlers; }

    /**
     * Static accessor for Bukkit's event registration.
     *
     * @return handler registry for this event type.
     */
    public static HandlerList getHandlerList() { return handlers; }
}