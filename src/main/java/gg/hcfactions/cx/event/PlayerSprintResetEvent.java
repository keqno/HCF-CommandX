package gg.hcfactions.cx.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public final class PlayerSprintResetEvent extends PlayerEvent {
    @Getter public static final HandlerList handlerList = new HandlerList();

    public PlayerSprintResetEvent(Player who) {
        super(who);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
