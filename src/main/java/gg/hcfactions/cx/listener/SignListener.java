package gg.hcfactions.cx.listener;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.kits.KitManager;
import gg.hcfactions.cx.warp.WarpManager;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public record SignListener(@Getter CXService service) implements Listener {
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        final Player player = event.getPlayer();
        final String header = event.getLine(0);

        if (header == null) {
            return;
        }

        if (!player.hasPermission(CXPermissions.CX_ADMIN)) {
            return;
        }

        if (header.equals(WarpManager.WARP_SIGN_IDENTIFIER)) {
            final String warpName = event.getLine(1);

            if (service.getWarpManager().getWarp(warpName).isEmpty()) {
                player.sendMessage(ChatColor.RED + "Warp not found");
                event.setCancelled(true);
                return;
            }

            event.setLine(0, WarpManager.FORMATTED_WARP_SIGN_IDENTIFIER);
            event.setLine(2, "");
            event.setLine(3, "");
            player.sendMessage(ChatColor.YELLOW + "Warp sign created");
            return;
        }

        if (header.equals(KitManager.KIT_SIGN_IDENTIFIER)) {
            final String kitName = event.getLine(1);

            if (service.getKitManager().getKitByName(kitName).isEmpty()) {
                player.sendMessage(ChatColor.RED + "Kit not found");
                event.setCancelled(true);
                return;
            }

            event.setLine(0, KitManager.FORMATTED_KIT_SIGN_IDENTIFIER);
            event.setLine(2, "");
            event.setLine(3, "");
            player.sendMessage(ChatColor.YELLOW + "Kit sign created");
        }
    }

    @EventHandler
    public void onSignColorFormat(SignChangeEvent event) {
        final Player player = event.getPlayer();

        if (!player.hasPermission(CXPermissions.CX_MOD)) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            final String line = event.getLine(i);

            if (line == null) {
                continue;
            }

            event.setLine(i, ChatColor.translateAlternateColorCodes('&', line));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null) {
            return;
        }

        if (!(clickedBlock.getState() instanceof final Sign sign)) {
            return;
        }

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        final String header = sign.getLine(0);

        if (header.equals(WarpManager.FORMATTED_WARP_SIGN_IDENTIFIER)) {
            final String warpName = sign.getLine(1);

            service.getWarpManager().getWarp(warpName).ifPresent(warp -> {
                warp.teleport(player);
                player.sendMessage(ChatColor.YELLOW + "You have teleported to " + ChatColor.BLUE + warp.getName());
            });

            return;
        }

        if (header.equals(KitManager.FORMATTED_KIT_SIGN_IDENTIFIER)) {
            final String kitName = sign.getLine(1);
            service.getKitManager().getKitByName(kitName).ifPresent(kit -> kit.give(player));
            return;
        }
    }
}
