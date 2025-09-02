package gg.hcfactions.cx.listener;

import gg.hcfactions.cx.CXService;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public record AttributeListener(@Getter CXService service) implements Listener {
    /**
     * Handles cleaning up player values back to default upon login
     *
     * @param event PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        service.getAttributeManager().resetToDefaults(player);
    }
}
