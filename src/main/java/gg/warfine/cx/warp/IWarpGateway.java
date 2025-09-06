package gg.warfine.cx.warp;

import gg.warfine.libs.bukkit.location.impl.BLocatable;

import java.util.UUID;

public interface IWarpGateway {
    UUID uniqueId();
    String destinationName();
    BLocatable block();
}
