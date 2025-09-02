package gg.hcfactions.cx.modules.world;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.event.PortalPlatformGenerateEvent;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public final class WorldModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean disablePistonBreakingDoors;
    private boolean disableEnderchests;
    private boolean disableEntityBlockChanges;
    private boolean disableExplosiveExploits;
    private boolean disableEnterBedMessage;
    private boolean disableEggSpawners;
    private boolean nerfExplosiveDamage;
    private double nerfExplosiveDamageAmount;
    private boolean witherSpawnCooldown;
    private boolean allowPearlingThroughTraps;
    private boolean generateNetherPortalPlatforms;
    private long nextWitherSpawn;
    private final Set<EntityType> disabledEntities;
    private final Set<EntityType> disabledSpawnerBlockBreaks;

    public WorldModule(CXService service) {
        this.service = service;
        this.key = "world.general.";
        this.nextWitherSpawn = Time.now();
        this.disabledEntities = Sets.newHashSet();
        this.disabledSpawnerBlockBreaks = Sets.newHashSet();
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
        disabledEntities.clear();
        disabledSpawnerBlockBreaks.clear();

        this.enabled = false;
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        enabled = conf.getBoolean(getKey() + "enabled");
        disablePistonBreakingDoors = conf.getBoolean(getKey() + "disable_piston_breaking_doors");
        disableEnderchests = conf.getBoolean(getKey() + "disable_enderchests");
        allowPearlingThroughTraps = conf.getBoolean(getKey() + "allow_pearling_through_traps");
        disableEntityBlockChanges = conf.getBoolean(getKey() + "disable_entity_block_changes");
        disableExplosiveExploits = conf.getBoolean(getKey() + "disable_explosive_exploits");
        disableEnterBedMessage = conf.getBoolean(getKey() + "disable_enter_bed_message");
        disableEggSpawners = conf.getBoolean(getKey() + "disable_egg_spawners");
        witherSpawnCooldown = conf.getBoolean(getKey() + "wither_spawn_cooldown");
        generateNetherPortalPlatforms = conf.getBoolean(getKey() + "generate_nether_portal_platforms");
        nerfExplosiveDamage = conf.getBoolean(getKey() + "nerf_explosive_damage.enabled");
        nerfExplosiveDamageAmount = conf.getDouble(getKey() + "nerf_explosive_damage.amount");

        final List<String> disabledEntityNames = conf.getStringList(getKey() + "disabled_entities");
        final List<String> disabledEntitySpawnerBreakNames = conf.getStringList(getKey() + "disabled_spawner_break");

        disabledEntities.clear();
        disabledSpawnerBlockBreaks.clear();

        for (String entityName : disabledEntityNames) {
            try {
                final EntityType type = EntityType.valueOf(entityName);
                disabledEntities.add(type);
            } catch (IllegalArgumentException e) {
                getPlugin().getAresLogger().error("bad entity type: " + entityName);
            }
        }

        for (String entityName : disabledEntitySpawnerBreakNames) {
            try {
                final EntityType type = EntityType.valueOf(entityName);
                disabledSpawnerBlockBreaks.add(type);
            } catch (IllegalArgumentException e) {
                getPlugin().getAresLogger().error("bad entity type: " + entityName);
            }
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerUseMonsterEgg(PlayerInteractEvent event) {
        if (!event.useInteractedBlock().equals(Event.Result.ALLOW) || !disableEggSpawners) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();
        final Block block = event.getClickedBlock();
        final Action action = event.getAction();

        if (!action.equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (block == null || !block.getType().equals(Material.SPAWNER)) {
            return;
        }

        if (item == null || !item.getType().name().endsWith("SPAWN_EGG")) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_MOD)) {
            return;
        }

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        player.sendMessage(ChatColor.RED + "Monster Spawners can not be modified using Spawn Eggs");
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!isEnabled() || event.isCancelled() || !nerfExplosiveDamage) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!(event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) && !(event.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION))) {
            return;
        }

        final double newAmount = event.getDamage() * nerfExplosiveDamageAmount;
        event.setDamage(newAmount);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onWitherSpawn(CreatureSpawnEvent event) {
        if (!isEnabled() || !witherSpawnCooldown || event.isCancelled()) {
            return;
        }

        if (!event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.BUILD_WITHER)) {
            return;
        }

        if (nextWitherSpawn < Time.now()) {
            event.setCancelled(true);
            return;
        }

        nextWitherSpawn = Time.now() + (60 * 1000L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEnabled() || disabledSpawnerBlockBreaks.isEmpty()) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (!block.getType().equals(Material.SPAWNER)) {
            return;
        }

        final CreatureSpawner spawner = (CreatureSpawner) block.getState();
        if (!disabledSpawnerBlockBreaks.contains(spawner.getSpawnedType())) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "This type of Creature Spawner can not be broken");
    }

    @EventHandler
    public void onCreatureSpawn(PreCreatureSpawnEvent event) {
        if (!isEnabled() || disabledEntities.isEmpty() || event.isCancelled()) {
            return;
        }

        if (disabledEntities.contains(event.getType())
                && !(event.getReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER) || event.getReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBlockChange(EntityChangeBlockEvent event) {
        if (!isEnabled() || !disableEntityBlockChanges) {
            return;
        }

        if (
                event.getEntity() instanceof Player
                || event.getEntity() instanceof FallingBlock
                || event.getEntity() instanceof Villager) {

            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!isEnabled() || event.isCancelled() || !disablePistonBreakingDoors) {
            return;
        }

        final List<Block> modified = event.getBlocks();
        for (Block modifiedBlock : modified) {
            if (modifiedBlock == null || modifiedBlock.getType().equals(Material.AIR)) {
                continue;
            }

            if (modifiedBlock.getType().name().contains("_DOOR")) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onEnderchestInteract(PlayerInteractEvent event) {
        if (!isEnabled() || !disableEnderchests || event.useInteractedBlock().equals(Event.Result.DENY)) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (block == null || !block.getType().equals(Material.ENDER_CHEST)) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        player.sendMessage(ChatColor.RED + "Enderchests have been disabled");
    }

    @EventHandler
    public void onBombBlockInteract(PlayerInteractEvent event) {
        if (!isEnabled() || !disableExplosiveExploits) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();

        if (block == null || !block.getType().isInteractable()) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        if (player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        if (block.getType().name().endsWith("_BED") && block.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            player.sendMessage(ChatColor.RED + "You can not sleep in the nether");
            event.setCancelled(true);
            return;
        }

        if (block.getType().equals(Material.RESPAWN_ANCHOR) && !block.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            final RespawnAnchor anchor = (RespawnAnchor) block.getBlockData();
            if ((anchor.getCharges() + 1) >= anchor.getMaximumCharges()) {
                player.sendMessage(ChatColor.RED + "Respawn anchors are disabled outside of The Nether");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!isEnabled() || !disableExplosiveExploits) {
            return;
        }

        if (event.getEntity() instanceof EnderCrystal) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!isEnabled() || !allowPearlingThroughTraps) {
            return;
        }

        final Projectile proj = event.getEntity();

        if (!(proj instanceof EnderPearl)) {
            return;
        }

        final Block hitBlock = event.getHitBlock();

        if (hitBlock == null) {
            return;
        }

        if (hitBlock.getType().equals(Material.TRIPWIRE)) {
            event.setCancelled(true);
        }

        else if (hitBlock.getType().name().endsWith("_FENCE_GATE")) {
            final BlockState state = hitBlock.getState();
            final BlockData data = state.getBlockData();

            if (data instanceof final Gate gate) {
                if (gate.isOpen()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onPlayerEnterBed(PlayerBedEnterEvent event) {
        if (!isEnabled() || !disableEnterBedMessage) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        new Scheduler(getPlugin()).sync(() ->
                Bukkit.getOnlinePlayers().forEach(onlinePlayer -> onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(" "))))
                .delay(1L)
                .run();
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!isEnabled() || !generateNetherPortalPlatforms) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();

        if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
            return;
        }

        final Location to = event.getTo();

        if (to == null || to.getWorld() == null || !to.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            return;
        }

        new Scheduler(getPlugin()).sync(() -> {
            final Location playerLoc = player.getLocation();

            if (playerLoc.getWorld() == null) {
                return;
            }

            final int minX = playerLoc.getBlockX() - 4;
            final int minZ = playerLoc.getBlockZ() - 4;
            final int maxX = playerLoc.getBlockX() + 4;
            final int maxZ = playerLoc.getBlockZ() + 4;
            final int y = playerLoc.getBlockY() - 1;
            final List<Block> platformBlocks = Lists.newArrayList();

            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    final Block block = playerLoc.getWorld().getBlockAt(x, y, z);
                    platformBlocks.add(block);
                }
            }

            final PortalPlatformGenerateEvent generateEvent = new PortalPlatformGenerateEvent(playerLoc, platformBlocks);
            Bukkit.getPluginManager().callEvent(generateEvent);

            if (generateEvent.isCancelled() || generateEvent.getBlockList().isEmpty()) {
                return;
            }

            generateEvent.getBlockList().forEach(block -> block.setType(Material.OBSIDIAN));
        }).delay(1L).run();
    }
}
