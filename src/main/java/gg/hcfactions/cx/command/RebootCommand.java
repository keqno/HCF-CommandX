package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.base.consumer.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@CommandAlias("reboot")
@AllArgsConstructor
public final class RebootCommand extends BaseCommand {
    @Getter public final CXService service;

    @Subcommand("start")
    @Description("Start a reboot")
    @Syntax("[seconds]")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onStart(CommandSender sender, @Optional String namedTime) {
        int s = 60;

        if (namedTime != null) {
            try {
                s = Integer.parseInt(namedTime);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid time format (seconds only)");
                return;
            }
        }

        service.getRebootModule().startReboot(s);
    }

    @Subcommand("cancel|stop")
    @Description("Stop a reboot")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onCancel(CommandSender sender) {
        service.getRebootModule().cancelReboot();
    }

    @Subcommand("reschedule|schedule")
    @Description("Reschedule a reboot")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Syntax("<time>")
    public void onReschedule(CommandSender sender, String namedTime) {
        service.getRebootModule().rescheduleReboot(namedTime, new Promise() {
            @Override
            public void resolve() {
                sender.sendMessage(ChatColor.GREEN + "Reboot has been rescheduled");
            }

            @Override
            public void reject(String s) {
                sender.sendMessage(ChatColor.RED + "Failed to reschedule reboot: " + s);
            }
        });
    }

    @Subcommand("info")
    @Description("See the next reboot time")
    public void onInfo(CommandSender sender) {
        service.getRebootModule().printReboot(sender);
    }
}
