package com.example.smp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandSyncListener implements Listener {

    private final SMPPlugin plugin;

    public CommandSyncListener(SMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String commandLine = event.getMessage().substring(1); // "/" を除去
        logAndSyncCommand(player.getName(), commandLine);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        String commandLine = event.getCommand();
        logAndSyncCommand("CONSOLE", commandLine);
    }

    private void logAndSyncCommand(String senderName, String commandLine) {
        if (commandLine.trim().isEmpty()) return;

        String[] parts = commandLine.split(" ");
        String cmd = parts[0].toLowerCase();
        
        // ログに残すかどうかのフィルター（必要に応じて除外コマンドを追加）
        if (cmd.equals("login") || cmd.equals("register") || cmd.equals("l") || cmd.equals("reg")) {
            return;
        }

        // ターゲットの推定（引数に対象プレイヤー名やセレクターがあるか）
        String targetPlayer = null;
        if (parts.length > 1) {
            String arg = parts[1];
            // @a, @p, @r, @e, @s などのセレクター、またはプレーンな文字列
            targetPlayer = arg; 
        }

        String serverName = plugin.isHubMode() ? "hub" : "smp";

        DatabaseManager db = plugin.getDatabaseManager();
        if (db != null && db.isEnabled()) {
            db.logCommandAsync(senderName, commandLine, serverName, targetPlayer);
        }
    }
}
