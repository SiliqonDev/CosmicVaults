package dev.wonkypigs.cosmicvaults.Listener;

import dev.wonkypigs.cosmicvaults.CosmicVaults;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class UpdateChecker implements Listener {

    private static final CosmicVaults plugin = CosmicVaults.getInstance();

    private String url = "https://api.spigotmc.org/legacy/update.php?resource=";
    private String id = "106729";

    private boolean isAvailable;
    private String remoteVersion;

    public UpdateChecker() {

    }

    public boolean isAvailable() {
        return isAvailable;
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        if(plugin.getConfig().getBoolean("update-checker")) {
            if (event.getPlayer().isOp()) {
                if (isAvailable()) {
                    event.getPlayer().sendMessage("&cThere is a new update available for CosmicVaults!\n&cCurrent version: " + plugin.getDescription().getVersion() + "\n&cNew version: " + remoteVersion);
                }
            }
        }
    }

    public void check() {
        isAvailable = checkUpdate();
    }

    private boolean checkUpdate() {
        plugin.getLogger().info("Checking for updates...");
        try {
            String localVersion = CosmicVaults.getInstance().getDescription().getVersion();
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url + id).openConnection();
            connection.setRequestMethod("GET");
            String raw = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();

            if(raw.contains("-")) {
                remoteVersion = raw.split("-")[0].trim();
            } else {
                remoteVersion = raw;
            }

            if(!localVersion.equalsIgnoreCase(remoteVersion))
                return true;

        } catch (IOException e) {
            return false;
        }
        return false;
    }

}