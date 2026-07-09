
    private final DatabaseManager dbManager;

    public SMPManager(JavaPlugin plugin) {
        this.plugin = plugin;
        dbManager = new DatabaseManager(plugin);
    }

    /** Save an order for a player */
    public void saveOrder(UUID playerUuid, String itemName, int quantity, long price) {
        String orderId = UUID.randomUUID().toString();
        dbManager.saveOrder(orderId, playerUuid, itemName, quantity, price);
    }

    /** Retrieve all orders for a player by UUID */
    public List<Order> getOrdersByPlayerUUID(UUID playerUuid) {
        return dbManager.getOrdersByPlayerUUID(playerUuid);
    }