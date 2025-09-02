package gg.hcfactions.cx.command;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.menu.InvseeMenu;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.bukkit.services.impl.ranks.RankService;
import gg.hcfactions.libs.bukkit.services.impl.ranks.model.impl.AresRank;
import gg.hcfactions.libs.bukkit.utils.Enchants;
import gg.hcfactions.libs.bukkit.utils.Players;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public final class EssentialCommand extends BaseCommand {
    public final CXService service;

    @CommandAlias("world")
    @CommandPermission(CXPermissions.CX_MOD)
    @Description("Change your world")
    @Syntax("<world>")
    @CommandCompletion("@worlds")
    public void onChangeWorld(Player player, String worldName) {
        final World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "World not found");
            return;
        }

        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.YELLOW + "World has been changed to " + ChatColor.BLUE + world.getName());
    }

    @CommandAlias("broadcast")
    @CommandPermission(CXPermissions.CX_MOD)
    @Syntax("[-p|-r] <message>")
    @Description("Broadcast a message")
    public void onBroadcast(CommandSender sender, String message) {
        final String[] split = message.split(" ");
        final boolean asPlayer = (split.length > 1 && split[0].equalsIgnoreCase("-p"));
        final boolean raw = (split.length > 1 && split[0].equalsIgnoreCase("-r"));

        if (raw) {
            final String trimmed = message.substring(3);
            Bukkit.broadcast(service.getPlugin().getMiniMessage().deserialize(trimmed));
            return;
        }

        if (asPlayer) {
            final String trimmed = message.substring(3);
            final Component senderComponent = Component.text("[", NamedTextColor.GRAY)
                            .append(Component.text(sender.getName(), NamedTextColor.DARK_RED))
                                    .append(Component.text("]", NamedTextColor.GRAY))
                                            .appendSpace();

            Bukkit.broadcast(senderComponent.append(Component.text(trimmed, NamedTextColor.RED)));
            return;
        }

        Bukkit.broadcast(Component.text("[", NamedTextColor.GRAY)
                .append(Component.text("Staff", NamedTextColor.DARK_RED))
                .append(Component.text("]", NamedTextColor.GRAY))
                .appendSpace().append(Component.text(message, NamedTextColor.RED)));
    }

    @CommandAlias("rename")
    @Description("Rename the item in your hand")
    @Syntax("<name>")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onRenameItem(Player player, String name) {
        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().equals(Material.AIR)) {
            player.sendMessage(Component.text("You are not holding an item", NamedTextColor.RED));
            return;
        }

        final ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            player.sendMessage(Component.text("Item does not have any metadata", NamedTextColor.RED));
            return;
        }

        Component deserializedName = service.getPlugin().getMiniMessage().deserialize(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        meta.displayName(deserializedName);
        hand.setItemMeta(meta);

        player.sendMessage(Component.text("Item has been renamed to")
                .appendSpace().append(deserializedName));
    }

    @CommandAlias("repair")
    @Description("Repair your items/armor")
    @Syntax("[-a]")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onRepairItem(Player player, @Optional @Values("-a") String all) {
        if (all != null && all.equalsIgnoreCase("-a")) {
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor == null || armor.getType().equals(Material.AIR)) {
                    continue;
                }

                armor.setDurability((short)0);
            }

            player.sendMessage(ChatColor.YELLOW + "Your armor has been repaired");
            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().equals(Material.AIR)) {
            player.sendMessage(ChatColor.RED + "You are not holding an item");
            return;
        }

        hand.setDurability((short)0);
        player.sendMessage(ChatColor.YELLOW + "Your item has been repaired");
    }

    @CommandAlias("gamemode|gm")
    @Description("Change your gamemode")
    @Syntax("<survival|creative|adventure|spectator>")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onGamemodeChange(
            Player player, @Values("creative|survival|adventure|spectator|c|s|a|spec|0|1|2|3") String gamemodeName,
            @Optional String username
    ) {
        GameMode gamemode = null;

        if (gamemodeName.equalsIgnoreCase("s") || gamemodeName.equalsIgnoreCase("survival") || gamemodeName.equalsIgnoreCase("0")) {
            gamemode = GameMode.SURVIVAL;
        } else if (gamemodeName.equalsIgnoreCase("c") || gamemodeName.equalsIgnoreCase("creative") || gamemodeName.equalsIgnoreCase("1")) {
            gamemode = GameMode.CREATIVE;
        } else if (gamemodeName.equalsIgnoreCase("a") || gamemodeName.equalsIgnoreCase("adventure") || gamemodeName.equalsIgnoreCase("2")) {
            gamemode = GameMode.ADVENTURE;
        } else if (gamemodeName.equalsIgnoreCase("spec") || gamemodeName.equalsIgnoreCase("spectator") || gamemodeName.equalsIgnoreCase("3")) {
            gamemode = GameMode.SPECTATOR;
        }

        if (gamemode == null) {
            player.sendMessage(ChatColor.RED + "Invalid gamemode");
            return;
        }

        if (username != null) {
            final Player otherPlayer = Bukkit.getPlayer(username);

            if (otherPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            if (otherPlayer.getGameMode().equals(gamemode)) {
                player.sendMessage(ChatColor.RED + "Player gamemode is already set");
                return;
            }

            otherPlayer.setGameMode(gamemode);
            otherPlayer.sendMessage(ChatColor.YELLOW + "Your gamemode has been changed to " + ChatColor.BLUE + StringUtils.capitalize(gamemode.name().toLowerCase()));
            player.sendMessage(ChatColor.GOLD + otherPlayer.getName() + ChatColor.YELLOW + "'s gamemode has been changed to " + ChatColor.BLUE + StringUtils.capitalize(gamemode.name().toLowerCase()));
            return;
        }

        if (player.getGameMode().equals(gamemode)) {
            player.sendMessage(ChatColor.RED + "Your gamemode is already set");
            return;
        }

        player.setGameMode(gamemode);
        player.sendMessage(ChatColor.YELLOW + "Your gamemode has been changed to " + ChatColor.BLUE + StringUtils.capitalize(gamemode.name().toLowerCase()));
    }

    @CommandAlias("clear")
    @Description("Clear an inventory")
    @Syntax("[username]")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onClearInventory(Player player, @Optional String username) {
        if (username != null) {
            final Player otherPlayer = Bukkit.getPlayer(username);
            if (otherPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            otherPlayer.getInventory().clear();
            otherPlayer.getInventory().setArmorContents(null);
            otherPlayer.sendMessage(ChatColor.YELLOW + "Your inventory was cleared by " +  ChatColor.BLUE + player.getName());
            player.sendMessage(ChatColor.YELLOW + "You have cleared " + ChatColor.BLUE + otherPlayer.getName() + ChatColor.YELLOW + "'s inventory");
            return;
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.sendMessage(ChatColor.YELLOW + "Your inventory has been cleared");
    }

    @CommandAlias("heal")
    @Description("Heal a player")
    @Syntax("[username]")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onHeal(Player player, @Optional String username) {
        if (username != null) {
            final Player otherPlayer = Bukkit.getPlayer(username);
            if (otherPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            Players.resetHealth(otherPlayer);
            otherPlayer.sendMessage(ChatColor.YELLOW + "You have been healed by " + ChatColor.BLUE + player.getName());
            player.sendMessage(ChatColor.YELLOW + "You have healed " + ChatColor.BLUE + otherPlayer.getName());
            return;
        }

        Players.resetHealth(player);
        player.sendMessage(ChatColor.YELLOW + "You have been healed");
    }

    @CommandAlias("invsee")
    @Description("Spectate a players inventory")
    @CommandCompletion("@players")
    @Syntax("<username>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onInvsee(Player player, String username) {
        final Player otherPlayer = Bukkit.getPlayer(username);
        if (otherPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        final InvseeMenu menu = new InvseeMenu(service, player, otherPlayer);
        menu.open();
    }

    /*
        /tp johnsama - teleport to player
        /tp johnsama Symotic - teleport player to player
        /tp 100 100 100 - teleport to specific coordinates in current world
        /tp 100 100 100 world - teleport to specific coordinates in specific world
        /tpall - teleport all players on the server to current location
     */

    @CommandAlias("teleport|tp")
    @Description("Teleport to a player")
    @Syntax("<name>")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String username) {
        final Player otherPlayer = Bukkit.getPlayer(username);

        if (otherPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        player.teleport(otherPlayer);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + otherPlayer.getName());
    }

    @CommandAlias("teleport|tp")
    @Description("Teleport a player to another player")
    @Syntax("<name> <name>")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String username, String otherUsername) {
        final Player fromPlayer = Bukkit.getPlayer(username);
        final Player toPlayer = Bukkit.getPlayer(otherUsername);

        if (fromPlayer == null) {
            player.sendMessage(ChatColor.RED + username + " not found");
            return;
        }

        if (toPlayer == null) {
            player.sendMessage(ChatColor.RED + otherUsername + " not found");
            return;
        }

        fromPlayer.teleport(toPlayer);
        fromPlayer.sendMessage(ChatColor.YELLOW + "You have been teleported to " + ChatColor.BLUE + toPlayer.getName());
    }

    @CommandAlias("teleport|tp")
    @Description("Teleport to specific coordinates in your current world")
    @Syntax("<x> <y> <z>")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String namedX, String namedY, String namedZ) {
        double x, y, z;
        try {
            x = Double.parseDouble(namedX);
            y = Double.parseDouble(namedY);
            z = Double.parseDouble(namedZ);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates");
            return;
        }

        final Location location = new Location(player.getWorld(), x, y, z);
        player.teleport(location);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + x + ", " + y + ", " + z + ChatColor.YELLOW + " in " + ChatColor.BLUE + player.getWorld().getName());
    }

    @CommandAlias("teleport|tp")
    @Description("Teleport to specific coordinates in a specific world")
    @Syntax("<x> <y> <z> [world]")
    @CommandCompletion("@worlds")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onTeleport(Player player, String namedX, String namedY, String namedZ, String namedWorld) {
        final World world = Bukkit.getWorld(namedWorld);

        if (world == null) {
            player.sendMessage(ChatColor.RED + "World not found");
            return;
        }

        double x, y, z;
        try {
            x = Double.parseDouble(namedX);
            y = Double.parseDouble(namedY);
            z = Double.parseDouble(namedZ);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinates");
            return;
        }

        final Location location = new Location(world, x, y, z);
        player.teleport(location);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + x + ", " + y + ", " + z + ChatColor.YELLOW + " in " + ChatColor.BLUE + world.getName());
    }

    @CommandAlias("tpall")
    @Description("Teleport all players in the server to your current location")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onTeleportAll(Player player) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                p.teleport(player);
                p.sendMessage(ChatColor.YELLOW + "You have been teleported to " + ChatColor.BLUE + player.getName());
            }
        });

        player.sendMessage(ChatColor.YELLOW + "Teleported " + ChatColor.BLUE + (Bukkit.getOnlinePlayers().size() - 1) + " players" + ChatColor.YELLOW + " to your current location");
    }

    @CommandAlias("enchant|enchantment")
    @Description("Enchant item(s)")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @CommandCompletion("@enchants")
    @Syntax("<enchantment> <level> [-a]")
    public void onEnchant(Player player, String enchantmentName, String levelName, @Optional String flag) {
        final Enchantment enchantment = Enchants.getEnchantment(enchantmentName);

        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "Enchantment not found");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(levelName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid enchantment level (integer)");
            return;
        }

        if (flag != null) {
            if (!flag.equalsIgnoreCase("-a")) {
                player.sendMessage(ChatColor.RED + "Invalid flag");
                return;
            }

            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor == null || armor.getType().equals(Material.AIR)) {
                    continue;
                }

                armor.addUnsafeEnchantment(enchantment, level);
                player.sendMessage(ChatColor.YELLOW + "Enchanted " + ChatColor.RESET + StringUtils.capitalize(armor.getType().name().replaceAll("_", " "))
                        + ChatColor.YELLOW + " with " + ChatColor.BLUE + StringUtils.capitalize(enchantment.getKey().getKey()) + " " + level);
            }

            return;
        }

        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType().equals(Material.AIR) || !hand.getType().isItem()) {
            player.sendMessage(ChatColor.RED + "You are not holding a valid item");
            return;
        }

        hand.addUnsafeEnchantment(enchantment, level);
        player.sendMessage(ChatColor.YELLOW + "Enchanted " + ChatColor.RESET + StringUtils.capitalize(hand.getType().name().replaceAll("_", " "))
                + ChatColor.YELLOW + " with " + ChatColor.BLUE + StringUtils.capitalize(enchantment.getKey().getKey()) + " " + level);
    }

    @CommandAlias("list")
    @Description("List all online players")
    public void onList(Player player) {
        final RankService rankService = (RankService) service.getPlugin().getService(RankService.class);
        final Map<AresRank, List<String>> res = Maps.newTreeMap(Comparator.comparingInt(AresRank::getWeight));
        final List<String> defaultNames = Lists.newArrayList();

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            if (player.canSee(onlinePlayer)) {
                final AresRank rank = rankService.getHighestRank(onlinePlayer);

                if (rank == null) {
                    defaultNames.add(onlinePlayer.getName());
                } else if (res.containsKey(rank)) {
                    res.get(rank).add(onlinePlayer.getName());
                } else {
                    res.put(rank, Lists.newArrayList(onlinePlayer.getName()));
                }
            }
        });

        final List<String> result = Lists.newArrayList();
        final List<String> rankNames = Lists.newArrayList();

        res.forEach((rank, usernames) -> {
            rankNames.add(net.md_5.bungee.api.ChatColor.of(rank.getColorCode()) + StringUtils.capitalize(rank.getName()));
            usernames.forEach(name -> result.add(net.md_5.bungee.api.ChatColor.of(rank.getColorCode()) + name));
        });

        rankNames.add("Default");

        Collections.reverse(result);
        result.addAll(defaultNames);

        player.sendMessage(" ");
        player.sendMessage(ChatColor.WHITE + "Player List (" + ChatColor.AQUA + result.size() + " online" + ChatColor.WHITE + ")");
        player.sendMessage(Joiner.on(ChatColor.RESET + ", ").join(rankNames));
        player.sendMessage(Joiner.on(ChatColor.RESET + ", ").join(result));
        player.sendMessage(" ");
    }
}
