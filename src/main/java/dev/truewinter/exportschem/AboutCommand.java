package dev.truewinter.exportschem;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AboutCommand extends ESCommand {
    private ExportSchem exportSchem;

    public AboutCommand() {
        this.exportSchem = ExportSchem.getInstance();
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GREEN + "ExportSchem" + ChatColor.RESET
                + " v" + exportSchem.getDescription().getVersion() + " by "
                + ChatColor.LIGHT_PURPLE + "TrueWinter" + ChatColor.RESET
        );
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return true;
    }
}
