package me.BaddCamden.DayUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import me.BaddCamden.SessionLibrary.SessionManager;

public class DayUtilsPlugin extends JavaPlugin {

    private DayUtilsConfiguration configuration;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuration = new DayUtilsConfiguration(this);
        configuration.load();
    }

    @Override
    public void onDisable() {
        if (configuration != null) {
            configuration.save();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dayutilsreload")) {
            return handleReload(sender);
        }

        if (command.getName().equalsIgnoreCase("dayutils")) {
            return handleStatus(sender);
        }

        return false;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("dayutils.reload")) {
            sender.sendMessage("§cYou do not have permission to reload DayUtils.");
            return true;
        }

        reloadConfig();
        configuration.load();
        sender.sendMessage("§aDayUtils configuration reloaded.");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("dayutils.status")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        ZoneId zone = ZoneId.of(configuration.getTimezoneId());
        LocalDate today = LocalDate.now(zone).plusDays(configuration.getDefaultDayOffset());
        String message = "§eToday is " + today.format(DateTimeFormatter.ISO_LOCAL_DATE) + " (" + zone + ")";

        if (configuration.shouldBroadcastStatus()) {
            Bukkit.broadcastMessage(message);
        } else {
            sender.sendMessage(message);
        }

        int dayCounter = fetchDayCounter();
        if (dayCounter >= 0) {
            String counterMessage = "§7SessionLibrary day counter: " + dayCounter;
            if (configuration.shouldBroadcastStatus()) {
                Bukkit.broadcastMessage(counterMessage);
            } else {
                sender.sendMessage(counterMessage);
            }
        }

        return true;
    }

    private int fetchDayCounter() {
        Plugin dependency = Bukkit.getPluginManager().getPlugin("SessionLibrary");
        if (dependency == null) {
            return -1;
        }
        try {
            return SessionManager.getDayCounterValue();
        } catch (Exception ex) {
            getLogger().warning("Unable to read SessionLibrary day counter: " + ex.getMessage());
            return -1;
        }
    }
}
