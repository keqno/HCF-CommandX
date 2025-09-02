package gg.hcfactions.cx.modules.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.event.PreMobstackEvent;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class MobstackModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private String nameplatePrefix;
    private int stackInterval;
    private int stackRadius;
    private int maxStackSize;
    private int breedingCooldown;
    private final List<EntityType> stackableTypes;
    private final List<UUID> stackSkip;
    private final Map<UUID, Long> breedCooldowns;

    private BukkitTask stackTickingTask;

    private final ImmutableMap<EntityType, List<Material>> BREED_MATS = ImmutableMap.<EntityType, List<Material>>builder()
            .put(EntityType.COW, ImmutableList.of(Material.WHEAT))
            .put(EntityType.SHEEP, ImmutableList.of(Material.WHEAT))
            .put(EntityType.MOOSHROOM, ImmutableList.of(Material.WHEAT))
            .put(EntityType.HORSE, ImmutableList.of(Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT))
            .put(EntityType.DONKEY, ImmutableList.of(Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_CARROT))
            .put(EntityType.CHICKEN, ImmutableList.of(Material.MELON_SEEDS, Material.PUMPKIN_SEEDS, Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS))
            .put(EntityType.PIG, ImmutableList.of(Material.CARROT, Material.POTATO, Material.BEETROOT))
            .put(EntityType.WOLF, ImmutableList.of(Material.PORKCHOP, Material.COOKED_PORKCHOP, Material.BEEF, Material.COOKED_BEEF, Material.CHICKEN, Material.COOKED_CHICKEN, Material.MUTTON, Material.COOKED_MUTTON, Material.ROTTEN_FLESH, Material.RABBIT, Material.COOKED_RABBIT))
            .put(EntityType.OCELOT, ImmutableList.of(Material.COD, Material.COOKED_COD, Material.COOKED_SALMON, Material.SALMON))
            .put(EntityType.CAT, ImmutableList.of(Material.COD, Material.COOKED_COD, Material.COOKED_SALMON, Material.SALMON))
            .put(EntityType.RABBIT, ImmutableList.of(Material.DANDELION, Material.CARROT, Material.GOLDEN_CARROT))
            .put(EntityType.LLAMA, ImmutableList.of(Material.HAY_BLOCK))
            .put(EntityType.TRADER_LLAMA, ImmutableList.of(Material.HAY_BLOCK))
            .put(EntityType.TURTLE, ImmutableList.of(Material.SEAGRASS))
            .put(EntityType.PANDA, ImmutableList.of(Material.BAMBOO))
            .put(EntityType.STRIDER, ImmutableList.of(Material.WARPED_FUNGUS))
            .put(EntityType.HOGLIN, ImmutableList.of(Material.CRIMSON_FUNGUS))
            .put(EntityType.AXOLOTL, ImmutableList.of(Material.TROPICAL_FISH_BUCKET))
            .put(EntityType.FOX, ImmutableList.of(Material.SWEET_BERRIES, Material.GLOW_BERRIES))
            .put(EntityType.FROG, ImmutableList.of(Material.SLIME_BALL))
            .put(EntityType.CAMEL, ImmutableList.of(Material.CACTUS))
            .put(EntityType.SNIFFER, ImmutableList.of(Material.TORCHFLOWER_SEEDS))
            .build();

    public MobstackModule(CXService service) {
        this.service = service;
        this.key = "world.mobstacking.";
        this.stackableTypes = Lists.newArrayList();
        this.stackSkip = Lists.newArrayList();
        this.breedCooldowns = Maps.newConcurrentMap();
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        getPlugin().registerListener(this);

        startStackTickingTask();
    }

    @Override
    public void onDisable() {
        if (stackTickingTask != null) {
            stackTickingTask.cancel();
            stackTickingTask = null;
        }

        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();

        stackTickingTask.cancel();
        stackTickingTask = null;
        startStackTickingTask();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.stackInterval = conf.getInt(getKey() + "stack_interval");
        this.stackRadius = conf.getInt(getKey() + "stack_radius");
        this.maxStackSize = conf.getInt(getKey() + "max_stack_size");
        this.breedingCooldown = conf.getInt(getKey() + "breeding_cooldown");
        this.nameplatePrefix = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(conf.getString(getKey() + "stack_nameplate_prefix")));

        stackableTypes.clear();

        for (String entityName : conf.getStringList(getKey() + "types")) {
            try {
                stackableTypes.add(EntityType.valueOf(entityName));
            } catch (IllegalArgumentException e) {
                getPlugin().getAresLogger().error("bad entity type: " + entityName);
            }
        }

        getPlugin().getAresLogger().info("loaded " + stackableTypes.size() + " stackable types");
    }

    private void startStackTickingTask() {
        stackTickingTask = new Scheduler(getPlugin()).sync(() -> {
            for (World world : Bukkit.getWorlds()) {
                for (LivingEntity livingEntity : world.getLivingEntities()) {
                    if (!stackableTypes.contains(livingEntity.getType())) {
                        continue;
                    }

                    if (stackSkip.contains(livingEntity.getUniqueId())) {
                        continue;
                    }

                    if (livingEntity.getHealth() <= 0.0 || livingEntity.isDead()) {
                        continue;
                    }

                    if (livingEntity.isLeashed()) {
                        continue;
                    }

                    if (livingEntity.getCustomName() != null && !livingEntity.getCustomName().startsWith(nameplatePrefix)) {
                        continue;
                    }

                    final List<LivingEntity> toMerge = Lists.newArrayList();
                    toMerge.add(livingEntity);

                    // TODO: Make configurable
                    for (Entity nearby : livingEntity.getNearbyEntities(stackRadius, stackRadius, stackRadius)) {
                        if (!(nearby instanceof final LivingEntity nearbyLivingEntity)) {
                            continue;
                        }

                        if (!nearby.getType().equals(livingEntity.getType())) {
                            continue;
                        }

                        if (stackSkip.contains(nearby.getUniqueId())) {
                            continue;
                        }

                        if (livingEntity instanceof final Ageable entityA) {
                            final Ageable entityB = (Ageable)nearbyLivingEntity;

                            if (entityA.isAdult() != entityB.isAdult()) {
                                continue;
                            }
                        }

                        if (livingEntity instanceof final Colorable entityA) {
                            final Colorable entityB = (Colorable)nearbyLivingEntity;

                            if (entityA.getColor() != entityB.getColor()) {
                                continue;
                            }
                        }

                        if (nearbyLivingEntity.isLeashed()) {
                            continue;
                        }

                        if (nearbyLivingEntity.getCustomName() != null && !nearbyLivingEntity.getCustomName().startsWith(nameplatePrefix)) {
                            if (toMerge.contains(nearbyLivingEntity)) {
                                continue;
                            }
                        }

                        final PreMobstackEvent mobstackEvent = new PreMobstackEvent(livingEntity, nearbyLivingEntity);
                        Bukkit.getPluginManager().callEvent(mobstackEvent);

                        if (mobstackEvent.isCancelled()) {
                            continue;
                        }

                        toMerge.add(nearbyLivingEntity);
                    }

                    if (toMerge.size() > 1) {
                        stack(toMerge);
                    }
                }
            }

            stackSkip.clear();
        }).repeat(stackInterval * 20L, stackInterval * 20L).run();
    }

    /**
     * Performs a stack for all provided Living Entity
     * @param entities Living Entity
     */
    private void stack(List<LivingEntity> entities) {
        if (entities.size() <= 1) {
            return;
        }

        entities.sort((o1, o2) -> {
            final int stackA = getStackSize(o1);
            final int stackB = getStackSize(o2);
            return stackA - stackB;
        });

        Collections.reverse(entities);

        final LivingEntity host = entities.get(0);
        int size = getStackSize(host);

        for (LivingEntity merged : entities) {
            if (merged.getUniqueId().equals(host.getUniqueId())) {
                continue;
            }

            final int mergedSize = getStackSize(merged);

            if ((mergedSize + size) > maxStackSize) {
                continue;
            }

            size += getStackSize(merged);
            stackSkip.add(merged.getUniqueId());
            merged.remove();
        }

        host.setCustomName(nameplatePrefix + size);
        host.setCustomNameVisible(true);
        stackSkip.add(host.getUniqueId());
    }

    /**
     * Returns the size of the stack for the provided Living Entity
     *
     * Returns as 1 if entity is not a stack
     * @param entity Living Entity
     * @return Stack Size
     */
    private int getStackSize(LivingEntity entity) {
        if (entity.getCustomName() == null || !entity.getCustomName().startsWith(nameplatePrefix)) {
            return 1;
        }

        try {
            return Integer.parseInt(entity.getCustomName().replace(nameplatePrefix, ""));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    /**
     * Returns true if the provided Living Entity is a stack
     * @param entity Living Entity
     * @return True if a stack
     */
    private boolean isStacked(LivingEntity entity) {
        return entity.getCustomName() != null && entity.getCustomName().startsWith(nameplatePrefix);
    }

    /**
     * Returns the cooldown for the provided player to breed stacked mobs
     * @param player Player
     * @return Time in millis before being able to breed mobs again
     */
    private long getBreedingCooldown(Player player) {
        return breedCooldowns.getOrDefault(player.getUniqueId(), 0L);
    }

    /**
     * Handles reducing stack size on entity death
     * @param event EntityDeathEvent
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        final int stackSize = getStackSize(entity);

        if (!isStacked(entity)) {
            return;
        }

        final LivingEntity clone = (LivingEntity)entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

        if ((stackSize - 1) > 1) {
            clone.setCustomName(nameplatePrefix + (stackSize - 1));
            clone.setCustomNameVisible(true);
        }

        clone.setNoDamageTicks(2);
        clone.setFireTicks(entity.getFireTicks());
        clone.setRemainingAir(entity.getRemainingAir());
        clone.setVelocity(entity.getVelocity());
        clone.setTicksLived(entity.getTicksLived());

        entity.getActivePotionEffects().forEach(clone::addPotionEffect);

        if (entity instanceof final Colorable entityA) {
            final Colorable entityB = (Colorable)clone;
            entityB.setColor(entityA.getColor());
        }

        if (entity instanceof final Ageable entityA) {
            final Ageable entityB = (Ageable)clone;
            entityB.setAge(entityA.getAge());
        }

        stackSkip.add(entity.getUniqueId());
    }

    /**
     * Handles breeding stacked mobs
     * @param event PlayerInteractEntityEvent
     */
    @EventHandler
    public void onBreed(PlayerInteractEntityEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (!(event.getRightClicked() instanceof final LivingEntity entity)) {
            return;
        }

        final Player player = event.getPlayer();
        final UUID uniqueId = player.getUniqueId();
        final ItemStack hand = player.getInventory().getItemInMainHand();

        if (!BREED_MATS.containsKey(entity.getType())) {
            return;
        }

        if (!Objects.requireNonNull(BREED_MATS.get(entity.getType())).contains(hand.getType())) {
            return;
        }

        if (!isStacked(entity)) {
            return;
        }

        event.setCancelled(true);

        final int stackSize = getStackSize(entity);

        if (stackSize <= 1) {
            player.sendMessage(ChatColor.RED + "Stack size must be at least 2 to begin breeding");
            return;
        }

        if (getBreedingCooldown(player) > Time.now()) {
            player.sendMessage(
                    ChatColor.RED + "You can not breed animals for another " +
                            ChatColor.RED + "" + ChatColor.BOLD + Time.convertToDecimal(getBreedingCooldown(player) - Time.now()) +
                            ChatColor.RED + "s");

            return;
        }

        final LivingEntity baby = (LivingEntity)entity.getWorld().spawnEntity(entity.getLocation(), entity.getType());

        if (baby instanceof Ageable) {
            ((Ageable)baby).setBaby();
        }

        if (baby instanceof final Colorable colorable) {
            final Colorable parent = (Colorable)entity;
            colorable.setColor(parent.getColor());
        }

        if (hand.getAmount() == 1) {
            player.getInventory().removeItem(hand);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }

        breedCooldowns.put(player.getUniqueId(), (Time.now() + (breedingCooldown * 1000L)));
        new Scheduler(getPlugin()).sync(() -> breedCooldowns.remove(uniqueId)).delay(breedingCooldown * 20L).run();
    }
}
