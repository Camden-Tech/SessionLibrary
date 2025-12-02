package me.BaddCamden.SessionLibrary;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.BaddCamden.SessionLibrary.commands.SessionCommand;
import me.BaddCamden.SessionLibrary.events.SessionAutostartEvent;

public class SessionManager extends JavaPlugin {

    // Singleton instance for easy access from other classes
    private static SessionManager instance;

    // Static configuration/data (loaded once, reused)
    public static FileConfiguration config;
    public static FileConfiguration data;
    public static File dataFile;

    // Session variables
    public static Session currentSession;

    // Session data in memory
    public static int sessionCount;
    public static boolean autostart;
    public static int autostartBuffer;
    public static int defaultDuration;

    @Override
    public void onEnable() {
        instance = this;

        // Load default config (copies from jar if not present)
        saveDefaultConfig();
        config = getConfig();

        // Load data file (sessiondata.yml) once
        dataFile = new File(getDataFolder(), "sessiondata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create sessiondata.yml");
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load static values (in-memory only)
        sessionCount = data.getInt("session-count", 0);
        defaultDuration = config.getInt("session-duration", 3600);
        autostart = config.getBoolean("autostart", false);
        autostartBuffer = config.getInt("autostart-buffer", 60);

        // Register command
        if (getCommand("session") != null) {
            getCommand("session").setExecutor(new SessionCommand(this));
        } else {
            getLogger().severe("Command 'session' not found in plugin.yml!");
        }

        getLogger().info("SessionManager enabled. Current session count: " + sessionCount);

        // Auto-start session if enabled (with buffer)
        if (autostart) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                currentSession = new Session(this, defaultDuration, true);
                // Fire autostart event so other plugins can react
                Bukkit.getPluginManager().callEvent(new SessionAutostartEvent(currentSession));
                currentSession.start();
            }, autostartBuffer * 20L); // seconds -> ticks
        }
    }

    @Override
    public void onDisable() {
        // Save in-memory session count to data file
        data.set("session-count", sessionCount);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save config values back to config.yml
        config.set("session-duration", defaultDuration);
        config.set("autostart", autostart);
        config.set("autostart-buffer", autostartBuffer);
        saveConfig();

        // Stop session cleanly if running
        if (currentSession != null && currentSession.isRunning()) {
            currentSession.stopSession();
        }

        getLogger().info("SessionManager disabled.");
        instance = null;
    }

    // ------------------------------------------------------------------------
    // API / control methods for other plugins
    // ------------------------------------------------------------------------

    public static SessionManager getInstance() {
        return instance;
    }

    public static Session getCurrentSession() {
        return currentSession;
    }

    public static boolean hasActiveSession() {
        return currentSession != null && currentSession.isRunning();
    }

    public static int getSessionCount() {
        return sessionCount;
    }

    public static void setSessionCount(int count) {
        sessionCount = count;
    }

    public static int getDefaultDuration() {
        return defaultDuration;
    }

    public static void setDefaultDuration(int seconds) {
        if (seconds > 0) {
            defaultDuration = seconds;
        }
    }

    public static boolean isAutostartEnabled() {
        return autostart;
    }

    public static void setAutostartEnabled(boolean enabled) {
        autostart = enabled;
    }

    public static int getAutostartBuffer() {
        return autostartBuffer;
    }

    public static void setAutostartBuffer(int bufferSeconds) {
        if (bufferSeconds >= 0) {
            autostartBuffer = bufferSeconds;
        }
    }

    /**
     * Start a new session from another plugin.
     *
     * @param durationSeconds duration in seconds, or <= 0 to use defaultDuration.
     * @param autoStartFlag whether this was auto-started (for your own semantics).
     * @return the started Session instance.
     */
    public static Session startNewSession(int durationSeconds, boolean autoStartFlag) {
        if (instance == null) {
            throw new IllegalStateException("SessionManager not loaded yet");
        }

        if (currentSession == null || !currentSession.isRunning()) {
            int dur = (durationSeconds > 0) ? durationSeconds : defaultDuration;
            currentSession = new Session(instance, dur, autoStartFlag);
            currentSession.start();
        }
        return currentSession;
    }

    public static void endSession() {
        if (currentSession != null) {
            currentSession.beginEndSequence();
        }
    }

    public static void stopSession() {
        if (currentSession != null) {
            currentSession.stopSession();
        }
    }

    public static void resetSessionTimer() {
        if (currentSession != null) {
            currentSession.reset();
        }
    }
}