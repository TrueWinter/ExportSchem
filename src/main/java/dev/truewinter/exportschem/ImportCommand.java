package dev.truewinter.exportschem;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ImportCommand extends ESCommand {
    private ExportSchem exportSchem;

    public ImportCommand() {
        this.exportSchem = ExportSchem.getInstance();
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args) {
        String key = generateKey();
        exportSchem.addImportKey(key, (Player) sender);

        int importExpiryMinutes = exportSchem.getConfig().getInt("import-expiry-minutes");
        TextComponent component = new TextComponent(ChatColor.GREEN + "Import session started, ");
        TextComponent clickComponent = new TextComponent(ChatColor.BOLD + "click here to upload schematic" + ChatColor.RESET + ". ");
        clickComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.OPEN_URL,
                exportSchem.getConfig().getString("url-prefix") + "upload/" + key
        ));
        TextComponent expiryComponent = new TextComponent(ChatColor.YELLOW + "This link will expire in "
                + ChatColor.BOLD + importExpiryMinutes + ChatColor.RESET + ChatColor.YELLOW + " minute"
        );
        if (importExpiryMinutes > 1) {
            expiryComponent.addExtra(ChatColor.YELLOW + "s");
        }
        TextComponent expiryComponent2 = new TextComponent(ChatColor.YELLOW + ", or after uploading a schematic");

        component.addExtra(clickComponent);
        component.addExtra(expiryComponent);
        component.addExtra(expiryComponent2);

        sender.spigot().sendMessage(component);
        exportSchem.getLogger().info(sender.getName() + " generated new import key (" + key + ")");
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("exportschem.command.import");
    }

    private String generateKey() {
        String uuid = Util.generateUUID();

        if (exportSchem.hasImportKey(uuid)) {
            return generateKey();
        }

        return uuid;
    }
}
