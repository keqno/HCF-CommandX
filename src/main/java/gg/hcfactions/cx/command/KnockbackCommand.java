package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.modules.player.combat.KnockbackModule;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@AllArgsConstructor
@CommandAlias("knockback|kb")
public final class KnockbackCommand extends BaseCommand {
    @Getter public final KnockbackModule module;

    @Subcommand("get")
    @Description("View the current knockback values")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onGetKnockbackValues(Player player) {
        player.sendMessage(ChatColor.AQUA + "Knockback Values");
        player.sendMessage(ChatColor.GRAY + " - Horizontal: " + module.getKnockbackHorizontal());
        player.sendMessage(ChatColor.GRAY + " - Vertical: " + module.getKnockbackVertical());
        player.sendMessage(ChatColor.GRAY + " - Extra Horizontal: " + module.getKnockbackExtraHorizontal());
        player.sendMessage(ChatColor.GRAY + " - Extra Vertical: " + module.getKnockbackExtraVertical());
        player.sendMessage(ChatColor.GRAY + " - Vertical Clamp: " + module.getKnockbackVerticalLimit());
        player.sendMessage(ChatColor.GRAY + " - Sprint Reset Modifier: " + module.getSprintResetModifier());
        player.sendMessage(ChatColor.GRAY + " - Sprint Modifier: " + module.getSprintModifier());
        player.sendMessage(ChatColor.GRAY + " - Ground Check: " + module.isRequireGroundCheck());
    }

    @Subcommand("set")
    @Syntax("<h|v|eh|ev|vl|srm|sm> <amount>")
    @Description("Update the knockback values")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onSetKnockbackValues(Player player, @Values("h|v|eh|ev|vl|srm|sm") String fieldName, String valueName) {
        double v;
        try {
            v = Double.parseDouble(valueName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid value (must be a double)");
            return;
        }

        if (fieldName.equalsIgnoreCase("h")) {
            module.setKnockbackHorizontal(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated horizontal to " + v);
            return;
        }

        if (fieldName.equalsIgnoreCase("v")) {
            module.setKnockbackVertical(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated vertical to " + v);
            return;
        }

        if (fieldName.equalsIgnoreCase("eh")) {
            module.setKnockbackExtraHorizontal(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated extra horizontal to " + v);
            return;
        }

        if (fieldName.equalsIgnoreCase("ev")) {
            module.setKnockbackExtraVertical(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated extra vertical to " + v);
            return;
        }

        if (fieldName.equalsIgnoreCase("vl")) {
            module.setKnockbackVerticalLimit(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated vertical limit to " + v);
            return;
        }

        if (fieldName.equalsIgnoreCase("srm")) {
            module.setSprintResetModifier(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated sprint reset modifier to " + v);
            return;
        }

        if (fieldName.equalsIgnoreCase("sm")) {
            module.setSprintModifier(v);
            module.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Updated sprint modifier to " + v);
            return;
        }

        player.sendMessage(ChatColor.RED + "Invalid field name (see syntax)");
    }
}
