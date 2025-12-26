package me.BaddCamden.SessionLibrary.hooks;


import me.BaddCamden.SessionLibrary.Session;

@FunctionalInterface
public interface SessionEndHook {

    /**
     * Invoked when the session begins its end sequence, allowing custom shutdown logic.
     *
     * @param session The Session that is ending.
     * @return true to signal that the hook completed all required shutdown behavior and the
     *         default countdown should be skipped; false to allow additional hooks or default logic.
     */
    boolean handleEndSequence(Session session);
}
