package com.example.smp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SMPPlugin extends JavaPlugin {

    private SMPManager smpManager;
    private TPAManager tpaManager;
    private VelocityBridge velocityBridge;
    private DatabaseManager databaseManager;
    private boolean hubMode;

    public java.io.File getCustomDataFile() {
        return new java.io.File(getDataFolder(), "smp_data.yml");
    }

    public static java.util.List<String> networkPlayers = new java.util.ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.hubMode = isHubMode();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        velocityBridge = new VelocityBridge(this);
        smpManager = new SMPManager(this);
        velocityBridge.setManager(smpManager);
        getServer().getPluginManager().registerEvents(smpManager, this);
        getServer().getPluginManager().registerEvents(new CommandSyncListener(this), this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", (channel, player, message) -> {
            if (!channel.equals("BungeeCord")) return;
            com.google.common.io.ByteArrayDataInput in = com.google.common.io.ByteStreams.newDataInput(message);
            String subchannel = in.readUTF();
            if (subchannel.equals("PlayerList")) {
                String server = in.readUTF();
                String[] playerList = in.readUTF().split(", ");
                java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(playerList));
                list.remove("");
                networkPlayers = list;
            }
        });

        getServer().getScheduler().runTaskTimerAsynchronously(this, (task) -> {
            Player p = com.google.common.collect.Iterables.getFirst(getServer().getOnlinePlayers(), null);
            if (p != null) {
                com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                out.writeUTF("PlayerList");
                out.writeUTF("ALL");
                p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
            }
        }, 20L, 100L);

        tpaManager = new TPAManager(this);
        getServer().getPluginManager().registerEvents(tpaManager, this);

        if (!hubMode) {
            org.bukkit.World overworld = getServer().getWorld("world");
            if (overworld != null) {
                overworld.getWorldBorder().setCenter(0.0, 0.0);
                overworld.getWorldBorder().setSize(20000.0);
            }
            org.bukkit.World nether = getServer().getWorld("world_nether");
            if (nether != null) {
                nether.getWorldBorder().setCenter(0.0, 0.0);
                nether.getWorldBorder().setSize(2500.0);
            }
            org.bukkit.World end = getServer().getWorld("world_the_end");
            if (end != null) {
                end.getWorldBorder().setCenter(0.0, 0.0);
                end.getWorldBorder().setSize(20000.0);
            }
        }

        int port = getServer().getPort();
        int hubPort = getConfig().getInt("hub-port", 10000);
        int smpPort = getConfig().getInt("smp-port", 39384);
        getLogger().info("GemSMP が有効になりました。 mode: " + (hubMode ? "hub" : "smp")
                + ", port: " + port + ", hub-port: " + hubPort + ", smp-port: " + smpPort);
        if (port != hubPort && port != smpPort) {
            getLogger().warning("Invalid port configuration.");
            getLogger().warning("Please specify mode: hub or mode: smp in config.yml.");
        }

        // --- 追加: データベース同期タスクの開始 ---
        DatabaseSync dbSync = new DatabaseSync(this, smpManager);
        dbSync.startSyncTask();
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (smpManager != null) smpManager.disable();
        getLogger().info("GemSMP disabled.");
    }

    public SMPManager getSmpManager() {
        return smpManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public VelocityBridge getVelocityBridge() {
        return velocityBridge;
    }

    public boolean isHubMode() {
        int port = getServer().getPort();
        int hubPort = getConfig().getInt("hub-port", 10000);
        int smpPort = getConfig().getInt("smp-port", 39384);
        if (port == hubPort && port != smpPort) return true;
        if (port == smpPort && port != hubPort) return false;
        return getConfig().getString("mode", "smp").equalsIgnoreCase("hub");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (hubMode) {
            if (!(sender instanceof Player p)) return true;

            if (cmd.equals("sethome") || cmd.equals("back") || cmd.equals("oldhome")) {
                String full = args.length > 0 ? cmd + " " + String.join(" ", args) : cmd;
                p.sendActionBar("§aサバイバルサーバーへ移動しています...");
                smpManager.forwardCommandToSmp(p, full);
                return true;
            }

            if (cmd.equals("hub") || cmd.equals("spawn") || cmd.equals("lobby")) {
                smpManager.teleportToHub(p);
                return true;
            }
        }

        if (cmd.equals("rtp")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length > 0) {
                smpManager.handleRTPCommand(p, args[0]);
            } else {
                smpManager.handleRTP(p);
            }
            return true;
        }

        if (cmd.equals("rtpqueue") || cmd.equals("rtpq")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleRtpQueueCommand(p);
            return true;
        }
 
        if (cmd.equals("sethome")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length < 1) { p.sendActionBar("§c/sethome <1-9>"); return true; }
            try { smpManager.setNewHome(p, Integer.parseInt(args[0])); }
            catch (NumberFormatException e) { p.sendActionBar("§c/sethome <1-9>"); }
            return true;
        }

        if (cmd.equals("home")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length < 1) { 
                smpManager.openHomeGui(p);
                return true; 
            }
            try { smpManager.goNewHome(p, Integer.parseInt(args[0])); }
            catch (NumberFormatException e) { p.sendActionBar("§c/home <1-9>"); }
            return true;
        }

        if (cmd.equals("oldhome")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.openOldHomeGui(p);
            return true;
        }
 
        if (cmd.equals("tpa")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length < 1) { p.sendActionBar("§c/tpa <player>"); return true; }
            tpaManager.handleTpa(p, args[0]);
            return true;
        }

        if (cmd.equals("tpacancel")) {
            if (!(sender instanceof Player p)) return true;
            tpaManager.handleTpacancel(p);
            return true;
        }
 
        if (cmd.equals("tpaccept")) {
            if (!(sender instanceof Player p)) return true;
            String name = args.length > 0 ? args[0] : null;
            tpaManager.handleTpaccept(p, name);
            return true;
        }

        if (cmd.equals("tpauto")) {
            if (!(sender instanceof Player p)) return true;
            tpaManager.handleTpauto(p);
            return true;
        }
 
        if (cmd.equals("sell")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleSell(p);
            return true;
        }

        if (cmd.equals("sellmulti")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.openSellMultiGui(p);
            return true;
        }
 
        if (cmd.equals("ah")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length > 0 && args[0].equalsIgnoreCase("sell")) {
                if (args.length < 2) {
                    p.sendActionBar("§c/ah sell <価格>");
                } else {
                    smpManager.handleAhSell(p, args[1]);
                }
            } else if (args.length > 0) {
                String query = String.join(" ", args);
                smpManager.handleAhCommandSearch(p, query);
            } else {
                smpManager.openAhGui(p);
            }
            return true;
        }

        if (cmd.equals("team")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleTeamCommand(p, args);
            return true;
        }

        if (cmd.equals("order")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleOrderCommand(p, args);
            return true;
        }
        if (cmd.equals("/admin")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleAdminCommand(p, args);
            return true;
        }

        if (cmd.equals("tp") || cmd.equals("gamemode") || cmd.equals("ban") ||
            cmd.equals("unban") || cmd.equals("ban-ip") || cmd.equals("unban-ip") ||
            cmd.equals("ec") || cmd.equals("enderchest") ||
            cmd.equals("overpos1") || cmd.equals("overpos2") ||
            cmd.equals("netherpos1") || cmd.equals("netherpos2") ||
            cmd.equals("endpos1") || cmd.equals("endpos2") ||
            cmd.equals("afpos1") || cmd.equals("afpos2") ||
            cmd.equals("overposremove") || cmd.equals("netherposremove") || cmd.equals("endposremove") || cmd.equals("afposremove") ||
            cmd.equals("endopen") || cmd.equals("sp") ||
            cmd.equals("money") || cmd.equals("shard") || cmd.equals("op")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleOpCommand(p, cmd, args);
            return true;
        }

        if (cmd.equals("settings")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.openSettingsGui(p);
            return true;
        }

        if (cmd.equals("bounty")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleBountyCommand(p, args);
            return true;
        }

        if (cmd.equals("ignore")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleIgnoreCommand(p, args);
            return true;
        }

        if (cmd.equals("/")) {
            return true;
        }

        if (cmd.equals("worth")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length > 0) {
                String query = String.join(" ", args);
                smpManager.openSellListGui(p, 1, query);
            } else {
                smpManager.openSellListGui(p, 1, null);
            }
            return true;
        }

        if (cmd.equals("pay")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length < 2) {
                p.sendActionBar("§c/pay <player> <amount>");
                return true;
            }
            smpManager.handlePayCommand(p, args[0], args[1]);
            return true;
        }

        if (cmd.equals("shop")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.openShopMenu(p);
            return true;
        }

        if (cmd.equals("tpahere")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length < 1) { p.sendActionBar("§c/tpahere <プレイヤー名>"); return true; }
            tpaManager.handleTpahere(p, args[0]);
            return true;
        }

        if (cmd.equals("baltop")) {
            smpManager.handleBaltopCommand(sender);
            return true;
        }
        
        if (cmd.equals("nv")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.handleNvCommand(p);
            return true;
        }

        if (cmd.equals("hub") || cmd.equals("spawn") || cmd.equals("lobby")) {
            if (!(sender instanceof Player p)) return true;
            smpManager.teleportToHub(p);
            return true;
        }

        if (cmd.equals("stats")) {
            if (!(sender instanceof Player p)) return true;
            if (args.length > 0) {
                org.bukkit.OfflinePlayer target = getServer().getOfflinePlayer(args[0]);
                smpManager.openStatsGui(p, target);
            } else {
                smpManager.openStatsGui(p, p);
            }
            return true;
        }

        if (cmd.equals("deaths")) {
            if (!(sender instanceof Player p)) return true;
            if (!p.isOp()) {
                p.sendMessage("§cYou don't have permission.");
                return true;
            }
            smpManager.handleDeathsCommand(p);
            return true;
        }

        if (cmd.equals("afkpos1")) {
            if (!(sender instanceof Player p)) return true;
            if (!p.isOp()) { p.sendMessage("§cYou don't have permission."); return true; }
            smpManager.setAfkPos1(p);
            return true;
        }

        if (cmd.equals("afkpos2")) {
            if (!(sender instanceof Player p)) return true;
            if (!p.isOp()) { p.sendMessage("§cYou don't have permission."); return true; }
            smpManager.setAfkPos2(p);
            return true;
        }

        return false;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("home") || cmd.equals("sethome")) {
            if (args.length == 1) {
                return java.util.Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
            }
        }
        if (cmd.equals("rtp")) {
            if (args.length == 1) {
                return java.util.Arrays.asList("overworld", "nether", "end");
            }
        }
        if (cmd.equals("tpa") || cmd.equals("tpahere") || cmd.equals("bounty") || cmd.equals("ignore") || cmd.equals("tp")) {
            if (args.length == 1) {
                java.util.List<String> players = new java.util.ArrayList<>(networkPlayers);
                if (players.isEmpty()) {
                    for (Player p : getServer().getOnlinePlayers()) {
                        players.add(p.getName());
                    }
                }
                players.remove(sender.getName());
                players.removeIf(name -> !name.toLowerCase().startsWith(args[0].toLowerCase()));
                return players;
            }
            if (cmd.equals("bounty") && args.length == 2) {
                return java.util.Arrays.asList("add");
            }
        }
        if (cmd.equals("ah")) {
            if (args.length == 1) {
                return java.util.Arrays.asList("sell");
            }
        }
        if (cmd.equals("team")) {
            if (args.length == 1) {
                return java.util.Arrays.asList("create", "join", "leave", "home", "kick", "invite", "sethome");
            }
            if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("kick") || sub.equals("invite")) {
                    java.util.List<String> players = new java.util.ArrayList<>(networkPlayers);
                    if (players.isEmpty()) {
                        for (Player p : getServer().getOnlinePlayers()) {
                            players.add(p.getName());
                        }
                    }
                    players.remove(sender.getName());
                    players.removeIf(name -> !name.toLowerCase().startsWith(args[1].toLowerCase()));
                    return players;
                }
            }
        }
        if (cmd.equals("order")) {
            if (args.length == 1) {
                java.util.List<String> items = new java.util.ArrayList<>();
                for (org.bukkit.Material m : org.bukkit.Material.values()) {
                    if (!m.isAir() && !m.name().contains("LEGACY_")) {
                        items.add(m.name().toLowerCase());
                    }
                }
                return items;
            }
        }

        if (cmd.equals("pay")) {
            if (args.length == 1) {
                java.util.List<String> players = new java.util.ArrayList<>(networkPlayers);
                if (players.isEmpty()) {
                    for (Player p : getServer().getOnlinePlayers()) {
                        players.add(p.getName());
                    }
                }
                players.remove(sender.getName());
                players.removeIf(name -> !name.toLowerCase().startsWith(args[0].toLowerCase()));
                return players;
            }
        }
        return null;
    }
}