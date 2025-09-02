package gg.hcfactions.cx.hologram;

import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;

public interface IHologramExecutor {
    /**
     * Create a new hologram
     * @param location Origin Location
     * @param initialText Initial text rendered on line 0
     * @param order Order
     */
    void createHologram(PLocatable location, String initialText, EHologramOrder order);

    /**
     * Add a new line to an existing hologram
     * @param hologramId Hologram ID
     * @param text Text to add
     * @param promise Promise
     */
    void addLineToHologram(int hologramId, String text, Promise promise);

    /**
     * Remove an existing line from a hologram
     * @param hologramId Hologram ID
     * @param index Index position to remove
     * @param promise Promise
     */
    void removeLineFromHologram(int hologramId, int index, Promise promise);

    /**
     * Update an existing line on a hologram
     * @param hologramid Hologram ID
     * @param index Index position to update
     * @param newText Next text to set
     * @param promise Promise
     */
    void updateLineForHologram(int hologramid, int index, String newText, Promise promise);

    /**
     * Delete all armor stands associated with the provided hologram ID
     * @param hologramId Hologram ID
     * @param promise Promise
     */
    void deleteHologram(int hologramId, Promise promise);
}
