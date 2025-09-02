package gg.hcfactions.cx.warp;

import gg.hcfactions.libs.base.consumer.FailablePromise;
import gg.hcfactions.libs.base.consumer.Promise;
import org.bukkit.entity.Player;

public interface IWarpExecutor {
    /**
     * @return Warp Manager Instance
     */
    WarpManager getManager();

    /**
     * Creates a new warp
     * @param player Player (reading from thier location)
     * @param warpName Name of warp
     * @param promise Promise
     */
    void createWarp(Player player, String warpName, Promise promise);

    /**
     * Removes an existing warp
     * @param player Player
     * @param warmName Name of warp
     */
    void removeWarp(Player player, String warmName, Promise promise);

    /**
     * Lists all warps
     * @param player Player to view warps
     */
    void listWarps(Player player);

    /**
     * Create a new gateway block
     *
     * @param player Player
     * @param destinationName Warp name
     * @param promise Promise
     */
    void createGateway(Player player, String destinationName, Promise promise);

    /**
     * Modify all nearby END_GATEWAY blocks to be Gateways to the desired destination
     *
     * @param player Player
     * @param destinationName Destination warp name
     * @param radius Radius to search for End Gateway Blockd
     * @param promise Promise
     */
    void createGatewayRadius(Player player, String destinationName, int radius, FailablePromise<Integer> promise);

    /**
     * Delete a gateway and all portal blocks associated to it
     *
     * @param player Player
     * @param destinationName Warp name
     * @param promise Promise
     */
    void deleteGateway(Player player, String destinationName, Promise promise);
}
