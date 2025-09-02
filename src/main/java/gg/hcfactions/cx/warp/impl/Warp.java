package gg.hcfactions.cx.warp.impl;

import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Objects;

public final class Warp extends PLocatable {
    @Getter public final String name;

    public Warp(Player player, String name) {
        this.name = name;
        this.x = player.getLocation().getX();
        this.y = player.getLocation().getY();
        this.z = player.getLocation().getZ();
        this.yaw = player.getLocation().getYaw();
        this.pitch = player.getLocation().getPitch();
        this.worldName = Objects.requireNonNull(player.getLocation().getWorld()).getName();
    }

    public Warp(double x, double y, double z, float yaw, float pitch, String worldName, String warpName) {
        this.name = warpName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldName = worldName;
    }

    public void teleport(Player player) {
        player.teleport(getBukkitLocation());
    }
}
