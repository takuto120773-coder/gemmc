package com.example.smp.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.example.smp.DatabaseManager;
import java.util.UUID;

public class SellCommand implements CommandExecutor {
    private final DatabaseManager dbManager;

    public SellCommand(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        String[] items = {"item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9"};
        for (String item : items) {
            dbManager.saveOrder(UUID.randomUUID().toString(), player.getUniqueId(), item, 1, 10);
        }
        sender.sendMessage("Items sold successfully!");

        return true;
    }
}