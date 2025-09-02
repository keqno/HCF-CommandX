package gg.hcfactions.cx.modules.chat;

import com.google.common.collect.Maps;
import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.events.impl.ProcessedChatEvent;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChatModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private boolean disableJoinLeaveMessages;
    private boolean chatRateLimited;

    private int chatDelay;
    private Map<UUID, Long> recentChatters;
    private List<String> whitelistedLinks;

    public ChatModule(CXService service) {
        this.service = service;
        this.key = "chat.";
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
        setEnabled(false);
    }

    @Override
    public void onReload() {
        loadConfig();
    }

    private void loadConfig() {
        final YamlConfiguration conf = getConfig();
        this.enabled = conf.getBoolean(getKey() + "enabled");
        this.disableJoinLeaveMessages = conf.getBoolean(getKey() + "disable_join_leave_messages");
        this.chatRateLimited = conf.getBoolean(getKey() + "rate_limit.enabled");
        this.chatDelay = conf.getInt(getKey() + "rate_limit.delay");
        this.whitelistedLinks = conf.getStringList(getKey() + "whitelisted_links");
        this.recentChatters = Maps.newConcurrentMap();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();

        if (disableJoinLeaveMessages) {
            event.setJoinMessage(null);
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission(CXPermissions.CX_MOD)) {
                p.sendMessage(ChatColor.GRAY + player.getName() + " has connected");
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!isEnabled()) {
            return;
        }

        final Player player = event.getPlayer();

        if (disableJoinLeaveMessages) {
            event.setQuitMessage(null);
        }

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission(CXPermissions.CX_MOD)) {
                p.sendMessage(ChatColor.GRAY + player.getName() + " has disconnected");
            }
        });
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onPostLink(ProcessedChatEvent event) {
        if (!isEnabled() || whitelistedLinks.isEmpty()) {
            return;
        }

        final Player player = event.getPlayer();
        final String message = event.getMessage();
        final String[] split = message.split(" ");

        if (player.hasPermission(CXPermissions.CX_MOD) || player.hasPermission(CXPermissions.CX_PREMIUM)) {
            return;
        }

        for (String str : split) {
            if (isBlacklistedLink(str)) {
                player.sendMessage(ChatColor.RED + "This type of link is not permitted on this server");
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onChat(ProcessedChatEvent event) {
        if (!isEnabled() || !chatRateLimited) {
            return;
        }

        final Player player = event.getPlayer();

        if (player.hasPermission(CXPermissions.CX_MOD) || player.hasPermission(CXPermissions.CX_PREMIUM)) {
            return;
        }

        final long remainingCooldown = recentChatters.getOrDefault(player.getUniqueId(), 0L);

        if (remainingCooldown > Time.now()) {
            player.sendMessage(ChatColor.RED + "Please wait " + (Time.convertToDecimal(remainingCooldown - Time.now())) + "s before sending another message");
            event.setCancelled(true);
            return;
        }

        final UUID uniqueId = player.getUniqueId();
        final long nextAllowedMessage = (Time.now() + (chatDelay * 1000L));

        recentChatters.put(player.getUniqueId(), nextAllowedMessage);
        new Scheduler(getPlugin()).sync(() -> recentChatters.remove(uniqueId)).delay(chatDelay * 20L).run();
    }

    /**
     * Returns true if the provided link is blacklisted
     * @param message Message
     * @return True if blacklisted
     */
    private boolean isBlacklistedLink(String message) {
        final boolean match = message.matches("^(http://www\\.|https://www\\.|http://|https://)?[a-z0-9]+([\\-.][a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(/.*)?$");

        for (String whitelisted : whitelistedLinks) {
            if (message.contains(whitelisted)) {
                return false;
            }
        }

        return match;
    }
}
