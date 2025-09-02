package gg.hcfactions.cx.hologram.impl;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.hologram.EHologramOrder;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

@Getter
public final class Hologram {
    public CXService service;
    public int id;
    public List<Component> text;
    public PLocatable origin;
    public EHologramOrder order;

    public Hologram(CXService service, int id, List<Component> text, PLocatable origin, EHologramOrder order) {
        this.service = service;
        this.id = id;
        this.origin = origin;
        this.order = order;
        this.text = Lists.newArrayList(text);
    }

    /**
     * Spawn the entity(s) in to the world
     */
    public void spawn() {
        double offset = 0.0;

        for (Component line : this.text) {
            final double newY = (order.equals(EHologramOrder.DESCENDING)) ? origin.getY() - offset : origin.getY() + offset;
            final ArmorStand as = (ArmorStand) Objects.requireNonNull(origin.getBukkitLocation().getWorld()).spawnEntity(
                    new PLocatable(origin.getWorldName(), origin.getX(), newY, origin.getZ(), origin.getYaw(), origin.getPitch()).getBukkitLocation(),
                    EntityType.ARMOR_STAND
            );

            as.customName(line);
            as.setInvisible(true);
            as.setCustomNameVisible(true);
            as.setGravity(false);
            as.setCollidable(false);
            as.getPersistentDataContainer().set(service.getNamespacedKey(), PersistentDataType.STRING, "hologram");

            offset += 0.3;
        }
    }

    /**
     * Despawn all entities associated with this hologram from the world
     */
    public void despawn() {
        final double searchRadius = text.size()*0.3;

        for (Entity nearby : Objects.requireNonNull(origin.getBukkitLocation().getWorld()).getNearbyEntities(origin.getBukkitLocation(), 1.0, searchRadius, 1.0)) {
            if (!(nearby instanceof ArmorStand)) {
                continue;
            }

            nearby.remove();
        }
    }

    /**
     * Add a new line to this hologram
     * @param newText New text to be added
     */
    public void addLine(Component newText) {
        final double newY = order.equals(EHologramOrder.DESCENDING) ? origin.getY() - (text.size()*0.3) : origin.getY() + (text.size()*0.3);
        final ArmorStand as = (ArmorStand) Objects.requireNonNull(origin.getBukkitLocation().getWorld()).spawnEntity(
                new PLocatable(origin.getWorldName(), origin.getX(), newY, origin.getZ(), origin.getYaw(), origin.getPitch()).getBukkitLocation(),
                EntityType.ARMOR_STAND
        );

        as.customName(newText);
        as.setCustomNameVisible(true);
        as.setGravity(false);
        as.setInvisible(true);
        as.setCollidable(false);
        as.getPersistentDataContainer().set(service.getNamespacedKey(), PersistentDataType.STRING, "hologram");

        text.add(newText);
    }

    /**
     * Update an existing line on this hologram
     * @param index Index
     * @param newText New text to be added
     * @return True if update was performed
     */
    public boolean updateLine(int index, Component newText) {
        if (index >= text.size()) {
            addLine(newText);
            return true;
        }

        final Component current = text.get(index);
        final double searchRadius = text.size()*0.3;
        String currentName = PlainTextComponentSerializer.plainText().serialize(current);

        for (Entity nearby : Objects.requireNonNull(origin.getBukkitLocation().getWorld()).getNearbyEntities(origin.getBukkitLocation(), 1.0, searchRadius, 1.0)) {
            if (!(nearby instanceof final ArmorStand as)) {
                continue;
            }

            if (as.customName() == null) {
                continue;
            }

            String nearbyName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(as.customName()));

            if (!nearbyName.equals(currentName)) {
                continue;
            }

            as.customName(newText);
            text.set(index, newText);
            return true;
        }

        return false;
    }

    /**
     * Remove an existing line from this hologram
     * @param index Index
     * @return True if update was performed
     */
    public boolean removeLine(int index) {
        if (index > text.size()) {
            return false;
        }

        final Component line = text.get(index);
        final double searchRadius = text.size()*0.3;
        String lineText = PlainTextComponentSerializer.plainText().serialize(line);

        for (Entity nearby : Objects.requireNonNull(origin.getBukkitLocation().getWorld()).getNearbyEntities(origin.getBukkitLocation(), 1.0, searchRadius, 1.0)) {
            if (!(nearby instanceof final ArmorStand as)) {
                continue;
            }

            if (as.customName() == null) {
                continue;
            }

            String nearbyText = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(as.customName()));

            if (!nearbyText.equals(lineText)) {
                continue;
            }

            as.remove();
            return true;
        }

        return false;
    }
}
