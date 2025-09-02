package gg.hcfactions.cx.modules.player.combat;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;

public final class PotionPrecisionModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private double triggerThreshold;
    private double modifierAmount;

    public PotionPrecisionModule(CXService service) {
        this.service = service;
        this.key = "combat.potion_precision.";
        this.enabled = false;
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
        PotionSplashEvent.getHandlerList().unregister(this);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        enabled = getConfig().getBoolean(getKey() + "enabled");
        triggerThreshold = getConfig().getDouble(getKey() + "threshold");
        modifierAmount = getConfig().getDouble(getKey() + "modifier");
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPotionSplash(PotionSplashEvent event) {
        if (!isEnabled() || event.isCancelled()) {
            return;
        }

        if (!(event.getPotion().getShooter() instanceof final Player player)) {
            return;
        }

        event.getAffectedEntities().forEach(affected -> {
            if (affected.getUniqueId().equals(player.getUniqueId()) && player.isSprinting()) {
                final double intensity = event.getIntensity(affected);

                if (intensity > triggerThreshold) {
                    final double newIntensity = Math.min(intensity + modifierAmount, 1.0);
                    event.setIntensity(affected, newIntensity);
                }
            }
        });
    }
}
