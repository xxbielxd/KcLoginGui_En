package com.puddingkc;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.cjcrafter.foliascheduler.util.ServerVersions;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class KcLoginGui extends JavaPlugin implements Listener {

    private long delayTime;
    private boolean debugMode;
    private boolean isFolia;
    private boolean closeKick;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        getLogger().info("KcLoginGui is enabling, current AuthMe version");
        saveDefaultConfig();

        if (isFloodgateEnabled("floodgate")) {
            getLogger().warning("The required plugin Floodgate is missing, plugin failed to enable");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (isFloodgateEnabled("AuthMe")) {
            getLogger().warning("The required plugin AuthMe is missing, plugin failed to enable");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (ServerVersions.isFolia()) {
            isFolia = ServerVersions.isFolia();
            getLogger().info("Currently running in a Folia environment, compatibility enabled.");
        }

        loadConfig();
        getServer().getPluginManager().registerEvents(this,this);
    }

    private void loadConfig() {
        reloadConfig();
        config = getConfig();
        delayTime = config.getLong("delay-time", 45L);
        debugMode = config.getBoolean("debug", false);
        closeKick = config.getBoolean("close-kick", true);
    }

    private boolean isFloodgateEnabled(String plugin) {
        return !Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FloodgateApi floodgateApi = FloodgateApi.getInstance();

        if (!floodgateApi.isFloodgatePlayer(player.getUniqueId())) return;

        sendDebugLog(player.getName() + " Bedrock player detected");

        FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
        handleAuthentication(player, floodgatePlayer);
    }

    private void handleAuthentication(Player player, FloodgatePlayer floodgatePlayer) {
        AuthMeApi authMeApi = AuthMeApi.getInstance();

        if (authMeApi.isRegistered(player.getName())) {
            sendFormWithDelay(player, floodgatePlayer, getLoginForm(player));
        } else if (!authMeApi.isRegistered(player.getName())){
            sendFormWithDelay(player, floodgatePlayer, getRegisterForm(player));
        }
    }

    private void sendFormWithDelay(Player player, FloodgatePlayer floodgatePlayer, CustomForm.Builder formBuilder) {
        if (isFolia) {
            ServerImplementation scheduler = new FoliaCompatibility(this).getServerImplementation();
            scheduler.async().runDelayed(task -> {
                if (!AuthMeApi.getInstance().isAuthenticated(player)) {
                    floodgatePlayer.sendForm(formBuilder.build());
                    sendDebugLog(player.getName() + " Window " + formBuilder + " has been sent");
                }
            }, delayTime);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!AuthMeApi.getInstance().isAuthenticated(player)) {
                        floodgatePlayer.sendForm(formBuilder.build());
                        sendDebugLog(player.getName() + " Window " + formBuilder + " has been sent");
                    }
                }
            }.runTaskLater(this, delayTime);
        }
    }

    private CustomForm.Builder getLoginForm(Player player) {
        return CustomForm.builder()
                .title(getMessage("login-title"))
                .input(getMessage("login-password-title"), getMessage("login-password-placeholder"))
                .validResultHandler(response -> handleLoginResponse(player, response.asInput()))
                .closedResultHandler(response -> {
                    if (closeKick) {
                        player.kickPlayer(getMessage("close-window"));
                    }
                });
    }

    private void handleLoginResponse(Player player, String password) {
        AuthMeApi authMeApi = AuthMeApi.getInstance();

        if (authMeApi.checkPassword(player.getName(), password)) {
            authMeApi.forceLogin(player);
            sendDebugLog(player.getName() + " Login successful");
        } else {
            player.sendMessage(getMessage("wrong-password"));
            sendFormWithDelay(player, FloodgateApi.getInstance().getPlayer(player.getUniqueId()), getLoginForm(player));
        }
    }


    private CustomForm.Builder getRegisterForm(Player player) {
        return CustomForm.builder()
                .title(getMessage("reg-title"))
                .input(getMessage("reg-password-title"), getMessage("reg-password-placeholder"))
                .input(getMessage("reg-confirmPassword-title"), getMessage("reg-confirmPassword-placeholder"))
                .validResultHandler(response -> handleRegisterResponse(player, response.asInput(0), response.asInput(1)))
                .closedResultHandler(response -> {
                    if (closeKick) {
                        player.kickPlayer(getMessage("close-window"));
                    }
                });
    }

    private void handleRegisterResponse(Player player, String password, String confirmPassword) {
        AuthMeApi authMeApi = AuthMeApi.getInstance();

        if (password == null || confirmPassword == null || password.isEmpty() || confirmPassword.isEmpty()) {
            player.sendMessage(getMessage("password-empty"));
            sendFormWithDelay(player, FloodgateApi.getInstance().getPlayer(player.getUniqueId()), getRegisterForm(player));
            return;
        }

        if (!password.equals(confirmPassword)) {
            player.sendMessage(getMessage("passwords-not-match"));
            sendFormWithDelay(player, FloodgateApi.getInstance().getPlayer(player.getUniqueId()), getRegisterForm(player));
            return;
        }

        int minLength = getConfig().getInt("authme-settings.min-password-length", 4);
        int maxLength = getConfig().getInt("authme-settings.max-password-length", 16);

        if (password.length() < minLength || password.length() > maxLength) {
            player.sendMessage(
                getMessage("password-too-short")
                    .replace("%min%", String.valueOf(minLength))
                    .replace("%max%", String.valueOf(maxLength))
            );
            sendFormWithDelay(player, FloodgateApi.getInstance().getPlayer(player.getUniqueId()), getRegisterForm(player));
            return;
        }

        if (!password.matches("[!-~]*")) {
            player.sendMessage(getMessage("password-invalid-chars"));
            sendFormWithDelay(player, FloodgateApi.getInstance().getPlayer(player.getUniqueId()), getRegisterForm(player));
            return;
        }

        authMeApi.forceRegister(player, password);
        sendDebugLog(player.getName() + " Registration successful");
    }


    private void sendDebugLog(String message) {
        if (debugMode) {
            getLogger().info(message);
        }
    }

    private String getMessage(String key) {
        return config.getString("messages." + key, "&cText missing, please check the configuration file.").replace("&", "§");
    }
}
