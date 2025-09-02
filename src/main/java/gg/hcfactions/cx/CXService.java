package gg.hcfactions.cx;

import com.github.retrooper.packetevents.PacketEvents;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gg.hcfactions.cx.attributes.AttributeManager;
import gg.hcfactions.cx.broadcasts.BroadcastManager;
import gg.hcfactions.cx.command.*;
import gg.hcfactions.cx.hologram.HologramManager;
import gg.hcfactions.cx.kits.KitManager;
import gg.hcfactions.cx.listener.AttributeListener;
import gg.hcfactions.cx.listener.HologramListener;
import gg.hcfactions.cx.listener.SignListener;
import gg.hcfactions.cx.listener.WarpGatewayListener;
import gg.hcfactions.cx.message.MessageManager;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.cx.modules.chat.ChatModule;
import gg.hcfactions.cx.modules.display.TablistModule;
import gg.hcfactions.cx.modules.player.combat.*;
import gg.hcfactions.cx.modules.player.exploit.ExploitPatchModule;
import gg.hcfactions.cx.modules.player.items.ItemModificationModule;
import gg.hcfactions.cx.modules.player.items.ItemVelocityModule;
import gg.hcfactions.cx.modules.player.vanish.VanishManager;
import gg.hcfactions.cx.modules.reboot.RebootModule;
import gg.hcfactions.cx.modules.world.*;
import gg.hcfactions.cx.rollback.RollbackManager;
import gg.hcfactions.cx.warp.WarpManager;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import gg.hcfactions.libs.bukkit.services.IAresService;
import gg.hcfactions.libs.bukkit.utils.Enchants;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

@Getter
public final class CXService implements IAresService {
    public final AresPlugin plugin;
    public final NamespacedKey namespacedKey;
    public final String name = "Command X";

    public MessageManager messageManager;
    public VanishManager vanishManager;
    public WarpManager warpManager;
    public KitManager kitManager;
    public BroadcastManager broadcastManager;
    public HologramManager hologramManager;
    public RollbackManager rollbackManager;
    public AttributeManager attributeManager;
    public RebootModule rebootModule;
    public AnimationModule animationModule;
    public KnockbackModule knockbackModule;
    public ItemVelocityModule itemVelocityModule;
    public WorldModule worldModule;
    public ChatModule chatModule;
    public PotionLimitModule potionLimitModule;
    public EnchantLimitModule enchantLimitModule;
    public MobstackModule mobstackModule;
    public ItemModificationModule itemModificationModule;
    public RegenModule regenModule;
    public TablistModule tablistModule;
    public ExploitPatchModule exploitPatchModule;
    public EXPBonusModule expBonusModule;
    public DurabilityModule durabilityModule;
    public ElytraBalanceModule elytraBalanceModule;
    public ShulkerModule shulkerModule;
    public EntityDropModule entityDropModule;
    public PotionPrecisionModule potionPrecisionModule;

    private final Map<Class<? extends ICXModule>, ICXModule> moduleRepository;

    public CXService(AresPlugin plugin) {
        this.plugin = plugin;
        this.namespacedKey = new NamespacedKey(plugin, "cx");
        this.moduleRepository = Maps.newHashMap();
    }

    private ICXModule registerModule(ICXModule module) {
        if (moduleRepository.containsKey(module.getClass())) {
            throw new IllegalStateException("Module already registered: " + module.getClass());
        }

        moduleRepository.put(module.getClass(), module);
        plugin.getAresLogger().info("Registering module: {}", module.getClass().getSimpleName());
        return module;
    }

    private void startModules() {
        plugin.getAresLogger().info("Starting {} modules", moduleRepository.size());
        moduleRepository.values().forEach(ICXModule::onEnable);
    }

    private void stopModules() {
        plugin.getAresLogger().info("Stopping {} modules", moduleRepository.size());
        moduleRepository.values().forEach(ICXModule::onDisable);
    }

    private void reloadModules() {
        plugin.getAresLogger().info("Reloading {} modules", moduleRepository.size());

        // Special reload scenarios
        warpManager.loadWarps();
        warpManager.loadGateways();
        kitManager.loadKits();
        broadcastManager.loadBroadcasts();
        hologramManager.reloadHolograms();

        moduleRepository.values().forEach(ICXModule::onReload);
    }

    @Override
    public void onEnable() {
        plugin.registerCommand(new EssentialCommand(this));
        plugin.registerCommand(new ReloadCommand(this));
        plugin.registerCommand(new MessageCommand(this));
        plugin.registerCommand(new RebootCommand(this));
        plugin.registerCommand(new VanishCommand(this));
        plugin.registerCommand(new WarpCommand(this));
        plugin.registerCommand(new KitCommand(this));
        plugin.registerCommand(new HologramCommand(this));
        plugin.registerCommand(new RollbackCommand(this));
        plugin.registerCommand(new AttributeCommand(this));

        plugin.registerListener(new SignListener(this));
        plugin.registerListener(new WarpGatewayListener(this));
        plugin.registerListener(new HologramListener(this));
        plugin.registerListener(new AttributeListener(this));

        messageManager = new MessageManager(this);
        vanishManager = new VanishManager(this);
        rollbackManager = new RollbackManager(this);
        attributeManager = new AttributeManager(this);

        broadcastManager = new BroadcastManager(this);
        broadcastManager.loadBroadcasts();
        broadcastManager.startBroadcaster();

        warpManager = new WarpManager(this);
        warpManager.loadWarps();
        warpManager.loadGateways();

        kitManager = new KitManager(this);
        kitManager.loadKits();

        hologramManager = new HologramManager(this);
        hologramManager.loadHolograms();
        new Scheduler(plugin).sync(() -> hologramManager.spawnHolograms()).delay(20L).run();

        // command completions
        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("warps", ctx -> {
            final List<String> names = Lists.newArrayList();
            warpManager.getWarpRepository().forEach(w -> names.add(w.getName()));
            return names;
        });

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("kits", ctx -> {
            final List<String> names = Lists.newArrayList();
            kitManager.getKitRepository().forEach(k -> names.add(k.getName()));
            return names;
        });

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("enchants", ctx -> {
            final List<String> names = Lists.newArrayList();

            for (Enchantment enc : Enchants.getAllEnchantments()) {
                names.add(enc.getKey().getKey().toLowerCase());
            }

            return names;
        });

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("materials", ctx -> {
            final List<String> names = Lists.newArrayList();

            for (Material material : Material.values()) {
                names.add(material.name());
            }

            return names;
        });

        plugin.getCommandManager().getCommandCompletions().registerAsyncCompletion("attributes", ctx -> {
            final List<String> names = Lists.newArrayList();
            AttributeManager.DEFAULT_ATTRIBUTES.keySet().forEach(key -> names.add(key.name()));
            return names;
        });

        knockbackModule = (KnockbackModule) registerModule(new KnockbackModule(this));
        itemVelocityModule = (ItemVelocityModule) registerModule(new ItemVelocityModule(this));
        worldModule = (WorldModule) registerModule(new WorldModule(this));
        chatModule = (ChatModule) registerModule(new ChatModule(this));
        potionLimitModule = (PotionLimitModule) registerModule(new PotionLimitModule(this));
        enchantLimitModule = (EnchantLimitModule) registerModule(new EnchantLimitModule(this));
        itemModificationModule = (ItemModificationModule) registerModule(new ItemModificationModule(this));
        mobstackModule = (MobstackModule) registerModule(new MobstackModule(this));
        regenModule = (RegenModule) registerModule(new RegenModule(this));
        tablistModule = (TablistModule) registerModule(new TablistModule(this));
        rebootModule = (RebootModule) registerModule(new RebootModule(this));
        exploitPatchModule = (ExploitPatchModule) registerModule(new ExploitPatchModule(this));
        expBonusModule = (EXPBonusModule) registerModule(new EXPBonusModule(this));
        durabilityModule = (DurabilityModule) registerModule(new DurabilityModule(this));
        elytraBalanceModule = (ElytraBalanceModule) registerModule(new ElytraBalanceModule(this));
        shulkerModule = (ShulkerModule) registerModule(new ShulkerModule(this));
        entityDropModule = (EntityDropModule) registerModule(new EntityDropModule(this));
        potionPrecisionModule = (PotionPrecisionModule) registerModule(new PotionPrecisionModule(this));

        if (PacketEvents.getAPI().isLoaded()) {
            animationModule = (AnimationModule) registerModule(new AnimationModule(this));
        }

        startModules();
    }

    @Override
    public void onDisable() {
        stopModules();
        hologramManager.despawnHolograms();
    }

    @Override
    public void onReload() {
        reloadModules();
    }
}
