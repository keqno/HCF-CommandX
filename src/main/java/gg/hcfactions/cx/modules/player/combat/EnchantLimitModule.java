package gg.hcfactions.cx.modules.player.combat;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.event.EnchantLimitApplyEvent;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.utils.Enchants;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class EnchantLimitModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;
    @Getter public final Map<Enchantment, Integer> enchantLimits;

    private final Random random;

    public EnchantLimitModule(CXService service) {
        this.service = service;
        this.random = new Random();
        this.key = "combat.enchant_limits.";
        this.enchantLimits = Maps.newHashMap();
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
        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();

        enchantLimits.clear();

        if (conf.getConfigurationSection(getKey() + "limits") == null) {
            getPlugin().getAresLogger().warn("Could not find any Enchantment Limits. Skipping...");
            return;
        }

        this.enabled = conf.getBoolean(getKey() + "enabled");

        for (String enchantmentName : Objects.requireNonNull(conf.getConfigurationSection(getKey() + "limits")).getKeys(false)) {
            final Enchantment enchantment = Enchants.getEnchantment(enchantmentName);
            final int maxLevel = conf.getInt(getKey() + "limits." + enchantmentName);

            if (enchantment == null) {
                getPlugin().getAresLogger().error("bad enchantment name: {}", enchantmentName);
                continue;
            }

            enchantLimits.put(enchantment, maxLevel);
        }

        getPlugin().getAresLogger().info("loaded {} enchantment limits", enchantLimits.size());
    }

    /**
     * Returns the max level for the provided enchantment
     * @param enchantment Enchantment
     * @return Max Enchantment Level
     */
    public int getMaxEnchantmentLevel(Enchantment enchantment) {
        return enchantLimits.getOrDefault(enchantment, -1);
    }

    /**
     * Returns a random enchantment and enchantment level
     * @param item ItemStack to check
     * @return Immutable Singleton Map of Enchantment and Enchantment Level
     */
    private Map<Enchantment, Integer> getRandomEnchantment(ItemStack item) {
        final List<Enchantment> validEnchantments = Lists.newArrayList();

        for (Enchantment enc : Enchants.getAllEnchantments()) {
            if (getMaxEnchantmentLevel(enc) <= 0) {
                continue;
            }

            if (enc.canEnchantItem(item)) {
                validEnchantments.add(enc);
            }
        }

        // catch-all
        if (validEnchantments.isEmpty()) {
            return Collections.singletonMap(Enchantment.UNBREAKING, 0);
        }

        final int pos = Math.abs(random.nextInt(validEnchantments.size()));
        final int randomLevel = Math.abs(random.nextInt(3));

        final Enchantment randomEnchantment = Enchants.getAllEnchantments().get(pos);
        if (randomEnchantment == null) {
            return null;
        }

        return Collections.singletonMap(randomEnchantment, enchantLimits.getOrDefault(randomEnchantment, randomLevel));
    }

    /**
     * Updates enchantments for the provided player and item
     * @param player Player
     * @param item ItemStack to revert
     */
    private void updateEnchantments(Player player, ItemStack item) {
        if (item.getEnchantments().isEmpty()) {
            return;
        }

        final List<Enchantment> toRemove = Lists.newArrayList();
        final Map<Enchantment, Integer> toLower = Maps.newHashMap();

        for (Enchantment enchantment : item.getEnchantments().keySet()) {
            final int level = item.getEnchantmentLevel(enchantment);
            final int maxEnchantmentLevel = getMaxEnchantmentLevel(enchantment);

            if (maxEnchantmentLevel == -1) {
                continue;
            }

            if (maxEnchantmentLevel == 0) {
                toRemove.add(enchantment);
                continue;
            }

            if (maxEnchantmentLevel < level) {
                toLower.put(enchantment, maxEnchantmentLevel);
            }
        }

        final Map<Enchantment, Integer> eventValues = Maps.newHashMap();
        eventValues.putAll(toLower);
        toRemove.forEach(removed -> eventValues.put(removed, -1));

        final EnchantLimitApplyEvent applyEvent = new EnchantLimitApplyEvent(player, item, eventValues);
        Bukkit.getPluginManager().callEvent(applyEvent);

        if (applyEvent.isCancelled()) {
            return;
        }

        applyEvent.getLimitedEnchantments().forEach((enchantment, newValue) -> {
            if (newValue == -1) {
                item.removeEnchantment(enchantment);
                player.sendMessage(ChatColor.DARK_RED + "Removed Enchantment" + ChatColor.RED + ": " +
                        ChatColor.WHITE + StringUtils.capitalize(enchantment.getKey().getKey().toLowerCase().replace("_", " ")));
            } else {
                item.addUnsafeEnchantment(enchantment, newValue);

                player.sendMessage(ChatColor.BLUE + "Updated Enchantment" + ChatColor.AQUA + ": " +
                        ChatColor.WHITE + StringUtils.capitalize(enchantment.getKey().getKey().toLowerCase().replace("_", " ")));
            }
        });
    }

    /**
     * Handles updating enchantments on Entity attack
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.getEntity() instanceof final Player player) {
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor == null || armor.getType().equals(Material.AIR)) {
                    continue;
                }

                updateEnchantments(player, armor);
            }
        }

        if (event.getDamager() instanceof final Player player) {
            final ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType().equals(Material.AIR)) {
                return;
            }

            updateEnchantments(player, item);
        }
    }

    /**
     * Handles reverting enchantments on bow fire
     * @param event EntityShootBowEvent
     */
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        final ItemStack item = event.getBow();

        if (item == null || item.getType().equals(Material.AIR)) {
            return;
        }

        updateEnchantments(player, item);
    }

    /**
     * Removes illegal enchantments from the Enchantment Table
     * @param event PrepareItemEnchantEvent
     */
    @EventHandler
    public void onPrepEnchantItem(PrepareItemEnchantEvent event) {
        if (!isEnabled()) {
            return;
        }

        final ItemStack item = event.getItem();
        final EnchantmentOffer[] offers = event.getOffers();

        for (EnchantmentOffer offer : offers) {
            if (offer == null) {
                continue;
            }

            final int maxEnchantmentLimit = getMaxEnchantmentLevel(offer.getEnchantment());

            if (maxEnchantmentLimit == -1) {
                continue;
            }

            if (maxEnchantmentLimit == 0) {
                final Map<Enchantment, Integer> replacement = getRandomEnchantment(item);

                Objects.requireNonNull(replacement).forEach((enchantment, level) -> {
                    if (level > 0) {
                        offer.setEnchantment(enchantment);
                        offer.setEnchantmentLevel(level);
                    }
                });

                continue;
            }

            if (offer.getEnchantmentLevel() > maxEnchantmentLimit) {
                offer.setEnchantmentLevel(maxEnchantmentLimit);
            }
        }
    }

    /**
     * Handles removing misc enchantments that are illegal
     * @param event EnchantItemEvent
     */
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        if (!isEnabled()) {
            return;
        }

        final List<Enchantment> toRemove = Lists.newArrayList();

        for (Enchantment enchantmentToAdd : event.getEnchantsToAdd().keySet()) {
            int currentLevel = event.getEnchantsToAdd().get(enchantmentToAdd);
            final int maxLevel = getMaxEnchantmentLevel(enchantmentToAdd);

            if (maxLevel == -1) {
                continue;
            }

            if (maxLevel == 0) {
                toRemove.add(enchantmentToAdd);
                continue;
            }

            if (currentLevel > maxLevel) {
                currentLevel = maxLevel;
            }

            event.getEnchantsToAdd().put(enchantmentToAdd, currentLevel);
        }

        if (!toRemove.isEmpty()) {
            toRemove.forEach(removedEnchantment -> event.getEnchantsToAdd().remove(removedEnchantment));
        }
    }
}
