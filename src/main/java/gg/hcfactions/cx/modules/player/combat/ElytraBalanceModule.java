package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Maps;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;

public final class ElytraBalanceModule implements ICXModule, Listener {
    @Getter CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean removeElytraOnProjectileDamage;
    private boolean removeElytraBhopping;

    private BukkitTask bhopResetTask;
    private int bhopMaxViolations;
    private int bhopResetInterval;
    private Map<UUID, Integer> bhopVL;

    public ElytraBalanceModule(CXService service) {
        this.service = service;
        this.key = "combat.elytra.";
        this.bhopVL = Maps.newConcurrentMap();
    }

    @Override
    public void onEnable() {
        loadConfig();
        getPlugin().registerListener(this);

        bhopResetTask = new Scheduler(getPlugin()).async(() -> bhopVL.clear()).repeat(0L, bhopResetInterval * 20L).run();

        this.enabled = true;
    }

    @Override
    public void onDisable() {
        if (bhopResetTask != null) {
            bhopResetTask.cancel();
            bhopResetTask = null;
        }

        this.enabled = false;
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        removeElytraOnProjectileDamage = conf.getBoolean(getKey() + "remove_elytra_on_projectile_damage");
        removeElytraBhopping = conf.getBoolean(getKey() + "remove_elytra_bhopping");
        bhopMaxViolations = conf.getInt(getKey() + "bhop_max_vl");
        bhopResetInterval = conf.getInt(getKey() + "bhop_vl_reset_interval");
    }

    /**
     * Adds a b-hop attempt violation
     *
     * If the player reaches the violation threshold we'll pop their elytra off
     *
     * @param player Player
     */
    private void addVL(Player player) {
        final int currentVL = bhopVL.getOrDefault(player.getUniqueId(), 0);
        bhopVL.put(player.getUniqueId(), currentVL + 1);
    }

    /**
     * Resets a players b-hop violations
     *
     * @param player Player
     */
    private void resetVL(Player player) {
        bhopVL.remove(player.getUniqueId());
    }

    /**
     * Removes the elytra from the provided players chestplate slot and drops it
     * on the floor of their current location.
     *
     * @param player Player
     */
    private void removeElytraFromChestplate(Player player) {
        if (
                player.getEquipment() == null
                        || player.getEquipment().getChestplate() == null
                        || !player.getEquipment().getChestplate().getType().equals(Material.ELYTRA)
        ) {
            return;
        }

        final ItemStack elytra = player.getEquipment().getChestplate();

        player.getEquipment().setChestplate(new ItemStack(Material.AIR));
        player.getWorld().dropItemNaturally(player.getLocation(), elytra);
        player.sendMessage(ChatColor.RED + "Your elytra has fallen to your feet");
    }

    /**
     * Performs an air check to see if elytra flight should be possible
     *
     * @param player Player
     * @param dist Distance requirement
     * @return True if the player is high enough from the floor
     */
    private boolean isInAir(Player player, int dist) {
        final Location location = player.getLocation();

        for (int i = 0; i < dist; i++) {
            final Location newLocation = new Location(location.getWorld(), location.getBlockX(), location.getBlockY() - i, location.getBlockZ());

            if (newLocation.getBlock().getType().equals(Material.AIR) || !newLocation.getBlock().getType().isSolid()) {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * Removes elytra if hit by a projectile
     * @param event ProjectileHitEvent
     */
    @EventHandler (priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.isCancelled() || !removeElytraOnProjectileDamage) {
            return;
        }

        if (!(event.getHitEntity() instanceof final Player player)) {
            return;
        }

        if (player.isGliding()) {
            player.setGliding(false);
        }

        removeElytraFromChestplate(player);
    }

    /**
     * Removes elytra while attempting to b-hop
     * @param event EntityToggleGlideEvent
     */
    @EventHandler
    public void onPlayerToggleFlight(EntityToggleGlideEvent event) {
        if (!isEnabled() || !removeElytraBhopping || event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (!event.isGliding()) {
            return;
        }

        if (!isInAir(player, 3)) {
            addVL(player);

            final int currentVL = bhopVL.getOrDefault(player.getUniqueId(), 0);

            if (currentVL >= bhopMaxViolations) {
                removeElytraFromChestplate(player);
                resetVL(player);
            }

            event.setCancelled(true);
        }
    }
}
