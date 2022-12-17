package dev.wonkypigs.cosmicvaults;

import dev.wonkypigs.cosmicvaults.Commands.VaultsCommand;
import dev.wonkypigs.cosmicvaults.Handlers.VaultHandler;
import dev.wonkypigs.cosmicvaults.Listener.UpdateChecker;
import dev.wonkypigs.cosmicvaults.Listener.VaultMenuListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
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
        registerPermissions();
        mySqlSetup();

        int pluginId = 17072; // <-- Replace with the id of your plugin!
        Metrics metrics = new Metrics(this, pluginId);

        UpdateChecker updateChecker = new UpdateChecker();
        updateChecker.check();

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
        getServer().getPluginManager().registerEvents(new UpdateChecker(), this);
    }

    public void registerPermissions() {
        // Registering all plugin permissions
        getServer().getPluginManager().addPermission(new Permission("cosmicvaults.vaults"));
        getServer().getPluginManager().addPermission(new Permission("cosmicvaults.vaults.unlimited"));
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

                if (getConfig().getString("database.type").equalsIgnoreCase("sqlite")) {
                    // create local database file and stuff
                    Class.forName("org.sqlite.JDBC");
                    File file = new File(getDataFolder(), "database.db");
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    setConnection(DriverManager.getConnection("jdbc:sqlite:" + file));
                } else {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    // create database if not exists
                    setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "?autoReconnect=true&useSSL=false", username, password));
                    getConnection().createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
                    setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password));
                }
                getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS player_vaults (UUID varchar(50), VAULT_ID int, CONTENTS TEXT)").executeUpdate();
                getLogger().info("Successfully connected to the MySQL database");
            }
        } catch (SQLException | ClassNotFoundException | IOException e) {
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
}
