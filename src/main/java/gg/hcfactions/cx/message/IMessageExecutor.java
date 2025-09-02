package gg.hcfactions.cx.message;

import gg.hcfactions.libs.base.consumer.Promise;
import org.bukkit.entity.Player;

public interface IMessageExecutor {
    /**
     * Send a message to a player
     * @param sender Sender
     * @param receiver Receiver
     * @param message Message
     * @param promise Promise
     */
    void sendMessage(Player sender, Player receiver, String message, Promise promise);

    /**
     * Send a reply to the most recent messager
     * @param sender Sender
     * @param message Receiver
     * @param promise Promise
     */
    void sendReply(Player sender, String message, Promise promise);
}
