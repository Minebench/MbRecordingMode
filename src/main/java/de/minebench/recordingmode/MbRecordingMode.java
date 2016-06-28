package de.minebench.recordingmode;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.kitteh.vanish.VanishPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * MbRecordingMode
 * Copyright (C) 2016 Max Lee (https://github.com/Phoenix616/)
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class MbRecordingMode extends JavaPlugin implements Listener {

    // UUID String to start LocationInfo mapping
    private Map<String, LocationInfo> recordingPlayers = new HashMap<String, LocationInfo>();

    private ConfigAccessor recordingYml;

    private VanishPlugin vnp;
    private String prefix = ChatColor.DARK_RED + "\u25cf " + ChatColor.RESET;

    public void onEnable() {
        recordingYml = new ConfigAccessor(this, "recording.yml");
        ConfigurationSerialization.registerClass(LocationInfo.class, "LocationInfo");
        loadConfig();
        if (getServer().getPluginManager().isPluginEnabled("VanishNoPacket")) {
            vnp = (VanishPlugin) getServer().getPluginManager().getPlugin("VanishNoPacket");
        } else {
            getLogger().log(Level.SEVERE, "VanishNoPacket was not found/is not enabled?");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("recordingmode").setExecutor(new RecordingCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void loadConfig() {
        new BukkitRunnable() {
            public void run() {
                saveDefaultConfig();
                reloadConfig();
                recordingYml.reloadConfig();

                if(recordingYml.getConfig().isSet("users")) {
                    for(String uuid : recordingYml.getConfig().getConfigurationSection("users").getKeys(false)) {
                        recordingPlayers.put(uuid, (LocationInfo) recordingYml.getConfig().get("users." + uuid));
                    }
                }
            }
        }.runTaskAsynchronously(this);
    }

    /**
     * Toggle the recording state of a player
     *
     * @param sender The player to set the state for
     * @return <tt>true</tt> if the state was changed, <tt>false</tt> if it wasn't
     */
    public boolean toggleRecording(CommandSender sender) {
        if (sender instanceof Player) {
            if (setRecording((Player) sender, !isRecording((Player) sender))) {
                sender.sendMessage(ChatColor.YELLOW + "You are " + (
                        isRecording((Player) sender) ?
                                "now " + ChatColor.GREEN + "RECORDING" :
                                "no longer " + ChatColor.RED + "RECORDING"
                ) + ChatColor.YELLOW + "!");
                return true;
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Only a player can be set to recording!");
        }
        return false;
    }

    /**
     * Set the recording state of a player
     *
     * @param player  The player to set the state for
     * @param enabled On/Off
     * @return <tt>true</tt> if the state was changed, <tt>false</tt> if it wasn't
     */
    private boolean setRecording(Player player, boolean enabled) {
        if ((enabled && isRecording(player)) || !enabled && !isRecording(player)) {
            return false;
        }

        if (enabled) {
            recordingPlayers.put(player.getUniqueId().toString(), new LocationInfo(player.getLocation()));
        } else {
            Location location = recordingPlayers.remove(player.getUniqueId().toString()).toBukkit();
            player.teleport(location);
        }
        recordingYml.getConfig().createSection("users", recordingPlayers);
        new BukkitRunnable() {
            public void run() {
                recordingYml.saveConfig();
            }
        }.runTaskAsynchronously(this);
        setFeatures(player, enabled);

        return true;
    }

    /**
     * Enabled ot disable the recording features
     */
    private void setFeatures(Player player, boolean enabled) {
        player.setAllowFlight(enabled);
        player.setFlying(enabled);
        if (enabled) {
            boolean sleepingIgnored = player.isSleepingIgnored();
            vnp.getManager().vanish(player, true, false);
            player.setSleepingIgnored(sleepingIgnored);

            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team == null) {
                Team recordingTeam = player.getScoreboard().getTeam("tag_recording");
                if (recordingTeam == null) {
                    recordingTeam = player.getScoreboard().registerNewTeam("tag_recording");
                    recordingTeam.setPrefix(ChatColor.DARK_RED + "\u25cf "); // black filled circle
                }
                team = recordingTeam;
            } else if (team.getName().startsWith("tag_")) {
                String teamName = team.getName().length() < 16 ? team.getName() + "r" : team.getName().substring(0, 15) + "r";
                Team newTeam = player.getScoreboard().getTeam(teamName);
                if (newTeam == null) {
                    newTeam = player.getScoreboard().registerNewTeam(teamName);
                    try  {
                        newTeam.setPrefix(prefix + team.getPrefix()); // black filled circle
                    } catch (IllegalArgumentException e){ // prefix longer than 32
                        newTeam.setPrefix(team.getPrefix());
                    }
                    newTeam.setSuffix(team.getSuffix());
                }
                team = newTeam;
            } else {
                team = null;
            }
            if (team != null) {
                team.addEntry(player.getName());
            }

        } else {
            vnp.getManager().reveal(player, true, false);

            Team team = player.getScoreboard().getEntryTeam(player.getName());
            if (team != null) {
                if ("tag_recording".equals(team.getName())) {
                    team.removeEntry(player.getName());
                } else if (team.getName().startsWith("tag_")) {
                    String teamName = team.getName().substring(0, team.getName().length() - 1);
                    Team tagTeam = player.getScoreboard().getTeam(teamName);
                    if (tagTeam != null) {
                        tagTeam.addEntry(player.getName());
                    } else {
                        tagTeam = player.getScoreboard().registerNewTeam(teamName);
                        tagTeam.setPrefix(team.getPrefix().substring(5));
                        tagTeam.setSuffix(team.getSuffix());
                    }
                    tagTeam.addEntry(player.getName());
                }
            }
        }
    }


    public boolean isRecording(Player player) {
        return isRecording(player.getUniqueId());
    }

    public boolean isRecording(UUID playerId) {
        return recordingPlayers.containsKey(playerId.toString());
    }

    @EventHandler
    public void onWorldSwitch(PlayerChangedWorldEvent event) {
        if (isRecording(event.getPlayer()) && !event.getPlayer().hasPermission("mbrecordingmode.worldswitch")) {
            setFeatures(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        if (isRecording(event.getPlayer()) && !event.getPlayer().hasPermission("mbrecordingmode.logout")) {
            setFeatures(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
        if (isRecording(event.getPlayer()) && !event.getPlayer().hasPermission("mbrecordingmode.logout")) {
            setRecording(event.getPlayer(), false);
        }
    }
}
