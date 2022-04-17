package dev.truewinter.exportschem;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class ExportSchem extends JavaPlugin {
    private WebServer webServer;
    private HashMap<String, Long> schematicTimes = new HashMap<>();
    private HashMap<String, ImportKey> importKeys = new HashMap<>();
    private Timer deleteTimer;
    private static ExportSchem instance;

    public static ExportSchem getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Setting this in simplelogger.properties didn't work
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        instance = this;
        this.saveDefaultConfig();

        File schemDir = new File(getDataFolder() + File.separator + "schematics");
        if (!schemDir.exists()) {
            boolean success = schemDir.mkdir();

            if (!success) {
                getLogger().severe("Failed to create schematics folder, disabling plugin");
                setEnabled(false);
                return;
            }
        }

        if (getConfig().getBoolean("clear-on-startup")) {
            File[] files = schemDir.listFiles();

            for (int i = 0; i < files.length; i++) {
                boolean deleted = files[i].delete();

                if (!deleted) {
                    getLogger().warning("Failed to delete schematic " + files[i].getName());
                }
            }

            getLogger().info("Deleted all schematics");
        }

        loadWebServer();

        getCommand("exportschem").setExecutor(new CommandHandler());

        deleteTimer = new Timer();
        TimerTask deleteTask = new TimerTask() {
            @Override
            public void run() {
                int deleteAfterMinutes = getConfig().getInt("delete-after-minutes");
                int importExpiryMinutes = getConfig().getInt("import-expiry-minutes");

                if (deleteAfterMinutes > 0) {
                    for (String schemId : schematicTimes.keySet()) {
                        long currentTime = System.currentTimeMillis();
                        long savedTime = schematicTimes.get(schemId);

                        if ((currentTime - savedTime) > (deleteAfterMinutes * 60 * 1000L)) {
                            File file = new File(getDataFolder() + File.separator + "schematics" + File.separator + schemId);
                            boolean deleted = file.delete();
                            schematicTimes.remove(schemId);

                            if (!deleted) {
                                getLogger().warning("Failed to delete " + schemId + " after " + deleteAfterMinutes + " minutes");
                            }
                        }
                    }
                }

                for (String importId : importKeys.keySet()) {
                    long currentTime = System.currentTimeMillis();
                    long savedTime = importKeys.get(importId).getTimestamp();

                    if ((currentTime - savedTime) > (importExpiryMinutes * 60 * 1000L)) {
                        importKeys.remove(importId);
                    }
                }
            }
        };
        deleteTimer.scheduleAtFixedRate(deleteTask, 0, 60 * 1000);

        getLogger().info("ExportSchem v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        webServer.stopServer();
        if (deleteTimer != null) {
            deleteTimer.cancel();
        }
        getLogger().info("ExportSchem v" + getDescription().getVersion() + " disabled.");
    }

    // Because Bukkit...
    // https://javalin.io/tutorials/javalin-and-minecraft-servers
    private void loadWebServer() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClassLoader());

        webServer = new WebServer(
                getConfig().getInt("port"),
                getDataFolder() + File.separator + "schematics",
                getLogger()
        );

        webServer.run();

        Thread.currentThread().setContextClassLoader(classLoader);
    }

    protected void addFile(String file) {
        schematicTimes.put(file, System.currentTimeMillis());
    }

    protected void addImportKey(String key, Player player) {
        importKeys.put(key, new ImportKey(key, player));
    }

    protected boolean hasImportKey(String key) {
        return importKeys.containsKey(key);
    }

    protected ImportKey getImportKey(String key) {
        return importKeys.get(key);
    }

    protected void importSchem(String fileName, InputStream input, ImportKey importKey) throws Exception {
        if (!importKey.getPlayer().isOnline()) {
            throw new Exception("Player is offline");
        }

        if (!importKey.getPlayer().hasPermission("exportschem.command.import")) {
            throw new Exception("Player does not have permission to import schematics");
        }

        String fileExt = fileName.split(Pattern.quote("."))[fileName.split(Pattern.quote(".")).length - 1].replace(".", "");
        ClipboardFormat format = ClipboardFormats.findByAlias(fileExt);

        if (format == null) {
            throw new Exception("Unsupported schematic format");
        }

        try (ClipboardReader reader = format.getReader(input)) {
            com.sk89q.worldedit.entity.Player actor = BukkitAdapter.adapt(importKey.getPlayer());
            SessionManager manager = WorldEdit.getInstance().getSessionManager();
            LocalSession localSession = manager.get(actor);
            Clipboard clipboard = reader.read();
            ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
            localSession.setClipboard(clipboardHolder);

            importKey.getPlayer().sendMessage(ChatColor.GREEN + "Schematic imported");
            importKeys.remove(importKey.getKey());
        }
    }
}
