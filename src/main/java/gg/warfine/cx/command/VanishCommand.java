package gg.warfine.cx.command;

import gg.warfine.cx.CXPermissions;
import gg.warfine.cx.CXService;
import gg.warfine.libs.acf.BaseCommand;
import gg.warfine.libs.acf.annotation.CommandAlias;
import gg.warfine.libs.acf.annotation.CommandPermission;
import gg.warfine.libs.acf.annotation.Description;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@AllArgsConstructor
public final class VanishCommand extends BaseCommand {
    @Getter public CXService service;

    @CommandAlias("vanish|v")
    @CommandPermission(CXPermissions.CX_MOD)
    @Description("Toggle vanish")
    public void onVanish(Player player) {
        if (service.getVanishManager().isVanished(player)) {
            service.getVanishManager().unvanish(player);
            player.sendMessage(ChatColor.DARK_AQUA + "You have unvanished");
            return;
        }

        service.getVanishManager().setVanished(player);
        player.sendMessage(ChatColor.DARK_AQUA + "You have vanished");
    }
}
