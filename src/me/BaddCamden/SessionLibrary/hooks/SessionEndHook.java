package me.BaddCamden.SessionLibrary.hooks;


import me.BaddCamden.SessionLibrary.Session;

@FunctionalInterface
public interface SessionEndHook {

    /**
     * Called when a session's end sequence starts.
     *
     * @param session The Session that is ending.
     * @return true if this hook fully handled the end sequence and
     *         the default 60-second countdown + shutdown should be skipped.
     *         false to let other hooks / default logic continue.
     */
    boolean handleEndSequence(Session session);
}
