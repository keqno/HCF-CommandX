package gg.hcfactions.cx.modules.player.items;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public final class ItemVelocityModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private double pearlVelocity;
    private double potionVelocity;

    public ItemVelocityModule(CXService service) {
        this.service = service;
        this.key = "items.velocity.";
    }

    @Override
    public void onEnable() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        pearlVelocity = conf.getDouble(getKey() + "enderpearl");
        potionVelocity = conf.getDouble(getKey() + "potions");

        if (isEnabled()) {
            getPlugin().registerListener(this);
        }
    }

    @Override
    public void onDisable() {
        ProjectileLaunchEvent.getHandlerList().unregister(this);

        this.enabled = false;
    }

    @Override
    public void onReload() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        pearlVelocity = conf.getDouble(getKey() + "enderpearl");
        potionVelocity = conf.getDouble(getKey() + "potions");
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof final Player player)) {
            return;
        }

        if (projectile instanceof EnderPearl) {
            projectile.setVelocity(player.getLocation().getDirection().normalize().multiply(pearlVelocity));
            return;
        }

        if (projectile instanceof ThrownPotion) {
            projectile.setVelocity(player.getLocation().getDirection().normalize().multiply(potionVelocity));
        }
    }
}
