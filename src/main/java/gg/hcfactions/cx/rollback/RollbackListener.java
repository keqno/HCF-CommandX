package gg.hcfactions.cx.rollback;

import gg.hcfactions.cx.rollback.impl.RollbackInventory;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;

public record RollbackListener(@Getter RollbackManager manager) implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final RollbackInventory inv = manager.createInventory(
                player.getName(),
                Arrays.asList(player.getInventory().getContents()),
                Arrays.asList(player.getInventory().getArmorContents())
        );

        manager.getRollbackRepository().add(inv);
    }
}
