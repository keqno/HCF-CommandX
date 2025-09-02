package gg.hcfactions.cx.message;

import com.google.common.collect.Maps;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.message.impl.MessageExecutor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MessageManager implements Listener {
    @Getter public final CXService service;
    @Getter public final MessageExecutor executor;
    @Getter public final Map<UUID, UUID> recentlyMessaged;

    public MessageManager(CXService service) {
        this.service = service;
        this.executor = new MessageExecutor(this);
        this.recentlyMessaged = Maps.newConcurrentMap();

        service.getPlugin().registerListener(this);
    }

    public Optional<UUID> getRecentlyMessaged(Player player) {
        return Optional.ofNullable(recentlyMessaged.get(player.getUniqueId()));
    }

    public void setRecentlyMessaged(Player sender, Player receiver) {
        recentlyMessaged.put(sender.getUniqueId(), receiver.getUniqueId());
        recentlyMessaged.put(receiver.getUniqueId(), sender.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        recentlyMessaged.remove(event.getPlayer().getUniqueId());
    }
}
