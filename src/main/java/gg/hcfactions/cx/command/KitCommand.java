package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.base.consumer.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("kit")
@AllArgsConstructor
public final class KitCommand extends BaseCommand {
    @Getter public final CXService service;

    @Subcommand("give")
    @Description("Give yourself a kit")
    @Syntax("<kit>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @CommandCompletion("@kits")
    public void onGiveKit(Player player, String kitName) {
        service.getKitManager().getExecutor().giveKit(player, kitName, new Promise() {
            @Override
            public void resolve() {}

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @Subcommand("give")
    @Description("Give player a kit")
    @Syntax("<player> <kit>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @CommandCompletion("@kits")
    public void onGiveKit(Player player, String playerName, String kitName) {
        final Player otherPlayer = Bukkit.getPlayer(playerName);
        if (otherPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        service.getKitManager().getExecutor().giveKit(player, otherPlayer, kitName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.YELLOW + "Kit has been given to " + ChatColor.BLUE + otherPlayer.getName());
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @Subcommand("create")
    @Description("Create a new kit")
    @Syntax("<name>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onCreateKit(Player player, String kitName) {
        service.getKitManager().getExecutor().createKit(player, kitName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.YELLOW + "Kit created");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @Subcommand("delete|del")
    @Description("Delete an existing kit")
    @Syntax("<name>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onDeleteKit(Player player, String kitName) {
        service.getKitManager().getExecutor().deleteKit(player, kitName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.YELLOW + "Kit deleted");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @Subcommand("list")
    @Description("List all kits")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onListKit(Player player) {
        service.getKitManager().getExecutor().listKits(player);
    }
}
