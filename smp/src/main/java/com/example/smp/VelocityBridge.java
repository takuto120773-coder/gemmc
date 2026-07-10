package com.example.smp;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocityプロキシとのプラグインメッセージ連携 (制御メッセージのみ)。
 * プレイヤーデータの共有はMySQL (DatabaseManager) が担当する。
 *
 * チャンネル: gemsmp:main
 * メッセージ形式: [type(UTF)] [secret(UTF)] [payload...]
 *  - "SEND"    <serverName> : バックエンド→Velocity。指定サーバーへ接続
 *  - "RTP"     hub→Velocity: <worldKey> / Velocity→smp: <targetUuid> <worldKey>
 *              サーバー間RTP。Velocityは転送前にsmpへ事前通知し、参加時に前回位置へ
 *              立たせず即RTPできるようにする
 *  - "CMD"     <targetUuid> <command> : hub→Velocity→smp。コマンド転送 (smpへ移動後に実行)
 *  - "CHAT"    backend→Velocity: <message>
 *              Velocity→backend: <senderUuid> <senderName> <serverTag> <message>
 *  - "FLUSH"   Velocity→backend: このプレイヤーのデータをDBへ保存せよ (サーバー移動直前)
 *  - "FLUSHOK" backend→Velocity: FLUSHの保存完了ack。Velocityはこれを待ってから
 *              接続を切り替える → 移動先は必ず最新データを読み込める
 *
 * secret はクライアント偽装メッセージ対策。config.yml の velocity.secret と
 * Velocity側 plugins/gemsmp/config.properties の secret を同じ値にすること。
 */
public class VelocityBridge implements PluginMessageListener {

    public static final String CHANNEL = "gemsmp:main";

    private final SMPPlugin plugin;
    private final String secret;
    private SMPManager manager;

    /** Velocity経由で受信したRTP要求の時刻 (新規参加時の自動RTPとの競合回避用) */
    private final Map<UUID, Long> recentIncomingRtp = new ConcurrentHashMap<>();

    public VelocityBridge(SMPPlugin plugin) {
        this.plugin = plugin;
        this.secret = plugin.getConfig().getString("velocity.secret", "");
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void setManager(SMPManager manager) {
        this.manager = manager;
    }

    // ─── 送信 ─────────────────────────────────────────────────────────────

    /** プレイヤーを指定したVelocityサーバーへ接続する */
    public void sendToServer(Player player, String serverName) {
        send(player, "SEND", serverName);
    }

    /** (hubモード) SMPサーバーへ転送してRTPさせるようVelocityへ依頼する */
    public void requestRtp(Player player, String worldKey) {
        send(player, "RTP", worldKey);
    }

    /** チャットを他サーバーへ転送する */
    public void sendChat(Player sender, String message) {
        send(sender, "CHAT", message);
    }

    /** 指定したUUIDのプレイヤーにコマンドを実行させるよう他サーバーへ転送する */
    public void sendCommand(Player player, UUID targetUuid, String cmd) {
        send(player, "CMD", targetUuid.toString(), cmd);
    }

    /** FLUSH完了のackをVelocityへ返す (このプレイヤーのDB保存が終わった) */
    public void sendFlushOk(Player player) {
        send(player, "FLUSHOK");
    }

    // ─── クロスサーバーAH (hubからの購入/出品/キャンセルをsmpで実行する) ──
    // REQはVelocityがsmpサーバーへ、RESは対象プレイヤーがいるサーバーへ届ける

    public void sendAhBuyReq(Player carrier, UUID buyerUuid, String itemKey) {
        send(carrier, "AH_BUY_REQ", buyerUuid.toString(), itemKey);
    }

    public void sendAhBuyRes(Player carrier, UUID buyerUuid, String itemKey, String status, String itemB64, long newMoney) {
        send(carrier, "AH_BUY_RES", buyerUuid.toString(), itemKey, status,
                itemB64 == null ? "" : itemB64, String.valueOf(newMoney));
    }

    public void sendAhSellReq(Player carrier, UUID sellerUuid, long price, String itemB64) {
        send(carrier, "AH_SELL_REQ", sellerUuid.toString(), String.valueOf(price), itemB64 == null ? "" : itemB64);
    }

    // ─── クロスサーバーtpa ────────────────────────────────────────────────
    // 要求者は移動せず現サーバーに留まり、承認された時だけ移動+テレポートする

    /** (要求者側) 別サーバーにいる相手へtpa/tpahereリクエストを送る */
    public void sendTpaReqToOtherServer(Player sender, String targetName, boolean isHere) {
        send(sender, "TPA_REQ", sender.getUniqueId().toString(), sender.getName(), targetName, String.valueOf(isHere));
    }

    /** (承認者側) クロスサーバーtpaを承認したことをVelocityへ伝える */
    public void sendTpaAccept(Player target, UUID requesterUuid, boolean isHere) {
        send(target, "TPA_ACCEPT", target.getUniqueId().toString(), requesterUuid.toString(), String.valueOf(isHere));
    }

    public void sendAhSellRes(Player carrier, UUID sellerUuid, String status, String msg) {
        send(carrier, "AH_SELL_RES", sellerUuid.toString(), status, msg == null ? "" : msg);
    }

    public void sendAhCancelReq(Player carrier, UUID cancellerUuid, String itemKey) {
        send(carrier, "AH_CANCEL_REQ", cancellerUuid.toString(), itemKey);
    }

    public void sendAhCancelRes(Player carrier, UUID cancellerUuid, String itemKey, String status, String itemB64) {
        send(carrier, "AH_CANCEL_RES", cancellerUuid.toString(), itemKey, status, itemB64 == null ? "" : itemB64);
    }

    public void sendRankPrefix(Player player, String prefix) {
        send(player, "RANK_PREFIX", player.getUniqueId().toString(), prefix);
    }

    public void sendSyncAh(Player player) {
        if (player != null) {
            send(player, "SYNC_AH");
        }
    }

    public void sendSyncOrder(Player player) {
        if (player != null) {
            send(player, "SYNC_ORDER");
        }
    }

    private void send(Player player, String type, String... payload) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(type);
            out.writeUTF(secret);
            for (String s : payload) out.writeUTF(s);
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().warning("[VelocityBridge] メッセージ送信に失敗: " + e.getMessage());
        }
    }

    // ─── 状態管理 ─────────────────────────────────────────────────────────

    /** windowMs 以内にVelocity経由のRTP要求を受け取っていれば true */
    public boolean hasRecentIncomingRtp(UUID uuid, long windowMs) {
        Long t = recentIncomingRtp.get(uuid);
        return t != null && System.currentTimeMillis() - t <= windowMs;
    }

    /** プレイヤー退出時に呼ぶ */
    public void clearPlayer(UUID uuid) {
        recentIncomingRtp.remove(uuid);
    }

    // ─── 受信 ─────────────────────────────────────────────────────────────

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!CHANNEL.equals(channel)) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String type = in.readUTF();
            String msgSecret = in.readUTF();
            if (!secret.equals(msgSecret)) {
                plugin.getLogger().warning("[VelocityBridge] secret不一致のメッセージを破棄しました (" + player.getName() + ")");
                return;
            }
            switch (type) {
                case "RTP" -> {
                    // Velocity→backend: <targetUuid> <worldKey> (転送前の事前通知の場合あり)
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    String worldKey = in.readUTF();
                    recentIncomingRtp.put(targetUuid, System.currentTimeMillis());
                    if (manager != null) manager.handleIncomingRtp(targetUuid, worldKey);
                }
                case "SYNC_AH" -> {
                    if (manager != null) manager.refreshAuctionsFromDb();
                }
                case "SYNC_ORDER" -> {
                    if (manager != null) manager.refreshOrdersFromDb();
                }
                case "CHAT" -> {
                    // 別サーバーのチャット。player は単なる搬送役なので使わない
                    UUID senderUuid = UUID.fromString(in.readUTF());
                    String senderName = in.readUTF();
                    String serverTag = in.readUTF();
                    String message = in.readUTF();
                    if (manager != null) manager.handleIncomingChat(senderUuid, senderName, serverTag, message);
                }
                case "CMD" -> {
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    String cmd = in.readUTF();
                    if (manager != null) manager.handleIncomingCmd(targetUuid, cmd);
                }
                case "FLUSH" -> {
                    // サーバー移動直前: このプレイヤーのデータをDBへ保存してackを返す
                    if (manager != null) manager.handleFlushRequest(player);
                }
                case "AH_BUY_REQ" -> {
                    UUID buyer = UUID.fromString(in.readUTF());
                    String itemKey = in.readUTF();
                    if (manager != null) manager.handleAhBuyReq(buyer, itemKey);
                }
                case "AH_BUY_RES" -> {
                    UUID buyer = UUID.fromString(in.readUTF());
                    String itemKey = in.readUTF();
                    String status = in.readUTF();
                    String itemB64 = in.readUTF();
                    long newMoney = Long.parseLong(in.readUTF());
                    if (manager != null) manager.handleAhBuyRes(buyer, itemKey, status,
                            itemB64.isEmpty() ? null : itemB64, newMoney);
                }
                case "AH_SELL_REQ" -> {
                    UUID seller = UUID.fromString(in.readUTF());
                    long price = Long.parseLong(in.readUTF());
                    String itemB64 = in.readUTF();
                    if (manager != null) manager.handleAhSellReq(seller, price, itemB64);
                }
                case "AH_SELL_RES" -> {
                    UUID seller = UUID.fromString(in.readUTF());
                    String status = in.readUTF();
                    String msg = in.readUTF();
                    if (manager != null) manager.handleAhSellRes(seller, status, msg.isEmpty() ? null : msg);
                }
                case "AH_CANCEL_REQ" -> {
                    UUID canceller = UUID.fromString(in.readUTF());
                    String itemKey = in.readUTF();
                    if (manager != null) manager.handleAhCancelReq(canceller, itemKey);
                }
                case "AH_CANCEL_RES" -> {
                    UUID canceller = UUID.fromString(in.readUTF());
                    String itemKey = in.readUTF();
                    String status = in.readUTF();
                    String itemB64 = in.readUTF();
                    if (manager != null) manager.handleAhCancelRes(canceller, itemKey, status,
                            itemB64.isEmpty() ? null : itemB64);
                }
                case "TPA_ASK" -> {
                    // (承認者側サーバー) 別サーバーからのtpaリクエストを受信
                    UUID requesterUuid = UUID.fromString(in.readUTF());
                    String requesterName = in.readUTF();
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    boolean isHere = Boolean.parseBoolean(in.readUTF());
                    if (manager != null) manager.handleIncomingTpaReq(requesterUuid, requesterName, targetUuid, isHere);
                }
                case "TPA_OFFLINE" -> {
                    // (要求者側サーバー) 相手が見つからなかった
                    UUID requesterUuid = UUID.fromString(in.readUTF());
                    String targetName = in.readUTF();
                    if (manager != null) manager.notifyTpaTargetOffline(requesterUuid, targetName);
                }
                case "TPA_TP" -> {
                    // (移動先サーバー) mover が到着したら anchor へテレポートさせる
                    UUID moverUuid = UUID.fromString(in.readUTF());
                    UUID anchorUuid = UUID.fromString(in.readUTF());
                    String anchorName = in.readUTF();
                    if (manager != null) manager.handleTpaArrival(moverUuid, anchorUuid, anchorName);
                }
                default -> plugin.getLogger().warning("[VelocityBridge] 不明なメッセージタイプ: " + type);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[VelocityBridge] メッセージ解析に失敗: " + e.getMessage());
        }
    }
}
