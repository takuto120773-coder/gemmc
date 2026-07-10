package com.example.smp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * MySQLによるサーバー間プレイヤーデータ共有 (hub ⇔ smp)。
 * DBの1行が唯一の正本。DB操作はすべて専用シングルスレッドで実行し、
 * サーバースレッドを一切ブロックしない。
 *
 * ※このファイルはデプロイ済みjar(7/9 11:03)からデコンパイル復元したもの。
 *   壊れたスタブで上書きしないこと。
 */
public class DatabaseManager {

    public static class PlayerStats {
        public long money, shards, keys, bounty, kills, deaths, playtime;
        public String rank = "";
        public String team = "";
        public String ignoreCsv = "";
        public String settingsBits = "";
        /** 登録済みホーム枠のCSV (例 "1,2,4")。hubのホームGUI表示用 */
        public String homeSlots = "";
    }

    public static class PlayerState {
        public String inventoryB64;
        public String enderChestB64;
        public double health = 20.0;
        public int food = 20;
        public float saturation = 5.0f;
        public int xpLevel = 0;
        public float xpProgress = 0.0f;
        public String gamemode = "SURVIVAL";
        public int heldSlot = 0;
    }

    public static class PlayerRow {
        public PlayerStats stats = new PlayerStats();
        public PlayerState state = new PlayerState();
        public long updatedAt;
        /** この行を最後に書き込んだサーバー ("smp"/"hub"/null) */
        public String updatedBy;
        /** inventory_b64 に本物のインベントリが入っているか */
        public boolean hasInv;
    }

    public static class GlobalData {
        public long keyallSeconds;
        public int smpOnline;
        public int hubOnline;
        public int totalJoins;
        public boolean isEndOpen;
    }

    public static class BaltopEntry {
        public UUID uuid;
        public String name;
        public long money;

        public BaltopEntry(UUID uuid, String name, long money) {
            this.uuid = uuid;
            this.name = name;
            this.money = money;
        }
    }

    public static class CrossServerCommand {
        public long id;
        public String senderName;
        public String commandText;
        public String serverFrom;
        public String targetPlayer;
        public boolean isExecuted;
        public long createdAt;
    }

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private ExecutorService executor;

    private static final Set<String> STAT_COLUMNS = Set.of(
            "money", "shards", "keys_count", "bounty", "kills", "deaths", "playtime");
    private static final Set<String> STRING_COLUMNS = Set.of("rank_name", "team_name");

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("database.enabled", false)) {
            if (config.getBoolean("velocity.enabled", true)) {
                plugin.getLogger().warning("database.enabled が false のため、hub/smp間のプレイヤーデータ共有は無効です。"
                        + "共有するには config.yml の database セクションを設定してください。");
            }
            return false;
        }

        HikariConfig hikariConfig = new HikariConfig();
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String dbName = config.getString("database.database", "gemsmp");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8&connectTimeout=5000&socketTimeout=10000";

        hikariConfig.setJdbcUrl(url);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(config.getString("database.username", "root"));
        hikariConfig.setPassword(config.getString("database.password", ""));
        hikariConfig.setMaximumPoolSize(4);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(5000L);
        hikariConfig.setPoolName("GemSMP-MySQL");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "GemSMP-DB");
                t.setDaemon(true);
                return t;
            });
            plugin.getLogger().info("MySQLに接続しました。(" + host + ":" + port + "/" + dbName + ")");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("MySQLへの接続に失敗しました! hub/smp間のデータ共有は無効になります: " + e.getMessage());
            if (dataSource != null) {
                dataSource.close();
                dataSource = null;
            }
            return false;
        }
    }

    public void disconnect() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(10L, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            executor = null;
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isEnabled() {
        return dataSource != null && !dataSource.isClosed() && executor != null;
    }

    public void runAfterDb(Runnable r) {
        if (isEnabled()) {
            executor.submit(r);
        } else {
            r.run();
        }
    }

    /** DBスレッドでタスクを実行する */
    public void runAsync(Runnable task) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                plugin.getLogger().warning("[DB] 処理に失敗しました: " + e.getMessage());
            }
        });
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS player_data ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "money BIGINT NOT NULL DEFAULT 0, "
                    + "shards BIGINT NOT NULL DEFAULT 0, "
                    + "keys_count BIGINT NOT NULL DEFAULT 0, "
                    + "bounty BIGINT NOT NULL DEFAULT 0, "
                    + "kills BIGINT NOT NULL DEFAULT 0, "
                    + "deaths BIGINT NOT NULL DEFAULT 0, "
                    + "playtime BIGINT NOT NULL DEFAULT 0, "
                    + "rank_name VARCHAR(32) DEFAULT NULL, "
                    + "team_name VARCHAR(64) DEFAULT NULL, "
                    + "ignore_list TEXT, "
                    + "settings_bits VARCHAR(16) DEFAULT NULL, "
                    + "inventory_b64 LONGTEXT, "
                    + "enderchest_b64 LONGTEXT, "
                    + "health DOUBLE NOT NULL DEFAULT 20, "
                    + "food INT NOT NULL DEFAULT 20, "
                    + "saturation FLOAT NOT NULL DEFAULT 5, "
                    + "xp_level INT NOT NULL DEFAULT 0, "
                    + "xp_progress FLOAT NOT NULL DEFAULT 0, "
                    + "updated_at BIGINT NOT NULL DEFAULT 0, "
                    + "updated_by VARCHAR(8) DEFAULT NULL, "
                    + "has_inv TINYINT(1) NOT NULL DEFAULT 0, "
                    + "home_slots VARCHAR(64) DEFAULT NULL)");
            addColumnIfMissing(stmt, "player_data", "updated_by", "VARCHAR(8) DEFAULT NULL");
            addColumnIfMissing(stmt, "player_data", "has_inv", "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing(stmt, "player_data", "home_slots", "VARCHAR(64) DEFAULT NULL");
            addColumnIfMissing(stmt, "player_data", "gamemode", "VARCHAR(32) DEFAULT 'SURVIVAL'");
            addColumnIfMissing(stmt, "player_data", "held_slot", "INT NOT NULL DEFAULT 0");
            stmt.execute("CREATE TABLE IF NOT EXISTS global_data ("
                    + "id INT PRIMARY KEY, "
                    + "keyall_seconds BIGINT NOT NULL DEFAULT 2700, "
                    + "smp_online INT NOT NULL DEFAULT 0, "
                    + "total_joins INT NOT NULL DEFAULT 0)");
            addColumnIfMissing(stmt, "global_data", "is_end_open", "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing(stmt, "global_data", "hub_online", "INT NOT NULL DEFAULT 0");
            stmt.execute("INSERT IGNORE INTO global_data (id) VALUES (1)");
            stmt.execute("CREATE TABLE IF NOT EXISTS player_names ("
                    + "uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(32))");
            stmt.execute("CREATE TABLE IF NOT EXISTS auction_items ("
                    + "item_key VARCHAR(64) PRIMARY KEY, "
                    + "seller_uuid VARCHAR(36), "
                    + "seller_name VARCHAR(32), "
                    + "item_base64 LONGTEXT, "
                    + "price BIGINT NOT NULL DEFAULT 0, "
                    + "expire_time BIGINT NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS order_items ("
                    + "order_id VARCHAR(64) PRIMARY KEY, "
                    + "requester_uuid VARCHAR(36), "
                    + "requester_name VARCHAR(32), "
                    + "item_template_base64 LONGTEXT, "
                    + "price BIGINT NOT NULL DEFAULT 0, "
                    + "count INT NOT NULL DEFAULT 0, "
                    + "collected INT NOT NULL DEFAULT 0, "
                    + "listed_time BIGINT NOT NULL DEFAULT 0, "
                    + "cancelled TINYINT(1) NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS order_storage ("
                    + "order_id VARCHAR(64) PRIMARY KEY, item_base64 LONGTEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS item_prices ("
                    + "material VARCHAR(64) PRIMARY KEY, price BIGINT NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS cross_server_commands ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "sender_name VARCHAR(32), "
                    + "command_text TEXT, "
                    + "server_from VARCHAR(32), "
                    + "target_player VARCHAR(32), "
                    + "is_executed TINYINT(1) NOT NULL DEFAULT 0, "
                    + "created_at BIGINT NOT NULL)");
        }
    }

    /** 列が無ければ追加する (旧テーブルからの移行。既にあればSQLエラーを無視) */
    private void addColumnIfMissing(Statement stmt, String table, String column, String definition) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // 既に存在する場合はここに来る (正常)
        }
    }

    // ─── 個別stat即時書き込み (smpのみ。/pay等でオフラインの相手でも即DBに反映) ──

    public void updatePlayerStat(UUID uuid, String column, long value) {
        if (!isEnabled()) return;
        if (!STAT_COLUMNS.contains(column)) {
            plugin.getLogger().warning("[DB] 不正なカラム名: " + column);
            return;
        }
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO player_data (uuid, " + column + ", updated_at) VALUES (?,?,?) "
                                 + "ON DUPLICATE KEY UPDATE " + column + "=VALUES(" + column + "), updated_at=VALUES(updated_at)")) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, value);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] " + column + " の更新に失敗: " + e.getMessage());
            }
        });
    }

    /** 加算方式の更新 (最新値の上書きより競合に強い) */
    public void incrementPlayerStat(UUID uuid, String column, long delta) {
        if (!isEnabled()) return;
        if (!STAT_COLUMNS.contains(column)) {
            plugin.getLogger().warning("[DB] 不正なカラム名: " + column);
            return;
        }
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO player_data (uuid, " + column + ", updated_at) VALUES (?,?,?) "
                                 + "ON DUPLICATE KEY UPDATE " + column + "=" + column + "+?, updated_at=VALUES(updated_at)")) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, delta);
                ps.setLong(3, System.currentTimeMillis());
                ps.setLong(4, delta);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] " + column + " のインクリメントに失敗: " + e.getMessage());
            }
        });
    }

    public void updatePlayerStatString(UUID uuid, String column, String value) {
        if (!isEnabled()) return;
        if (!STRING_COLUMNS.contains(column)) {
            plugin.getLogger().warning("[DB] 不正なカラム名(文字列): " + column);
            return;
        }
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO player_data (uuid, " + column + ", updated_at) VALUES (?,?,?) "
                                 + "ON DUPLICATE KEY UPDATE " + column + "=VALUES(" + column + "), updated_at=VALUES(updated_at)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, value);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] " + column + " の更新に失敗: " + e.getMessage());
            }
        });
    }

    // ─── オークション/オーダーの共有 ──────────────────────────────────────

    public void addAuctionItem(SMPManager.AuctionItem ai) {
        if (!isEnabled() || ai == null || ai.item == null) return;
        String key = ai.id;
        String sellerUuid = ai.sellerUUID != null ? ai.sellerUUID.toString() : "";
        String sellerName = ai.sellerName != null ? ai.sellerName : "";
        String itemB64 = serializeItems(new ItemStack[]{ai.item});
        long price = ai.price;
        long listed = ai.listedTime;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO auction_items (item_key, seller_uuid, seller_name, item_base64, price, expire_time) "
                                 + "VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                                 + "seller_uuid=VALUES(seller_uuid), seller_name=VALUES(seller_name), "
                                 + "item_base64=VALUES(item_base64), price=VALUES(price), expire_time=VALUES(expire_time)")) {
                ps.setString(1, key);
                ps.setString(2, sellerUuid);
                ps.setString(3, sellerName);
                ps.setString(4, itemB64);
                ps.setLong(5, price);
                ps.setLong(6, listed);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] 出品の保存に失敗: " + e.getMessage());
            }
        });
    }

    public void removeAuctionItem(String itemKey) {
        if (!isEnabled() || itemKey == null) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM auction_items WHERE item_key=?")) {
                ps.setString(1, itemKey);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] 出品の削除に失敗: " + e.getMessage());
            }
        });
    }

    public void addOrderItem(SMPManager.OrderItem oi) {
        if (!isEnabled() || oi == null) return;
        String orderId = oi.id;
        String requesterUuid = oi.requesterUUID != null ? oi.requesterUUID.toString() : "";
        String requesterName = oi.requesterName != null ? oi.requesterName : "";
        String templateB64 = oi.itemTemplate != null ? serializeItems(new ItemStack[]{oi.itemTemplate}) : null;
        long price = oi.price;
        int count = oi.count;
        int collected = oi.collected;
        long listed = oi.listedTime;
        boolean cancelled = oi.cancelled;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO order_items (order_id, requester_uuid, requester_name, item_template_base64, price, count, collected, listed_time, cancelled) "
                                 + "VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
                                 + "requester_uuid=VALUES(requester_uuid), requester_name=VALUES(requester_name), "
                                 + "item_template_base64=VALUES(item_template_base64), price=VALUES(price), "
                                 + "count=VALUES(count), collected=VALUES(collected), listed_time=VALUES(listed_time), cancelled=VALUES(cancelled)")) {
                ps.setString(1, orderId);
                ps.setString(2, requesterUuid);
                ps.setString(3, requesterName);
                ps.setString(4, templateB64);
                ps.setLong(5, price);
                ps.setInt(6, count);
                ps.setInt(7, collected);
                ps.setLong(8, listed);
                ps.setBoolean(9, cancelled);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] オーダーの保存に失敗: " + e.getMessage());
            }
        });
    }

    public void removeOrderItem(String orderId) {
        if (!isEnabled() || orderId == null) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
                    ps.setString(1, orderId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_storage WHERE order_id=?")) {
                    ps.setString(1, orderId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] オーダーの削除に失敗: " + e.getMessage());
            }
        });
    }

    public void loadAllAuctionsAsync(Consumer<List<SMPManager.AuctionItem>> callback) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            List<SMPManager.AuctionItem> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM auction_items");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SMPManager.AuctionItem ai = new SMPManager.AuctionItem();
                    ai.id = rs.getString("item_key");
                    String uuidStr = rs.getString("seller_uuid");
                    if (uuidStr != null && !uuidStr.isEmpty()) {
                        try { ai.sellerUUID = UUID.fromString(uuidStr); } catch (Exception ignored) {}
                    }
                    ai.sellerName = rs.getString("seller_name");
                    String b64 = rs.getString("item_base64");
                    if (b64 != null && !b64.isEmpty()) {
                        ItemStack[] arr = deserializeItemsSafe(b64);
                        if (arr != null && arr.length > 0) ai.item = arr[0];
                    }
                    ai.price = rs.getLong("price");
                    ai.listedTime = rs.getLong("expire_time");
                    if (ai.item != null) list.add(ai);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] Failed to load auctions: " + e.getMessage());
            }
            if (callback != null) {
                ((SMPPlugin) plugin).getSmpManager().runGlobalTask(() -> callback.accept(list));
            }
        });
    }

    public void loadAllOrdersAsync(Consumer<List<SMPManager.OrderItem>> callback) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            List<SMPManager.OrderItem> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM order_items");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SMPManager.OrderItem oi = new SMPManager.OrderItem();
                    oi.id = rs.getString("order_id");
                    String uuidStr = rs.getString("requester_uuid");
                    if (uuidStr != null && !uuidStr.isEmpty()) {
                        try { oi.requesterUUID = UUID.fromString(uuidStr); } catch (Exception ignored) {}
                    }
                    oi.requesterName = rs.getString("requester_name");
                    String b64 = rs.getString("item_template_base64");
                    if (b64 != null && !b64.isEmpty()) {
                        ItemStack[] arr = deserializeItemsSafe(b64);
                        if (arr != null && arr.length > 0) oi.itemTemplate = arr[0];
                    }
                    oi.price = rs.getLong("price");
                    oi.count = rs.getInt("count");
                    oi.collected = rs.getInt("collected");
                    oi.listedTime = rs.getLong("listed_time");
                    oi.cancelled = rs.getBoolean("cancelled");
                    list.add(oi);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] Failed to load orders: " + e.getMessage());
            }
            if (callback != null) {
                ((SMPPlugin) plugin).getSmpManager().runGlobalTask(() -> callback.accept(list));
            }
        });
    }

    public void updateOrderStorage(String orderId, List<ItemStack> items) {
        if (!isEnabled() || orderId == null) return;
        String b64 = items != null && !items.isEmpty() ? serializeItems(items.toArray(new ItemStack[0])) : null;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                if (b64 != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO order_storage (order_id, item_base64) VALUES (?, ?) "
                                    + "ON DUPLICATE KEY UPDATE item_base64=VALUES(item_base64)")) {
                        ps.setString(1, orderId);
                        ps.setString(2, b64);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM order_storage WHERE order_id=?")) {
                        ps.setString(1, orderId);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] Failed to update order storage: " + e.getMessage());
            }
        });
    }

    public void loadAllOrderStorageAsync(Consumer<Map<String, List<ItemStack>>> callback) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            Map<String, List<ItemStack>> map = new HashMap<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM order_storage");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String orderId = rs.getString("order_id");
                    String b64 = rs.getString("item_base64");
                    if (b64 == null || b64.isEmpty()) continue;
                    ItemStack[] arr = deserializeItemsSafe(b64);
                    if (arr == null) continue;
                    List<ItemStack> list = new ArrayList<>();
                    for (ItemStack is : arr) {
                        if (is == null || is.getType() == Material.AIR) continue;
                        list.add(is);
                    }
                    map.put(orderId, list);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] Failed to load order storage: " + e.getMessage());
            }
            if (callback != null) {
                ((SMPPlugin) plugin).getSmpManager().runGlobalTask(() -> callback.accept(map));
            }
        });
    }

    // ─── シリアライズ (SMPManager.itemsToBase64/itemsFromBase64 と互換) ──────

    /** ItemStack配列をGZIP+Base64にシリアライズする */
    public static String serializeItems(ItemStack[] items) {
        try (java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
             java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(raw);
             org.bukkit.util.io.BukkitObjectOutputStream out = new org.bukkit.util.io.BukkitObjectOutputStream(gzip)) {
            out.writeInt(items.length);
            for (ItemStack it : items) out.writeObject(it);
            out.flush();
            gzip.finish();
            return java.util.Base64.getEncoder().encodeToString(raw.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack[] deserializeItemsSafe(String base64) {
        try (java.io.ByteArrayInputStream raw = new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(base64));
             java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(raw);
             org.bukkit.util.io.BukkitObjectInputStream in = new org.bukkit.util.io.BukkitObjectInputStream(gzip)) {
            int len = in.readInt();
            ItemStack[] items = new ItemStack[len];
            for (int i = 0; i < len; i++) items[i] = (ItemStack) in.readObject();
            return items;
        } catch (Exception e) {
            return null;
        }
    }

    // ─── プレイヤーデータ保存/読込 ─────────────────────────────────────────

    /**
     * プレイヤーデータを保存する。
     * stats != null (smp): 全カラム + state。stats == null (hub): stateのみ (statsは触らない)。
     */
    public void savePlayerAsync(UUID uuid, PlayerStats stats, PlayerState state, String updatedBy, Runnable afterSave) {
        if (!isEnabled()) {
            if (afterSave != null) afterSave.run();
            return;
        }
        long now = System.currentTimeMillis();
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                if (stats != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO player_data (uuid, money, shards, keys_count, bounty, kills, deaths, playtime, "
                                    + "rank_name, team_name, ignore_list, settings_bits, "
                                    + "inventory_b64, enderchest_b64, health, food, saturation, xp_level, xp_progress, "
                                    + "gamemode, held_slot, updated_at, updated_by, home_slots, has_inv) "
                                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1) "
                                    + "ON DUPLICATE KEY UPDATE "
                                    + "money=VALUES(money), shards=VALUES(shards), keys_count=VALUES(keys_count), "
                                    + "bounty=VALUES(bounty), kills=VALUES(kills), deaths=VALUES(deaths), playtime=VALUES(playtime), "
                                    + "rank_name=VALUES(rank_name), team_name=VALUES(team_name), ignore_list=VALUES(ignore_list), "
                                    + "settings_bits=VALUES(settings_bits), "
                                    + "inventory_b64=VALUES(inventory_b64), enderchest_b64=VALUES(enderchest_b64), "
                                    + "health=VALUES(health), food=VALUES(food), saturation=VALUES(saturation), "
                                    + "xp_level=VALUES(xp_level), xp_progress=VALUES(xp_progress), "
                                    + "gamemode=VALUES(gamemode), held_slot=VALUES(held_slot), "
                                    + "updated_at=VALUES(updated_at), updated_by=VALUES(updated_by), "
                                    + "home_slots=VALUES(home_slots), has_inv=1")) {
                        ps.setString(1, uuid.toString());
                        ps.setLong(2, stats.money);
                        ps.setLong(3, stats.shards);
                        ps.setLong(4, stats.keys);
                        ps.setLong(5, stats.bounty);
                        ps.setLong(6, stats.kills);
                        ps.setLong(7, stats.deaths);
                        ps.setLong(8, stats.playtime);
                        ps.setString(9, stats.rank);
                        ps.setString(10, stats.team);
                        ps.setString(11, stats.ignoreCsv);
                        ps.setString(12, stats.settingsBits);
                        ps.setString(13, state.inventoryB64);
                        ps.setString(14, state.enderChestB64);
                        ps.setDouble(15, state.health);
                        ps.setInt(16, state.food);
                        ps.setFloat(17, state.saturation);
                        ps.setInt(18, state.xpLevel);
                        ps.setFloat(19, state.xpProgress);
                        ps.setString(20, state.gamemode);
                        ps.setInt(21, state.heldSlot);
                        ps.setLong(22, now);
                        ps.setString(23, updatedBy);
                        ps.setString(24, stats.homeSlots);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO player_data (uuid, inventory_b64, enderchest_b64, health, food, saturation, "
                                    + "xp_level, xp_progress, gamemode, held_slot, updated_at, updated_by, has_inv) "
                                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1) "
                                    + "ON DUPLICATE KEY UPDATE "
                                    + "inventory_b64=VALUES(inventory_b64), enderchest_b64=VALUES(enderchest_b64), "
                                    + "health=VALUES(health), food=VALUES(food), saturation=VALUES(saturation), "
                                    + "xp_level=VALUES(xp_level), xp_progress=VALUES(xp_progress), "
                                    + "gamemode=VALUES(gamemode), held_slot=VALUES(held_slot), "
                                    + "updated_at=VALUES(updated_at), updated_by=VALUES(updated_by), has_inv=1")) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, state.inventoryB64);
                        ps.setString(3, state.enderChestB64);
                        ps.setDouble(4, state.health);
                        ps.setInt(5, state.food);
                        ps.setFloat(6, state.saturation);
                        ps.setInt(7, state.xpLevel);
                        ps.setFloat(8, state.xpProgress);
                        ps.setString(9, state.gamemode);
                        ps.setInt(10, state.heldSlot);
                        ps.setLong(11, now);
                        ps.setString(12, updatedBy);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] " + uuid + " の保存に失敗: " + e.getMessage());
            } finally {
                if (afterSave != null) afterSave.run();
            }
        });
    }

    /**
     * (smp起動時) 既存プレイヤーの所持金・EC等を一括でDBへ移行する。
     * inventory_b64 / has_inv / updated_by には触れない (インベントリはsmp初回参加時に取得)。
     */
    public void bulkSeedStatsEc(List<Object[]> records) {
        if (!isEnabled() || records.isEmpty()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_data (uuid, money, shards, keys_count, bounty, kills, deaths, playtime, "
                                + "rank_name, team_name, ignore_list, settings_bits, enderchest_b64, home_slots, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "money=VALUES(money), shards=VALUES(shards), keys_count=VALUES(keys_count), "
                                + "bounty=VALUES(bounty), kills=VALUES(kills), deaths=VALUES(deaths), playtime=VALUES(playtime), "
                                + "rank_name=VALUES(rank_name), team_name=VALUES(team_name), ignore_list=VALUES(ignore_list), "
                                + "settings_bits=VALUES(settings_bits), enderchest_b64=VALUES(enderchest_b64), home_slots=VALUES(home_slots)")) {
                    for (Object[] r : records) {
                        PlayerStats st = (PlayerStats) r[1];
                        ps.setString(1, r[0].toString());
                        ps.setLong(2, st.money);
                        ps.setLong(3, st.shards);
                        ps.setLong(4, st.keys);
                        ps.setLong(5, st.bounty);
                        ps.setLong(6, st.kills);
                        ps.setLong(7, st.deaths);
                        ps.setLong(8, st.playtime);
                        ps.setString(9, st.rank);
                        ps.setString(10, st.team);
                        ps.setString(11, st.ignoreCsv);
                        ps.setString(12, st.settingsBits);
                        ps.setString(13, (String) r[2]);
                        ps.setString(14, st.homeSlots);
                        ps.setLong(15, System.currentTimeMillis());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                plugin.getLogger().info("[DB] " + records.size() + "人分のデータをDBへ移行しました。");
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] 一括移行に失敗: " + e.getMessage());
            }
        });
    }

    /** プレイヤーデータを読み込む。行が無ければ null をコールバックに渡す (DBスレッド上) */
    public void loadPlayerAsync(UUID uuid, Consumer<PlayerRow> callback) {
        if (!isEnabled()) {
            callback.accept(null);
            return;
        }
        executor.submit(() -> {
            PlayerRow row = null;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_data WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        row = new PlayerRow();
                        row.stats.money = rs.getLong("money");
                        row.stats.shards = rs.getLong("shards");
                        row.stats.keys = rs.getLong("keys_count");
                        row.stats.bounty = rs.getLong("bounty");
                        row.stats.kills = rs.getLong("kills");
                        row.stats.deaths = rs.getLong("deaths");
                        row.stats.playtime = rs.getLong("playtime");
                        row.stats.rank = nullToEmpty(rs.getString("rank_name"));
                        row.stats.team = nullToEmpty(rs.getString("team_name"));
                        row.stats.ignoreCsv = nullToEmpty(rs.getString("ignore_list"));
                        row.stats.settingsBits = nullToEmpty(rs.getString("settings_bits"));
                        row.stats.homeSlots = nullToEmpty(rs.getString("home_slots"));
                        row.state.inventoryB64 = rs.getString("inventory_b64");
                        row.state.enderChestB64 = rs.getString("enderchest_b64");
                        row.state.health = rs.getDouble("health");
                        row.state.food = rs.getInt("food");
                        row.state.saturation = rs.getFloat("saturation");
                        row.state.xpLevel = rs.getInt("xp_level");
                        row.state.xpProgress = rs.getFloat("xp_progress");
                        row.state.gamemode = nullToEmpty(rs.getString("gamemode"));
                        if (row.state.gamemode.isEmpty()) row.state.gamemode = "SURVIVAL";
                        row.state.heldSlot = rs.getInt("held_slot");
                        row.updatedAt = rs.getLong("updated_at");
                        row.updatedBy = rs.getString("updated_by");
                        row.hasInv = rs.getBoolean("has_inv");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] " + uuid + " の読込に失敗: " + e.getMessage());
            }
            try {
                callback.accept(row);
            } catch (Exception e) {
                plugin.getLogger().warning("[DB] 読込コールバックに失敗: " + e.getMessage());
            }
        });
    }

    // ─── グローバルデータ ─────────────────────────────────────────────────

    /** (smp) keyallタイマー・オンライン人数・総参加人数・end開放状態を書き込む */
    public void pushGlobalAsync(long keyallSeconds, int smpOnline, int totalJoins, boolean isEndOpen) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE global_data SET keyall_seconds=?, smp_online=?, total_joins=?, is_end_open=? WHERE id=1")) {
                ps.setLong(1, keyallSeconds);
                ps.setInt(2, smpOnline);
                ps.setInt(3, totalJoins);
                ps.setBoolean(4, isEndOpen);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] global_data の更新に失敗: " + e.getMessage());
            }
        });
    }

    /** (hub) hubのオンライン人数を書き込む */
    public void pushGlobalHubAsync(int hubOnline) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE global_data SET hub_online=? WHERE id=1")) {
                ps.setInt(1, hubOnline);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] global_data (hub) の更新に失敗: " + e.getMessage());
            }
        });
    }

    public void loadGlobalAsync(Consumer<GlobalData> callback) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM global_data WHERE id=1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    GlobalData g = new GlobalData();
                    g.keyallSeconds = rs.getLong("keyall_seconds");
                    g.smpOnline = rs.getInt("smp_online");
                    g.hubOnline = rs.getInt("hub_online");
                    g.totalJoins = rs.getInt("total_joins");
                    g.isEndOpen = rs.getBoolean("is_end_open");
                    callback.accept(g);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] global_data の読込に失敗: " + e.getMessage());
            }
        });
    }

    // ─── Baltop / プレイヤー名 ───────────────────────────────────────────

    /** 所持金トップを取得する (hub/smpどちらでも使える。DBが正本) */
    public void getBaltopAsync(int limit, Consumer<List<BaltopEntry>> callback) {
        if (!isEnabled()) {
            callback.accept(new ArrayList<>());
            return;
        }
        executor.submit(() -> {
            List<BaltopEntry> top = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT p.uuid, p.money, n.player_name FROM player_data p "
                                 + "LEFT JOIN player_names n ON p.uuid = n.uuid ORDER BY p.money DESC LIMIT ?")) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uidStr = rs.getString("uuid");
                        long money = rs.getLong("money");
                        String pName = rs.getString("player_name");
                        try {
                            top.add(new BaltopEntry(UUID.fromString(uidStr), pName, money));
                        } catch (Exception ignored) {}
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] Baltop取得に失敗: " + e.getMessage());
            }
            callback.accept(top);
        });
    }

    /** プレイヤー名を記録する (baltop等の表示用) */
    public void updatePlayerNameAsync(UUID uuid, String name) {
        if (!isEnabled() || name == null) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO player_names (uuid, player_name) VALUES (?, ?) "
                                 + "ON DUPLICATE KEY UPDATE player_name=VALUES(player_name)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    // ─── アイテム価格 ─────────────────────────────────────────────────────

    public void saveItemPrice(String materialName, long price) {
        if (!isEnabled() || materialName == null) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO item_prices (material, price) VALUES (?, ?) "
                                 + "ON DUPLICATE KEY UPDATE price=VALUES(price)")) {
                ps.setString(1, materialName);
                ps.setLong(2, price);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] item_prices の更新に失敗: " + e.getMessage());
            }
        });
    }

    // ─── クロスサーバーコマンドログ ───────────────────────────────────────

    public void logCommandAsync(String senderName, String commandText, String serverFrom, String targetPlayer) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO cross_server_commands (sender_name, command_text, server_from, target_player, created_at) "
                                 + "VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, senderName != null ? senderName : "CONSOLE");
                ps.setString(2, commandText);
                ps.setString(3, serverFrom);
                ps.setString(4, targetPlayer);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] コマンドログの保存に失敗: " + e.getMessage());
            }
        });
    }

    public void getPendingCommandsAsync(Consumer<List<CrossServerCommand>> callback) {
        if (!isEnabled()) {
            callback.accept(new ArrayList<>());
            return;
        }
        executor.submit(() -> {
            List<CrossServerCommand> commands = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT * FROM cross_server_commands WHERE is_executed = 0 ORDER BY id ASC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CrossServerCommand cmd = new CrossServerCommand();
                    cmd.id = rs.getLong("id");
                    cmd.senderName = rs.getString("sender_name");
                    cmd.commandText = rs.getString("command_text");
                    cmd.serverFrom = rs.getString("server_from");
                    cmd.targetPlayer = rs.getString("target_player");
                    cmd.isExecuted = rs.getBoolean("is_executed");
                    cmd.createdAt = rs.getLong("created_at");
                    commands.add(cmd);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] 未実行コマンドの取得に失敗: " + e.getMessage());
            }
            callback.accept(commands);
        });
    }

    public void markCommandExecutedAsync(long commandId) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE cross_server_commands SET is_executed = 1 WHERE id = ?")) {
                ps.setLong(1, commandId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("[DB] コマンド実行済みマークに失敗: " + e.getMessage());
            }
        });
    }

    public void deleteOldCommandsAsync(long olderThanMillis) {
        if (!isEnabled()) return;
        executor.submit(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM cross_server_commands WHERE created_at < ? AND is_executed = 1")) {
                ps.setLong(1, System.currentTimeMillis() - olderThanMillis);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        });
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
