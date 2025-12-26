package me.BaddCamden.SessionLibrary.commands;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import me.BaddCamden.SessionLibrary.Session;
import me.BaddCamden.SessionLibrary.SessionManager;

public class SessionCommand implements CommandExecutor, TabCompleter {

    private final SessionManager plugin;

    /**
     * Construct the command handler with access to the plugin context.
     *
     * @param plugin owning plugin used for session control.
     */
    public SessionCommand(SessionManager plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle execution of the /session command, allowing administrators to manage sessions.
     *
     * @param sender originator of the command.
     * @param cmd    command instance.
     * @param label  label used.
     * @param args   arguments supplied by the sender.
     * @return true once processing is complete.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("sessionlibrary.admin")) {
            sender.sendMessage(SessionManager.config.getString(
                    "messages.no-permission",
                    "You do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(SessionManager.config.getString(
                    "messages.usage",
                    "Usage: /session <start|end|reset|stop|duration|autostart|autostartbuffer>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start":
                int duration = SessionManager.defaultDuration;
                if (SessionManager.currentSession == null) {
                    SessionManager.currentSession = new Session(plugin, duration, false);
                }
                SessionManager.currentSession.start();
                sender.sendMessage(SessionManager.config.getString(
                        "messages.session-start-admin",
                        "Session started."));
                break;

            case "reset":
                if (SessionManager.currentSession != null) {
                    SessionManager.currentSession.reset();
                    sender.sendMessage(SessionManager.config.getString(
                            "messages.session-reset-admin",
                            "Session timer reset."));
                }
                break;

            case "end":
                if (SessionManager.currentSession != null) {
                    // Start end sequence; default or hooks handle shutdown
                    SessionManager.currentSession.beginEndSequence();
                    sender.sendMessage(SessionManager.config.getString(
                            "messages.session-end-admin",
                            "Session ended. Server shutting down..."));
                }
                break;

            case "stop":
                if (SessionManager.currentSession != null) {
                    SessionManager.currentSession.stopSession();
                }
                SessionManager.sessionCount = 0;
                sender.sendMessage(SessionManager.config.getString(
                        "messages.session-stop-admin",
                        "Session stopped and count reset."));
                break;

            case "duration":
                if (args.length > 1) {
                    try {
                        int d = Integer.parseInt(args[1]);
                        SessionManager.defaultDuration = d;
                        sender.sendMessage(
                                SessionManager.config
                                        .getString("messages.session-duration-set",
                                                "Session duration set to %seconds% seconds.")
                                        .replace("%seconds%", String.valueOf(d)));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(SessionManager.config.getString(
                                "messages.session-duration-invalid",
                                "Invalid number."));
                    }
                } else {
                    sender.sendMessage(SessionManager.config.getString(
                            "messages.usage",
                            "Usage: /session duration <seconds>"));
                }
                break;

            case "autostart":
                SessionManager.autostart = !SessionManager.autostart;
                sender.sendMessage(
                        SessionManager.config
                                .getString("messages.autostart-toggled",
                                        "Autostart set to %value%.")
                                .replace("%value%", String.valueOf(SessionManager.autostart)));
                break;

            case "autostartbuffer":
                if (args.length > 1) {
                    try {
                        int buffer = Integer.parseInt(args[1]);
                        SessionManager.autostartBuffer = buffer;
                        sender.sendMessage(
                                SessionManager.config
                                        .getString("messages.autostartbuffer-set",
                                                "Autostart buffer set to %seconds% seconds.")
                                        .replace("%seconds%", String.valueOf(buffer)));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(SessionManager.config.getString(
                                "messages.autostartbuffer-invalid",
                                "Invalid number."));
                    }
                } else {
                    sender.sendMessage(SessionManager.config.getString(
                            "messages.usage",
                            "Usage: /session autostartbuffer <seconds>"));
                }
                break;

            default:
                sender.sendMessage(SessionManager.config.getString(
                        "messages.usage",
                        "Usage: /session <start|end|reset|stop|duration|autostart|autostartbuffer>"));
                break;
        }

        return true;
    }

    /**
     * Provide tab completion hints for the /session command.
     *
     * @param sender command source requesting completions.
     * @param command command being completed.
     * @param alias alias used for the command.
     * @param args arguments already supplied.
     * @return list of suggested completions.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("sessionlibrary.admin")) {
            return Collections.emptyList();
        }

        List<String> subcommands = Arrays.asList(
                "start", "end", "reset", "stop", "duration", "autostart", "autostartbuffer");

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String option : subcommands) {
                if (option.startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "duration":
                    return Collections.singletonList(String.valueOf(SessionManager.defaultDuration));
                case "autostartbuffer":
                    return Collections.singletonList(String.valueOf(SessionManager.autostartBuffer));
                default:
                    break;
            }
        }

        return Collections.emptyList();
    }
}
