package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.rollback.impl.RollbackInventory;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@AllArgsConstructor
@CommandAlias("invrollback|invrb")
public final class RollbackCommand extends BaseCommand {
    @Getter public CXService service;

    @Subcommand("give")
    @CommandCompletion("@players")
    @Description("Give yourself a rollback inventory")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onGive(Player player, String username) {
        final Optional<RollbackInventory> invQuery = service.getRollbackManager().getInventory(username);

        if (invQuery.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Inventory not found");
            return;
        }

        final RollbackInventory inv = invQuery.get();

        inv.give(player);
        player.sendMessage(ChatColor.GOLD + "Rolled back " + ChatColor.RESET + inv.getUsername() + ChatColor.GOLD + "'s inventory");
    }

    @Subcommand("delete|del")
    @CommandCompletion("@players")
    @Description("Delete a rollback inventory")
    @CommandPermission(CXPermissions.CX_MOD)
    public void onDelete(Player player, String username) {
        final Optional<RollbackInventory> invQuery = service.getRollbackManager().getInventory(username);

        if (invQuery.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Inventory not found");
            return;
        }

        final RollbackInventory inv = invQuery.get();

        service.getRollbackManager().getRollbackRepository().remove(inv);
        player.sendMessage(ChatColor.YELLOW + "Rollback inventory deleted");
    }
}
