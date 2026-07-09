package com.example.smp.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * GemSMP のサーバー間連携 (Velocity側)。
 *
 * プレイヤーデータの共有はMySQLが担当するため、Velocityは制御のみを行う:
 * - FLUSHハンドシェイク: サーバー移動前に旧サーバーへ保存指示を送り、
 *   保存完了(FLUSHOK)を待ってから接続を切り替える → 移動先は必ず最新データを読める
 * - クロスサーバーRTP / コマンド転送 (smpへ移動してから実行)
 * - チャット共有 / タブリスト共有 / 入退出メッセージ
 * - 前回smpで抜けたプレイヤーを次回参加時に直接smpへ接続
 *
 * メッセージ形式はバックエンド側 VelocityBridge と共通:
 *   [type(UTF)] [secret(UTF)] [payload...]
 */
@Plugin(
        id = "gemsmp",
        name = "GemSMP-Velocity",
        version = "1.0.0",
        description = "GemSMP cross-server bridge",
        authors = {"GemMC"}
)
public class GemSMPVelocity {

    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("gemsmp", "main");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    /** FLUSHのack待ちタイムアウト (これを超えたら接続を続行する) */
    private static final long FLUSH_TIMEOUT_MS = 1500;

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    /** SMPサーバーへの転送完了を待っているRTP要求 (uuid -> worldKey) ※事前通知に失敗した場合のみ使用 */
    private final Map<UUID, String> pendingRtp = new ConcurrentHashMap<>();
    /** SMPサーバーへの転送完了を待っているコマンド実行要求 (uuid -> command) ※事前通知に失敗した場合のみ使用 */
    private final Map<UUID, String> pendingCmds = new ConcurrentHashMap<>();
    /** 各プレイヤーが最後にいたサーバー (次回参加時の初期サーバー振り分け用)。ディスクにも永続化 */
    private final Map<UUID, String> lastServers = new ConcurrentHashMap<>();
    /** サーバー移動時のFLUSH ack待ち */
    private final Map<UUID, CompletableFuture<Void>> flushAcks = new ConcurrentHashMap<>();
    /** タブリストにこちらが追加したエントリ (viewer uuid -> target uuids) */
    private final Map<UUID, Set<UUID>> tabEntries = new ConcurrentHashMap<>();

    private String hubServer = "hub";
    private String smpServer = "smp";
    private String secret = "";

    // Discord Integration
    private String discordBotToken = "";
    private String discordGuildId = "";
    private net.dv8tion.jda.api.JDA jda = null;
    private net.dv8tion.jda.api.entities.channel.concrete.TextChannel linkedChatChannel = null;
    private net.dv8tion.jda.api.entities.channel.concrete.Category serverInfoCategory = null;
    private String pinnedMessageId = "";

    @Inject
    public GemSMPVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        loadConfig();
        proxy.getChannelRegistrar().register(CHANNEL);
        loadLastServers();

        // タブリスト共有: 1秒ごとに他サーバーのプレイヤーをタブへ反映
        proxy.getScheduler().buildTask(this, this::refreshTabLists)
                .delay(1, TimeUnit.SECONDS)
                .repeat(1, TimeUnit.SECONDS)
                .schedule();

        logger.info("GemSMP-Velocity が有効になりました。(hub={}, smp={})", hubServer, smpServer);
        if (secret.isEmpty()) {
            logger.warn("secret が未設定です。plugins/gemsmp/config.properties と各サーバーの GemSMP config.yml に同じ secret を設定してください。");
        }

        initDiscordBot();
    }

    private void loadConfig() {
        try {
            Files.createDirectories(dataDirectory);
            Path file = dataDirectory.resolve("config.properties");
            if (!Files.exists(file)) {
                Files.write(file, List.of(
                        "# GemSMP-Velocity 設定",
                        "# velocity.toml の [servers] に登録した名前を指定",
                        "hub-server=hub",
                        "smp-server=smp",
                        "# 各バックエンドの GemSMP config.yml の velocity.secret と同じ値にする",
                        "secret=",
                        "# Discord Bot Token (空で無効化)",
                        "discord-bot-token=",
                        "# Discord サーバー (Guild) ID (空の場合は最初に参加しているサーバーを自動選択)",
                        "discord-guild-id="
                ));
            }
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }
            hubServer = props.getProperty("hub-server", "hub").trim();
            smpServer = props.getProperty("smp-server", "smp").trim();
            secret = props.getProperty("secret", "").trim();
            discordBotToken = props.getProperty("discord-bot-token", "").trim();
            discordGuildId = props.getProperty("discord-guild-id", "").trim();
        } catch (IOException e) {
            logger.error("config.properties の読み込みに失敗しました", e);
        }
    }

    // ─── プラグインメッセージ ────────────────────────────────────────────

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;
        // クライアント偽装防止のため、このチャンネルは常にプロキシで止める
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection connection)) return;

        Player player = connection.getPlayer();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String type = in.readUTF();
            String msgSecret = in.readUTF();
            if (!secret.equals(msgSecret)) {
                logger.warn("secret不一致のメッセージを破棄しました (from {})", connection.getServerInfo().getName());
                return;
            }
            switch (type) {
                case "SEND" -> {
                    String target = in.readUTF();
                    connectPlayer(player, target);
                }
                case "RANK_PREFIX" -> {
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    String prefix = in.readUTF();
                    tabNames.put(targetUuid, prefix);
                }
                case "RTP" -> {
                    String worldKey = in.readUTF();
                    if (isOnServer(player, smpServer)) {
                        sendRtpToBackend(player, worldKey);
                    } else {
                        // 転送前にsmpバックエンドへ事前通知 (参加時に前回位置に立たせず即RTPさせるため)
                        if (!preDeliverToSmp(buildRtpPayload(player.getUniqueId(), worldKey))) {
                            pendingRtp.put(player.getUniqueId(), worldKey);
                        }
                        connectPlayer(player, smpServer);
                    }
                }
                case "CMD" -> {
                    // hubからのコマンド転送: プレイヤーをsmpへ移動させ、到着時に実行させる
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    String cmdLine = in.readUTF();
                    Player target = proxy.getPlayer(targetUuid).orElse(player);
                    if (isOnServer(target, smpServer)) {
                        target.getCurrentServer().ifPresent(cs ->
                                cs.sendPluginMessage(CHANNEL, buildCmdPayload(targetUuid, cmdLine)));
                    } else {
                        if (!preDeliverToSmp(buildCmdPayload(targetUuid, cmdLine))) {
                            pendingCmds.put(targetUuid, cmdLine);
                        }
                        connectPlayer(target, smpServer);
                    }
                }
                case "CHAT" -> {
                    String message = in.readUTF();
                    forwardChat(connection, player, message);
                }
                case "FLUSHOK" -> {
                    // 旧サーバーのDB保存完了 → 保留中のサーバー切り替えを続行
                    CompletableFuture<Void> ack = flushAcks.remove(player.getUniqueId());
                    if (ack != null) ack.complete(null);
                }
                case "AH_BUY_REQ", "AH_SELL_REQ", "AH_CANCEL_REQ" -> {
                    // hubからのAH操作要求 → smpサーバーへそのまま届ける
                    UUID requester = UUID.fromString(in.readUTF());
                    if (!preDeliverToSmp(event.getData())) {
                        // smpに誰もいない → 失敗応答を送信元サーバーへ返す
                        connection.sendPluginMessage(CHANNEL, buildAhFailure(type, requester, in));
                    }
                }
                case "AH_BUY_RES", "AH_SELL_RES", "AH_CANCEL_RES" -> {
                    // smpからのAH操作応答 → 対象プレイヤーがいるサーバーへそのまま届ける
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    proxy.getPlayer(targetUuid)
                            .flatMap(Player::getCurrentServer)
                            .ifPresent(cs -> cs.sendPluginMessage(CHANNEL, event.getData()));
                }
                case "SYNC_AH", "SYNC_ORDER" -> {
                    // DB更新の通知を他サーバーへブロードキャスト
                    for (RegisteredServer rs : proxy.getAllServers()) {
                        if (!rs.getServerInfo().equals(connection.getServerInfo())) {
                            rs.sendPluginMessage(CHANNEL, event.getData());
                        }
                    }
                }
                case "TPA_REQ" -> {
                    // 要求者サーバーから: 相手を探して相手のサーバーへ TPA_ASK を届ける
                    UUID requesterUuid = UUID.fromString(in.readUTF());
                    String requesterName = in.readUTF();
                    String targetName = in.readUTF();
                    boolean isHere = Boolean.parseBoolean(in.readUTF());
                    handleTpaRequest(connection, requesterUuid, requesterName, targetName, isHere);
                }
                case "TPA_ACCEPT" -> {
                    // 承認者サーバーから: mover を anchor のサーバーへ転送し、到着後に tp させる
                    UUID targetUuid = UUID.fromString(in.readUTF());
                    UUID requesterUuid = UUID.fromString(in.readUTF());
                    boolean isHere = Boolean.parseBoolean(in.readUTF());
                    handleTpaAccept(requesterUuid, targetUuid, isHere);
                }
                default -> logger.warn("不明なメッセージタイプ: {}", type);
            }
        } catch (IOException e) {
            logger.warn("プラグインメッセージの解析に失敗しました", e);
        }
    }

    // ─── FLUSHハンドシェイク (データ受け渡しの直列化) ─────────────────────

    /**
     * サーバー移動の直前に旧サーバーへFLUSH(DB保存指示)を送り、保存完了を待ってから
     * 接続を切り替える。これで移動先のサーバーは必ず最新のデータをDBから読み込める。
     * タイムアウトしても接続は続行する (旧サーバーのquit時保存がフォールバック)。
     */
    @Subscribe
    public EventTask onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        Optional<ServerConnection> current = player.getCurrentServer();
        if (current.isEmpty()) return null; // 初回接続はFLUSH不要
        ServerConnection from = current.get();

        return EventTask.async(() -> {
            UUID uuid = player.getUniqueId();
            try {
                CompletableFuture<Void> ack = new CompletableFuture<>();
                flushAcks.put(uuid, ack);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF("FLUSH");
                out.writeUTF(secret);
                from.sendPluginMessage(CHANNEL, bytes.toByteArray());
                ack.get(FLUSH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception timeoutOrError) {
                // ackが来なくても移動は続行 (保存はquitイベントでも行われる)
            } finally {
                flushAcks.remove(uuid);
            }
        });
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 最後にいたサーバーを記録 (次回参加時の初期サーバー振り分け用)
        player.getCurrentServer().ifPresent(cs ->
                lastServers.put(uuid, cs.getServerInfo().getName()));

        boolean onSmp = isOnServer(player, smpServer);

        // 事前通知に失敗していたRTP/コマンドを、接続確立後すぐに配信する
        final String rtpWorld = onSmp ? pendingRtp.remove(uuid) : null;
        final String cmdLine = onSmp ? pendingCmds.remove(uuid) : null;
        if (rtpWorld != null || cmdLine != null) {
            proxy.getScheduler().buildTask(this, () -> {
                if (rtpWorld != null) sendRtpToBackend(player, rtpWorld);
                if (cmdLine != null) {
                    player.getCurrentServer().ifPresent(cs ->
                            cs.sendPluginMessage(CHANNEL, buildCmdPayload(uuid, cmdLine)));
                }
            }).delay(150, TimeUnit.MILLISECONDS).schedule();
        }
    }

    /** 前回smpサーバーで抜けたプレイヤーは、次回参加時に直接smpへ接続させる */
    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        String last = lastServers.get(event.getPlayer().getUniqueId());
        if (last != null && last.equalsIgnoreCase(smpServer)) {
            proxy.getServer(smpServer).ifPresent(event::setInitialServer);
        }
    }

    // ─── RTP/CMD ペイロード生成・事前配信 ────────────────────────────────

    private byte[] buildRtpPayload(UUID targetUuid, String worldKey) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("RTP");
            out.writeUTF(secret);
            out.writeUTF(targetUuid.toString());
            out.writeUTF(worldKey);
            return bytes.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private byte[] buildCmdPayload(UUID targetUuid, String cmdLine) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("CMD");
            out.writeUTF(secret);
            out.writeUTF(targetUuid.toString());
            out.writeUTF(cmdLine);
            return bytes.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    // ─── クロスサーバーtpa ────────────────────────────────────────────────

    /** 要求者サーバーからのtpaリクエストを相手のサーバーへ中継する */
    private void handleTpaRequest(ServerConnection source, UUID requesterUuid, String requesterName,
                                  String targetName, boolean isHere) {
        Optional<Player> target = proxy.getPlayer(targetName);
        if (target.isEmpty() || target.get().getCurrentServer().isEmpty()) {
            // 相手がオンラインでない → 要求者サーバーへ通知
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeUTF("TPA_OFFLINE");
                out.writeUTF(secret);
                out.writeUTF(requesterUuid.toString());
                out.writeUTF(targetName);
                source.sendPluginMessage(CHANNEL, bytes.toByteArray());
            } catch (IOException ignored) {}
            return;
        }
        Player t = target.get();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("TPA_ASK");
            out.writeUTF(secret);
            out.writeUTF(requesterUuid.toString());
            out.writeUTF(requesterName);
            out.writeUTF(t.getUniqueId().toString());
            out.writeUTF(String.valueOf(isHere));
            t.getCurrentServer().get().sendPluginMessage(CHANNEL, bytes.toByteArray());
        } catch (IOException ignored) {}
    }

    /**
     * 承認された。mover(移動する側)を anchor(留まる側)のサーバーへ転送し、
     * anchor のサーバーへ TPA_TP を送って到着後にテレポートさせる。
     *  - tpa (isHere=false): 要求者が承認者の元へ → mover=requester, anchor=target
     *  - tpahere (isHere=true): 承認者が要求者の元へ → mover=target, anchor=requester
     */
    private void handleTpaAccept(UUID requesterUuid, UUID targetUuid, boolean isHere) {
        UUID moverUuid = isHere ? targetUuid : requesterUuid;
        UUID anchorUuid = isHere ? requesterUuid : targetUuid;
        Optional<Player> moverO = proxy.getPlayer(moverUuid);
        Optional<Player> anchorO = proxy.getPlayer(anchorUuid);
        if (moverO.isEmpty() || anchorO.isEmpty()) return;
        Player mover = moverO.get();
        Player anchor = anchorO.get();
        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> anchorServerO =
                anchor.getCurrentServer().map(ServerConnection::getServer);
        if (anchorServerO.isEmpty()) return;
        com.velocitypowered.api.proxy.server.RegisteredServer anchorServer = anchorServerO.get();

        byte[] tpMsg;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("TPA_TP");
            out.writeUTF(secret);
            out.writeUTF(moverUuid.toString());
            out.writeUTF(anchorUuid.toString());
            out.writeUTF(anchor.getUsername());
            tpMsg = bytes.toByteArray();
        } catch (IOException e) {
            return;
        }

        boolean moverOnAnchorServer = mover.getCurrentServer()
                .map(cs -> cs.getServerInfo().getName().equals(anchorServer.getServerInfo().getName()))
                .orElse(false);

        // anchor のサーバーへ「mover が来たら tp して」と伝える (先に登録)
        anchorServer.sendPluginMessage(CHANNEL, tpMsg);

        if (!moverOnAnchorServer) {
            // mover を anchor のサーバーへ転送 (到着時に backend が tp する)
            mover.createConnectionRequest(anchorServer).fireAndForget();
        }
    }

    /**
     * smpバックエンドへサーバー宛メッセージを事前配信する。
     * smpサーバーに誰もいない場合は届かないので false (その場合は接続後配信にフォールバック)。
     */
    private boolean preDeliverToSmp(byte[] payload) {
        if (payload.length == 0) return false;
        return proxy.getServer(smpServer)
                .map(server -> !server.getPlayersConnected().isEmpty() && server.sendPluginMessage(CHANNEL, payload))
                .orElse(false);
    }

    /** smpが無人でAH要求を処理できない場合の失敗応答を作る */
    private byte[] buildAhFailure(String reqType, UUID requester, DataInputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        switch (reqType) {
            case "AH_BUY_REQ" -> {
                String itemKey = in.readUTF();
                out.writeUTF("AH_BUY_RES");
                out.writeUTF(secret);
                out.writeUTF(requester.toString());
                out.writeUTF(itemKey);
                out.writeUTF("FAIL_ERROR");
                out.writeUTF("");
                out.writeUTF("0");
            }
            case "AH_SELL_REQ" -> {
                out.writeUTF("AH_SELL_RES");
                out.writeUTF(secret);
                out.writeUTF(requester.toString());
                out.writeUTF("FAIL_ERROR");
                out.writeUTF("");
            }
            default -> { // AH_CANCEL_REQ
                String itemKey = in.readUTF();
                out.writeUTF("AH_CANCEL_RES");
                out.writeUTF(secret);
                out.writeUTF(requester.toString());
                out.writeUTF(itemKey);
                out.writeUTF("FAIL");
                out.writeUTF("");
            }
        }
        return bytes.toByteArray();
    }

    // ─── 入退出メッセージ (ネットワーク全体) ─────────────────────────────

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        broadcast("§a[+] §f" + event.getPlayer().getUsername());
        
        Component msg = Component.text("GemMC公式Discord: ", NamedTextColor.BLUE)
                .append(Component.text("https://discord.gg/QPtetXSD2", NamedTextColor.YELLOW)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://discord.gg/QPtetXSD2")));
        event.getPlayer().sendMessage(msg);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingRtp.remove(uuid);
        pendingCmds.remove(uuid);
        tabEntries.remove(uuid);
        CompletableFuture<Void> ack = flushAcks.remove(uuid);
        if (ack != null) ack.complete(null);
        saveLastServers();
        broadcast("§c[-] §f" + event.getPlayer().getUsername());
    }

    private void broadcast(String legacyText) {
        Component comp = LEGACY.deserialize(legacyText);
        for (Player p : proxy.getAllPlayers()) {
            p.sendMessage(comp);
        }
    }

    // ─── チャット共有 ─────────────────────────────────────────────────────

    private void forwardChat(ServerConnection source, Player sender, String message) {
        String sourceName = source.getServerInfo().getName();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("CHAT");
            out.writeUTF(secret);
            out.writeUTF(sender.getUniqueId().toString());
            out.writeUTF(sender.getUsername());
            out.writeUTF(""); // タグは表示しない (プロトコル互換のため空文字を送る)
            out.writeUTF(message);
            byte[] data = bytes.toByteArray();

            for (RegisteredServer server : proxy.getAllServers()) {
                if (server.getServerInfo().getName().equalsIgnoreCase(sourceName)) continue;
                if (server.getPlayersConnected().isEmpty()) continue;
                server.sendPluginMessage(CHANNEL, data);
            }

            // --- Discord Sync ---
            if (linkedChatChannel != null) {
                // message format should just be "Username: Message" without color codes
                String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "");
                // Discordの@here/@everyone/@roleメンションを無効化（マイクラからの悪用防止）
                // ゼロ幅スペース(\u200b)を@の直後に挿入してメンションとして機能しないようにする
                cleanMessage = cleanMessage
                    .replace("@everyone", "@\u200beveryone")
                    .replace("@here", "@\u200bhere");
                // <@数字> 形式のユーザー/ロールメンションは [mention] に置換
                cleanMessage = cleanMessage.replaceAll("<@(!|&)?\\d+>", "[mention]");
                linkedChatChannel.sendMessage(sender.getUsername() + ": " + cleanMessage).queue();
            }
        } catch (IOException e) {
            logger.warn("チャット転送に失敗しました", e);
        }
    }

    // ─── タブリスト共有 ───────────────────────────────────────────────────

    private final java.util.Map<UUID, String> tabNames = new java.util.concurrent.ConcurrentHashMap<>();

    private void refreshTabLists() {
        try {
            for (Player viewer : proxy.getAllPlayers()) {
                String viewerServer = viewer.getCurrentServer()
                        .map(cs -> cs.getServerInfo().getName()).orElse(null);
                if (viewerServer == null) continue;

                Set<UUID> prev = tabEntries.getOrDefault(viewer.getUniqueId(), Set.of());
                Set<UUID> now = new HashSet<>();

                for (Player target : proxy.getAllPlayers()) {
                    if (target.getUniqueId().equals(viewer.getUniqueId())) continue;
                    String targetServer = target.getCurrentServer()
                            .map(cs -> cs.getServerInfo().getName()).orElse(null);
                    if (targetServer == null || targetServer.equalsIgnoreCase(viewerServer)) continue;

                    now.add(target.getUniqueId());
                    
                    String prefix = tabNames.getOrDefault(target.getUniqueId(), "§f");
                    int latency = (int) target.getPing();
                    String displayNameStr = prefix + target.getUsername() + " §8" + latency + "ms";
                    Component displayName = LEGACY.deserialize(displayNameStr);

                    if (!viewer.getTabList().containsEntry(target.getUniqueId())) {
                        try {
                            viewer.getTabList().addEntry(TabListEntry.builder()
                                    .tabList(viewer.getTabList())
                                    .profile(target.getGameProfile())
                                    .displayName(displayName)
                                    .latency(latency)
                                    .gameMode(0)
                                    .build());
                        } catch (Exception ignored) {}
                    } else {
                        try {
                            viewer.getTabList().getEntry(target.getUniqueId()).ifPresent(entry -> {
                                if (entry.getLatency() != latency) entry.setLatency(latency);
                                Component currentName = entry.getDisplayNameComponent().orElse(null);
                                if (currentName == null || !currentName.equals(displayName)) {
                                    entry.setDisplayName(displayName);
                                }
                            });
                        } catch (Exception ignored) {}
                    }
                }

                for (UUID old : prev) {
                    if (now.contains(old)) continue;
                    Optional<Player> target = proxy.getPlayer(old);
                    boolean sameServerNow = target.isPresent() && target.get().getCurrentServer()
                            .map(cs -> cs.getServerInfo().getName().equalsIgnoreCase(viewerServer)).orElse(false);
                    // 同一サーバーになった場合はバックエンドが本物のエントリを管理するので触らない
                    if (!sameServerNow) {
                        try {
                            viewer.getTabList().removeEntry(old);
                        } catch (Exception ignored) {}
                    }
                }

                tabEntries.put(viewer.getUniqueId(), now);
            }
        } catch (Exception e) {
            logger.warn("タブリスト更新に失敗しました", e);
        }
    }

    // ─── RTP / 接続 ──────────────────────────────────────────────────────

    private boolean isOnServer(Player player, String serverName) {
        return player.getCurrentServer()
                .map(cs -> cs.getServerInfo().getName().equalsIgnoreCase(serverName))
                .orElse(false);
    }

    private void connectPlayer(Player player, String serverName) {
        Optional<RegisteredServer> server = proxy.getServer(serverName);
        if (server.isEmpty()) {
            logger.warn("サーバー '{}' が velocity.toml に登録されていません", serverName);
            player.sendMessage(Component.text("接続先サーバーが見つかりません: " + serverName, NamedTextColor.RED));
            return;
        }
        player.createConnectionRequest(server.get()).fireAndForget();
    }

    private void sendRtpToBackend(Player player, String worldKey) {
        player.getCurrentServer().ifPresent(cs ->
                cs.sendPluginMessage(CHANNEL, buildRtpPayload(player.getUniqueId(), worldKey)));
    }

    // ─── 最終サーバー記録 (次回参加時の振り分け用) ────────────────────────

    private Path lastServersFile() {
        return dataDirectory.resolve("last-servers.properties");
    }

    private void loadLastServers() {
        try {
            Path file = lastServersFile();
            if (!Files.exists(file)) return;
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }
            for (String key : props.stringPropertyNames()) {
                try {
                    lastServers.put(UUID.fromString(key), props.getProperty(key));
                } catch (Exception ignored) {}
            }
            if (!lastServers.isEmpty()) {
                logger.info("最終サーバー記録を {} 件読み込みました。", lastServers.size());
            }
        } catch (IOException e) {
            logger.warn("最終サーバー記録の読み込みに失敗しました", e);
        }
    }

    private void saveLastServers() {
        try {
            Properties props = new Properties();
            for (Map.Entry<UUID, String> e : lastServers.entrySet()) {
                props.setProperty(e.getKey().toString(), e.getValue());
            }
            try (var out = Files.newOutputStream(lastServersFile())) {
                props.store(out, "GemSMP last server per player");
            }
        } catch (IOException e) {
            logger.warn("最終サーバー記録の保存に失敗しました", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(com.velocitypowered.api.event.proxy.ProxyShutdownEvent event) {
        if (jda != null) {
            if (infoChannel != null && !pinnedMessageId.isEmpty()) {
                try {
                    net.dv8tion.jda.api.entities.Message msg = infoChannel.retrieveMessageById(pinnedMessageId).complete();
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder()
                            .setTitle("サーバー情報")
                            .setColor(java.awt.Color.RED)
                            .addField("Status", "🔴 Offline", false)
                            .addField("Player", "0", false)
                            .setTimestamp(java.time.Instant.now());
                    msg.editMessageEmbeds(embed.build()).complete();
                } catch (Exception ignored) {}
            }
            jda.shutdown();
        }
    }

    private class DiscordListener extends net.dv8tion.jda.api.hooks.ListenerAdapter {
        @Override
        public void onReady(@org.jetbrains.annotations.NotNull net.dv8tion.jda.api.events.session.ReadyEvent event) {
            logger.info("Discord Bot is ready!");
            setupDiscordChannels();
        }

        @Override
        public void onMessageReceived(@org.jetbrains.annotations.NotNull net.dv8tion.jda.api.events.message.MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;
            if (linkedChatChannel != null && event.getChannel().getId().equals(linkedChatChannel.getId())) {
                String text = event.getMessage().getContentDisplay();

                Component msg = Component.text("[Discord] ", NamedTextColor.BLUE)
                        .append(Component.text(event.getAuthor().getName() + ": " + text, NamedTextColor.WHITE));
                for (Player p : proxy.getAllPlayers()) {
                    p.sendMessage(msg);
                }
            }
        }
    }

    private net.dv8tion.jda.api.entities.channel.concrete.TextChannel infoChannel = null;

    private void setupDiscordChannels() {
        if (jda == null) return;
        net.dv8tion.jda.api.entities.Guild guild = null;
        if (!discordGuildId.isEmpty()) {
            guild = jda.getGuildById(discordGuildId);
        }
        if (guild == null && !jda.getGuilds().isEmpty()) {
            guild = jda.getGuilds().get(0);
        }
        if (guild == null) {
            logger.warn("Discord Bot is not in any server!");
            return;
        }

        // サーバー情報 カテゴリ
        List<net.dv8tion.jda.api.entities.channel.concrete.Category> categories = guild.getCategoriesByName("サーバー情報", true);
        if (categories.isEmpty()) {
            serverInfoCategory = guild.createCategory("サーバー情報").complete();
        } else {
            serverInfoCategory = categories.get(0);
        }

        // サーバーチャット (チャット連携用)
        List<net.dv8tion.jda.api.entities.channel.concrete.TextChannel> chatTexts = guild.getTextChannelsByName("サーバーチャット", true);
        if (chatTexts.isEmpty()) {
            linkedChatChannel = guild.createTextChannel("サーバーチャット").setParent(serverInfoCategory).complete();
        } else {
            linkedChatChannel = chatTexts.get(0);
            if (linkedChatChannel.getParentCategory() == null || !linkedChatChannel.getParentCategory().getId().equals(serverInfoCategory.getId())) {
                linkedChatChannel.getManager().setParent(serverInfoCategory).queue();
            }
        }

        // サーバー情報 (パネル用)
        List<net.dv8tion.jda.api.entities.channel.concrete.TextChannel> infoTexts = guild.getTextChannelsByName("サーバー情報", true);
        if (infoTexts.isEmpty()) {
            infoChannel = guild.createTextChannel("サーバー情報").setParent(serverInfoCategory).complete();
        } else {
            infoChannel = infoTexts.get(0);
            if (infoChannel.getParentCategory() == null || !infoChannel.getParentCategory().getId().equals(serverInfoCategory.getId())) {
                infoChannel.getManager().setParent(serverInfoCategory).queue();
            }
        }

        // ピン留めパネルの検索・作成
        if (infoChannel != null) {
            infoChannel.retrievePinnedMessages().queue(messages -> {
                net.dv8tion.jda.api.entities.Message panelMsg = null;
                for (net.dv8tion.jda.api.entities.Message msg : messages) {
                    if (msg.getAuthor().getId().equals(jda.getSelfUser().getId()) && !msg.getEmbeds().isEmpty()) {
                        panelMsg = msg;
                        break;
                    }
                }

                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder()
                        .setTitle("サーバー情報")
                        .setColor(java.awt.Color.GREEN)
                        .addField("Status", "🟢 Online", false)
                        .addField("Player", String.valueOf(proxy.getPlayerCount()), false)
                        .setTimestamp(java.time.Instant.now());

                if (panelMsg == null) {
                    infoChannel.sendMessageEmbeds(embed.build()).queue(msg -> {
                        msg.pin().queue();
                        pinnedMessageId = msg.getId();
                    });
                } else {
                    pinnedMessageId = panelMsg.getId();
                    panelMsg.editMessageEmbeds(embed.build()).queue();
                }
            });
        }

        // 1分ごとにパネルを更新
        proxy.getScheduler().buildTask(this, () -> {
            if (infoChannel != null && !pinnedMessageId.isEmpty()) {
                infoChannel.retrieveMessageById(pinnedMessageId).queue(msg -> {
                    net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder()
                            .setTitle("サーバー情報")
                            .setColor(java.awt.Color.GREEN)
                            .addField("Status", "🟢 Online", false)
                            .addField("Player", String.valueOf(proxy.getPlayerCount()), false)
                            .setTimestamp(java.time.Instant.now());
                    msg.editMessageEmbeds(embed.build()).queue(null, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE));
                }, new net.dv8tion.jda.api.exceptions.ErrorHandler().ignore(net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_MESSAGE));
            }
        }).delay(1, TimeUnit.MINUTES).repeat(1, TimeUnit.MINUTES).schedule();
    }

    private void initDiscordBot() {
        if (discordBotToken == null || discordBotToken.isEmpty()) {
            logger.info("Discord Bot Token is empty. Discord integration is disabled.");
            return;
        }

        try {
            jda = net.dv8tion.jda.api.JDABuilder.createDefault(discordBotToken)
                    .enableIntents(net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT, net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordListener())
                    .build();
            logger.info("Discord Bot is starting...");
        } catch (Exception e) {
            logger.error("Failed to initialize Discord Bot", e);
        }
    }
}
