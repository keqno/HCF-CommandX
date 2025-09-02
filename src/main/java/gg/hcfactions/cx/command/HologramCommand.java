package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.hologram.EHologramOrder;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.*;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

@AllArgsConstructor
@CommandAlias("hologram|holo")
public final class HologramCommand extends BaseCommand {
    @Getter public CXService service;

    @Subcommand("create")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Create a new hologram")
    @Syntax("<text>")
    public void onCreate(Player player, @Values("ascending|descending") String orderName, String text) {
        EHologramOrder order = EHologramOrder.DESCENDING;
        try {
            order = EHologramOrder.valueOf(orderName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage("Invalid order (ASCENDING/DESCENDING), skipping...");
        }

        service.getHologramManager().getExecutor().createHologram(new PLocatable(player), text, order);
    }

    @Subcommand("addline")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Add a line to an existing hologram")
    @Syntax("<id> <text>")
    public void onAddLine(Player player, String hidName, String text) {
        int hid;
        try {
            hid = Integer.parseInt(hidName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid hologram id");
            return;
        }

        service.getHologramManager().getExecutor().addLineToHologram(hid, text, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.GREEN + "Line added");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to add line: " + s);
            }
        });
    }

    @Subcommand("updateline")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Update a line for an existing hologram")
    @Syntax("<id> <index> <text>")
    public void onUpdateLine(Player player, String hidName, String indexName, String text) {
        int hid;
        try {
            hid = Integer.parseInt(hidName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid hologram id");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(indexName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid index");
            return;
        }

        service.getHologramManager().getExecutor().updateLineForHologram(hid, index, text, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.GREEN + "Line updated");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to update line: " + s);
            }
        });
    }

    @Subcommand("remline|removeline")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Remove a line from an existing hologram")
    @Syntax("<id> <index>")
    public void onRemoveLine(Player player, String hidName, String indexName) {
        int hid;
        try {
            hid = Integer.parseInt(hidName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid hologram id");
            return;
        }

        int index;
        try {
            index = Integer.parseInt(indexName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid index");
            return;
        }

        service.getHologramManager().getExecutor().removeLineFromHologram(hid, index, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.GREEN + "Line removed");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to remove line: " + s);
            }
        });
    }

    @Subcommand("delete")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Delete a hologram")
    @Syntax("<id>")
    public void onDelete(Player player, String hidName) {
        int hid;
        try {
            hid = Integer.parseInt(hidName);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid hologram id");
            return;
        }

        service.getHologramManager().getExecutor().deleteHologram(hid, new Promise() {
            @Override
            public void resolve() {
                player.sendMessage(ChatColor.GREEN + "Hologram deleted");
            }

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + "Failed to delete hologram: " + s);
            }
        });
    }

    @Subcommand("reload")
    @CommandPermission(CXPermissions.CX_ADMIN)
    @Description("Reload holograms")
    public void onReload(CommandSender sender) {
        service.getHologramManager().reloadHolograms();
        sender.sendMessage(ChatColor.GREEN + "Holograms reloaded");
    }
}
