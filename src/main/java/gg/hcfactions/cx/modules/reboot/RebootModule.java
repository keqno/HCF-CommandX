package gg.hcfactions.cx.modules.reboot;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.cx.modules.reboot.event.ServerRestartEvent;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.AresPlugin;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.spigotmc.RestartCommand;

public final class RebootModule implements ICXModule {
    public static final String REBOOT_PREFIX = ChatColor.DARK_RED + "[" + ChatColor.RED + "Reboot" + ChatColor.DARK_RED + "]" + ChatColor.RED + " ";

    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;
    @Getter @Setter public boolean rebootInProgress;
    @Getter @Setter public long rebootCommenceTime;
    @Getter @Setter public long rebootTime;
    @Getter @Setter public BukkitTask rebootTask;

    public RebootModule(CXService service) {
        this.service = service;
        this.key = "reboot.";
        this.enabled = false;
    }

    @Override
    public void onEnable() {
        loadConfig();

        if (!isEnabled()) {
            return;
        }

        initScheduler();
    }

    @Override
    public void onDisable() {
        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
        initScheduler();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.rebootCommenceTime = Time.now() + (conf.getInt(getKey() + "max_lifespan")*1000L);
    }

    private void initScheduler() {
        if (rebootTask != null) {
            rebootTask.cancel();
        }

        rebootTask = new Scheduler(getPlugin()).async(() -> {
            if (!isRebootInProgress() && Time.now() >= rebootCommenceTime) {
                startReboot(60);
            }

            if (isRebootInProgress() && Time.now() >= rebootTime) {
                Bukkit.getServer().savePlayers();
                Bukkit.getWorlds().forEach(World::save);

                final ServerRestartEvent restartEvent = new ServerRestartEvent();
                Bukkit.getPluginManager().callEvent(restartEvent);

                if (!restartEvent.isCancelled()) {
                    new Scheduler(getPlugin()).sync(RestartCommand::restart).run();
                }
            }
        }).repeat(20L, 20L).run();
    }

    /**=
     * @return Time (in millis) until reboot
     */
    public long getTimeUntilReboot() {
        return isRebootInProgress() ? rebootTime - Time.now() : rebootCommenceTime - Time.now();
    }

    /**
     * Start the reboot process with the provided amount of seconds
     * @param countdownSeconds Time until reboot (in seconds)
     */
    public void startReboot(int countdownSeconds) {
        setRebootTime(Time.now() + (countdownSeconds*1000L));
        setRebootInProgress(true);

        Bukkit.broadcastMessage(REBOOT_PREFIX + "Server will restart in " + Time.convertToRemaining(countdownSeconds*1000L));
    }

    /**
     * Cancel the reboot in progress
     */
    public void cancelReboot() {
        setRebootInProgress(false);
        Bukkit.broadcastMessage(REBOOT_PREFIX + "Server restart has been cancelled");
    }

    /**
     * Reschedule reboot to a named time
     * @param timeStr Time string (1h30s5s)
     * @param promise (Promise)
     */
    public void rescheduleReboot(String timeStr, Promise promise) {
        final long ms;
        try {
            ms = Time.parseTime(timeStr);
        } catch (NumberFormatException e) {
            promise.reject("Invalid time format");
            return;
        }

        if (isRebootInProgress()) {
            setRebootInProgress(false);
            Bukkit.broadcastMessage(REBOOT_PREFIX + "Server restart has been rescheduled");
        }

        setRebootCommenceTime(Time.now() + ms);
        Bukkit.broadcastMessage(REBOOT_PREFIX + "Server restart has been rescheduled to " + Time.convertToRemaining(ms));
        promise.resolve();
    }

    /**
     * Print reboot info to a command sender
     * @param sender Receiver
     */
    public void printReboot(CommandSender sender) {
        sender.sendMessage(REBOOT_PREFIX + "The server is expected to restart in " + Time.convertToRemaining(getTimeUntilReboot()));
    }
}
