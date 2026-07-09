/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.threadedregions.scheduler.ScheduledTask
 *  net.md_5.bungee.api.ChatColor
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ClickEvent
 *  net.md_5.bungee.api.chat.ClickEvent$Action
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  net.md_5.bungee.api.chat.TextComponent
 *  net.md_5.bungee.api.chat.hover.content.Content
 *  net.md_5.bungee.api.chat.hover.content.Text
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Sound
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package com.example.smp;

import com.example.smp.SMPPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TPAManager
implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, TpaRequest> sentRequests = new ConcurrentHashMap<UUID, TpaRequest>();
    private final Map<UUID, UUID> latestIncoming = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Long> tpaCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, ScheduledTask> countdownTasks = new ConcurrentHashMap<UUID, ScheduledTask>();
    private final Map<UUID, Location> startLocations = new ConcurrentHashMap<UUID, Location>();
    private final Set<UUID> tpautoEnabled = ConcurrentHashMap.newKeySet();

    public TPAManager(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin)plugin, t -> {
            for (UUID uuid : this.tpautoEnabled) {
                Player p = Bukkit.getPlayer((UUID)uuid);
                if (p == null || !p.isOnline()) continue;
                p.sendActionBar("\u00a7aTPAUTO ON");
            }
        }, 1L, 20L);
    }

    public void handleTpauto(Player player) {
        if (this.tpautoEnabled.remove(player.getUniqueId())) {
            player.sendMessage("\u00a7atpauto\u3092\u7121\u52b9\u306b\u3057\u307e\u3057\u305f");
        } else {
            this.tpautoEnabled.add(player.getUniqueId());
            player.sendMessage("\u00a7atpauto\u3092\u6709\u52b9\u306b\u3057\u307e\u3057\u305f");
        }
    }

    public void handleTpa(Player sender, String targetName) {
        if (!this.checkCooldown(sender)) {
            return;
        }
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(sender, "tpa")) {
            return;
        }
        Player target = Bukkit.getPlayerExact((String)targetName);
        if (target == null || !target.isOnline()) {
            SMPManager mgr = ((SMPPlugin)this.plugin).getSmpManager();
            if (mgr != null && mgr.isVelocityEnabled()) {
                // \u76f8\u624b\u306f\u3053\u306e\u30b5\u30fc\u30d0\u30fc\u306b\u5c45\u306a\u3044 \u2192 Velocity\u7d4c\u7531\u3067\u5225\u30b5\u30fc\u30d0\u30fc\u3078\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308b
                mgr.getBridge().sendTpaReqToOtherServer(sender, targetName, false);
                sender.sendActionBar("\u00a7a\u00a7f" + targetName + " \u00a7a\u306b\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308a\u307e\u3057\u305f\uff01");
                return;
            }
            sender.sendActionBar("\u00a7c\u30d7\u30ec\u30a4\u30e4\u30fc \u00a7f" + targetName + " \u00a7c\u306f\u30aa\u30f3\u30e9\u30a4\u30f3\u3067\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
            return;
        }
        if (target.equals((Object)sender)) {
            sender.sendActionBar("\u00a7c\u81ea\u5206\u81ea\u8eab\u306bTP\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308c\u307e\u305b\u3093\uff01");
            return;
        }
        if (!((SMPPlugin)this.plugin).getSmpManager().isTpaEnabled(target.getUniqueId()) || ((SMPPlugin)this.plugin).getSmpManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendActionBar("\u00a7c\u305d\u306e\u30d7\u30ec\u30a4\u30e4\u30fc\u306ftpa\u3092\u8a31\u53ef\u3057\u3066\u3044\u307e\u305b\u3093\uff01");
            return;
        }
        if (this.tpautoEnabled.contains(target.getUniqueId())) {
            sender.sendActionBar("\u00a7f" + target.getName() + " \u00a7a\u306btpa\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308a\u307e\u3057\u305f\uff01");
            target.sendMessage("\u00a7atpauto\u306b\u3088\u308a\u81ea\u52d5\u3067\u8a31\u53ef\u3057\u307e\u3057\u305f");
            this.acceptRequest(target, sender, TpaType.TPA, true);
            return;
        }
        this.sendRequest(sender, target, TpaType.TPA);
    }

    public void handleTpahere(Player sender, String targetName) {
        if (!this.checkCooldown(sender)) {
            return;
        }
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(sender, "tpahere")) {
            return;
        }
        Player target = Bukkit.getPlayerExact((String)targetName);
        if (target == null || !target.isOnline()) {
            SMPManager mgr = ((SMPPlugin)this.plugin).getSmpManager();
            if (mgr != null && mgr.isVelocityEnabled()) {
                // \u76f8\u624b\u306f\u3053\u306e\u30b5\u30fc\u30d0\u30fc\u306b\u5c45\u306a\u3044 \u2192 Velocity\u7d4c\u7531\u3067\u5225\u30b5\u30fc\u30d0\u30fc\u3078\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308b
                mgr.getBridge().sendTpaReqToOtherServer(sender, targetName, true);
                sender.sendActionBar("\u00a7a\u00a7f" + targetName + " \u00a7a\u306b\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308a\u307e\u3057\u305f\uff01");
                return;
            }
            sender.sendActionBar("\u00a7c\u30d7\u30ec\u30a4\u30e4\u30fc \u00a7f" + targetName + " \u00a7c\u306f\u30aa\u30f3\u30e9\u30a4\u30f3\u3067\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
            return;
        }
        if (target.equals((Object)sender)) {
            sender.sendActionBar("\u00a7c\u81ea\u5206\u81ea\u8eab\u306bTP\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308c\u307e\u305b\u3093\uff01");
            return;
        }
        if (!((SMPPlugin)this.plugin).getSmpManager().isTpaHereEnabled(target.getUniqueId()) || ((SMPPlugin)this.plugin).getSmpManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendActionBar("\u00a7c\u305d\u306e\u30d7\u30ec\u30a4\u30e4\u30fc\u306ftpahere\u3092\u8a31\u53ef\u3057\u3066\u3044\u307e\u305b\u3093\uff01");
            return;
        }
        if (this.tpautoEnabled.contains(target.getUniqueId())) {
            sender.sendActionBar("\u00a7f" + target.getName() + " \u00a7a\u306btpahere\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308a\u307e\u3057\u305f\uff01");
            target.sendMessage("\u00a7atpauto\u306b\u3088\u308a\u81ea\u52d5\u3067\u8a31\u53ef\u3057\u307e\u3057\u305f");
            this.acceptRequest(target, sender, TpaType.TPAHERE, true);
            return;
        }
        this.sendRequest(sender, target, TpaType.TPAHERE);
    }

    public void handleTpacancel(Player sender) {
        TpaRequest req = this.sentRequests.remove(sender.getUniqueId());
        if (req != null) {
            UUID targetId;
            if (req.expiryTask != null) {
                req.expiryTask.cancel();
            }
            if (this.latestIncoming.get(targetId = req.target) != null && this.latestIncoming.get(targetId).equals(sender.getUniqueId())) {
                this.latestIncoming.remove(targetId);
            }
            sender.sendActionBar("\u00a7aTP\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u30ad\u30e3\u30f3\u30bb\u30eb\u3057\u307e\u3057\u305f\uff01");
            Player target = Bukkit.getPlayer((UUID)targetId);
            if (target != null && target.isOnline()) {
                target.sendActionBar("\u00a7e" + sender.getName() + " \u00a7c\u304cTP\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u30ad\u30e3\u30f3\u30bb\u30eb\u3057\u307e\u3057\u305f\uff01");
            }
        } else {
            sender.sendActionBar("\u00a7c\u73fe\u5728\u9001\u4fe1\u4e2d\u306eTP\u30ea\u30af\u30a8\u30b9\u30c8\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
        }
    }

    public void handleTpaccept(Player recipient, String requesterName) {
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(recipient, "tpaccept")) {
            return;
        }
        // 別サーバーからのクロスサーバーtpaリクエストがあれば先に処理する
        if (((SMPPlugin)this.plugin).getSmpManager().tryAcceptCrossTpa(recipient)) {
            return;
        }
        UUID senderUUID = null;
        if (requesterName == null) {
            senderUUID = this.latestIncoming.remove(recipient.getUniqueId());
            if (senderUUID == null) {
                recipient.sendActionBar("\u00a7c\u4fdd\u7559\u4e2d\u306eTP\u30ea\u30af\u30a8\u30b9\u30c8\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
                return;
            }
        } else {
            Player pReq = Bukkit.getPlayerExact(requesterName);
            if (pReq == null) {
                recipient.sendActionBar("\u00a7c\u30d7\u30ec\u30a4\u30e4\u30fc \u00a7f" + requesterName + " \u00a7c\u306f\u30aa\u30f3\u30e9\u30a4\u30f3\u3067\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
                return;
            }
            senderUUID = pReq.getUniqueId();
            this.latestIncoming.remove(recipient.getUniqueId(), senderUUID);
        }
        TpaRequest req = this.sentRequests.remove(senderUUID);
        if (req == null) {
            recipient.sendActionBar("\u00a7c\u30ea\u30af\u30a8\u30b9\u30c8\u306e\u6709\u52b9\u671f\u9650\u304c\u5207\u308c\u3066\u3044\u308b\u304b\u3001\u30ad\u30e3\u30f3\u30bb\u30eb\u3055\u308c\u307e\u3057\u305f\uff01");
            return;
        }
        if (req.expiryTask != null) {
            req.expiryTask.cancel();
        }
        Player requester = Bukkit.getPlayer(senderUUID);
        if (requester == null || !requester.isOnline()) {
            recipient.sendActionBar("\u00a7c\u30ea\u30af\u30a8\u30b9\u30c8\u306e\u9001\u4fe1\u8005\u304c\u30aa\u30f3\u30e9\u30a4\u30f3\u3067\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
            return;
        }
        if (((SMPPlugin)this.plugin).getSmpManager().checkCombatBlocked(requester, "tpaccept")) {
            recipient.sendActionBar("\u00a7c" + requester.getName() + " \u306f\u30b3\u30f3\u30d0\u30c3\u30c8\u4e2d\u3067\u3059\uff01");
            return;
        }
        this.acceptRequest(recipient, requester, req.type, false);
    }

    private boolean checkCooldown(Player sender) {
        long last = this.tpaCooldowns.getOrDefault(sender.getUniqueId(), 0L);
        long now = System.currentTimeMillis();
        long diff = (now - last) / 1000L;
        if (diff < 20L) {
            sender.sendActionBar("\u00a7c\u3059\u3067\u306btpa\u3092\u9001\u3063\u3066\u3044\u307e\u3059");
            return false;
        }
        return true;
    }

    private void sendRequest(Player sender, Player target, TpaType type) {
        if (this.sentRequests.containsKey(sender.getUniqueId())) {
            TpaRequest oldReq = this.sentRequests.remove(sender.getUniqueId());
            if (oldReq.expiryTask != null) {
                oldReq.expiryTask.cancel();
            }
        }
        TpaRequest req = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), type);
        req.expiryTask = Bukkit.getGlobalRegionScheduler().runDelayed((Plugin)this.plugin, t -> {
            Player s;
            this.sentRequests.remove(sender.getUniqueId());
            if (this.latestIncoming.get(target.getUniqueId()) != null && this.latestIncoming.get(target.getUniqueId()).equals(sender.getUniqueId())) {
                this.latestIncoming.remove(target.getUniqueId());
            }
            if ((s = Bukkit.getPlayer((UUID)sender.getUniqueId())) != null && s.isOnline()) {
                String typeStr = type == TpaType.TPA ? "tpa" : "tpahere";
                s.sendMessage("\u00a7c" + target.getName() + " \u3078\u306e" + typeStr + "\u306e\u6709\u52b9\u671f\u9650\u304c\u5207\u308c\u307e\u3057\u305f");
            }
        }, 400L);
        this.sentRequests.put(sender.getUniqueId(), req);
        this.latestIncoming.put(target.getUniqueId(), sender.getUniqueId());
        this.tpaCooldowns.put(sender.getUniqueId(), System.currentTimeMillis());
        String typeStr = type == TpaType.TPA ? "tpa" : "tpahere";
        sender.sendActionBar("\u00a7f" + target.getName() + " \u00a7a\u306b" + typeStr + "\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u9001\u308a\u307e\u3057\u305f\uff01");
        TextComponent notify = new TextComponent(sender.getName());
        notify.setColor(ChatColor.WHITE);
        TextComponent msg = new TextComponent(" \u304b\u3089" + typeStr + "\u30ea\u30af\u30a8\u30b9\u30c8\u304c\u5c4a\u304d\u307e\u3057\u305f\n");
        msg.setColor(ChatColor.GREEN);
        notify.addExtra((BaseComponent)msg);
        TextComponent click = new TextComponent("[\u30af\u30ea\u30c3\u30af\u3057\u3066\u627f\u8a8d]");
        click.setColor(ChatColor.GREEN);
        click.setBold(Boolean.valueOf(true));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Content[]{new Text("\u30af\u30ea\u30c3\u30af\u3057\u3066\u627f\u8a8d\u3059\u308b")}));
        notify.addExtra((BaseComponent)click);
        target.spigot().sendMessage((BaseComponent)notify);
    }

    private void acceptRequest(Player recipient, Player requester, TpaType type, boolean isAuto) {
        Player destinationPlayer;
        Player teleportingPlayer;
        if (!isAuto) {
            recipient.sendMessage("\u00a7f" + requester.getName() + " \u00a7a\u304b\u3089\u306eTP\u30ea\u30af\u30a8\u30b9\u30c8\u3092\u627f\u8a8d\u3057\u307e\u3057\u305f\uff01");
            recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
        requester.sendActionBar("\u00a7aTP\u30ea\u30af\u30a8\u30b9\u30c8\u304c\u627f\u8a8d\u3055\u308c\u307e\u3057\u305f\u30015\u79d2\u5f8c\u306b\u30c6\u30ec\u30dd\u30fc\u30c8\u3057\u307e\u3059\uff01");
        requester.playSound(requester.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        if (type == TpaType.TPA) {
            teleportingPlayer = requester;
            destinationPlayer = recipient;
        } else {
            teleportingPlayer = recipient;
            destinationPlayer = requester;
            recipient.sendActionBar("\u00a7a5\u79d2\u5f8c\u306b\u30c6\u30ec\u30dd\u30fc\u30c8\u3057\u307e\u3059\uff01");
        }
        this.startLocations.put(teleportingPlayer.getUniqueId(), teleportingPlayer.getLocation().clone());
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin)this.plugin, new Consumer<ScheduledTask>() {
            int count = 5;

            @Override
            public void accept(ScheduledTask t) {
                Player tpPlayer = Bukkit.getPlayer(teleportingPlayer.getUniqueId());
                Player destPlayer = Bukkit.getPlayer(destinationPlayer.getUniqueId());
                if (tpPlayer == null || !tpPlayer.isOnline()) {
                    TPAManager.this.cancelTpa(teleportingPlayer.getUniqueId(), null);
                    t.cancel();
                    return;
                }
                if (this.count <= 0) {
                    TPAManager.this.cancelTpa(teleportingPlayer.getUniqueId(), null);
                    if (destPlayer == null || !destPlayer.isOnline()) {
                        tpPlayer.sendActionBar("\u00a7c\u76f8\u624b\u304c\u30aa\u30f3\u30e9\u30a4\u30f3\u3067\u306f\u3042\u308a\u307e\u305b\u3093\uff01");
                        tpPlayer.playSound(tpPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                        t.cancel();
                        return;
                    }
                    tpPlayer.teleportAsync(destPlayer.getLocation());
                    tpPlayer.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false));
                    tpPlayer.sendActionBar("\u00a7f" + destPlayer.getName() + " \u00a7a\u306b\u30c6\u30ec\u30dd\u30fc\u30c8\u3057\u307e\u3057\u305f\uff01");
                    t.cancel();
                    return;
                }
                tpPlayer.sendActionBar("\u00a7e" + this.count + "\u00a7a\u79d2\u5f8c\u306b\u30c6\u30ec\u30dd\u30fc\u30c8\u3057\u307e\u3059\uff01");
                tpPlayer.playSound(tpPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.0f);
                --this.count;
            }
        }, 1L, 20L);
        this.countdownTasks.put(teleportingPlayer.getUniqueId(), task);
    }

    private void cancelTpa(UUID uuid, String reason) {
        Player p;
        ScheduledTask t = this.countdownTasks.remove(uuid);
        if (t != null) {
            try {
                t.cancel();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.startLocations.remove(uuid);
        if (reason != null && (p = Bukkit.getPlayer((UUID)uuid)) != null) {
            p.sendActionBar("\u00a7c" + reason);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        if (!this.countdownTasks.containsKey(uuid)) {
            return;
        }
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            this.cancelTpa(uuid, "\u79fb\u52d5\u3057\u305f\u305f\u3081\u30c6\u30ec\u30dd\u30fc\u30c8\u3092\u30ad\u30e3\u30f3\u30bb\u30eb\u3057\u307e\u3057\u305f\uff01");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        UUID uuid = player.getUniqueId();
        if (!this.countdownTasks.containsKey(uuid)) {
            return;
        }
        this.cancelTpa(uuid, "\u30c0\u30e1\u30fc\u30b8\u3092\u53d7\u3051\u305f\u305f\u3081\u30c6\u30ec\u30dd\u30fc\u30c8\u3092\u30ad\u30e3\u30f3\u30bb\u30eb\u3057\u307e\u3057\u305f\uff01");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.tpautoEnabled.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        this.tpautoEnabled.remove(event.getEntity().getUniqueId());
    }

    public static enum TpaType {
        TPA,
        TPAHERE;

    }

    public static class TpaRequest {
        public UUID sender;
        public UUID target;
        public TpaType type;
        public ScheduledTask expiryTask;

        public TpaRequest(UUID sender, UUID target, TpaType type) {
            this.sender = sender;
            this.target = target;
            this.type = type;
        }
    }
}

