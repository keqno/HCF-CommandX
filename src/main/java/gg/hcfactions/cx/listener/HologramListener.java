package gg.hcfactions.cx.listener;

import gg.hcfactions.cx.CXService;
import lombok.Getter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public record HologramListener(@Getter CXService service) implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof final ArmorStand armorStand)) {
            return;
        }

        if (!armorStand.getPersistentDataContainer().has(service.getNamespacedKey(), PersistentDataType.STRING)) {
            return;
        }

        final String data = armorStand.getPersistentDataContainer().get(service.getNamespacedKey(), PersistentDataType.STRING);

        if (data == null || !data.equalsIgnoreCase("hologram")) {
            return;
        }

        event.setCancelled(true);
    }
}
