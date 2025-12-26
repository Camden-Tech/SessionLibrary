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

    /**
     * Determine whether status messages should be broadcast to all players.
     *
     * @return true when broadcast is enabled.
     */
    public boolean shouldBroadcastStatus() {
        return broadcastStatus;
    }

    /**
     * Retrieve the configured timezone identifier used for date calculations.
     *
     * @return timezone ID string.
     */
    public String getTimezoneId() {
        return timezoneId;
    }

    /**
     * Get the default day offset applied to the displayed date.
     *
     * @return offset in days.
     */
    public int getDefaultDayOffset() {
        return defaultDayOffset;
    }

    /**
     * Update the timezone identifier and defer persistence until {@link #save()}.
     *
     * @param timezoneId new timezone ID.
     */
    public void setTimezoneId(String timezoneId) {
        this.timezoneId = timezoneId;
    }

    /**
     * Enable or disable broadcasting of status messages.
     *
     * @param broadcastStatus desired broadcast flag.
     */
    public void setBroadcastStatus(boolean broadcastStatus) {
        this.broadcastStatus = broadcastStatus;
    }

    /**
     * Configure the default day offset used when reporting the date.
     *
     * @param defaultDayOffset offset in days.
     */
    public void setDefaultDayOffset(int defaultDayOffset) {
        this.defaultDayOffset = defaultDayOffset;
    }

    /**
     * Validate a timezone string and return a safe identifier.
     *
     * @param configuredZone raw value from configuration.
     * @return sanitized timezone ID.
     */
    private String parseTimezone(String configuredZone) {
        try {
            return java.time.ZoneId.of(configuredZone).getId();
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid timezone in config.yml: \"" + configuredZone + "\". Defaulting to UTC.");
            return "UTC";
        }
    }
}
