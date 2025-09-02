package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.CommandAlias;
import gg.hcfactions.libs.acf.annotation.CommandPermission;
import gg.hcfactions.libs.acf.annotation.Description;
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
