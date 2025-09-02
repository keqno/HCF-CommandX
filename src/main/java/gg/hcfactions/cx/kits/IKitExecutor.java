package gg.hcfactions.cx.kits;

import gg.hcfactions.libs.base.consumer.Promise;
import org.bukkit.entity.Player;

public interface IKitExecutor {
    /**
     * @return Kit Manager instance
     */
    KitManager manager();

    /**
     * Give a kit to self
     * @param player Player
     * @param kitName Kit name
     * @param promise Promise
     */
    void giveKit(Player player, String kitName, Promise promise);

    /**
     * Give a kit to the player
     * @param givingPlayer Bukkit Player
     * @param receivingPlayer Bukkit Player
     * @param kitName Kit Name
     * @param promise Promise
     */
    void giveKit(Player givingPlayer, Player receivingPlayer, String kitName, Promise promise);

    /**
     * Create a new kit
     * @param player Player
     * @param kitName Kit name
     * @param promise Promise
     */
    void createKit(Player player, String kitName, Promise promise);

    /**
     * Delete an existing kit
     * @param player Player
     * @param kitName Kit name
     * @param promise Promise
     */
    void deleteKit(Player player, String kitName, Promise promise);

    /**
     * List all available kits
     * @param player Player
     */
    void listKits(Player player);
}
