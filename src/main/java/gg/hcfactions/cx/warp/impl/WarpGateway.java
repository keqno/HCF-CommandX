package gg.hcfactions.cx.warp.impl;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.warp.IWarpGateway;
import gg.hcfactions.libs.bukkit.location.impl.BLocatable;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

public record WarpGateway(@Getter CXService service,
                          @Getter UUID uniqueId,
                          @Getter String destinationName,
                          @Getter BLocatable block) implements IWarpGateway {

    public void teleport(Player player) {
        service.getWarpManager().getWarp(destinationName).ifPresent(warp -> warp.teleport(player));
    }
}
