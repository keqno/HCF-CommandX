package gg.hcfactions.cx.modules.player.vanish;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.menu.VanishedChestMenu;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VanishManager implements Listener {
    @Getter public final CXService service;

    private final Set<UUID> vanishedPlayers;

    private final ImmutableList<Material> CONTAINER_MATS = ImmutableList.of(
            Material.CHEST,
            Material.TRAPPED_CHEST,
            Material.ENDER_CHEST,
            Material.SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX
    );

    public VanishManager(CXService service) {
        this.service = service;
        this.vanishedPlayers = Sets.newConcurrentHashSet();

        service.getPlugin().registerListener(this);
    }

    /**
     * @param player Player
     * @return True if player is vanished
     */
    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    /**
     * @param uuid Bukkit UUID
     * @return True if player is vanished
     */
    public boolean isVanished(UUID uuid) {
        return vanishedPlayers.contains(uuid);
    }

    /**
     * Vanish the player from the server
     * @param player Player to vanish
     */
    public void setVanished(Player player) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.hasPermission(CXPermissions.CX_MOD)) {
                p.hidePlayer(service.getPlugin(), player);
            }
        });

        vanishedPlayers.add(player.getUniqueId());
    }

    /**
     * Unvanish player from the server
     * @param player Player
     */
    public void unvanish(Player player) {
        Bukkit.getOnlinePlayers().forEach(p -> p.showPlayer(service.getPlugin(), player));
        vanishedPlayers.remove(player.getUniqueId());
    }

    /**
     * @return ImmutableList of all online players currently vanished
     */
    public ImmutableList<Player> getVanishedPlayers() {
        final List<Player> res = Lists.newArrayList();

        vanishedPlayers.forEach(vp -> {
            final Player bukkitPlayer = Bukkit.getPlayer(vp);

            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                res.add(bukkitPlayer);
            }
        });

        return ImmutableList.copyOf(res);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (!player.hasPermission(CXPermissions.CX_MOD)) {
            getVanishedPlayers().forEach(vp -> player.hidePlayer(service.getPlugin(), vp));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        if (isVanished(player)) {
            unvanish(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        final Action action = event.getAction();

        if (event.isCancelled()) {
            return;
        }

        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (block == null || !CONTAINER_MATS.contains(block.getType())) {
            return;
        }

        if (!isVanished(player)) {
            return;
        }

        final BlockState state = block.getState();
        final Container container = (Container)state;

        final VanishedChestMenu menu = new VanishedChestMenu(service.getPlugin(), player, container.getInventory());
        menu.open();

        event.setCancelled(true);
        player.sendMessage(ChatColor.DARK_AQUA + "Silently opening container...");
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        final Player player = event.getPlayer();

        if (isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        final Player player = event.getPlayer();

        if (isVanished(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onActivateSculk(BlockReceiveGameEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (!isVanished(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityCollide(ProjectileHitEvent event) {
        final Entity hitEntity = event.getHitEntity();

        if (hitEntity instanceof final Player player) {
            if (isVanished(player)) {
                event.setCancelled(true);
            }
        }
    }
}
