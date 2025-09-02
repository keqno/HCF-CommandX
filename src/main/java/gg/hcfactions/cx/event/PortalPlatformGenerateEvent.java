
package gg.hcfactions.cx.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PortalPlatformGenerateEvent extends Event implements Cancellable {
    @Getter public static final HandlerList handlerList = new HandlerList();
    @Getter public final Location origin;
    @Getter public final List<Block> blockList;
    @Getter @Setter public boolean cancelled;

    public PortalPlatformGenerateEvent(Location origin, List<Block> blockList) {
        super(false);
        this.origin = origin;
        this.blockList = blockList;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
