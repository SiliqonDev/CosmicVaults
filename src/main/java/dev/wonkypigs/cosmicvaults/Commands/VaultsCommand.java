package dev.wonkypigs.cosmicvaults.Commands;

import dev.wonkypigs.cosmicvaults.CosmicVaults;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VaultsCommand implements CommandExecutor {

    private static final CosmicVaults plugin = CosmicVaults.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("cosmicvaults.vaults")) {
                // Open main vaults menu
                vaultMenuFiller(1, player);
            } else {
                player.sendMessage(plugin.getConfigValue("no-permission"));
            }
        } else {
            sender.sendMessage(plugin.getConfigValue("must-be-player"));
        }
        return true;
    }

    public static void vaultMenuFiller(int page, Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 45, "&d&lY&5&lo&d&lu&5&lr &d&lV&5&la&d&lu&5&ll&d&lt&5&ls&r | Page ".replace("&", "§") + page);
        final int currpage = page - 1;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // get vaults from database
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("SELECT VAULT_ID FROM cosmicVaults_vaults WHERE UUID=? ORDER BY VAULT_ID ASC");
                statement.setString(1, player.getUniqueId().toString());
                ResultSet results = statement.executeQuery();
                int total_items = 0;
                while (results.next()) {
                    total_items++;
                }

                // bottom and top row with color
                for (int i = 0; i < 9; i++) {
                    ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("");
                    item.setItemMeta(meta);
                    inv.setItem(i, item);
                }
                for (int i = 36; i < 45; i++) {
                    ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(" ");
                    item.setItemMeta(meta);
                    inv.setItem(i, item);
                }

                // last row center item to diamond
                ItemStack newVault = new ItemStack(Material.DIAMOND);
                ItemMeta newVaultMeta = newVault.getItemMeta();
                newVaultMeta.setDisplayName("&b&lCreate New Vault".replace("&", "§"));
                newVault.setItemMeta(newVaultMeta);
                inv.setItem(40, newVault);

                // no vaults?
                if (total_items == 0) {
                    ItemStack item = new ItemStack(Material.BARRIER);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("&cYou have no vaults!".replace("&", "§"));
                    item.setItemMeta(meta);
                    inv.setItem(22, item);
                    // get out of scheduler
                    Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
                    return;
                }

                // setting stuff up
                int first_item = currpage * 27;
                int last_item = first_item + 26;
                if (last_item > total_items) {
                    last_item = total_items;
                }
                if (last_item < total_items) {
                    ItemStack item = new ItemStack(Material.GREEN_WOOL);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("&aNext Page".replace("&", "§"));
                    item.setItemMeta(meta);
                    inv.setItem(43, item);
                }
                if (first_item > 0) {
                    ItemStack item = new ItemStack(Material.RED_WOOL);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("&cPrevious Page".replace("&", "§"));
                    item.setItemMeta(meta);
                    inv.setItem(37, item);
                }

                // file page with items
                int curr_item = -1;
                results = statement.executeQuery();
                while (results.next()) {
                    curr_item++;
                    if ((curr_item < first_item) || (curr_item > last_item)) {
                        continue;
                    }
                    int vaultId = results.getInt("VAULT_ID");
                    ItemStack vaultItem = new ItemStack(Material.ENDER_CHEST, 1);
                    ItemMeta vaultItemMeta = vaultItem.getItemMeta();
                    vaultItemMeta.setDisplayName("&6&lVault ".replace("&", "§") + vaultId);
                    vaultItemMeta.setLore(null);
                    vaultItem.setItemMeta(vaultItemMeta);
                    inv.addItem(vaultItem);
                }
                // out of scheduler
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
