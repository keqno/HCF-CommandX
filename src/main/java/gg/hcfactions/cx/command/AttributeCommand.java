package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

@AllArgsConstructor
@CommandAlias("attr")
public final class AttributeCommand extends BaseCommand {
    @Getter public final CXService service;

    @Subcommand("scale")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Scale a player by a set value")
    @Syntax("<player> <value> <durationInTicks>")
    public void onScale(Player player, String targetName, double newValue, int durationTicks) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        service.getAttributeManager().scale(target, newValue, 20L * durationTicks, true);
        player.sendMessage(Component.text(target.getName() + " will now scale to " + newValue, NamedTextColor.GREEN));

        if (!player.getUniqueId().equals(target.getUniqueId())) {
            target.sendMessage(Component.text("You will now scale to " + newValue + "x size", NamedTextColor.YELLOW));
        }
    }

    @Subcommand("set")
    @CommandCompletion("@players @attributes")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Set an individual attribute for a player with interpolation")
    @Syntax("<player> <attribute> <value> <durationInTicks>")
    public void onSetAttribute(Player player, String targetName, String attributeName, double value, int durationTicks) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        Attribute attribute;
        try {
            attribute = Attribute.valueOf(attributeName);
        } catch (IllegalArgumentException ex) {
            player.sendMessage(Component.text("Invalid attribute", NamedTextColor.RED));
            return;
        }

        service.getAttributeManager().setAttribute(target, attribute, value, durationTicks);
        player.sendMessage(Component.text(target.getName() + "'s attribute has been updated", NamedTextColor.GREEN));

        if (!player.getUniqueId().equals(target.getUniqueId())) {
            target.sendMessage(Component.text("Your attributes have been updated", NamedTextColor.YELLOW));
        }
    }

    @Subcommand("reset")
    @CommandCompletion("@players")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Reset a player's attributes to default values")
    @Syntax("<player>")
    public void onResetAttribute(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        service.getAttributeManager().resetToDefaults(target);
        player.sendMessage(Component.text("Player attributes have been reset", NamedTextColor.GREEN));

        if (!player.getUniqueId().equals(target.getUniqueId())) {
            target.sendMessage(Component.text("Your attributes have been reset by" + player.getName(), NamedTextColor.YELLOW));
        }
    }
}
