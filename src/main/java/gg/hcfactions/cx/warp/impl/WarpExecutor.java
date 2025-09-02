package gg.hcfactions.cx.warp.impl;

import gg.hcfactions.cx.warp.IWarpExecutor;
import gg.hcfactions.cx.warp.WarpManager;
import gg.hcfactions.libs.base.consumer.FailablePromise;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.BLocatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
public final class WarpExecutor implements IWarpExecutor {
    @Getter public WarpManager manager;

    @Override
    public void createWarp(Player player, String warpName, Promise promise) {
        if (manager.getWarp(warpName).isPresent()) {
            promise.reject("Warp name is already in use");
            return;
        }

        final Warp warp = new Warp(player, warpName);
        manager.getWarpRepository().add(warp);
        manager.saveWarp(warp);
        promise.resolve();
    }

    @Override
    public void removeWarp(Player player, String warpName, Promise promise) {
        final Optional<Warp> existing = manager.getWarp(warpName);

        if (existing.isEmpty()) {
            promise.reject("Warp not found");
            return;
        }

        manager.deleteWarp(existing.get());
        promise.resolve();
    }

    @Override
    public void listWarps(Player player) {
        if (manager.getWarpRepository().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No warps set");
            return;
        }

        player.sendMessage(Component.text("Warp List", NamedTextColor.GOLD)
                .appendSpace().append(Component.text("(" + manager.getWarpRepository().size() + " available)", NamedTextColor.DARK_AQUA)));

        manager.getWarpRepository().forEach(w -> {
            Component component = Component.text("-")
                    .appendSpace().append(Component.text(w.getName(), NamedTextColor.GOLD)
                            .hoverEvent(Component.text("Click to teleport")
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/warp " + w.getName()))));

            player.sendMessage(component);
        });
    }

    @Override
    public void createGateway(Player player, String destinationName, Promise promise) {
        final Optional<Warp> warpQuery = manager.getWarp(destinationName);

        if (warpQuery.isEmpty()) {
            promise.reject("Warp not found");
            return;
        }

        final Optional<WarpGateway> gatewayQuery = manager.getGateway(player.getLocation().getBlock());

        if (gatewayQuery.isPresent()) {
            promise.reject("This block is already a gateway");
            return;
        }

        final Warp warp = warpQuery.get();
        final Block origin = player.getLocation().getBlock();
        final Block portalBlock = origin.getRelative(BlockFace.DOWN);
        final WarpGateway gateway = new WarpGateway(manager.getService(), UUID.randomUUID(), warp.getName(), new BLocatable(portalBlock));

        manager.getGatewayRepository().add(gateway);
        manager.saveGateway(gateway);
        portalBlock.setType(Material.END_GATEWAY);

        promise.resolve();
    }

    @Override
    public void createGatewayRadius(Player player, String destinationName, int radius, FailablePromise<Integer> promise) {
        final Location loc = player.getLocation();

        if (radius > 16) {
            promise.reject("Radius must be 16 blocks of less");
            return;
        }

        final int maxX = loc.getBlockX() + radius;
        final int maxY = loc.getBlockY() + radius;
        final int maxZ = loc.getBlockZ() + radius;
        final int minX = loc.getBlockX() - radius;
        final int minY = loc.getBlockY() - radius;
        final int minZ = loc.getBlockZ() - radius;
        int updated = 0;

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    final Location cursor = new Location(loc.getWorld(), x, y, z);

                    if (!cursor.getBlock().getType().equals(Material.END_GATEWAY)) {
                        continue;
                    }

                    final Optional<WarpGateway> existing = manager.getGateway(cursor.getBlock());

                    if (existing.isPresent()) {
                        continue;
                    }

                    final WarpGateway gateway = new WarpGateway(manager.getService(), UUID.randomUUID(), destinationName, new BLocatable(cursor.getBlock()));
                    manager.getGatewayRepository().add(gateway);
                    manager.saveGateway(gateway);
                    updated += 1;
                }
            }
        }

        if (updated == 0) {
            promise.reject("No gateway blocks found");
            return;
        }

        promise.resolve(updated);
    }

    @Override
    public void deleteGateway(Player player, String destinationName, Promise promise) {
        final boolean removed = manager.getGatewayRepository().removeIf(g -> g.getDestinationName().equalsIgnoreCase(destinationName));

        if (!removed) {
            promise.reject("No gateways found");
            return;
        }

        promise.resolve();
    }
}
