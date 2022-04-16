package dev.truewinter.exportschem;

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExportSchemCommand extends ESCommand {
    private ExportSchem exportSchem;

    public ExportSchemCommand() {
        this.exportSchem = ExportSchem.getInstance();
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(player);
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession localSession = manager.get(actor);
        ClipboardHolder clipboard;

        try {
            clipboard = localSession.getClipboard();
        } catch (EmptyClipboardException e) {
            sender.sendMessage(ChatColor.RED + "Please copy an area first");
            return;
        }

        File file = getNewFile();

        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
            writer.write(clipboard.getClipboard());
        } catch(IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to save clipboard to file. Please contact an admin.");
            e.printStackTrace();
        }

        int deleteAfterMinutes = exportSchem.getConfig().getInt("delete-after-minutes");
        TextComponent component = new TextComponent(ChatColor.GREEN + "Schematic saved, ");
        TextComponent clickComponent = new TextComponent(ChatColor.BOLD + "click here to download" + ChatColor.RESET + ". ");
        clickComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.OPEN_URL,
                exportSchem.getConfig().getString("url-prefix") + "download/" + file.getName()
        ));
        TextComponent expiryComponent = new TextComponent(ChatColor.YELLOW + "This link will expire in "
                + ChatColor.BOLD + deleteAfterMinutes + ChatColor.YELLOW + " minute"
        );
        if (deleteAfterMinutes > 1) {
            expiryComponent.addExtra(ChatColor.YELLOW + "s");
        }

        component.addExtra(clickComponent);
        component.addExtra(expiryComponent);

        player.spigot().sendMessage(component);
        exportSchem.getLogger().info(player.getName() + " saved a schematic (" + file.getName() + ")");
        exportSchem.addFile(file.getName());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args) {
            return new ArrayList<>();
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("exportschem.command.exportschem");
    }

    private File getNewFile() {
        File file = new File(exportSchem.getDataFolder() + File.separator + "schematics" + File.separator + Util.generateUUID() + ".schem");

        if (file.exists()) {
            return getNewFile();
        }

        return file;
    }
}
