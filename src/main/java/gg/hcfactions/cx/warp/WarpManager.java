package gg.hcfactions.cx.warp;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.warp.impl.Warp;
import gg.hcfactions.cx.warp.impl.WarpExecutor;
import gg.hcfactions.cx.warp.impl.WarpGateway;
import gg.hcfactions.libs.base.consumer.UnsafePromise;
import gg.hcfactions.libs.bukkit.location.impl.BLocatable;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class WarpManager {
    public static final String WARP_SIGN_IDENTIFIER = "[Warp]";
    public static final String FORMATTED_WARP_SIGN_IDENTIFIER = ChatColor.DARK_GRAY + "[" + ChatColor.BLUE + "Warp" + ChatColor.DARK_GRAY + "]";

    @Getter public final CXService service;
    @Getter public final WarpExecutor executor;
    @Getter public List<Warp> warpRepository;
    @Getter public List<WarpGateway> gatewayRepository;

    public WarpManager(CXService service) {
        this.service = service;
        this.executor = new WarpExecutor(this);
        this.warpRepository = Lists.newArrayList();
        this.gatewayRepository = Lists.newArrayList();
    }

    public Optional<Warp> getWarp(String warpName) {
        return warpRepository.stream().filter(w -> w.getName().equalsIgnoreCase(warpName)).findFirst();
    }

    public void getWarpGateway(Block block, UnsafePromise<WarpGateway> promise) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        String world = block.getWorld().getName();

        new Scheduler(service.getPlugin()).async(() -> {
            Optional<WarpGateway> query = gatewayRepository.stream().filter(w ->
                    w.getBlock().getX() == x
                            && w.getBlock().getY() == y
                            && w.getBlock().getZ() == z
                            && w.getBlock().getWorldName().equalsIgnoreCase(world)
            ).findFirst();

            new Scheduler(service.getPlugin()).sync(() -> {
                if (query.isEmpty()) {
                    return;
                }

                promise.accept(query.get());
            }).run();
        }).run();
    }

    public Optional<WarpGateway> getGateway(Block block) {
        return gatewayRepository
                .stream()
                .filter(w -> w.getBlock().getX() == block.getX() && w.getBlock().getY() == block.getY() && w.getBlock().getZ() == block.getZ() && w.getBlock().getWorldName().equalsIgnoreCase(block.getWorld().getName()))
                .findFirst();
    }

    public List<WarpGateway> getGatewayByDestination(String destName) {
        return gatewayRepository
                .stream().filter(gateway -> gateway.getDestinationName().equalsIgnoreCase(destName)).collect(Collectors.toList());
    }

    public void loadWarps() {
        if (!warpRepository.isEmpty()) {
            warpRepository.clear();
        }

        final YamlConfiguration conf = service.getPlugin().loadConfiguration("warps");

        if (conf.getConfigurationSection("data") == null) {
            service.getPlugin().getAresLogger().warn("could not find data in warps.yml. skipping...");
            return;
        }

        for (String warpName : Objects.requireNonNull(conf.getConfigurationSection("data")).getKeys(false)) {
            final String key = "data." + warpName + ".";
            final double x = conf.getDouble(key + "x");
            final double y = conf.getDouble(key + "y");
            final double z = conf.getDouble(key + "z");
            final float yaw = (float)conf.getDouble(key + "yaw");
            final float pitch = (float)conf.getDouble(key + "pitch");
            final String worldName = conf.getString(key + "world");

            final Warp warp = new Warp(x, y, z, yaw, pitch, worldName, warpName);

            warpRepository.add(warp);
        }

        service.getPlugin().getAresLogger().info("loaded " + warpRepository.size() + " warps");
    }

    public void loadGateways() {
        if (!gatewayRepository.isEmpty()) {
            gatewayRepository.clear();
        }

        final YamlConfiguration conf = service.getPlugin().loadConfiguration("gateways");

        if (conf.getConfigurationSection("data") == null) {
            service.getPlugin().getAresLogger().warn("could not find data in gateways.yml. skipping...");
            return;
        }

        for (String gatewayId : Objects.requireNonNull(conf.getConfigurationSection("data")).getKeys(false)) {
            final String key = "data." + gatewayId + ".";
            final double x = conf.getDouble(key + "x");
            final double y = conf.getDouble(key + "y");
            final double z = conf.getDouble(key + "z");
            final String worldName = conf.getString(key + "world");
            final String destinationName = conf.getString(key + "destination");

            final Optional<Warp> warpQuery = warpRepository.stream().filter(w -> w.getName().equalsIgnoreCase(destinationName)).findFirst();

            if (warpQuery.isEmpty()) {
                service.getPlugin().getAresLogger().warn("failed to link gateway, warp: " + destinationName + " was not found");
                continue;
            }

            if (gatewayRepository.stream().anyMatch(g -> g.getBlock().getX() == x && g.getBlock().getY() == y && g.getBlock().getZ() == z && g.getBlock().getWorldName().equalsIgnoreCase(worldName))) {
                service.getPlugin().getAresLogger().warn("found duplicate warp gateway for " + destinationName);
                continue;
            }

            final WarpGateway gateway = new WarpGateway(service, UUID.fromString(gatewayId), destinationName, new BLocatable(worldName, x, y, z));
            gatewayRepository.add(gateway);
        }

        service.getPlugin().getAresLogger().info("loaded " + gatewayRepository.size() + " warp gateway blocks");
    }

    public void saveWarp(Warp warp) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("warps");
        final String key = "data." + warp.getName() + ".";

        conf.set(key + "x", warp.getX());
        conf.set(key + "y", warp.getY());
        conf.set(key + "z", warp.getZ());
        conf.set(key + "yaw", warp.getYaw());
        conf.set(key + "pitch", warp.getPitch());
        conf.set(key + "world", warp.getWorldName());

        service.getPlugin().saveConfiguration("warps", conf);
    }

    public void saveGateway(WarpGateway gateway) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("gateways");
        final String key = "data." + gateway.getUniqueId().toString() + ".";

        conf.set(key + "destination", gateway.getDestinationName());
        conf.set(key + "x", gateway.getBlock().getX());
        conf.set(key + "y", gateway.getBlock().getY());
        conf.set(key + "z", gateway.getBlock().getZ());
        conf.set(key + "world", gateway.getBlock().getWorldName());

        service.getPlugin().saveConfiguration("gateways", conf);
    }

    public void deleteWarp(Warp warp) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("warps");
        conf.set("data." + warp.getName(), null);
        service.getPlugin().saveConfiguration("warps", conf);
    }

    public void deleteGateway(WarpGateway gateway) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("gateways");
        conf.set("data." + gateway.getUniqueId().toString(), null);
        service.getPlugin().saveConfiguration("gateways", conf);
    }
}
