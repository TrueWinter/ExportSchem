package dev.truewinter.exportschem;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends ESCommand {
    private ExportSchem exportSchem;

    public ReloadCommand() {
        this.exportSchem = ExportSchem.getInstance();
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args) {
        exportSchem.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Config reloaded. If you changed the server port, you will need to restart the server.");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("exportschem.command.reload");
    }
}
