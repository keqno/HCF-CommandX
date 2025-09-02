package gg.hcfactions.cx.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class EnchantLimitApplyEvent extends Event implements Cancellable {
    @Getter public static final HandlerList handlerList = new HandlerList();
    @Getter public final Player player;
    @Getter public final ItemStack item;
    @Getter public final Map<Enchantment, Integer> limitedEnchantments;
    @Getter @Setter public boolean cancelled;

    public EnchantLimitApplyEvent(Player player, ItemStack item, Map<Enchantment, Integer> limitedEnchantments) {
        this.player = player;
        this.item = item;
        this.limitedEnchantments = limitedEnchantments;
        this.cancelled = false;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
