package gg.hcfactions.cx.hologram.impl;

import gg.hcfactions.cx.hologram.EHologramOrder;
import gg.hcfactions.cx.hologram.HologramManager;
import gg.hcfactions.cx.hologram.IHologramExecutor;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Collections;

@Getter
@AllArgsConstructor
public final class HologramExecutor implements IHologramExecutor {
    public HologramManager manager;

    @Override
    public void createHologram(PLocatable location, String initialText, EHologramOrder order) {
        final Hologram holo = new Hologram(
                manager.getService(),
                manager.getNextId().getAndIncrement(),
                Collections.singletonList(manager.getService().getPlugin().getMiniMessage().deserialize(initialText)),
                location,
                order
        );

        manager.getHologramRepository().add(holo);
        manager.saveHolograms();

        holo.spawn();
    }

    @Override
    public void addLineToHologram(int hologramId, String text, Promise promise) {
        final Hologram holo = manager.getHologramRepository().stream().filter(h -> h.getId() == hologramId).findFirst().orElse(null);

        if (holo == null) {
            promise.reject("Hologram not found");
            return;
        }

        holo.addLine(manager.getService().getPlugin().getMiniMessage().deserialize(text));
        manager.saveHolograms();
        promise.resolve();
    }

    @Override
    public void removeLineFromHologram(int hologramId, int index, Promise promise) {
        final Hologram holo = manager.getHologramRepository().stream().filter(h -> h.getId() == hologramId).findFirst().orElse(null);

        if (holo == null) {
            promise.reject("Hologram not found");
            return;
        }

        final boolean removed = holo.removeLine(index);

        if (!removed) {
            promise.reject("Failed to remove line (out of bounds or no match)");
            return;
        }

        manager.saveHolograms();
        promise.resolve();
    }

    @Override
    public void updateLineForHologram(int hologramId, int index, String newText, Promise promise) {
        final Hologram holo = manager.getHologramRepository().stream().filter(h -> h.getId() == hologramId).findFirst().orElse(null);

        if (holo == null) {
            promise.reject("Hologram not found");
            return;
        }

        final boolean updated = holo.updateLine(index, manager.getService().getPlugin().getMiniMessage().deserialize(newText));

        if (!updated) {
            promise.reject("Failed to remove line (out of bounds or no match)");
            return;
        }

        manager.saveHolograms();
        promise.resolve();
    }

    @Override
    public void deleteHologram(int hologramId, Promise promise) {
        final Hologram holo = manager.getHologramRepository().stream().filter(h -> h.getId() == hologramId).findFirst().orElse(null);

        if (holo == null) {
            promise.reject("Hologram not found");
            return;
        }

        holo.despawn();
        manager.getHologramRepository().remove(holo);
        manager.deleteHologram(holo);
        promise.resolve();
    }
}
