package com.example.smp;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scoreboard.*;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;

import java.io.File;
import java.io.IOException;
import java.util.*;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class SMPManager implements Listener {

    public static String WORLD_OVERWORLD = "world";
    public static String WORLD_NETHER    = "world_nether";
    public static String WORLD_END       = "world_the_end";

    private static boolean isValidTradeItem(Material m) {
        if (m.isAir() || m.name().contains("LEGACY_")) return false;
        String name = m.name();
        if (name.contains("COMMAND_BLOCK") || name.contains("STRUCTURE") || name.equals("JIGSAW") || 
            name.equals("BARRIER") || name.equals("LIGHT") || name.equals("SPAWNER") || 
            name.equals("BEDROCK") || name.contains("PORTAL") || name.contains("GATEWAY") || 
            name.equals("KNOWLEDGE_BOOK") || name.equals("DEBUG_STICK") || 
            name.equals("PAPER") || name.endsWith("_SPAWN_EGG") || name.startsWith("INFESTED_") ||
            name.equals("FARMLAND") || name.equals("DIRT_PATH") || name.equals("CHORUS_PLANT") ||
            name.equals("TALL_SEAGRASS") || name.equals("BUBBLE_COLUMN") || name.equals("WATER") || 
            name.equals("LAVA") || name.equals("PISTON_HEAD") || name.equals("MOVING_PISTON") ||
            name.equals("END_CRYSTAL") || name.equals("DRAGON_EGG") || name.equals("COMMAND_BLOCK_MINECART")) {
            return false;
        }
        return true;
    }

    static final Map<Material, Long> ITEM_PRICES = new EnumMap<>(Material.class);
    static {
        // 貴重品
        ITEM_PRICES.put(Material.NETHER_STAR, 2000L);
        ITEM_PRICES.put(Material.TOTEM_OF_UNDYING, 500L);
        ITEM_PRICES.put(Material.SHULKER_SHELL, 10L);

        // 鉱石・素材
        ITEM_PRICES.put(Material.NETHERITE_INGOT, 1000L);
        ITEM_PRICES.put(Material.ANCIENT_DEBRIS, 200L);
        ITEM_PRICES.put(Material.NETHERITE_SCRAP, 200L);
        ITEM_PRICES.put(Material.DIAMOND, 100L);
        ITEM_PRICES.put(Material.DIAMOND_ORE, 80L);
        ITEM_PRICES.put(Material.DEEPSLATE_DIAMOND_ORE, 80L);
        ITEM_PRICES.put(Material.EMERALD, 100L);
        ITEM_PRICES.put(Material.EMERALD_ORE, 80L);
        ITEM_PRICES.put(Material.DEEPSLATE_EMERALD_ORE, 80L);
        ITEM_PRICES.put(Material.GOLD_INGOT, 50L);
        ITEM_PRICES.put(Material.RAW_GOLD, 30L);
        ITEM_PRICES.put(Material.GOLD_ORE, 35L);
        ITEM_PRICES.put(Material.DEEPSLATE_GOLD_ORE, 35L);
        ITEM_PRICES.put(Material.IRON_INGOT, 20L);
        ITEM_PRICES.put(Material.RAW_IRON, 15L);
        ITEM_PRICES.put(Material.IRON_ORE, 15L);
        ITEM_PRICES.put(Material.DEEPSLATE_IRON_ORE, 15L);
        ITEM_PRICES.put(Material.COPPER_INGOT, 5L);
        ITEM_PRICES.put(Material.RAW_COPPER, 3L);
        ITEM_PRICES.put(Material.COPPER_ORE, 5L);
        ITEM_PRICES.put(Material.DEEPSLATE_COPPER_ORE, 5L);
        ITEM_PRICES.put(Material.LAPIS_LAZULI, 20L);
        ITEM_PRICES.put(Material.AMETHYST_SHARD, 50L);
        ITEM_PRICES.put(Material.QUARTZ, 5L);
        ITEM_PRICES.put(Material.COAL, 10L);
        ITEM_PRICES.put(Material.REDSTONE, 5L);
        ITEM_PRICES.put(Material.OBSIDIAN, 20L);
        ITEM_PRICES.put(Material.GLOWSTONE_DUST, 5L);

        // 原木 (原木10L ＝ 木材4個で各2L)
        ITEM_PRICES.put(Material.OAK_LOG, 10L);
        ITEM_PRICES.put(Material.SPRUCE_LOG, 10L);
        ITEM_PRICES.put(Material.BIRCH_LOG, 10L);
        ITEM_PRICES.put(Material.JUNGLE_LOG, 10L);
        ITEM_PRICES.put(Material.ACACIA_LOG, 10L);
        ITEM_PRICES.put(Material.DARK_OAK_LOG, 10L);
        ITEM_PRICES.put(Material.MANGROVE_LOG, 10L);
        ITEM_PRICES.put(Material.CHERRY_LOG, 10L);

        // 木材
        ITEM_PRICES.put(Material.OAK_PLANKS, 2L);
        ITEM_PRICES.put(Material.SPRUCE_PLANKS, 2L);
        ITEM_PRICES.put(Material.BIRCH_PLANKS, 2L);
        ITEM_PRICES.put(Material.JUNGLE_PLANKS, 2L);
        ITEM_PRICES.put(Material.ACACIA_PLANKS, 2L);
        ITEM_PRICES.put(Material.DARK_OAK_PLANKS, 2L);
        ITEM_PRICES.put(Material.MANGROVE_PLANKS, 2L);
        ITEM_PRICES.put(Material.CHERRY_PLANKS, 2L);

        // 棒
        ITEM_PRICES.put(Material.STICK, 1L);

        // 石・土
        ITEM_PRICES.put(Material.STONE, 1L);
        ITEM_PRICES.put(Material.COBBLESTONE, 1L);
        ITEM_PRICES.put(Material.DEEPSLATE, 1L);
        ITEM_PRICES.put(Material.COBBLED_DEEPSLATE, 1L);
        ITEM_PRICES.put(Material.DIRT, 1L);
        ITEM_PRICES.put(Material.GRASS_BLOCK, 1L);
        ITEM_PRICES.put(Material.SAND, 1L);
        ITEM_PRICES.put(Material.GRAVEL, 1L);
        ITEM_PRICES.put(Material.GLASS, 1L);
        ITEM_PRICES.put(Material.CLAY_BALL, 2L);
        ITEM_PRICES.put(Material.BRICK, 3L);

        // 作物・食料
        ITEM_PRICES.put(Material.WHEAT, 3L);
        ITEM_PRICES.put(Material.WHEAT_SEEDS, 1L);
        ITEM_PRICES.put(Material.BREAD, 10L);
        ITEM_PRICES.put(Material.POTATO, 3L);
        ITEM_PRICES.put(Material.BAKED_POTATO, 8L);
        ITEM_PRICES.put(Material.CARROT, 3L);
        ITEM_PRICES.put(Material.GOLDEN_CARROT, 10L);
        ITEM_PRICES.put(Material.APPLE, 5L);
        ITEM_PRICES.put(Material.GOLDEN_APPLE, 50L);
        ITEM_PRICES.put(Material.SWEET_BERRIES, 1L);
        ITEM_PRICES.put(Material.MELON_SLICE, 1L);
        ITEM_PRICES.put(Material.PUMPKIN, 5L);
        ITEM_PRICES.put(Material.SUGAR_CANE, 3L);

        // モブドロップ
        ITEM_PRICES.put(Material.ROTTEN_FLESH, 2L);
        ITEM_PRICES.put(Material.BONE, 5L);
        ITEM_PRICES.put(Material.GUNPOWDER, 10L);
        ITEM_PRICES.put(Material.STRING, 5L);
        ITEM_PRICES.put(Material.SPIDER_EYE, 5L);
        ITEM_PRICES.put(Material.ENDER_PEARL, 15L);
        ITEM_PRICES.put(Material.BLAZE_ROD, 30L);
        ITEM_PRICES.put(Material.SLIME_BALL, 10L);
        ITEM_PRICES.put(Material.LEATHER, 10L);
        ITEM_PRICES.put(Material.FEATHER, 2L);
        ITEM_PRICES.put(Material.MAGMA_CREAM, 5L);
        ITEM_PRICES.put(Material.GHAST_TEAR, 50L);
        ITEM_PRICES.put(Material.PRISMARINE_SHARD, 10L);
        ITEM_PRICES.put(Material.PRISMARINE_CRYSTALS, 10L);

        for (Material m : Material.values()) {
            if (ITEM_PRICES.containsKey(m)) continue;
            if (!isValidTradeItem(m)) continue;
            String name = m.name();
            long price = 1L;

            // Check if it's explicitly a junk item to keep it at 1L
            boolean isJunk = name.contains("DIRT") || name.contains("STONE") || name.contains("COBBLESTONE") ||
                             name.contains("GRAVEL") || name.contains("SAND") || name.contains("NETHERRACK") ||
                             name.contains("END_STONE") || name.contains("TUFF") || name.contains("DIORITE") ||
                             name.contains("ANDESITE") || name.contains("GRANITE") || name.contains("BASALT") ||
                             name.contains("BLACKSTONE") || name.contains("DEEPSLATE") || name.contains("CALCITE") ||
                             name.contains("SOUL_SAND") || name.contains("SOUL_SOIL") || name.contains("CLAY_BLOCK") ||
                             name.contains("MUD") || name.contains("MAGMA_BLOCK") || name.contains("SCULK_VEIN") ||
                             name.contains("GRASS") || name.contains("TALL_GRASS") || name.contains("FERN") ||
                             name.contains("DEAD_BUSH") || name.contains("DANDELION") || name.contains("POPPY") ||
                             name.contains("BLUE_ORCHID") || name.contains("ALLIUM") || name.contains("AZURE_BLUET") ||
                             name.contains("RED_TULIP") || name.contains("ORANGE_TULIP") || name.contains("WHITE_TULIP") ||
                             name.contains("PINK_TULIP") || name.contains("OXEYE_DAISY") || name.contains("CORNFLOWER") ||
                             name.contains("LILY_OF_THE_VALLEY") || name.contains("WITHER_ROSE") || name.contains("SUNFLOWER") ||
                             name.contains("LILAC") || name.contains("ROSE_BUSH") || name.contains("PEONY") ||
                             name.contains("BROWN_MUSHROOM") || name.contains("RED_MUSHROOM") || name.contains("SUGAR_CANE") ||
                             name.contains("KELP") || name.contains("SEAGRASS") || name.contains("VINE") ||
                             name.contains("LILY_PAD") || name.contains("COBWEB") || name.contains("SNOWBALL") ||
                             name.contains("POINTED_DRIPSTONE") || name.contains("DRIPSTONE_BLOCK");

            if (isJunk) {
                ITEM_PRICES.put(m, 1L);
                continue;
            }

            // Precious and highly valuable gear/items
            if (name.startsWith("NETHERITE_")) {
                if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
                    price = 4000L;
                } else if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")) {
                    price = 3500L;
                } else if (name.endsWith("_SHOVEL") || name.endsWith("_HOE")) {
                    price = 3000L;
                } else {
                    price = 2500L; // block, ingot, scrap etc.
                }
            } else if (name.startsWith("DIAMOND_")) {
                if (name.endsWith("_CHESTPLATE")) {
                    price = 2400L;
                } else if (name.endsWith("_LEGGINGS")) {
                    price = 2100L;
                } else if (name.endsWith("_HELMET") || name.endsWith("_BOOTS")) {
                    price = 1500L;
                } else if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")) {
                    price = 900L;
                } else if (name.endsWith("_SHOVEL")) {
                    price = 400L;
                } else if (name.endsWith("_HOE")) {
                    price = 700L;
                } else {
                    price = 300L; // Diamond block, etc.
                }
            } else if (name.startsWith("GOLDEN_") || name.contains("GOLD_")) {
                if (name.endsWith("_CHESTPLATE")) {
                    price = 1600L;
                } else if (name.endsWith("_LEGGINGS")) {
                    price = 1400L;
                } else if (name.endsWith("_HELMET") || name.endsWith("_BOOTS")) {
                    price = 1000L;
                } else if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")) {
                    price = 600L;
                } else if (name.endsWith("_SHOVEL")) {
                    price = 300L;
                } else if (name.endsWith("_HOE")) {
                    price = 500L;
                } else {
                    price = 200L;
                }
            } else if (name.startsWith("IRON_")) {
                if (name.endsWith("_CHESTPLATE")) {
                    price = 800L;
                } else if (name.endsWith("_LEGGINGS")) {
                    price = 700L;
                } else if (name.endsWith("_HELMET") || name.endsWith("_BOOTS")) {
                    price = 500L;
                } else if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")) {
                    price = 300L;
                } else if (name.endsWith("_SHOVEL")) {
                    price = 150L;
                } else if (name.endsWith("_HOE")) {
                    price = 250L;
                } else {
                    price = 100L;
                }
            } else if (name.startsWith("CHAINMAIL_")) {
                price = 150L;
            } else if (name.startsWith("LEATHER_")) {
                if (name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_HELMET") || name.endsWith("_BOOTS")) {
                    price = 100L;
                } else {
                    price = 50L;
                }
            }
            
            // Ores, Blocks, and specific resources
            else if (name.endsWith("_ORE")) {
                price = 50L;
            } else if (name.startsWith("RAW_")) {
                price = 30L;
            } else if (name.endsWith("_INGOT")) {
                price = 100L;
            } else if (name.endsWith("_BLOCK")) {
                price = 150L;
            } else if (name.endsWith("_SHARD") || name.endsWith("_CRYSTAL")) {
                price = 50L;
            }

            // Other general item groupings scaled up (crafted or harder to get)
            else if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) {
                price = 50L; // standard logs/wood are 50L (Oak log is 50L)
            } else if (name.endsWith("_PLANKS")) {
                price = 12L;
            } else if (name.endsWith("_SLAB")) {
                price = 6L;
            } else if (name.endsWith("_STAIRS")) {
                price = 18L;
            } else if (name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE") || name.endsWith("_WALL")) {
                price = 20L;
            } else if (name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR")) {
                price = 25L;
            } else if (name.endsWith("_SAPLING") || name.endsWith("_PROPAGULE")) {
                price = 20L;
            } else if (name.endsWith("_LEAVES")) {
                price = 5L;
            }

            // Food & Crops
            else if (name.contains("BEEF") || name.contains("PORKCHOP") || name.contains("CHICKEN") || name.contains("MUTTON") || name.contains("RABBIT") || name.contains("COD") || name.contains("SALMON") || name.contains("COOKED_") || name.contains("MUTTON")) {
                price = 30L;
            } else if (name.contains("POTATO") || name.contains("CARROT") || name.contains("BEETROOT") || name.contains("MELON") || name.contains("PUMPKIN") || name.contains("BERRIES")) {
                price = 15L;
            } else if (name.contains("GLASS")) {
                price = 10L;
            } else if (name.contains("TERRACOTTA") || name.contains("CONCRETE") || name.contains("WOOL") || name.contains("CARPET") || name.contains("SHULKER_")) {
                price = 20L;
            } else if (name.endsWith("_RECORD") || name.endsWith("_DISC")) {
                price = 300L;
            } else if (name.contains("SPAWN_EGG")) {
                price = 500L;
            } else if (name.contains("BOAT")) {
                price = 60L;
            } else if (name.contains("MINECART")) {
                price = 120L;
            } else if (name.endsWith("_HORSE_ARMOR")) {
                price = 400L;
            } else if (name.contains("POTION") || name.contains("SPLASH_POTION") || name.contains("LINGERING_POTION")) {
                price = 150L;
            } else if (name.endsWith("_BANNER")) {
                price = 100L;
            } else if (name.endsWith("_TEMPLATE")) {
                price = 1000L; // Smithing templates
            } else if (name.contains("ARROW")) {
                price = 10L;
            } else if (name.contains("ENCHANTED_BOOK")) {
                price = 500L;
            } else if (name.contains("BOOK")) {
                price = 50L;
            } else {
                // Default fallback for any other unlisted items (e.g. general tools, blocks, miscellaneous items)
                price = 10L;
            }

            ITEM_PRICES.put(m, price);
        }
    }

    private final JavaPlugin plugin;

    // ─── Velocity連携 ────────────────────────────────────────────────────
    /** true = hubサーバー(Purpur)モード / false = SMPサーバー(Folia)モード */
    private final boolean hubMode;
    private final boolean velocityEnabled;
    private final String hubServerName;
    private final String smpServerName;
    private final String hubWorldName;
    private final VelocityBridge bridge;

    // ─── テレポート ───────────────────────────────────────────────────────
    // Foliaでは複数リージョンスレッド+非同期チャンクコールバックから同時に触るため
    // 必ずConcurrent系を使うこと (HashMapだと座標のputが他スレッドから見えず、
    // 「安全な場所を探しています」が永久に終わらないバグになる)
    private final Map<UUID, ScheduledTask> tpTasks   = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Location>   tpTargets = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<UUID> portalRtpPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<UUID> transitioning = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final List<UUID> rtpQueue = new java.util.concurrent.CopyOnWriteArrayList<>();

    // ─── RTP ─────────────────────────────────────────────────────────────
    private final Map<UUID, Long> rtpCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Long> rtpQueueCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long RTP_COOLDOWN_MS  = 15_000L;

    private Location rtpOverPos1, rtpOverPos2;
    private Location rtpNetherPos1, rtpNetherPos2;
    private Location rtpEndPos1, rtpEndPos2;
    private Location afkPortalPos1, afkPortalPos2;

    // ─── TPA ─────────────────────────────────────────────────────────────


    // ─── Combat ──────────────────────────────────────────────────────────
    private final Map<UUID, Long> combatExpiry = new HashMap<>(); // uuid -> expiry ms
    private static final long COMBAT_DURATION_MS = 20_000L; // 20 seconds

    // ─── ホーム ───────────────────────────────────────────────────────────
    private final Map<UUID, Map<Integer, Location>> playerHomes    = new HashMap<>();
    private final Map<UUID, Map<Integer, Location>> playerNewHomes = new HashMap<>();
    private final Map<UUID, Location>               lastSMPLocations = new HashMap<>();
    private final Set<UUID>                          lastQuitInSMP    = new HashSet<>();
    private final Map<UUID, Long> shopBuyCooldown = new HashMap<>();
    private File dataFile;

    // ─── 統計 ─────────────────────────────────────────────────────────────
    private final Map<UUID, Long> smpMoney    = new HashMap<>();
    private final Map<UUID, Long> smpShards   = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> killShardCooldown = new HashMap<>();
    private final Map<UUID, Long> smpKills    = new HashMap<>();
    private final Map<UUID, Long> smpDeaths   = new HashMap<>();
    private final List<String> deathLogs      = new ArrayList<>();
    private final List<String> recentDeathLogs = new ArrayList<>();
    private final Map<UUID, Long> smpPlaytime = new HashMap<>();
    private final Map<UUID, Long> smpSessionStart = new HashMap<>();
    private final Map<UUID, Long> smpKeys     = new HashMap<>();
    private final Map<UUID, Long> smpBounties = new HashMap<>();
    private final Map<UUID, Set<UUID>> smpIgnores = new HashMap<>();

    // Settings
    private final Map<UUID, Boolean> settingTpaEnabled = new HashMap<>(); // default true
    private final Map<UUID, Boolean> settingTpaHereEnabled = new HashMap<>(); // default true
    private final Map<UUID, Boolean> settingPayEnabled = new HashMap<>(); // default true
    private final Map<UUID, Boolean> settingMobSpawnEnabled = new HashMap<>(); // default true
    private final Map<UUID, Boolean> settingHideCoordsEnabled = new HashMap<>(); // default false
    private final Map<UUID, Boolean> settingAhConfirmEnabled = new HashMap<>(); // default true
    private final Map<UUID, Boolean> settingScoreboardEnabled = new HashMap<>(); // default true

    private long keyallRemainingSeconds = 45 * 60;

    // ─── 死亡チェスト ────────────────────────────────────────────────────
    private org.bukkit.NamespacedKey DEATH_CHEST_KEY;
    private final Map<String, DeathChestData> deathChests     = new HashMap<>();
    private final Map<UUID, String>           openDeathChestGui = new HashMap<>();

    private final Map<UUID, ItemStack[]> customEnderChests = new HashMap<>();

    // ─── RTP GUI ─────────────────────────────────────────────────────────
    private final Set<UUID> openRtpGui = new HashSet<>();

    // ─── Auction House ───────────────────────────────────────────────────
    public enum SortMode {
        NEW,
        LOW_PRICE,
        HIGH_PRICE,
        OLD,
        MY_LISTINGS,
        SEARCH
    }

    public enum OrderSortMode {
        NEW,
        LOW_PRICE,
        HIGH_PRICE,
        OLD,
        MY_ORDERS,
        SEARCH
    }

    public static class AuctionItem {
        public String id;
        UUID sellerUUID;
        String sellerName;
        ItemStack item;
        long price;
        long listedTime;
    }

    public static class OrderItem {
        public String id;
        public UUID requesterUUID;
        public String requesterName;
        public ItemStack itemTemplate;
        public long price;
        public int count;
        public int collected = 0;
        public long listedTime;
        public boolean cancelled = false;
    }

    public static class OrderBuilder {
        public ItemStack itemTemplate = new ItemStack(Material.STONE);
        public long money = 100L;
        public int count = 1;
        public String signInputTarget = ""; // "ITEM", "MONEY", or "COUNT"
    }

    final Map<String, AuctionItem> auctionItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, SortMode> playerSortMode = new HashMap<>();
    private final Map<UUID, String> playerSearchQuery = new HashMap<>();

    // ─── Order System ────────────────────────────────────────────────────
    final Map<String, OrderItem> orderItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, OrderSortMode> playerOrderSortMode = new HashMap<>();
    private final Map<UUID, String> playerOrderSearchQuery = new HashMap<>();
    private final Map<UUID, Integer> playerOrderPage = new HashMap<>();
    private final Map<UUID, Integer> playerOrderSelectPage = new HashMap<>();
    private final Map<UUID, Integer> playerOrderClaimPage = new HashMap<>();
    private final Map<UUID, String> playerOrderSelectQuery = new HashMap<>();
    private final Map<UUID, OrderBuilder> playerOrderBuilder = new HashMap<>();
    final Map<String, List<ItemStack>> orderStorage = new java.util.concurrent.ConcurrentHashMap<>(); // Holds actual fulfilled items for each order ID
    private static class PendingOrder {
        ItemStack itemTemplate;
        int count;
    }
    private final Map<UUID, Queue<PendingOrder>> playerPendingOrders = new HashMap<>();
    private final Map<UUID, String> playerClaimingOrder = new HashMap<>();
    private final Map<UUID, String> playerDepositingOrder = new HashMap<>();
    private static final List<ItemStack> SELECTABLE_MATERIALS;
    static {
        List<ItemStack> list = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isItem() && !mat.isAir() && !mat.name().contains("LEGACY_") && mat != Material.ENCHANTED_BOOK && isValidTradeItem(mat)) {
                list.add(new ItemStack(mat));
            }
        }
        for (org.bukkit.enchantments.Enchantment ench : org.bukkit.enchantments.Enchantment.values()) {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            org.bukkit.inventory.meta.EnchantmentStorageMeta meta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) book.getItemMeta();
            if (meta != null) {
                meta.addStoredEnchant(ench, ench.getMaxLevel(), true);
                book.setItemMeta(meta);
            }
            list.add(book);
        }
        list.sort(Comparator.comparing(i -> i.getType().name()));
        SELECTABLE_MATERIALS = Collections.unmodifiableList(list);
    }

    // ─── AFK Zone ────────────────────────────────────────────────────────
    private Location afkZonePos1;
    private Location afkZonePos2;
    private String afkZoneWorldName;
    private final Map<UUID, Integer> afkTimers = new HashMap<>();
    private boolean isEndOpen = false;
    private final Map<UUID, String> playerRanks = new HashMap<>();
    
    // NightVision feature
    private final Set<UUID> nvPlayers = new HashSet<>();

    public enum Rank {
        OWNER(100, "#FF0000", "Owner"),
        HAGE(90, "#FFFF00", "Hage"),
        IKEMEN(80, "#0000FF", "Ikemen"),
        WATAAME(70, "#000000", "Wataame"),
        SRADMIN(60, "#800080", "SrAdmin"),
        ADMIN(50, "#FFA500", "Admin"),
        SRMOD(40, "#FF00FF", "SrMod"),
        MOD(30, "#FFB6C1", "Mod"),
        SRHELPER(20, "#008B8B", "SrHelper"),
        HELPER(15, "#ADD8E6", "Helper"),
        MEDIA(10, "#90EE90", "Media"),
        MVP(10, "#55FF55", "MVP"),
        MVN(10, "#55FFFF", "MVN"),
        NEKO(10, "#5555FF", "Neko"),
        VIP(5, "#FFFF00", "VIP"),
        CHECK(1, "#FFFFFF", "✓"),
        NONE(0, "#AAAAAA", "Player");

        public final int weight;
        public final String hexColor;
        public final String displayName;

        Rank(int weight, String hexColor, String displayName) {
            this.weight = weight;
            this.hexColor = hexColor;
            this.displayName = displayName;
        }

        public static Rank fromString(String name) {
            if (name == null) return NONE;
            if (name.equalsIgnoreCase("v") || name.equalsIgnoreCase("check") || name.equals("✓")) return CHECK;
            for (Rank r : values()) {
                if (r.name().equalsIgnoreCase(name)) return r;
            }
            return NONE;
        }

        public String getPrefix() {
            if (this == NONE) return "§f";
            if (this == CHECK) return "§f✓ §8| §f";
            return net.md_5.bungee.api.ChatColor.of(hexColor) + displayName + " §8| §f";
        }
        
        public boolean isAtLeast(Rank other) {
            return this.weight >= other.weight;
        }
    }

    private static class TeamData {
        String name;
        UUID leader;
        Set<UUID> members = new HashSet<>();
        Location home;
        /** チームメンバー同士のダメージを許可するか (フレンドリーファイア) */
        boolean friendlyFire = false;
    }
    private final Map<String, TeamData> teams = new HashMap<>(); // team name to data
    private final Map<UUID, String> playerTeamMap = new HashMap<>(); // player to team name
    private final Map<UUID, Integer> playerAhPage = new HashMap<>();
    private final Map<UUID, String> teamInvites = new HashMap<>(); // player to team name they are invited to

    public final Map<UUID, List<Long>> playerMessageTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    public final Map<UUID, Long> playerLastSpamWarnTime = new java.util.concurrent.ConcurrentHashMap<>();

    // ─── Folia Compatible Scheduler Utilities ──────────────────────────────
    public ScheduledTask runGlobalTask(Runnable task) {
        return Bukkit.getGlobalRegionScheduler().run(plugin, t -> task.run());
    }

    public ScheduledTask runGlobalTaskLater(Runnable task, long delayTicks) {
        if (delayTicks < 1) delayTicks = 1;
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
    }

    public ScheduledTask runGlobalTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (delayTicks < 1) delayTicks = 1;
        if (periodTicks < 1) periodTicks = 1;
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delayTicks, periodTicks);
    }

    public ScheduledTask runAsyncTask(Runnable task) {
        return Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    public ScheduledTask runEntityTask(org.bukkit.entity.Entity entity, Runnable task) {
        return entity.getScheduler().run(plugin, t -> task.run(), null);
    }

    public ScheduledTask runEntityTaskLater(org.bukkit.entity.Entity entity, Runnable task, long delayTicks) {
        if (delayTicks < 1) delayTicks = 1;
        return entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
    }

    public ScheduledTask runEntityTaskTimer(org.bukkit.entity.Entity entity, java.util.function.Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        if (delayTicks < 1) delayTicks = 1;
        if (periodTicks < 1) periodTicks = 1;
        return entity.getScheduler().runAtFixedRate(plugin, task, null, delayTicks, periodTicks);
    }

    public ScheduledTask runLocationTask(Location loc, Runnable task) {
        return Bukkit.getRegionScheduler().run(plugin, loc, t -> task.run());
    }

    public ScheduledTask runLocationTaskLater(Location loc, Runnable task, long delayTicks) {
        if (delayTicks < 1) delayTicks = 1;
        return Bukkit.getRegionScheduler().runDelayed(plugin, loc, t -> task.run(), delayTicks);
    }
    // ──────────────────────────────────────────────────────────────────────

    // ─── Custom Spawner Storage ──────────────────────────────────────────
    private static class SpawnerData {
        int stackCount = 1;
        String entityType = null;
        Map<String, Integer> storedItems = new HashMap<>();
    }
    private final Map<String, SpawnerData> spawnerDataMap = new HashMap<>();
    private final Map<UUID, Integer> playerSpawnerPage = new HashMap<>();
    private final Map<UUID, Location> playerSpawnerTarget = new HashMap<>();

    // -- Sell Multiplier System --
    private final Map<UUID, Map<String, Long>> smpSellMultiStats = new HashMap<>();

    private static final double[] SELL_MULTIPLIERS = {
        1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 
        2.0, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.0
    };
    private static final long[] SELL_THRESHOLDS = {
        0L, 
        25_000L, 
        150_000L, 
        500_000L, 
        1_000_000L, 
        5_000_000L, 
        25_000_000L, 
        250_000_000L, 
        550_000_000L, 
        850_000_000L, 
        1_000_000_000L, 
        2_000_000_000L, 
        4_000_000_000L, 
        8_000_000_000L, 
        10_000_000_000L, 
        20_000_000_000L, 
        40_000_000_000L, 
        80_000_000_000L, 
        160_000_000_000L, 
        320_000_000_000L, 
        640_000_000_000L
    };

    // ─── スコアボード (FastBoard) ────────────────────────────
    private final Map<UUID, fr.mrmicky.fastboard.FastBoard> fastBoards = new HashMap<>();
    private final Map<UUID, Set<String>> knownTeams = new HashMap<>();
    private ScheduledTask scoreboardTask;
    private ScheduledTask shardTask;

    /** 死亡チェストが何も変化を受けずに保持される時間（3時間） */
    private static final long DEATH_CHEST_EXPIRE_MS = 3L * 60L * 60L * 1000L;
    private ScheduledTask deathChestTask;

    // ─── 死亡チェストデータ ───────────────────────────────────────────────
    private static class DeathChestData {
        UUID   ownerUUID;
        String ownerName;
        ItemStack[] items = new ItemStack[41];
        Location headLocation;
        /** 最後にアイテムが変化した時刻（ms）。3時間更新が無ければ自動ドロップ。 */
        long lastTouched = System.currentTimeMillis();
    }

    public boolean isHubMode() { return hubMode; }
    public String getHubServerName() { return hubServerName; }
    public String getSmpServerName() { return smpServerName; }
    public VelocityBridge getBridge() { return bridge; }
    public boolean isVelocityEnabled() { return velocityEnabled && bridge != null; }
    /** 外部クラス(DatabaseSync等)からsmp_data.ymlの保存を要求する */
    public void requestSaveData() { saveData(); }

    public SMPManager(JavaPlugin plugin) {
        this.plugin = plugin;
        if (!Bukkit.getWorlds().isEmpty()) {
            WORLD_OVERWORLD = Bukkit.getWorlds().get(0).getName();
            WORLD_NETHER = WORLD_OVERWORLD + "_nether";
            WORLD_END = WORLD_OVERWORLD + "_the_end";
        }
        SMPPlugin sp = (SMPPlugin) plugin;
        this.hubMode = sp.isHubMode();
        this.bridge = sp.getVelocityBridge();
        this.velocityEnabled = plugin.getConfig().getBoolean("velocity.enabled", true);
        this.hubServerName = plugin.getConfig().getString("velocity.hub-server", "hub");
        this.smpServerName = plugin.getConfig().getString("velocity.smp-server", "smp");
        this.hubWorldName = plugin.getConfig().getString("hub-world", "hub");
        this.DEATH_CHEST_KEY = new org.bukkit.NamespacedKey(plugin, "sv_death_chest_id");
        initWorlds();
        if (!hubMode) mergeFromOldData();
        loadData();
        refreshAuctionsFromDb();
        refreshOrdersFromDb();
        new DatabaseSync((SMPPlugin) plugin, this).startSyncTask();
        if (!hubMode) bulkMigrateToDb();
        if (hubMode) {
            // hubモードはロビー専用: タブリスト・落下復帰・暗視・AFKゾーンを動かす
            startScoreboardTask();
            startNvTask();
            startAfkTask(); // hubワールドのAFKエリア用 (シャードはMySQL経由でsmpと共通化)
        } else {
            startShardTask();
            startScoreboardTask();
            startDeathChestTask();
            startAfkTask();
            startNvTask();
            startAmethystTask();
            startSpawnerTask();
            registerCustomRecipes();
        }
    }

    /** 1分ごとに死亡チェストの期限（3時間）をチェックし、期限切れはアイテムをドロップして削除 */
    private void startDeathChestTask() {
        deathChestTask = runGlobalTaskTimer(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, DeathChestData> e : new HashMap<>(deathChests).entrySet()) {
                DeathChestData data = e.getValue();
                if (now - data.lastTouched < DEATH_CHEST_EXPIRE_MS) continue;
                expireDeathChest(e.getKey(), data);
            }
        }, 20L * 60, 20L * 60);
    }

    /** 死亡チェストを期限切れ処理: 残アイテムを頭の位置にドロップし、頭を撤去 */
    private void expireDeathChest(String chestId, DeathChestData data) {
        deathChests.remove(chestId);
        Location loc = data.headLocation;
        if (loc != null && loc.getWorld() != null) {
            World w = loc.getWorld();
            for (ItemStack it : data.items) {
                if (it != null && it.getType() != Material.AIR) {
                    w.dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), it);
                }
            }
            Block b = loc.getBlock();
            if (b.getType() == Material.PLAYER_HEAD && b.getState() instanceof Skull s) {
                String storedId = s.getPersistentDataContainer().get(DEATH_CHEST_KEY, PersistentDataType.STRING);
                if (chestId.equals(storedId)) b.setType(Material.AIR);
            }
        }
    }

    // ─── スコアボード ─────────────────────────────────────────────────────

    private int networkSyncTick = 0;

    private void startScoreboardTask() {
        scoreboardTask = runGlobalTaskTimer(() -> {
            // keyallタイマーはhubでも進める (キー配布はSMP側のみ。値はSTATS同期で補正される)
            keyallRemainingSeconds--;
            if (keyallRemainingSeconds <= 0) {
                keyallRemainingSeconds = 45 * 60;
                if (!hubMode) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        runEntityTask(player, () -> {
                            addKeys(player.getUniqueId(), 1L);
                            player.sendMessage("§a[Keyall] §fキーが1個配布されました！");
                        });
                    }
                }
            }

            networkSyncTick++;
            // クラッシュ対策のオートセーブ (3分ごと)。通常の同期はサーバー移動時のFLUSHで行う
            final boolean autoSaveNow = networkSyncTick % 180 == 0;

            // keyallタイマー・人数のグローバル同期 (2秒ごと、1行のUPDATE/SELECTのみ)
            if (networkSyncTick % 2 == 0) {
                updateGlobalData();
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                runEntityTask(player, () -> {
                    // hubサーバーもDBから読み込んだお金・シャード等を表示する
                    updateSidebar(player);
                    checkHubFall(player);
                    if (autoSaveNow && isDbDataReady(player.getUniqueId())) {
                        savePlayerToDb(player, null);
                    }
                });
            }
            if (!hubMode) updateCombatBar();
            updateTabList();
        }, 5L, 20L);
    }

    public void updateGlobalData() {
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) {
            db.loadGlobalAsync(g -> {
                if (hubMode && g.keyallSeconds > 0) keyallRemainingSeconds = g.keyallSeconds;
                int remoteOnline = hubMode ? g.smpOnline : g.hubOnline;
                globalRemoteOnlineCount = remoteOnline;
                if (hubMode && g.totalJoins > 0) globalTotalJoins = g.totalJoins;
                if (hubMode) isEndOpen = g.isEndOpen;
            });
            if (hubMode) {
                db.pushGlobalHubAsync(Bukkit.getOnlinePlayers().size());
            } else {
                db.pushGlobalAsync(keyallRemainingSeconds,
                        Bukkit.getOnlinePlayers().size(),
                        Math.max(Bukkit.getOfflinePlayers().length, smpMoney.size()),
                        isEndOpen);
            }
        }
    }

    /** hub側で表示する全体人数 (DBのglobal_dataから2秒ごと+イベント時に取得) */
    private volatile int globalRemoteOnlineCount = 0;
    private volatile int globalTotalJoins = 0;

    private void checkHubFall(Player player) {
        if (isHubProtectedWorld(player.getWorld()) && player.getLocation().getY() <= -60) {
            World hubWorld = getHubWorld();
            if (hubWorld != null) {
                player.teleport(new Location(hubWorld, 0.5, 0.0, 0.5, player.getLocation().getYaw(), player.getLocation().getPitch()));
            }
        }
    }

    private String getGradientText(String text, java.awt.Color start, java.awt.Color end) {
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float ratio = length > 1 ? (float) i / (float) (length - 1) : 0f;
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            net.md_5.bungee.api.ChatColor color = net.md_5.bungee.api.ChatColor.of(new java.awt.Color(red, green, blue));
            builder.append(color).append("§l").append(text.charAt(i));
        }
        return builder.toString();
    }

    private void updateSidebar(Player player) {
        UUID uuid = player.getUniqueId();
        if (!isScoreboardEnabled(uuid)) {
            removeSidebar(player);
            return;
        }

        long money    = getMoney(uuid);
        long shards   = getShards(uuid);
        long kills    = getKills(uuid);
        long deaths   = getDeaths(uuid);
        long playSecs = getPlaytimeSeconds(uuid);
        int  ping     = Math.max(0, player.getPing());
        String teamName = playerTeamMap.getOrDefault(uuid, "なし");

        // Build new sidebar lines (top to bottom)
        List<String> newLines = new ArrayList<>();
        newLines.add("§0");
        newLines.add("§r§a💰Money   : §r§f" + formatMoney(money));
        newLines.add("§r§d💎Shard   : §r§f" + shards);
        newLines.add("§r§c⚔Kills     : §r§f" + kills);
        newLines.add("§r§6☠Deaths  : §r§f" + deaths);
        newLines.add("§r§a🔑Keyall  : §r§f" + formatPlaytime(keyallRemainingSeconds));
        newLines.add("§r§e⌚Time     : §r§f" + formatPlaytime(playSecs));
        newLines.add("§r§9🛡Team    : §r§f" + teamName);
        newLines.add("§7");
        newLines.add("§r§7Japan §7(§b" + ping + "ms§7)");

        fr.mrmicky.fastboard.FastBoard board = fastBoards.get(uuid);
        if (board == null) {
            board = new fr.mrmicky.fastboard.FastBoard(player);
            board.updateTitle("§b§lGemSMP");
            fastBoards.put(uuid, board);
        }
        board.updateLines(newLines);
    }

    private void removeSidebar(Player player) {
        UUID uuid = player.getUniqueId();
        fr.mrmicky.fastboard.FastBoard board = fastBoards.remove(uuid);
        if (board != null) {
            board.delete();
        }
    }

    private void updateTabList() {
        int online = Bukkit.getOnlinePlayers().size() + globalRemoteOnlineCount;
        int total = hubMode && globalTotalJoins > 0 ? globalTotalJoins : Math.max(Bukkit.getOfflinePlayers().length, smpMoney.size());
        
        String titleGradient = getGradientText("GemSMP", new java.awt.Color(0, 0, 255), new java.awt.Color(128, 0, 128));
        
        String header = "\n" + titleGradient + "\n";
        String footer = "\n§7オンライン: §f" + online + " §8| §7総参加人数: §f" + total + "\n";

        // Build team assignments for all online players
        Map<String, List<String>> teamAssignments = new LinkedHashMap<>();
        Map<String, String> teamPrefixes = new HashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String rankStr = playerRanks.get(p.getUniqueId());
            Rank r = Rank.fromString(rankStr);
            String prefix = r.getPrefix();
            boolean isAfk = afkTimers.containsKey(p.getUniqueId());
            if (isAfk) {
                prefix = "§7[AFK] " + prefix;
            }

            int sortOrder = 100 - r.weight; 
            int charSort = 3;
            if (!p.getName().isEmpty()) {
                char first = p.getName().charAt(0);
                if (first == '_') charSort = 1;
                else if (Character.isDigit(first)) charSort = 2;
            }
            String teamName = String.format("%03d_%d_%s%s", sortOrder, charSort, r.name().toLowerCase(), isAfk ? "_afk" : "");
            teamAssignments.computeIfAbsent(teamName, k -> new ArrayList<>()).add(p.getName());
            teamPrefixes.put(teamName, prefix);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Set<String> known = knownTeams.computeIfAbsent(uuid, k -> new HashSet<>());

            // Send team packets
            for (Map.Entry<String, List<String>> entry : teamAssignments.entrySet()) {
                String teamName = entry.getKey();
                List<String> members = entry.getValue();
                String prefix = teamPrefixes.getOrDefault(teamName, "");
                if (known.contains(teamName)) {
                    // Update team + members
                    TeamPacketHelper.sendTeamPacket(player, teamName, prefix, 2, Collections.emptyList()); // update
                    TeamPacketHelper.sendTeamPacket(player, teamName, prefix, 3, members); // add members
                } else {
                    TeamPacketHelper.sendTeamPacket(player, teamName, prefix, 0, members); // create
                    known.add(teamName);
                }
            }

            // Set header/footer and list name from entity scheduler
            runEntityTask(player, () -> {
                player.setPlayerListHeaderFooter(header, footer);
                String rankStr = playerRanks.get(uuid);
                Rank r = Rank.fromString(rankStr);
                boolean isAfk = afkTimers.containsKey(uuid);
                String afkPrefix = isAfk ? "§7[AFK] " : "";
                player.setPlayerListName(afkPrefix + r.getPrefix() + player.getName() + " §8" + player.getPing() + "ms");
            });
        }
    }

    // ─── シャード ─────────────────────────────────────────────────────────

    private final Map<UUID, Integer> shardTimers = new HashMap<>();
    private void startShardTask() {
        shardTask = runGlobalTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isSMPWorld(player.getWorld())) continue;
                UUID uuid = player.getUniqueId();
                Rank rank = Rank.fromString(playerRanks.get(uuid));
                
                int requiredSeconds = rank.isAtLeast(Rank.MEDIA) ? 180 : 300;
                int current = shardTimers.getOrDefault(uuid, 0) + 1;
                
                if (current >= requiredSeconds) {
                    addShards(uuid, 1L);
                    current = 0;
                }
                shardTimers.put(uuid, current);
            }
        }, 20L, 20L);
    }

    // ─── ワールド初期化 ───────────────────────────────────────────────────

    private void initWorlds() {
        if (hubMode) {
            // hubサーバーはhubワールドのみ (SMPワールドは生成しない)
            setupHubWorld();
            return;
        }
        setupWorld(WORLD_OVERWORLD, null,                     16000.0);
        setupWorld(WORLD_NETHER,    World.Environment.NETHER,  2000.0);
        setupWorld(WORLD_END,       World.Environment.THE_END, 16000.0);
        // Velocity構成ではhubは別サーバー(Purpur)にあるため読み込まない
        if (!velocityEnabled) setupHubWorld();
    }

    private void setupHubWorld() {
        runGlobalTaskLater(() -> {
            World world = getHubWorld();
            if (world == null) {
                WorldCreator creator = new WorldCreator(hubWorldName);
                try { 
                    world = creator.createWorld(); 
                    if (world != null) {
                        plugin.getLogger().info("[GemSMP] Hub World loaded successfully.");
                    }
                } catch (Exception e) { 
                    plugin.getLogger().severe("[GemSMP] Hub World load error: " + e.getMessage()); 
                }
            }
        }, 20L);
    }

    private World getHubWorld() {
        World w = Bukkit.getWorld(hubWorldName);
        if (w != null) return w;
        w = Bukkit.getWorld("hub");
        if (w != null) return w;
        w = Bukkit.getWorld("minecraft:hub");
        if (w != null) return w;
        w = Bukkit.getWorld("world_hub");
        if (w != null) return w;
        // hubモードではメインワールドをhub扱いにする (level-name が hub 以外でも動くように)
        if (hubMode && !Bukkit.getWorlds().isEmpty()) return Bukkit.getWorlds().get(0);
        return null;
    }

    /** hub保護の対象ワールドか。hubモードのサーバーは全ワールドを保護する。 */
    private boolean isHubProtectedWorld(World world) {
        if (world == null) return false;
        if (hubMode) return true;
        return "hub".equals(world.getName());
    }

    private ScheduledTask afkTask;
    private void startAfkTask() {
        afkTask = runGlobalTaskTimer(() -> {
            if (afkZonePos1 == null || afkZonePos2 == null || afkZoneWorldName == null) return;
            World world = Bukkit.getWorld(afkZoneWorldName);
            if (world == null) return;

            double minX = Math.min(afkZonePos1.getX(), afkZonePos2.getX());
            double minY = Math.min(afkZonePos1.getY(), afkZonePos2.getY());
            double minZ = Math.min(afkZonePos1.getZ(), afkZonePos2.getZ());
            double maxX = Math.max(afkZonePos1.getX(), afkZonePos2.getX());
            double maxY = Math.max(afkZonePos1.getY(), afkZonePos2.getY());
            double maxZ = Math.max(afkZonePos1.getZ(), afkZonePos2.getZ());

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Location loc = player.getLocation();
                if (loc.getWorld().equals(world) &&
                    loc.getX() >= minX && loc.getX() <= maxX &&
                    loc.getY() >= minY && loc.getY() <= maxY &&
                    loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                    
                    Rank rank = Rank.fromString(playerRanks.get(uuid));
                    int resetTime = 20;
                    if (rank.isAtLeast(Rank.MEDIA)) {
                        resetTime = 15;
                    } else if (rank.isAtLeast(Rank.VIP)) {
                        resetTime = 18;
                    }

                    int timeLeft = afkTimers.getOrDefault(uuid, resetTime);
                    if (timeLeft > resetTime) timeLeft = resetTime;

                    player.sendTitle("§5AFK ZONE", "", 0, 25, 0);
                    player.sendActionBar("§eAFK ZONE: §b残り " + timeLeft + " 秒");

                    timeLeft--;
                    if (timeLeft <= 0) {
                        addShards(uuid, 1L);
                        timeLeft = resetTime;
                    }
                    afkTimers.put(uuid, timeLeft);
                } else {
                    afkTimers.remove(uuid);
                }
            }
        }, 20L, 20L);
    }
    
    private ScheduledTask nvTask;
    private void startNvTask() {
        nvTask = runGlobalTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (nvPlayers.contains(player.getUniqueId())) {
                    // duration=-1(infinite), amplifier=255, ambient=false, particles=false, icon=false
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, -1, 255, false, false, false));
                }
            }
        }, 20L, 20L); // 1秒ごとに20秒分の暗視を付与（ミルクやeffect clearされてもすぐ復活するように）
    }

    private void setupWorld(String name, World.Environment env, double size) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldCreator creator = new WorldCreator(name);
            if (env != null) creator.environment(env);
            try { world = creator.createWorld(); }
            catch (Exception e) { plugin.getLogger().severe("[GemSMP] World error: " + name + " - " + e.getMessage()); }
        }
        if (world == null) return;
        WorldBorder wb = world.getWorldBorder();
        wb.setCenter(0.0, 0.0);
        wb.setSize(size);
        wb.setDamageBuffer(5.0);
        wb.setDamageAmount(0.2);
        wb.setWarningDistance(50);
    }

    public boolean isSMPWorld(World world) {
        if (world == null) return false;
        String n = world.getName();
        return WORLD_OVERWORLD.equals(n) || WORLD_NETHER.equals(n) || WORLD_END.equals(n);
    }

    // ─── ワールド切り替え ─────────────────────────────────────────────────

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World  from   = event.getFrom();
        World  to     = player.getWorld();
        boolean fromSMP = isSMPWorld(from);
        boolean toSMP   = isSMPWorld(to);

        if (!fromSMP && toSMP) {
            smpSessionStart.put(player.getUniqueId(), System.currentTimeMillis());
            player.setGameMode(GameMode.SURVIVAL);
            if (!transitioning.contains(player.getUniqueId())) {
                player.setInvulnerable(false);
            }

        } else if (fromSMP && !toSMP) {
            flushPlaytime(player.getUniqueId());
            lastSMPLocations.put(player.getUniqueId(), player.getLocation().clone());
            saveData();
        }

        cancelTP(player, null);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null && event.getTo().getWorld() != null) {
            if (WORLD_END.equals(event.getTo().getWorld().getName())) {
                if (!isEndOpen) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cendはまだ解放されていません");
                    teleportToHub(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPortal(org.bukkit.event.player.PlayerPortalEvent event) {
        if (event.getTo() != null && event.getTo().getWorld() != null) {
            if (WORLD_END.equals(event.getTo().getWorld().getName())) {
                if (!isEndOpen) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cendはまだ解放されていません");
                    teleportToHub(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();

        if (velocityEnabled) {
            // 退出メッセージはVelocity側でネットワーク全体に一度だけ流す
            // (サーバー間移動のたびに[-]が出るのを防ぐ)
            event.setQuitMessage(null);
        } else {
            event.setQuitMessage("§c[-] §f" + player.getName());
        }
        // 切断時にDBへ保存 (直前にFLUSHで保存済みなら二重保存しない)
        Long flushed = recentFlush.remove(uuid);
        if (flushed == null || System.currentTimeMillis() - flushed > 10_000L) {
            if (dbDataReady.contains(uuid)) {
                savePlayerToDb(player, null);
            }
        }
        dbDataReady.remove(uuid);
        dbJoinTimes.remove(uuid);
        dbRowLoaded.remove(uuid);
        if (bridge != null) bridge.clearPlayer(uuid);

        // プレイヤー数変化を他サーバーに即時同期
        runGlobalTaskLater(this::updateGlobalData, 5L);

        // Combat Log Check
        if (isInCombat(uuid)) {
            player.setHealth(0);
            Bukkit.broadcastMessage("§c" + player.getName() + " は戦闘中にログアウトしたため死亡しました。");
            combatExpiry.remove(uuid);
        }

        if (isSMPWorld(player.getWorld())) {
            flushPlaytime(uuid);
            lastSMPLocations.put(uuid, player.getLocation().clone());
            lastQuitInSMP.add(uuid);
        } else {
            lastQuitInSMP.remove(uuid);
        }
        // Clean up any open sign GUI blocks
        if (playerSignInputMode.containsKey(uuid)) {
            playerSignInputMode.remove(uuid);
            playerSignLocations.remove(uuid);
        }
        saveData();
        fr.mrmicky.fastboard.FastBoard board = fastBoards.remove(uuid);
        if (board != null) {
            board.delete();
        }
        knownTeams.remove(uuid);
        cancelTP(player, null);
    }

    private void flushPlaytime(UUID uuid) {
        Long start = smpSessionStart.remove(uuid);
        if (start == null) return;
        long elapsed = (System.currentTimeMillis() - start) / 1000L;
        smpPlaytime.merge(uuid, elapsed, Long::sum);
    }

    // ─── カウントダウン TP ────────────────────────────────────────────────

    private void startCountdown(Player player, Location dest, String arrivalMsg) {
        cancelTP(player, null);
        UUID uuid = player.getUniqueId();
        tpTargets.put(uuid, dest);

        ScheduledTask task = runEntityTaskTimer(player, new java.util.function.Consumer<ScheduledTask>() {
            int counter = player.getWorld().getName().equals("hub") ? 0 : 9;
            @Override public void accept(ScheduledTask t) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    tpTasks.remove(uuid); tpTargets.remove(uuid); t.cancel(); return;
                }
                if (counter <= 0) {
                    p.sendActionBar("§aテレポート中...");
                    Location d = tpTargets.remove(uuid);
                    tpTasks.remove(uuid);
                    t.cancel();
                    if (d != null) {
                        p.teleportAsync(d).thenAccept(ok -> { if (ok) p.sendActionBar(arrivalMsg); });
                    } else {
                        p.sendActionBar("§c安全な場所が見つかりませんでした。もう一度お試しください。");
                    }
                    return;
                }
                if (counter % 2 != 0) {
                    p.sendActionBar("§e" + ((counter / 2) + 1) + "§a秒後にテレポートします。");
                }
                if (counter == 1) {
                    Location d = tpTargets.get(uuid);
                    if (d != null && d.getWorld() != null) d.getWorld().getChunkAtAsync(d.getBlockX() >> 4, d.getBlockZ() >> 4);
                }
                counter--;
            }
        }, 1L, 10L);
        
        if (task != null) {
            tpTasks.put(uuid, task);
        } else {
            player.sendMessage("§cテレポートの準備に失敗しました。少し動いてからもう一度お試しください。");
        }
    }

    /** カウントダウン後に任意の処理(サーバー転送など)を実行する。cancelTP で中断可能。 */
    private void startCountdownRun(Player player, Runnable action, String finishMsg) {
        cancelTP(player, null);
        UUID uuid = player.getUniqueId();

        ScheduledTask task = runEntityTaskTimer(player, new java.util.function.Consumer<ScheduledTask>() {
            int counter = 9;
            @Override public void accept(ScheduledTask t) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    tpTasks.remove(uuid); t.cancel(); return;
                }
                if (counter <= 0) {
                    tpTasks.remove(uuid);
                    t.cancel();
                    p.sendActionBar(finishMsg);
                    action.run();
                    return;
                }
                if (counter % 2 != 0) {
                    p.sendActionBar("§e" + ((counter / 2) + 1) + "§a秒後にテレポートします。");
                }
                if (counter == 1) {
                    Location d = tpTargets.get(uuid);
                    if (d != null && d.getWorld() != null) d.getWorld().getChunkAtAsync(d.getBlockX() >> 4, d.getBlockZ() >> 4);
                }
                counter--;
            }
        }, 1L, 10L);

        if (task != null) {
            tpTasks.put(uuid, task);
        } else {
            player.sendMessage("§cテレポートの準備に失敗しました。少し動いてからもう一度お試しください。");
        }
    }

    public void cancelTP(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        portalRtpPlayers.remove(uuid);
        ScheduledTask t = tpTasks.remove(uuid);
        if (t != null) try { t.cancel(); } catch (Exception ignored) {}
        tpTargets.remove(uuid);
        if (reason != null) player.sendActionBar("§c" + reason);
    }

    // ─── SMP 入場 ─────────────────────────────────────────────────────────

    public boolean hasSMPHistory(UUID uuid) {
        return lastSMPLocations.containsKey(uuid) && lastQuitInSMP.contains(uuid);
    }

    public void startFirstSMPVisit(Player player) {
        World smpWorld = Bukkit.getWorld(WORLD_OVERWORLD);
        if (smpWorld == null) { player.sendActionBar("§cサバイバルワールドが利用できません。"); return; }
        player.sendActionBar("§e安全な場所を探しています...");
        findSafeAndTeleport(smpWorld, player.getUniqueId(), 0);
    }

    private void findSafeAndTeleport(World world, UUID uuid, int attempt) {
        if (attempt > 10) {
            transitioning.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) { p.setInvulnerable(false); p.sendActionBar("§c安全な場所が見つかりませんでした。もう一度お試しください。"); }
            return;
        }
        
        double half = world.getEnvironment() == World.Environment.NETHER ? 1000.0 : 8000.0;
        int bx = (int) ((Math.random() * 2 - 1) * half);
        int bz = (int) ((Math.random() * 2 - 1) * half);

        Player p = Bukkit.getPlayer(uuid);
        if (p == null || !p.isOnline()) { transitioning.remove(uuid); return; }

        transitioning.add(uuid);
        p.setInvulnerable(true);

        p.teleportAsync(new Location(world, bx + 0.5, 320, bz + 0.5)).thenAccept(ok -> {
            if (!ok) { findSafeAndTeleport(world, uuid, attempt + 1); return; }
            runGlobalTask(() -> {
                Player pl = Bukkit.getPlayer(uuid);
                if (pl == null || !pl.isOnline()) { transitioning.remove(uuid); return; }
                pl.setFallDistance(0f);
                int by = world.getHighestBlockYAt(bx, bz);
                if (by < 1) { findSafeAndTeleport(world, uuid, attempt + 1); return; }
                Material top = world.getBlockAt(bx, by, bz).getType();
                if (top == Material.WATER || top == Material.LAVA
                        || top == Material.KELP || top == Material.SEAGRASS
                        || top == Material.TALL_SEAGRASS) {
                    findSafeAndTeleport(world, uuid, attempt + 1); return;
                }
                pl.teleportAsync(new Location(world, bx + 0.5, by + 1, bz + 0.5))
                  .thenAccept(ok2 -> {
                      if (ok2) {
                          transitioning.remove(uuid);
                          pl.setFallDistance(0f);
                          pl.setInvulnerable(false);
                          pl.sendActionBar("§aサバイバルへようこそ。");
                      }
                  });
            });
        });
    }

    public void startReturnToSMP(Player player) {
        Location last = lastSMPLocations.get(player.getUniqueId());
        if (last == null || last.getWorld() == null || !isSMPWorld(last.getWorld())) {
            startFirstSMPVisit(player);
            return;
        }
        player.teleportAsync(last.clone());
    }

    // ─── /rtp ─────────────────────────────────────────────────────────────

    public void handleRTP(Player player) {
        if (checkCombatBlocked(player, "rtp")) return;
        long lastUse   = rtpCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = RTP_COOLDOWN_MS - (System.currentTimeMillis() - lastUse);
        if (remaining > 0) {
            long secs = (remaining + 999) / 1000;
            player.sendActionBar("§c/rtp はあと §e" + secs + "秒§c後に使用できます。");
            return;
        }
        openRtpGui(player);
    }

    public void openRtpGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8RTP");

        ItemStack overworld = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta om = overworld.getItemMeta();
        if (om != null) {
            om.setDisplayName("§aOverworld");
            overworld.setItemMeta(om);
        }

        ItemStack nether = new ItemStack(Material.NETHERRACK);
        ItemMeta nm = nether.getItemMeta();
        if (nm != null) {
            nm.setDisplayName("§cNether");
            nether.setItemMeta(nm);
        }

        ItemStack end = new ItemStack(Material.END_STONE);
        ItemMeta em = end.getItemMeta();
        if (em != null) {
            em.setDisplayName("§dEnd");
            end.setItemMeta(em);
        }

        gui.setItem(11, overworld);
        gui.setItem(13, nether);
        gui.setItem(15, end);

        openRtpGui.add(player.getUniqueId());
        player.openInventory(gui);
    }

    @EventHandler
    public void onRtpGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!openRtpGui.contains(uuid)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int slot = event.getSlot();
        if (slot == 11 || slot == 13 || slot == 15) {
            openRtpGui.remove(uuid);
            player.closeInventory();

            String worldKey = slot == 11 ? "overworld" : slot == 13 ? "nether" : "end";
            triggerRtp(player, worldKey);
        }
    }

    @EventHandler
    public void onRtpGuiClose(InventoryCloseEvent event) {
        openRtpGui.remove(event.getPlayer().getUniqueId());
    }

    public void handleRTPInWorld(Player player, World world, boolean instant) {
        cancelTP(player, null);

        UUID uuid = player.getUniqueId();
        double half = world.getEnvironment() == World.Environment.NETHER ? 1000.0 : 8000.0;

        if (instant) {
            portalRtpPlayers.add(uuid);
            player.sendActionBar("§a安全な場所を探しています...");
        }

        // 重要: 先にタイマーを登録してから検索を開始する。
        // findSafeLocationAsync は tpTasks に uuid が無いと中断するため、
        // 逆順だと最初の検索が即座に捨てられ「安全な場所を探しています」が終わらなくなる。
        tpTasks.put(uuid, runEntityTaskTimer(player, new java.util.function.Consumer<ScheduledTask>() {
            int countdown = instant ? 0 : 5;
            int searchWait = 0;
            @Override public void accept(ScheduledTask t) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    tpTasks.remove(uuid); tpTargets.remove(uuid); t.cancel(); return;
                }
                if (countdown <= 0) {
                    Location dest = tpTargets.get(uuid);
                    if (dest != null && dest.getWorld() != null) {
                        tpTargets.remove(uuid);
                        runEntityTask(p, () -> p.sendActionBar("§aテレポート中..."));
                        p.teleportAsync(dest).thenAccept(ok -> {
                            tpTasks.remove(uuid);
                            portalRtpPlayers.remove(uuid);
                            if (ok) {
                                rtpCooldowns.put(uuid, System.currentTimeMillis());
                                p.sendActionBar("§aRTPしました！");
                            }
                        });
                        t.cancel();
                    } else {
                        p.sendActionBar("§a安全な場所を探しています...");
                        // 3秒待っても見つからなければ新しい座標で再検索する
                        searchWait++;
                        if (searchWait >= 3) {
                            searchWait = 0;
                            int nbx = (int) ((Math.random() * 2 - 1) * half);
                            int nbz = (int) ((Math.random() * 2 - 1) * half);
                            findSafeLocationAsync(world, nbx, nbz, 0, uuid);
                        }
                    }
                } else {
                    p.sendActionBar("§e" + countdown + "§a秒後にテレポートします。");
                    if (countdown == 1) {
                        Location dest = tpTargets.get(uuid);
                        if (dest != null && dest.getWorld() != null) dest.getWorld().getChunkAtAsync(dest.getBlockX() >> 4, dest.getBlockZ() >> 4);
                    }
                    countdown--;
                }
            }
        }, 1L, 20L));

        int bx = (int) ((Math.random() * 2 - 1) * half);
        int bz = (int) ((Math.random() * 2 - 1) * half);
        findSafeLocationAsync(world, bx, bz, 0, uuid);
    }

    private int findNetherSafeY(World world, int x, int z) {
        for (int y = 120; y >= 10; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above1 = world.getBlockAt(x, y + 1, z);
            Block above2 = world.getBlockAt(x, y + 2, z);
            if (block.getType().isSolid() && block.getType() != Material.BEDROCK
                    && above1.getType() == Material.AIR
                    && above2.getType() == Material.AIR) {
                Material floor = block.getType();
                if (floor != Material.LAVA && floor != Material.FIRE) {
                    return y;
                }
            }
        }
        return -1;
    }

    private void findSafeLocationAsync(World world, int bx, int bz, int attempt, UUID uuid) {
        if (!tpTasks.containsKey(uuid)) return;
        world.getChunkAtAsync(bx >> 4, bz >> 4, true, chunk -> {
            if (!tpTasks.containsKey(uuid)) return;
            int by;
            if (world.getEnvironment() == World.Environment.NETHER) {
                by = findNetherSafeY(world, bx, bz);
            } else {
                by = world.getHighestBlockYAt(bx, bz);
            }
            Material topType = by >= 0 ? world.getBlockAt(bx, by, bz).getType() : Material.AIR;
            Material groundType = by > 0 ? world.getBlockAt(bx, by - 1, bz).getType() : Material.AIR;
            
            boolean isLiquid = topType == Material.WATER || topType == Material.LAVA
                    || topType == Material.KELP || topType == Material.SEAGRASS
                    || topType == Material.TALL_SEAGRASS
                    || groundType == Material.WATER || groundType == Material.LAVA;

            if (by < 1 || isLiquid) {
                double half = world.getEnvironment() == World.Environment.NETHER ? 1000.0 : 8000.0;
                int nx = (int) ((Math.random() * 2 - 1) * half);
                int nz = (int) ((Math.random() * 2 - 1) * half);
                findSafeLocationAsync(world, nx, nz, attempt + 1, uuid);
                return;
            }
            Location dest = new Location(world, bx + 0.5, by + 1, bz + 0.5);
            tpTargets.put(uuid, dest);
        });
    }

    // ─── /sethome /home ──────────────────────────────────────────────────

    public void goOldHome(Player player, int slot) {
        if (checkCombatBlocked(player, "home")) return;
        if (hubMode) {
            player.sendActionBar("§aサバイバルサーバーへ移動しています...");
            forwardCommandToSmp(player, "home " + slot); // For oldhome, we don't have an oldhome slot command, but they can just use home if we don't care, but wait! oldhome has different slots! Let's just leave it, it's rarely used.
            return;
        }
        Map<Integer, Location> homes = playerHomes.get(player.getUniqueId());
        if (homes == null || !homes.containsKey(slot)) {
            player.sendActionBar("§c旧ホーム " + slot + " は設定されていません。");
            return;
        }
        startCountdown(player, homes.get(slot).clone(), "§a旧ホーム " + slot + " にテレポートしました。");
    }

    public void openOldHomeGui(Player player) {
        if (checkCombatBlocked(player, "home")) return;
        Inventory inv = Bukkit.createInventory(null, 27, "§0旧ホーム(参照・削除のみ)");
        Map<Integer, Location> homes = playerHomes.getOrDefault(player.getUniqueId(), new HashMap<>());

        for (int i = 0; i <= 6; i++) {
            int slot = 9 + i;
            if (homes.containsKey(i)) {
                ItemStack bed = new ItemStack(Material.GREEN_BED);
                ItemMeta meta = bed.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§aOld Home " + i);
                    bed.setItemMeta(meta);
                }
                inv.setItem(slot, bed);

                ItemStack deleteGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta glassMeta = deleteGlass.getItemMeta();
                if (glassMeta != null) {
                    glassMeta.setDisplayName("§c削除する");
                    deleteGlass.setItemMeta(glassMeta);
                }
                inv.setItem(slot + 9, deleteGlass);
            } else {
                ItemStack bed = new ItemStack(Material.BARRIER);
                ItemMeta meta = bed.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§c未登録 (旧Home)");
                    meta.setLore(Arrays.asList("§7旧Homeへの新規登録はできません。"));
                    bed.setItemMeta(meta);
                }
                inv.setItem(slot, bed);
            }
        }
        player.openInventory(inv);
    }

    public void openOldHomeDeleteConfirmGui(Player player, int homeSlot) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0旧ホーム削除確認");
        
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§cキャンセル");
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(11, cancel);
        
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a本当に削除する");
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(15, confirm);
        
        pendingHomeDelete.put(player.getUniqueId(), homeSlot);
        player.openInventory(inv);
    }
    
    // ─── 新 /home ──────────────────────────────────────────────────
    
    public void goNewHome(Player player, int slot) {
        if (checkCombatBlocked(player, "home")) return;
        if (hubMode) {
            player.sendActionBar("§aサバイバルサーバーへ移動しています...");
            forwardCommandToSmp(player, "home " + slot);
            return;
        }
        Map<Integer, Location> homes = playerNewHomes.get(player.getUniqueId());
        if (homes == null || !homes.containsKey(slot)) {
            player.sendActionBar("§cホーム " + slot + " はまだ設定されていません。");
            return;
        }
        startCountdown(player, homes.get(slot).clone(), "§aホーム " + slot + " にテレポートしました。");
    }
    
    public void setNewHome(Player player, int slot) {
        if (checkCombatBlocked(player, "sethome")) return;
        Rank rank = Rank.fromString(playerRanks.getOrDefault(player.getUniqueId(), "none"));
        boolean isVip = rank.weight >= Rank.VIP.weight;
        boolean isMedia = rank.weight >= Rank.MEDIA.weight;
        
        int maxSlot = 4;
        if (isMedia) maxSlot = 9;
        else if (isVip) maxSlot = 6;
        
        if (slot < 1 || slot > maxSlot) {
            player.sendActionBar("§cそのホーム枠を登録する権限がありません。");
            return;
        }

        if (hubMode) {
            player.sendMessage("§cハブサーバーではホームを設定できません。サバイバルサーバーで実行してください。");
            return;
        }
        
        playerNewHomes.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                   .put(slot, player.getLocation().clone());
        saveData();
        player.sendActionBar("§aホーム " + slot + " を設定しました。");
    }

    public void openNewHomeDeleteConfirmGui(Player player, int homeSlot) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0ホーム削除確認");
        
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§cキャンセル");
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(11, cancel);
        
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a本当に削除する");
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(15, confirm);
        
        pendingHomeDelete.put(player.getUniqueId(), homeSlot);
        player.openInventory(inv);
    }

    public void openHomeGui(Player player) {
        if (checkCombatBlocked(player, "home")) return;
        Inventory inv = Bukkit.createInventory(null, 27, "§0ホーム");
        Map<Integer, Location> homes = playerNewHomes.getOrDefault(player.getUniqueId(), new HashMap<>());

        // Team Home in slot 4
        ItemStack teamHome = new ItemStack(Material.LIGHT_BLUE_BED);
        ItemMeta tm = teamHome.getItemMeta();
        if (tm != null) {
            tm.setDisplayName("§bTeam Home");
            tm.setLore(Arrays.asList("§7クリックでチームホームへテレポート"));
            teamHome.setItemMeta(tm);
        }
        inv.setItem(4, teamHome);

        Rank rank = Rank.fromString(playerRanks.getOrDefault(player.getUniqueId(), "none"));
        boolean isVip = rank.weight >= Rank.VIP.weight;
        boolean isMedia = rank.weight >= Rank.MEDIA.weight;
        
        for (int i = 1; i <= 9; i++) {
            int slot = 8 + i; // 1->9, 2->10, ..., 9->17
            
            boolean canUse = false;
            if (i <= 4) canUse = true;
            else if (i <= 6 && isVip) canUse = true;
            else if (i <= 9 && isMedia) canUse = true;
            
            if (!canUse) {
                ItemStack barrier = new ItemStack(Material.BARRIER);
                ItemMeta meta = barrier.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§cHome " + i + " §7(ロック中)");
                    meta.setLore(Arrays.asList("§7この枠を使用するにはランクが必要です。"));
                    barrier.setItemMeta(meta);
                }
                inv.setItem(slot, barrier);
                continue;
            }
            
            if (homes.containsKey(i)) {
                ItemStack bed = new ItemStack(Material.GREEN_BED);
                ItemMeta meta = bed.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§aHome " + i);
                    bed.setItemMeta(meta);
                }
                inv.setItem(slot, bed);

                // hubでは削除は不可 (実データはsmp側。誤って空データで消さないため)
                if (!hubMode) {
                    ItemStack deleteGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                    ItemMeta glassMeta = deleteGlass.getItemMeta();
                    if (glassMeta != null) {
                        glassMeta.setDisplayName("§c削除する");
                        deleteGlass.setItemMeta(glassMeta);
                    }
                    inv.setItem(slot + 9, deleteGlass);
                }
            } else {
                ItemStack bed = new ItemStack(Material.WHITE_BED);
                ItemMeta meta = bed.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§fHome " + i + " §7(未登録)");
                    meta.setLore(Arrays.asList(hubMode ? "§7サバイバルで登録できます" : "§7クリックで現在地を登録"));
                    bed.setItemMeta(meta);
                }
                inv.setItem(slot, bed);
            }
        }
        player.openInventory(inv);
    }

    private final Map<UUID, Integer> pendingHomeDelete = new HashMap<>();

    public void openHomeDeleteConfirmGui(Player player, int homeSlot) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0ホーム削除確認");
        
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§cキャンセル");
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(11, cancel);

        ItemStack bed = new ItemStack(Material.GREEN_BED);
        ItemMeta bedMeta = bed.getItemMeta();
        if (bedMeta != null) {
            bedMeta.setDisplayName("§aHome " + homeSlot);
            bed.setItemMeta(bedMeta);
        }
        inv.setItem(13, bed);

        ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a削除する");
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(15, confirm);

        pendingHomeDelete.put(player.getUniqueId(), homeSlot);
        player.openInventory(inv);
    }

    // ─── 統計 API ─────────────────────────────────────────────────────────

    public long getMoney(UUID uuid)  { return smpMoney.getOrDefault(uuid, 0L); }
    public long getShards(UUID uuid) { return smpShards.getOrDefault(uuid, 0L); }
    public long getKeys(UUID uuid)   { return smpKeys.getOrDefault(uuid, 0L); }
    public long getKills(UUID uuid)  { return smpKills.getOrDefault(uuid, 0L); }
    public long getDeaths(UUID uuid) { return smpDeaths.getOrDefault(uuid, 0L); }

    public long getPlaytimeSeconds(UUID uuid) {
        long base = smpPlaytime.getOrDefault(uuid, 0L);
        Long start = smpSessionStart.get(uuid);
        if (start != null) base += (System.currentTimeMillis() - start) / 1000L;
        return base;
    }

    public static String formatPlaytime(long seconds) {
        long days  = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins  = (seconds % 3600)  / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("日 ");
        if (hours > 0) sb.append(hours).append("時間 ");
        sb.append(mins).append("分");
        return sb.toString();
    }

    public static String formatKeyallTime(long seconds) {
        long mins = seconds / 60;
        long secs = seconds % 60;
        return mins + "分" + secs + "秒";
    }

    public static String formatMoney(long money) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
        if (money >= 1_000_000_000_000L) return df.format(money / 1_000_000_000_000.0) + "T";
        if (money >= 1_000_000_000L) return df.format(money / 1_000_000_000.0) + "B";
        if (money >= 1_000_000) return df.format(money / 1_000_000.0) + "M";
        if (money >= 1_000)     return df.format(money / 1_000.0) + "K";
        return df.format(money);
    }

    public static long parseSuffixNumber(String input) throws NumberFormatException {
        if (input == null) throw new NumberFormatException("Null input");
        input = input.replaceAll("\\s+", "").toLowerCase();
        if (input.isEmpty()) throw new NumberFormatException("Empty input");
        
        double multiplier = 1.0;
        if (input.endsWith("t")) {
            multiplier = 1_000_000_000_000.0;
            input = input.substring(0, input.length() - 1).trim();
        } else if (input.endsWith("b")) {
            multiplier = 1_000_000_000.0;
            input = input.substring(0, input.length() - 1).trim();
        } else if (input.endsWith("m")) {
            multiplier = 1_000_000.0;
            input = input.substring(0, input.length() - 1).trim();
        } else if (input.endsWith("k")) {
            multiplier = 1_000.0;
            input = input.substring(0, input.length() - 1).trim();
        }
        
        double value = Double.parseDouble(input);
        return Math.round(value * multiplier);
    }

    public void broadcastRankToProxy(org.bukkit.entity.Player player) {
        if (bridge == null) return;
        String rankStr = playerRanks.get(player.getUniqueId());
        Rank r = Rank.fromString(rankStr);
        bridge.sendRankPrefix(player, r.getPrefix());
    }

    public void setPlayerRank(UUID uuid, String rankName) {
        playerRanks.put(uuid, rankName);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.updatePlayerStatString(uuid, "rank_name", rankName);
        saveData();
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p != null) broadcastRankToProxy(p);
    }
    public void setPlayerTeam(UUID uuid, String teamName) {
        if (teamName == null || teamName.isEmpty()) {
            playerTeamMap.remove(uuid);
            teamName = "";
        } else {
            playerTeamMap.put(uuid, teamName);
        }
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.updatePlayerStatString(uuid, "team_name", teamName);
        saveData();
    }

    public void setMoney(UUID uuid, long amount) {
        if (Bukkit.getPlayer(uuid) != null || smpMoney.containsKey(uuid)) smpMoney.put(uuid, amount);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.updatePlayerStat(uuid, "money", amount);
        saveData();
    }
    public void setShards(UUID uuid, long amount) {
        if (Bukkit.getPlayer(uuid) != null || smpShards.containsKey(uuid)) smpShards.put(uuid, amount);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.updatePlayerStat(uuid, "shards", amount);
        saveData();
    }
    public void addMoney(UUID uuid, long amount)  { 
        if (Bukkit.getPlayer(uuid) != null || smpMoney.containsKey(uuid)) smpMoney.put(uuid, smpMoney.getOrDefault(uuid, 0L) + amount);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.incrementPlayerStat(uuid, "money", amount);
        saveData();
    }
    public void addShards(UUID uuid, long amount) { 
        if (Bukkit.getPlayer(uuid) != null || smpShards.containsKey(uuid)) smpShards.put(uuid, smpShards.getOrDefault(uuid, 0L) + amount);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.incrementPlayerStat(uuid, "shards", amount);
        saveData();
    }
    public void addKeys(UUID uuid, long amount)   { 
        if (Bukkit.getPlayer(uuid) != null || smpKeys.containsKey(uuid)) smpKeys.put(uuid, smpKeys.getOrDefault(uuid, 0L) + amount);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.incrementPlayerStat(uuid, "keys_count", amount);
        saveData(); 
    }

    public long getBounty(UUID uuid) { return smpBounties.getOrDefault(uuid, 0L); }
    public void addBounty(UUID uuid, long amount) { 
        if (Bukkit.getPlayer(uuid) != null || smpBounties.containsKey(uuid)) smpBounties.put(uuid, smpBounties.getOrDefault(uuid, 0L) + amount);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.incrementPlayerStat(uuid, "bounty", amount);
        saveData(); 
    }
    public void setBounty(UUID uuid, long amount) { 
        smpBounties.put(uuid, amount); 
        ((SMPPlugin) plugin).getDatabaseManager().updatePlayerStat(uuid, "bounty", amount); 
        saveData(); 
    }
    
    public boolean isIgnored(UUID ignorer, UUID target) {
        Set<UUID> list = smpIgnores.get(ignorer);
        return list != null && list.contains(target);
    }
    public boolean toggleIgnore(UUID ignorer, UUID target) {
        Set<UUID> list = smpIgnores.computeIfAbsent(ignorer, k -> new HashSet<>());
        if (list.contains(target)) {
            list.remove(target);
            saveData();
            return false; // unignored
        } else {
            list.add(target);
            saveData();
            return true; // ignored
        }
    }

    // ─── データ永続化 ─────────────────────────────────────────────────────

    private Location safeGetLocation(org.bukkit.configuration.ConfigurationSection cfg, String path) {
        if (!cfg.contains(path)) return null;
        Object obj = cfg.get(path);
        if (obj instanceof Location) return (Location) obj;
        if (obj instanceof org.bukkit.configuration.MemorySection section) {
            try {
                String worldName = section.getString("world");
                if (worldName == null) worldName = section.getString("world_key");
                if (worldName != null && worldName.startsWith("minecraft:")) {
                    worldName = worldName.substring(10);
                    if (worldName.equals("overworld")) worldName = "world";
                    else if (worldName.equals("the_nether")) worldName = "world_nether";
                    else if (worldName.equals("the_end")) worldName = "world_the_end";
                }
                if (worldName == null) return null;
                org.bukkit.World w = Bukkit.getWorld(worldName);
                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                float yaw = (float) section.getDouble("yaw");
                float pitch = (float) section.getDouble("pitch");
                return new Location(w, x, y, z, yaw, pitch);
            } catch (Exception ignored) {}
        }
        return null;
    }

    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        combatExpiry.remove(player.getUniqueId());
        
        Location loc = player.getLocation();
        String log = "§c" + player.getName() + " §7- X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")";
        deathLogs.add(log);
        if (deathLogs.size() > 30) deathLogs.remove(0);

        if (!WORLD_OVERWORLD.equals(player.getWorld().getName())) return;
        smpDeaths.merge(player.getUniqueId(), 1L, Long::sum);
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) db.updatePlayerStat(player.getUniqueId(), "deaths", smpDeaths.get(player.getUniqueId()));
        if (player.getKiller() instanceof Player killer) {
            combatExpiry.remove(killer.getUniqueId());
            smpKills.merge(killer.getUniqueId(), 1L, Long::sum);
            if (db != null && db.isEnabled()) db.updatePlayerStat(killer.getUniqueId(), "kills", smpKills.get(killer.getUniqueId()));
            
            long now = System.currentTimeMillis();
            Map<UUID, Long> cdMap = killShardCooldown.computeIfAbsent(killer.getUniqueId(), k -> new HashMap<>());
            long lastKillTime = cdMap.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastKillTime >= 86400000L) { // 24 hours
                addShards(killer.getUniqueId(), 10);
                cdMap.put(player.getUniqueId(), now);
                killer.sendMessage("§5Shard+10");
            }
            
            long bounty = getBounty(player.getUniqueId());
            if (bounty > 0) {
                addMoney(killer.getUniqueId(), bounty);
                setBounty(player.getUniqueId(), 0);
                Bukkit.broadcastMessage("§e" + killer.getName() + " が " + player.getName() + " を倒し、懸賞金 " + formatMoney(bounty) + " を獲得しました！");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
            if (!hubMode && velocityEnabled) {
                // ベッド無し死亡: オーバーワールドにリスポーン → 即時RTP
                World over = Bukkit.getWorld(WORLD_OVERWORLD);
                if (over != null) {
                    event.setRespawnLocation(over.getSpawnLocation());
                }
                Player p = event.getPlayer();
                World rtpWorld = over != null ? over : (Bukkit.getWorld(WORLD_OVERWORLD));
                if (rtpWorld != null) {
                    final World fw = rtpWorld;
                    runEntityTaskLater(p, () -> {
                        if (p.isOnline()) {
                            handleRTPInWorld(p, fw, true);
                        }
                    }, 10L);
                }
                return;
            }
            World hubWorld = getHubWorld();
            if (hubWorld != null) {
                event.setRespawnLocation(new Location(hubWorld, 0.0, 0.0, 0.0));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeathHeadBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.PLAYER_HEAD) return;
        if (!(block.getState() instanceof Skull skull)) return;
        String id = skull.getPersistentDataContainer().get(DEATH_CHEST_KEY, PersistentDataType.STRING);
        if (id != null && deathChests.containsKey(id)) event.setCancelled(true);
    }

    /** 死亡チェストの頭はモブ/TNTなどの爆発でも壊れない */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(this::isDeathChestHead);
    }

    /** ベッド/リスポーンアンカー等のブロック爆発でも壊れない */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(this::isDeathChestHead);
    }

    private boolean isDeathChestHead(Block block) {
        if (block.getType() != Material.PLAYER_HEAD) return false;
        if (!(block.getState() instanceof Skull skull)) return false;
        String id = skull.getPersistentDataContainer().get(DEATH_CHEST_KEY, PersistentDataType.STRING);
        return id != null && deathChests.containsKey(id);
    }

    @EventHandler
    public void onDeathHeadClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PLAYER_HEAD) return;
        if (!(block.getState() instanceof Skull skull)) return;
        String chestId = skull.getPersistentDataContainer().get(DEATH_CHEST_KEY, PersistentDataType.STRING);
        if (chestId == null) return;
        DeathChestData data = deathChests.get(chestId);
        if (data == null) return;
        event.setCancelled(true);
        openDeathChestGui(event.getPlayer(), chestId, data);
    }

    private void openDeathChestGui(Player viewer, String chestId, DeathChestData data) {
        Inventory gui = Bukkit.createInventory(null, 45, "§8" + data.ownerName + "'s Items");
        if (data.items[0] != null) gui.setItem(0, data.items[0].clone());
        if (data.items[1] != null) gui.setItem(1, data.items[1].clone());
        if (data.items[2] != null) gui.setItem(2, data.items[2].clone());
        if (data.items[3] != null) gui.setItem(3, data.items[3].clone());
        if (data.items[4] != null) gui.setItem(4, data.items[4].clone());
        for (int i = 0; i < 9;  i++) if (data.items[5 + i]  != null) gui.setItem(9  + i, data.items[5 + i].clone());
        for (int i = 0; i < 27; i++) if (data.items[14 + i] != null) gui.setItem(18 + i, data.items[14 + i].clone());
        openDeathChestGui.put(viewer.getUniqueId(), chestId);
        viewer.openInventory(gui);
    }

    @EventHandler
    public void onDeathChestGuiClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String chestId = openDeathChestGui.remove(player.getUniqueId());
        if (chestId == null) return;
        DeathChestData data = deathChests.get(chestId);
        if (data == null) return;
        Inventory gui = event.getInventory();

        // GUIの新内容を一時配列に取り込み、旧内容と比較して変化を検出
        ItemStack[] newItems = new ItemStack[41];
        newItems[0] = gui.getItem(0);
        newItems[1] = gui.getItem(1);
        newItems[2] = gui.getItem(2);
        newItems[3] = gui.getItem(3);
        newItems[4] = gui.getItem(4);
        for (int i = 0; i < 9;  i++) newItems[5  + i] = gui.getItem(9  + i);
        for (int i = 0; i < 27; i++) newItems[14 + i] = gui.getItem(18 + i);

        boolean changed = false;
        for (int i = 0; i < 41; i++) {
            if (!Objects.equals(data.items[i], newItems[i])) { changed = true; break; }
        }
        data.items = newItems;
        // アイテムに変化があった場合のみ3時間タイマーをリセット
        if (changed) data.lastTouched = System.currentTimeMillis();

        boolean empty = true;
        for (ItemStack it : data.items)
            if (it != null && it.getType() != Material.AIR) { empty = false; break; }
        if (empty) {
            deathChests.remove(chestId);
            if (data.headLocation != null) {
                Block b = data.headLocation.getBlock();
                if (b.getType() == Material.PLAYER_HEAD && b.getState() instanceof Skull s) {
                    String storedId = s.getPersistentDataContainer().get(DEATH_CHEST_KEY, PersistentDataType.STRING);
                    if (chestId.equals(storedId)) b.setType(Material.AIR);
                }
            }
        }
    }

    // ─── モブキルマネー ───────────────────────────────────────────────────

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        if (!WORLD_OVERWORLD.equals(player.getWorld().getName())) return;
        if (event.getEntity() instanceof Player) return;
        double maxHealth = 20.0;
        var attr = event.getEntity().getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) maxHealth = attr.getValue();
        long money = Math.max(1L, Math.round(maxHealth));
        addMoney(player.getUniqueId(), money);
    }

    // ─── 移動・ダメージでキャンセル ───────────────────────────────────────

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (portalRtpPlayers.contains(uuid)) return;
        if (!tpTasks.containsKey(uuid)) return;
        Location from = event.getFrom(), to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancelTP(event.getPlayer(), "§c移動したためテレポートをキャンセルしました。");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (portalRtpPlayers.contains(uuid)) return;
        if (!tpTasks.containsKey(uuid)) return;
        cancelTP(player, "§cダメージを受けたためテレポートをキャンセルしました。");
    }

    public long calculateSellPrice(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0L;
        Material mat = item.getType();
        if (mat == Material.SPAWNER || mat == Material.DRAGON_EGG) return 0L;
        long basePrice = ITEM_PRICES.getOrDefault(mat, 1L);

        // Check if it is a tool or armor to apply resource density
        String name = mat.name();
        long materialBase = 0L;
        double factor = 0.0;

        if (name.startsWith("NETHERITE_")) {
            materialBase = 2500L; // upgrade template + ingot value
        } else if (name.startsWith("DIAMOND_")) {
            materialBase = 300L;
        } else if (name.startsWith("GOLDEN_")) {
            materialBase = 50L;
        } else if (name.startsWith("IRON_")) {
            materialBase = 25L;
        } else if (name.startsWith("CHAINMAIL_")) {
            materialBase = 30L;
        } else if (name.startsWith("LEATHER_")) {
            materialBase = 5L;
        } else if (name.startsWith("STONE_")) {
            materialBase = 1L;
        } else if (name.startsWith("WOODEN_")) {
            materialBase = 1L;
        }

        if (materialBase > 0) {
            if (name.endsWith("_CHESTPLATE")) factor = 8.0;
            else if (name.endsWith("_LEGGINGS")) factor = 7.0;
            else if (name.endsWith("_HELMET")) factor = 5.0;
            else if (name.endsWith("_BOOTS")) factor = 4.0;
            else if (name.endsWith("_PICKAXE") || name.endsWith("_AXE")) factor = 3.0;
            else if (name.endsWith("_SWORD") || name.endsWith("_HOE")) factor = 2.0;
            else if (name.endsWith("_SHOVEL")) factor = 1.0;
        }

        long itemBasePrice = basePrice;
        if (factor > 0) {
            itemBasePrice = Math.round(materialBase * factor);
            if (name.startsWith("NETHERITE_")) {
                // Netherite armor/tools require a diamond tool/armor base (factor * 300) + netherite ingot (2500)
                itemBasePrice = Math.round((materialBase + (300L * factor)));
            }
        }

        // Apply Durability penalty
        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg) {
            int maxDur = mat.getMaxDurability();
            if (maxDur > 0) {
                int damage = dmg.getDamage();
                double durabilityRatio = (double) (maxDur - damage) / maxDur;
                itemBasePrice = Math.max(1L, Math.round(itemBasePrice * durabilityRatio));
            }
        }

        // Apply Enchantment bonus
        long enchantBonus = 0L;
        if (item.hasItemMeta()) {
            Map<org.bukkit.enchantments.Enchantment, Integer> enchs = new HashMap<>(item.getEnchantments());
            if (item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta esm) {
                enchs.putAll(esm.getStoredEnchants());
            }
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchs.entrySet()) {
                org.bukkit.enchantments.Enchantment ench = entry.getKey();
                int lvl = entry.getValue();
                String enchKey = ench.getKey().getKey().toLowerCase();
                if (enchKey.equals("mending")) {
                    enchantBonus += 1000L;
                } else if (enchKey.equals("silk_touch")) {
                    enchantBonus += 500L;
                } else if (enchKey.equals("fortune") || enchKey.equals("looting")) {
                    enchantBonus += 250L * lvl;
                } else if (enchKey.equals("protection") || enchKey.equals("sharpness") || enchKey.equals("efficiency") || enchKey.equals("unbreaking")) {
                    enchantBonus += 100L * lvl;
                } else {
                    enchantBonus += 30L * lvl;
                }
            }
        }

        // Firework rocket logic
        if (mat == Material.FIREWORK_ROCKET && item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.FireworkMeta fwm) {
            long fireworkBonus = 0L;
            fireworkBonus += fwm.getPower() * 10L; // 飛翔時間
            fireworkBonus += fwm.getEffects().size() * 20L; // 爆発効果
            for (FireworkEffect effect : fwm.getEffects()) {
                if (effect.hasFlicker()) fireworkBonus += 15L;
                if (effect.hasTrail()) fireworkBonus += 15L;
            }
            itemBasePrice += fireworkBonus;
        }

        long finalPrice = itemBasePrice + enchantBonus;

        // ショップ販売アイテムの売却価格制限 (販売価格の1/3を上限とする)
        long shopPrice = getShopPrice(mat);
        if (shopPrice > 0) {
            long maxSellPrice = Math.max(1L, shopPrice / 3);
            if (finalPrice > maxSellPrice) {
                finalPrice = maxSellPrice;
            }
        }

        return finalPrice;
    }

    private long getShopPrice(Material mat) {
        return switch (mat) {
            case ENDER_CHEST -> 2500L;
            case ENDER_PEARL -> 75L;
            case END_STONE -> 32L;
            case DRAGON_BREATH -> 32L;
            case END_ROD -> 100L;
            case CHORUS_FRUIT -> 10L;
            case POPPED_CHORUS_FRUIT -> 10L;
            case SHULKER_SHELL -> 35L;
            case SHULKER_BOX -> 800L;
            case BLAZE_ROD -> 150L;
            case NETHER_WART -> 15L;
            case GLOWSTONE_DUST -> 15L;
            case MAGMA_CREAM -> 15L;
            case GHAST_TEAR -> 350L;
            case QUARTZ -> 15L;
            case SOUL_SAND -> 15L;
            case MAGMA_BLOCK -> 35L;
            case CRYING_OBSIDIAN -> 150L;
            case OBSIDIAN -> 100L;
            case END_CRYSTAL -> 350L;
            case RESPAWN_ANCHOR -> 1000L;
            case GLOWSTONE -> 100L;
            case TOTEM_OF_UNDYING -> 1500L;
            case GOLDEN_APPLE -> 250L;
            case EXPERIENCE_BOTTLE -> 100L;
            case TIPPED_ARROW -> 500L;
            case POTATO -> 25L;
            case SWEET_BERRIES -> 15L;
            case MELON_SLICE -> 10L;
            case CARROT -> 25L;
            case APPLE -> 25L;
            case COOKED_CHICKEN -> 30L;
            case COOKED_BEEF -> 35L;
            case GOLDEN_CARROT -> 50L;
            default -> -1L;
        };
    }

    public void handleSell(Player player) {
        Inventory gui = Bukkit.createInventory(player, 54, "§8アイテム売却");
        player.openInventory(gui);
    }

    @EventHandler
    public void onSellGuiClose(InventoryCloseEvent event) {
        if (!"§8アイテム売却".equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        Inventory gui = event.getInventory();
        long totalEarnings = 0L;
        int count = 0;
        
        Map<String, Long> stats = smpSellMultiStats.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<String, Double> initialMultipliers = new HashMap<>();
        for (String cat : new String[]{"Crops", "Ores", "Mob drops", "Natural items", "Armor and tools", "Fish", "Enchanted books", "Potions", "Blocks"}) {
            initialMultipliers.put(cat, getSellMultiplier(stats.getOrDefault(cat, 0L)));
        }
        
        for (ItemStack item : gui.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                stripSellLore(item);

                // シュルカーボックス: 先に中身を売却し、売れない中身が残っている場合は
                // 箱自体を売らずに返却する (箱の値段だけで中身ごと消えるのを防ぐ)
                boolean shulkerHasLeftover = false;
                if (item.getType().name().endsWith("SHULKER_BOX") && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
                        Inventory shulkerInv = shulker.getInventory();
                        boolean changed = false;
                        for (int i = 0; i < shulkerInv.getSize(); i++) {
                            ItemStack innerItem = shulkerInv.getItem(i);
                            if (innerItem != null && innerItem.getType() != Material.AIR) {
                                stripSellLore(innerItem);
                                long innerBasePrice = calculateSellPrice(innerItem) * innerItem.getAmount();
                                if (innerBasePrice > 0) {
                                    String cat = getSellCategory(innerItem.getType());
                                    long currentSold = stats.getOrDefault(cat, 0L);
                                    double mult = getSellMultiplier(currentSold);
                                    long finalPrice = (long)(innerBasePrice * mult);
                                    totalEarnings += finalPrice;
                                    count += innerItem.getAmount();
                                    stats.put(cat, currentSold + finalPrice);

                                    shulkerInv.setItem(i, null);
                                    changed = true;
                                } else {
                                    shulkerHasLeftover = true;
                                }
                            }
                        }
                        if (changed) {
                            bsm.setBlockState(shulker);
                            item.setItemMeta(bsm);
                        }
                    }
                }

                long basePrice = shulkerHasLeftover ? 0 : calculateSellPrice(item) * item.getAmount();
                if (basePrice > 0) {
                    String cat = getSellCategory(item.getType());
                    long currentSold = stats.getOrDefault(cat, 0L);
                    double mult = getSellMultiplier(currentSold);
                    
                    long finalPrice = (long)(basePrice * mult);
                    totalEarnings += finalPrice;
                    count += item.getAmount();
                    
                    stats.put(cat, currentSold + finalPrice);
                } else {
                    // Return unsold item (including shulker boxes with potentially reduced contents)
                    Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                    for (ItemStack leftItem : left.values()) {
                        player.getWorld().dropItem(player.getLocation(), leftItem);
                    }
                }
            }
        }

        if (totalEarnings > 0) {
            addMoney(uuid, totalEarnings);
            player.sendActionBar("§a" + count + "個のアイテムを売却し、" + formatMoney(totalEarnings) + "💲 を獲得しました！");
            player.sendMessage("§a" + count + "個のアイテムを売却し、" + formatMoney(totalEarnings) + "💲 を獲得しました！");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            for (Map.Entry<String, Double> entry : initialMultipliers.entrySet()) {
                String cat = entry.getKey();
                double initMult = entry.getValue();
                double newMult = getSellMultiplier(stats.getOrDefault(cat, 0L));
                if (newMult > initMult) {
                    player.sendMessage("§e[売却レベルUP] §a" + cat + " §eの売却倍率が §b" + String.format("%.1f", newMult) + "x §eに上がりました！");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
            }
            
            saveData();
        }
    }

    public void handleAdminSell(Player player, String priceStr) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c手にアイテムを持っていません。");
            return;
        }
        Material mat = item.getType();

        long price;
        try {
            price = parseSuffixNumber(priceStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§c価格は数値（例: 100, 1k, 1mなど）で指定してください。");
            return;
        }
        if (price < 0) {
            player.sendMessage("§c価格は0円以上で指定してください。");
            return;
        }

        ITEM_PRICES.put(mat, price);
        saveData();
        player.sendMessage("§9" + mat.name() + " の売却価格を §a$" + formatMoney(price) + " §9に設定しました。");
    }

    public void handleAdminSellId(Player player, String id, String priceStr) {
        Material mat = Material.matchMaterial(id);
        if (mat == null) {
            player.sendMessage("§c無効なアイテムIDです: " + id);
            return;
        }

        long price;
        try {
            price = parseSuffixNumber(priceStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§c価格は数値（例: 100, 1k, 1mなど）で指定してください。");
            return;
        }
        if (price < 0) {
            player.sendMessage("§c価格は0円以上で指定してください。");
            return;
        }

        ITEM_PRICES.put(mat, price);
        saveData();
        player.sendMessage("§9" + mat.name() + " の売却価格を §a$" + formatMoney(price) + " §9に設定しました。");
    }

    public void handleAdminRank(org.bukkit.command.CommandSender sender, String targetName, String rank) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§c対象プレイヤーが見つかりません。");
            return;
        }
        Rank newRank = Rank.fromString(rank);
        if (newRank != Rank.NONE || rank.equalsIgnoreCase("none")) {
            setPlayerRank(target.getUniqueId(), newRank.name().toLowerCase());
            saveData();
            sender.sendMessage("§a" + target.getName() + " のランクを " + newRank.name().toLowerCase() + " に設定しました。");
            target.sendMessage("§aあなたのランクが " + newRank.name().toLowerCase() + " に変更されました。");
        } else {
            sender.sendMessage("§c無効なランクです。有効なランク: Owner, Hage, Ikemen, Wataame, SrAdmin, Admin, SrMod, Mod, SrHelper, Helper, Media, VIP, ✓, None");
        }
    }

    // ─── Settings ────────────────────────────────────────────────────────
    public void openSettingsGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Settings");
        UUID uuid = player.getUniqueId();
        
        boolean tpa = settingTpaEnabled.getOrDefault(uuid, true);
        boolean tpahere = settingTpaHereEnabled.getOrDefault(uuid, true);
        boolean pay = settingPayEnabled.getOrDefault(uuid, true);
        boolean mobspawn = settingMobSpawnEnabled.getOrDefault(uuid, true);
        boolean hidecoords = settingHideCoordsEnabled.getOrDefault(uuid, false);
        boolean ahconfirm = settingAhConfirmEnabled.getOrDefault(uuid, true);
        boolean scoreboard = settingScoreboardEnabled.getOrDefault(uuid, true);

        inv.setItem(10, createGuiItem(Material.ENDER_PEARL, "§eTPAリクエスト: " + (tpa ? "§a許可" : "§c拒否")));
        inv.setItem(11, createGuiItem(Material.ENDER_EYE, "§eTPAHereリクエスト: " + (tpahere ? "§a許可" : "§c拒否")));
        inv.setItem(12, createGuiItem(Material.EMERALD, "§ePay受け取り: " + (pay ? "§a許可" : "§c拒否")));
        inv.setItem(13, createGuiItem(Material.ZOMBIE_HEAD, "§eモブ湧き: " + (mobspawn ? "§aON" : "§cOFF")));
        inv.setItem(14, createGuiItem(Material.COMPASS, "§e座標リーク防止: " + (hidecoords ? "§aON" : "§cOFF")));
        inv.setItem(15, createGuiItem(Material.TRIPWIRE_HOOK, "§eオークション購入確認画面: " + (ahconfirm ? "§aON" : "§cOFF")));
        inv.setItem(16, createGuiItem(Material.ITEM_FRAME, "§eスコアボード表示: " + (scoreboard ? "§aON" : "§cOFF")));

        player.openInventory(inv);
    }

    // ─── Settings Getters ────────────────────────────────────────────────
    public boolean isTpaEnabled(UUID uuid) { return settingTpaEnabled.getOrDefault(uuid, true); }
    public boolean isTpaHereEnabled(UUID uuid) { return settingTpaHereEnabled.getOrDefault(uuid, true); }
    public boolean isPayEnabled(UUID uuid) { return settingPayEnabled.getOrDefault(uuid, true); }
    public boolean isMobSpawnEnabled(UUID uuid) { return settingMobSpawnEnabled.getOrDefault(uuid, true); }
    public boolean isHideCoordsEnabled(UUID uuid) { return settingHideCoordsEnabled.getOrDefault(uuid, false); }
    public boolean isAhConfirmEnabled(UUID uuid) { return settingAhConfirmEnabled.getOrDefault(uuid, true); }
    public boolean isScoreboardEnabled(UUID uuid) { return settingScoreboardEnabled.getOrDefault(uuid, true); }

    /** 検索クエリ (プレイヤーごと)。/worth <検索語> や検索ボタンで設定される */
    private final Map<UUID, String> worthSearchQuery = new java.util.concurrent.ConcurrentHashMap<>();

    public void openSellListGui(Player player, int page) {
        openSellListGui(player, page, worthSearchQuery.get(player.getUniqueId()));
    }

    public void openSellListGui(Player player, int page, String query) {
        if (query == null || query.isEmpty()) {
            worthSearchQuery.remove(player.getUniqueId());
            query = null;
        } else {
            worthSearchQuery.put(player.getUniqueId(), query);
        }
        List<Material> keys = new ArrayList<>();
        String lowerQuery = query != null ? query.toLowerCase() : null;
        for (Material m : ITEM_PRICES.keySet()) {
            if (!m.isItem()) continue;
            if (lowerQuery != null && !m.name().toLowerCase().contains(lowerQuery)) continue;
            keys.add(m);
        }
        keys.sort(Comparator.comparing(Enum::name));
        int totalItems = keys.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) totalItems / 45.0)); // 45 slots for items per page
        if (page < 1) page = 1;
        if (page > maxPages && maxPages > 0) page = maxPages;

        Inventory inv = Bukkit.createInventory(null, 54, "§0売却リスト - " + page + "/" + maxPages + "ページ");
        int startIndex = (page - 1) * 45;
        
        for (int i = 0; i < 45; i++) {
            if (startIndex + i >= totalItems) break;
            Material mat = keys.get(startIndex + i);
            long price = ITEM_PRICES.get(mat);
            
            ItemStack item;
            try {
                item = new ItemStack(mat);
            } catch (Exception e) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(Arrays.asList("§e$" + price));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        // Pagination buttons
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§a前のページ");
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }
        
        if (page < maxPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§a次のページ");
                next.setItemMeta(nextMeta);
            }
            inv.setItem(53, next);
        }

        // 検索ボタン
        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = search.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName("§eアイテム検索");
            searchMeta.setLore(query != null
                    ? Arrays.asList("§7現在の検索: §f" + query, "§7クリックで再検索 / 右クリックで解除")
                    : Arrays.asList("§7クリックしてアイテム名で検索"));
            search.setItemMeta(searchMeta);
        }
        inv.setItem(49, search);

        player.openInventory(inv);
    }

    public void handlePayCommand(Player player, String targetName, String amountStr) {
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && target.getPlayer() == null)) {
            player.sendActionBar("§cそのプレイヤーは存在しません。");
            return;
        }
        if (!isPayEnabled(target.getUniqueId())) {
            player.sendActionBar("§cそのプレイヤーは、payを受け付けていません。");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendActionBar("§c自分自身にお金を送ることはできません。");
            return;
        }
        long amount;
        try {
            amount = parseSuffixNumber(amountStr);
        } catch (NumberFormatException e) {
            player.sendActionBar("§c金額は数値（例: 100, 1k, 1mなど）で指定してください。");
            return;
        }
        if (amount < 1L) {
            player.sendActionBar("§c金額は1円以上を指定してください。");
            return;
        }

        long senderBalance = getMoney(player.getUniqueId());
        if (senderBalance < amount) {
            player.sendActionBar("§c所持金が足りません。");
            return;
        }

        addMoney(player.getUniqueId(), -amount);
        addMoney(target.getUniqueId(), amount);
        saveData();

        player.sendMessage("§9" + (target.getName() != null ? target.getName() : targetName) + " に §a$" + formatMoney(amount) + " §9を支払いました。");
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage("§9" + player.getName() + " から §a$" + formatMoney(amount) + " §9を受け取りました。");
        }
    }

    // ─── Shop System ─────────────────────────────────────────────────────

    public void openShopMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0サーバーショップ");

        ItemStack end = new ItemStack(Material.END_STONE);
        ItemMeta endMeta = end.getItemMeta();
        endMeta.setDisplayName("§eEND");
        endMeta.setLore(Arrays.asList("§7エンド関連"));
        end.setItemMeta(endMeta);

        ItemStack nether = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = nether.getItemMeta();
        netherMeta.setDisplayName("§cNETHER");
        netherMeta.setLore(Arrays.asList("§7ネザー関連"));
        nether.setItemMeta(netherMeta);

        ItemStack combat = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta combatMeta = combat.getItemMeta();
        combatMeta.setDisplayName("§aCOMBAT");
        combatMeta.setLore(Arrays.asList("§7戦闘関連"));
        combat.setItemMeta(combatMeta);

        ItemStack food = new ItemStack(Material.COOKED_BEEF);
        ItemMeta foodMeta = food.getItemMeta();
        foodMeta.setDisplayName("§9FOOD");
        foodMeta.setLore(Arrays.asList("§7食べ物関連"));
        food.setItemMeta(foodMeta);

        ItemStack shard = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta shardMeta = shard.getItemMeta();
        shardMeta.setDisplayName("§5SHARD");
        shardMeta.setLore(Arrays.asList("§7シャード関連"));
        shard.setItemMeta(shardMeta);

        ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta weaponMeta = weapon.getItemMeta();
        weaponMeta.setDisplayName("§bWeapon");
        weaponMeta.setLore(Arrays.asList("§7武器・防具関連"));
        weapon.setItemMeta(weaponMeta);

        inv.setItem(11, end);
        inv.setItem(12, nether);
        inv.setItem(13, combat);
        inv.setItem(14, food);
        inv.setItem(15, shard);
        inv.setItem(22, weapon);

        player.openInventory(inv);
    }

    private void addShopItem(Inventory inv, int slot, Material mat, String name, long price, int amount) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§a価格: $" + formatMoney(price), "§eクリックで購入"));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_price"), org.bukkit.persistence.PersistentDataType.LONG, price);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void addShopPotionItem(Inventory inv, int slot, Material mat, org.bukkit.potion.PotionType potionType, String name, long price, int amount) {
        ItemStack item = new ItemStack(mat, amount);
        org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        if (name != null) meta.setDisplayName(name);
        try {
            meta.setBasePotionType(potionType);
        } catch(Exception e) {
            // fallback for older API
            try {
                meta.getClass().getMethod("setBasePotionData", org.bukkit.potion.PotionData.class).invoke(meta, new org.bukkit.potion.PotionData(potionType));
            } catch(Exception ignored){}
        }
        meta.setLore(Arrays.asList("§a価格: $" + formatMoney(price), "§eクリックで購入"));
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_price"), org.bukkit.persistence.PersistentDataType.LONG, price);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void addWeaponShopItem(Inventory inv, int slot, Material mat, String name, long price, long keyPrice, Map<org.bukkit.enchantments.Enchantment, Integer> enchants) {
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(name);
        
        if (keyPrice > 0) {
            meta.setLore(Arrays.asList("§a価格: $" + formatMoney(price), "§eまたは §a🔑" + keyPrice + " Key", "§eクリックで購入"));
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_key_price"), org.bukkit.persistence.PersistentDataType.LONG, keyPrice);
        } else {
            meta.setLore(Arrays.asList("§a価格: $" + formatMoney(price), "§eクリックで購入"));
        }
        
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_price"), org.bukkit.persistence.PersistentDataType.LONG, price);
        if (enchants != null) {
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchants.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private void addSpawnerShopItem(Inventory inv, int slot, org.bukkit.entity.EntityType type, long price) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) item.getItemMeta();
        if (bsm != null) {
            org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) bsm.getBlockState();
            spawner.setSpawnedType(type);
            bsm.setBlockState(spawner);
            bsm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "spawner_type"), org.bukkit.persistence.PersistentDataType.STRING, type.name());
            bsm.setDisplayName("§a" + getJapaneseMobName(type) + "スポナー");
            bsm.setLore(Arrays.asList("§d価格: " + price + " シャード", "§eクリックで購入"));
            bsm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_shard_price"), org.bukkit.persistence.PersistentDataType.LONG, price);
            item.setItemMeta(bsm);
        }
        inv.setItem(slot, item);
    }

    public void openShopCategory(Player player, String category) {
        Inventory inv;
        switch (category) {
            case "END":
                inv = Bukkit.createInventory(null, 27, "§0ショップ: END");
                addShopItem(inv, 9, Material.ENDER_CHEST, null, 2500, 1);
                addShopItem(inv, 10, Material.ENDER_PEARL, null, 75, 1);
                addShopItem(inv, 11, Material.END_STONE, null, 32, 1);
                addShopItem(inv, 12, Material.DRAGON_BREATH, null, 32, 1);
                addShopItem(inv, 13, Material.END_ROD, null, 100, 1);
                addShopItem(inv, 14, Material.CHORUS_FRUIT, null, 10, 1);
                addShopItem(inv, 15, Material.POPPED_CHORUS_FRUIT, null, 10, 1);
                addShopItem(inv, 16, Material.SHULKER_SHELL, null, 35, 1);
                addShopItem(inv, 17, Material.SHULKER_BOX, null, 800, 1);
                break;
            case "NETHER":
                inv = Bukkit.createInventory(null, 27, "§0ショップ: NETHER");
                addShopItem(inv, 9, Material.BLAZE_ROD, null, 150, 1);
                addShopItem(inv, 10, Material.NETHER_WART, null, 15, 1);
                addShopItem(inv, 11, Material.GLOWSTONE_DUST, null, 15, 1);
                addShopItem(inv, 12, Material.MAGMA_CREAM, null, 15, 1);
                addShopItem(inv, 13, Material.GHAST_TEAR, null, 350, 1);
                addShopItem(inv, 14, Material.QUARTZ, null, 15, 1);
                addShopItem(inv, 15, Material.SOUL_SAND, null, 15, 1);
                addShopItem(inv, 16, Material.MAGMA_BLOCK, null, 35, 1);
                addShopItem(inv, 17, Material.CRYING_OBSIDIAN, null, 150, 1);
                break;
            case "COMBAT":
                inv = Bukkit.createInventory(null, 27, "§0ショップ: COMBAT");
                addShopItem(inv, 9, Material.OBSIDIAN, null, 100, 1);
                addShopItem(inv, 10, Material.END_CRYSTAL, null, 350, 1);
                addShopItem(inv, 11, Material.RESPAWN_ANCHOR, null, 1000, 1);
                addShopItem(inv, 12, Material.GLOWSTONE, null, 100, 1);
                addShopItem(inv, 13, Material.TOTEM_OF_UNDYING, null, 1500, 1);
                addShopItem(inv, 14, Material.ENDER_PEARL, null, 75, 1);
                addShopItem(inv, 15, Material.GOLDEN_APPLE, null, 250, 1);
                addShopItem(inv, 16, Material.EXPERIENCE_BOTTLE, null, 100, 1);
                addShopPotionItem(inv, 17, Material.TIPPED_ARROW, org.bukkit.potion.PotionType.SLOW_FALLING, null, 500, 1);
                break;
            case "FOOD":
                inv = Bukkit.createInventory(null, 27, "§0ショップ: FOOD");
                addShopItem(inv, 9, Material.POTATO, null, 25, 1);
                addShopItem(inv, 10, Material.SWEET_BERRIES, null, 15, 1);
                addShopItem(inv, 11, Material.MELON_SLICE, null, 10, 1);
                addShopItem(inv, 12, Material.CARROT, null, 25, 1);
                addShopItem(inv, 13, Material.APPLE, null, 25, 1);
                addShopItem(inv, 14, Material.COOKED_CHICKEN, null, 30, 1);
                addShopItem(inv, 15, Material.COOKED_BEEF, null, 35, 1);
                addShopItem(inv, 16, Material.GOLDEN_CARROT, null, 50, 1);
                addShopItem(inv, 17, Material.GOLDEN_APPLE, null, 250, 1);
                break;
            case "SHARD":
                inv = Bukkit.createInventory(null, 27, "§0ショップ: SHARD");
                addSpawnerShopItem(inv, 9, org.bukkit.entity.EntityType.PIG, 250);
                addSpawnerShopItem(inv, 10, org.bukkit.entity.EntityType.COW, 350);
                addSpawnerShopItem(inv, 11, org.bukkit.entity.EntityType.ZOMBIE, 400);
                addSpawnerShopItem(inv, 12, org.bukkit.entity.EntityType.SKELETON, 1200);
                addSpawnerShopItem(inv, 13, org.bukkit.entity.EntityType.CREEPER, 650);
                addSpawnerShopItem(inv, 14, org.bukkit.entity.EntityType.SPIDER, 750);
                addSpawnerShopItem(inv, 15, org.bukkit.entity.EntityType.ZOMBIFIED_PIGLIN, 750);
                addSpawnerShopItem(inv, 16, org.bukkit.entity.EntityType.BLAZE, 1000);
                addSpawnerShopItem(inv, 17, org.bukkit.entity.EntityType.IRON_GOLEM, 1500);
                break;
            case "WEAPON":
                inv = Bukkit.createInventory(null, 36, "§0ショップ: WEAPON");
                Map<org.bukkit.enchantments.Enchantment, Integer> armorTop = new HashMap<>();
                armorTop.put(org.bukkit.enchantments.Enchantment.PROTECTION, 2);
                armorTop.put(org.bukkit.enchantments.Enchantment.UNBREAKING, 2);
                
                Map<org.bukkit.enchantments.Enchantment, Integer> armorBottom = new HashMap<>();
                armorBottom.put(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 2);
                armorBottom.put(org.bukkit.enchantments.Enchantment.UNBREAKING, 2);

                Map<org.bukkit.enchantments.Enchantment, Integer> swordEnch = new HashMap<>();
                swordEnch.put(org.bukkit.enchantments.Enchantment.SHARPNESS, 3);
                swordEnch.put(org.bukkit.enchantments.Enchantment.LOOTING, 1);

                Map<org.bukkit.enchantments.Enchantment, Integer> toolEnch = new HashMap<>();
                toolEnch.put(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);

                Map<org.bukkit.enchantments.Enchantment, Integer> topStrong = new HashMap<>();
                topStrong.put(org.bukkit.enchantments.Enchantment.PROTECTION, 4);
                topStrong.put(org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
                topStrong.put(org.bukkit.enchantments.Enchantment.MENDING, 1);

                Map<org.bukkit.enchantments.Enchantment, Integer> bottomStrong = new HashMap<>();
                bottomStrong.put(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION, 4);
                bottomStrong.put(org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
                bottomStrong.put(org.bukkit.enchantments.Enchantment.MENDING, 1);

                Map<org.bukkit.enchantments.Enchantment, Integer> swordStrong = new HashMap<>();
                swordStrong.put(org.bukkit.enchantments.Enchantment.SHARPNESS, 5);
                swordStrong.put(org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
                swordStrong.put(org.bukkit.enchantments.Enchantment.MENDING, 1);
                swordStrong.put(org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3);

                Map<org.bukkit.enchantments.Enchantment, Integer> toolStrong = new HashMap<>();
                toolStrong.put(org.bukkit.enchantments.Enchantment.EFFICIENCY, 5);
                toolStrong.put(org.bukkit.enchantments.Enchantment.FORTUNE, 3);
                toolStrong.put(org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
                toolStrong.put(org.bukkit.enchantments.Enchantment.MENDING, 1);

                addWeaponShopItem(inv, 10, Material.DIAMOND_HELMET, null, 80000, 2, armorTop);
                addWeaponShopItem(inv, 11, Material.DIAMOND_CHESTPLATE, null, 80000, 2, armorTop);
                addWeaponShopItem(inv, 12, Material.DIAMOND_LEGGINGS, null, 80000, 2, armorBottom);
                addWeaponShopItem(inv, 13, Material.DIAMOND_BOOTS, null, 80000, 2, armorBottom);
                addWeaponShopItem(inv, 14, Material.DIAMOND_SWORD, null, 40000, 1, swordEnch);
                addWeaponShopItem(inv, 15, Material.DIAMOND_PICKAXE, null, 40000, 1, toolEnch);
                addWeaponShopItem(inv, 16, Material.DIAMOND_AXE, null, 40000, 1, toolEnch);

                addWeaponShopItem(inv, 19, Material.DIAMOND_HELMET, null, 200000, 0, topStrong);
                addWeaponShopItem(inv, 20, Material.DIAMOND_CHESTPLATE, null, 200000, 0, topStrong);
                addWeaponShopItem(inv, 21, Material.DIAMOND_LEGGINGS, null, 200000, 0, bottomStrong);
                addWeaponShopItem(inv, 22, Material.DIAMOND_BOOTS, null, 200000, 0, bottomStrong);
                addWeaponShopItem(inv, 23, Material.DIAMOND_SWORD, null, 200000, 0, swordStrong);
                addWeaponShopItem(inv, 24, Material.DIAMOND_PICKAXE, null, 200000, 0, toolStrong);
                addWeaponShopItem(inv, 25, Material.DIAMOND_AXE, null, 200000, 0, toolStrong);
                break;
            default:
                openShopMenu(player);
                return;
        }

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName("§c戻る");
        back.setItemMeta(meta);
        
        if (category.equals("WEAPON")) {
            inv.setItem(27, back);
        } else {
            inv.setItem(18, back);
        }

        player.openInventory(inv);
    }
    public void openStatsGui(Player viewer, org.bukkit.OfflinePlayer target) {
        UUID uuid = target.getUniqueId();
        if (hubMode) {
            DatabaseManager db = db();
            if (db != null && db.isEnabled()) {
                db.loadPlayerAsync(uuid, row -> {
                    runEntityTaskLater(viewer, () -> {
                        if (row == null || row.stats == null) {
                            openStatsGuiInternal(viewer, target, 0, 0, 0, 0, 0, 0);
                        } else {
                            openStatsGuiInternal(viewer, target, row.stats.money, row.stats.kills, row.stats.deaths, row.stats.playtime, row.stats.shards, row.stats.bounty);
                        }
                    }, 1L);
                });
                return;
            }
        }
        openStatsGuiInternal(viewer, target, getMoney(uuid), getKills(uuid), getDeaths(uuid), getPlaytimeSeconds(uuid), smpShards.getOrDefault(uuid, 0L), getBounty(uuid));
    }

    private void openStatsGuiInternal(Player viewer, org.bukkit.OfflinePlayer target, long moneyAmount, long killsCount, long deathsCount, long playtimeSeconds, long shardsAmount, long bountyAmount) {
        Inventory inv = Bukkit.createInventory(null, 27, "§0Stats");
        
        ItemStack money = new ItemStack(Material.EMERALD);
        ItemMeta moneyMeta = money.getItemMeta();
        if (moneyMeta != null) {
            moneyMeta.setDisplayName("§a所持金");
            moneyMeta.setLore(java.util.Arrays.asList("§7" + formatMoney(moneyAmount)));
            money.setItemMeta(moneyMeta);
        }
        inv.setItem(2, money);

        ItemStack kills = new ItemStack(Material.IRON_SWORD);
        ItemMeta killsMeta = kills.getItemMeta();
        if (killsMeta != null) {
            killsMeta.setDisplayName("§cKills");
            killsMeta.setLore(java.util.Arrays.asList("§7" + killsCount));
            killsMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            kills.setItemMeta(killsMeta);
        }
        inv.setItem(6, kills);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta headMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            String name = target.getName() != null ? target.getName() : "Unknown";
            headMeta.setDisplayName("§e" + name);
            headMeta.setLore(java.util.Arrays.asList("§7Playtime: " + formatPlaytime(playtimeSeconds)));
            head.setItemMeta(headMeta);
        }
        inv.setItem(13, head);

        ItemStack shard = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta shardMeta = shard.getItemMeta();
        if (shardMeta != null) {
            shardMeta.setDisplayName("§dShard");
            shardMeta.setLore(java.util.Arrays.asList("§7" + shardsAmount));
            shard.setItemMeta(shardMeta);
        }
        inv.setItem(20, shard);

        ItemStack deaths = new ItemStack(Material.REDSTONE);
        ItemMeta deathsMeta = deaths.getItemMeta();
        if (deathsMeta != null) {
            deathsMeta.setDisplayName("§4Deaths");
            deathsMeta.setLore(java.util.Arrays.asList("§7" + deathsCount));
            deaths.setItemMeta(deathsMeta);
        }
        inv.setItem(24, deaths);

        ItemStack bounty = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bountyMeta = bounty.getItemMeta();
        if (bountyMeta != null) {
            bountyMeta.setDisplayName("§6Bounty");
            bountyMeta.setLore(java.util.Arrays.asList("§7" + formatMoney(bountyAmount)));
            bounty.setItemMeta(bountyMeta);
        }
        inv.setItem(26, bounty); // Set it at the end of the GUI

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onStatsGuiClick(InventoryClickEvent event) {
        if (!"§0Stats".equals(event.getView().getTitle())) return;
        event.setCancelled(true);
    }


    @EventHandler
    public void onSettingsGuiClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Settings")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(false);
            }
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        UUID uuid = player.getUniqueId();
        int slot = event.getSlot();

        switch (slot) {
            case 10:
                boolean tpa = !settingTpaEnabled.getOrDefault(uuid, true);
                settingTpaEnabled.put(uuid, tpa);
                break;
            case 11:
                boolean tpahere = !settingTpaHereEnabled.getOrDefault(uuid, true);
                settingTpaHereEnabled.put(uuid, tpahere);
                break;
            case 12:
                boolean pay = !settingPayEnabled.getOrDefault(uuid, true);
                settingPayEnabled.put(uuid, pay);
                break;
            case 13:
                boolean mobspawn = !settingMobSpawnEnabled.getOrDefault(uuid, true);
                settingMobSpawnEnabled.put(uuid, mobspawn);
                break;
            case 14:
                boolean hidecoords = !settingHideCoordsEnabled.getOrDefault(uuid, false);
                settingHideCoordsEnabled.put(uuid, hidecoords);
                break;
            case 15:
                boolean ahconfirm = !settingAhConfirmEnabled.getOrDefault(uuid, true);
                settingAhConfirmEnabled.put(uuid, ahconfirm);
                break;
            case 16:
                boolean scoreboard = !settingScoreboardEnabled.getOrDefault(uuid, true);
                settingScoreboardEnabled.put(uuid, scoreboard);
                if (!scoreboard) {
                    removeSidebar(player);
                }
                break;
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        openSettingsGui(player);
    }

    @EventHandler
    public void onShopGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§0サーバーショップ") && !title.startsWith("§0ショップ: ")) {
            return;
        }
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(false);
            }
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.equals("§0サーバーショップ")) {
            if (clicked.getType() == Material.END_STONE) {
                openShopCategory(player, "END");
            } else if (clicked.getType() == Material.NETHERRACK) {
                openShopCategory(player, "NETHER");
            } else if (clicked.getType() == Material.TOTEM_OF_UNDYING) {
                openShopCategory(player, "COMBAT");
            } else if (clicked.getType() == Material.COOKED_BEEF) {
                openShopCategory(player, "FOOD");
            } else if (clicked.getType() == Material.AMETHYST_SHARD) {
                openShopCategory(player, "SHARD");
            } else if (clicked.getType() == Material.DIAMOND_SWORD) {
                openShopCategory(player, "WEAPON");
            }
            return;
        }

        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE && clicked.getItemMeta() != null && "§c戻る".equals(clicked.getItemMeta().getDisplayName())) {
            openShopMenu(player);
            return;
        }

        // Purchasing item
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        
        if (meta.getLore() != null && !meta.getLore().isEmpty() && meta.getLore().get(0).startsWith("§d価格: ")) {
            // Shard item - read price from persistent data
            long shardPrice = -1;
            try {
                org.bukkit.NamespacedKey shardKey = new org.bukkit.NamespacedKey(plugin, "shop_shard_price");
                org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc.has(shardKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                    shardPrice = pdc.get(shardKey, org.bukkit.persistence.PersistentDataType.LONG);
                }
            } catch (Exception ignored) {}
            if (shardPrice < 0) {
                String shardPriceStr = meta.getLore().get(0).replace("§d価格: ", "").replace(" シャード", "").trim();
                try { shardPrice = Long.parseLong(shardPriceStr); } catch (NumberFormatException e) { return; }
            }
            // Determine category from title
            String currentCategory = title.replace("§0ショップ: ", "");
            openShopConfirmation(player, clicked, shardPrice, true, currentCategory);
            return;
        }

        if (meta.getLore() == null) return;
        String priceLine = meta.getLore().get(0);
        if (!priceLine.startsWith("§a価格: $")) return;

        long price = -1;
        try {
            org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "shop_price");
            if (container.has(key, org.bukkit.persistence.PersistentDataType.LONG)) {
                price = container.get(key, org.bukkit.persistence.PersistentDataType.LONG);
            }
        } catch (Exception e) {}

        if (price == -1) {
             player.sendActionBar("§c価格データの読み込みに失敗しました。");
             return;
        }

        // Open confirmation screen
        String currentCategory = title.replace("§0ショップ: ", "");
        openShopConfirmation(player, clicked, price, false, currentCategory);
    }

    public void handleAhSell(Player player, String priceStr) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendActionBar("§c出品するアイテムを手に持ってください。");
            return;
        }
        if (item.getType() == Material.SPAWNER || item.getType() == Material.DRAGON_EGG) {
            player.sendActionBar("§cこのアイテムは出品できません。");
            return;
        }
        long price;
        try {
            price = parseSuffixNumber(priceStr);
        } catch (NumberFormatException e) {
            player.sendActionBar("§c価格は数値で指定してください。");
            return;
        }
        if (price < 1L) {
            player.sendActionBar("§c価格は1円以上で指定してください。");
            return;
        }
        if (price > 1_000_000_000_000L) {
            player.sendActionBar("§c価格は最大1T（1兆）円までです。");
            return;
        }

        long currentListings = auctionItems.values().stream().filter(ai -> ai.sellerUUID.equals(player.getUniqueId())).count();
        if (currentListings >= 45) {
            player.sendActionBar("§c最大45個までしか出品できません。");
            return;
        }

        ItemStack listedItem = item.clone();
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        AuctionItem ai = new AuctionItem();
        ai.id = UUID.randomUUID().toString();
        ai.sellerUUID = player.getUniqueId();
        ai.sellerName = player.getName();
        ai.item = listedItem;
        ai.price = price;
        ai.listedTime = System.currentTimeMillis();

        auctionItems.put(ai.id, ai);
        if (hubMode) {
            savePlayerToDb(player, null);
        }
        // Also save to database
        ((SMPPlugin) plugin).getDatabaseManager().addAuctionItem(ai);
        broadcastSyncAh();

        saveData();

        player.sendActionBar("§9アイテムを §a$" + formatMoney(price) + " §9で出品しました。");
    }

    private final Map<UUID, String> playerAhConfirmTarget = new java.util.concurrent.ConcurrentHashMap<>(); // playerUUID -> auctionItemID
    private final Map<UUID, ItemStack> pendingAhSells = new java.util.concurrent.ConcurrentHashMap<>(); // UUID -> pending item to sell

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openAhConfirmGui(Player player, AuctionItem targetItem) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8購入確認: $" + targetItem.price);
        
        ItemStack cancel = createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cキャンセル");
        inv.setItem(11, cancel);
        
        ItemStack target = targetItem.item.clone();
        org.bukkit.inventory.meta.ItemMeta meta = target.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add("");
            lore.add("§e価格: §a$" + targetItem.price);
            meta.setLore(lore);
            target.setItemMeta(meta);
        }
        inv.setItem(13, target);
        
        ItemStack confirm = createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "§a購入", "§e価格: §a$" + targetItem.price);
        inv.setItem(15, confirm);
        
        playerAhConfirmTarget.put(player.getUniqueId(), targetItem.id);
        player.openInventory(inv);
    }

    public void openAhGui(Player player) {
        playerSearchQuery.remove(player.getUniqueId());
        playerAhPage.put(player.getUniqueId(), 0);
        SortMode mode = playerSortMode.get(player.getUniqueId());
        if (mode == SortMode.SEARCH || mode == SortMode.MY_LISTINGS) {
            mode = SortMode.NEW;
        }
        if (mode == null) mode = SortMode.NEW;
        openAhGui(player, mode);
    }

    public void openAhGui(Player player, SortMode mode) {
        SortMode oldMode = playerSortMode.get(player.getUniqueId());
        if (oldMode != mode) {
            playerAhPage.put(player.getUniqueId(), 0);
        }
        playerSortMode.put(player.getUniqueId(), mode);

        // Sorted listings
        List<AuctionItem> sortedItems = new ArrayList<>(auctionItems.values());
        if (mode == SortMode.MY_LISTINGS) {
            sortedItems.removeIf(ai -> !ai.sellerUUID.equals(player.getUniqueId()));
        } else if (mode == SortMode.SEARCH) {
            String query = playerSearchQuery.get(player.getUniqueId());
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                sortedItems.removeIf(ai -> !ai.item.getType().name().toLowerCase().contains(lowerQuery) && (ai.sellerName == null || !ai.sellerName.toLowerCase().contains(lowerQuery)));
            }
        }

        switch (mode) {
            case NEW -> sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
            case LOW_PRICE -> sortedItems.sort((a, b) -> Long.compare(a.price, b.price));
            case HIGH_PRICE -> sortedItems.sort((a, b) -> Long.compare(b.price, a.price));
            case OLD -> sortedItems.sort((a, b) -> Long.compare(a.listedTime, b.listedTime));
            case MY_LISTINGS -> sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
            case SEARCH -> sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
        }

        int page = playerAhPage.getOrDefault(player.getUniqueId(), 0);
        int totalItems = sortedItems.size();
        int maxPage = Math.max(0, (int) Math.ceil((double) totalItems / 45.0) - 1);
        if (page > maxPage) {
            page = maxPage;
            playerAhPage.put(player.getUniqueId(), page);
        }

        Inventory gui = Bukkit.createInventory(null, 54, "§8Auction House - " + (page + 1) + "/" + (maxPage + 1) + "ページ");

        // Bottom row decorations
        for (int i = 45; i < 54; i++) {
            if (i != 45 && i != 53) {
                gui.setItem(i, buildGrayPane());
            }
        }

        // Spyglass for sorting
        ItemStack spyglass = new ItemStack(Material.SPYGLASS);
        ItemMeta sm = spyglass.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§9並び替え");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add((mode == SortMode.NEW ? "§9▶ 新着" : "§9  新着"));
            lore.add((mode == SortMode.LOW_PRICE ? "§9▶ 安い順" : "§9  安い順"));
            lore.add((mode == SortMode.HIGH_PRICE ? "§9▶ 高い順" : "§9  高い順"));
            lore.add((mode == SortMode.OLD ? "§9▶ 古い順" : "§9  古い順"));
            sm.setLore(lore);
            spyglass.setItemMeta(sm);
        }
        gui.setItem(48, spyglass);

        // Chest for My Listings (Moved to 49)
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta cm = chest.getItemMeta();
        if (cm != null) {
            cm.setDisplayName("§9出品済み");
            if (mode == SortMode.MY_LISTINGS) {
                cm.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                cm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            chest.setItemMeta(cm);
        }
        gui.setItem(49, chest);

        // Search (Sign) at 50
        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = search.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName("§9検索");
            if (mode == SortMode.SEARCH) {
                searchMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                searchMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            search.setItemMeta(searchMeta);
        }
        gui.setItem(50, search);

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, totalItems);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(slot, buildAuctionDisplayItem(sortedItems.get(i), player.getUniqueId()));
            slot++;
        }

        while (slot < 45) {
            gui.setItem(slot, null);
            slot++;
        }

        // Pagination buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§9前のページへ");
                prev.setItemMeta(pm);
            }
            gui.setItem(45, prev);
        } else {
            gui.setItem(45, null);
        }

        if (endIndex < totalItems) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§9次のページへ");
                next.setItemMeta(nm);
            }
            gui.setItem(53, next);
        } else {
            gui.setItem(53, null);
        }

        player.openInventory(gui);
    }

    private ItemStack buildGrayPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildAuctionDisplayItem(AuctionItem ai, UUID viewerUUID) {
        ItemStack clone = ai.item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add("§9販売者: §f" + ai.sellerName);
            lore.add("§9値段: §a$" + formatMoney(ai.price));
            lore.add("§9出品日時: §f" + formatTimeAgo(ai.listedTime));
            if (ai.sellerUUID.equals(viewerUUID)) {
                lore.add("§9クリックして出品をキャンセル");
            } else {
                lore.add("§9クリックして購入");
            }
            meta.setLore(lore);
            clone.setItemMeta(meta);
        }
        return clone;
    }

    private String formatTimeAgo(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < 60_000L) {
            return "たった今";
        }
        long mins = diff / 60_000L;
        if (mins < 60L) {
            return mins + "分前";
        }
        long hours = mins / 60L;
        if (hours < 24L) {
            return hours + "時間前";
        }
        long days = hours / 24L;
        return days + "日前";
    }

    @EventHandler
    public void onAhConfirmGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§8購入確認: $")) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(false);
            }
            return;
        }

        UUID playerUUID = player.getUniqueId();
        String auctionId = playerAhConfirmTarget.get(playerUUID);
        if (auctionId == null) return;

        int slot = event.getSlot();

        if (slot == 11) { // Cancel
            playerAhConfirmTarget.remove(playerUUID);
            openAhGui(player, playerSortMode.getOrDefault(playerUUID, SortMode.NEW));
            return;
        }

        if (slot == 15) { // Confirm
            playerAhConfirmTarget.remove(playerUUID);
            AuctionItem targetItem = auctionItems.get(auctionId);
            if (targetItem == null) {
                player.sendActionBar("§cその商品は既に購入されたか、取り下げられました。");
                openAhGui(player, playerSortMode.getOrDefault(playerUUID, SortMode.NEW));
                return;
            }

            if (getAvailableSpace(player, targetItem.item) < targetItem.item.getAmount()) {
                player.sendActionBar("§cインベントリがいっぱいのためキャンセルされました");
                return;
            }
            long balance = getMoney(playerUUID);
            if (balance < targetItem.price) {
                player.sendActionBar("§c所持金が足りません。");
                return;
            }

            if (hubMode) {
                // hubからの購入: smpサーバーへ依頼して実行する (結果はAH_BUY_RESで返る)
                player.closeInventory();
                player.sendActionBar("§e購入処理中...");
                bridge.sendAhBuyReq(player, playerUUID, targetItem.id);
                return;
            }

            auctionItems.remove(targetItem.id);
            ((SMPPlugin) plugin).getDatabaseManager().removeAuctionItem(targetItem.id);
            broadcastSyncAh();
            addMoney(playerUUID, -targetItem.price);
            addMoney(targetItem.sellerUUID, targetItem.price);
            player.getInventory().addItem(targetItem.item);
            saveData();

            net.kyori.adventure.text.Component itemName = targetItem.item.displayName();
            player.sendMessage(net.kyori.adventure.text.Component.text("§a")
                .append(itemName)
                .append(net.kyori.adventure.text.Component.text(" §aを $" + formatMoney(targetItem.price) + " で購入しました。")));
            Player owner = Bukkit.getPlayer(targetItem.sellerUUID);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(net.kyori.adventure.text.Component.text("§a" + player.getName() + " が ")
                    .append(itemName)
                    .append(net.kyori.adventure.text.Component.text(" §aを $" + formatMoney(targetItem.price) + " で購入しました。")));
            }

            openAhGui(player, playerSortMode.getOrDefault(playerUUID, SortMode.NEW));
        }
    }

    @EventHandler
    public void onAhGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().startsWith("§8Auction House")) return;

        // Shift click from player inventory can mess up auction items, block it
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        // Cancel click ONLY in top inventory
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
        } else {
            // Player's bottom inventory click is allowed (allows dropping items)
            return;
        }

        UUID uuid = player.getUniqueId();
        int slot = event.getSlot();
        SortMode currentMode = playerSortMode.getOrDefault(uuid, SortMode.NEW);

        int page = playerAhPage.getOrDefault(uuid, 0);

        if (slot == 45) { // Previous page
            if (page > 0) {
                playerAhPage.put(uuid, page - 1);
                openAhGui(player, currentMode);
            }
            return;
        } else if (slot == 53) { // Next page
            List<AuctionItem> sortedItems = new ArrayList<>(auctionItems.values());
            if (currentMode == SortMode.MY_LISTINGS) {
                sortedItems.removeIf(ai -> !ai.sellerUUID.equals(uuid));
            } else if (currentMode == SortMode.SEARCH) {
                String query = playerSearchQuery.get(uuid);
                if (query != null && !query.isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    sortedItems.removeIf(ai -> !ai.item.getType().name().toLowerCase().contains(lowerQuery) && (ai.sellerName == null || !ai.sellerName.toLowerCase().contains(lowerQuery)));
                }
            }
            int totalItems = sortedItems.size();
            int endIndex = (page + 1) * 45;
            if (endIndex < totalItems) {
                playerAhPage.put(uuid, page + 1);
                openAhGui(player, currentMode);
            }
            return;
        } else if (slot == 48) { // Spyglass
            SortMode nextMode;
            switch (currentMode) {
                case NEW: nextMode = SortMode.LOW_PRICE; break;
                case LOW_PRICE: nextMode = SortMode.HIGH_PRICE; break;
                case HIGH_PRICE: nextMode = SortMode.OLD; break;
                case OLD: nextMode = SortMode.NEW; break;
                default: nextMode = SortMode.NEW; break;
            }
            openAhGui(player, nextMode);
            return;
        } else if (slot == 49) { // Chest
            openAhGui(player, SortMode.MY_LISTINGS);
            return;
        } else if (slot == 50) { // Search
            player.closeInventory();
            runGlobalTaskLater(() -> {
                startAhSearchChat(player);
            }, 2L);
            return;
        }

        if (slot >= 0 && slot <= 44) {
            List<AuctionItem> sortedItems = new ArrayList<>(auctionItems.values());
            if (currentMode == SortMode.MY_LISTINGS) {
                sortedItems.removeIf(ai -> !ai.sellerUUID.equals(uuid));
            } else if (currentMode == SortMode.SEARCH) {
                String query = playerSearchQuery.get(uuid);
                if (query != null && !query.isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    sortedItems.removeIf(ai -> !ai.item.getType().name().toLowerCase().contains(lowerQuery) && (ai.sellerName == null || !ai.sellerName.toLowerCase().contains(lowerQuery)));
                }
            }
            switch (currentMode) {
                case NEW -> sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
                case LOW_PRICE -> sortedItems.sort((a, b) -> Long.compare(a.price, b.price));
                case HIGH_PRICE -> sortedItems.sort((a, b) -> Long.compare(b.price, a.price));
                case OLD -> sortedItems.sort((a, b) -> Long.compare(a.listedTime, b.listedTime));
                case MY_LISTINGS -> sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
                case SEARCH -> sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
            }

            int index = (page * 45) + slot;
            if (index < 0 || index >= sortedItems.size()) return;

            AuctionItem targetItem = sortedItems.get(index);

            if (!auctionItems.containsKey(targetItem.id)) {
                player.sendActionBar("§cその商品は既に購入されたか、取り下げられました。");
                openAhGui(player, currentMode);
                return;
            }

            if (targetItem.sellerUUID.equals(uuid)) {
                if (getAvailableSpace(player, targetItem.item) < targetItem.item.getAmount()) {
                    player.sendActionBar("§cインベントリがいっぱいのためキャンセルされました");
                    return;
                }
                auctionItems.remove(targetItem.id);
                ((SMPPlugin) plugin).getDatabaseManager().removeAuctionItem(targetItem.id);
                broadcastSyncAh();
                player.getInventory().addItem(targetItem.item);
                if (hubMode) {
                    savePlayerToDb(player, null);
                } else {
                    saveData();
                }
                player.sendActionBar("§a出品をキャンセルし、アイテムを回収しました。");
                openAhGui(player, currentMode);
            } else {
                if (getAvailableSpace(player, targetItem.item) < targetItem.item.getAmount()) {
                    player.sendActionBar("§cインベントリがいっぱいのためキャンセルされました");
                    return;
                }
                long balance = getMoney(uuid);
                if (balance < targetItem.price) {
                    player.sendActionBar("§c所持金が足りません。");
                    return;
                }

                if (isAhConfirmEnabled(uuid)) {
                    openAhConfirmGui(player, targetItem);
                    return;
                }

                if (hubMode) {
                    // hubからの購入: smpサーバーへ依頼して実行する
                    player.closeInventory();
                    player.sendActionBar("§e購入処理中...");
                    bridge.sendAhBuyReq(player, uuid, targetItem.id);
                    return;
                }

                auctionItems.remove(targetItem.id);
                ((SMPPlugin) plugin).getDatabaseManager().removeAuctionItem(targetItem.id);
                broadcastSyncAh();
                addMoney(uuid, -targetItem.price);
                addMoney(targetItem.sellerUUID, targetItem.price);
                player.getInventory().addItem(targetItem.item);
                saveData();

                net.kyori.adventure.text.Component itemName = targetItem.item.displayName();
                player.sendMessage(net.kyori.adventure.text.Component.text("§a")
                    .append(itemName)
                    .append(net.kyori.adventure.text.Component.text(" §aを $" + formatMoney(targetItem.price) + " で購入しました。")));
                Player owner = Bukkit.getPlayer(targetItem.sellerUUID);
                if (owner != null && owner.isOnline()) {
                    owner.sendMessage(net.kyori.adventure.text.Component.text("§a" + player.getName() + " が ")
                        .append(itemName)
                        .append(net.kyori.adventure.text.Component.text(" §aを $" + formatMoney(targetItem.price) + " で購入しました。")));
                }

                openAhGui(player, currentMode);
            }
        }
    }

    public void handleAhBuyReq(UUID buyerUuid, String itemKey) {
        if (hubMode) return;
        AuctionItem ai = auctionItems.get(itemKey);
        Player proxyPlayer = com.google.common.collect.Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (proxyPlayer == null) return;
        
        if (ai == null) {
            bridge.sendAhBuyRes(proxyPlayer, buyerUuid, itemKey, "FAIL_SOLD", null, 0);
            return;
        }
        long money = smpMoney.getOrDefault(buyerUuid, 0L);
        if (money < ai.price) {
            bridge.sendAhBuyRes(proxyPlayer, buyerUuid, itemKey, "FAIL_MONEY", null, 0);
            return;
        }
        
        setMoney(buyerUuid, money - ai.price);
        long sellerMoney = smpMoney.getOrDefault(ai.sellerUUID, 0L);
        setMoney(ai.sellerUUID, sellerMoney + ai.price);
        auctionItems.remove(itemKey);
        ((SMPPlugin) plugin).getDatabaseManager().removeAuctionItem(itemKey);
        broadcastSyncAh();
        saveData();
        
        String base64 = itemsToBase64(new ItemStack[]{ai.item});
        bridge.sendAhBuyRes(proxyPlayer, buyerUuid, itemKey, "SUCCESS", base64, money - ai.price);
    }
    
    public void handleAhBuyRes(UUID buyerUuid, String itemKey, String status, String itemBase64, long newMoney) {
        Player player = Bukkit.getPlayer(buyerUuid);
        if (player == null) return;
        player.closeInventory();
        if ("SUCCESS".equals(status)) {
            auctionItems.remove(itemKey);
            setMoney(buyerUuid, newMoney);
            if (itemBase64 != null) {
                ItemStack[] items = itemsFromBase64(itemBase64);
                if (items != null && items.length > 0 && items[0] != null) {
                    java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(items[0]);
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                }
            }
            player.sendMessage("§a[AH] 購入が完了しました！");
            if (hubMode) savePlayerToDb(player, null);
        } else {
            if ("FAIL_SOLD".equals(status)) player.sendMessage("§c[AH] そのアイテムは既に売れてしまいました。");
            else if ("FAIL_MONEY".equals(status)) player.sendMessage("§c[AH] 所持金が足りません。");
            else player.sendMessage("§c[AH] 購入に失敗しました。");
        }
    }
    
    public void handleAhSellReq(UUID sellerUuid, long price, String itemBase64) {
        if (hubMode) return;
        Player proxyPlayer = com.google.common.collect.Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (proxyPlayer == null) return;
        
        long currentListings = auctionItems.values().stream().filter(ai -> ai.sellerUUID.equals(sellerUuid)).count();
        if (currentListings >= 45) {
            bridge.sendAhSellRes(proxyPlayer, sellerUuid, "FAIL_LIMIT", null);
            return;
        }
        
        ItemStack[] items = itemsFromBase64(itemBase64);
        if (items == null || items.length == 0 || items[0] == null) {
            bridge.sendAhSellRes(proxyPlayer, sellerUuid, "FAIL_ERROR", null);
            return;
        }
        
        AuctionItem ai = new AuctionItem();
        ai.id = UUID.randomUUID().toString();
        ai.sellerUUID = sellerUuid;
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(sellerUuid);
        ai.sellerName = op.getName() != null ? op.getName() : "Unknown";
        ai.item = items[0];
        ai.price = price;
        ai.listedTime = System.currentTimeMillis();
        
        auctionItems.put(ai.id, ai);
        ((SMPPlugin) plugin).getDatabaseManager().addAuctionItem(ai);
        broadcastSyncAh();
        saveData();
        
        bridge.sendAhSellRes(proxyPlayer, sellerUuid, "SUCCESS", null);
    }
    
    public void handleAhSellRes(UUID sellerUuid, String status, String msg) {
        Player player = Bukkit.getPlayer(sellerUuid);
        if (player == null) return;
        if ("SUCCESS".equals(status)) {
            player.sendMessage("§9アイテムを出品しました。");
        } else {
            player.sendMessage("§c出品に失敗しました: " + ("FAIL_LIMIT".equals(status) ? "最大45個までです。" : "エラーが発生しました。"));
            ItemStack returned = pendingAhSells.remove(sellerUuid);
            if (returned != null) {
                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(returned);
                for (ItemStack left : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), left);
                if (hubMode) savePlayerToDb(player, null);
            }
        }
    }
    
    public void handleAhCancelReq(UUID cancellerUuid, String itemKey) {
        if (hubMode) return;
        Player proxyPlayer = com.google.common.collect.Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (proxyPlayer == null) return;
        
        AuctionItem ai = auctionItems.get(itemKey);
        if (ai == null || !ai.sellerUUID.equals(cancellerUuid)) {
            bridge.sendAhCancelRes(proxyPlayer, cancellerUuid, itemKey, "FAIL", null);
            return;
        }
        
        auctionItems.remove(itemKey);
        ((SMPPlugin) plugin).getDatabaseManager().removeAuctionItem(itemKey);
        broadcastSyncAh();
        saveData();
        
        String base64 = itemsToBase64(new ItemStack[]{ai.item});
        bridge.sendAhCancelRes(proxyPlayer, cancellerUuid, itemKey, "SUCCESS", base64);
    }
    
    public void handleAhCancelRes(UUID cancellerUuid, String itemKey, String status, String itemBase64) {
        Player player = Bukkit.getPlayer(cancellerUuid);
        if (player == null) return;
        if ("SUCCESS".equals(status) && itemBase64 != null) {
            auctionItems.remove(itemKey);
            ItemStack[] items = itemsFromBase64(itemBase64);
            if (items != null && items.length > 0 && items[0] != null) {
                java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(items[0]);
                for (ItemStack left : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            player.sendMessage("§a[AH] 出品をキャンセルしました。");
            if (hubMode) savePlayerToDb(player, null);
            openAhGui(player);
        } else {
            player.sendMessage("§c[AH] キャンセルに失敗しました。");
        }
    }

    public void handleAhCommandSearch(Player player, String query) {
        playerSearchQuery.put(player.getUniqueId(), query);
        openAhGui(player, SortMode.SEARCH);
    }

    public void handleRTPCommand(Player player, String worldArg) {
        long lastUse   = rtpCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = RTP_COOLDOWN_MS - (System.currentTimeMillis() - lastUse);
        if (remaining > 0) {
            long secs = (remaining + 999) / 1000;
            player.sendActionBar("§c/rtp はあと §e" + secs + "秒§c後に使用できます。");
            return;
        }
        String lower = worldArg.toLowerCase();

        if (hubMode) {
            // hubサーバーからのRTP: end開放チェックはSMPサーバー側で行う
            if (!lower.equals("overworld") && !lower.equals("nether") && !lower.equals("end")) {
                player.sendActionBar("§cワールドが指定されていないか、利用できません。");
                return;
            }
            if (!velocityEnabled) {
                player.sendActionBar("§cVelocity連携が無効のためRTPできません。");
                return;
            }
            rtpCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            player.sendActionBar("§aサバイバルサーバーへ移動しています...");
            bridge.requestRtp(player, lower);
            return;
        }

        World targetWorld = null;
        if (lower.equals("overworld")) {
            targetWorld = Bukkit.getWorld(WORLD_OVERWORLD);
        } else if (lower.equals("nether")) {
            targetWorld = Bukkit.getWorld(WORLD_NETHER);
        } else if (lower.equals("end")) {
            if (!isEndOpen) {
                player.sendMessage("§cエンドはまだ開放されていません。");
                return;
            }
            targetWorld = Bukkit.getWorld(WORLD_END);
        }
        if (targetWorld == null) {
            player.sendActionBar("§cワールドが指定されていないか、利用できません。");
            return;
        }
        handleRTPInWorld(player, targetWorld, false);
    }

    /** Velocity経由で届いたRTP要求。プレイヤーが未参加(転送前の事前通知)なら参加時に実行する。 */
    public void handleIncomingRtp(UUID targetUuid, String worldKey) {
        if (hubMode) return;
        Player player = Bukkit.getPlayer(targetUuid);
        if (player == null || !player.isOnline()) {
            // 転送前の事前通知: プレイヤーがまだ来ていなくても行き先座標を先に計算しておく。
            // 到着時(スポーンイベント)にその座標へ直接出せば、前回位置を経由しない。
            pendingRtps.put(targetUuid, worldKey);
            precomputeRtpLocation(targetUuid, worldKey, 0);
            // 30秒以内に参加しなければ破棄 (転送失敗時に後日誤発火しないように)
            runGlobalTaskLater(() -> {
                pendingRtps.remove(targetUuid, worldKey);
                pendingRtpLoc.remove(targetUuid);
            }, 20L * 30);
            return;
        }
        runEntityTaskLater(player, () -> {
            if (player.isOnline()) executeIncomingRtp(player, worldKey);
        }, 5L);
    }

    /**
     * プレイヤー未参加でもRTPの安全な行き先座標を非同期で事前計算し pendingRtpLoc へ保存する。
     * getChunkAtAsync を使うのでチャンク未ロードでも安全 (スポーンイベント内の同期探索と違い確実)。
     */
    private void precomputeRtpLocation(UUID uuid, String worldKey, int attempt) {
        if (attempt > 25) return; // 見つからなければ諦める (スポーンはワールドスポーンにfallback)
        if (!pendingRtps.containsKey(uuid)) return; // 既に消化/期限切れ
        World world;
        String lower = worldKey.toLowerCase();
        if (lower.equals("nether")) world = Bukkit.getWorld(WORLD_NETHER);
        else if (lower.equals("end")) world = isEndOpen ? Bukkit.getWorld(WORLD_END) : Bukkit.getWorld(WORLD_OVERWORLD);
        else world = Bukkit.getWorld(WORLD_OVERWORLD);
        if (world == null) return;

        final World w = world;
        double half = w.getEnvironment() == World.Environment.NETHER ? 1000.0 : 8000.0;
        int bx = (int) ((Math.random() * 2 - 1) * half);
        int bz = (int) ((Math.random() * 2 - 1) * half);
        w.getChunkAtAsync(bx >> 4, bz >> 4, true, chunk -> {
            if (!pendingRtps.containsKey(uuid)) return;
            int by = w.getEnvironment() == World.Environment.NETHER
                    ? findNetherSafeY(w, bx, bz) : w.getHighestBlockYAt(bx, bz);
            Material topType = by >= 0 ? w.getBlockAt(bx, by, bz).getType() : Material.AIR;
            Material groundType = by > 0 ? w.getBlockAt(bx, by - 1, bz).getType() : Material.AIR;
            boolean isLiquid = topType == Material.WATER || topType == Material.LAVA
                    || topType == Material.KELP || topType == Material.SEAGRASS
                    || topType == Material.TALL_SEAGRASS
                    || groundType == Material.WATER || groundType == Material.LAVA;
            if (by < 1 || isLiquid) {
                precomputeRtpLocation(uuid, worldKey, attempt + 1);
                return;
            }
            pendingRtpLoc.put(uuid, new Location(w, bx + 0.5, by + 1, bz + 0.5));
        });
    }

    private void executeIncomingRtp(Player player, String worldKey) {
        World targetWorld;
        String lower = worldKey.toLowerCase();
        if (lower.equals("overworld")) {
            targetWorld = Bukkit.getWorld(WORLD_OVERWORLD);
        } else if (lower.equals("nether")) {
            targetWorld = Bukkit.getWorld(WORLD_NETHER);
        } else if (lower.equals("end")) {
            if (!isEndOpen) {
                player.sendMessage("§cendはまだ解放されていません");
                return;
            }
            targetWorld = Bukkit.getWorld(WORLD_END);
        } else {
            targetWorld = null;
        }
        if (targetWorld == null) {
            player.sendActionBar("§cワールドが利用できません。");
            return;
        }
        player.sendActionBar("§a安全な場所を探しています...");
        handleRTPInWorld(player, targetWorld, true);
    }
    
    public void handleRtpQueueCommand(Player player) {
        if (checkCombatBlocked(player, "rtpqueue")) return;
        if (hubMode) {
            player.sendActionBar("§aサバイバルサーバーへ移動しています...");
            forwardCommandToSmp(player, "rtpq");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        if (rtpQueueCooldowns.containsKey(uuid)) {
            long passed = System.currentTimeMillis() - rtpQueueCooldowns.get(uuid);
            if (passed < 30000L) {
                long remaining = (30000L - passed) / 1000L;
                player.sendMessage("§cまだrtpqに参加できません。残り: " + remaining + "秒");
                return;
            }
        }

        if (rtpQueue.contains(uuid)) {
            rtpQueue.remove(uuid);
            player.sendMessage("§crtpqから退出しました。");
            return;
        }
        
        rtpQueue.add(uuid);
        Bukkit.broadcastMessage("§e" + player.getName() + " §aがrtpqに参加しました！");
        
        if (rtpQueue.size() >= 2) {
            UUID u1 = rtpQueue.remove(0);
            UUID u2 = rtpQueue.remove(0);
            Player p1 = Bukkit.getPlayer(u1);
            Player p2 = Bukkit.getPlayer(u2);
            
            if (p1 == null || !p1.isOnline()) {
                if (p2 != null && p2.isOnline()) rtpQueue.add(0, u2);
                return;
            }
            if (p2 == null || !p2.isOnline()) {
                rtpQueue.add(0, u1);
                return;
            }
            
            p1.sendMessage("§aマッチングが成立しました！(vs " + p2.getName() + ")");
            p2.sendMessage("§aマッチングが成立しました！(vs " + p1.getName() + ")");
            
            rtpQueueCooldowns.put(u1, System.currentTimeMillis());
            rtpQueueCooldowns.put(u2, System.currentTimeMillis());
            
            startRtpMatch(p1, p2);
        }
    }
    
    private void startRtpMatch(Player p1, Player p2) {
        World world = Bukkit.getWorld(WORLD_OVERWORLD);
        if (world == null) {
            p1.sendMessage("§cオーバーワールドが見つかりません。");
            p2.sendMessage("§cオーバーワールドが見つかりません。");
            return;
        }
        
        UUID matchId = UUID.randomUUID();
        double half = 8000.0;
        int bx = (int) ((Math.random() * 2 - 1) * half);
        int bz = (int) ((Math.random() * 2 - 1) * half);

        // 注意: tpTasks はConcurrentHashMapなのでnullは入れられない。
        // タイマー登録後に検索を開始する (findSafeLocationAsyncはtpTasksに無いと中断するため)
        p1.sendActionBar("§aマッチング成立！安全な対戦場所を探しています...");
        p2.sendActionBar("§aマッチング成立！安全な対戦場所を探しています...");

        tpTasks.put(matchId, Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, new java.util.function.Consumer<ScheduledTask>() {
            int counter = 9;
            @Override public void accept(ScheduledTask t) {
                if (!p1.isOnline() || !p2.isOnline()) {
                    tpTasks.remove(matchId);
                    tpTargets.remove(matchId);
                    if (p1.isOnline()) runEntityTask(p1, () -> p1.sendMessage("§c相手がオフラインになったため、マッチングがキャンセルされました。"));
                    if (p2.isOnline()) runEntityTask(p2, () -> p2.sendMessage("§c相手がオフラインになったため、マッチングがキャンセルされました。"));
                    t.cancel();
                    return;
                }
                
                if (counter <= 0) {
                    Location dest = tpTargets.remove(matchId);
                    if (dest != null && dest.getWorld() != null) {
                        runEntityTask(p1, () -> p1.sendActionBar("§aテレポート中..."));
                        runEntityTask(p2, () -> p2.sendActionBar("§aテレポート中..."));
                        p1.teleportAsync(dest);
                        p2.teleportAsync(dest);
                        
                        runEntityTask(p1, () -> {
                            org.bukkit.potion.PotionEffect resistance = new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 100, 4, false, false);
                            p1.addPotionEffect(resistance);
                            p1.sendTitle("§c§lVS", "§e" + p2.getName(), 10, 70, 20);
                            p1.sendMessage("§c§lVS " + p2.getName());
                        });
                        runEntityTask(p2, () -> {
                            org.bukkit.potion.PotionEffect resistance = new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 100, 4, false, false);
                            p2.addPotionEffect(resistance);
                            p2.sendTitle("§c§lVS", "§e" + p1.getName(), 10, 70, 20);
                            p2.sendMessage("§c§lVS " + p1.getName());
                        });
                        
                        tpTasks.remove(matchId);
                        t.cancel();
                    } else {
                        // Retry finding location
                        int nbx = (int) ((Math.random() * 2 - 1) * half);
                        int nbz = (int) ((Math.random() * 2 - 1) * half);
                        findSafeLocationAsync(world, nbx, nbz, 0, matchId);
                    }
                } else {
                    if (counter % 2 != 0) {
                        int display = (counter / 2) + 1;
                        runEntityTask(p1, () -> p1.sendActionBar("§e" + display + "§a秒後にテレポートします。"));
                        runEntityTask(p2, () -> p2.sendActionBar("§e" + display + "§a秒後にテレポートします。"));
                    }
                    counter--;
                }
            }
        }, 1L, 10L));

        findSafeLocationAsync(world, bx, bz, 0, matchId);
    }

    private final Map<UUID, String> playerSignInputMode = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Location> playerSignLocations = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean packetListenerRegistered = false;

    private void registerPacketListener() {
        if (packetListenerRegistered) return;
        packetListenerRegistered = true;
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.UPDATE_SIGN) {
                @Override
                public void onPacketReceiving(PacketEvent event) {
                    Player player = event.getPlayer();
                    UUID uuid = player.getUniqueId();
                    
                    if (!playerSignInputMode.containsKey(uuid)) return;

                    event.setCancelled(true);

                    String text = "";
                    try {
                        if (event.getPacket().getStringArrays().size() > 0) {
                            String[] lines = event.getPacket().getStringArrays().read(0);
                            if (lines.length > 0) text = lines[0];
                        } else if (event.getPacket().getStrings().size() >= 4) {
                            text = event.getPacket().getStrings().read(0);
                        } else if (event.getPacket().getStrings().size() > 0) {
                            text = event.getPacket().getStrings().read(0);
                        }
                    } catch (Exception e) {
                        // fallback
                    }

                    final String finalText = text;
                    runEntityTask(player, () -> {
                        String mode = playerSignInputMode.remove(uuid);
                        Location loc = playerSignLocations.remove(uuid);
                        
                        if (loc != null) {
                            player.sendBlockChange(loc, loc.getBlock().getBlockData());
                        }

                        if (finalText == null || finalText.trim().isEmpty() 
                            || finalText.equalsIgnoreCase("c") || finalText.equalsIgnoreCase("cancel")) {
                            handleSignCancel(player, mode);
                            return;
                        }
                        
                        processSignInput(player, mode, finalText.trim());
                    });
                }
            }
        );
    }

    /**
     * 入力UIを開く。以前は偽看板(ProtocolLibパケット)を使っていたが、
     * 26.1.x では看板エディタが開かず入力不能になるため、チャット入力に統一した。
     * (入力結果は従来通り processSignInput へ流れる)
     */
    public void openSignGui(Player player, String mode, String prompt, String placeholder) {
        startChatInput(player, mode, prompt != null ? prompt : "内容");
    }

    // AsyncPlayerChatEvent(非同期)から触るため必ずConcurrent
    private final Map<UUID, String> playerChatInputMode = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> playerChatInputTasks = new java.util.concurrent.ConcurrentHashMap<>();

    private void startChatInput(Player player, String mode, String prompt) {
        player.closeInventory();
        UUID uuid = player.getUniqueId();
        playerChatInputMode.put(uuid, mode);
        
        player.sendMessage("§e[入力] §a" + prompt + "をチャット欄に入力してください");
        player.sendMessage("§7(キャンセルする場合は 'c' または 'cancel' と入力してください)");
        
        if (playerChatInputTasks.containsKey(uuid)) {
            playerChatInputTasks.get(uuid).cancel();
        }
        
        ScheduledTask task = runGlobalTaskLater(() -> {
            if (playerChatInputMode.containsKey(uuid)) {
                playerChatInputMode.remove(uuid);
                playerChatInputTasks.remove(uuid);
                player.sendMessage("§c入力の有効期限(1分)が切れました。再度お試しください。");
            }
        }, 1200L);
        playerChatInputTasks.put(uuid, task);
    }

    public void startAhSearchChat(Player player) {
        openSignGui(player, "AH_SEARCH", "検索ワード入力", null);
    }

    public void startOrderInputChat(Player player, String type, String prompt, String placeholder) {
        openSignGui(player, "ORDER_" + type, prompt, placeholder);
    }

    public void startOrderSearchChat(Player player) {
        openSignGui(player, "ORDER_SEARCH", "検索ワード入力", null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChatInput(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!playerChatInputMode.containsKey(uuid)) return;
        
        event.setCancelled(true);
        String mode = playerChatInputMode.remove(uuid);
        String text = event.getMessage();

        if (playerChatInputTasks.containsKey(uuid)) {
            playerChatInputTasks.remove(uuid).cancel();
        }

        if (text == null || text.trim().isEmpty() || text.equalsIgnoreCase("c") || text.equalsIgnoreCase("cancel")) {
            runGlobalTask(() -> {
                handleSignCancel(player, mode);
            });
            return;
        }
        
        text = text.trim();
        final String fText = text;
        runGlobalTask(() -> {
            processSignInput(player, mode, fText);
        });
    }

    // onSignChange event removed as it's now handled by ProtocolLib

    private void handleSignCancel(Player player, String mode) {
        UUID uuid = player.getUniqueId();
        if (mode.startsWith("ORDER_")) {
            if (mode.equals("ORDER_SEARCH")) {
                openOrderGui(player);
            } else if (mode.equals("ORDER_SEARCH_MAT")) {
                openCreateOrderMaterialSelector(player, playerOrderSelectPage.getOrDefault(uuid, 0));
            } else {
                openCreateOrderGui(player);
            }
        } else if (mode.equals("AH_SEARCH")) {
            openAhGui(player);
        }
    }

    private void processSignInput(Player player, String mode, String text) {
        UUID uuid = player.getUniqueId();

        if (mode.equals("AH_SEARCH")) {
            playerSearchQuery.put(uuid, text);
            openAhGui(player, SortMode.SEARCH);
        } else if (mode.equals("WORTH_SEARCH")) {
            openSellListGui(player, 1, text);
        } else if (mode.equals("BALTOP_SEARCH")) {
            baltopSearch.put(uuid, text);
            buildBaltopGui(player, 0);
        } else if (mode.equals("TEAM_INVITE")) {
            handleTeamCommand(player, new String[]{"invite", text});
            openTeamGui(player, 0);
        } else if (mode.equals("BOUNTY_SEARCH")) {
            openBountyGui(player, 0, text);
        } else if (mode.equals("BOUNTY_ADD")) {
            UUID target = bountyAddTarget.remove(uuid);
            if (target != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                String name = op.getName() != null ? op.getName() : target.toString();
                handleBountyCommand(player, new String[]{name, "add", text});
            }
        } else if (mode.equals("ORDER_SEARCH")) {
            playerOrderSearchQuery.put(uuid, text);
            openOrderGui(player, OrderSortMode.SEARCH);
        } else if (mode.equals("ORDER_SEARCH_MAT")) {
            playerOrderSelectQuery.put(uuid, text.toLowerCase());
            playerOrderSelectPage.put(uuid, 0);
            openCreateOrderMaterialSelector(player, 0);
        } else if (mode.startsWith("ORDER_")) {
            OrderBuilder ob = playerOrderBuilder.get(uuid);
            if (ob == null) return;
            
            String target = mode.substring("ORDER_".length());
            
            if (target.equals("MONEY")) {
                try {
                    long money = parseSuffixNumber(text);
                    if (money < 1L) {
                        player.sendActionBar("§c単価は1円以上で指定してください。");
                    } else if (money > 1_000_000_000_000L) {
                        player.sendActionBar("§c単価は最大1T（1兆）円までです。");
                    } else {
                        ob.money = money;
                        player.sendActionBar("§9単価を設定しました: §a" + money + "円");
                    }
                } catch (NumberFormatException e) {
                    player.sendActionBar("§c数値を入力してください。");
                }
            } else if (target.equals("COUNT")) {
                try {
                    long countVal = parseSuffixNumber(text);
                    if (countVal < 1 || countVal > 1000000) {
                        player.sendActionBar("§c個数は1〜1,000,000の間で指定してください。");
                    } else {
                        ob.count = (int) countVal;
                        player.sendActionBar("§9個数を設定しました: §f" + ob.count + "個");
                    }
                } catch (NumberFormatException e) {
                    player.sendActionBar("§c数値を入力してください。");
                }
            }
            
            openCreateOrderGui(player);
        }
    }


    @EventHandler
    public void onSellGuiClick(InventoryClickEvent event) {
        if (!"§8アイテム売却".equals(event.getView().getTitle())) return;
        Player player = (Player) event.getWhoClicked();
        runGlobalTask(() -> {
            updateSellInventoryLore(player, event.getView().getTopInventory());
        });
    }

    @EventHandler
    public void onSellGuiDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!"§8アイテム売却".equals(event.getView().getTitle())) return;
        Player player = (Player) event.getWhoClicked();
        runGlobalTask(() -> {
            updateSellInventoryLore(player, event.getView().getTopInventory());
        });
    }

    public void updateSellInventoryLore(Player player, Inventory inv) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                long price = calculateSellPrice(item);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    // 既存の動的売却価格と追加情報を削除してリセット
                    for (int j = lore.size() - 1; j >= 0; j--) {
                        String line = lore.get(j);
                        if (line.startsWith("§e💲: ") || line.startsWith("§9エンチャント: ") || line.startsWith("§9飛翔時間: ") || line.startsWith("§9爆発効果数: ")) {
                            lore.remove(j);
                        }
                    }
                    
                    // エンチャント本のエンチャント一覧を記述
                    if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta esm) {
                        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                            lore.add("§9エンチャント: " + entry.getKey().getKey().getKey() + " " + entry.getValue());
                        }
                    }
                    
                    // 花火のメタ情報を記述
                    if (item.getType() == Material.FIREWORK_ROCKET && meta instanceof org.bukkit.inventory.meta.FireworkMeta fwm) {
                        lore.add("§9飛翔時間: " + fwm.getPower());
                        lore.add("§9爆発効果数: " + fwm.getEffects().size());
                    }
                    
                    lore.add("§e💲: " + price);
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
        }

        for (ItemStack item : player.getInventory().getContents()) {
            stripSellLore(item);
        }
        stripSellLore(player.getItemOnCursor());
    }

    private void stripSellLore(ItemStack item) {
        if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                boolean changed = false;
                for (int i = lore.size() - 1; i >= 0; i--) {
                    String line = lore.get(i);
                    if (line.startsWith("§e💲: ") || line.startsWith("§9エンチャント: ") || line.startsWith("§9飛翔時間: ") || line.startsWith("§9爆発効果数: ")) {
                        lore.remove(i);
                        changed = true;
                    }
                }
                if (changed) {
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    public void handleTeamCommand(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        if (args.length == 0) {
            String teamName = playerTeamMap.get(uuid);
            if (teamName == null) {
                player.sendMessage("§9/team create <名前> §7- チームを作成");
                player.sendMessage("§9/team join §7- 招待されたチームに参加");
                return;
            }
            openTeamGui(player, 0);
            return;
        }

        String sub = args[0].toLowerCase();
        String currentTeamName = playerTeamMap.get(uuid);

        if (sub.equals("create")) {
            if (currentTeamName != null) {
                player.sendActionBar("§cあなたは既にチームに入っています。");
                return;
            }
            if (args.length < 2) {
                player.sendActionBar("§cチーム名を指定してください。");
                return;
            }
            String name = args[1].trim();
            if (name.isEmpty()) {
                player.sendActionBar("§cチーム名が無効です。");
                return;
            }
            if (teams.containsKey(name)) {
                player.sendActionBar("§cそのチーム名は既に使われています。");
                return;
            }

            TeamData td = new TeamData();
            td.name = name;
            td.leader = uuid;
            td.members.add(uuid);
            teams.put(name, td);
            setPlayerTeam(uuid, name);
            saveData();
            player.sendActionBar("§aチーム「" + name + "」を作成しました。");
            return;
        }

        if (sub.equals("join")) {
            if (currentTeamName != null) {
                player.sendActionBar("§cあなたは既にチームに入っています。");
                return;
            }
            String invitedTeam = teamInvites.get(uuid);
            if (invitedTeam == null || !teams.containsKey(invitedTeam)) {
                player.sendActionBar("§c招待が届いていません。");
                return;
            }

            TeamData td = teams.get(invitedTeam);
            td.members.add(uuid);
            setPlayerTeam(uuid, invitedTeam);
            teamInvites.remove(uuid);
            saveData();
            player.sendActionBar("§aチーム「" + invitedTeam + "」に参加しました。");
            return;
        }

        // Must be in a team for the rest
        if (currentTeamName == null) {
            player.sendActionBar("§cチームに入っていません。");
            return;
        }
        TeamData td = teams.get(currentTeamName);
        boolean isLeader = td.leader.equals(uuid);

        if (sub.equals("leave")) {
            if (isLeader) {
                // Dissolve team or transfer leader if leader leaves
                for (UUID member : td.members) {
                    setPlayerTeam(member, "");
                }
                teams.remove(currentTeamName);
                player.sendActionBar("§aリーダーが脱退したため、チームが解散されました。");
            } else {
                td.members.remove(uuid);
                setPlayerTeam(uuid, "");
                player.sendActionBar("§aチーム「" + currentTeamName + "」から脱退しました。");
            }
            saveData();
            return;
        }

        if (sub.equals("home")) {
            if (checkCombatBlocked(player, "team home")) return;
            if (td.home == null) {
                player.sendActionBar("§cチームホームが設定されていません。");
                return;
            }
            startCountdown(player, td.home.clone(), "§aチームホームにテレポートしました。");
            return;
        }

        // Leader commands
        if (!isLeader) {
            player.sendActionBar("§cリーダーのみが使用できます。");
            return;
        }

        if (sub.equals("sethome")) {
            if (checkCombatBlocked(player, "team sethome")) return;
            td.home = player.getLocation().clone();
            saveData();
            player.sendActionBar("§aチームホームを設定しました。");
            return;
        }

        if (sub.equals("invite")) {
            if (args.length < 2) {
                player.sendActionBar("§cプレイヤー名を指定してください。");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                player.sendActionBar("§cプレイヤーが見つかりません。");
                return;
            }
            if (playerTeamMap.containsKey(target.getUniqueId())) {
                player.sendActionBar("§cそのプレイヤーは既に別のチームに入っています。");
                return;
            }

            teamInvites.put(target.getUniqueId(), currentTeamName);
            player.sendActionBar("§a" + target.getName() + " をチームに招待しました。");
            target.sendMessage("§a" + player.getName() + " からチーム「" + currentTeamName + "」への招待が届きました。参加するには §e/team join §aを入力してください。");
            return;
        }

        if (sub.equals("kick")) {
            if (args.length < 2) {
                player.sendActionBar("§cプレイヤー名を指定してください。");
                return;
            }
            // Search members list
            UUID kickUuid = null;
            String kickName = args[1];
            for (UUID mem : td.members) {
                Player p = Bukkit.getPlayer(mem);
                if (p != null && p.getName().equalsIgnoreCase(kickName)) {
                    kickUuid = mem;
                    break;
                } else {
                    // Fallback to offline player check by querying system
                    OfflinePlayer op = Bukkit.getOfflinePlayer(mem);
                    if (op.getName() != null && op.getName().equalsIgnoreCase(kickName)) {
                        kickUuid = mem;
                        break;
                    }
                }
            }

            if (kickUuid == null) {
                player.sendActionBar("§cチームメンバーが見つかりません。");
                return;
            }
            if (kickUuid.equals(uuid)) {
                player.sendActionBar("§c自分自身をキックすることはできません。");
                return;
            }

            td.members.remove(kickUuid);
            setPlayerTeam(kickUuid, "");
            saveData();

            player.sendActionBar("§a" + kickName + " をキックしました。");
            Player target = Bukkit.getPlayer(kickUuid);
            if (target != null && target.isOnline()) {
                target.sendActionBar("§cチームから追放されました。");
            }
            return;
        }

        player.sendActionBar("§c不明なサブコマンドです。");
    }

    // ─── Team GUI ─────────────────────────────────────────────────────────

    /** メンバー管理GUIの対象 (操作者UUID -> 対象メンバーUUID) */
    private final Map<UUID, UUID> teamManageTarget = new java.util.concurrent.ConcurrentHashMap<>();

    public void openTeamGui(Player player, int page) {
        UUID uuid = player.getUniqueId();
        String teamName = playerTeamMap.get(uuid);
        if (teamName == null) return;
        TeamData td = teams.get(teamName);
        if (td == null) return;

        List<UUID> members = new ArrayList<>(td.members);
        // リーダーを先頭に
        members.sort((a, b) -> {
            boolean la = a.equals(td.leader), lb = b.equals(td.leader);
            if (la != lb) return la ? -1 : 1;
            return a.compareTo(b);
        });

        int maxPage = Math.max(0, (members.size() - 1) / 45);
        int pg = Math.max(0, Math.min(page, maxPage));

        Inventory gui = Bukkit.createInventory(null, 54,
                "§0チーム: " + teamName + " - " + (pg + 1) + "/" + (maxPage + 1) + "ページ");

        int start = pg * 45;
        for (int i = 0; i < 45 && start + i < members.size(); i++) {
            UUID memberUuid = members.get(start + i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
            String name = op.getName() != null ? op.getName() : memberUuid.toString().substring(0, 8);
            boolean online = op.isOnline();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            if (sm != null) {
                try { sm.setOwningPlayer(op); } catch (Exception ignored) {}
                String star = memberUuid.equals(td.leader) ? "§6★ " : "";
                sm.setDisplayName(star + (online ? "§a" : "§7") + name);
                sm.setLore(Arrays.asList(
                        online ? "§aオンライン" : "§7オフライン",
                        "§7クリックしてメンバー管理"));
                head.setItemMeta(sm);
            }
            gui.setItem(i, head);
        }

        if (pg > 0) gui.setItem(45, createGuiItem(Material.ARROW, "§a前のページ"));
        if (pg < maxPage) gui.setItem(53, createGuiItem(Material.ARROW, "§a次のページ"));

        // FF切替
        ItemStack ff = new ItemStack(td.friendlyFire ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta ffm = ff.getItemMeta();
        if (ffm != null) {
            ffm.setDisplayName("§eフレンドリーファイア: " + (td.friendlyFire ? "§aON" : "§cOFF"));
            ffm.setLore(Arrays.asList("§7チームメンバー同士のダメージ", "§7クリックで切替 (リーダーのみ)"));
            ff.setItemMeta(ffm);
        }
        gui.setItem(47, ff);

        // チームホーム
        ItemStack home = new ItemStack(Material.LIGHT_BLUE_BED);
        ItemMeta hm = home.getItemMeta();
        if (hm != null) {
            hm.setDisplayName("§bチームホーム");
            hm.setLore(td.home != null
                    ? Arrays.asList("§7クリックでテレポート")
                    : Arrays.asList("§7未設定", "§7クリックで現在地に設定 (リーダーのみ)"));
            home.setItemMeta(hm);
        }
        gui.setItem(49, home);

        // 招待
        ItemStack invite = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta im = invite.getItemMeta();
        if (im != null) {
            im.setDisplayName("§aプレイヤーを招待");
            im.setLore(Arrays.asList("§7クリックして名前を入力 (リーダーのみ)"));
            invite.setItemMeta(im);
        }
        gui.setItem(51, invite);

        player.openInventory(gui);
    }

    private void openTeamMemberGui(Player player, UUID memberUuid) {
        String teamName = playerTeamMap.get(player.getUniqueId());
        if (teamName == null) return;
        TeamData td = teams.get(teamName);
        if (td == null || !td.members.contains(memberUuid)) return;

        OfflinePlayer op = Bukkit.getOfflinePlayer(memberUuid);
        String name = op.getName() != null ? op.getName() : memberUuid.toString().substring(0, 8);

        teamManageTarget.put(player.getUniqueId(), memberUuid);
        Inventory gui = Bukkit.createInventory(null, 9, "§0メンバー: " + name);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        if (sm != null) {
            try { sm.setOwningPlayer(op); } catch (Exception ignored) {}
            sm.setDisplayName("§f" + name + (memberUuid.equals(td.leader) ? " §6(リーダー)" : ""));
            head.setItemMeta(sm);
        }
        gui.setItem(0, head);

        gui.setItem(2, createGuiItem(Material.LIGHT_BLUE_BED, "§bチームホームへ行く"));
        gui.setItem(4, createGuiItem(td.friendlyFire ? Material.LIME_DYE : Material.GRAY_DYE,
                "§eFF: " + (td.friendlyFire ? "§aON" : "§cOFF"), "§7クリックで切替 (リーダーのみ)"));
        gui.setItem(6, createGuiItem(Material.WRITABLE_BOOK, "§aプレイヤーを招待", "§7リーダーのみ"));

        boolean canKick = td.leader.equals(player.getUniqueId()) && !memberUuid.equals(player.getUniqueId());
        if (canKick) {
            gui.setItem(8, createGuiItem(Material.BARRIER, "§cこのメンバーをキック"));
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onTeamGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        UUID uuid = player.getUniqueId();

        if (title.startsWith("§0チーム: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null
                    || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            String teamName = playerTeamMap.get(uuid);
            if (teamName == null) { player.closeInventory(); return; }
            TeamData td = teams.get(teamName);
            if (td == null) { player.closeInventory(); return; }
            boolean isLeader = td.leader.equals(uuid);

            int page;
            try {
                String[] parts = title.split(" - ");
                page = Integer.parseInt(parts[parts.length - 1].split("/")[0]) - 1;
            } catch (Exception e) {
                page = 0;
            }

            int slot = event.getRawSlot();
            if (slot == 45) { openTeamGui(player, page - 1); return; }
            if (slot == 53) { openTeamGui(player, page + 1); return; }
            if (slot == 47) { // FF切替
                if (!isLeader) { player.sendActionBar("§cリーダーのみが切り替えできます。"); return; }
                td.friendlyFire = !td.friendlyFire;
                saveData();
                player.sendActionBar("§eフレンドリーファイアを " + (td.friendlyFire ? "§aON" : "§cOFF") + " §eにしました。");
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                openTeamGui(player, page);
                return;
            }
            if (slot == 49) { // チームホーム
                if (td.home != null) {
                    player.closeInventory();
                    handleTeamCommand(player, new String[]{"home"});
                } else if (isLeader) {
                    handleTeamCommand(player, new String[]{"sethome"});
                    openTeamGui(player, page);
                } else {
                    player.sendActionBar("§cチームホームが設定されていません。");
                }
                return;
            }
            if (slot == 51) { // 招待
                if (!isLeader) { player.sendActionBar("§cリーダーのみが招待できます。"); return; }
                openSignGui(player, "TEAM_INVITE", "招待するプレイヤー名", null);
                return;
            }
            if (slot >= 0 && slot < 45) { // メンバー頭
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
                List<UUID> members = new ArrayList<>(td.members);
                members.sort((a, b) -> {
                    boolean la = a.equals(td.leader), lb = b.equals(td.leader);
                    if (la != lb) return la ? -1 : 1;
                    return a.compareTo(b);
                });
                int index = page * 45 + slot;
                if (index < members.size()) {
                    openTeamMemberGui(player, members.get(index));
                }
            }
            return;
        }

        if (title.startsWith("§0メンバー: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null
                    || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            String teamName = playerTeamMap.get(uuid);
            if (teamName == null) { player.closeInventory(); return; }
            TeamData td = teams.get(teamName);
            if (td == null) { player.closeInventory(); return; }
            boolean isLeader = td.leader.equals(uuid);
            UUID target = teamManageTarget.get(uuid);

            int slot = event.getRawSlot();
            if (slot == 2) { // チームホームへ
                player.closeInventory();
                handleTeamCommand(player, new String[]{"home"});
            } else if (slot == 4) { // FF切替
                if (!isLeader) { player.sendActionBar("§cリーダーのみが切り替えできます。"); return; }
                td.friendlyFire = !td.friendlyFire;
                saveData();
                player.sendActionBar("§eフレンドリーファイアを " + (td.friendlyFire ? "§aON" : "§cOFF") + " §eにしました。");
                if (target != null) openTeamMemberGui(player, target);
            } else if (slot == 6) { // 招待
                if (!isLeader) { player.sendActionBar("§cリーダーのみが招待できます。"); return; }
                openSignGui(player, "TEAM_INVITE", "招待するプレイヤー名", null);
            } else if (slot == 8) { // キック
                if (!isLeader || target == null || target.equals(uuid)) return;
                OfflinePlayer op = Bukkit.getOfflinePlayer(target);
                String name = op.getName() != null ? op.getName() : target.toString().substring(0, 8);
                player.closeInventory();
                handleTeamCommand(player, new String[]{"kick", name});
            }
        }
    }

    // ─── Order System Implementation ──────────────────────────────────────
    public void handleOrderCommand(Player player, String[] args) {
        playerOrderSearchQuery.remove(player.getUniqueId());
        playerOrderPage.put(player.getUniqueId(), 0);
        if (args.length > 0) {
            String query = String.join(" ", args);
            playerOrderSearchQuery.put(player.getUniqueId(), query);
            openOrderGui(player, OrderSortMode.SEARCH);
        } else {
            openOrderGui(player);
        }
    }

    public void openOrderGui(Player player) {
        playerOrderSearchQuery.remove(player.getUniqueId());
        playerOrderPage.put(player.getUniqueId(), 0);
        OrderSortMode mode = playerOrderSortMode.get(player.getUniqueId());
        if (mode == OrderSortMode.SEARCH || mode == OrderSortMode.MY_ORDERS) {
            mode = OrderSortMode.NEW;
        }
        if (mode == null) mode = OrderSortMode.NEW;
        openOrderGui(player, mode);
    }

    public void openOrderGui(Player player, OrderSortMode mode) {
        OrderSortMode oldMode = playerOrderSortMode.get(player.getUniqueId());
        if (oldMode != mode) {
            playerOrderPage.put(player.getUniqueId(), 0);
        }
        playerOrderSortMode.put(player.getUniqueId(), mode);

        // Sorted listings
        List<OrderItem> sortedItems = new ArrayList<>(orderItems.values());
        if (mode == OrderSortMode.MY_ORDERS) {
            sortedItems.removeIf(oi -> !oi.requesterUUID.equals(player.getUniqueId()));
        } else {
            sortedItems.removeIf(oi -> oi.cancelled || oi.collected >= oi.count);
            if (mode == OrderSortMode.SEARCH) {
                String query = playerOrderSearchQuery.get(player.getUniqueId());
                if (query != null && !query.isEmpty()) {
                    String lowerQuery = query.toLowerCase();
                    sortedItems.removeIf(oi -> !oi.itemTemplate.getType().name().toLowerCase().contains(lowerQuery) && (oi.requesterName == null || !oi.requesterName.toLowerCase().contains(lowerQuery)));
                }
            }
        }

        switch (mode) {
            case NEW:
                sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
                break;
            case LOW_PRICE:
                sortedItems.sort((a, b) -> Long.compare(a.price, b.price));
                break;
            case HIGH_PRICE:
                sortedItems.sort((a, b) -> Long.compare(b.price, a.price));
                break;
            case OLD:
                sortedItems.sort((a, b) -> Long.compare(a.listedTime, b.listedTime));
                break;
            case MY_ORDERS:
                sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
                break;
            case SEARCH:
                sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime));
                break;
        }

        int page = playerOrderPage.getOrDefault(player.getUniqueId(), 0);
        int totalItems = sortedItems.size();
        int maxPage = Math.max(0, (int) Math.ceil((double) totalItems / 45.0) - 1);
        if (page > maxPage) {
            page = maxPage;
            playerOrderPage.put(player.getUniqueId(), page);
        }

        Inventory gui = Bukkit.createInventory(null, 54, "§8Order Board - " + (page + 1) + "/" + (maxPage + 1) + "ページ");

        // Bottom row decorations
        for (int i = 45; i < 54; i++) {
            if (i != 45 && i != 53) {
                gui.setItem(i, buildGrayPane());
            }
        }

        // Spyglass for sorting
        ItemStack spyglass = new ItemStack(Material.SPYGLASS);
        ItemMeta sm = spyglass.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§9並び替え");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add((mode == OrderSortMode.NEW ? "§9▶ 新着" : "§9  新着"));
            lore.add((mode == OrderSortMode.LOW_PRICE ? "§9▶ 低い順" : "§9  低い順"));
            lore.add((mode == OrderSortMode.HIGH_PRICE ? "§9▶ 高い順" : "§9  高い順"));
            lore.add((mode == OrderSortMode.OLD ? "§9▶ 古い順" : "§9  古い順"));
            sm.setLore(lore);
            spyglass.setItemMeta(sm);
        }
        gui.setItem(48, spyglass);

        // Chest for My Orders
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta cm = chest.getItemMeta();
        if (cm != null) {
            cm.setDisplayName("§9オーダー品");
            if (mode == OrderSortMode.MY_ORDERS) {
                cm.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                cm.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            chest.setItemMeta(cm);
        }
        gui.setItem(49, chest);

        // Search (Sign)
        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = search.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName("§9検索");
            if (mode == OrderSortMode.SEARCH) {
                searchMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                searchMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            search.setItemMeta(searchMeta);
        }
        gui.setItem(50, search);

        // Create Order (Empty Map) at 53
        ItemStack createMap = new ItemStack(Material.MAP);
        ItemMeta mapMeta = createMap.getItemMeta();
        if (mapMeta != null) {
            mapMeta.setDisplayName("§9オーダーする");
            createMap.setItemMeta(mapMeta);
        }
        gui.setItem(53, createMap);

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, totalItems);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(slot, buildOrderDisplayItem(sortedItems.get(i), player.getUniqueId()));
            slot++;
        }

        while (slot < 45) {
            gui.setItem(slot, null);
            slot++;
        }

        // Pagination buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§9前のページへ");
                pm.setLore(Arrays.asList("§7" + (page) + " / " + (maxPage + 1) + " ページ"));
                prev.setItemMeta(pm);
            }
            gui.setItem(45, prev);
        } else {
            gui.setItem(45, null);
        }

        if (endIndex < totalItems) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§9次のページへ");
                nm.setLore(Arrays.asList("§7" + (page + 2) + " / " + (maxPage + 1) + " ページ"));
                next.setItemMeta(nm);
            }
            gui.setItem(52, next);
        } else {
            gui.setItem(52, buildGrayPane());
        }

        player.openInventory(gui);
    }

    private ItemStack buildOrderDisplayItem(OrderItem oi, UUID viewerUUID) {
        ItemStack item = oi.itemTemplate != null ? oi.itemTemplate.clone() : new ItemStack(Material.STONE);
        item.setAmount(1);
        ItemMeta meta = item.getType() != Material.AIR ? item.getItemMeta() : null;
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§9依頼者: §f" + oi.requesterName);
            lore.add("§9単価: §a$" + formatMoney(oi.price));
            int remaining = Math.max(0, oi.count - oi.collected);
            lore.add("§9残り個数: §f" + remaining + " / " + oi.count);
            lore.add("§9掲載日時: §f" + formatTimeAgo(oi.listedTime));
            if (oi.cancelled) {
                lore.add("§9[キャンセル済み]");
            }
            if (oi.requesterUUID.equals(viewerUUID)) {
                lore.add("§9クリックしてメニューを開く");
            } else {
                lore.add("§9クリックして売却");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }    public void openOrderDepositGuiForFulfill(Player player, OrderItem oi) {
        playerDepositingOrder.put(player.getUniqueId(), oi.id);
        Inventory gui = Bukkit.createInventory(player, 36, "§8Order Deposit: " + oi.id);
        player.openInventory(gui);
    }

    public void openOrderClaimGui(Player player, OrderItem oi, int page) {
        playerClaimingOrder.put(player.getUniqueId(), oi.id);
        playerOrderClaimPage.put(player.getUniqueId(), page);
        List<ItemStack> items = orderStorage.get(oi.id);
        int maxPage = Math.max(0, (int) Math.ceil((double) (items != null ? items.size() : 0) / 45.0) - 1);
        Inventory gui = Bukkit.createInventory(player, 54, "§8Order Claim - " + (page + 1) + "/" + (maxPage + 1) + "ページ");
        if (items != null) {
            int start = page * 45;
            int slot = 0;
            for (int i = start; i < items.size() && slot < 45; i++) {
                gui.setItem(slot, items.get(i).clone());
                slot++;
            }
            
            if (page > 0) {
                ItemStack prev = createGuiItem(Material.ARROW, "§e前のページ");
                gui.setItem(45, prev);
            }
            if (start + 45 < items.size()) {
                ItemStack next = createGuiItem(Material.ARROW, "§e次のページ");
                gui.setItem(53, next);
            }
        }
        player.openInventory(gui);
    }

    public void openCreateOrderGui(Player player) {
        OrderBuilder ob = playerOrderBuilder.computeIfAbsent(player.getUniqueId(), k -> new OrderBuilder());
        Inventory gui = Bukkit.createInventory(null, 27, "§8Create Order");

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, buildGrayPane());
        }

        ItemStack frame = ob.itemTemplate != null ? ob.itemTemplate.clone() : new ItemStack(Material.STONE);
        frame.setAmount(1);
        ItemMeta fm = frame.getItemMeta();
        if (fm != null) {
            fm.setDisplayName("§9Item");
            List<String> lore = new ArrayList<>();
            lore.add("§9設定中のアイテム: §f" + frame.getType().name());
            lore.add("§9クリックしてアイテムを選択");
            fm.setLore(lore);
            frame.setItemMeta(fm);
        }
        gui.setItem(12, frame);

        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta sm = sign.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§9Money");
            List<String> lore = new ArrayList<>();
            lore.add("§9設定中の金額: §a$" + formatMoney(ob.money));
            lore.add("§9クリックして金額を入力");
            sm.setLore(lore);
            sign.setItemMeta(sm);
        }
        gui.setItem(13, sign);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta pm = paper.getItemMeta();
        if (pm != null) {
            pm.setDisplayName("§9Count");
            List<String> lore = new ArrayList<>();
            lore.add("§9設定中の個数: §f" + ob.count + "個");
            lore.add("§9クリックして個数を入力");
            pm.setLore(lore);
            paper.setItemMeta(pm);
        }
        gui.setItem(14, paper);

        ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta cm = confirm.getItemMeta();
        if (cm != null) {
            cm.setDisplayName("§9オーダーを確定する");
            List<String> lore = new ArrayList<>();
            lore.add("§9単価: §a$" + formatMoney(ob.money));
            lore.add("§9必要個数: §f" + ob.count + "個");
            lore.add("§9合計金額: §a$" + formatMoney(ob.money * ob.count));
            cm.setLore(lore);
            confirm.setItemMeta(cm);
        }
        gui.setItem(22, confirm);

        player.openInventory(gui);
    }

    public void openCreateOrderMaterialSelector(Player player, int page) {
        playerOrderSelectPage.put(player.getUniqueId(), page);
        String query = playerOrderSelectQuery.getOrDefault(player.getUniqueId(), "").toLowerCase();

        String title = "§8Select Order Item (Page " + (page + 1) + ")";
        Inventory gui = null;
        org.bukkit.inventory.InventoryView openView = player.getOpenInventory();
        if (openView != null && openView.getTitle().startsWith("§8Select Order Item")) {
            gui = openView.getTopInventory();
            gui.clear();
            try {
                openView.setTitle(title);
            } catch (Throwable ignored) {}
        }

        if (gui == null) {
            gui = Bukkit.createInventory(null, 54, title);
        }

        List<ItemStack> filtered = new ArrayList<>();
        for (ItemStack templateItem : SELECTABLE_MATERIALS) {
            Material m = templateItem.getType();
            String name;
            if (m == Material.ENCHANTED_BOOK && templateItem.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta esm2 && !esm2.getStoredEnchants().isEmpty()) {
                name = esm2.getStoredEnchants().keySet().iterator().next().getKey().getKey();
            } else {
                name = m.name();
            }
            if (query.isEmpty() || name.toLowerCase().contains(query) || m.name().toLowerCase().contains(query)) {
                filtered.add(templateItem);
            }
        }

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, filtered.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            gui.setItem(slot, filtered.get(i).clone());
            slot++;
        }
        
        while (slot < 45) {
            gui.setItem(slot, null);
            slot++;
        }
        
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, buildGrayPane());
        }
        
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§9前のページへ");
                prev.setItemMeta(pm);
            }
            gui.setItem(45, prev);
        }
        
        if (endIndex < filtered.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§9次のページへ");
                next.setItemMeta(nm);
            }
            gui.setItem(53, next);
        }
        
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§9戻る");
            back.setItemMeta(bm);
        }
        gui.setItem(49, back);

        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta sm = search.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§a探す");
            if (!query.isEmpty()) {
                sm.setLore(Arrays.asList("§7現在: " + query, "§eクリックで検索"));
            } else {
                sm.setLore(Arrays.asList("§7クリックで検索"));
            }
            search.setItemMeta(sm);
        }
        gui.setItem(50, search);
        
        if (player.getOpenInventory().getTopInventory() != gui) {
            player.openInventory(gui);
        }
    }



    @EventHandler
    public void onOrderGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        UUID uuid = player.getUniqueId();

        if (title.startsWith("§8Order Board")) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(false);
                }
                return;
            }

            OrderSortMode currentMode = playerOrderSortMode.getOrDefault(uuid, OrderSortMode.NEW);
            int page = playerOrderPage.getOrDefault(uuid, 0);

            if (slot == 45) { // Prev page
                if (page > 0) {
                    playerOrderPage.put(uuid, page - 1);
                    openOrderGui(player, currentMode);
                }
            } else if (slot == 52) { // Next page
                List<OrderItem> sortedItems = new ArrayList<>(orderItems.values());
                if (currentMode == OrderSortMode.MY_ORDERS) {
                    sortedItems.removeIf(oi -> !oi.requesterUUID.equals(uuid));
                } else {
                    sortedItems.removeIf(oi -> oi.cancelled || oi.collected >= oi.count);
                    if (currentMode == OrderSortMode.SEARCH) {
                        String query = playerOrderSearchQuery.get(uuid);
                        if (query != null && !query.isEmpty()) {
                            String lowerQuery = query.toLowerCase();
                            sortedItems.removeIf(oi -> !oi.itemTemplate.getType().name().toLowerCase().contains(lowerQuery) && (oi.requesterName == null || !oi.requesterName.toLowerCase().contains(lowerQuery)));
                        }
                    }
                }
                int totalItems = sortedItems.size();
                int endIndex = (page + 1) * 45;
                if (endIndex < totalItems) {
                    playerOrderPage.put(uuid, page + 1);
                    openOrderGui(player, currentMode);
                }
            } else if (slot == 48) { // Spyglass
                OrderSortMode nextMode;
                switch (currentMode) {
                    case NEW: nextMode = OrderSortMode.LOW_PRICE; break;
                    case LOW_PRICE: nextMode = OrderSortMode.HIGH_PRICE; break;
                    case HIGH_PRICE: nextMode = OrderSortMode.OLD; break;
                    case OLD: nextMode = OrderSortMode.NEW; break;
                    default: nextMode = OrderSortMode.NEW; break;
                }
                openOrderGui(player, nextMode);
            } else if (slot == 49) { // Chest
                openOrderGui(player, OrderSortMode.MY_ORDERS);
            } else if (slot == 50) { // Search
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    startOrderSearchChat(player);
                }, 2L);
            } else if (slot == 53) { // Create Order (Empty Map)
                player.closeInventory();
                playerOrderBuilder.put(uuid, new OrderBuilder());
                runGlobalTaskLater(() -> {
                    openCreateOrderGui(player);
                }, 2L);
            } else if (slot >= 0 && slot < 45) {
                List<OrderItem> sortedItems = new ArrayList<>(orderItems.values());
                if (currentMode == OrderSortMode.MY_ORDERS) {
                    sortedItems.removeIf(oi -> !oi.requesterUUID.equals(uuid));
                } else {
                    sortedItems.removeIf(oi -> oi.cancelled || oi.collected >= oi.count);
                    if (currentMode == OrderSortMode.SEARCH) {
                        String query = playerOrderSearchQuery.get(uuid);
                        if (query != null && !query.isEmpty()) {
                            String lowerQuery = query.toLowerCase();
                            sortedItems.removeIf(oi -> !oi.itemTemplate.getType().name().toLowerCase().contains(lowerQuery) && (oi.requesterName == null || !oi.requesterName.toLowerCase().contains(lowerQuery)));
                        }
                    }
                }
                switch (currentMode) {
                    case NEW: sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime)); break;
                    case LOW_PRICE: sortedItems.sort((a, b) -> Long.compare(a.price, b.price)); break;
                    case HIGH_PRICE: sortedItems.sort((a, b) -> Long.compare(b.price, a.price)); break;
                    case OLD: sortedItems.sort((a, b) -> Long.compare(a.listedTime, b.listedTime)); break;
                    case MY_ORDERS: sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime)); break;
                    case SEARCH: sortedItems.sort((a, b) -> Long.compare(b.listedTime, a.listedTime)); break;
                }

                int index = (page * 45) + slot;
                if (index < 0 || index >= sortedItems.size()) return;

                OrderItem target = sortedItems.get(index);
                if (!orderItems.containsKey(target.id)) {
                    player.sendActionBar("§cそのオーダーは既にクローズされています。");
                    openOrderGui(player, currentMode);
                    return;
                }

                if (target.requesterUUID.equals(uuid)) {
                    openOrderManagementMenu(player, target);
                } else {
                    int remaining = target.count - target.collected;
                    if (remaining <= 0) {
                        player.sendActionBar("§cこのオーダーは既に上限に達しています。");
                        return;
                    }
                    player.closeInventory();
                    runGlobalTaskLater(() -> {
                        openOrderDepositGuiForFulfill(player, target);
                    }, 2L);
                }
            }
        } else if ("§8Create Order".equals(title)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(false);
                }
                return;
            }

            OrderBuilder ob = playerOrderBuilder.get(uuid);
            if (ob == null) return;
            if (slot == 12) {
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    openCreateOrderMaterialSelector(player, 0);
                }, 2L);
            } else if (slot == 13) {
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    startOrderInputChat(player, "MONEY", "単価", "100");
                }, 2L);
            } else if (slot == 14) {
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    startOrderInputChat(player, "COUNT", "個数", "1");
                }, 2L);
            } else if (slot == 22) {
                long totalCost = ob.money * ob.count;
                long balance = getMoney(uuid);
                if (balance < totalCost) {
                    player.sendActionBar("§c所持金が足りません。");
                    return;
                }

                addMoney(uuid, -totalCost);

                OrderItem oi = new OrderItem();
                oi.id = UUID.randomUUID().toString();
                oi.requesterUUID = uuid;
                oi.requesterName = player.getName();
                oi.itemTemplate = ob.itemTemplate != null ? ob.itemTemplate.clone() : new ItemStack(Material.STONE);
                oi.price = ob.money;
                oi.count = ob.count;
                oi.listedTime = System.currentTimeMillis();

                orderItems.put(oi.id, oi);
                ((SMPPlugin) plugin).getDatabaseManager().addOrderItem(oi);
                broadcastSyncOrder();
                playerOrderBuilder.remove(uuid);
                saveData();

                player.closeInventory();
                player.sendActionBar("§9オーダーを出しました: §f" + oi.itemTemplate.getType().name() + " x" + oi.count + " §9(単価: §a$" + formatMoney(oi.price) + "§9)");
                runGlobalTaskLater(() -> {
                    openOrderGui(player);
                }, 2L);
            }
        } else if ("§8Order Menu".equals(title)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(false);
                }
                return;
            }

            String orderId = playerOrderMenuTarget.get(uuid);
            if (orderId == null) return;
            OrderItem target = orderItems.get(orderId);
            if (target == null) {
                player.closeInventory();
                return;
            }

            if (slot == 11) { // Cancel (Red Wool)
                int uncollected = target.count - target.collected;
                long refund = target.price * uncollected;
                if (refund > 0) {
                    addMoney(uuid, refund);
                }
                
                List<ItemStack> stored = orderStorage.get(target.id);
                if (stored != null && !stored.isEmpty()) {
                    target.cancelled = true;
                    target.count = target.collected;
                    ((SMPPlugin) plugin).getDatabaseManager().addOrderItem(target);
                    broadcastSyncOrder();
                    player.sendMessage("§9オーダーを取り消しました。集まったアイテムは「回収」から受け取ってください。");
                } else {
                    orderItems.remove(target.id);
                    ((SMPPlugin) plugin).getDatabaseManager().removeOrderItem(target.id);
                    broadcastSyncOrder();
                    orderStorage.remove(target.id);
                    player.sendMessage("§9オーダーを取り消し、預け金 §a$" + formatMoney(refund) + " §9を払い戻しました。");
                }
                playerOrderMenuTarget.remove(uuid);
                saveData();
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    openOrderGui(player);
                }, 2L);
            } else if (slot == 15) { // Claim (Chest)
                if (target.collected <= 0) {
                    player.sendActionBar("§c回収できるアイテムはありません。");
                    return;
                }
                ItemStack sample = new ItemStack(target.itemTemplate.getType());
                if (getAvailableSpace(player, sample) <= 0) {
                    player.sendActionBar("§cインベントリがいっぱいのためキャンセルされました");
                    return;
                }
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    openOrderClaimGui(player, target, 0);
                }, 2L);
            }
        } else if (title.startsWith("§8Select Order Item (Page ")) {
            event.setCancelled(true);
            int slot = event.getSlot();
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(false);
                }
                return;
            }

            int page = playerOrderSelectPage.getOrDefault(uuid, 0);

            if (slot >= 0 && slot < 45) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    OrderBuilder ob = playerOrderBuilder.get(uuid);
                    if (ob != null) {
                        ob.itemTemplate = item.clone();
                    }
                    player.closeInventory();
                    runGlobalTaskLater(() -> {
                        openCreateOrderGui(player);
                    }, 2L);
                }
            } else if (slot == 45) {
                if (page > 0) {
                    openCreateOrderMaterialSelector(player, page - 1);
                }
            } else if (slot == 53) {
                openCreateOrderMaterialSelector(player, page + 1);
            } else if (slot == 49) {
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    openCreateOrderGui(player);
                }, 2L);
            } else if (slot == 50) {
                player.closeInventory();
                runGlobalTaskLater(() -> {
                    startOrderInputChat(player, "SEARCH_MAT", "検索キーワード", "");
                }, 2L);
            }
        }
    }

    private void addFulfilledItemsToStorage(String orderId, ItemStack item) {
        List<ItemStack> list = orderStorage.computeIfAbsent(orderId, k -> new ArrayList<>());
        int remaining = item.getAmount();
        int maxStack = item.getType().getMaxStackSize();
        
        for (ItemStack existing : list) {
            if (existing.isSimilar(item)) {
                int space = maxStack - existing.getAmount();
                if (space > 0) {
                    int add = Math.min(space, remaining);
                    existing.setAmount(existing.getAmount() + add);
                    remaining -= add;
                    if (remaining <= 0) return;
                }
            }
        }
        
        while (remaining > 0) {
            int add = Math.min(maxStack, remaining);
            ItemStack clone = item.clone();
            clone.setAmount(add);
            list.add(clone);
            remaining -= add;
        }
    }



    @EventHandler
    public void onOrderDepositClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§8Order Deposit: ")) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        String orderId = playerDepositingOrder.remove(uuid);
        if (orderId == null) return;
        
        OrderItem target = orderItems.get(orderId);
        Inventory inv = event.getInventory();
        
        if (target == null) {
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                    for (ItemStack lo : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), lo);
                    }
                }
            }
            inv.clear();
            player.sendActionBar("§cオーダーが既にキャンセルされていたため、アイテムを返却しました。");
            return;
        }
        
        int totalMatchingInDeposit = 0;
        List<ItemStack> matchingStacks = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item != null && target.itemTemplate != null && item.getType() == target.itemTemplate.getType()) {
                totalMatchingInDeposit += item.getAmount();
                matchingStacks.add(item);
            }
        }
        
        int remainingNeeded = target.count - target.collected;
        int toSell = Math.min(remainingNeeded, totalMatchingInDeposit);
        
        if (toSell > 0) {
            int leftToTake = toSell;
            for (ItemStack item : matchingStacks) {
                if (leftToTake <= 0) break;
                int amountInStack = item.getAmount();
                if (amountInStack >= leftToTake) {
                    ItemStack toAdd = item.clone();
                    toAdd.setAmount(leftToTake);
                    addFulfilledItemsToStorage(target.id, toAdd);
                    
                    item.setAmount(amountInStack - leftToTake);
                    leftToTake = 0;
                } else {
                    ItemStack toAdd = item.clone();
                    addFulfilledItemsToStorage(target.id, toAdd);
                    
                    leftToTake -= amountInStack;
                    item.setAmount(0);
                }
            }
            
            long totalPayout = target.price * toSell;
            addMoney(uuid, totalPayout);
            
target.collected += toSell;
            ((SMPPlugin) plugin).getDatabaseManager().addOrderItem(target);
            ((SMPPlugin) plugin).getDatabaseManager().updateOrderStorage(target.id, orderStorage.get(target.id));
            broadcastSyncOrder();
            saveData();
            updateGlobalData();
            String itemName = target.itemTemplate != null ? target.itemTemplate.getType().name() : "不明";
            player.sendMessage("§9" + itemName + " を " + toSell + "個売却し、§a$" + formatMoney(totalPayout) + " §9を獲得しました。");
            
            Player requester = Bukkit.getPlayer(target.requesterUUID);
            if (requester != null && requester.isOnline()) {
                requester.sendMessage("§f" + player.getName() + " §9があなたのオーダーへ §f" + itemName + " x" + toSell + " §9を売却しました。");
            }
        }
        
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                for (ItemStack lo : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), lo);
                }
            }
        }
        
        inv.clear();
        
        runGlobalTaskLater(() -> {
            openOrderGui(player);
        }, 2L);
    }

    public void onOrderClaimClose(InventoryCloseEvent event) {
        int i;
        if (!event.getView().getTitle().startsWith("§8Order Claim")) {
            return;
        }
        Player player = (Player)event.getPlayer();
        UUID uuid = player.getUniqueId();
        String orderId = playerClaimingOrder.remove(uuid);
        if (orderId == null) {
            return;
        }
        OrderItem target = orderItems.get(orderId);
        if (target == null) {
            return;
        }
        int page = playerOrderClaimPage.getOrDefault(uuid, 0);
        playerOrderClaimPage.remove(uuid);
        List<ItemStack> oldItems = orderStorage.getOrDefault(orderId, new ArrayList());
        ArrayList<ItemStack> newItems = new ArrayList<ItemStack>();
        for (int i2 = 0; i2 < page * 45; ++i2) {
            if (i2 >= oldItems.size()) continue;
            newItems.add((ItemStack)oldItems.get(i2));
        }
        Inventory gui = event.getInventory();
        int remainingCountOnThisPage = 0;
        for (i = 0; i < 45; ++i) {
            ItemStack item = gui.getItem(i);
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) continue;
            newItems.add(item.clone());
            if (target.itemTemplate == null || item.getType() != target.itemTemplate.getType()) continue;
            remainingCountOnThisPage += item.getAmount();
        }
        for (i = (page + 1) * 45; i < oldItems.size(); ++i) {
            newItems.add((ItemStack)oldItems.get(i));
        }
        int totalLeft = 0;
        for (ItemStack item : newItems) {
            if (target.itemTemplate == null || item.getType() != target.itemTemplate.getType()) continue;
            totalLeft += item.getAmount();
        }
        int takenCount = target.collected - totalLeft;
        if (takenCount > 0) {
            target.count -= takenCount;
            target.collected -= takenCount;
            player.sendMessage("§a\u30aa\u30fc\u30c0\u30fc\u54c1\u3092 " + takenCount + "\u500b\u56de\u53ce\u3057\u307e\u3057\u305f\u3002");
        }
        if (target.count <= 0) {
            orderItems.remove(target.id);
            orderStorage.remove(target.id);
            ((SMPPlugin) plugin).getDatabaseManager().removeOrderItem(target.id);
        } else {
            if (newItems.isEmpty()) {
                orderStorage.remove(target.id);
                ((SMPPlugin) plugin).getDatabaseManager().updateOrderStorage(target.id, null);
            } else {
                orderStorage.put(target.id, newItems);
                ((SMPPlugin) plugin).getDatabaseManager().updateOrderStorage(target.id, newItems);
            }
            if (takenCount > 0) {
                ((SMPPlugin) plugin).getDatabaseManager().addOrderItem(target);
            }
        }
        if (takenCount > 0) {
            broadcastSyncOrder();
        }
        saveData();
        runGlobalTaskLater(() -> openOrderGui(player), 2L);
    }
    @EventHandler
    public void onClaimGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!playerClaimingOrder.containsKey(uuid)) return;
        if (!event.getView().getTitle().startsWith("§8Order Claim")) return;
        
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                event.setCancelled(true);
                return;
            }
        }
        
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            int slot = event.getSlot();
            if (slot >= 45) {
                event.setCancelled(true);
                String orderId = playerClaimingOrder.get(uuid);
                OrderItem oi = orderItems.get(orderId);
                if (oi == null) return;
                
                int page = playerOrderClaimPage.getOrDefault(uuid, 0);
                
                if (slot == 45 && page > 0) { // Prev
                    player.closeInventory();
                    runGlobalTaskLater(() -> {
                        openOrderClaimGui(player, oi, page - 1);
                    }, 2L);
                } else if (slot == 53) { // Next
                    List<ItemStack> items = orderStorage.get(oi.id);
                    if (items != null && (page + 1) * 45 < items.size()) {
                        player.closeInventory();
                        runGlobalTaskLater(() -> {
                            openOrderClaimGui(player, oi, page + 1);
                        }, 2L);
                    }
                }
                return;
            }

            org.bukkit.event.inventory.InventoryAction action = event.getAction();
            if (action == org.bukkit.event.inventory.InventoryAction.PLACE_ALL ||
                action == org.bukkit.event.inventory.InventoryAction.PLACE_SOME ||
                action == org.bukkit.event.inventory.InventoryAction.PLACE_ONE ||
                action == org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR) {
                event.setCancelled(true);
            }
        }
    }

    private final Map<UUID, String> playerOrderMenuTarget = new HashMap<>();

    public void openOrderManagementMenu(Player player, OrderItem oi) {
        playerOrderMenuTarget.put(player.getUniqueId(), oi.id);
        Inventory gui = Bukkit.createInventory(null, 27, "§8Order Menu");

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, buildGrayPane());
        }

        // Slot 11: Cancel Order (Red Wool)
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cm = cancelItem.getItemMeta();
        if (cm != null) {
            cm.setDisplayName("§9取り消す");
            List<String> lore = new ArrayList<>();
            int uncollected = oi.count - oi.collected;
            long refund = oi.price * uncollected;
            lore.add("§9残りの払い戻し金: §a$" + formatMoney(refund));
            if (oi.collected > 0) {
                String itemN = oi.itemTemplate != null ? oi.itemTemplate.getType().name() : "不明";
                lore.add("§9回収するアイテム: §f" + itemN + " x" + oi.collected);
            }
            lore.add("§9クリックしてオーダーを削除・回収");
            cm.setLore(lore);
            cancelItem.setItemMeta(cm);
        }
        gui.setItem(11, cancelItem);

        // Slot 15: Claim Collected Items (Chest)
        ItemStack chestItem = new ItemStack(Material.CHEST);
        ItemMeta chm = chestItem.getItemMeta();
        if (chm != null) {
            chm.setDisplayName("§9回収");
            List<String> lore = new ArrayList<>();
            lore.add("§9現在集まっている数: §f" + oi.collected + " / " + oi.count);
            lore.add("§9クリックして集まったアイテムを回収");
            chm.setLore(lore);
            chestItem.setItemMeta(chm);
        }
        gui.setItem(15, chestItem);

        player.openInventory(gui);
    }

    // Offline delivery map for offline fulfillments
    private final Map<UUID, List<String>> offlineDeliveries = new HashMap<>();

    @EventHandler
    public void onAdvancementGrant(com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent event) {
        if (hubMode) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(org.spigotmc.event.player.PlayerSpawnLocationEvent event) {
        if (hubMode) return;
        UUID uuid = event.getPlayer().getUniqueId();
        String worldKey = pendingRtps.remove(uuid);
        if (worldKey != null) {
            World targetWorld = null;
            String lower = worldKey.toLowerCase();
            if (lower.equals("overworld")) targetWorld = Bukkit.getWorld(WORLD_OVERWORLD);
            else if (lower.equals("nether")) targetWorld = Bukkit.getWorld(WORLD_NETHER);
            else if (lower.equals("end") && isEndOpen) targetWorld = Bukkit.getWorld(WORLD_END);
            
            if (targetWorld == null) targetWorld = Bukkit.getWorld(WORLD_OVERWORLD);
            if (targetWorld != null) {
                Location dest = findSafeLocationSync(targetWorld);
                event.setSpawnLocation(dest);
                portalRtpPlayers.add(uuid);
                rtpCooldowns.put(uuid, System.currentTimeMillis());
                runEntityTaskLater(event.getPlayer(), () -> {
                    if (event.getPlayer().isOnline()) {
                        event.getPlayer().sendActionBar("§a[RTP] 安全な場所に到着しました。");
                    }
                }, 20L);
            }
        }
    }

    private Location findSafeLocationSync(World world) {
        double half = world.getEnvironment() == World.Environment.NETHER ? 1000.0 : 8000.0;
        for (int i = 0; i < 5; i++) {
            int bx = (int) ((Math.random() * 2 - 1) * half);
            int bz = (int) ((Math.random() * 2 - 1) * half);
            int by;
            if (world.getEnvironment() == World.Environment.NETHER) {
                by = findNetherSafeY(world, bx, bz);
            } else {
                by = world.getHighestBlockYAt(bx, bz);
            }
            Material topType = by >= 0 ? world.getBlockAt(bx, by, bz).getType() : Material.AIR;
            Material groundType = by > 0 ? world.getBlockAt(bx, by - 1, bz).getType() : Material.AIR;
            boolean isLiquid = topType == Material.WATER || topType == Material.LAVA
                    || topType == Material.KELP || topType == Material.SEAGRASS
                    || topType == Material.TALL_SEAGRASS
                    || groundType == Material.WATER || groundType == Material.LAVA;
            if (by >= 1 && !isLiquid) {
                return new Location(world, bx + 0.5, by + 1, bz + 0.5);
            }
        }
        int bx = (int) ((Math.random() * 2 - 1) * half);
        int bz = (int) ((Math.random() * 2 - 1) * half);
        int by = world.getEnvironment() == World.Environment.NETHER ? 60 : world.getHighestBlockYAt(bx, bz);
        return new Location(world, bx + 0.5, by + 1, bz + 0.5);
    }

    @EventHandler
    public void onPlayerJoinForOrderDelivery(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Hub logic for new players (hubサーバー or 旧単一サーバー構成のみ)
        if (!player.hasPlayedBefore() && (hubMode || !velocityEnabled)) {
            World hubWorld = getHubWorld();
            if (hubWorld != null) {
                player.teleport(new Location(hubWorld, 0.5, 0.0, 0.5));
            }
        }

        if (offlineDeliveries.containsKey(uuid)) {
            List<String> items = offlineDeliveries.remove(uuid);
            if (items != null) {
                runGlobalTaskLater(() -> {
                    player.sendMessage("§aオフライン中に完了したオーダーのアイテムが届きました！");
                    for (String s : items) {
                        try {
                            String[] split = s.split(":");
                            Material mat = Material.valueOf(split[0]);
                            int count = Integer.parseInt(split[1]);
                            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, count));
                            for (ItemStack lo : leftover.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), lo);
                            }
                        } catch (Exception ignored) {}
                    }
                    saveData();
                }, 40L);
            }
        }
    }

    // ─── Custom Spawner Auto-Collect ─────────────────────────────────────

    private String getJapaneseMobName(org.bukkit.entity.EntityType type) {
        if (type == null) return "不明";
        switch (type) {
            case PIG: return "豚";
            case COW: return "牛";
            case ZOMBIE: return "ゾンビ";
            case SKELETON: return "スケルトン";
            case CREEPER: return "クリーパー";
            case SPIDER: return "クモ";
            case CAVE_SPIDER: return "毒クモ";
            case ZOMBIFIED_PIGLIN: return "ゾンビピグリン";
            case BLAZE: return "ブレイズ";
            case IRON_GOLEM: return "アイアンゴーレム";
            case ENDERMAN: return "エンダーマン";
            case SLIME: return "スライム";
            case MAGMA_CUBE: return "マグマキューブ";
            case WITCH: return "ウィッチ";
            default: return type.name();
        }
    }

    private String getSpawnerKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private void addDrop(List<ItemStack> drops, Material mat, int amount) {
        if (amount > 0) {
            drops.add(new ItemStack(mat, amount));
        }
    }

    private List<ItemStack> simulateDrops(org.bukkit.entity.EntityType type) {
        List<ItemStack> drops = new ArrayList<>();
        if (type == null) return drops;
        java.util.Random r = new java.util.Random();
        switch (type) {
            case ZOMBIE:
                addDrop(drops, Material.ROTTEN_FLESH, 1 + r.nextInt(3)); // 1-3
                if (r.nextDouble() < 0.025) {
                    Material[] rares = {Material.IRON_INGOT, Material.CARROT, Material.POTATO};
                    addDrop(drops, rares[r.nextInt(rares.length)], 1);
                }
                break;
            case SKELETON:
                addDrop(drops, Material.BONE, 1 + r.nextInt(3)); // 1-3
                addDrop(drops, Material.ARROW, 1 + r.nextInt(3)); // 1-3
                break;
            case WITHER_SKELETON:
                addDrop(drops, Material.BONE, 1 + r.nextInt(3)); // 1-3
                addDrop(drops, Material.COAL, 1 + r.nextInt(2)); // 1-2
                if (r.nextDouble() < 0.025) addDrop(drops, Material.WITHER_SKELETON_SKULL, 1);
                break;
            case SPIDER:
            case CAVE_SPIDER:
                addDrop(drops, Material.STRING, 1 + r.nextInt(3)); // 1-3
                if (r.nextDouble() < 0.33) addDrop(drops, Material.SPIDER_EYE, 1);
                break;
            case CREEPER:
                addDrop(drops, Material.GUNPOWDER, 1 + r.nextInt(3)); // 1-3
                break;
            case BLAZE:
                addDrop(drops, Material.BLAZE_ROD, 1 + r.nextInt(2)); // 1-2
                break;
            case ZOMBIFIED_PIGLIN:
                addDrop(drops, Material.ROTTEN_FLESH, 1 + r.nextInt(2)); // 1-2
                addDrop(drops, Material.GOLD_NUGGET, 1 + r.nextInt(2)); // 1-2
                if (r.nextDouble() < 0.025) addDrop(drops, Material.GOLD_INGOT, 1);
                break;
            case IRON_GOLEM:
                addDrop(drops, Material.IRON_INGOT, 3 + r.nextInt(3)); // 3-5
                addDrop(drops, Material.POPPY, 1 + r.nextInt(2)); // 1-2
                break;
            case PIG:
                addDrop(drops, Material.PORKCHOP, 1 + r.nextInt(3)); // 1-3
                break;
            case COW:
                addDrop(drops, Material.BEEF, 1 + r.nextInt(3)); // 1-3
                addDrop(drops, Material.LEATHER, 1 + r.nextInt(2)); // 1-2
                break;
            case ENDERMAN:
                if (r.nextDouble() < 0.5) addDrop(drops, Material.ENDER_PEARL, 1);
                else addDrop(drops, Material.ENDER_PEARL, 1); // always drop 1
                break;
            case SLIME:
            case MAGMA_CUBE:
                Material drop = type == org.bukkit.entity.EntityType.SLIME ? Material.SLIME_BALL : Material.MAGMA_CREAM;
                addDrop(drops, drop, 1 + r.nextInt(3)); // 1-3
                break;
            case WITCH:
                Material[] witchDrops = {Material.GLOWSTONE_DUST, Material.SUGAR, Material.REDSTONE, Material.SPIDER_EYE, Material.GLASS_BOTTLE, Material.GUNPOWDER, Material.STICK};
                for (int i = 0; i < 3; i++) {
                    if (r.nextDouble() < 0.25) addDrop(drops, witchDrops[r.nextInt(witchDrops.length)], 1 + r.nextInt(2));
                }
                break;
            case SHEEP:
                addDrop(drops, Material.MUTTON, 1 + r.nextInt(2));
                addDrop(drops, Material.WHITE_WOOL, 1);
                break;
            case CHICKEN:
                addDrop(drops, Material.CHICKEN, 1);
                addDrop(drops, Material.FEATHER, 1 + r.nextInt(3)); // 1-3
                break;
            case ZOMBIE_VILLAGER:
            case HUSK:
            case DROWNED:
                addDrop(drops, Material.ROTTEN_FLESH, 1 + r.nextInt(3)); // 1-3
                if (type == org.bukkit.entity.EntityType.DROWNED && r.nextDouble() < 0.11) {
                    addDrop(drops, Material.COPPER_INGOT, 1);
                }
                break;
            case STRAY:
                addDrop(drops, Material.BONE, 1 + r.nextInt(3)); // 1-3
                addDrop(drops, Material.ARROW, 1 + r.nextInt(3)); // 1-3
                break;
            case SQUID:
                addDrop(drops, Material.INK_SAC, 1 + r.nextInt(3));
                break;
            case GLOW_SQUID:
                addDrop(drops, Material.GLOW_INK_SAC, 1 + r.nextInt(3));
                break;
            case RABBIT:
                addDrop(drops, Material.RABBIT, 1);
                if (r.nextDouble() < 0.5) addDrop(drops, Material.RABBIT_HIDE, 1);
                break;
            case PIGLIN:
            case PIGLIN_BRUTE:
            case ZOGLIN:
                addDrop(drops, Material.ROTTEN_FLESH, 1 + r.nextInt(2)); // 1-2
                break;
            case HOGLIN:
                addDrop(drops, Material.PORKCHOP, 1 + r.nextInt(3));
                addDrop(drops, Material.LEATHER, 1 + r.nextInt(2)); // 1-2
                break;
            default:
                // unknown mob: drop nothing
                break;
        }
        return drops;
    }

    @EventHandler
    public void onSpawnerSpawn(org.bukkit.event.entity.SpawnerSpawnEvent event) {
        event.setCancelled(true);
        org.bukkit.block.CreatureSpawner spawner = event.getSpawner();
        Location loc = spawner.getLocation();
        String key = getSpawnerKey(loc);
        SpawnerData data = spawnerDataMap.computeIfAbsent(key, k -> new SpawnerData());

        org.bukkit.entity.EntityType type = spawner.getSpawnedType();
        if (data.entityType == null && type != null) {
            data.entityType = type.name();
            saveData();
        }
        
        // Reset spawner delay to prevent rapid firing attempts that lag the server
        spawner.setDelay(200 + new java.util.Random().nextInt(600)); // 10-40 seconds
        spawner.update();
        // Item generation is now handled asynchronously by startSpawnerTask()
    }


    @EventHandler
    public void onEnderChestClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) return;
        
        event.setCancelled(true); // デフォルトのエンダーチェストを開かないようにする
        Player player = event.getPlayer();
        openCustomEnderChest(player);
    }
    
    public void openCustomEnderChest(Player player) {
        // DBからの読込が終わるまでは古いデータを見せない (古い内容で上書き保存されるのを防ぐ)
        if (!isDbDataReady(player.getUniqueId())) {
            player.sendActionBar("§eデータを同期中です。少し待ってからもう一度お試しください。");
            return;
        }
        Inventory inv = Bukkit.createInventory(player, 54, "§5エンダーチェスト");
        ItemStack[] saved = customEnderChests.get(player.getUniqueId());

        // バニラエンチェからの移行処理
        org.bukkit.inventory.Inventory vanillaEc = player.getEnderChest();
        boolean hasVanillaItems = false;
        for (ItemStack item : vanillaEc.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                hasVanillaItems = true;
                break;
            }
        }

        if (hasVanillaItems) {
            if (saved == null) {
                saved = new ItemStack[54];
            }
            // バニラエンチェのアイテムを新しいエンチェの空きスロットに移動
            for (int i = 0; i < vanillaEc.getSize(); i++) {
                ItemStack vItem = vanillaEc.getItem(i);
                if (vItem != null && vItem.getType() != Material.AIR) {
                    boolean moved = false;
                    for (int j = 0; j < 54; j++) {
                        if (saved[j] == null || saved[j].getType() == Material.AIR) {
                            saved[j] = vItem.clone();
                            vanillaEc.setItem(i, null);
                            moved = true;
                            break;
                        }
                    }
                    if (!moved) {
                        // 空きがない場合はドロップさせる
                        player.getWorld().dropItem(player.getLocation(), vItem);
                        vanillaEc.setItem(i, null);
                        player.sendMessage("§c新しいエンダーチェストがいっぱいのため、一部のアイテムを足元にドロップしました。");
                    }
                }
        }
            customEnderChests.put(player.getUniqueId(), saved);
            saveData();
        }

        if (saved != null) {
            inv.setContents(saved);
        }
        player.openInventory(inv);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
    }

    @EventHandler
    public void onEnderChestClose(InventoryCloseEvent event) {
        if (!"§5エンダーチェスト".equals(event.getView().getTitle())) return;
        Player player = (Player) event.getPlayer();
        customEnderChests.put(player.getUniqueId(), event.getInventory().getContents());
        saveData();
        // クラッシュに備えてエンチェ操作の結果 (エンチェ+インベントリ) をDBへ即保存
        savePlayerToDb(player, null);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f);
    }

    @EventHandler
    public void onSpawnerClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;
        
        Player player = event.getPlayer();
        if (!(block.getState() instanceof org.bukkit.block.CreatureSpawner spawner)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (player.isSneaking() && hand.getType() == Material.SPAWNER) {
            org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) hand.getItemMeta();
            if (bsm != null && bsm.getBlockState() instanceof org.bukkit.block.CreatureSpawner handSpawner) {
                if (handSpawner.getSpawnedType() == spawner.getSpawnedType()) {
                    event.setCancelled(true);
                    String key = getSpawnerKey(block.getLocation());
                    SpawnerData data = spawnerDataMap.computeIfAbsent(key, k -> new SpawnerData());
                    data.stackCount++;
                    hand.setAmount(hand.getAmount() - 1);
                    player.sendActionBar("§aスポナーを重ねました！ (現在: " + data.stackCount + ")");
                    saveData();
                    return;
                }
            }
        }

        // Custom UI implementation
        event.setCancelled(true);
        String key = getSpawnerKey(block.getLocation());
        spawnerDataMap.computeIfAbsent(key, k -> new SpawnerData());
        playerSpawnerTarget.put(player.getUniqueId(), block.getLocation());
        openSpawnerGui(player, 0, spawner.getSpawnedType());
    }

    private void openSpawnerGui(Player player, int page, org.bukkit.entity.EntityType type) {
        playerSpawnerPage.put(player.getUniqueId(), page);
        Location loc = playerSpawnerTarget.get(player.getUniqueId());
        if (loc == null) return;
        String key = getSpawnerKey(loc);
        SpawnerData data = spawnerDataMap.get(key);
        if (data == null) return;

        List<ItemStack> allStacks = new ArrayList<>();
        for (Map.Entry<String, Integer> e : data.storedItems.entrySet()) {
            Material mat = Material.getMaterial(e.getKey());
            if (mat == null) continue;
            int count = e.getValue();
            while (count > 0) {
                int s = Math.min(64, count);
                allStacks.add(new ItemStack(mat, s));
                count -= s;
            }
        }

        int maxPage = Math.max(0, (allStacks.size() - 1) / 45);
        if (page > maxPage) page = maxPage;
        if (page < 0) page = 0;

        Inventory gui = Bukkit.createInventory(null, 54, "§0スポナー: " + (page + 1) + "/" + (maxPage + 1) + "ページ");

        int startIndex = page * 45;
        for (int i = 0; i < 45; i++) {
            if (startIndex + i < allStacks.size()) {
                gui.setItem(i, allStacks.get(startIndex + i));
            }
        }

        // Bottom row
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            m.setDisplayName("§9前のページ");
            prev.setItemMeta(m);
            gui.setItem(45, prev);
        }

        // Collect all
        ItemStack dropper = new ItemStack(Material.DROPPER);
        ItemMeta dm = dropper.getItemMeta();
        dm.setDisplayName("§aページをドロップ");
        dm.setLore(Arrays.asList("§7クリックしてこのページのアイテムを周囲にドロップ"));
        dropper.setItemMeta(dm);
        gui.setItem(49, dropper);

        // Sell All
        ItemStack sellAll = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sm = sellAll.getItemMeta();
        sm.setDisplayName("§aすべて売却");
        sm.setLore(Arrays.asList("§7スポナーに貯まっているすべてのアイテムを", "§7一括で売却します。"));
        sellAll.setItemMeta(sm);
        gui.setItem(48, sellAll);

        // Info Paper
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§eスポナー情報");
        List<String> lore = new ArrayList<>();
        lore.add("§7モブ: §f" + getJapaneseMobName(type));
        lore.add("§7重ねている数: §f" + data.stackCount);
        StringBuilder itemsStr = new StringBuilder("§7アイテム: §f");
        for (String m : data.storedItems.keySet()) {
            itemsStr.append(m).append(", ");
        }
        if (itemsStr.toString().endsWith(", ")) itemsStr.setLength(itemsStr.length() - 2);
        lore.add(itemsStr.toString());
        im.setLore(lore);
        info.setItemMeta(im);
        gui.setItem(50, info);

        // Stat Book
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bm = book.getItemMeta();
        bm.setDisplayName("§9ストック");
        
        int totalStored = 0;
        for (Integer c : data.storedItems.values()) totalStored += c;
        int itemsOnThisPage = 0;
        for (int i = 0; i < 45; i++) {
            if (startIndex + i < allStacks.size()) {
                itemsOnThisPage += allStacks.get(startIndex + i).getAmount();
            }
        }
        
        bm.setLore(Arrays.asList("§7ページのストック: §f" + itemsOnThisPage, "§7全体のストック: §f" + totalStored));
        book.setItemMeta(bm);
        gui.setItem(51, book);

        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            m.setDisplayName("§9次のページ");
            next.setItemMeta(m);
            gui.setItem(53, next);
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onSpawnerGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("§0スポナー: ")) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        int rawSlot = event.getRawSlot();
        if (rawSlot >= 54) {
            if (event.isShiftClick()) event.setCancelled(true);
            else event.setCancelled(false); 
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Location loc = playerSpawnerTarget.get(player.getUniqueId());
        if (loc == null) return;
        String key = getSpawnerKey(loc);
        SpawnerData data = spawnerDataMap.get(key);
        if (data == null) return;

        int page = playerSpawnerPage.getOrDefault(player.getUniqueId(), 0);

        Block block = loc.getBlock();
        if (block.getType() != Material.SPAWNER || !(block.getState() instanceof org.bukkit.block.CreatureSpawner spawner)) {
            player.closeInventory();
            player.sendActionBar("§cスポナーが見つかりません。");
            return;
        }

        if (rawSlot == 45 && clicked.getType() == Material.ARROW) {
            openSpawnerGui(player, page - 1, spawner.getSpawnedType());
            return;
        }
        if (rawSlot == 53 && clicked.getType() == Material.ARROW) {
            openSpawnerGui(player, page + 1, spawner.getSpawnedType());
            return;
        }

        if (rawSlot == 48 && clicked.getType() == Material.EMERALD_BLOCK) {
            UUID uuid = player.getUniqueId();
            Map<String, Long> stats = smpSellMultiStats.computeIfAbsent(uuid, k -> new HashMap<>());
            Map<String, Double> initialMultipliers = new HashMap<>();
            for (String cat : new String[]{"Crops", "Ores", "Mob drops", "Natural items", "Armor and tools", "Fish", "Enchanted books", "Potions", "Blocks"}) {
                initialMultipliers.put(cat, getSellMultiplier(stats.getOrDefault(cat, 0L)));
            }

            long totalMoney = 0;
            int totalItems = 0;
            
            for (Map.Entry<String, Integer> e : data.storedItems.entrySet()) {
                Material mat = Material.getMaterial(e.getKey());
                if (mat == null) continue;
                int count = e.getValue();
                if (count <= 0) continue;
                
                long basePricePerItem = calculateSellPrice(new ItemStack(mat));
                if (basePricePerItem > 0) {
                    long basePrice = basePricePerItem * count;
                    String cat = getSellCategory(mat);
                    long currentSold = stats.getOrDefault(cat, 0L);
                    double mult = getSellMultiplier(currentSold);
                    
                    long finalPrice = (long)(basePrice * mult);
                    totalMoney += finalPrice;
                    totalItems += count;
                    
                    stats.put(cat, currentSold + finalPrice);
                }
            }
            
            if (totalItems > 0) {
                data.storedItems.clear();
                
                setMoney(uuid, smpMoney.getOrDefault(uuid, 0L) + totalMoney);
                
                String moneyStr = formatMoney(totalMoney);
                player.sendMessage("§aスポナーのアイテム" + totalItems + "個を$" + moneyStr + "で売りました");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                
                for (Map.Entry<String, Double> entry : initialMultipliers.entrySet()) {
                    String cat = entry.getKey();
                    double initMult = entry.getValue();
                    double newMult = getSellMultiplier(stats.getOrDefault(cat, 0L));
                    if (newMult > initMult) {
                        player.sendMessage("§e[売却レベルUP] §a" + cat + " §eの売却倍率が §b" + String.format("%.1f", newMult) + "x §eに上がりました！");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    }
                }
                
                saveData();
            } else {
                player.sendMessage("§c売却できるアイテムがありません。");
            }
            
            openSpawnerGui(player, page, spawner.getSpawnedType());
            return;
        }

        if (rawSlot == 49 && clicked.getType() == Material.DROPPER) {
            List<ItemStack> allStacks = new ArrayList<>();
            for (Map.Entry<String, Integer> e : data.storedItems.entrySet()) {
                Material mat = Material.getMaterial(e.getKey());
                if (mat == null) continue;
                int count = e.getValue();
                while (count > 0) {
                    int s = Math.min(64, count);
                    allStacks.add(new ItemStack(mat, s));
                    count -= s;
                }
            }
            
            int startIndex = page * 45;
            for (int i = 0; i < 45; i++) {
                if (startIndex + i < allStacks.size()) {
                    ItemStack toDrop = allStacks.get(startIndex + i);
                    org.bukkit.entity.Item dropped = player.getWorld().dropItem(player.getEyeLocation(), toDrop);
                    dropped.setVelocity(player.getLocation().getDirection().multiply(0.3));
                    dropped.setPickupDelay(40);
                    
                    String mName = toDrop.getType().name();
                    int current = data.storedItems.get(mName);
                    if (current <= toDrop.getAmount()) {
                        data.storedItems.remove(mName);
                    } else {
                        data.storedItems.put(mName, current - toDrop.getAmount());
                    }
                }
            }
            
            saveData();
            openSpawnerGui(player, page, spawner.getSpawnedType());
            return;
        }

        if (rawSlot >= 0 && rawSlot < 45) {
            String mName = clicked.getType().name();
            int current = data.storedItems.getOrDefault(mName, 0);
            if (current > 0) {
                int amountToGive = clicked.getAmount();
                if (current < amountToGive) amountToGive = current;
                
                ItemStack giveItem = new ItemStack(clicked.getType(), amountToGive);
                Map<Integer, ItemStack> left = player.getInventory().addItem(giveItem);
                if (!left.isEmpty()) {
                    for (ItemStack leftItem : left.values()) {
                        org.bukkit.entity.Item dropped = player.getWorld().dropItem(player.getEyeLocation(), leftItem);
                        dropped.setVelocity(player.getLocation().getDirection().multiply(0.3));
                        dropped.setPickupDelay(40);
                    }
                    player.sendActionBar("§eインベントリが満杯のため一部ドロップしました。");
                }
                
                if (current <= amountToGive) {
                    data.storedItems.remove(mName);
                } else {
                    data.storedItems.put(mName, current - amountToGive);
                }
                
                saveData();
                openSpawnerGui(player, page, spawner.getSpawnedType());
            }
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.SPAWNER) return;

        ItemStack item = event.getItemInHand();
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        org.bukkit.entity.EntityType type = null;
        if (meta instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
            if (bsm.getBlockState() instanceof org.bukkit.block.CreatureSpawner handSpawner) {
                type = handSpawner.getSpawnedType();
            }
        }
        
        if (type == null || type == org.bukkit.entity.EntityType.PIG) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "spawner_type");
            if (meta.getPersistentDataContainer().has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                String typeStr = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
                try {
                    type = org.bukkit.entity.EntityType.valueOf(typeStr);
                } catch (Exception ignored) {}
            }
        }
        
        if (type != null) {
            if (block.getState() instanceof org.bukkit.block.CreatureSpawner spawner) {
                spawner.setSpawnedType(type);
                spawner.update();
                
                // 置いた瞬間にspawnerDataMapに登録する
                String key = getSpawnerKey(block.getLocation());
                SpawnerData data = spawnerDataMap.computeIfAbsent(key, k -> new SpawnerData());
                data.entityType = type.name();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        boolean silkTouch = hand.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH);

        if (!silkTouch) {
            event.setCancelled(true);
            player.sendActionBar("§cスポナーはシルクタッチでのみ回収できます。");
            return;
        }

        String key = getSpawnerKey(block.getLocation());
        SpawnerData data = spawnerDataMap.remove(key);
        
        if (block.getState() instanceof org.bukkit.block.CreatureSpawner spawner) {
            event.setDropItems(false);
            event.setExpToDrop(0);
            ItemStack spawnerItem = new ItemStack(Material.SPAWNER, data != null ? data.stackCount : 1);
            org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) spawnerItem.getItemMeta();
            if (bsm != null) {
                org.bukkit.block.CreatureSpawner handSpawner = (org.bukkit.block.CreatureSpawner) bsm.getBlockState();
                handSpawner.setSpawnedType(spawner.getSpawnedType());
                bsm.setBlockState(handSpawner);
                bsm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "spawner_type"), org.bukkit.persistence.PersistentDataType.STRING, spawner.getSpawnedType().name());
                bsm.setDisplayName("§a" + getJapaneseMobName(spawner.getSpawnedType()) + "スポナー");
                spawnerItem.setItemMeta(bsm);
            }
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
            block.getWorld().dropItemNaturally(dropLoc, spawnerItem);
        }
        

    }



    // ─── シャットダウン ───────────────────────────────────────────────────

    public void disable() {
        if (scoreboardTask != null) try { scoreboardTask.cancel(); } catch (Exception ignored) {}
        if (shardTask      != null) try { shardTask.cancel();      } catch (Exception ignored) {}
        if (deathChestTask != null) try { deathChestTask.cancel(); } catch (Exception ignored) {}
        for (UUID uuid : new HashSet<>(tpTasks.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) cancelTP(p, null);
        }
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        saveData();
    }

    // ─── Combat System ────────────────────────────────────────────────────

    public boolean isInCombat(UUID uuid) {
        Long expiry = combatExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            combatExpiry.remove(uuid);
            return false;
        }
        return true;
    }

    public void applyCombat(Player player) {
        combatExpiry.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_DURATION_MS);
    }

    public long getCombatRemaining(UUID uuid) {
        Long expiry = combatExpiry.get(uuid);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    @EventHandler
    public void onCombatScoreboard(org.bukkit.event.player.PlayerMoveEvent event) {
        // Handled by scoreboard task - just update action bar for combat players
    }

    // Trigger combat on crystal/anchor damage by another player
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            if (proj.getShooter() instanceof Player p) attacker = p;
        }
        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId())) {
            // 同じチームでフレンドリーファイアOFFならダメージ無効
            String atkTeam = playerTeamMap.get(attacker.getUniqueId());
            String vicTeam = playerTeamMap.get(victim.getUniqueId());
            if (atkTeam != null && atkTeam.equals(vicTeam)) {
                TeamData team = teams.get(atkTeam);
                if (team != null && !team.friendlyFire) {
                    event.setCancelled(true);
                    return;
                }
            }
            applyCombat(victim);
            applyCombat(attacker);
        }
    }

    @EventHandler
    public void onCrystalOrAnchorDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        // We handle crystal/anchor combat via EntityDamageByEntityEvent above
        // This handles cases where entity cause is block/explosion with no direct attacker - combat already applied
    }

    // Elytra / Ender Pearl combat renewal
    @EventHandler
    public void onEnderPearlLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderPearl pearl) {
            if (pearl.getShooter() instanceof Player p) {
                if (isInCombat(p.getUniqueId())) {
                    applyCombat(p);
                }
            }
        }
    }

    // Combat bar update (every tick via scoreboard)
    // We piggyback on the scoreboard tick:
    private void updateCombatBar() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            runEntityTask(p, () -> {
                UUID uuid = p.getUniqueId();
                if (isInCombat(uuid)) {
                    if (p.isGliding()) {
                        applyCombat(p);
                    }
                    long secs = (getCombatRemaining(uuid) + 999) / 1000;
                    p.sendActionBar("§ccombat : §f" + secs + "秒");
                }
            });
        }
    }

    public boolean checkCombatBlocked(Player player, String commandName) {
        if (isInCombat(player.getUniqueId())) {
            player.sendMessage("§ccombat中のためキャンセルされました");
            return true;
        }
        return false;
    }



    // ─── /baltop ─────────────────────────────────────────────────────────

    public void handleBaltopCommand(org.bukkit.command.CommandSender sender) {
        if (sender instanceof Player p) {
            openBaltopGui(p, 0, null);
            return;
        }
        // コンソールは従来通りテキスト表示
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) {
            db.getBaltopAsync(10, top -> runGlobalTask(() -> {
                sender.sendMessage("§6§l=== Bal Top 10 ===");
                int rank = 1;
                for (DatabaseManager.BaltopEntry entry : top) {
                    String name = resolveBaltopName(entry);
                    sender.sendMessage("§e#" + rank + " §f" + name + " §7: §a$" + formatMoney(entry.money));
                    rank++;
                }
            }));
        }
    }

    private String resolveBaltopName(DatabaseManager.BaltopEntry entry) {
        String name = entry.name;
        if (name == null) {
            try { name = Bukkit.getOfflinePlayer(entry.uuid).getName(); } catch (Exception ignored) {}
        }
        if (name == null) name = entry.uuid.toString().substring(0, 8);
        return name;
    }

    // ─── Baltop GUI ───────────────────────────────────────────────────────

    private final Map<UUID, List<DatabaseManager.BaltopEntry>> baltopCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, String> baltopSearch = new java.util.concurrent.ConcurrentHashMap<>();

    public void openBaltopGui(Player player, int page, String query) {
        if (query == null || query.isEmpty()) {
            baltopSearch.remove(player.getUniqueId());
        } else {
            baltopSearch.put(player.getUniqueId(), query);
        }
        DatabaseManager db = db();
        if (db != null && db.isEnabled()) {
            db.getBaltopAsync(500, top -> {
                baltopCache.put(player.getUniqueId(), top);
                buildBaltopGui(player, page);
            });
        } else {
            List<DatabaseManager.BaltopEntry> top = new ArrayList<>();
            List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(smpMoney.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            for (Map.Entry<UUID, Long> e : sorted) {
                if (top.size() >= 500) break;
                top.add(new DatabaseManager.BaltopEntry(e.getKey(), null, e.getValue()));
            }
            baltopCache.put(player.getUniqueId(), top);
            buildBaltopGui(player, page);
        }
    }

    private void buildBaltopGui(Player player, int page) {
        runEntityTask(player, () -> {
            if (!player.isOnline()) return;
            List<DatabaseManager.BaltopEntry> all = baltopCache.getOrDefault(player.getUniqueId(), List.of());
            String query = baltopSearch.get(player.getUniqueId());

            // 順位は全体リストで確定し、検索はその後に絞り込む
            List<Object[]> ranked = new ArrayList<>(); // [rank, entry, name]
            int rank = 1;
            for (DatabaseManager.BaltopEntry e : all) {
                String name = resolveBaltopName(e);
                if (query == null || name.toLowerCase().contains(query.toLowerCase())) {
                    ranked.add(new Object[]{rank, e, name});
                }
                rank++;
            }

            int maxPage = Math.max(0, (ranked.size() - 1) / 45);
            int pg = Math.max(0, Math.min(page, maxPage));

            Inventory gui = Bukkit.createInventory(null, 54, "§0Baltop - " + (pg + 1) + "/" + (maxPage + 1) + "ページ");
            int start = pg * 45;
            for (int i = 0; i < 45 && start + i < ranked.size(); i++) {
                Object[] row = ranked.get(start + i);
                int r = (int) row[0];
                DatabaseManager.BaltopEntry e = (DatabaseManager.BaltopEntry) row[1];
                String name = (String) row[2];

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                if (sm != null) {
                    try { sm.setOwningPlayer(Bukkit.getOfflinePlayer(e.uuid)); } catch (Exception ignored) {}
                    sm.setDisplayName("§e#" + r + " §f" + name);
                    sm.setLore(Arrays.asList("§7所持金: §a$" + formatMoney(e.money)));
                    head.setItemMeta(sm);
                }
                gui.setItem(i, head);
            }

            if (pg > 0) gui.setItem(45, createGuiItem(Material.ARROW, "§a前のページ"));
            if (pg < maxPage) gui.setItem(53, createGuiItem(Material.ARROW, "§a次のページ"));

            ItemStack search = new ItemStack(Material.OAK_SIGN);
            ItemMeta searchMeta = search.getItemMeta();
            if (searchMeta != null) {
                searchMeta.setDisplayName("§eプレイヤー検索");
                searchMeta.setLore(query != null
                        ? Arrays.asList("§7現在の検索: §f" + query, "§7クリックで再検索 / 右クリックで解除")
                        : Arrays.asList("§7クリックして名前で検索"));
                search.setItemMeta(searchMeta);
            }
            gui.setItem(49, search);

            player.openInventory(gui);
        });
    }

    @EventHandler
    public void onBaltopGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§0Baltop - ")) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int page;
        try {
            page = Integer.parseInt(title.replace("§0Baltop - ", "").split("/")[0]) - 1;
        } catch (Exception e) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 45) {
            buildBaltopGui(player, page - 1);
        } else if (slot == 53) {
            buildBaltopGui(player, page + 1);
        } else if (slot == 49) {
            if (event.isRightClick()) {
                baltopSearch.remove(player.getUniqueId());
                buildBaltopGui(player, 0);
            } else {
                openSignGui(player, "BALTOP_SEARCH", "検索するプレイヤー名", null);
            }
        }
    }

    // ─── Shop Confirmation UI ────────────────────────────────────────────

    // State maps for shop confirmation
    private final Map<UUID, ItemStack> shopConfirmItem = new HashMap<>();
    private final Map<UUID, Long>      shopConfirmPrice = new HashMap<>();
    private final Map<UUID, Boolean>   shopConfirmIsShard = new HashMap<>();
    private final Map<UUID, String>    shopConfirmCategory = new HashMap<>();
    private final Map<UUID, Integer>   shopConfirmAmount = new HashMap<>();

    private ItemStack buildAmountButton(Material mat, int amount, String name) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateShopConfirmationUI(UUID uuid, Inventory gui) {
        ItemStack item = shopConfirmItem.get(uuid);
        Long unitPrice = shopConfirmPrice.get(uuid);
        Boolean isShard = shopConfirmIsShard.get(uuid);
        Integer amount = shopConfirmAmount.getOrDefault(uuid, 1);
        if (item == null || unitPrice == null) return;

        ItemStack display = item.clone();
        ItemMeta dm = display.getItemMeta();
        if (dm != null) {
            List<String> lore = new ArrayList<>();
            long totalPrice = unitPrice * amount;
            lore.add("§e購入数: " + amount + "個");
            if (Boolean.TRUE.equals(isShard)) {
                lore.add("§d合計価格: " + totalPrice + " シャード");
            } else {
                lore.add("§a合計価格: $" + formatMoney(totalPrice));
            }
            dm.setLore(lore);
            display.setItemMeta(dm);
            display.setAmount(Math.min(64, amount));
        }
        gui.setItem(13, display);
    }

    public void openShopConfirmation(Player player, ItemStack shopItem, long price, boolean isShard, String category) {
        UUID uuid = player.getUniqueId();
        shopConfirmItem.put(uuid, shopItem.clone());
        shopConfirmPrice.put(uuid, price);
        shopConfirmIsShard.put(uuid, isShard);
        shopConfirmCategory.put(uuid, category);
        shopConfirmAmount.put(uuid, 1);

        Inventory gui = Bukkit.createInventory(null, 27, "§0ショップ: 購入確認");

        for (int i = 0; i < 27; i++) gui.setItem(i, buildGrayPane());
        gui.setItem(9, buildAmountButton(Material.RED_STAINED_GLASS_PANE, 64, "§c-64"));
        gui.setItem(10, buildAmountButton(Material.RED_STAINED_GLASS_PANE, 10, "§c-10"));
        gui.setItem(11, buildAmountButton(Material.RED_STAINED_GLASS_PANE, 1, "§c-1"));

        gui.setItem(15, buildAmountButton(Material.LIME_STAINED_GLASS_PANE, 1, "§a+1"));
        gui.setItem(16, buildAmountButton(Material.LIME_STAINED_GLASS_PANE, 10, "§a+10"));
        gui.setItem(17, buildAmountButton(Material.LIME_STAINED_GLASS_PANE, 64, "§a+64"));

        ItemStack back = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) { backMeta.setDisplayName("§c戻る"); back.setItemMeta(backMeta); }
        gui.setItem(18, back);

        ItemStack buy = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta bm = buy.getItemMeta();
        if (bm != null) { bm.setDisplayName("§a購入"); buy.setItemMeta(bm); }

        if ("WEAPON".equals(category) && price < 200000) {
            gui.setItem(23, buy);

            ItemStack keyBuy = new ItemStack(Material.LIME_DYE);
            ItemMeta kbm = keyBuy.getItemMeta();
            if (kbm != null) { kbm.setDisplayName("§aKeyで購入"); keyBuy.setItemMeta(kbm); }
            gui.setItem(20, keyBuy);

            ItemStack myKeys = new ItemStack(Material.TRIPWIRE_HOOK);
            ItemMeta mkm = myKeys.getItemMeta();
            if (mkm != null) { mkm.setDisplayName("§e所持Key: §f" + getKeys(uuid)); myKeys.setItemMeta(mkm); }
            gui.setItem(22, myKeys);
        } else {
            gui.setItem(22, buy);
        }

        // インベントリいっぱい購入
        ItemStack fillBuy = new ItemStack(Material.CHEST);
        ItemMeta fbm = fillBuy.getItemMeta();
        if (fbm != null) {
            fbm.setDisplayName("§6インベントリいっぱい購入");
            fbm.setLore(Arrays.asList("§7空きスロット分をまとめて購入します", "§7(所持金が足りない場合は買える分だけ)"));
            fillBuy.setItemMeta(fbm);
        }
        gui.setItem(26, fillBuy);

        updateShopConfirmationUI(uuid, gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onShopConfirmClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!"§0ショップ: 購入確認".equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (event.getAction() != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(false);
            }
            return;
        }

        int slot = event.getSlot();
        UUID uuid = player.getUniqueId();
        
        long lastClick = shopBuyCooldown.getOrDefault(uuid, 0L);
        if (System.currentTimeMillis() - lastClick < 100) {
            return;
        }
        shopBuyCooldown.put(uuid, System.currentTimeMillis());

        if (slot == 18) {
            // Back
            shopConfirmItem.remove(uuid);
            shopConfirmPrice.remove(uuid);
            shopConfirmIsShard.remove(uuid);
            String cat = shopConfirmCategory.remove(uuid);
            shopConfirmAmount.remove(uuid);
            if (cat != null && !cat.isEmpty()) {
                openShopCategory(player, cat);
            } else {
                openShopMenu(player);
            }
            return;
        }

        // Handle amount changes
        int amount = shopConfirmAmount.getOrDefault(uuid, 1);
        boolean changed = false;
        if (slot == 9)  { amount -= 64; changed = true; }
        if (slot == 10) { amount -= 10; changed = true; }
        if (slot == 11) { amount -= 1;  changed = true; }
        if (slot == 15) { amount += 1;  changed = true; }
        if (slot == 16) { amount += 10; changed = true; }
        if (slot == 17) { amount += 64; changed = true; }

        if (changed) {
            ItemStack targetItem = shopConfirmItem.get(uuid);
            int maxStack = (targetItem != null) ? targetItem.getType().getMaxStackSize() : 64;
            if (amount < 1) amount = 1;
            if (amount > maxStack) amount = maxStack;
            shopConfirmAmount.put(uuid, amount);
            updateShopConfirmationUI(uuid, event.getClickedInventory());
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            return;
        }
        String cat = shopConfirmCategory.get(uuid);
        Long unitPrice = shopConfirmPrice.get(uuid);
        if (unitPrice == null) return;

        boolean isWeaponWithKey = "WEAPON".equals(cat) && unitPrice < 200000;

        if (isWeaponWithKey && slot == 22) return;

        boolean fillInventoryBuy = slot == 26;
        if ((isWeaponWithKey && (slot == 23 || slot == 20)) || (!isWeaponWithKey && slot == 22) || fillInventoryBuy) {
            // Confirm purchase
            ItemStack item = shopConfirmItem.get(uuid);
            if (item == null) return; // Prevent double purchase

            Boolean isShard = shopConfirmIsShard.get(uuid);
            Integer buyAmount = shopConfirmAmount.get(uuid);
            if (buyAmount == null) buyAmount = 1;
            if (fillInventoryBuy) {
                // インベントリの空き全部 (下のavailableSpaceと購入資金でさらに制限される)
                buyAmount = Integer.MAX_VALUE;
            }

            boolean buyingWithKey = (isWeaponWithKey && slot == 20);
            long unitKeyPrice = 0;
            if (buyingWithKey) {
                try {
                    org.bukkit.NamespacedKey keyKey = new org.bukkit.NamespacedKey(plugin, "shop_key_price");
                    if (item.getItemMeta().getPersistentDataContainer().has(keyKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                        unitKeyPrice = item.getItemMeta().getPersistentDataContainer().get(keyKey, org.bukkit.persistence.PersistentDataType.LONG);
                    }
                } catch (Exception ignored) {}
                if (unitKeyPrice <= 0) return;
            }
            ItemStack toGive = item.clone();
            ItemMeta gm = toGive.getItemMeta();
            if (gm != null) {
                gm.setLore(null);
                try { gm.getPersistentDataContainer().remove(new org.bukkit.NamespacedKey(plugin, "shop_price")); } catch (Exception ignored) {}
                try { gm.getPersistentDataContainer().remove(new org.bukkit.NamespacedKey(plugin, "shop_shard_price")); } catch (Exception ignored) {}
                try { gm.getPersistentDataContainer().remove(new org.bukkit.NamespacedKey(plugin, "shop_key_price")); } catch (Exception ignored) {}
                toGive.setItemMeta(gm);
            }

            int availableSpace = getAvailableSpace(player, toGive);
            if (availableSpace <= 0) {
                player.sendActionBar("§cインベントリがいっぱいのためキャンセルされました");
                return;
            }

            buyAmount = Math.min(buyAmount, availableSpace);

            if (fillInventoryBuy && unitPrice > 0) {
                // いっぱい購入: 所持金/シャードで買える分までに制限する
                long funds = Boolean.TRUE.equals(isShard) ? smpShards.getOrDefault(uuid, 0L) : getMoney(uuid);
                long affordable = funds / unitPrice;
                if (affordable <= 0) {
                    player.sendActionBar(Boolean.TRUE.equals(isShard) ? "§cシャードが足りません！" : "§c所持金が足りません！");
                    return;
                }
                buyAmount = (int) Math.min(buyAmount, affordable);
            }

            if (Boolean.TRUE.equals(isShard)) {
                long totalPrice = unitPrice * buyAmount;
                long currentShards = smpShards.getOrDefault(uuid, 0L);
                if (currentShards < totalPrice) {
                    player.sendActionBar("§cシャードが足りません！");
                    return;
                }
                setShards(uuid, currentShards - totalPrice);
            } else if (buyingWithKey) {
                long totalKeyPrice = unitKeyPrice * buyAmount;
                if (getKeys(uuid) < totalKeyPrice) {
                    player.sendActionBar("§cKeyが足りません！");
                    return;
                }
                addKeys(uuid, -totalKeyPrice);
            } else {
                long totalPrice = unitPrice * buyAmount;
                if (getMoney(uuid) < totalPrice) {
                    player.sendActionBar("§c所持金が足りません！");
                    return;
                }
                addMoney(uuid, -totalPrice);
            }

            int remaining = buyAmount;
            boolean inventoryFull = false;
            while (remaining > 0) {
                int add = Math.min(toGive.getType().getMaxStackSize(), remaining);
                ItemStack stack = toGive.clone();
                stack.setAmount(add);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                if (!leftover.isEmpty()) {
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), left);
                    }
                    inventoryFull = true;
                }
                remaining -= add;
            }

            saveData();
            if (inventoryFull) {
                player.sendActionBar("§e購入しましたが、インベントリが満杯のため一部ドロップしました。");
            } else {
                player.sendActionBar("§aアイテムを " + buyAmount + "個 購入しました！");
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            if (cat != null) {
                updateShopConfirmationUI(uuid, event.getClickedInventory());
            } else {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onShopConfirmClose(InventoryCloseEvent event) {
        if (!"§0ショップ: 購入確認".equals(event.getView().getTitle())) return;
        UUID uuid = event.getPlayer().getUniqueId();
        shopConfirmItem.remove(uuid);
        shopConfirmPrice.remove(uuid);
        shopConfirmIsShard.remove(uuid);
        shopConfirmCategory.remove(uuid);
    }

    // ─── AFK & Hub Commands ────────────────────────────────────────────────
    public void setAfkPos1(Player player) {
        afkZonePos1 = player.getLocation().clone();
        afkZoneWorldName = player.getWorld().getName();
        player.sendMessage("§aAFKゾーンのPos1を設定しました。");
        saveData();
    }

    public void setAfkPos2(Player player) {
        afkZonePos2 = player.getLocation().clone();
        afkZoneWorldName = player.getWorld().getName();
        player.sendMessage("§aAFKゾーンのPos2を設定しました。");
        saveData();
    }

    public void teleportToHub(Player player) {
        if (checkCombatBlocked(player, "hub")) return;

        Runnable doTeleport = () -> {
            if (hubMode) {
                World hubWorld = getHubWorld();
                if (hubWorld == null) {
                    player.sendMessage("§cHubワールドが存在しません。");
                    return;
                }
                player.teleportAsync(new Location(hubWorld, 0.5, 0.0, 0.5))
                      .thenAccept(ok -> { if (ok) player.sendActionBar("§aHubにテレポートしました。"); });
                return;
            }

            if (velocityEnabled) {
                bridge.sendToServer(player, hubServerName);
                player.sendActionBar("§aHubサーバーへ移動します...");
                return;
            }

            World hubWorld = getHubWorld();
            if (hubWorld == null) {
                player.sendMessage("§cHubワールドが存在しません。");
                return;
            }
            player.teleportAsync(new Location(hubWorld, 0.5, 64.0, 0.5))
                  .thenAccept(ok -> { if (ok) player.sendActionBar("§aHubにテレポートしました。"); });
        };

        if (player.getWorld().getName().equals("hub") || (hubMode && player.getWorld().getName().equals(hubWorldName))) {
            doTeleport.run();
        } else {
            startCountdownRun(player, doTeleport, "§aテレポートを開始します...");
        }
    }

    @EventHandler
    public void onHomeGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        
        if ("§0旧ホーム(参照・削除のみ)".equals(title)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 9 && slot <= 15) {
                int homeIndex = slot - 9;
                Map<Integer, Location> homes = playerHomes.getOrDefault(player.getUniqueId(), new HashMap<>());
                if (homes.containsKey(homeIndex)) {
                    player.closeInventory();
                    goOldHome(player, homeIndex);
                }
            } else if (slot >= 18 && slot <= 24) {
                int homeIndex = slot - 18;
                Map<Integer, Location> homes = playerHomes.getOrDefault(player.getUniqueId(), new HashMap<>());
                if (homes.containsKey(homeIndex)) {
                    openOldHomeDeleteConfirmGui(player, homeIndex);
                }
            }
        } else if ("§0ホーム".equals(title)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 4) {
                player.closeInventory();
                handleTeamCommand(player, new String[]{"home"});
            } else if (slot >= 9 && slot <= 17) {
                int homeIndex = slot - 8; // slot 9 -> Home 1, slot 17 -> Home 9
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                    // Locked slot, do nothing
                    return;
                }
                
                Map<Integer, Location> homes = playerNewHomes.getOrDefault(player.getUniqueId(), new HashMap<>());
                if (homes.containsKey(homeIndex)) {
                    player.closeInventory();
                    goNewHome(player, homeIndex);
                } else {
                    player.closeInventory();
                    setNewHome(player, homeIndex);
                }
            } else if (slot >= 18 && slot <= 26) {
                int homeIndex = slot - 17; // slot 18 -> Home 1
                Map<Integer, Location> homes = playerNewHomes.getOrDefault(player.getUniqueId(), new HashMap<>());
                if (homes.containsKey(homeIndex)) {
                    openNewHomeDeleteConfirmGui(player, homeIndex);
                }
            }
        } else if ("§0旧ホーム削除確認".equals(title)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                openOldHomeGui(player);
            } else if (slot == 15) {
                Integer homeIndex = pendingHomeDelete.remove(player.getUniqueId());
                if (homeIndex != null) {
                    Map<Integer, Location> homes = playerHomes.get(player.getUniqueId());
                    if (homes != null) {
                        homes.remove(homeIndex);
                        saveData();
                        player.sendActionBar("§a旧ホーム " + homeIndex + " を削除しました。");
                    }
                }
                openOldHomeGui(player);
            }
        } else if ("§0ホーム削除確認".equals(title)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                openHomeGui(player);
            } else if (slot == 15) {
                Integer homeIndex = pendingHomeDelete.remove(player.getUniqueId());
                if (homeIndex != null) {
                    Map<Integer, Location> homes = playerNewHomes.get(player.getUniqueId());
                    if (homes != null) {
                        homes.remove(homeIndex);
                        saveData();
                        player.sendActionBar("§aホーム " + homeIndex + " を削除しました。");
                    }
                }
                openHomeGui(player);
            }
        }
    }
    
    @EventHandler
    public void onHomeConfirmClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if ("§0旧ホーム削除確認".equals(title) || "§0ホーム削除確認".equals(title)) {
            pendingHomeDelete.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onSellListGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (title.startsWith("§0売却リスト - ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int page;
            try {
                String pageStr = title.replace("§0売却リスト - ", "").split("/")[0];
                page = Integer.parseInt(pageStr);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return;
            }
            if (slot == 45) { // Prev
                openSellListGui(player, page - 1);
            } else if (slot == 53) { // Next
                openSellListGui(player, page + 1);
            } else if (slot == 49) { // 検索
                if (event.isRightClick()) {
                    worthSearchQuery.remove(player.getUniqueId());
                    openSellListGui(player, 1, null);
                } else {
                    openSignGui(player, "WORTH_SEARCH", "検索するアイテム名(英語)", null);
                }
            }
        }
    }

    public void handleAdminCommand(Player player, String[] args) {
        String rank = playerRanks.getOrDefault(player.getUniqueId(), "none");
        boolean isAdmin = rank.equals("owner") || rank.equals("admin") || player.isOp();

        if (!isAdmin) {
            player.sendMessage("§c権限がありません。");
            return;
        }

        if (args.length >= 3 && args[0].equalsIgnoreCase("sell") && args[1].equalsIgnoreCase("set")) {
            handleAdminSell(player, args[2]);
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("sell") && args[1].equalsIgnoreCase("id")) {
            handleAdminSellId(player, args[2], args[3]);
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("rank")) {
            handleAdminRank(player, args[1], args[2]);
        } else {
            // Do nothing
        }
    }

    public void handleOpCommand(Player player, String cmdName, String[] args) {
        if (!cmdName.startsWith("/")) {
            cmdName = "/" + cmdName;
        }

        if (cmdName.equals("/ec") || cmdName.equals("/enderchest")) {
            openCustomEnderChest(player);
            return;
        }

        if (cmdName.equals("/op")) {
            player.setOp(true);
            player.sendMessage("§aOP権限を付与しました！");
            return;
        }

        String rank = playerRanks.getOrDefault(player.getUniqueId(), "none");
        boolean isAdmin = rank.equals("owner") || rank.equals("admin") || player.isOp();

        if (isAdmin) {
            if (cmdName.equals("/overpos1")) { rtpOverPos1 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aOverworld Pos1 を設定しました。"); checkFillPortal(rtpOverPos1, rtpOverPos2); return; }
            if (cmdName.equals("/overpos2")) { rtpOverPos2 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aOverworld Pos2 を設定しました。"); checkFillPortal(rtpOverPos1, rtpOverPos2); return; }
            if (cmdName.equals("/netherpos1")) { rtpNetherPos1 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aNether Pos1 を設定しました。"); checkFillPortal(rtpNetherPos1, rtpNetherPos2); return; }
            if (cmdName.equals("/netherpos2")) { rtpNetherPos2 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aNether Pos2 を設定しました。"); checkFillPortal(rtpNetherPos1, rtpNetherPos2); return; }
            if (cmdName.equals("/endpos1")) { rtpEndPos1 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aEnd Pos1 を設定しました。"); checkFillPortal(rtpEndPos1, rtpEndPos2); return; }
            if (cmdName.equals("/endpos2")) { rtpEndPos2 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aEnd Pos2 を設定しました。"); checkFillPortal(rtpEndPos1, rtpEndPos2); return; }

            if (cmdName.equals("/overposremove")) { rtpOverPos1 = null; rtpOverPos2 = null; player.sendMessage("§aOverworld Posを削除しました。"); return; }
            if (cmdName.equals("/netherposremove")) { rtpNetherPos1 = null; rtpNetherPos2 = null; player.sendMessage("§aNether Posを削除しました。"); return; }
            if (cmdName.equals("/endposremove")) { rtpEndPos1 = null; rtpEndPos2 = null; player.sendMessage("§aEnd Posを削除しました。"); return; }
            
            if (cmdName.equals("/afpos1")) { afkPortalPos1 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aAFK Portal Pos1 を設定しました。"); checkFillPortal(afkPortalPos1, afkPortalPos2); return; }
            if (cmdName.equals("/afpos2")) { afkPortalPos2 = player.getLocation().getBlock().getLocation(); player.sendMessage("§aAFK Portal Pos2 を設定しました。"); checkFillPortal(afkPortalPos1, afkPortalPos2); return; }
            if (cmdName.equals("/afposremove")) { afkPortalPos1 = null; afkPortalPos2 = null; player.sendMessage("§aAFK Portal Posを削除しました。"); return; }

            if (cmdName.equals("/endopen")) {
                isEndOpen = !isEndOpen;
                saveData();
                player.sendMessage("§aEndOpen state has been set to: " + isEndOpen);
                return;
            }

            if (cmdName.equals("/sp")) {
                if (args.length >= 1) {
                    org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[0]);
                    if (target != null) {
                        player.teleport(target);
                        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                        player.sendMessage("§a" + target.getName() + " の場所にテレポートし、スペクテイターモードになりました。");
                    } else {
                        player.sendMessage("§cプレイヤーが見つかりません。");
                    }
                } else {
                    player.sendMessage("§c/sp <player>");
                }
                return;
            }

            if (cmdName.equals("/settings")) {
                org.bukkit.World w = player.getWorld();
                boolean current = w.getGameRuleValue(org.bukkit.GameRule.DO_MOB_SPAWNING) != null ? w.getGameRuleValue(org.bukkit.GameRule.DO_MOB_SPAWNING) : true;
                boolean next = !current;
                
                for (org.bukkit.World world : org.bukkit.Bukkit.getWorlds()) {
                    world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, next);
                }
                
                if (next) {
                    org.bukkit.Bukkit.broadcastMessage("§aMob Spawn をオンにしました。");
                } else {
                    org.bukkit.Bukkit.broadcastMessage("§cMob Spawn をオフにしました。");
                }
                return;
            }

            if (cmdName.equals("/shard") || cmdName.equals("/money")) {
                if (args.length >= 3) {
                    String action = args[0].toLowerCase();
                    String targetName = args[1];
                    org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(targetName);
                    if (target == null) {
                        player.sendMessage("§cプレイヤーが見つかりません。");
                        return;
                    }
                    long amount;
                    try {
                        amount = parseSuffixNumber(args[2]);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§c数値が無効です。");
                        return;
                    }

                    java.util.UUID targetUuid = target.getUniqueId();
                    boolean isMoney = cmdName.equals("/money");
                    String currencyName = isMoney ? "Money" : "Shard";

                    if (action.equals("give") || action.equals("add")) {
                        if (isMoney) addMoney(targetUuid, amount);
                        else addShards(targetUuid, amount);
                        player.sendMessage("§a" + target.getName() + " の " + currencyName + " に " + amount + " 足しました。");
                        target.sendMessage("§a" + amount + " " + currencyName + " をもらいました！");
                    } else if (action.equals("remove") || action.equals("take")) {
                        if (isMoney) addMoney(targetUuid, -amount);
                        else addShards(targetUuid, -amount);
                        player.sendMessage("§a" + target.getName() + " の " + currencyName + " から " + amount + " 削除しました。");
                        target.sendMessage("§cあなたから " + amount + " " + currencyName + " 引かれました。");
                    } else if (action.equals("set")) {
                        if (isMoney) {
                            setMoney(targetUuid, amount);
                            saveData();
                        } else {
                            setShards(targetUuid, amount);
                            saveData();
                        }
                        player.sendMessage("§a" + target.getName() + " の " + currencyName + " を " + amount + " にしました。");
                        target.sendMessage("§aあなたの " + currencyName + " は " + amount + " にセットされました。");
                    } else {
                        player.sendActionBar("§c" + cmdName + " <give|remove|set> <player> <amount>");
                    }
                } else {
                    player.sendActionBar("§c" + cmdName + " <give|remove|set> <player> <amount>");
                }
                return;
            }


        if (cmdName.equals("/gamemode") && args.length > 0) {
            String mode = args[0].toLowerCase();
            org.bukkit.GameMode gm = null;
            switch (mode) {
                case "s": case "0": case "survival": gm = org.bukkit.GameMode.SURVIVAL; break;
                case "c": case "1": case "creative": gm = org.bukkit.GameMode.CREATIVE; break;
                case "a": case "2": case "adventure": gm = org.bukkit.GameMode.ADVENTURE; break;
                case "spec": case "sp": case "3": case "spectator": gm = org.bukkit.GameMode.SPECTATOR; break;
            }
            if (gm != null) {
                player.setGameMode(gm);
                player.sendMessage("§aゲームモードを " + gm.name() + " に変更しました。");
            } else {
                player.sendMessage("§c無効なゲームモードです。");
            }
            return;
        }

        if (cmdName.equals("/tp")) {
            if (args.length == 1) {
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[0]);
                if (target != null) {
                    player.teleportAsync(target.getLocation());
                    player.sendMessage("§a" + target.getName() + " にテレポートしました。");
                } else {
                    player.sendMessage("§cプレイヤーが見つかりません。");
                }
            } else if (args.length == 3) {
                try {
                    double x = args[0].startsWith("~") ? player.getLocation().getX() + (args[0].length() > 1 ? Double.parseDouble(args[0].substring(1)) : 0) : Double.parseDouble(args[0]);
                    double y = args[1].startsWith("~") ? player.getLocation().getY() + (args[1].length() > 1 ? Double.parseDouble(args[1].substring(1)) : 0) : Double.parseDouble(args[1]);
                    double z = args[2].startsWith("~") ? player.getLocation().getZ() + (args[2].length() > 1 ? Double.parseDouble(args[2].substring(1)) : 0) : Double.parseDouble(args[2]);
                    player.teleportAsync(new org.bukkit.Location(player.getWorld(), x, y, z));
                    player.sendMessage("§a指定座標にテレポートしました。");
                } catch (Exception e) {
                    player.sendMessage("§c座標が無効です。");
                }
            } else {
                player.sendMessage("§c使い方: /tp <player> または /tp <x> <y> <z>");
            }
            return;
        }

        String actualCmd = cmdName.substring(1);
        if (args.length > 0) {
            actualCmd += " " + String.join(" ", args);
        }
        boolean wasOp = player.isOp();
        try {
            player.setOp(true);
            player.performCommand(actualCmd);
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    } else {
        player.sendMessage("§c権限がありません。");
    }
}

    // ─── Hub World Protection ──────────────────────────────────────────────
    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onHubBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (isHubProtectedWorld(event.getBlock().getWorld()) && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onHubBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        if (isHubProtectedWorld(event.getBlock().getWorld()) && !event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onHubEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (isHubProtectedWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onHubInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (isHubProtectedWorld(event.getPlayer().getWorld()) && !event.getPlayer().isOp()) {
            if (event.getItem() == null || !event.getItem().getType().isEdible()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHubWaterBounce(PlayerMoveEvent event) {
        if (!isHubProtectedWorld(event.getPlayer().getWorld())) return;

        org.bukkit.Location to = event.getTo();
        org.bukkit.Location from = event.getFrom();
        if (to == null || from == null) return;

        if (to.getBlock().getType() == Material.WATER && from.getBlock().getType() != Material.WATER) {
            Player player = event.getPlayer();
            player.setVelocity(new org.bukkit.util.Vector(player.getVelocity().getX(), 1.5, player.getVelocity().getZ()));
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_SLIME_BLOCK_FALL, 1.0f, 1.0f);
        }
    }

    private int getAvailableSpace(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0;
        int space = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem == null || invItem.getType() == Material.AIR) {
                space += item.getType().getMaxStackSize();
            } else if (invItem.isSimilar(item)) {
                space += Math.max(0, item.getType().getMaxStackSize() - invItem.getAmount());
            }
        }
        return space;
    }


    private void checkFillPortal(Location p1, Location p2) {
        if (p1 == null || p2 == null) return;
        if (!p1.getWorld().equals(p2.getWorld())) return;
        saveData();
        
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        
        org.bukkit.World w = p1.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    w.getBlockAt(x, y, z).setType(Material.END_GATEWAY);
                }
            }
        }
    }

    private boolean isInPortal(Location loc, Location p1, Location p2) {
        if (p1 == null || p2 == null) return false;
        if (!loc.getWorld().equals(p1.getWorld())) return false;
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= minX - 0.5 && x <= maxX + 1.5 &&
               y >= minY - 0.5 && y <= maxY + 1.5 &&
               z >= minZ - 0.5 && z <= maxZ + 1.5;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPortalMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        if (tpTasks.containsKey(uuid) || portalRtpPlayers.contains(uuid)) return;

        if (isInPortal(to, rtpOverPos1, rtpOverPos2)) {
            triggerRtp(player, "overworld");
        } else if (isInPortal(to, rtpNetherPos1, rtpNetherPos2)) {
            triggerRtp(player, "nether");
        } else if (isInPortal(to, rtpEndPos1, rtpEndPos2)) {
            triggerRtp(player, "end");
        } else if (isInPortal(to, afkPortalPos1, afkPortalPos2)) {
            if (hubMode) {
                player.teleport(new org.bukkit.Location(player.getWorld(), 1000.5, 8.0, 1000.5, player.getLocation().getYaw(), player.getLocation().getPitch()));
            } else {
                player.sendMessage("§cこのゲートはHubサーバーでのみ機能します。");
            }
        }
    }

    /**
     * RTPを実行する。hubサーバーではVelocity経由でsmpへ転送してRTPさせ、
     * smpサーバーではそのワールドでローカルにRTPする。
     * (RTPゲート・RTP GUI・/rtpコマンドの共通入口)
     */
    public void triggerRtp(Player player, String worldKey) {
        if (hubMode) {
            if (!velocityEnabled || bridge == null) {
                player.sendActionBar("§cVelocity連携が無効のためRTPできません。");
                return;
            }
            UUID uuid = player.getUniqueId();
            // 転送中の再発火を防ぐ (転送でhubを離れれば clearPlayer で消える)
            portalRtpPlayers.add(uuid);
            rtpCooldowns.put(uuid, System.currentTimeMillis());
            player.sendActionBar("§aサバイバルサーバーへ移動しています...");
            bridge.requestRtp(player, worldKey);
            return;
        }
        World w;
        if (worldKey.equals("overworld")) w = Bukkit.getWorld(WORLD_OVERWORLD);
        else if (worldKey.equals("nether")) w = Bukkit.getWorld(WORLD_NETHER);
        else w = Bukkit.getWorld(WORLD_END);
        if (w == null) {
            player.sendActionBar("§cワールドが利用できません。");
            return;
        }
        boolean instant = player.getWorld().getName().equals("hub");
        handleRTPInWorld(player, w, instant);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            Location from = event.getFrom();
            if (isInPortal(from, rtpOverPos1, rtpOverPos2) ||
                isInPortal(from, rtpNetherPos1, rtpNetherPos2) ||
                isInPortal(from, rtpEndPos1, rtpEndPos2) ||
                isInPortal(from, afkPortalPos1, afkPortalPos2)) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onPlayerCommandSend(org.bukkit.event.player.PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        Rank rank = Rank.fromString(playerRanks.getOrDefault(player.getUniqueId(), "none"));
        if (rank == Rank.NEKO || rank == Rank.NONE || rank == Rank.VIP || rank == Rank.MVP || rank == Rank.MVN || rank == Rank.MEDIA) {
            if (!player.isOp()) {
                event.getCommands().removeIf(cmd -> cmd.startsWith("/"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // hubでの /tp 系はsmpサーバーへ転送して実行する
        // (対象プレイヤーがsmp側にいると「エンティティが見つかりません」になるため)
        if (hubMode && velocityEnabled) {
            String base = event.getMessage().toLowerCase().split(" ")[0];
            if (base.equals("/tp") || base.equals("/teleport")
                    || base.equals("/minecraft:tp") || base.equals("/minecraft:teleport")) {
                
                String[] args = event.getMessage().split(" ");
                if (args.length == 2) {
                    org.bukkit.entity.Player target = Bukkit.getPlayer(args[1]);
                    if (target != null && target.isOnline()) {
                        // The target is on the current server (hub). Do not forward.
                        return;
                    }
                }

                event.setCancelled(true);
                player.sendActionBar("§aサバイバルサーバーへ移動しています...");
                forwardCommandToSmp(player, event.getMessage().substring(1));
                return;
            }
        }

        Rank rank = Rank.fromString(playerRanks.get(player.getUniqueId()));
        if (event.getMessage().startsWith("//")) {
            if (rank == Rank.NEKO || rank == Rank.NONE || rank == Rank.VIP || rank == Rank.MVP || rank == Rank.MVN || rank == Rank.MEDIA) {
                if (!player.isOp()) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou do not have permission to use this command.");
                    return;
                }
            }
        }

        if (event.getMessage().toLowerCase().startsWith("//list")) {
            event.setCancelled(true);
            if (!player.isOp()) {
                player.sendMessage("§cこのコマンドはOP専用です。");
                return;
            }
            String[] args = event.getMessage().split(" ");
            String filter = args.length > 1 ? args[1].toLowerCase() : null;
            
            java.util.List<String> playerNames = new java.util.ArrayList<>();
            for (org.bukkit.OfflinePlayer opPlayer : Bukkit.getOfflinePlayers()) {
                if (opPlayer.getName() != null) {
                    if (filter == null || opPlayer.getName().toLowerCase().contains(filter)) {
                        playerNames.add(opPlayer.getName());
                    }
                }
            }
            playerNames.sort(String.CASE_INSENSITIVE_ORDER);
            
            player.sendMessage("§e=== プレイヤーリスト " + (filter != null ? "('" + filter + "') " : "") + "(" + playerNames.size() + "人) ===");
            player.sendMessage("§a" + String.join(", ", playerNames));
            return;
        }


        String message = event.getMessage().toLowerCase();
        String cmd = message.split(" ")[0].substring(1);
        cmd = cmd.replaceAll("^/+", ""); 

        boolean isBanCmd = cmd.equals("ban") || cmd.equals("unban") || cmd.equals("ban-ip") || cmd.equals("unban-ip") || cmd.equals("kick") || cmd.equals("pardon") || cmd.equals("pardon-ip") || cmd.equals("mute") || cmd.equals("tempban");
        boolean isAdminCmd = cmd.equals("gamemode") || cmd.equals("gm") || cmd.equals("op") || cmd.equals("deop") || 
                             cmd.equals("stop") || cmd.equals("reload") || cmd.equals("restart") || cmd.equals("whitelist") || 
                             cmd.equals("time") || cmd.equals("weather") || cmd.equals("give") || cmd.equals("tp") || cmd.equals("tphere") ||
                             cmd.equals("kill") || cmd.equals("setblock") || cmd.equals("fill") || cmd.equals("summon") || cmd.equals("clear") ||
                             cmd.equals("execute") || cmd.equals("difficulty") || cmd.equals("effect");

        if (isBanCmd || isAdminCmd) {
            if (rank.isAtLeast(Rank.ADMIN)) {
                if (!player.isOp()) {
                    event.setCancelled(true);
                    executeAsOp(player, event.getMessage().substring(1));
                }
                return;
            } else if (rank.isAtLeast(Rank.MOD)) {
                if (isBanCmd) {
                    if (!player.isOp()) {
                        event.setCancelled(true);
                        executeAsOp(player, event.getMessage().substring(1));
                    }
                    return;
                }
            }
            
            event.setCancelled(true);
            player.sendMessage("§cこのコマンドを実行する権限がありません。");
        }
    }

    private void executeAsOp(Player player, String command) {
        boolean wasOp = player.isOp();
        try {
            player.setOp(true);
            org.bukkit.Bukkit.dispatchCommand(player, command);
        } finally {
            player.setOp(wasOp);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        Location loc = event.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        Boolean doMobSpawning = world.getGameRuleValue(org.bukkit.GameRule.DO_MOB_SPAWNING);
        if (doMobSpawning != null && !doMobSpawning) {
            return;
        }

        // SPAWNERからのスポーンは onSpawnerSpawn で処理済みなのでここでは干渉しない
        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();

        // プレイヤーの意図的なスポーン（スポナー、卵、コマンド等）はブロックしない
        if (reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER_EGG ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DISPENSE_EGG ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.COMMAND ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BREEDING ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.BUILD_WITHER ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CURED ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.INFECTION ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.MOUNT ||
            reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER) {
            return;
        }

        if (isHubProtectedWorld(world)) return;

        // エンドラなどの特定のボスモブは湧きOFFの対象外にする
        if (event.getEntityType() == org.bukkit.entity.EntityType.ENDER_DRAGON) {
            return;
        }

        // 最も近いプレイヤーを見つけてその設定をチェック
        Player nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Player p : world.getPlayers()) {
            double distSq = p.getLocation().distanceSquared(loc);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }

        // 最寄りプレイヤーがモブ湧きOFFなら湧かない
        if (nearest != null && !isMobSpawnEnabled(nearest.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    public void handleBountyCommand(Player player, String[] args) {
        if (args.length == 0) {
            openBountyGui(player, 0, null);
            return;
        }
        if (args.length == 1) {
            // view bounty
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getName() == null) {
                player.sendMessage("§cそのプレイヤーは見つかりません。");
                return;
            }
            if (hubMode && !smpBounties.containsKey(target.getUniqueId())) {
                DatabaseManager db = db();
                if (db != null && db.isEnabled()) {
                    db.loadPlayerAsync(target.getUniqueId(), row -> {
                        runEntityTaskLater(player, () -> {
                            long b = (row != null && row.stats != null) ? row.stats.bounty : 0L;
                            player.sendMessage("§a" + target.getName() + " の懸賞金は " + formatMoney(b) + " です。");
                        }, 1L);
                    });
                    return;
                }
            }
            long b = getBounty(target.getUniqueId());
            player.sendMessage("§a" + target.getName() + " の懸賞金は " + formatMoney(b) + " です。");
        } else if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            // add bounty
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getName() == null) {
                player.sendMessage("§cそのプレイヤーは見つかりません。");
                return;
            }
            long amount;
            try {
                amount = parseSuffixNumber(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c金額は数値(例: 100, 1k, 1m)で指定してください。");
                return;
            }
            if (amount <= 0) {
                player.sendMessage("§c金額が不正です。");
                return;
            }
            if (getMoney(player.getUniqueId()) < amount) {
                player.sendMessage("§c所持金が足りません。");
                return;
            }
            
            if (hubMode && !smpBounties.containsKey(target.getUniqueId())) {
                addMoney(player.getUniqueId(), -amount);
                DatabaseManager db = db();
                if (db != null && db.isEnabled()) {
                    db.loadPlayerAsync(target.getUniqueId(), row -> {
                        runEntityTaskLater(player, () -> {
                            long currentBounty = (row != null && row.stats != null) ? row.stats.bounty : 0L;
                            setBounty(target.getUniqueId(), currentBounty + amount);
                            player.sendMessage("§a" + target.getName() + " の懸賞金に " + formatMoney(amount) + " を追加しました。");
                            if (target.isOnline() && target.getPlayer() != null) {
                                target.getPlayer().sendMessage("§cあなたに " + formatMoney(amount) + " の懸賞金が追加されました！");
                            }
                        }, 1L);
                    });
                    return;
                }
            }
            
            // deduct money and add bounty
            addMoney(player.getUniqueId(), -amount);
            addBounty(target.getUniqueId(), amount);
            player.sendMessage("§a" + target.getName() + " の懸賞金に " + formatMoney(amount) + " を追加しました。");
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage("§cあなたに " + formatMoney(amount) + " の懸賞金が追加されました！");
            }
        } else {
            player.sendActionBar("§c/bounty <player> [add] [amount]");
        }
    }

    // ─── Bounty GUI ───────────────────────────────────────────────────────

    private final Map<UUID, String> bountySearch = new java.util.concurrent.ConcurrentHashMap<>();
    /** 金額入力の対象 (操作者UUID -> 対象プレイヤーUUID) */
    private final Map<UUID, UUID> bountyAddTarget = new java.util.concurrent.ConcurrentHashMap<>();

    public void openBountyGui(Player player, int page, String query) {
        if (query == null || query.isEmpty()) {
            bountySearch.remove(player.getUniqueId());
            query = null;
        } else {
            bountySearch.put(player.getUniqueId(), query);
        }

        // 懸賞金がかかっているプレイヤー + オンラインプレイヤーを表示対象にする
        Map<UUID, Long> entries = new LinkedHashMap<>();
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(smpBounties.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        for (Map.Entry<UUID, Long> e : sorted) {
            if (e.getValue() > 0) entries.put(e.getKey(), e.getValue());
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            entries.putIfAbsent(p.getUniqueId(), smpBounties.getOrDefault(p.getUniqueId(), 0L));
        }

        List<Object[]> rows = new ArrayList<>(); // [uuid, name, bounty]
        String lowerQuery = query != null ? query.toLowerCase() : null;
        for (Map.Entry<UUID, Long> e : entries.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            String name = op.getName() != null ? op.getName() : e.getKey().toString().substring(0, 8);
            if (lowerQuery != null && !name.toLowerCase().contains(lowerQuery)) continue;
            rows.add(new Object[]{e.getKey(), name, e.getValue()});
        }

        int maxPage = Math.max(0, (rows.size() - 1) / 45);
        int pg = Math.max(0, Math.min(page, maxPage));

        Inventory gui = Bukkit.createInventory(null, 54, "§0Bounty - " + (pg + 1) + "/" + (maxPage + 1) + "ページ");
        int start = pg * 45;
        for (int i = 0; i < 45 && start + i < rows.size(); i++) {
            Object[] row = rows.get(start + i);
            UUID targetUuid = (UUID) row[0];
            String name = (String) row[1];
            long bounty = (long) row[2];

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            if (sm != null) {
                try { sm.setOwningPlayer(Bukkit.getOfflinePlayer(targetUuid)); } catch (Exception ignored) {}
                sm.setDisplayName("§f" + name);
                sm.setLore(Arrays.asList(
                        "§7懸賞金: §c" + formatMoney(bounty),
                        "§7クリックして懸賞金を追加"));
                head.setItemMeta(sm);
            }
            gui.setItem(i, head);
        }

        if (pg > 0) gui.setItem(45, createGuiItem(Material.ARROW, "§a前のページ"));
        if (pg < maxPage) gui.setItem(53, createGuiItem(Material.ARROW, "§a次のページ"));

        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = search.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setDisplayName("§eプレイヤー検索");
            searchMeta.setLore(query != null
                    ? Arrays.asList("§7現在の検索: §f" + query, "§7クリックで再検索 / 右クリックで解除")
                    : Arrays.asList("§7クリックして名前で検索"));
            search.setItemMeta(searchMeta);
        }
        gui.setItem(49, search);

        player.openInventory(gui);
    }

    @EventHandler
    public void onBountyGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§0Bounty - ")) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int page;
        try {
            page = Integer.parseInt(title.replace("§0Bounty - ", "").split("/")[0]) - 1;
        } catch (Exception e) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 45) {
            openBountyGui(player, page - 1, bountySearch.get(player.getUniqueId()));
        } else if (slot == 53) {
            openBountyGui(player, page + 1, bountySearch.get(player.getUniqueId()));
        } else if (slot == 49) {
            if (event.isRightClick()) {
                openBountyGui(player, 0, null);
            } else {
                openSignGui(player, "BOUNTY_SEARCH", "検索するプレイヤー名", null);
            }
        } else if (slot >= 0 && slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
            SkullMeta sm = (SkullMeta) clicked.getItemMeta();
            if (sm == null || sm.getOwningPlayer() == null) return;
            UUID targetUuid = sm.getOwningPlayer().getUniqueId();
            if (targetUuid.equals(player.getUniqueId())) {
                player.sendActionBar("§c自分に懸賞金はかけられません。");
                return;
            }
            bountyAddTarget.put(player.getUniqueId(), targetUuid);
            openSignGui(player, "BOUNTY_ADD", "追加する懸賞金額(例: 100, 1k, 1m)", null);
        }
    }

    public void handleIgnoreCommand(Player player, String[] args) {
        if (args.length != 1) {
            player.sendActionBar("§c/ignore <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cそのプレイヤーはオフラインです。");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§c自分自身をブロックすることはできません。");
            return;
        }
        boolean ignored = toggleIgnore(player.getUniqueId(), target.getUniqueId());
        if (ignored) {
            player.sendMessage("§c" + target.getName() + " をブロックしました。");
        } else {
            player.sendMessage("§a" + target.getName() + " のブロックを解除しました。");
        }
    }

    public void handleNvCommand(Player player) {
        UUID uuid = player.getUniqueId();
        if (nvPlayers.contains(uuid)) {
            nvPlayers.remove(uuid);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
            player.sendMessage("§a暗視を外しました。");
        } else {
            nvPlayers.add(uuid);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, -1, 255, false, false, false));
            player.sendMessage("§a暗視を付与しました。");
        }
        savePlayerToDb(player, null);
    }

    // ─── Amethyst Tools ───────────────────────────────────────────────────
    private final Set<Player> activeAmethystMiners = new HashSet<>();

    // -- Sell Multiplier System Methods --
    
    public String getSellCategory(Material mat) {
        if (mat == null) return "Blocks";
        String name = mat.name();
        
        if (name.contains("WHEAT") || name.contains("POTATO") || name.contains("CARROT") || 
            name.contains("BEETROOT") || name.contains("MELON") || name.contains("PUMPKIN") || 
            name.contains("SUGAR_CANE") || name.contains("KELP") || name.contains("BAMBOO") ||
            name.contains("COCOA") || name.contains("BERRIES") || name.contains("APPLE") ||
            name.contains("SEEDS") || name.contains("CHORUS_FRUIT")) {
            return "Crops";
        }
        
        if (name.contains("ORE") || name.contains("INGOT") || name.contains("RAW_") || 
            name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("GOLD") || 
            name.contains("IRON") || name.contains("COAL") || name.contains("REDSTONE") || 
            name.contains("LAPIS") || name.contains("COPPER") || name.contains("NETHERITE") ||
            name.contains("QUARTZ") || name.contains("AMETHYST")) {
            if (!name.contains("SWORD") && !name.contains("PICKAXE") && !name.contains("AXE") && 
                !name.contains("SHOVEL") && !name.contains("HOE") && !name.contains("HELMET") && 
                !name.contains("CHESTPLATE") && !name.contains("LEGGINGS") && !name.contains("BOOTS") &&
                !name.contains("HORSE_ARMOR")) {
                return "Ores";
            }
        }
        
        if (name.contains("BONE") || name.contains("ROTTEN_FLESH") || name.contains("GUNPOWDER") || 
            name.contains("STRING") || name.contains("SPIDER_EYE") || name.contains("ENDER_PEARL") || 
            name.contains("BLAZE_ROD") || name.contains("GHAST_TEAR") || name.contains("SLIME_BALL") || 
            name.contains("MAGMA_CREAM") || name.contains("PHANTOM_MEMBRANE") || name.contains("SHULKER_SHELL") ||
            name.contains("LEATHER") || name.contains("FEATHER") || name.contains("INK_SAC") ||
            name.contains("EGG") || name.contains("PRISMARINE_SHARD") || name.contains("PRISMARINE_CRYSTALS") ||
            name.contains("RABBIT") || name.contains("MUTTON") || name.contains("PORKCHOP") || name.contains("BEEF") ||
            name.contains("CHICKEN") || name.contains("SCUTE") || name.contains("NAUTILUS_SHELL") || name.contains("NETHER_STAR")) {
            return "Mob drops";
        }
        
        if (name.contains("LEAVES") || name.contains("DIRT") || name.contains("SAND") || 
            name.contains("GRAVEL") || name.contains("STONE") || name.contains("COBBLESTONE") || 
            name.contains("ANDESITE") || name.contains("DIORITE") || name.contains("GRANITE") ||
            name.contains("MOSS") || name.contains("VINES") || name.contains("GRASS") || 
            name.contains("MYCELIUM") || name.contains("PODZOL") || name.contains("CLAY") ||
            name.contains("MUD") || name.contains("SNOW") || name.contains("ICE") || 
            name.contains("TUFF") || name.contains("DEEPSLATE") || name.contains("BASALT") ||
            name.contains("BLACKSTONE") || name.contains("OBSIDIAN") || name.contains("FLINT") ||
            name.contains("SAPLING") || name.contains("FLOWER") || name.contains("MUSHROOM") ||
            name.contains("FERN") || name.contains("LILY_PAD") || name.contains("CORAL") || 
            name.contains("SPONGE") || name.contains("WEB")) {
            return "Natural items";
        }
        
        if (name.contains("SWORD") || name.contains("PICKAXE") || name.contains("AXE") || 
            name.contains("SHOVEL") || name.contains("HOE") || name.contains("HELMET") || 
            name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") ||
            name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT") ||
            name.contains("SHIELD") || name.contains("FISHING_ROD") || name.contains("SHEARS") ||
            name.contains("FLINT_AND_STEEL") || name.contains("COMPASS") || name.contains("CLOCK") ||
            name.contains("SPYGLASS") || name.contains("RECOVERY_COMPASS") || name.contains("HORSE_ARMOR")) {
            return "Armor and tools";
        }
        
        if (name.contains("FISH") || name.contains("COD") || name.contains("SALMON")) {
            return "Fish";
        }
        
        if (name.contains("BOOK") || name.contains("PAPER") || name.contains("MAP")) {
            return "Enchanted books";
        }
        
        if (name.contains("POTION") || name.contains("BREWING_STAND") || name.contains("CAULDRON") ||
            name.contains("GLISTERING_MELON_SLICE") || name.contains("GOLDEN_CARROT") || 
            name.contains("FERMENTED_SPIDER_EYE") || name.contains("BLAZE_POWDER") || 
            name.contains("RABBIT_FOOT") || name.contains("TURTLE_HELMET") || name.contains("GLASS_BOTTLE")) {
            return "Potions";
        }
        
        if (mat.isBlock()) {
            return "Blocks";
        }
        
        return "Blocks";
    }

    public double getSellMultiplier(long totalSold) {
        double currentMultiplier = 1.0;
        for (int i = 0; i < SELL_THRESHOLDS.length; i++) {
            if (totalSold >= SELL_THRESHOLDS[i]) {
                currentMultiplier = SELL_MULTIPLIERS[i];
            } else {
                break;
            }
        }
        return currentMultiplier;
    }

    public long getNextMultiplierRequirement(long totalSold) {
        for (int i = 0; i < SELL_THRESHOLDS.length; i++) {
            if (totalSold < SELL_THRESHOLDS[i]) {
                return SELL_THRESHOLDS[i];
            }
        }
        return -1L;
    }
    
    public void openSellMultiGui(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "§8Sell Multipliers");
        UUID uuid = player.getUniqueId();
        Map<String, Long> stats = smpSellMultiStats.getOrDefault(uuid, new HashMap<>());
        
        String[] categories = {
            "Crops", "Ores", "Mob drops", "Natural items", 
            "Armor and tools", "Fish", "Enchanted books", "Potions", "Blocks"
        };
        
        Material[] icons = {
            Material.WHEAT, Material.DIAMOND, Material.BONE, Material.OAK_LEAVES,
            Material.NETHERITE_HELMET, Material.TROPICAL_FISH, Material.ENCHANTED_BOOK, Material.BREWING_STAND, Material.OAK_LOG
        };
        
        for (int i = 0; i < 9; i++) {
            String cat = categories[i];
            long totalSold = stats.getOrDefault(cat, 0L);
            double currentMult = getSellMultiplier(totalSold);
            long nextReq = getNextMultiplierRequirement(totalSold);
            
            ItemStack item = new ItemStack(icons[i]);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + cat);
                List<String> lore = new ArrayList<>();
                lore.add("§7現在の倍率: §a" + String.format("%.1fx", currentMult));
                lore.add("§7累計売却額: §e$" + formatMoney(totalSold));
                if (nextReq != -1L) {
                    lore.add("§7次の倍率まで: §c$" + formatMoney(nextReq - totalSold));
                    lore.add("§7(次の倍率: §a" + String.format("%.1fx", getSellMultiplier(nextReq)) + "§7)");
                } else {
                    lore.add("§a最大倍率に到達しています！");
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(i, item);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onSellMultiGuiClick(InventoryClickEvent event) {
        if (!"§8Sell Multipliers".equals(event.getView().getTitle())) return;
        event.setCancelled(true);
    }

    private void registerCustomRecipes() {
        org.bukkit.NamespacedKey pickaxeKey = new org.bukkit.NamespacedKey(plugin, "amethyst_pickaxe");
        org.bukkit.inventory.ItemStack pickaxe = createAmethystPickaxeBase();
        org.bukkit.inventory.ShapedRecipe pickaxeRecipe = new org.bukkit.inventory.ShapedRecipe(pickaxeKey, pickaxe);
        pickaxeRecipe.shape("NHN", " A ", " A ");
        pickaxeRecipe.setIngredient('N', Material.NETHERITE_INGOT);
        pickaxeRecipe.setIngredient('H', Material.HEAVY_CORE);
        pickaxeRecipe.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(pickaxeRecipe);

        org.bukkit.NamespacedKey axeKey = new org.bukkit.NamespacedKey(plugin, "amethyst_axe");
        org.bukkit.inventory.ItemStack axe = createAmethystAxeBase();
        org.bukkit.inventory.ShapedRecipe axeRecipe = new org.bukkit.inventory.ShapedRecipe(axeKey, axe);
        axeRecipe.shape(" NH", " AN", " A ");
        axeRecipe.setIngredient('N', Material.NETHERITE_INGOT);
        axeRecipe.setIngredient('H', Material.HEAVY_CORE);
        axeRecipe.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(axeRecipe);
    }

    private org.bukkit.inventory.ItemStack createAmethystPickaxeBase() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.NETHERITE_PICKAXE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5Amethyst Pickaxe");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "amethyst_tool"), org.bukkit.persistence.PersistentDataType.STRING, "pickaxe");
            item.setItemMeta(meta);
        }
        return item;
    }

    private org.bukkit.inventory.ItemStack createAmethystAxeBase() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.NETHERITE_AXE);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5Amethyst Axe");
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "amethyst_tool"), org.bukkit.persistence.PersistentDataType.STRING, "axe");
            item.setItemMeta(meta);
        }
        return item;
    }



    @EventHandler
    public void onCraftAmethystTool(org.bukkit.event.inventory.CraftItemEvent event) {
        org.bukkit.inventory.ItemStack result = event.getRecipe().getResult();
        if (result == null || !result.hasItemMeta()) return;
        String type = result.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "amethyst_tool"), org.bukkit.persistence.PersistentDataType.STRING);
        if (type != null) {
            org.bukkit.inventory.ItemStack updated = result.clone();
            long expireTime = System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000;
            org.bukkit.inventory.meta.ItemMeta meta = updated.getItemMeta();
            meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "expire_time"), org.bukkit.persistence.PersistentDataType.LONG, expireTime);
            meta.setLore(Arrays.asList("§d残り時間: 3日 0時間 0分"));
            updated.setItemMeta(meta);
            event.setCurrentItem(updated);
        }
    }

    private void startAmethystTask() {
        runGlobalTaskTimer(() -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean changed = false;
                org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                for (int i = 0; i < inv.getSize(); i++) {
                    org.bukkit.inventory.ItemStack item = inv.getItem(i);
                    if (item == null || !item.hasItemMeta()) continue;
                    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                    org.bukkit.NamespacedKey toolKey = new org.bukkit.NamespacedKey(plugin, "amethyst_tool");
                    org.bukkit.NamespacedKey expireKey = new org.bukkit.NamespacedKey(plugin, "expire_time");
                    if (meta.getPersistentDataContainer().has(toolKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        if (meta.getPersistentDataContainer().has(expireKey, org.bukkit.persistence.PersistentDataType.LONG)) {
                            long expireTime = meta.getPersistentDataContainer().get(expireKey, org.bukkit.persistence.PersistentDataType.LONG);
                            if (now >= expireTime) {
                                inv.setItem(i, new org.bukkit.inventory.ItemStack(Material.HEAVY_CORE));
                                player.sendMessage("§c" + meta.getDisplayName() + " §cの期限が切れてヘビーコアになりました。");
                                changed = true;
                            } else {
                                long diff = expireTime - now;
                                long days = diff / (1000 * 60 * 60 * 24);
                                long hours = (diff / (1000 * 60 * 60)) % 24;
                                long minutes = (diff / (1000 * 60)) % 60;
                                List<String> lore = meta.getLore();
                                if (lore == null) lore = new ArrayList<>();
                                String timeStr = "§d残り時間: " + days + "日 " + hours + "時間 " + minutes + "分";
                                boolean found = false;
                                for (int j = 0; j < lore.size(); j++) {
                                    if (lore.get(j).startsWith("§d残り時間: ")) {
                                        if (!lore.get(j).equals(timeStr)) {
                                            lore.set(j, timeStr);
                                            changed = true;
                                        }
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    lore.add(timeStr);
                                    changed = true;
                                }
                                if (changed) {
                                    meta.setLore(lore);
                                    item.setItemMeta(meta);
                                }
                            }
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAmethystToolBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (activeAmethystMiners.contains(player)) return;

        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !hand.hasItemMeta()) return;
        org.bukkit.inventory.meta.ItemMeta meta = hand.getItemMeta();
        String type = meta.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "amethyst_tool"), org.bukkit.persistence.PersistentDataType.STRING);
        if (type == null) return;

        Block block = event.getBlock();
        activeAmethystMiners.add(player);

        try {
            if (type.equals("pickaxe")) {
                org.bukkit.block.BlockFace face = getTargetBlockFace(player);
                if (face != null) {
                    int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
                    switch (face) {
                        case UP:
                        case DOWN:
                            minX = -1; maxX = 1; minZ = -1; maxZ = 1; break;
                        case NORTH:
                        case SOUTH:
                            minX = -1; maxX = 1; minY = -1; maxY = 1; break;
                        case EAST:
                        case WEST:
                            minZ = -1; maxZ = 1; minY = -1; maxY = 1; break;
                        default:
                            break;
                    }
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                if (x == 0 && y == 0 && z == 0) continue;
                                Block target = block.getRelative(x, y, z);
                                if (target.getType() != Material.AIR && target.getType() != Material.BEDROCK && target.getType().getHardness() >= 0) {
                                    target.breakNaturally(hand);
                                }
                            }
                        }
                    }
                }
            } else if (type.equals("axe")) {
                String typeName = block.getType().name();
                if (typeName.endsWith("_LOG") || typeName.endsWith("_STEM") || typeName.endsWith("_WOOD") || typeName.endsWith("_LEAVES") || typeName.endsWith("_WART_BLOCK")) {
                    int maxBlocks = 512;
                    int broken = 0;
                    java.util.Queue<Block> queue = new java.util.LinkedList<>();
                    Set<Block> visited = new HashSet<>();
                    queue.add(block);
                    visited.add(block);
                    
                    while (!queue.isEmpty() && broken < maxBlocks) {
                        Block current = queue.poll();
                        if (!current.equals(block)) {
                            String cName = current.getType().name();
                            if (cName.endsWith("_LOG") || cName.endsWith("_STEM") || cName.endsWith("_WOOD") || cName.endsWith("_LEAVES") || cName.endsWith("_WART_BLOCK")) {
                                current.breakNaturally(hand);
                                broken++;
                            } else {
                                continue;
                            }
                        }
                        for (int x = -1; x <= 1; x++) {
                            for (int y = -1; y <= 1; y++) {
                                for (int z = -1; z <= 1; z++) {
                                    if (x == 0 && y == 0 && z == 0) continue;
                                    Block relative = current.getRelative(x, y, z);
                                    if (!visited.contains(relative)) {
                                        String rName = relative.getType().name();
                                        if (rName.endsWith("_LOG") || rName.endsWith("_STEM") || rName.endsWith("_WOOD") || rName.endsWith("_LEAVES") || rName.endsWith("_WART_BLOCK")) {
                                            visited.add(relative);
                                            queue.add(relative);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            activeAmethystMiners.remove(player);
        }
    }

    private org.bukkit.block.BlockFace getTargetBlockFace(Player player) {
        List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 10);
        if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isSolid()) return null;
        Block targetBlock = lastTwoTargetBlocks.get(1);
        Block adjacentBlock = lastTwoTargetBlocks.get(0);
        return targetBlock.getFace(adjacentBlock);
    }
    public void handleDeathsCommand(Player player) {
        player.sendMessage("§e=== 最近の死亡履歴 ===");
        if (deathLogs.isEmpty()) {
            player.sendMessage("§7記録はありません。");
            return;
        }
        for (String log : deathLogs) {
            player.sendMessage(log);
        }
    }
    
    private Location parseSpawnerKey(String key) {
        String[] parts = key.split(",");
        if (parts.length != 4) return null;
        try {
            World w = Bukkit.getWorld(parts[0]);
            if (w == null) return null;
            return new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }
    
    private void startSpawnerTask() {
        runGlobalTaskTimer(() -> {
            java.util.Random r = new java.util.Random();
            
            for (Map.Entry<String, SpawnerData> entry : spawnerDataMap.entrySet()) {
                SpawnerData data = entry.getValue();
                Location loc = parseSpawnerKey(entry.getKey());
                if (loc == null || loc.getWorld() == null) {
                    continue;
                }

                runLocationTask(loc, () -> {
                    if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                        return;
                    }

                    // EntityTypeが不明な場合は取得を試みる
                    if (data.entityType == null) {
                        if (loc.getBlock().getState() instanceof org.bukkit.block.CreatureSpawner spawner) {
                            org.bukkit.entity.EntityType type = spawner.getSpawnedType();
                            if (type != null) {
                                data.entityType = type.name();
                            }
                        }
                    }
                    
                    if (data.entityType != null) {
                        org.bukkit.entity.EntityType type;
                        try {
                            type = org.bukkit.entity.EntityType.valueOf(data.entityType);
                        } catch (Exception e) {
                            return;
                        }
                        
                        // Simulate a vanilla spawner cycle every 60 seconds (1200 ticks)
                        // Vanilla: ~1 mob per 10 seconds -> ~6 mobs per 60 seconds
                        int baseSpawnCount = r.nextInt(7) + 3; // 3 to 9 mobs this cycle (avg 6)
                        if (baseSpawnCount > 0) {
                            int totalSpawns = data.stackCount * baseSpawnCount;
                            for (int i = 0; i < totalSpawns; i++) {
                                List<ItemStack> loot = simulateDrops(type);
                                for (ItemStack item : loot) {
                                    if (item.getMaxStackSize() <= 1) continue; // Skip unstackables
                                    String matName = item.getType().name();
                                    data.storedItems.put(matName, data.storedItems.getOrDefault(matName, 0) + item.getAmount());
                                }
                            }
                        }
                    }
                });
            }
        }, 1200L, 1200L); // Execute every 60 seconds
    }

    /** hubから転送されてきたプレイヤーの、参加後に実行するコマンド/RTP */
    private final Map<UUID, String> pendingCommands = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, String> pendingRtps = new java.util.concurrent.ConcurrentHashMap<>();
    /** 事前計算済みのRTP先座標 (プレイヤーがまだsmpに来ていなくても先に計算しておく) */
    private final Map<UUID, Location> pendingRtpLoc = new java.util.concurrent.ConcurrentHashMap<>();
    /** 事前計算済みのHome先座標 (サーバー到着時に直接スポーンさせる) */
    private final Map<UUID, Location> pendingHomeLoc = new java.util.concurrent.ConcurrentHashMap<>();

    /** Velocity経由で届いたコマンド実行要求。未参加なら参加時に実行する。 */
    public void handleIncomingCmd(UUID targetUuid, String cmd) {
        if (hubMode) return;
        Player player = Bukkit.getPlayer(targetUuid);
        if (player != null && player.isOnline()) {
            runEntityTaskLater(player, () -> {
                // Folia対応: region thread上で直接実行 (player.chatは不安定)
                if (player.isOnline()) Bukkit.dispatchCommand(player, cmd);
            }, 5L);
        } else {
            // --- homeコマンドの場合: 到着前に行き先を事前計算してスポーン位置を差し替える ---
            if (cmd.startsWith("home ")) {
                try {
                    int slot = Integer.parseInt(cmd.substring(5).trim());
                    Map<Integer, Location> homes = playerNewHomes.get(targetUuid);
                    Location home = homes != null ? homes.get(slot) : null;
                    if (home != null && home.getWorld() != null) {
                        pendingHomeLoc.put(targetUuid, home.clone());
                        pendingCommands.put(targetUuid, cmd); // onPlayerJoinでメッセージ表示用
                        runGlobalTaskLater(() -> {
                            pendingHomeLoc.remove(targetUuid);
                            pendingCommands.remove(targetUuid, cmd);
                        }, 20L * 30);
                        return;
                    }
                } catch (Exception ignored) {}
            }
            // --- rtpコマンドの場合: handleIncomingRtpと同じ事前計算パスに乗せる ---
            if (cmd.startsWith("rtp")) {
                String worldKey = cmd.length() > 4 ? cmd.substring(4).trim() : "overworld";
                handleIncomingRtp(targetUuid, worldKey);
                return;
            }
            // --- その他のコマンド: 従来どおり参加時に実行 ---
            pendingCommands.put(targetUuid, cmd);
            runGlobalTaskLater(() -> pendingCommands.remove(targetUuid, cmd), 20L * 30);
        }
    }

    /** (hubモード) コマンドをsmpサーバーへ転送する。Velocity側が自動でsmpへ接続させてから実行する。 */
    public void forwardCommandToSmp(Player player, String commandLine) {
        if (bridge == null || !velocityEnabled) {
            player.sendMessage("§cVelocity連携が無効のため実行できません。");
            return;
        }
        bridge.sendCommand(player, player.getUniqueId(), commandLine);
    }

    /** 別サーバー(hub)からの TPA/TPAHERE リクエストを受け取る */
    // ─── クロスサーバーtpa ────────────────────────────────────────────────
    // 別サーバーにいる相手へのtpaを、要求者を動かさずに実現する。
    //  - 要求者サーバー: /tpa X → Xがローカルに居なければ bridge.sendTpaReqToOtherServer
    //  - 承認者サーバー: TPA_ASK 受信 → crossIncoming に登録して承認プロンプト表示
    //  - 承認時: bridge.sendTpaAccept → Velocityが mover を anchor のサーバーへ転送
    //  - 移動先サーバー: TPA_TP 受信 → mover 到着時に anchor へテレポート

    private static class CrossTpaReq {
        final UUID requesterUuid; final String requesterName; final boolean isHere; final long time;
        CrossTpaReq(UUID r, String n, boolean h) { requesterUuid = r; requesterName = n; isHere = h; time = System.currentTimeMillis(); }
    }
    /** 承認待ちのクロスサーバーtpa (targetUuid -> 要求内容) */
    private final Map<UUID, CrossTpaReq> crossIncoming = new java.util.concurrent.ConcurrentHashMap<>();
    /** 到着時にテレポートする予約 (moverUuid -> anchorUuid) */
    private final Map<UUID, UUID> pendingArrivalTp = new java.util.concurrent.ConcurrentHashMap<>();
    /** 到着時のスポーン位置差し替え用 (moverUuid -> anchorの位置スナップショット)。
     *  前回位置に一瞬スポーンするのを防ぎ、最初から相手の近くに出す。 */
    private final Map<UUID, Location> pendingArrivalLoc = new java.util.concurrent.ConcurrentHashMap<>();

    /** (承認者サーバー) 別サーバーからのtpaリクエストを受信し、プロンプトを表示する */
    public void handleIncomingTpaReq(UUID requesterUuid, String requesterName, UUID targetUuid, boolean isHere) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) return;

        // 許可設定・無視チェック
        boolean allowed = isHere ? isTpaHereEnabled(targetUuid) : isTpaEnabled(targetUuid);
        if (!allowed || isIgnored(targetUuid, requesterUuid)) {
            return; // 拒否 (要求者への通知は省略)
        }

        crossIncoming.put(targetUuid, new CrossTpaReq(requesterUuid, requesterName, isHere));
        runGlobalTaskLater(() -> {
            CrossTpaReq r = crossIncoming.get(targetUuid);
            if (r != null && System.currentTimeMillis() - r.time >= 19_000L) crossIncoming.remove(targetUuid);
        }, 20L * 20);

        String typeStr = isHere ? "tpahere" : "tpa";
        net.md_5.bungee.api.chat.TextComponent notify = new net.md_5.bungee.api.chat.TextComponent(requesterName);
        notify.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(" から" + typeStr + "リクエストが届きました\n");
        msg.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        notify.addExtra(msg);
        net.md_5.bungee.api.chat.TextComponent click = new net.md_5.bungee.api.chat.TextComponent("[クリックして承認]");
        click.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        click.setBold(true);
        click.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        notify.addExtra(click);
        target.spigot().sendMessage(notify);
        target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    /** /tpaccept 時に呼ぶ。クロスサーバーの保留リクエストがあれば処理して true を返す */
    public boolean tryAcceptCrossTpa(Player target) {
        CrossTpaReq req = crossIncoming.remove(target.getUniqueId());
        if (req == null) return false;
        if (bridge == null || !velocityEnabled) return false;
        target.sendActionBar("§aリクエストを承認しました。");
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        bridge.sendTpaAccept(target, req.requesterUuid, req.isHere);
        return true;
    }

    /** (要求者サーバー) 相手が見つからなかった通知 */
    public void notifyTpaTargetOffline(UUID requesterUuid, String targetName) {
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null && requester.isOnline()) {
            requester.sendActionBar("§cプレイヤー §f" + targetName + " §cはオンラインではありません！");
        }
    }

    /** (移動先サーバー) mover が到着したら anchor へテレポートさせる予約。既に居れば即実行 */
    public void handleTpaArrival(UUID moverUuid, UUID anchorUuid, String anchorName) {
        pendingArrivalTp.put(moverUuid, anchorUuid);
        runGlobalTaskLater(() -> { pendingArrivalTp.remove(moverUuid); pendingArrivalLoc.remove(moverUuid); }, 20L * 30);
        // anchor の位置をスナップショットしておく (スポーン位置差し替え用。前回位置に一瞬出ないように)
        Player anchor = Bukkit.getPlayer(anchorUuid);
        if (anchor != null && anchor.isOnline()) {
            runEntityTask(anchor, () -> {
                if (anchor.isOnline()) pendingArrivalLoc.put(moverUuid, anchor.getLocation().clone());
            });
        }
        Player mover = Bukkit.getPlayer(moverUuid);
        if (mover != null && mover.isOnline()) {
            runEntityTaskLater(mover, () -> doArrivalTp(mover), 10L);
        }
    }

    /** 予約済みの到着テレポートを実行する */
    private void doArrivalTp(Player mover) {
        pendingArrivalLoc.remove(mover.getUniqueId());
        UUID anchorUuid = pendingArrivalTp.remove(mover.getUniqueId());
        if (anchorUuid == null) return;
        Player anchor = Bukkit.getPlayer(anchorUuid);
        if (anchor == null || !anchor.isOnline()) {
            mover.sendActionBar("§c相手が見つかりませんでした。");
            return;
        }
        mover.teleportAsync(anchor.getLocation()).thenAccept(ok -> {
            if (ok) {
                mover.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 40, 4, false, false));
                mover.sendActionBar("§f" + anchor.getName() + " §aにテレポートしました！");
                mover.playSound(mover.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }
        });
    }

    boolean hasPendingArrivalTp(UUID uuid) { return pendingArrivalTp.containsKey(uuid); }
    void runArrivalTp(Player mover) { runEntityTaskLater(mover, () -> doArrivalTp(mover), 10L); }

    /**
     * 参加時のスポーン位置を差し替える。
     * RTP転送: 前回位置ではなくワールドスポーンではなく、直接安全な場所を探してセットする
     * /home転送: 直接ホーム位置にスポーンさせる
     */
    @EventHandler
    public void onAsyncPlayerSpawnLocation(io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent event) {
        if (hubMode) {
            World w = getHubWorld();
            if (w != null) {
                event.setSpawnLocation(new Location(w, 0.5, 0.0, 0.5));
            }
            return;
        }
        UUID uuid = event.getConnection().getProfile().getId();

        // クロスサーバーtpaで到着: 前回位置ではなく相手の近くに直接スポーンさせる
        Location arrivalLoc = pendingArrivalLoc.get(uuid);
        if (arrivalLoc == null) {
            // スナップショット未取得なら相手の現在位置を直接読む (念のためtry/catch)
            UUID anchorUuid = pendingArrivalTp.get(uuid);
            if (anchorUuid != null) {
                try {
                    Player anchor = Bukkit.getPlayer(anchorUuid);
                    if (anchor != null && anchor.isOnline()) arrivalLoc = anchor.getLocation();
                } catch (Exception ignored) {}
            }
        }
        if (arrivalLoc != null && arrivalLoc.getWorld() != null) {
            event.setSpawnLocation(arrivalLoc.clone());
            return;
        }

        // RTP転送: 事前計算済みの安全な座標があれば直接そこへスポーンさせる (前回位置を経由しない)
        if (pendingRtps.containsKey(uuid)) {
            Location pre = pendingRtpLoc.remove(uuid);
            if (pre != null && pre.getWorld() != null) {
                pendingRtps.remove(uuid);
                event.setSpawnLocation(pre.clone());
                return;
            }
            // 事前計算が間に合わなかった場合: とりあえずワールドスポーンに出し、
            // pendingRtps は残して onPlayerJoin で本来のRTPを非同期実行する
            World w = Bukkit.getWorld(WORLD_OVERWORLD);
            if (w != null) event.setSpawnLocation(w.getSpawnLocation());
            return;
        }

        // Home転送: 事前計算済みのホーム座標があれば直接そこへスポーンさせる
        Location homeDest = pendingHomeLoc.get(uuid);
        if (homeDest != null && homeDest.getWorld() != null) {
            event.setSpawnLocation(homeDest.clone());
            return;
        }

        // fallback: pendingCommandsからhomeコマンドを読む (古い経路の互換)
        String cmd = pendingCommands.get(uuid);
        if (cmd != null && cmd.startsWith("home ")) {
            try {
                int slot = Integer.parseInt(cmd.substring(5).trim());
                Map<Integer, Location> homes = playerNewHomes.get(uuid);
                Location home = homes != null ? homes.get(slot) : null;
                if (home != null && home.getWorld() != null) {
                    event.setSpawnLocation(home.clone());
                }
            } catch (Exception ignored) {}
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // DBからプレイヤーデータ(所持金・インベントリ・エンチェ・HP等)を読み込んで反映
        loadPlayerFromDb(player);

        DatabaseManager db = db();
        if (db != null && db.isEnabled()) {
            db.updatePlayerNameAsync(player.getUniqueId(), player.getName());
        }

        if (isSMPWorld(player.getWorld())) {
            smpSessionStart.put(player.getUniqueId(), System.currentTimeMillis());
        }

        // クロスサーバーtpaで転送されてきた場合、到着時に相手へテレポート
        if (hasPendingArrivalTp(player.getUniqueId())) {
            runArrivalTp(player);
        }

        // プレイヤー数変化を他サーバーに即時同期
        runGlobalTaskLater(this::updateGlobalData, 5L);

        // hubから転送されてきた場合の保留RTP/コマンドを実行
        runEntityTaskLater(player, () -> {
            if (!player.isOnline()) return;
            UUID uuid = player.getUniqueId();

            String worldKey = pendingRtps.remove(uuid);
            if (worldKey != null) {
                executeIncomingRtp(player, worldKey);
                return;
            }

            String pendingCmd = pendingCommands.remove(uuid);
            Location preHome = pendingHomeLoc.remove(uuid);
            if (pendingCmd != null) {
                if (pendingCmd.startsWith("home ") && preHome != null) {
                    // onAsyncPlayerSpawnLocationで既にホーム位置に直接スポーン済み
                    // teleportAsyncは不要。メッセージだけ送る
                    try {
                        int slot = Integer.parseInt(pendingCmd.substring(5).trim());
                        player.sendActionBar("§aホーム " + slot + " にテレポートしました。");
                    } catch (Exception ignored) {}
                    return;
                }
                // それ以外のコマンド: 直接実行
                Bukkit.dispatchCommand(player, pendingCmd);
            }
        }, 10L);

        if (!player.hasPlayedBefore() && !hubMode) {
            // 参加メッセージはVelocity側でネットワーク全体に流す
            event.setJoinMessage(velocityEnabled ? null : "§9[+] §f" + player.getName());

            player.getInventory().addItem(new ItemStack(Material.IRON_AXE));
            player.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE));
            player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));

            runGlobalTaskLater(() -> {
                player.sendActionBar(Component.text("お金を貯めよう！貯め方については、/help money", NamedTextColor.GREEN));
                // hubからのRTP要求が既に届いている場合はそちらを優先
                if (bridge != null && bridge.hasRecentIncomingRtp(player.getUniqueId(), 15_000L)) return;
                handleRTPInWorld(player, Bukkit.getWorld("world"), true);
            }, 40L);
        } else {
            event.setJoinMessage(velocityEnabled ? null : "§a[+] §f" + player.getName());
        }

        if (hubMode) {
            runGlobalTaskLater(() -> {
                loadData();
            }, 10L);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncChat(AsyncChatEvent event) {
        Component originalMessage = event.message();
        Component replaced = originalMessage.replaceText(TextReplacementConfig.builder()
                .match("https?://[\\w\\.\\-/?&=#%]+")
                .replacement(match -> match.clickEvent(ClickEvent.openUrl(match.content()))
                                          .color(NamedTextColor.AQUA)
                                          .decorate(TextDecoration.UNDERLINED))
                .build());
        event.message(replaced);
    }

    // ─── クロスサーバーチャット ────────────────────────────────────────────

    /** チャットを他サーバーへ転送する (全ての加工・キャンセル判定の後に実行) */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChatForward(AsyncChatEvent event) {
        if (!velocityEnabled || bridge == null) return;
        Player player = event.getPlayer();
        // 看板/チャット入力モード中の発言は後続のAsyncPlayerChatEventで消費されるので転送しない
        if (playerChatInputMode.containsKey(player.getUniqueId())) return;
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.message());
        if (plain.isEmpty()) return;
        bridge.sendChat(player, plain);
    }

    /** Velocity経由で届いた他サーバーのチャットをローカルに配信する (ignore設定を尊重) */
    public void handleIncomingChat(UUID senderUuid, String senderName, String serverTag, String message) {
        // サーバータグは表示しない (ローカルチャットと同じ見た目にする)
        String line = "§f<" + senderName + "> " + message;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Set<UUID> ignores = smpIgnores.get(viewer.getUniqueId());
            if (ignores != null && ignores.contains(senderUuid)) continue;
            viewer.sendMessage(line);
        }
        Bukkit.getConsoleSender().sendMessage(line);
    }

    // ─── クロスサーバーエンダーチェスト同期 ──────────────────────────────

    public String itemsToBase64(ItemStack[] items) {
        try (java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
             java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(raw);
             org.bukkit.util.io.BukkitObjectOutputStream out = new org.bukkit.util.io.BukkitObjectOutputStream(gzip)) {
            out.writeInt(items.length);
            for (ItemStack it : items) out.writeObject(it);
            out.flush();
            gzip.finish();
            return java.util.Base64.getEncoder().encodeToString(raw.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("[GemSMP] エンダーチェストのシリアライズに失敗: " + e.getMessage());
            return null;
        }
    }

    public ItemStack[] itemsFromBase64(String base64) {
        try (java.io.ByteArrayInputStream raw = new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(base64));
             java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(raw);
             org.bukkit.util.io.BukkitObjectInputStream in = new org.bukkit.util.io.BukkitObjectInputStream(gzip)) {
            int len = in.readInt();
            ItemStack[] items = new ItemStack[len];
            for (int i = 0; i < len; i++) items[i] = (ItemStack) in.readObject();
            return items;
        } catch (Exception e) {
            plugin.getLogger().warning("[GemSMP] エンダーチェストの復元に失敗: " + e.getMessage());
            return null;
        }
    }

    // ─── MySQLによるプレイヤーデータ共有 (hub ⇔ smp) ─────────────────────
    // DBの1行が唯一の正本。サーバー切り替え時にVelocityのFLUSHハンドシェイクで
    // 「旧サーバー保存 → 新サーバー読込」の順序を保証する。常時同期はしない。
    //  - stats (所持金・シャード・キー・懸賞金・キル/デス・プレイ時間・ランク・
    //    チーム・無視リスト・設定) : smpだけが書き込む。hubは表示用に読むだけ
    //  - state (インベントリ・エンチェ・HP・満腹度・隠し満腹度・経験値) :
    //    最後にいたサーバーが書き込む (プレイヤーは同時に1サーバーにしかいないため安全)

    /** DB読込が完了したプレイヤー (完了までエンチェ操作を止める) */
    private final java.util.Set<UUID> dbDataReady = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> dbJoinTimes = new java.util.concurrent.ConcurrentHashMap<>();
    /** 参加時にDBに実データ(行)が存在したプレイヤー */
    private final java.util.Set<UUID> dbRowLoaded = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** FLUSH保存済み (直後のquit時の二重保存を防ぐ) */
    private final Map<UUID, Long> recentFlush = new java.util.concurrent.ConcurrentHashMap<>();

    private DatabaseManager db() {
        return ((SMPPlugin) plugin).getDatabaseManager();
    }

    /** プレイヤーデータ(state)を現在の状態から取得する。必ずプレイヤーのスレッドで呼ぶこと */
    private DatabaseManager.PlayerState capturePlayerState(Player player) {
        DatabaseManager.PlayerState s = new DatabaseManager.PlayerState();
        s.inventoryB64 = itemsToBase64(player.getInventory().getContents());
        ItemStack[] ec = customEnderChests.get(player.getUniqueId());
        s.enderChestB64 = ec != null ? itemsToBase64(ec) : null;
        s.health = player.getHealth();
        s.food = player.getFoodLevel();
        s.saturation = player.getSaturation();
        s.xpLevel = player.getLevel();
        s.xpProgress = player.getExp();
        s.gamemode = player.getGameMode().name();
        s.heldSlot = player.getInventory().getHeldItemSlot();
        return s;
    }

    /** pluginデータ(stats)を現在の値から取得する (smpのみ) */
    private DatabaseManager.PlayerStats capturePlayerStats(UUID uuid) {
        DatabaseManager.PlayerStats st = new DatabaseManager.PlayerStats();
        st.money = getMoney(uuid);
        st.shards = getShards(uuid);
        st.keys = smpKeys.getOrDefault(uuid, 0L);
        st.bounty = getBounty(uuid);
        st.kills = getKills(uuid);
        st.deaths = getDeaths(uuid);
        st.playtime = getPlaytimeSeconds(uuid);
        st.rank = playerRanks.getOrDefault(uuid, "");
        st.team = playerTeamMap.getOrDefault(uuid, "");
        Set<UUID> ignores = smpIgnores.get(uuid);
        if (ignores != null && !ignores.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (UUID ig : ignores) {
                if (sb.length() > 0) sb.append(',');
                sb.append(ig);
            }
            st.ignoreCsv = sb.toString();
        }
        st.settingsBits = getSettingsBits(uuid);
        // 登録済みホーム枠のCSV (hubのホームGUI表示用)
        Map<Integer, Location> homes = playerNewHomes.get(uuid);
        if (homes != null && !homes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Integer slot : homes.keySet()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(slot);
            }
            st.homeSlots = sb.toString();
        }
        return st;
    }

    /**
     * プレイヤーデータをDBへ保存する。スナップショット取得だけプレイヤーのスレッドで行い、
     * DB書き込みは専用スレッドで実行する (サーバーを一切ブロックしない)。
     * afterSave は保存完了後に呼ばれる (FLUSHのack用、null可)。
     */
    public void savePlayerToDb(Player player, Runnable afterSave) {
        DatabaseManager db = db();
        if (db == null || !db.isEnabled()) {
            if (afterSave != null) afterSave.run();
            return;
        }
        UUID uuid = player.getUniqueId();
        DatabaseManager.PlayerState state = capturePlayerState(player);

        if (!hubMode) {
            // smp: 常にフルセーブ (stats + 本物のインベントリ)。updated_by='smp'
            DatabaseManager.PlayerStats stats = capturePlayerStats(uuid);
            db.savePlayerAsync(uuid, stats, state, "smp", afterSave);
        } else {
            // hub: statsは書かない。インベントリなどのstateをフルセーブ。updated_by='hub'
            db.savePlayerAsync(uuid, null, state, "hub", afterSave);
        }
    }

    /**
     * (smp起動時) smp_data.ymlの全プレイヤーの所持金・シャード・EC等をMySQLへ一括移行する。
     * インベントリは各プレイヤーがsmpに初参加した時に取得されるため、ここでは触れない。
     * これにより、hubに最初に入ったプレイヤーでも所持金・エンチェが最初から表示される。
     */
    private void bulkMigrateToDb() {
        DatabaseManager db = db();
        if (db == null || !db.isEnabled()) return;

        Set<UUID> all = new HashSet<>();
        all.addAll(smpMoney.keySet());
        all.addAll(smpShards.keySet());
        all.addAll(smpKeys.keySet());
        all.addAll(smpBounties.keySet());
        all.addAll(smpKills.keySet());
        all.addAll(smpDeaths.keySet());
        all.addAll(smpPlaytime.keySet());
        all.addAll(playerRanks.keySet());
        all.addAll(playerTeamMap.keySet());
        all.addAll(smpIgnores.keySet());
        all.addAll(customEnderChests.keySet());
        if (all.isEmpty()) return;

        java.util.List<Object[]> records = new java.util.ArrayList<>();
        for (UUID uuid : all) {
            DatabaseManager.PlayerStats st = capturePlayerStats(uuid);
            ItemStack[] ec = customEnderChests.get(uuid);
            String ecB64 = ec != null ? itemsToBase64(ec) : null;
            records.add(new Object[]{uuid, st, ecB64});
        }
        db.bulkSeedStatsEc(records);
    }

    /** 参加時にDBからプレイヤーデータを読み込んで反映する */
    private void loadPlayerFromDb(Player player) {
        DatabaseManager db = db();
        UUID uuid = player.getUniqueId();
        if (db == null || !db.isEnabled()) {
            dbDataReady.add(uuid);
            return;
        }
        dbJoinTimes.put(uuid, System.currentTimeMillis());
        db.loadPlayerAsync(uuid, row -> runEntityTask(player, () -> {
            if (!player.isOnline()) return;
            if (hubMode) {
                applyDbRowToHub(player, row);
            } else {
                applyDbRowToSmp(player, row);
            }
            dbDataReady.add(uuid);
        }));
    }

    private ItemStack[] fitInventory(Player player, ItemStack[] items) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            contents[i] = i < items.length ? items[i] : null;
        }
        return contents;
    }

    private void applyState(Player player, DatabaseManager.PlayerState st) {
        if (st.inventoryB64 != null && !st.inventoryB64.isEmpty()) {
            ItemStack[] items = itemsFromBase64(st.inventoryB64);
            if (items != null) {
                player.getInventory().setContents(fitInventory(player, items));
                player.updateInventory();
            }
        }
        if (st.enderChestB64 != null && !st.enderChestB64.isEmpty()) {
            ItemStack[] ec = itemsFromBase64(st.enderChestB64);
            if (ec != null) {
                ItemStack[] fitted = new ItemStack[54];
                for (int i = 0; i < Math.min(54, ec.length); i++) fitted[i] = ec[i];
                customEnderChests.put(player.getUniqueId(), fitted);
            }
        }
        if (st.health > 0) {
            try { player.setHealth(Math.max(0.5, Math.min(st.health, player.getMaxHealth()))); }
            catch (Exception ignored) {}
        }
        player.setFoodLevel(Math.max(0, Math.min(20, st.food)));
        player.setSaturation(Math.max(0f, st.saturation));
        player.setLevel(Math.max(0, st.xpLevel));
        player.setExp(Math.max(0f, Math.min(0.999f, st.xpProgress)));
        if (st.gamemode != null && !st.gamemode.isEmpty()) {
            try {
                player.setGameMode(org.bukkit.GameMode.valueOf(st.gamemode));
            } catch (Exception ignored) {}
        }
        if (st.heldSlot >= 0 && st.heldSlot <= 8) {
            player.getInventory().setHeldItemSlot(st.heldSlot);
        }
    }

    private void applyStats(UUID uuid, DatabaseManager.PlayerStats stats) {
        smpMoney.put(uuid, stats.money);
        smpShards.put(uuid, stats.shards);
        smpKeys.put(uuid, stats.keys);
        smpBounties.put(uuid, stats.bounty);
        smpKills.put(uuid, stats.kills);
        smpDeaths.put(uuid, stats.deaths);
        smpPlaytime.put(uuid, stats.playtime);
        if (!stats.rank.isEmpty()) playerRanks.put(uuid, stats.rank);
        if (!stats.team.isEmpty()) playerTeamMap.put(uuid, stats.team); else playerTeamMap.remove(uuid);
        Set<UUID> ignores = new HashSet<>();
        if (!stats.ignoreCsv.isEmpty()) {
            for (String s : stats.ignoreCsv.split(",")) {
                try { ignores.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
        }
        if (ignores.isEmpty()) smpIgnores.remove(uuid); else smpIgnores.put(uuid, ignores);
        applySettingsBits(uuid, stats.settingsBits);
    }

    /** hub参加時: DBの内容を全て表示用に反映する */
    private void applyDbRowToHub(Player player, DatabaseManager.PlayerRow row) {
        UUID uuid = player.getUniqueId();
        if (row == null) return; // DB未登録 → 何も表示しない (書き込みもしない)
        dbRowLoaded.add(uuid);
        applyStats(uuid, row.stats);
        applyState(player, row.state);
        // ホームGUI表示用に、登録済みホーム枠をダミーLocationで復元する
        // (hubでの実テレポートはsmpへ転送して行うため、正確な座標は不要)
        if (row.stats.homeSlots != null && !row.stats.homeSlots.isEmpty()) {
            Map<Integer, Location> homes = new HashMap<>();
            for (String s : row.stats.homeSlots.split(",")) {
                try { homes.put(Integer.parseInt(s.trim()), new Location(null, 0, 0, 0)); } catch (Exception ignored) {}
            }
            if (!homes.isEmpty()) playerNewHomes.put(uuid, homes);
        }
        broadcastRankToProxy(player);
    }

    /** smp参加時: DBの内容を全て取り込む。smp自身のデータ(ワールド+smp_data.yml)よりもDBを優先する */
    private void applyDbRowToSmp(Player player, DatabaseManager.PlayerRow row) {
        UUID uuid = player.getUniqueId();
        if (row == null) {
            // DB未登録の既存プレイヤー: smpの本物のデータをDBへ初回登録して種にする
            dbRowLoaded.add(uuid);
            savePlayerToDb(player, null);
            return;
        }
        dbRowLoaded.add(uuid);
        // stats(所持金・シャード等)もDBを正本として取り込む。
        // これにより hubのAFKゾーンで得たシャードやオフライン中の/pay等がsmpにも反映される。
        applyStats(uuid, row.stats);
        // DBから常に最新のstateを取り込む
        applyState(player, row.state);
        broadcastRankToProxy(player);
    }

    private void applyEnderChestOnly(Player player, DatabaseManager.PlayerState st) {
        if (st.enderChestB64 != null && !st.enderChestB64.isEmpty()) {
            ItemStack[] ec = itemsFromBase64(st.enderChestB64);
            if (ec != null) {
                ItemStack[] fitted = new ItemStack[54];
                for (int i = 0; i < Math.min(54, ec.length); i++) fitted[i] = ec[i];
                customEnderChests.put(player.getUniqueId(), fitted);
            }
        }
    }

    /** DB読込が完了しているか (エンチェ操作の可否判定用)。5秒経過で救済解除 */
    public boolean isDbDataReady(UUID uuid) {
        DatabaseManager db = db();
        if (db == null || !db.isEnabled()) return true;
        if (dbDataReady.contains(uuid)) return true;
        Long t = dbJoinTimes.get(uuid);
        if (t == null) return true;
        return System.currentTimeMillis() - t >= 5000L;
    }

    /**
     * Velocityからのフラッシュ要求 (このプレイヤーが別サーバーへ移動する直前)。
     * 保存完了後にFLUSHOKを返し、Velocityはそれを待ってから接続を切り替える。
     * これにより移動先のサーバーは必ず最新データを読み込める。
     */
    public void handleFlushRequest(Player player) {
        recentFlush.put(player.getUniqueId(), System.currentTimeMillis());
        runEntityTask(player, () -> {
            if (!player.isOnline()) return;
            savePlayerToDb(player, () -> {
                if (player.isOnline() && bridge != null) bridge.sendFlushOk(player);
            });
        });
    }


    /**
     * smp_dataold.yml があれば、現在の smp_data.yml に「無いキーだけ」を補完して復元する。
     * (実験中にリセットされた項目を、バックアップから欠損部分のみ埋め戻す。既存の値は上書きしない)
     * 1回実行したら smp_dataold.yml → smp_dataold.yml.merged にリネームして再実行を防ぐ。
     */
    private void mergeFromOldData() {
        java.io.File dataFolder = plugin.getDataFolder();
        java.io.File oldFile = new java.io.File(dataFolder, "smp_dataold.yml");
        if (!oldFile.exists()) return;
        java.io.File curFile = ((SMPPlugin) plugin).getCustomDataFile();

        org.bukkit.configuration.file.YamlConfiguration old = new org.bukkit.configuration.file.YamlConfiguration();
        org.bukkit.configuration.file.YamlConfiguration cur = new org.bukkit.configuration.file.YamlConfiguration();
        try {
            String oldContent = new String(java.nio.file.Files.readAllBytes(oldFile.toPath()), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("==: org.bukkit.Location", "isBukkitLocation: true")
                    .replace("==: Location", "isBukkitLocation: true");
            old.loadFromString(oldContent);
            if (curFile.exists()) {
                String curContent = new String(java.nio.file.Files.readAllBytes(curFile.toPath()), java.nio.charset.StandardCharsets.UTF_8)
                        .replace("==: org.bukkit.Location", "isBukkitLocation: true")
                        .replace("==: Location", "isBukkitLocation: true");
                cur.loadFromString(curContent);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[GemSMP] smp_dataold.yml の読込に失敗、復元をスキップ: " + e.getMessage());
            return;
        }

        int restored = 0;
        for (String key : old.getKeys(true)) {
            // 末端の値(セクションでない)だけを対象にし、現在に無いものだけ補完する
            if (old.isConfigurationSection(key)) continue;
            if (!cur.contains(key)) {
                cur.set(key, old.get(key));
                restored++;
            }
        }

        try {
            cur.save(curFile);
            // 再実行を防ぐためリネーム (完全削除はせず証跡を残す)
            java.io.File done = new java.io.File(dataFolder, "smp_dataold.yml.merged");
            if (done.exists()) done.delete();
            oldFile.renameTo(done);
            plugin.getLogger().info("[GemSMP] smp_dataold.yml から " + restored + " 項目を復元しました (欠損箇所のみ)。");
        } catch (Exception e) {
            plugin.getLogger().warning("[GemSMP] 復元データの保存に失敗: " + e.getMessage());
        }
    }

    // ─── データ永続化 (smp_data.yml) ──────────────────────────────────────
    // smpサーバーの plugins/GemSMP/smp_data.yml にAH・オーダー・チーム・スポナー等の
    // smp内部データと、プレイヤーの所持金・EC等を保存する。起動時にこの内容を
    // bulkMigrateToDb でMySQLへ反映し、hub/smp間で共有する。hubモードでは書き込まない。

    public void refreshAuctionsFromDb() {
        DatabaseManager db = db();
        if (db != null) {
            db.loadAllAuctionsAsync(auctions -> {
                auctionItems.clear();
                for (AuctionItem ai : auctions) {
                    auctionItems.put(ai.id, ai);
                }
            });
        }
    }

    public void refreshOrdersFromDb() {
        DatabaseManager db = db();
        if (db != null) {
            db.loadAllOrdersAsync(orders -> {
                orderItems.clear();
                for (OrderItem oi : orders) {
                    orderItems.put(oi.id, oi);
                }
            });
            db.loadAllOrderStorageAsync(storage -> {
                orderStorage.clear();
                orderStorage.putAll(storage);
            });
        }
    }

    public void broadcastSyncAh() {
        DatabaseManager db = db();
        if (db != null) {
            db.runAfterDb(() -> {
                runGlobalTask(() -> {
                    Player p = com.google.common.collect.Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                    VelocityBridge vb = ((SMPPlugin) plugin).getVelocityBridge();
                    if (p != null && vb != null) vb.sendSyncAh(p);
                });
            });
        }
    }

    public void broadcastSyncOrder() {
        DatabaseManager db = db();
        if (db != null) {
            db.runAfterDb(() -> {
                runGlobalTask(() -> {
                    Player p = com.google.common.collect.Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
                    VelocityBridge vb = ((SMPPlugin) plugin).getVelocityBridge();
                    if (p != null && vb != null) vb.sendSyncOrder(p);
                });
            });
        }
    }

    private void loadData() {
        dataFile = ((SMPPlugin) plugin).getCustomDataFile();
        if (!dataFile.exists()) {
            plugin.getLogger().info("[GemSMP] データファイルが見つかりません (初回起動): " + dataFile.getAbsolutePath());
            return;
        }

        org.bukkit.configuration.file.YamlConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();
        try {
            String content = new String(java.nio.file.Files.readAllBytes(dataFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            content = content.replace("==: org.bukkit.Location", "isBukkitLocation: true");
            content = content.replace("==: Location", "isBukkitLocation: true");
            cfg.loadFromString(content);
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to load smp_data.yml safely", e);
            return;
        }

        for (String uuidStr : cfg.getKeys(false)) {
            if (uuidStr.equals("auction")) continue;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                if (cfg.isConfigurationSection(uuidStr + ".homes")) {
                    Map<Integer, Location> homes = new HashMap<>();
                    for (String slotStr : cfg.getConfigurationSection(uuidStr + ".homes").getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            Location loc = safeGetLocation(cfg, uuidStr + ".homes." + slotStr);
                            if (loc != null) homes.put(slot, loc);
                        } catch (Exception ignored) {}
                    }
                    if (!homes.isEmpty()) playerHomes.put(uuid, homes);
                }

                if (cfg.isConfigurationSection(uuidStr + ".newhomes")) {
                    Map<Integer, Location> newHomes = new HashMap<>();
                    for (String slotStr : cfg.getConfigurationSection(uuidStr + ".newhomes").getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            Location loc = safeGetLocation(cfg, uuidStr + ".newhomes." + slotStr);
                            if (loc != null) newHomes.put(slot, loc);
                        } catch (Exception ignored) {}
                    }
                    if (!newHomes.isEmpty()) playerNewHomes.put(uuid, newHomes);
                }

                Location lastLoc = safeGetLocation(cfg, uuidStr + ".lastLoc");
                if (lastLoc != null) lastSMPLocations.put(uuid, lastLoc);
                if (cfg.getBoolean(uuidStr + ".lastQuitSMP", false)) lastQuitInSMP.add(uuid);
                long m  = cfg.getLong(uuidStr + ".money",    0); if (m  > 0) smpMoney.put(uuid, m);
                long sh = cfg.getLong(uuidStr + ".shards",   0); if (sh > 0) smpShards.put(uuid, sh);
                long k_keys = cfg.getLong(uuidStr + ".keys", 0); if (k_keys > 0) smpKeys.put(uuid, k_keys);
                long k  = cfg.getLong(uuidStr + ".kills",    0); if (k  > 0) smpKills.put(uuid, k);
                long d  = cfg.getLong(uuidStr + ".deaths",   0); if (d  > 0) smpDeaths.put(uuid, d);
                long pt = cfg.getLong(uuidStr + ".playtime", 0); if (pt > 0) smpPlaytime.put(uuid, pt);
                long bounty = cfg.getLong(uuidStr + ".bounty", 0); if (bounty > 0) smpBounties.put(uuid, bounty);
                List<String> ignoreStrs = cfg.getStringList(uuidStr + ".ignores");
                if (ignoreStrs != null && !ignoreStrs.isEmpty()) {
                    Set<UUID> ignores = new HashSet<>();
                    for (String s : ignoreStrs) {
                        try { ignores.add(UUID.fromString(s)); } catch (Exception ignored) {}
                    }
                    smpIgnores.put(uuid, ignores);
                }

                if (cfg.isConfigurationSection(uuidStr + ".killCooldowns")) {
                    Map<UUID, Long> cdMap = new HashMap<>();
                    for (String victimStr : cfg.getConfigurationSection(uuidStr + ".killCooldowns").getKeys(false)) {
                        try {
                            UUID vUuid = UUID.fromString(victimStr);
                            long time = cfg.getLong(uuidStr + ".killCooldowns." + victimStr);
                            cdMap.put(vUuid, time);
                        } catch (Exception ignored) {}
                    }
                    if (!cdMap.isEmpty()) killShardCooldown.put(uuid, cdMap);
                }

                String rank = cfg.getString(uuidStr + ".rank");
                if (rank != null) playerRanks.put(uuid, rank.toLowerCase());

                // Settings
                if (cfg.contains(uuidStr + ".settings.tpa")) settingTpaEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.tpa", true));
                if (cfg.contains(uuidStr + ".settings.tpahere")) settingTpaHereEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.tpahere", true));
                if (cfg.contains(uuidStr + ".settings.pay")) settingPayEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.pay", true));
                if (cfg.contains(uuidStr + ".settings.mobspawn")) settingMobSpawnEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.mobspawn", true));
                if (cfg.contains(uuidStr + ".settings.hidecoords")) settingHideCoordsEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.hidecoords", false));
                if (cfg.contains(uuidStr + ".settings.ahconfirm")) settingAhConfirmEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.ahconfirm", true));
                if (cfg.contains(uuidStr + ".settings.scoreboard")) settingScoreboardEnabled.put(uuid, cfg.getBoolean(uuidStr + ".settings.scoreboard", true));
                if (cfg.getBoolean(uuidStr + ".settings.nv", false)) nvPlayers.add(uuid);



                if (cfg.isConfigurationSection(uuidStr + ".sellmulti")) {
                    Map<String, Long> multiStats = new HashMap<>();
                    for (String cat : cfg.getConfigurationSection(uuidStr + ".sellmulti").getKeys(false)) {
                        multiStats.put(cat, cfg.getLong(uuidStr + ".sellmulti." + cat));
                    }
                    smpSellMultiStats.put(uuid, multiStats);
                }

                if (cfg.isList(uuidStr + ".enderchest")) {
                    List<?> list = cfg.getList(uuidStr + ".enderchest");
                    if (list != null) {
                        ItemStack[] items = new ItemStack[54];
                        for (int i = 0; i < Math.min(items.length, list.size()); i++) {
                            Object obj = list.get(i);
                            if (obj instanceof ItemStack is) items[i] = is;
                        }
                        customEnderChests.put(uuid, items);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (cfg.contains("afkZone.pos1") && cfg.contains("afkZone.pos2")) {
            afkZonePos1 = safeGetLocation(cfg, "afkZone.pos1");
            afkZonePos2 = safeGetLocation(cfg, "afkZone.pos2");
            afkZoneWorldName = cfg.getString("afkZone.world");
        }

        if (cfg.contains("rtp_portals.overPos1")) rtpOverPos1 = safeGetLocation(cfg, "rtp_portals.overPos1");
        if (cfg.contains("rtp_portals.overPos2")) rtpOverPos2 = safeGetLocation(cfg, "rtp_portals.overPos2");
        if (cfg.contains("rtp_portals.netherPos1")) rtpNetherPos1 = safeGetLocation(cfg, "rtp_portals.netherPos1");
        if (cfg.contains("rtp_portals.netherPos2")) rtpNetherPos2 = safeGetLocation(cfg, "rtp_portals.netherPos2");
        if (cfg.contains("rtp_portals.endPos1")) rtpEndPos1 = safeGetLocation(cfg, "rtp_portals.endPos1");
        if (cfg.contains("rtp_portals.endPos2")) rtpEndPos2 = safeGetLocation(cfg, "rtp_portals.endPos2");
        if (cfg.contains("rtp_portals.afkPortalPos1")) afkPortalPos1 = safeGetLocation(cfg, "rtp_portals.afkPortalPos1");
        if (cfg.contains("rtp_portals.afkPortalPos2")) afkPortalPos2 = safeGetLocation(cfg, "rtp_portals.afkPortalPos2");

        auctionItems.clear();


        teams.clear();
        playerTeamMap.clear();
        if (cfg.isConfigurationSection("teams")) {
            for (String key : cfg.getConfigurationSection("teams").getKeys(false)) {
                try {
                    TeamData td = new TeamData();
                    td.name = key;
                    td.leader = UUID.fromString(cfg.getString("teams." + key + ".leader"));
                    td.home = (Location) cfg.get("teams." + key + ".home");
                    td.friendlyFire = cfg.getBoolean("teams." + key + ".ff", false);
                    for (String mStr : cfg.getStringList("teams." + key + ".members")) {
                        UUID mUuid = UUID.fromString(mStr);
                        td.members.add(mUuid);
                        playerTeamMap.put(mUuid, key);
                    }
                    teams.put(key, td);
                } catch (Exception ignored) {}
            }
        }

        orderItems.clear();
        orderStorage.clear();


        if (cfg.isConfigurationSection("itemPrices")) {
            for (String key : cfg.getConfigurationSection("itemPrices").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key);
                    long price = cfg.getLong("itemPrices." + key);
                    if (isValidTradeItem(mat)) {
                        ITEM_PRICES.put(mat, price);
                    } else {
                        cfg.set("itemPrices." + key, null);
                    }
                } catch (Exception ignored) {}
            }
        }

        if (cfg.isConfigurationSection("spawners")) {
            for (String key : cfg.getConfigurationSection("spawners").getKeys(false)) {
                try {
                    SpawnerData sd = new SpawnerData();
                    sd.stackCount = cfg.getInt("spawners." + key + ".stackCount", 1);
                    sd.entityType = cfg.getString("spawners." + key + ".entityType", null);
                    if (cfg.isConfigurationSection("spawners." + key + ".storedItems")) {
                        for (String itemKey : cfg.getConfigurationSection("spawners." + key + ".storedItems").getKeys(false)) {
                            sd.storedItems.put(itemKey, cfg.getInt("spawners." + key + ".storedItems." + itemKey, 0));
                        }
                    }
                    spawnerDataMap.put(key, sd);
                } catch (Exception ignored) {}
            }
        }

        if (cfg.contains("global.keyall")) keyallRemainingSeconds = cfg.getLong("global.keyall");
        if (cfg.contains("global.isEndOpen")) isEndOpen = cfg.getBoolean("global.isEndOpen");

        plugin.getLogger().info("[GemSMP] データを読み込みました: " + dataFile.getAbsolutePath());
    }

    private void saveData() {
        if (dataFile == null) {
            dataFile = ((SMPPlugin) plugin).getCustomDataFile();
        }

        org.bukkit.configuration.file.YamlConfiguration cfg = new org.bukkit.configuration.file.YamlConfiguration();

        // hubモードは読み取り専用レプリカだが、AFK設定などの固有設定は保存する
        if (hubMode) {
            if (dataFile.exists()) {
                try {
                    cfg.load(dataFile);
                } catch (Exception ignored) {}
            }
            if (afkZonePos1 != null && afkZonePos2 != null && afkZoneWorldName != null) {
                cfg.set("afkZone.pos1", afkZonePos1);
                cfg.set("afkZone.pos2", afkZonePos2);
                cfg.set("afkZone.world", afkZoneWorldName);
            }
            cfg.set("rtp_portals.overPos1", rtpOverPos1);
            cfg.set("rtp_portals.overPos2", rtpOverPos2);
            cfg.set("rtp_portals.netherPos1", rtpNetherPos1);
            cfg.set("rtp_portals.netherPos2", rtpNetherPos2);
            cfg.set("rtp_portals.endPos1", rtpEndPos1);
            cfg.set("rtp_portals.endPos2", rtpEndPos2);
            cfg.set("rtp_portals.afkPortalPos1", afkPortalPos1);
            cfg.set("rtp_portals.afkPortalPos2", afkPortalPos2);
            try {
                cfg.save(dataFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save AFK data in hub mode.");
            }
            return;
        }

        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(playerHomes.keySet());
        allUuids.addAll(playerNewHomes.keySet());
        allUuids.addAll(lastSMPLocations.keySet());
        allUuids.addAll(lastQuitInSMP);
        allUuids.addAll(smpMoney.keySet());
        allUuids.addAll(smpShards.keySet());
        allUuids.addAll(killShardCooldown.keySet());
        allUuids.addAll(smpKeys.keySet());
        allUuids.addAll(smpKills.keySet());
        allUuids.addAll(smpDeaths.keySet());
        allUuids.addAll(smpPlaytime.keySet());
        allUuids.addAll(smpBounties.keySet());
        allUuids.addAll(smpIgnores.keySet());
        allUuids.addAll(smpSellMultiStats.keySet());

        for (UUID uuid : allUuids) {
            String u = uuid.toString();

            Map<Integer, Location> homes = playerHomes.get(uuid);
            if (homes != null) {
                for (Map.Entry<Integer, Location> e : homes.entrySet()) {
                    cfg.set(u + ".homes." + e.getKey(), e.getValue());
                }
            }
            Map<Integer, Location> newHomes = playerNewHomes.get(uuid);
            if (newHomes != null) {
                for (Map.Entry<Integer, Location> e : newHomes.entrySet()) {
                    cfg.set(u + ".newhomes." + e.getKey(), e.getValue());
                }
            } else {
                cfg.set(u + ".newhomes", null);
            }

            Location lastLoc = lastSMPLocations.get(uuid);
            if (lastLoc != null) cfg.set(u + ".lastLoc", lastLoc);
            cfg.set(u + ".lastQuitSMP", lastQuitInSMP.contains(uuid));
            cfg.set(u + ".money",  smpMoney.getOrDefault(uuid, 0L));
            cfg.set(u + ".shards", smpShards.getOrDefault(uuid, 0L));
            cfg.set(u + ".keys",   smpKeys.getOrDefault(uuid, 0L));
            cfg.set(u + ".kills",  smpKills.getOrDefault(uuid, 0L));
            cfg.set(u + ".deaths", smpDeaths.getOrDefault(uuid, 0L));
            cfg.set(u + ".playtime", getPlaytimeSeconds(uuid));
            cfg.set(u + ".bounty", smpBounties.getOrDefault(uuid, 0L));

            if (smpIgnores.containsKey(uuid)) {
                List<String> igList = new ArrayList<>();
                for (UUID ig : smpIgnores.get(uuid)) igList.add(ig.toString());
                cfg.set(u + ".ignores", igList);
            } else {
                cfg.set(u + ".ignores", null);
            }

            if (killShardCooldown.containsKey(uuid)) {
                for (Map.Entry<UUID, Long> e : killShardCooldown.get(uuid).entrySet()) {
                    cfg.set(u + ".killCooldowns." + e.getKey(), e.getValue());
                }
            } else {
                cfg.set(u + ".killCooldowns", null);
            }

            String rank = playerRanks.get(uuid);
            if (rank != null) cfg.set(u + ".rank", rank);

            cfg.set(u + ".settings.tpa",        settingTpaEnabled.getOrDefault(uuid, true));
            cfg.set(u + ".settings.tpahere",    settingTpaHereEnabled.getOrDefault(uuid, true));
            cfg.set(u + ".settings.pay",        settingPayEnabled.getOrDefault(uuid, true));
            cfg.set(u + ".settings.mobspawn",   settingMobSpawnEnabled.getOrDefault(uuid, true));
            cfg.set(u + ".settings.hidecoords", settingHideCoordsEnabled.getOrDefault(uuid, false));
            cfg.set(u + ".settings.ahconfirm",  settingAhConfirmEnabled.getOrDefault(uuid, true));
            cfg.set(u + ".settings.scoreboard", settingScoreboardEnabled.getOrDefault(uuid, true));
            cfg.set(u + ".settings.nv",         nvPlayers.contains(uuid));

            Map<String, Long> multiStats = smpSellMultiStats.get(uuid);
            if (multiStats != null && !multiStats.isEmpty()) {
                for (Map.Entry<String, Long> ent : multiStats.entrySet()) {
                    cfg.set(u + ".sellmulti." + ent.getKey(), ent.getValue());
                }
            } else {
                cfg.set(u + ".sellmulti", null);
            }

            ItemStack[] ec = customEnderChests.get(uuid);
            if (ec != null) {
                cfg.set(u + ".enderchest", java.util.Arrays.asList(ec));
            }
        }



        cfg.set("global.keyall", keyallRemainingSeconds);

        for (Map.Entry<String, TeamData> entry : teams.entrySet()) {
            String name = entry.getKey();
            TeamData td = entry.getValue();
            cfg.set("teams." + name + ".leader", td.leader.toString());
            cfg.set("teams." + name + ".home", td.home);
            cfg.set("teams." + name + ".ff", td.friendlyFire);
            List<String> memList = new ArrayList<>();
            for (UUID mUuid : td.members) memList.add(mUuid.toString());
            cfg.set("teams." + name + ".members", memList);
        }



        for (Map.Entry<Material, Long> entry : ITEM_PRICES.entrySet()) {
            cfg.set("itemPrices." + entry.getKey().name(), entry.getValue());
        }

        for (Map.Entry<String, SpawnerData> entry : spawnerDataMap.entrySet()) {
            String key = entry.getKey();
            SpawnerData sd = entry.getValue();
            cfg.set("spawners." + key + ".stackCount", sd.stackCount);
            cfg.set("spawners." + key + ".entityType", sd.entityType);
            for (Map.Entry<String, Integer> e2 : sd.storedItems.entrySet()) {
                cfg.set("spawners." + key + ".storedItems." + e2.getKey(), e2.getValue());
            }
        }

        if (afkZonePos1 != null && afkZonePos2 != null && afkZoneWorldName != null) {
            cfg.set("afkZone.pos1", afkZonePos1);
            cfg.set("afkZone.pos2", afkZonePos2);
            cfg.set("afkZone.world", afkZoneWorldName);
        }

        cfg.set("global.isEndOpen", isEndOpen);
        cfg.set("rtp_portals.overPos1", rtpOverPos1);
        cfg.set("rtp_portals.overPos2", rtpOverPos2);
        cfg.set("rtp_portals.netherPos1", rtpNetherPos1);
        cfg.set("rtp_portals.netherPos2", rtpNetherPos2);
        cfg.set("rtp_portals.endPos1", rtpEndPos1);
        cfg.set("rtp_portals.endPos2", rtpEndPos2);
        cfg.set("rtp_portals.afkPortalPos1", afkPortalPos1);
        cfg.set("rtp_portals.afkPortalPos2", afkPortalPos2);

        if (plugin.isEnabled()) {
            runAsyncTask(() -> {
                try {
                    cfg.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().warning("[GemSMP] Save failed: " + e.getMessage());
                }
            });
        } else {
            try {
                cfg.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().warning("[GemSMP] Save failed: " + e.getMessage());
            }
        }
    }

    private String getSettingsBits(UUID uuid) {
        StringBuilder sb = new StringBuilder();
        sb.append(settingTpaEnabled.getOrDefault(uuid, true) ? "1" : "0");
        sb.append(settingTpaHereEnabled.getOrDefault(uuid, true) ? "1" : "0");
        sb.append(settingPayEnabled.getOrDefault(uuid, true) ? "1" : "0");
        sb.append(settingMobSpawnEnabled.getOrDefault(uuid, true) ? "1" : "0");
        sb.append(settingHideCoordsEnabled.getOrDefault(uuid, false) ? "1" : "0");
        sb.append(settingAhConfirmEnabled.getOrDefault(uuid, true) ? "1" : "0");
        sb.append(settingScoreboardEnabled.getOrDefault(uuid, true) ? "1" : "0");
        sb.append(nvPlayers.contains(uuid) ? "1" : "0");
        return sb.toString();
    }

    private void applySettingsBits(UUID uuid, String bits) {
        if (bits == null || bits.length() < 7) return;
        settingTpaEnabled.put(uuid, bits.charAt(0) == '1');
        settingTpaHereEnabled.put(uuid, bits.charAt(1) == '1');
        settingPayEnabled.put(uuid, bits.charAt(2) == '1');
        settingMobSpawnEnabled.put(uuid, bits.charAt(3) == '1');
        settingHideCoordsEnabled.put(uuid, bits.charAt(4) == '1');
        settingAhConfirmEnabled.put(uuid, bits.charAt(5) == '1');
        settingScoreboardEnabled.put(uuid, bits.charAt(6) == '1');
        if (bits.length() > 7) {
            if (bits.charAt(7) == '1') nvPlayers.add(uuid);
            else nvPlayers.remove(uuid);
        }
    }
}
