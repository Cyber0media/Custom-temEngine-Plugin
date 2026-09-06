package com.customitem.engine;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles /engine, /engine give, /engine reload, /engine recipes.
 */
public final class EngineCommand implements CommandExecutor, TabCompleter {

    private final CustomItemEngine plugin;

    public EngineCommand(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Console cannot open the GUI.");
                return true;
            }
            plugin.getGuiManager().openMain((Player) sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("customitemengine.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                try {
                    plugin.reload();
                    sender.sendMessage(ChatColor.GREEN + "CustomItemEngine reloaded. Items: "
                            + plugin.getItemRegistry().size() + ", Active recipes: "
                            + plugin.getRecipeManager().activeCount());
                } catch (Throwable t) {
                    sender.sendMessage(ChatColor.RED + "Reload error: " + t.getMessage());
                }
                return true;

            case "recipes":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW
                        + "Right-click an item in /engine to open the Recipe Builder.");
                plugin.getGuiManager().openMain((Player) sender);
                return true;

            case "give":
                if (!sender.hasPermission("customitemengine.admin")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /engine give <player> <item_id>");
                    return true;
                }
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }
                String id = args[2];
                if (!plugin.getItemRegistry().has(id)) {
                    sender.sendMessage(ChatColor.RED + "Unknown item: " + id);
                    return true;
                }
                ItemStack item = plugin.getItemRegistry().build(id);
                if (item == null) {
                    sender.sendMessage(ChatColor.RED + "Could not build item: " + id);
                    return true;
                }
                target.getInventory().addItem(item).forEach((i, left) ->
                        target.getWorld().dropItemNaturally(target.getLocation(), left));
                sender.sendMessage(ChatColor.GREEN + "Gave " + id + " to " + target.getName());
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Usage: /engine [give <player> <item_id> | reload | recipes]");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "reload", "recipes").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return new ArrayList<>(plugin.getItemRegistry().all().keySet()).stream()
                    .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
