package dev.wonkypigs.cosmicvaults;

import dev.wonkypigs.cosmicvaults.Commands.VaultsCommand;
import dev.wonkypigs.cosmicvaults.Handlers.VaultHandler;
import dev.wonkypigs.cosmicvaults.Listener.VaultMenuListener;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class CosmicVaults extends JavaPlugin {
    private static CosmicVaults instance;{ instance = this; }
    private Connection connection;
    public String host, database, username, password;
    public int port;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        registerCommands();
        registerListeners();
        mySqlSetup();
        getLogger().info("CosmicVaults has been enabled successfully!");
    }

    @Override
    public void onDisable() { getLogger().info("CosmicVaults has shut down"); }

    public void registerCommands() {
        // Registering all plugin commands
        getCommand("vaults").setExecutor(new VaultsCommand());
    }

    public void registerListeners() {
        // Registering all plugin listeners
        getServer().getPluginManager().registerEvents(new VaultHandler(), this);
        getServer().getPluginManager().registerEvents(new VaultMenuListener(), this);
    }

    public void mySqlSetup() {
        try {
            File file = new File(getDataFolder(), "config.yml");
            host = YamlConfiguration.loadConfiguration(file).getString("database.host");
            port = YamlConfiguration.loadConfiguration(file).getInt("database.port");
            database = YamlConfiguration.loadConfiguration(file).getString("database.database");
            username = YamlConfiguration.loadConfiguration(file).getString("database.username");
            password = YamlConfiguration.loadConfiguration(file).getString("database.password");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            synchronized (this) {
                if (getConnection() != null && !getConnection().isClosed()) {
                    return;
                }

                Class.forName("com.mysql.cj.jdbc.Driver");
                setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password));

                getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS cosmicVaults_vaults (UUID varchar(50), VAULT_ID int, CONTENTS BLOB)").executeUpdate();
                getLogger().info("Successfully connected to the MySQL database");
            }
        } catch (SQLException | ClassNotFoundException e) {
            getLogger().info("Error connecting to the MySQL database");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public static CosmicVaults getInstance() {
        return instance;
    }


    // Getting values from config with color coding
    public String getConfigValue(String key) {
        String ans = getConfig().getString(key);
        return ChatColor.translateAlternateColorCodes('&', ans);
    }

    // register permissions
    public void registerPermissions() {
        getServer().getPluginManager().addPermission(new Permission("cosmicvaults.vaults"));
        getServer().getPluginManager().addPermission(new Permission("cosmicvaults.vaults.unlimited"));
    }
}