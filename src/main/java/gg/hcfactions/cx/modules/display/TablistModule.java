package gg.hcfactions.cx.modules.display;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.modules.ICXModule;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public final class TablistModule implements ICXModule, Listener {
    @Getter public final CXService service;
    @Getter public final String key;
    @Getter @Setter public boolean enabled;

    private Component playerListHeader;
    private Component playerListFooter;

    public TablistModule(CXService service) {
        this.service = service;
        this.key = "display.tablist.";
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

        final List<String> headerContent = conf.getStringList(getKey() + ".header");
        final List<String> footerContent = conf.getStringList(getKey() + ".footer");

        this.playerListHeader = Component.empty();
        this.playerListFooter = Component.empty();

        for (int i = 0; i < headerContent.size(); i++) {
            final String content = headerContent.get(i);

            if (i != 0) {
                playerListHeader = playerListHeader.appendNewline();
            }

            playerListHeader = playerListHeader.append(service.getPlugin().getMiniMessage().deserialize(content));
        }

        for (int i = 0; i < footerContent.size(); i++) {
            final String content = footerContent.get(i);

            if (i != 0) {
                playerListFooter = playerListFooter.appendNewline();
            }

            playerListFooter = playerListFooter.append(service.getPlugin().getMiniMessage().deserialize(content));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!isEnabled()) {
            return;
        }

        new Scheduler(getPlugin()).sync(() ->
                event.getPlayer().sendPlayerListHeaderAndFooter(playerListHeader, playerListFooter))
        .delay(10L).run();
    }
}
