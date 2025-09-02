package gg.hcfactions.cx.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PreMobstackEvent extends Event implements Cancellable {
    @Getter public static final HandlerList handlerList = new HandlerList();
    @Getter public final LivingEntity originEntity;
    @Getter public final LivingEntity mergingEntity;
    @Getter @Setter public boolean cancelled;

    public PreMobstackEvent(LivingEntity originEntity, LivingEntity mergingEntity) {
        this.originEntity = originEntity;
        this.mergingEntity = mergingEntity;
        this.cancelled = false;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
