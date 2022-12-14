package dev.wonkypigs.cosmicvaults.Handlers;

import dev.wonkypigs.cosmicvaults.Commands.VaultsCommand;
import dev.wonkypigs.cosmicvaults.CosmicVaults;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.wonkypigs.cosmicvaults.Helper.VaultSaveHelper.deserializeItemsArray;
import static dev.wonkypigs.cosmicvaults.Helper.VaultSaveHelper.serializeItemsArray;

public class VaultHandler implements Listener {

    private static final CosmicVaults plugin = CosmicVaults.getInstance();

    public static void openVault(Player player, int id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("SELECT CONTENTS FROM player_vaults WHERE UUID=? AND VAULT_ID=?");
                statement.setString(1, player.getUniqueId().toString());
                statement.setInt(2, id);
                ResultSet result = statement.executeQuery();

                // inventory
                int slots = (plugin.getConfig().getInt("vault-storage-rows")+1)*9;
                Inventory inv = plugin.getServer().createInventory(null, slots, "&5&lVault ".replace("&", "§") + id);

                // filling up
                if (result.next()) {
                    inv.setContents(deserializeItemsArray(result.getBlob("CONTENTS").getBinaryStream()));
                }
                result.close();

                for (int i = slots-9; i < slots; i++) {
                    ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(" ");
                    item.setItemMeta(meta);
                    inv.setItem(i, item);
                }
                ItemStack back = new ItemStack(Material.BARRIER);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.setDisplayName("&c&lBack to menu".replace("&", "§"));
                back.setItemMeta(backMeta);
                // last row middle item
                inv.setItem(slots-5, back);

                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(inv));

            } catch (SQLException | IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void saveVault(Player player, Inventory inv, int id) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // delete old items
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("DELETE FROM player_vaults WHERE UUID=? AND VAULT_ID=?");
                statement.setString(1, player.getUniqueId().toString());
                statement.setInt(2, id);
                statement.executeUpdate();

                // save new items
                statement = plugin.getConnection()
                        .prepareStatement("INSERT INTO player_vaults (UUID, VAULT_ID, CONTENTS) VALUES (?, ?, ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setInt(2, id);
                statement.setBlob(3, serializeItemsArray(inv.getContents()));
                statement.executeUpdate();

            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void createNewVault(Player player, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // can player make more vaults
                PreparedStatement statement = plugin.getConnection()
                        .prepareStatement("SELECT VAULT_ID FROM player_vaults WHERE UUID=? ORDER BY VAULT_ID DESC LIMIT 1");
                statement.setString(1, player.getUniqueId().toString());
                ResultSet result = statement.executeQuery();
                // if no vaults
                int vaults = 0;
                if (result.next()) {
                    vaults = result.getInt("VAULT_ID");
                } else {
                    vaults = 0;
                }
                result.close();

                // get player's effective perms
                AtomicInteger maxVaults = new AtomicInteger();
                maxVaults.set(Integer.parseInt(plugin.getConfig().getString("default-vaults")));
                player.getEffectivePermissions().forEach((perm) -> {
                    if(!player.hasPermission("cosmicvaults.vaults.unlimited")) {
                        if (perm.getPermission().startsWith("cosmicvaults.vaults.")) {
                            int vaultsPerm = Integer.parseInt(perm.getPermission().replace("cosmicvaults.vaults.", ""));
                            if (vaultsPerm > maxVaults.get()) {
                                maxVaults.set(vaultsPerm);
                            }
                        }
                    } else {
                        maxVaults.set(99999);
                    }
                });

                // max vaults and no cosmicvaults.vaults.unlimited perm?
                if ((vaults >= maxVaults.get()) && (!player.hasPermission("cosmicvaults.vaults.unlimited"))) {
                    player.sendMessage(plugin.getConfigValue("max-vaults-reached")
                            .replace("{vaults}", String.valueOf(maxVaults.get()))
                            .replace("{prefix}", plugin.getConfigValue("prefix"))
                            .replace("&", "§"));
                    return;
                }

                // create new vault for player
                statement = plugin.getConnection()
                        .prepareStatement("INSERT INTO player_vaults (UUID, VAULT_ID, CONTENTS) VALUES (?, ?, ?)");
                statement.setString(1, player.getUniqueId().toString());
                statement.setInt(2, vaults + 1);
                Inventory vault = plugin.getServer().createInventory(null, 27, "");
                statement.setBlob(3, serializeItemsArray(vault.getContents()));
                statement.executeUpdate();

                // update vaultsmenu
                VaultsCommand.vaultMenuFiller(page, player);
                player.sendMessage("§aNew vault created! (ID: " + (vaults + 1) + ")");
            } catch (SQLException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
