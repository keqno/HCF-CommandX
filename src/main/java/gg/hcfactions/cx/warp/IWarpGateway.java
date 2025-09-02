package gg.hcfactions.cx.warp;

import gg.hcfactions.libs.bukkit.location.impl.BLocatable;

import java.util.UUID;

public interface IWarpGateway {
    UUID uniqueId();
    String destinationName();
    BLocatable block();
}
