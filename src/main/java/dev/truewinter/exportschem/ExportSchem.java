package dev.truewinter.exportschem;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ExportSchem extends JavaPlugin {
    private WebServer webServer;
    private HashMap<String, Long> schematicTimes = new HashMap<>();
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
                for (String id : schematicTimes.keySet()) {
                    long currentTime = System.currentTimeMillis();
                    long savedTime = schematicTimes.get(id);

                    if ((currentTime - savedTime) > (deleteAfterMinutes * 60 * 1000L)) {
                        File file = new File(getDataFolder() + File.separator + "schematics" + File.separator + id);
                        boolean deleted = file.delete();
                        schematicTimes.remove(id);

                        if (!deleted) {
                            getLogger().warning("Failed to delete " + id + " after " + deleteAfterMinutes + " minutes");
                        }
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
        deleteTimer.cancel();
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
}
