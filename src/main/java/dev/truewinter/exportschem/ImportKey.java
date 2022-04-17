package dev.truewinter.exportschem;

import org.bukkit.entity.Player;

public class ImportKey {
    private String key;
    private Player player;
    private long timestamp;

    public ImportKey(String key, Player player) {
        this.key = key;
        this.player = player;
        this.timestamp = System.currentTimeMillis();
    }

    public String getKey() {
        return key;
    }

    public Player getPlayer() {
        return player;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
