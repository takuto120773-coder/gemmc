package com.example.smp;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TPAManager implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, TpaRequest> sentRequests = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> latestIncoming = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tpaCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> startLocations = new ConcurrentHashMap<>();
    private final Set<UUID> tpautoEnabled = ConcurrentHashMap.newKeySet();

    public TPAManager(JavaPlugin plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : tpautoEnabled) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        p.sendActionBar("§aTPAUTO ON");
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 20L);
    }

    public void handleTpauto(Player player) {
        if (this.tpautoEnabled.remove(player.getUniqueId())) {
            player.sendMessage("§atpautoを無効にしました");
        } else {
            this.tpautoEnabled.add(player.getUniqueId());
            player.sendMessage("§atpautoを有効にしました");
        }
    }

    public void handleTpa(Player sender, String targetName) {
        if (!this.checkCooldown(sender)) return;
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(sender, "tpa")) return;
        
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            SMPManager mgr = ((SMPPlugin)this.plugin).getSmpManager();
            if (mgr != null && mgr.isVelocityEnabled()) {
                mgr.getBridge().sendTpaReqToOtherServer(sender, targetName, false);
                sender.sendActionBar("§a§f" + targetName + " §aにリクエストを送りました！");
                return;
            }
            sender.sendActionBar("§cプレイヤー §f" + targetName + " §cはオンラインではありません！");
            return;
        }
        if (target.equals(sender)) {
            sender.sendActionBar("§c自分自身にTPリクエストを送れません！");
            return;
        }
        if (!((SMPPlugin)this.plugin).getSmpManager().isTpaEnabled(target.getUniqueId()) || ((SMPPlugin)this.plugin).getSmpManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendActionBar("§cそのプレイヤーはtpaを許可していません！");
            return;
        }
        if (this.tpautoEnabled.contains(target.getUniqueId())) {
            sender.sendActionBar("§f" + target.getName() + " §aにtpaリクエストを送りました！");
            target.sendMessage("§atpautoにより自動で許可しました");
            this.acceptRequest(target, sender, TpaType.TPA, true);
            return;
        }
        this.sendRequest(sender, target, TpaType.TPA);
    }

    public void handleTpahere(Player sender, String targetName) {
        if (!this.checkCooldown(sender)) return;
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(sender, "tpahere")) return;
        
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            SMPManager mgr = ((SMPPlugin)this.plugin).getSmpManager();
            if (mgr != null && mgr.isVelocityEnabled()) {
                mgr.getBridge().sendTpaReqToOtherServer(sender, targetName, true);
                sender.sendActionBar("§a§f" + targetName + " §aにリクエストを送りました！");
                return;
            }
            sender.sendActionBar("§cプレイヤー §f" + targetName + " §cはオンラインではありません！");
            return;
        }
        if (target.equals(sender)) {
            sender.sendActionBar("§c自分自身にTPリクエストを送れません！");
            return;
        }
        if (!((SMPPlugin)this.plugin).getSmpManager().isTpaHereEnabled(target.getUniqueId()) || ((SMPPlugin)this.plugin).getSmpManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendActionBar("§cそのプレイヤーはtpahereを許可していません！");
            return;
        }
        if (this.tpautoEnabled.contains(target.getUniqueId())) {
            sender.sendActionBar("§f" + target.getName() + " §aにtpahereリクエストを送りました！");
            target.sendMessage("§atpautoにより自動で許可しました");
            this.acceptRequest(target, sender, TpaType.TPAHERE, true);
            return;
        }
        this.sendRequest(sender, target, TpaType.TPAHERE);
    }

    public void handleTpacancel(Player sender) {
        TpaRequest req = this.sentRequests.remove(sender.getUniqueId());
        if (req != null) {
            UUID targetId = req.target;
            if (req.expiryTask != null) {
                req.expiryTask.cancel();
            }
            if (this.latestIncoming.get(targetId) != null && this.latestIncoming.get(targetId).equals(sender.getUniqueId())) {
                this.latestIncoming.remove(targetId);
            }
            sender.sendActionBar("§aTPリクエストをキャンセルしました！");
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                target.sendActionBar("§e" + sender.getName() + " §cがTPリクエストをキャンセルしました！");
            }
        } else {
            sender.sendActionBar("§c現在送信中のTPリクエストはありません！");
        }
    }

    public void handleTpaccept(Player recipient, String requesterName) {
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(recipient, "tpaccept")) return;
        if (((SMPPlugin)this.plugin).getSmpManager().tryAcceptCrossTpa(recipient)) return;
        
        UUID senderUUID = null;
        if (requesterName == null) {
            senderUUID = this.latestIncoming.remove(recipient.getUniqueId());
            if (senderUUID == null) {
                recipient.sendActionBar("§c保留中のTPリクエストはありません！");
                return;
            }
        } else {
            Player pReq = Bukkit.getPlayerExact(requesterName);
            if (pReq == null) {
                recipient.sendActionBar("§cプレイヤー §f" + requesterName + " §cはオンラインではありません！");
                return;
            }
            senderUUID = pReq.getUniqueId();
            this.latestIncoming.remove(recipient.getUniqueId(), senderUUID);
        }
        
        TpaRequest req = this.sentRequests.remove(senderUUID);
        if (req == null) {
            recipient.sendActionBar("§cリクエストの有効期限が切れているか、キャンセルされました！");
            return;
        }
        if (req.expiryTask != null) {
            req.expiryTask.cancel();
        }
        
        Player requester = Bukkit.getPlayer(senderUUID);
        if (requester == null || !requester.isOnline()) {
            recipient.sendActionBar("§cリクエストの送信者がオンラインではありません！");
            return;
        }
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(requester, "tpaccept")) {
            recipient.sendActionBar("§c" + requester.getName() + " はコンバット中です！");
            return;
        }
        this.acceptRequest(recipient, requester, req.type, false);
    }

    private boolean checkCooldown(Player sender) {
        long last = this.tpaCooldowns.getOrDefault(sender.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        long diff = (now - last) / 1000L;
        if (diff < 20L) {
            sender.sendActionBar("§cすでにtpaを送っています");
            return false;
        }
        return true;
    }

    private void sendRequest(Player sender, Player target, TpaType type) {
        if (this.sentRequests.containsKey(sender.getUniqueId())) {
            TpaRequest oldReq = this.sentRequests.remove(sender.getUniqueId());
            if (oldReq.expiryTask != null) oldReq.expiryTask.cancel();
        }
        
        TpaRequest req = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type);
        req.expiryTask = new BukkitRunnable() {
            @Override
            public void run() {
                sentRequests.remove(sender.getUniqueId());
                if (latestIncoming.get(target.getUniqueId()) != null && latestIncoming.get(target.getUniqueId()).equals(sender.getUniqueId())) {
                    latestIncoming.remove(target.getUniqueId());
                }
                Player s = Bukkit.getPlayer(sender.getUniqueId());
                if (s != null && s.isOnline()) {
                    String typeStr = type == TpaType.TPA ? "tpa" : "tpahere";
                    s.sendMessage("§c" + target.getName() + " への" + typeStr + "の有効期限が切れました");
                }
            }
        }.runTaskLater(plugin, 400L);
        
        this.sentRequests.put(sender.getUniqueId(), req);
        this.latestIncoming.put(target.getUniqueId(), sender.getUniqueId());
        this.tpaCooldowns.put(sender.getUniqueId(), System.currentTimeMillis());
        
        String typeStr = type == TpaType.TPA ? "tpa" : "tpahere";
        sender.sendActionBar("§f" + target.getName() + " §aに" + typeStr + "リクエストを送りました！");
        
        TextComponent notify = new TextComponent(sender.getName());
        notify.setColor(ChatColor.WHITE);
        TextComponent msg = new TextComponent(" から" + typeStr + "リクエストが届きました\n");
        msg.setColor(ChatColor.GREEN);
        notify.addExtra(msg);
        
        TextComponent click = new TextComponent("[クリックして承認]");
        click.setColor(ChatColor.GREEN);
        click.setBold(true);
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Content[]{new Text("クリックして承認する")}));
        notify.addExtra(click);
        
        target.spigot().sendMessage(notify);
    }

    private void acceptRequest(Player recipient, Player requester, TpaType type, boolean isAuto) {
        Player destinationPlayer;
        Player teleportingPlayer;
        if (!isAuto) {
            recipient.sendMessage("§f" + requester.getName() + " §aからのTPリクエストを承認しました！");
            recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
        requester.sendActionBar("§aTPリクエストが承認されました、5秒後にテレポートします！");
        requester.playSound(requester.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        
        if (type == TpaType.TPA) {
            teleportingPlayer = requester;
            destinationPlayer = recipient;
        } else {
            teleportingPlayer = recipient;
            destinationPlayer = requester;
            recipient.sendActionBar("§a5秒後にテレポートします！");
        }
        
        this.startLocations.put(teleportingPlayer.getUniqueId(), teleportingPlayer.getLocation().clone());
        BukkitTask task = new BukkitRunnable() {
            int count = 5;
            @Override
            public void run() {
                Player tpPlayer = Bukkit.getPlayer(teleportingPlayer.getUniqueId());
                Player destPlayer = Bukkit.getPlayer(destinationPlayer.getUniqueId());
                if (tpPlayer == null || !tpPlayer.isOnline()) {
                    cancelTpa(teleportingPlayer.getUniqueId(), null);
                    this.cancel();
                    return;
                }
                if (this.count <= 0) {
                    cancelTpa(teleportingPlayer.getUniqueId(), null);
                    if (destPlayer == null || !destPlayer.isOnline()) {
                        tpPlayer.sendActionBar("§c相手がオンラインではありません！");
                        tpPlayer.playSound(tpPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                        this.cancel();
                        return;
                    }
                    tpPlayer.teleportAsync(destPlayer.getLocation());
                    tpPlayer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false));
                    tpPlayer.sendActionBar("§f" + destPlayer.getName() + " §aにテレポートしました！");
                    this.cancel();
                    return;
                }
                tpPlayer.sendActionBar("§e" + this.count + "§a秒後にテレポートします！");
                tpPlayer.playSound(tpPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.0f);
                this.count--;
            }
        }.runTaskTimer(plugin, 1L, 20L);
        this.countdownTasks.put(teleportingPlayer.getUniqueId(), task);
    }

    private void cancelTpa(UUID uuid, String reason) {
        BukkitTask t = this.countdownTasks.remove(uuid);
        if (t != null) {
            try { t.cancel(); } catch (Exception ignored) {}
        }
        this.startLocations.remove(uuid);
        if (reason != null) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendActionBar("§c" + reason);
                p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        
        UUID uuid = event.getPlayer().getUniqueId();
        if (!this.countdownTasks.containsKey(uuid)) return;
        
        this.cancelTpa(uuid, "移動したためテレポートをキャンセルしました！");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        UUID uuid = event.getEntity().getUniqueId();
        if (!this.countdownTasks.containsKey(uuid)) return;
        this.cancelTpa(uuid, "ダメージを受けたためテレポートをキャンセルしました！");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.tpautoEnabled.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        this.tpautoEnabled.remove(event.getEntity().getUniqueId());
    }

    public enum TpaType { TPA, TPAHERE }

    public static class TpaRequest {
        public UUID sender;
        public UUID target;
        public TpaType type;
        public BukkitTask expiryTask;

        public TpaRequest(UUID sender, UUID target, TpaType type) {
            this.sender = sender;
            this.target = target;
            this.type = type;
        }
    }
}