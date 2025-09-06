//package gg.warfine.cx.modules.player.combat;
//
//import com.comphenix.protocol.PacketType;
//import com.comphenix.protocol.events.ListenerPriority;
//import com.comphenix.protocol.events.PacketAdapter;
//import com.comphenix.protocol.events.PacketContainer;
//import com.comphenix.protocol.events.PacketEvent;
//import com.comphenix.protocol.wrappers.EnumWrappers;
//import com.comphenix.protocol.wrappers.WrappedEnumEntityUseAction;
//import com.github.retrooper.packetevents.event.*;
//import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
//import com.google.common.collect.Queues;
//import com.google.common.collect.Sets;
//import gg.warfine.cx.CXService;
//import modules.gg.warfine.cx.ICXModule;
//import gg.warfine.libs.bukkit.events.impl.PlayerDamagePlayerEvent;
//import gg.warfine.libs.bukkit.scheduler.Scheduler;
//import gg.warfine.libs.bukkit.services.impl.account.AccountService;
//import gg.warfine.libs.bukkit.services.impl.account.model.AresAccount;
//import lombok.Getter;
//import lombok.Setter;
//import net.kyori.adventure.text.Component;
//import org.bukkit.*;
//import org.bukkit.attribute.Attribute;
//import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.enchantments.Enchantment;
//import org.bukkit.entity.*;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
//import org.bukkit.event.Listener;
//import org.bukkit.event.entity.EntityDamageByEntityEvent;
//import org.bukkit.event.entity.EntityDamageEvent;
//import org.bukkit.event.entity.ProjectileHitEvent;
//import org.bukkit.event.player.PlayerChangedWorldEvent;
//import org.bukkit.event.player.PlayerJoinEvent;
//import org.bukkit.event.player.PlayerQuitEvent;
//import org.bukkit.event.player.PlayerTeleportEvent;
//import org.bukkit.inventory.ItemStack;
//import org.bukkit.scheduler.BukkitTask;
//
//import java.util.*;
//
//public final class AnimationModule implements ICXModule, Listener {
//    @Getter public final CXService service;
//    @Getter public final String key;
//    @Getter @Setter public boolean enabled;
//    @Getter public Set<UUID> debugging;
//
//    // config values
//    private double maxReach;
//    private int noDamageTicks;
//
//    private AccountService accountService;
//    private BukkitTask attackQueueTask;
//    private final Queue<QueuedAttack> queuedAttacks;
//    private final Set<UUID> recentlyTakenProjectileDamage;
//    private final Set<UUID> recentlyTakenTickDamage;
//    private final Set<UUID> recentlyUsedEnderpearl;
//
//    public AnimationModule(CXService service) {
//        this.service = service;
//        this.key = "combat.animation.";
//        this.enabled = false;
//        this.accountService = null;
//        this.queuedAttacks = Queues.newConcurrentLinkedQueue();
//        this.recentlyTakenProjectileDamage = Sets.newConcurrentHashSet();
//        this.recentlyTakenTickDamage = Sets.newConcurrentHashSet();
//        this.recentlyUsedEnderpearl = Sets.newConcurrentHashSet();
//        this.debugging = Sets.newConcurrentHashSet();
//    }
//
//    @Override
//    public void onEnable() {
//        loadConfig();
//
//        if (!enabled) {
//            getPlugin().getAresLogger().error("animation module disabled");
//            return;
//        }
//
//        getPlugin().registerPacketListener(new PlayerAttackPacketListener(), PacketListenerPriority.LOW);
//        implQueueTask();
//
//        accountService = (AccountService)getPlugin().getService(AccountService.class);
//        if (accountService == null) {
//            getPlugin().getAresLogger().error("failed to obtain Account Service, critical strike audio queue will not be played");
//        }
//
//        getPlugin().registerListener(this);
//    }
//
//    @Override
//    public void onReload() {
//        debugging.clear();
//        queuedAttacks.clear();
//
//        loadConfig();
//
//        if (!enabled) {
//            if (attackQueueTask != null) {
//                attackQueueTask.cancel();
//                attackQueueTask = null;
//            }
//
//            return;
//        }
//
//        implQueueTask();
//    }
//
//    @Override
//    public void onDisable() {
//        debugging.clear();
//        queuedAttacks.clear();
//
//        if (attackQueueTask != null) {
//            attackQueueTask.cancel();
//            attackQueueTask = null;
//        }
//
//        PlayerJoinEvent.getHandlerList().unregister(this);
//        PlayerQuitEvent.getHandlerList().unregister(this);
//        PlayerChangedWorldEvent.getHandlerList().unregister(this);
//        EntityDamageEvent.getHandlerList().unregister(this);
//
//        enabled = false;
//    }
//
//    private void loadConfig() {
//        final YamlConfiguration conf = getConfig();
//        enabled = conf.getBoolean(getKey() + "enabled");
//        maxReach = conf.getDouble(getKey() + "max_reach");
//        noDamageTicks = conf.getInt(getKey() + "no_damage_ticks");
//    }
//
//    private void implQueueTask() {
//        if (attackQueueTask != null) {
//            attackQueueTask.cancel();
//            attackQueueTask = null;
//        }
//
//        attackQueueTask = new Scheduler(getPlugin()).sync(() -> {
//            while (!queuedAttacks.isEmpty()) {
//                final QueuedAttack attack = queuedAttacks.remove();
//                attack.getAttacked().damage(attack.getDamage(), attack.getAttacker());
//                if (debugging.contains(attack.getAttacker().getUniqueId())) {
//                    attack.getAttacker().sendMessage(ChatColor.GREEN + "DEBUG: hit processed");
//                }
//            }
//        }).repeat(0L, 1L).run();
//    }
//
//    /**
//     * @Deprecated
//     * We had a good thing going but you just had to go
//     * and fuck everything up @ProtocolLib
//     */
//    private void implPacketListener() {
//
//
//        getPlugin().getRegisteredProtocolManager().addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.LOWEST, PacketType.Play.Client.USE_ENTITY) {
//            @Override
//            public void onPacketReceiving(PacketEvent event) {
//                final PacketContainer packet = event.getPacket();
//                Bukkit.broadcast(Component.text(packet.getEnumEntityUseActions().toString()));
//                packet.getEnumEntityUseActions();
//                final WrappedEnumEntityUseAction useAction = packet.getEnumEntityUseActions().read(0);
//                final EnumWrappers.EntityUseAction action = useAction.getAction();
//                final int entityId = packet.getIntegers().read(0);
//
//                if (!action.equals(EnumWrappers.EntityUseAction.ATTACK)) {
//                    return;
//                }
//
//                if (Bukkit.getOnlinePlayers().stream().noneMatch(p -> p.getEntityId() == entityId)) {
//                    return;
//                }
//
//                event.setCancelled(true);
//
//                new Scheduler(plugin).sync(() -> {
//                    final Player damager = event.getPlayer();
//                    final Entity entity = packet.getEntityModifier(event).readSafely(0);
//
//                    if (entity == null) {
//                        if (debugging.contains(damager.getUniqueId())) {
//                            damager.sendMessage(ChatColor.RED + "DEBUG: no entity");
//                        }
//
//                        return;
//                    }
//
//                    if (entity instanceof Player && !((Player)entity).getGameMode().equals(GameMode.SURVIVAL)) {
//                        if (debugging.contains(damager.getUniqueId())) {
//                            damager.sendMessage(ChatColor.RED + "DEBUG: player is not in survival");
//                        }
//
//                        return;
//                    }
//
//                    if (!(entity instanceof final LivingEntity damaged)) {
//                        return;
//                    }
//
//                    final double distance = damager.getLocation().distanceSquared(damaged.getLocation());
//
//                    if (damaged.isDead()) {
//                        if (debugging.contains(damager.getUniqueId())) {
//                            damager.sendMessage(ChatColor.RED + "DEBUG: damaged entity is dead");
//                        }
//
//                        return;
//                    }
//
//                    if (distance > (maxReach * maxReach)) {
//                        if (debugging.contains(damager.getUniqueId())) {
//                            damager.sendMessage(ChatColor.RED + "DEBUG: " + distance + " > " + (maxReach * maxReach));
//                        }
//
//                        return;
//                    }
//
//                    double initialDamage = Objects.requireNonNull(damager.getAttribute(Attribute.ATTACK_DAMAGE)).getValue();
//                    boolean criticalHit = false;
//
//                    if (!((LivingEntity) damager).isOnGround() && damager.getVelocity().getY() < 0) {
//                        initialDamage *= 1.25;
//                        criticalHit = true;
//
//                        if (accountService != null) {
//                            final AresAccount cachedAccount = accountService.getCachedAccount(damager.getUniqueId());
//
//                            if (cachedAccount != null && cachedAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.USE_NEW_CRIT_SOUND)) {
//                                damager.playSound(damaged.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
//                            }
//                        }
//                    }
//
//                    queuedAttacks.add(new QueuedAttack(damager, damaged, initialDamage, criticalHit));
//                    if (debugging.contains(damager.getUniqueId())) {
//                        damager.sendMessage(ChatColor.YELLOW + "DEBUG: attack queued");
//                    }
//                }).run();
//            }
//        });
//    }
//
//    /**
//     * Handles setting the attack speed for a player upon logging in
//     * @param event PlayerJoinEvent
//     */
//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        if (!isEnabled()) {
//            return;
//        }
//
//        final Player player = event.getPlayer();
//        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED)).setBaseValue(2048.0);
//        player.saveData();
//    }
//
//    /**
//     * Handles reverting attack speed for a player quitting the server
//     * @param event PlayerQuitEvent
//     */
//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        final Player player = event.getPlayer();
//        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED)).setBaseValue(4.0);
//        player.saveData();
//    }
//
//    /**
//     * Handles updating attack speed for a player changing worlds
//     * @param event PlayerChangedWorldEvent
//     */
//    @EventHandler
//    public void onWorldChange(PlayerChangedWorldEvent event) {
//        if (!isEnabled()) {
//            return;
//        }
//
//        final Player player = event.getPlayer();
//        Objects.requireNonNull(player.getAttribute(Attribute.ATTACK_SPEED)).setBaseValue(2048.0);
//        player.saveData();
//    }
//
//    /**
//     * Handles applying fire aspect on attack
//     * @param event PlayerDamagePlayerEvent
//     */
//    @EventHandler (priority = EventPriority.MONITOR)
//    public void onPlayerDamagePlayer(PlayerDamagePlayerEvent event) {
//        if (!isEnabled()) {
//            return;
//        }
//
//        if (event.isCancelled()) {
//            return;
//        }
//
//        final Player damager = event.getDamager();
//        final Player damaged = event.getDamaged();
//        final ItemStack hand = damager.getInventory().getItemInMainHand();
//
//        if (damager.getUniqueId().equals(damaged.getUniqueId())) {
//            return;
//        }
//
//        // Fire Aspect
//        if (hand.hasItemMeta() && Objects.requireNonNull(hand.getItemMeta()).hasEnchant(Enchantment.FIRE_ASPECT)) {
//            damaged.setFireTicks(80 * hand.getItemMeta().getEnchantLevel(Enchantment.FIRE_ASPECT));
//        }
//    }
//
//    /**
//     * Handles applying fire aspect to non-player entities
//     * @param event EntityDamageByEntityEvent
//     */
//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
//        if (!isEnabled()) {
//            return;
//        }
//
//        if (event.isCancelled()) {
//            return;
//        }
//
//        final Entity damager = event.getDamager();
//        final Entity entity = event.getEntity();
//
//        if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
//            event.setCancelled(true);
//        }
//
//        if (!(damager instanceof final Player player) || entity instanceof Player) {
//            return;
//        }
//
//        final ItemStack hand = player.getInventory().getItemInMainHand();
//
//        if (player.getUniqueId().equals(entity.getUniqueId())) {
//            return;
//        }
//
//        // Fire Aspect
//        if (hand.hasItemMeta() && Objects.requireNonNull(hand.getItemMeta()).hasEnchant(Enchantment.FIRE_ASPECT)) {
//            entity.setFireTicks(80 * hand.getItemMeta().getEnchantLevel(Enchantment.FIRE_ASPECT));
//        }
//    }
//
//    @EventHandler (priority = EventPriority.MONITOR)
//    public void onProjectileHit(ProjectileHitEvent event) {
//        if (!isEnabled()) {
//            return;
//        }
//
//        if (event.isCancelled()) {
//            return;
//        }
//
//        if (!(event.getHitEntity() instanceof final Player player)) {
//            return;
//        }
//
//        if (player.getNoDamageTicks() <= 0) {
//            return;
//        }
//
//        if (recentlyTakenProjectileDamage.contains(player.getUniqueId())) {
//            event.setCancelled(true);
//            return;
//        }
//
//        final UUID uniqueId = player.getUniqueId();
//        final int preDamageTicks = player.getNoDamageTicks();
//
//        player.setNoDamageTicks(0);
//        recentlyTakenProjectileDamage.add(uniqueId);
//
//        new Scheduler(getPlugin()).sync(() -> recentlyTakenProjectileDamage.remove(uniqueId)).delay(20L).run();
//        new Scheduler(getPlugin()).sync(() -> player.setNoDamageTicks(preDamageTicks - 1)).run();
//    }
//
//    /**
//     * Listens for when a player and temporarily
//     * caches them so we can apply noDamageTicks
//     * accordingly.
//     * @param event Bukkit PlayerTeleportEvent
//     */
//    @EventHandler
//    public void onPlayerEnderpearl(PlayerTeleportEvent event) {
//        if (!isEnabled()) {
//            return;
//        }
//
//        if (event.isCancelled()) {
//            return;
//        }
//
//        if (!event.getCause().equals(PlayerTeleportEvent.TeleportCause.ENDER_PEARL)) {
//            return;
//        }
//
//        final UUID uuid = event.getPlayer().getUniqueId();
//        recentlyUsedEnderpearl.add(uuid);
//        new Scheduler(service.getPlugin()).sync(() -> recentlyUsedEnderpearl.remove(uuid)).delay(5L).run();
//    }
//
//    @EventHandler (priority = EventPriority.MONITOR)
//    public void onNoDamageTickApplied(EntityDamageEvent event) {
//        if (!(event.getEntity() instanceof final Player player)) {
//            return;
//        }
//
//        final UUID uniqueId = player.getUniqueId();
//        final EntityDamageEvent.DamageCause cause = event.getCause();
//        final boolean isEnderpearlDamage = (cause.equals(EntityDamageEvent.DamageCause.FALL) && recentlyUsedEnderpearl.contains(player.getUniqueId()));
//        final boolean isTickingCause = cause.equals(EntityDamageEvent.DamageCause.POISON)
//                || cause.equals(EntityDamageEvent.DamageCause.FIRE)
//                || cause.equals(EntityDamageEvent.DamageCause.LAVA)
//                || cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)
//                || cause.equals(EntityDamageEvent.DamageCause.FREEZE)
//                || cause.equals(EntityDamageEvent.DamageCause.WITHER)
//                || cause.equals(EntityDamageEvent.DamageCause.CRAMMING)
//                || cause.equals(EntityDamageEvent.DamageCause.CONTACT)
//                || cause.equals(EntityDamageEvent.DamageCause.DRAGON_BREATH)
//                || cause.equals(EntityDamageEvent.DamageCause.HOT_FLOOR)
//                || cause.equals(EntityDamageEvent.DamageCause.STARVATION)
//                || cause.equals(EntityDamageEvent.DamageCause.THORNS)
//                || cause.equals(EntityDamageEvent.DamageCause.VOID)
//                || cause.equals(EntityDamageEvent.DamageCause.DROWNING);
//
//        if (isTickingCause && recentlyTakenTickDamage.contains(uniqueId)) {
//            if (debugging.contains(player.getUniqueId())) {
//                player.sendMessage(ChatColor.RED + "DEBUG: skipped damage (isTicking and recentlyTakenTickDamage)");
//            }
//
//            event.setCancelled(true);
//            return;
//        }
//
//        final int ticks = (isTickingCause || isEnderpearlDamage) ? 0 : noDamageTicks;
//
//        if (isTickingCause) {
//            if (debugging.contains(player.getUniqueId())) {
//                player.sendMessage(ChatColor.RED + "DEBUG: skipped damage (isTicking and recentlyTakenTickDamage)");
//            }
//
//            recentlyTakenTickDamage.add(uniqueId);
//            new Scheduler(getPlugin()).sync(() -> recentlyTakenTickDamage.remove(uniqueId)).delay(noDamageTicks).run();
//        }
//
//        new Scheduler(getPlugin()).sync(() -> {
//            ((LivingEntity)event.getEntity()).setNoDamageTicks(ticks);
//
//            if (debugging.contains(player.getUniqueId())) {
//                player.sendMessage(ChatColor.AQUA + "set no damage ticks to " + ticks);
//            }
//        }).delay(1L).run();
//    }
//
//    public class PlayerAttackPacketListener implements PacketListener {
//        @Override
//        public void onPacketReceive(PacketReceiveEvent event) {
//            if (event.getPacketType() != com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.INTERACT_ENTITY) {
//                return;
//            }
//
//            WrapperPlayClientInteractEntity interactEntity = new WrapperPlayClientInteractEntity(event);
//            WrapperPlayClientInteractEntity.InteractAction action = interactEntity.getAction();
//
//            if (action != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
//                return;
//            }
//
//            int entityId = interactEntity.getEntityId();
//            Optional<? extends Player> playerQuery = Bukkit.getOnlinePlayers().stream().filter(p -> p.getEntityId() == entityId).findFirst();
//            if (playerQuery.isEmpty()) {
//                return;
//            }
//
//            Player attacker = Bukkit.getPlayer(event.getUser().getUUID());
//            Player attacked = playerQuery.get();
//
//            if (attacker == null) {
//                return;
//            }
//
//            if (!attacked.getGameMode().equals(GameMode.SURVIVAL) && !attacked.getGameMode().equals(GameMode.ADVENTURE)) {
//                if (debugging.contains(attacker.getUniqueId())) {
//                    attacker.sendMessage(ChatColor.RED + "DEBUG: skipped damage (invalid gamemode)");
//                }
//
//                return;
//            }
//
//            double hitDistance = attacker.getLocation().distanceSquared(attacked.getLocation());
//            if (hitDistance > (maxReach * maxReach)) {
//                if (debugging.contains(attacker.getUniqueId())) {
//                    attacker.sendMessage(ChatColor.RED + "DEBUG: skipped damage (invalid hit dist, " + hitDistance + " > " + (maxReach * maxReach) + ")");
//                }
//
//                return;
//            }
//
//            if (attacked.getHealth() <= 0 || attacked.isDead()) {
//                if (debugging.contains(attacker.getUniqueId())) {
//                    attacker.sendMessage(ChatColor.RED + "DEBUG: skipped damage (damaged is dead)");
//                }
//
//                return;
//            }
//
//            // We only want to process like this for swords
//            if (!attacker.getInventory().getItemInMainHand().getType().name().endsWith("SWORD")) {
//                return;
//            }
//
//            event.setCancelled(true);
//
//            double initialDamage = Objects.requireNonNull(attacker.getAttribute(Attribute.ATTACK_DAMAGE)).getValue();
//            boolean critical = false;
//
//            if (!((LivingEntity) attacker).isOnGround() && attacker.getVelocity().getY() < 0) {
//                initialDamage *= 1.25;
//                critical = true;
//            }
//
//            if (critical && accountService != null) {
//                AresAccount aresAccount = accountService.getCachedAccount(attacker.getUniqueId());
//                if (aresAccount != null && aresAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.USE_NEW_CRIT_SOUND)) {
//                    attacker.playSound(attacked.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
//                }
//            }
//
//            if (debugging.contains(attacker.getUniqueId())) {
//                attacker.sendMessage(ChatColor.YELLOW + "hit queued");
//            }
//
//            queuedAttacks.add(new QueuedAttack(attacker, attacked, initialDamage, critical));
//        }
//    }
//
//    public record QueuedAttack(@Getter Player attacker,
//                               @Getter LivingEntity attacked,
//                               @Getter double damage,
//                               @Getter boolean critical) {}
//}
