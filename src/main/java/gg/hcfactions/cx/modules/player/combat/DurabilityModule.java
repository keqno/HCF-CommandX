package gg.hcfactions.cx.modules.player.combat;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public final class DurabilityModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private int reduction;

    public DurabilityModule(CXService service) {
        this.service = service;
        this.key = "combat.durability.";
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        getPlugin().registerListener(this);
    }

    @Override
    public void onDisable() {
        PlayerItemDamageEvent.getHandlerList().unregister(this);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.reduction = conf.getInt(getKey() + "reduction");
    }

    private boolean isArmor(ItemStack item) {
        final Material type = item.getType();

        if (type.name().endsWith("_HELMET")) {
            return true;
        }

        if (type.name().endsWith("_CHESTPLATE")) {
            return true;
        }

        if (type.name().endsWith("_LEGGINGS")) {
            return true;
        }

        if (type.name().endsWith("_BOOTS")) {
            return true;
        }

        return false;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onItemDurability(PlayerItemDamageEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        if (!isArmor(event.getItem())) {
            return;
        }

        if (event.getDamage() <= 1) {
            return;
        }

        final int reduced = event.getDamage() / reduction;
        event.setDamage(reduced);
    }
}
