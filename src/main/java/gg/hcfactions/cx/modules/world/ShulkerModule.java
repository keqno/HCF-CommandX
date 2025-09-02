package gg.hcfactions.cx.modules.world;

import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.event.ShulkerPlaceEvent;
import gg.hcfactions.cx.hologram.EHologramOrder;
import gg.hcfactions.cx.hologram.impl.Hologram;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ShulkerModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private final Set<LockedShulkerBox> lockedShulkerRepository;

    public ShulkerModule(CXService service) {
        this.service = service;
        this.key = "world.shulkers.";
        this.lockedShulkerRepository = Sets.newConcurrentHashSet();
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        service.getPlugin().registerListener(this);
    }

    @Override
    public void onDisable() {
        lockedShulkerRepository.forEach(LockedShulkerBox::unlock);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        setEnabled(true);
    }

    private Optional<LockedShulkerBox> getLockedShulker(Block block) {
        return lockedShulkerRepository
                .stream()
                .filter(ls -> ls.getBlock().getX() == block.getX()
                        && ls.getBlock().getY() == block.getY()
                        && ls.getBlock().getZ() == block.getZ()
                        && ls.getBlock().getWorld().getName().equalsIgnoreCase(block.getWorld().getName()))
                .findFirst();
    }

    private Optional<LockedShulkerBox> getLockedShulkerByArmorStand(ArmorStand armorStand) {
        final PLocatable loc = new PLocatable(armorStand);

        return lockedShulkerRepository
                .stream()
                .filter(ls -> ls.getHologram().getOrigin().getDistance(loc) > 0.0 && ls.getHologram().getOrigin().getDistance(loc) < 1.0)
                .findFirst();
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled() || event.isCancelled()) {
            return;
        }

        final Block block = event.getBlock();

        getLockedShulker(block).ifPresent(lockedShulkerBox -> {
            lockedShulkerBox.getHologram().despawn();
            lockedShulkerRepository.remove(lockedShulkerBox);
        });
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnabled() || event.isCancelled()) {
            return;
        }

        final Block block = event.getBlock();
        final Player player = event.getPlayer();

        if (!block.getType().name().contains("SHULKER_BOX")) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_MOD)) {
            return;
        }

        final ShulkerPlaceEvent placeEvent = new ShulkerPlaceEvent(player, block);
        Bukkit.getPluginManager().callEvent(placeEvent);

        if (placeEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (placeEvent.getDuration() <= 0) {
            return;
        }

        final LockedShulkerBox lockedShulkerBox = new LockedShulkerBox(block, placeEvent.getDuration());
        lockedShulkerRepository.add(lockedShulkerBox);
        lockedShulkerBox.getHologram().spawn();
        lockedShulkerBox.startTickingTask();
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!isEnabled() || !event.useInteractedBlock().equals(Event.Result.ALLOW)) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        final Action action = event.getAction();

        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_MOD)) {
            return;
        }

        if (block == null || !block.getType().name().contains("SHULKER_BOX")) {
            return;
        }

        getLockedShulker(block).ifPresent(lockedShulkerBox -> {
            if (!lockedShulkerBox.isExpired()) {
                event.setUseInteractedBlock(Event.Result.DENY);
                player.sendMessage(Component.text("This Shulker Box will unlock in", NamedTextColor.RED)
                        .appendSpace().append(Component.text(Time.convertToDecimal(lockedShulkerBox.getExpire() - Time.now()), NamedTextColor.RED).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE))
                        .append(Component.text("s", NamedTextColor.RED).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)));
            }
        });
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onArmorStandInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof final ArmorStand armorStand)) {
            return;
        }

        getLockedShulkerByArmorStand(armorStand).ifPresent(holo -> event.setCancelled(true));
    }

    public class LockedShulkerBox {
        @Getter public final Block block;
        @Getter public final Hologram hologram;
        @Getter @Setter public long expire;
        @Getter public BukkitTask tickingTask;

        public LockedShulkerBox(Block block) {
            this.block = block;

            this.hologram = new Hologram(
                    service,
                    -1,
                    List.of(Component.text("Locked", NamedTextColor.RED), Component.text(Time.convertToRemaining(30*1000L), NamedTextColor.YELLOW)),
                    new PLocatable(block.getWorld().getName(), block.getX() + 0.5, block.getY() - 0.5, block.getZ() + 0.5, 0.0F, 0.0F),
                    EHologramOrder.DESCENDING
            );

            this.expire = Time.now() + (30 * 1000L);
        }

        public LockedShulkerBox(Block block, int lockSeconds) {
            this.block = block;

            this.hologram = new Hologram(
                    service,
                    -1,
                    List.of(Component.text("Locked", NamedTextColor.RED), Component.text(Time.convertToRemaining(lockSeconds*1000L), NamedTextColor.YELLOW)),
                    new PLocatable(block.getWorld().getName(), block.getX() + 0.5, block.getY() - 0.5, block.getZ() + 0.5, 0.0F, 0.0F),
                    EHologramOrder.DESCENDING
            );

            this.expire = Time.now() + (lockSeconds * 1000L);
        }

        public void startTickingTask() {
            if (tickingTask != null) {
                tickingTask.cancel();;
                tickingTask = null;
            }

            tickingTask = new Scheduler(service.getPlugin()).sync(() -> {
                if (isExpired()) {
                    unlock();
                    return;
                }

                hologram.updateLine(1, Component.text(Time.convertToRemaining(getExpire() - Time.now()), NamedTextColor.YELLOW));
            }).repeat(0L, 20L).run();
        }

        public void stopTickingTask() {
            if (tickingTask == null) {
                return;
            }

            tickingTask.cancel();
            tickingTask = null;
        }

        public void unlock() {
            if (hologram != null) {
                hologram.despawn();
            }

            if (tickingTask != null) {
                stopTickingTask();
            }

            lockedShulkerRepository.remove(this);
        }

        public boolean isExpired() {
            return expire <= Time.now();
        }
    }
}
