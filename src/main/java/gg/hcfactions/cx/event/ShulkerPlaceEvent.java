package gg.hcfactions.cx.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;
import org.jetbrains.annotations.NotNull;

public final class ShulkerPlaceEvent extends BlockEvent implements Cancellable {
    @Getter public static final HandlerList handlerList = new HandlerList();

    @Getter public Player player;
    @Getter @Setter public int duration;
    @Getter @Setter public boolean cancelled;

    public ShulkerPlaceEvent(Player who, Block block) {
        super(block);
        this.player = who;
        this.duration = 30;
        this.cancelled = false;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
