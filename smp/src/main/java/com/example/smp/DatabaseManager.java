
    /** Save an order to the database */
    public void saveOrder(String orderId, UUID playerUuid, String itemName, int quantity, long price) {
        runAfterDb(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO orders (order_id, player_uuid, item_name, quantity, price, order_time)
                          VALUES (?, ?, ?, ?, ?, ?)"
                 )) {
                stmt.setString(1, orderId);
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, itemName);
                stmt.setInt(4, quantity);
                stmt.setLong(5, price);
                stmt.setLong(6, System.currentTimeMillis() / 1000);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save order: " + e.getMessage());
            }
        });
    }

    /** Retrieve all orders for a player by UUID */
    public List<Order> getOrdersByPlayerUUID(UUID playerUuid) {
        List<Order> orders = new ArrayList<>();
        runAfterDb(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT order_id, item_name, quantity, price, order_time FROM orders WHERE player_uuid = ?"
                 )) {
                stmt.setString(1, playerUuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Order order = new Order();
                    order.setOrderId(rs.getString("order_id"));
                    order.setItemName(rs.getString("item_name"));
                    order.setQuantity(rs.getInt("quantity"));
                    order.setPrice(rs.getLong("price"));
                    order.setOrderTime(rs.getLong("order_time"));
                    orders.add(order);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to retrieve orders: " + e.getMessage());
            }
        });
        return orders;
    }
