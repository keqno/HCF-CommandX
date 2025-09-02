package gg.hcfactions.cx.modules.world;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public final class EXPBonusModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private double fortuneModifier;
    private double lootingModifier;

    public EXPBonusModule(CXService service) {
        this.service = service;
        this.key = "world.exp_bonus.";
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (isEnabled()) {
            getPlugin().registerListener(this);
        }
    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.fortuneModifier = conf.getDouble(getKey() + "modifiers.fortune");
        this.lootingModifier = conf.getDouble(getKey() + "modifiers.looting");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (fortuneModifier <= 1.0) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (!hand.containsEnchantment(Enchantment.FORTUNE)) {
            return;
        }

        final int multiplier = hand.getEnchantmentLevel(Enchantment.FORTUNE) + 1;
        final int xp = event.getExpToDrop();

        event.setExpToDrop((int)(Math.round(xp * (multiplier * fortuneModifier))));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (lootingModifier <= 1.0) {
            return;
        }

        final LivingEntity entity = event.getEntity();
        final Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        final ItemStack hand = killer.getInventory().getItemInMainHand();

        if (!hand.containsEnchantment(Enchantment.LOOTING)) {
            return;
        }

        final int multiplier = hand.getEnchantmentLevel(Enchantment.LOOTING) + 1;
        final int xp = event.getDroppedExp();

        event.setDroppedExp((int)(Math.round(xp * (multiplier * lootingModifier))));
    }
}
