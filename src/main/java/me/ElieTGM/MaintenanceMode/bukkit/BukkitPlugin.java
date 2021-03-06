
package me.ElieTGM.MaintenanceMode.bukkit;

import me.ElieTGM.MaintenanceMode.bukkit.command.CommandMaintenance;
import me.ElieTGM.MaintenanceMode.bukkit.event.PingProtocolEvent;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static me.ElieTGM.MaintenanceMode.Messages.colour;


public class BukkitPlugin extends JavaPlugin implements Listener {

    /**
     * Store the list of times to alert on
     */
    private List<Integer> alertTimes;

    /**
     * Store the countdown message format
     */
    private String countdownMessage;

    /**
     * Store the list of whitelisted players
     */
    private List<String> whitelist;
    
    private String protocolMessage;

    /**
     * Store the message players are shown when kicked
     */
    private String message_kick;

    /**
     * Store the message_motd that will be displayed when maintenance mode is active
     */
    private String message_motd;

    /**
     * Store whether maintenance mode is enabled or not
     */
    private boolean enabled;

    /**
     * Store the ID of the BukkitTask when {@link me.ElieTGM.MaintenanceMode.bukkit.EnableRunnable} is used
     */
    private int taskId = -1;

    /**
     * Store the countdown value
     */
    private int countdown;
    
    private boolean ChangeServerIconInMaintenance;
    
    public ProtocolManager protocolManager;
    private PingProtocolEvent listener;
    
	private static BukkitPlugin instance = null; //instance static methods //
	public static BukkitPlugin getInstance() {return instance; } //instance static methods //

    @SuppressWarnings({ "static-access" })
	@Override
    public void onEnable() {
        saveDefaultConfig();

        reload(false);

        PluginCommand command = getCommand("maintenance");

        CommandMaintenance cmd = new CommandMaintenance(this);
        command.setExecutor(cmd);
        command.setTabCompleter(cmd);

        Bukkit.getServer().getPluginManager().registerEvents(new ServerListener(this), this);
        
        instance = this;
        
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.listener = new PingProtocolEvent();
        this.listener.addPingResponsePacketListener();
        
    }

    @Override
    public void onDisable() {
        try {
            getConfig().set("enabled", enabled);
            getConfig().set("whitelist", whitelist);

            getConfig().save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().severe("Unable to save configuration values");
        }
    }

    /**
     * Kick a {@link org.bukkit.entity.Player} from the server
     * @param kick null to kick all players
     */
    public void kick(Player kick) {
        if(kick == null) {
            boolean skip = (whitelist == null || whitelist.size() < 1);
            for(Player player : Bukkit.getOnlinePlayers()) {
                if(skip) {
                    player.kickPlayer(getKickMessage().replaceAll("%newline", "\n"));
                } else {
                    if (!player.hasPermission("maintenance.bypass")) {
                        if (!whitelist.contains(player.getName())) {
                            player.kickPlayer(getKickMessage().replaceAll("%newline", "\n"));
                        }
                    }
                }
            }
        } else {
            kick.kickPlayer(getKickMessage().replaceAll("%newline", "\n"));
        }
    }

    /**
     * Get whether maintenance mode is enabled
     * @return {@link #enabled}
     */
    public boolean getEnabled() {
        return this.enabled;
    }

    /**
     * Get the maintenance mode motd
     * @return {@link #message_motd}
     */
    public String getMotd() {
        return this.message_motd;
    }

    /**
     * Get the message players will be kicked with when maintenance mode is active
     * @return {@link #message_kick}
     */
    public String getKickMessage() {
        return this.message_kick;
    }

    /**
     * Get the list of whitelisted players
     * @return {@link #whitelist}
     */
    public List<String> getWhitelist() {
        return whitelist;
    }


    public void setMaintenanceEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Reload our configuration file
     */
    public void reload(boolean file) {
        if(file) {
            reloadConfig();
        }
        alertTimes = new ArrayList<>();
        countdown = getConfig().getInt("activation.countdown", 15);
        List<String> list = getConfig().getStringList("activation.announce");
        for(String s : list) {
            try {
                int val = Integer.parseInt(s);
                if(!alertTimes.contains(val)) {
                    alertTimes.add(val);
                }
            } catch (NumberFormatException ex) {
                // proceed
            }
        }
        
        ChangeServerIconInMaintenance = getConfig().getBoolean("ChangeServerIconInMaintenance");
        
        countdownMessage = getConfig().getString("messages.activation", "&cServer entering maintenance mode in {{ TIME }}");
        whitelist = getConfig().getStringList("whitelist");
        enabled = getConfig().getBoolean("enabled");
        message_motd = getConfig().getString("messages.motd", "&c&lMaintenance Mode");
        message_kick = getConfig().getString("messages.kick", "&cThe server is in maintenance mode, sorry for any inconvenience.");

        protocolMessage = getConfig().getString("serverprotocol", "Maintenance");
        message_motd = colour(message_motd);
        message_kick = colour(message_kick);
    }

    /**
     * @return the value to count down from
     */
    public int getCountdown() {
        return countdown;
    }

    /**
     * @return list of seconds to alert on
     */
    public List<Integer> getAlertTimes() {
        return alertTimes;
    }

    /**
     * @return format for the countdown
     */
    public String getCountdownMessage() {
        return countdownMessage;
    }

    public String getProtocolMessage() {
        return protocolMessage;
    }

    
    /**
     * @return id of the active EnableRunnable
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Set the id of the active EnableRunnable
     *
     * @param id task id
     */
    public void setTaskId(int id) {
        this.taskId = id;
    }

    /**
     * Clear the task
     */
    public void clearTask() {
        // Added precaution
        if (taskId != -1) {
            if (Bukkit.getScheduler().isCurrentlyRunning(taskId)) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
        }
        taskId = -1;
    }
}
