package me.BaddCamden.SessionLibrary.events;


import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    /**
     * Create an event representing the conclusion of a session.
     *
     * @param session session that ended.
     */
    public SessionEndEvent(Session session) {
        this.session = session;
    }

    /**
     * Access the session associated with this end event.
     *
     * @return completed session.
     */
    public Session getSession() {
        return session;
    }

    /**
     * Required Bukkit handler list accessor.
     *
     * @return handler registry for this event.
     */
    public HandlerList getHandlers() { return handlers; }

    /**
     * Static accessor for Bukkit event registration.
     *
     * @return handler registry for this event type.
     */
    public static HandlerList getHandlerList() { return handlers; }
}
