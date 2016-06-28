package de.minebench.recordingmode;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
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
public class RecordingCommand implements CommandExecutor {
    private MbRecordingMode plugin;

    public RecordingCommand(MbRecordingMode plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0) {
            if("reload".equalsIgnoreCase(args[0])) {
                plugin.loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
            } else if("check".equalsIgnoreCase(args[0])) {
                Player player = getPlayer(sender, label, args);
                if(player != null) {
                    sender.sendMessage(ChatColor.YELLOW +
                            (player == sender ?
                                    "You are" :
                                    player.getName() + " is") +
                            (plugin.isRecording(player) ?
                                    ChatColor.GREEN :
                                    " not" + ChatColor.RED)
                            + " RECORDING"
                    );
                }

            } else if("toggle".equalsIgnoreCase(args[0])) {
                Player player = getPlayer(sender, label, args);
                if(player != null) {
                    if(plugin.toggleRecording(player) && player != sender) {
                        sender.sendMessage(ChatColor.YELLOW + "Recording for " + player.getName() + " is now " +
                                (plugin.isRecording(player) ?
                                        ChatColor.GREEN + "enabled" :
                                        ChatColor.RED + "disabled")
                        );
                    } else {
                        sender.sendMessage(ChatColor.RED + "Unknown error while toggling recording?");
                    }
                }

            } else {
                return false;
            }
        } else {
            plugin.toggleRecording(sender);
        }
        return true;
    }

    private Player getPlayer(CommandSender sender, String label, String[] args) {
        Player player = null;
        if(args.length > 1) {
            player = plugin.getServer().getPlayer(args[1]);
            if(player == null || !player.isOnline()) {
                sender.sendMessage(ChatColor.RED + "No player with the name " + args[1] + " was found online!");
            }
        } else if(sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Please use /" + label + " " + args[0] + " <playername> to run this command from the console!");
        }
        return player;
    }
}
