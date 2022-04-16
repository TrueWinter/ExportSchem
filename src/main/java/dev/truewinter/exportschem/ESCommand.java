package dev.truewinter.exportschem;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public abstract class ESCommand {
    public abstract void runCommand(CommandSender sender, Command command, String label, String[] args);
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<>();
    }
    public boolean isPlayerOnly() {
        return false;
    }
    public boolean hasPermission(CommandSender sender) {
        return false;
    }
}
