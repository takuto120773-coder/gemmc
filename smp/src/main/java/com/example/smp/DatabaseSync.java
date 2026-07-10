package com.example.smp;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * オークション/オーダー一覧のサーバー間共有。
 *
 * 書き込み: smp側が出品/購入/取消のたびに個別に書き込む (DatabaseManagerのadd/remove)。
 * 読み込み: hub側だけが5秒ごとに一覧をpullして表示用に反映する。
 * プレイヤーデータ(所持金・インベントリ等)はFLUSHハンドシェイク+参加時読込で
 * 同期されるため、ここでは扱わない。
 */
public class DatabaseSync {
    private final SMPPlugin plugin;
    private final SMPManager manager;

    public DatabaseSync(SMPPlugin plugin, SMPManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void startSyncTask() {
        DatabaseManager db = plugin.getDatabaseManager();
        if (db == null || !db.isEnabled()) return;

        // クロスサーバーコマンドのポーリング (1秒ごと)
        manager.runGlobalTaskTimer(() -> db.getPendingCommandsAsync(this::processCrossServerCommands), 20L, 20L);

        // 古いコマンドログの削除 (1時間ごと)
        manager.runGlobalTaskTimer(() -> db.deleteOldCommandsAsync(3600000L), 1200L, 72000L);

        if (plugin.isHubMode()) {
            // hub: 5秒ごとに一覧をpull (表示用。合計数十行のSELECTのみで軽量)
            manager.runGlobalTaskTimer(() -> db.runAsync(this::pullListings), 100L, 100L);
        } else {
            // smp: 書き込み側なので定期pullはしない!
            // (clear&replaceのpullは、出品直後のDB書き込み完了前に走ると出品が消える。
            //  "smpでAH/orderが見えない"バグの原因だった)
            // 起動時に1回だけ「DB→メモリのマージ(無い物だけ追加)」→「メモリ→DBへ全プッシュ」を行う。
            manager.runGlobalTaskLater(() -> db.runAsync(() -> {
                mergeListingsIntoMemoryOnce();
                manager.runGlobalTask(() -> {
                    for (SMPManager.AuctionItem ai : manager.auctionItems.values()) {
                        db.addAuctionItem(ai);
                    }
                    for (SMPManager.OrderItem oi : manager.orderItems.values()) {
                        db.addOrderItem(oi);
                    }
                    for (Map.Entry<org.bukkit.Material, Long> entry : manager.ITEM_PRICES.entrySet()) {
                        db.saveItemPrice(entry.getKey().name(), entry.getValue());
                    }
                    plugin.getLogger().info("[DB] 出品" + manager.auctionItems.size()
                            + "件 / オーダー" + manager.orderItems.size() + "件 / 価格" + manager.ITEM_PRICES.size() + "件 をDBへ反映しました。");
                });
            }), 100L);
        }
    }

    /** (smp起動時に1回) DBにあってメモリに無い出品/オーダーを取り込む (ymlが欠損してもDBから復元) */
    private void mergeListingsIntoMemoryOnce() {
        Map<String, SMPManager.AuctionItem> dbItems = new HashMap<>();
        Map<String, SMPManager.OrderItem> dbOrders = new HashMap<>();
        Map<String, List<ItemStack>> dbStorage = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            readListings(conn, dbItems, dbOrders, dbStorage);
        } catch (SQLException e) {
            plugin.getLogger().warning("[DB] 出品/オーダーの読込に失敗: " + e.getMessage());
            return;
        }
        manager.runGlobalTask(() -> {
            int addedAh = 0, addedOrder = 0;
            for (Map.Entry<String, SMPManager.AuctionItem> e : dbItems.entrySet()) {
                if (manager.auctionItems.putIfAbsent(e.getKey(), e.getValue()) == null) addedAh++;
            }
            for (Map.Entry<String, SMPManager.OrderItem> e : dbOrders.entrySet()) {
                if (manager.orderItems.putIfAbsent(e.getKey(), e.getValue()) == null) addedOrder++;
            }
            for (Map.Entry<String, List<ItemStack>> e : dbStorage.entrySet()) {
                manager.orderStorage.putIfAbsent(e.getKey(), e.getValue());
            }
            if (addedAh > 0 || addedOrder > 0) {
                plugin.getLogger().info("[DB] DBから出品" + addedAh + "件 / オーダー" + addedOrder + "件を復元しました。");
                manager.requestSaveData();
            }
        });
    }

    /** DBからオークション/オーダー/ストレージを読み込む (呼び出し側スレッドで実行) */
    private void readListings(Connection conn,
                              Map<String, SMPManager.AuctionItem> outItems,
                              Map<String, SMPManager.OrderItem> outOrders,
                              Map<String, List<ItemStack>> outStorage) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM auction_items");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    SMPManager.AuctionItem ai = new SMPManager.AuctionItem();
                    ai.id = rs.getString("item_key");
                    ai.sellerUUID = UUID.fromString(rs.getString("seller_uuid"));
                    ai.sellerName = rs.getString("seller_name");
                    ItemStack[] parsed = manager.itemsFromBase64(rs.getString("item_base64"));
                    if (parsed != null && parsed.length > 0) ai.item = parsed[0];
                    ai.price = rs.getLong("price");
                    ai.listedTime = rs.getLong("expire_time");
                    if (ai.item != null) outItems.put(ai.id, ai);
                } catch (Exception ignored) {}
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM order_items");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    SMPManager.OrderItem oi = new SMPManager.OrderItem();
                    oi.id = rs.getString("order_id");
                    oi.requesterUUID = UUID.fromString(rs.getString("requester_uuid"));
                    oi.requesterName = rs.getString("requester_name");
                    ItemStack[] templates = manager.itemsFromBase64(rs.getString("item_template_base64"));
                    if (templates != null && templates.length > 0) oi.itemTemplate = templates[0];
                    oi.price = rs.getLong("price");
                    oi.count = rs.getInt("count");
                    oi.collected = rs.getInt("collected");
                    oi.listedTime = rs.getLong("listed_time");
                    oi.cancelled = rs.getBoolean("cancelled");
                    if (oi.itemTemplate != null) outOrders.put(oi.id, oi);
                } catch (Exception ignored) {}
            }
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM order_storage");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    String orderId = rs.getString("order_id");
                    ItemStack[] items = manager.itemsFromBase64(rs.getString("item_base64"));
                    if (items != null) {
                        outStorage.computeIfAbsent(orderId, k -> new ArrayList<>()).addAll(Arrays.asList(items));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    /** (hub専用) DBからオークション/オーダー一覧を取得して表示用キャッシュへ反映する */
    private void pullListings() {
        Map<String, SMPManager.AuctionItem> newItems = new HashMap<>();
        Map<String, SMPManager.OrderItem> newOrders = new HashMap<>();
        Map<String, List<ItemStack>> newStorage = new HashMap<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            readListings(conn, newItems, newOrders, newStorage);

            // Shop Prices (hub表示用)
            Map<org.bukkit.Material, Long> newPrices = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM item_prices");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        org.bukkit.Material mat = org.bukkit.Material.valueOf(rs.getString("material"));
                        long price = rs.getLong("price");
                        newPrices.put(mat, price);
                    } catch (Exception ignored) {}
                }
            }
            if (!newPrices.isEmpty()) {
                manager.runGlobalTask(() -> {
                    manager.ITEM_PRICES.clear();
                    manager.ITEM_PRICES.putAll(newPrices);
                });
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Database sync failed: " + e.getMessage());
            return;
        }

        // 反映はメインスレッドで行う (GUI操作との競合を避ける)
        manager.runGlobalTask(() -> {
            manager.auctionItems.clear();
            manager.auctionItems.putAll(newItems);
            manager.orderItems.clear();
            manager.orderItems.putAll(newOrders);
            manager.orderStorage.clear();
            manager.orderStorage.putAll(newStorage);
        });
    }

    private void processCrossServerCommands(List<DatabaseManager.CrossServerCommand> commands) {
        if (commands == null || commands.isEmpty()) return;

        String currentServer = plugin.isHubMode() ? "hub" : "smp";
        DatabaseManager db = plugin.getDatabaseManager();

        manager.runGlobalTask(() -> {
            for (DatabaseManager.CrossServerCommand cmd : commands) {
                // 自サーバーから送信されたコマンドは無視する
                if (currentServer.equals(cmd.serverFrom)) {
                    continue;
                }

                // コマンドを実行する対象者がこのサーバーにいるか、またはブロードキャストかどうかを判断
                boolean shouldExecute = false;
                
                if (cmd.targetPlayer != null && !cmd.targetPlayer.isEmpty()) {
                    if (cmd.targetPlayer.startsWith("@")) {
                        // セレクターの場合は全サーバーで実行する
                        shouldExecute = true;
                    } else {
                        // 特定のプレイヤー名の場合、このサーバーにオンラインか確認
                        org.bukkit.entity.Player p = Bukkit.getPlayerExact(cmd.targetPlayer);
                        if (p != null && p.isOnline()) {
                            shouldExecute = true;
                        }
                    }
                } else {
                    // ターゲット指定がない場合は、グローバルに反映するコマンドとみなして実行
                    shouldExecute = true;
                }

                if (shouldExecute) {
                    // 送信者がプレイヤーの場合の代理実行などを考慮しつつ、今回はコンソールから実行
                    // ※ tpaなどのプラグインコマンドは、本来送信者が必要だが、
                    // 要求に応じて影響を反映させるため、ここではコンソールとしてディスパッチする。
                    // 必要に応じて、コマンドを書き換えて実行する（例: `/smp_proxy_tpa <sender> <target>` など）
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.commandText);
                        plugin.getLogger().info("[CrossServerCmd] Executed: " + cmd.commandText + " (from " + cmd.serverFrom + ")");
                    } catch (Exception e) {
                        plugin.getLogger().warning("[CrossServerCmd] Failed to execute: " + cmd.commandText);
                    }
                }

                // 実行済みマークをつける（どのサーバーでも一度読んだらマークしてよいか、または複数サーバーに届けるなら
                // このテーブルの運用方法を変える必要があるが、現状はHubとSMPの2サーバー構成を前提とし、読んだらマークする）
                db.markCommandExecutedAsync(cmd.id);
            }
        });
    }
}
