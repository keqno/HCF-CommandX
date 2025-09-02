package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@AllArgsConstructor
@CommandAlias("cx")
public final class ReloadCommand extends BaseCommand {
    @Getter public final CXService service;

    @Subcommand("debug")
    @Description("Enter debug mode")
    @CommandPermission(CXPermissions.CX_MOD)
    @CommandCompletion("@players")
    public void onDebug(Player player, @Optional String username) {
        Player toAdd = player;

        if (username != null) {
            toAdd = Bukkit.getPlayer(username);
        }

        if (toAdd == null) {
            player.sendMessage(ChatColor.RED + "Invalid player");
            return;
        }

        if (service.getAnimationModule().getDebugging().contains(toAdd.getUniqueId())) {
            service.getAnimationModule().getDebugging().remove(toAdd.getUniqueId());
            toAdd.sendMessage(ChatColor.AQUA + "Left debug mode");
            return;
        }

        service.getAnimationModule().getDebugging().add(toAdd.getUniqueId());
        toAdd.sendMessage(ChatColor.AQUA + "Entered debug mode");
    }

    @Subcommand("reload all")
    @Description("Reload CommandX")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onReload(Player player) {
        service.onReload();
        player.sendMessage(ChatColor.YELLOW + "CX Modules have been reloaded");
    }

    @Subcommand("reload knockback")
    @Description("Reload knockback values")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onReloadKnockback(Player player) {
        service.getKnockbackModule().onReload();
        player.sendMessage(ChatColor.YELLOW + "Reloaded knockback module");
    }

    @Subcommand("reload limits")
    @Description("Reload Enchantment/Potion Limits")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onReloadLimits(Player player) {
        service.getEnchantLimitModule().onReload();
        service.getPotionLimitModule().onReload();
        player.sendMessage(ChatColor.YELLOW + "Reloaded enchant/potion limits module");
    }

    @Subcommand("reload itemvelocity")
    @Description("Reload Item Velocity")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onReloadItemVelocity(Player player) {
        service.getItemVelocityModule().onReload();
        player.sendMessage(ChatColor.YELLOW + "Reloaded item velocity module");
    }
}
