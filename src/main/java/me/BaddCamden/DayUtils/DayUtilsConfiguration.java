package me.BaddCamden.DayUtils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simple configuration holder for DayUtils.
 * Reads once during enable (or reload) and writes back on disable.
 */
public class DayUtilsConfiguration {
    private final JavaPlugin plugin;

    private boolean broadcastStatus;
    private String timezoneId;
    private int defaultDayOffset;

    public DayUtilsConfiguration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load values from the plugin configuration into memory.
     */
    public void load() {
        FileConfiguration config = plugin.getConfig();
        broadcastStatus = config.getBoolean("broadcast", true);
        timezoneId = parseTimezone(config.getString("timezone", "UTC"));
        defaultDayOffset = config.getInt("default-day-offset", 0);
    }

    /**
     * Persist the in-memory values back to config.yml. Called during onDisable().
     */
    public void save() {
        FileConfiguration config = plugin.getConfig();
        config.set("broadcast", broadcastStatus);
        config.set("timezone", timezoneId);
        config.set("default-day-offset", defaultDayOffset);
        plugin.saveConfig();
    }

    public boolean shouldBroadcastStatus() {
        return broadcastStatus;
    }

    public String getTimezoneId() {
        return timezoneId;
    }

    public int getDefaultDayOffset() {
        return defaultDayOffset;
    }

    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    public void setBroadcastStatus(boolean broadcastStatus) {
        this.broadcastStatus = broadcastStatus;
    }

    public void setDefaultDayOffset(int defaultDayOffset) {
        this.defaultDayOffset = defaultDayOffset;
    }

    private String parseTimezone(String configuredZone) {
        try {
            return java.time.ZoneId.of(configuredZone).getId();
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid timezone in config.yml: \"" + configuredZone + "\". Defaulting to UTC.");
            return "UTC";
        }
    }
}
