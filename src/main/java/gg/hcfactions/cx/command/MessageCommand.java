package gg.hcfactions.cx.command;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.acf.BaseCommand;
import gg.hcfactions.libs.acf.annotation.CommandAlias;
import gg.hcfactions.libs.acf.annotation.CommandCompletion;
import gg.hcfactions.libs.acf.annotation.Description;
import gg.hcfactions.libs.acf.annotation.Syntax;
import gg.hcfactions.libs.base.consumer.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@AllArgsConstructor
@CommandAlias("message|msg|tell")
public class MessageCommand extends BaseCommand {
    @Getter public CXService service;

    @CommandAlias("message|msg|tell")
    @Description("Send a player a private message")
    @Syntax("<username> <message>")
    @CommandCompletion("@players")
    public void onMessage(Player player, String username, String message) {
        final Player otherPlayer = Bukkit.getPlayer(username);
        if (otherPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        service.getMessageManager().getExecutor().sendMessage(player, otherPlayer, message, new Promise() {
            @Override
            public void resolve() {}

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }

    @CommandAlias("reply|r")
    @Description("Reply to your most recently received message")
    @Syntax("<message>")
    public void onReply(Player player, String message) {
        service.getMessageManager().getExecutor().sendReply(player, message, new Promise() {
            @Override
            public void resolve() {}

            @Override
            public void reject(String s) {
                player.sendMessage(ChatColor.RED + s);
            }
        });
    }
}
