package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.warp.impl.Warp;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.base.consumer.FailablePromise;
import gg.hcfactions.libs.base.consumer.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@AllArgsConstructor
@CommandAlias("warp")
public final class WarpCommand extends BaseCommand {
    @Getter public CXService service;

    @Default
    @Description("Teleport to a warp")
    @CommandPermission(CXPermissions.CX_MOD)
    @Syntax("<name>")
    @CommandCompletion("@warps")
    public void onWarp(Player player, String warpName) {
        final Optional<Warp> warp = service.getWarpManager().getWarp(warpName);

        if (warp.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Warp not found");
            return;
        }

        warp.get().teleport(player);
        player.sendMessage(ChatColor.YELLOW + "Teleported to " + ChatColor.BLUE + warp.get().getName());
    }

    @Subcommand("create")
    @Description("Create a new warp")
    @Syntax("<name>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    public void onCreateWarp(Player player, String warpName) {
        service.getWarpManager().getExecutor().createWarp(player, warpName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.YELLOW + "Warp created");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @Subcommand("delete|del|remove|rem")
    @Description("Delete a warp")
    @Syntax("<name>")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @CommandCompletion("@warps")
    public void onRemoveWarp(Player player, String warpName) {
        service.getWarpManager().getExecutor().removeWarp(player, warpName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.YELLOW + "Warp removed");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @Subcommand("list")
    @Description("List all warps")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onListWarp(Player player) {
        service.getWarpManager().getExecutor().listWarps(player);
    }

    @Subcommand("gateway create")
    @Description("Create a new Warp Gateway Block")
    @CommandPermission(CXPermissions.CX_MOD)
    @CommandCompletion("@warps")
    public void onGatewayCreate(Player player, String warpName) {
        service.getWarpManager().getExecutor().createGateway(player, warpName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.GREEN + "Gateway linked");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to create: " + s);
            }
        });
    }

    @Subcommand("gateway create radius")
    @Description("Update any nearby End Gateway blocks to become a Warp Gateway Block")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @CommandCompletion("@warps")
    @Syntax("<radius> <destination>")
    public void onGatewayRadiusCreate(Player player, int radius, String warpName) {
        service.getWarpManager().getExecutor().createGatewayRadius(player, warpName, radius, new FailablePromise<>() {
            @Override
            public void resolve(Integer integer) {
                player.sendMessage(ChatColor.GREEN + "Updated " + integer + " gateway blocks");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to update gateway blocks: " + s);
            }
        });
    }

    @Subcommand("gateway delete")
    @Description("Delete an existing gateway")
    @CommandPermission(CXPermissions.CX_MOD)
    @CommandCompletion("@warps")
    public void onGatewayDelete(Player player, String warpName) {
        service.getWarpManager().getExecutor().deleteGateway(player, warpName, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.YELLOW + "Unlinked all gateways associated to " + warpName);
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to delete gateway: " + s);
            }
        });
    }
}
