package dev.truewinter.exportschem;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private ExportSchem exportSchem;
    private HashMap<String, ESCommand> subCommands = new HashMap<>();

    public CommandHandler() {
        this.exportSchem = ExportSchem.getInstance();

        registerSubCommand("about", new AboutCommand());
        registerSubCommand("reload", new ReloadCommand());
        registerSubCommand("import", new ImportCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ExportSchemCommand exportSchemCommand = new ExportSchemCommand();
            handleCommand(exportSchemCommand, sender, command, label, args);
        } else {
            if (args.length > 1) {
                sendUsageError(sender);
                // No need to have the server send an incorrect usage message, the plugin does it
                return true;
            }

            if (subCommands.containsKey(args[0])) {
                handleCommand(
                        subCommands.get(args[0]),
                        sender,
                        command,
                        label,
                        args
                );
            } else {
                sendUsageError(sender);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> tabComplete = new ArrayList<>();

            for (String subCommand : subCommands.keySet()) {
                if (!args[0].isBlank() && !subCommand.startsWith(args[0])) {
                    continue;
                }

                if (subCommands.get(subCommand).hasPermission(sender)) {
                    tabComplete.add(subCommand);
                }
            }

            return tabComplete;
        }

        if (args.length > 1 && subCommands.containsKey(args[0])) {
            return subCommands.get(args[0]).tabComplete(sender, command, label, args);
        }

        return new ArrayList<>();
    }

    private void handleCommand(ESCommand command, CommandSender sender, Command bukkitCommand, String label, String[] args) {
        if (!(sender instanceof Player) && command.isPlayerOnly()) {
            sender.sendMessage(ChatColor.RED + "That command can only be used by players");
            return;
        }

        if (!command.hasPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run that command");
            return;
        }

        command.runCommand(sender, bukkitCommand, label, args);
    }

    private void sendUsageError(CommandSender sender) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ChatColor.RED + "Incorrect command usage.");

        PluginCommand command = exportSchem.getCommand("exportschem");

        if (command != null) {
            stringBuilder.append(ChatColor.YELLOW + " Correct usage: " + command.getUsage());
        }

        sender.sendMessage(stringBuilder.toString());
    }

    private void registerSubCommand(String subCommand, ESCommand command) {
        subCommands.put(subCommand, command);
    }
}
