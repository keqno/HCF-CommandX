package gg.hcfactions.cx.attributes;

import com.google.common.collect.ImmutableMap;
import gg.hcfactions.cx.CXService;
import lombok.Getter;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Objects;

@Getter
@SuppressWarnings("DataFlowIssue")
public class AttributeManager {
    public static final ImmutableMap<Attribute, Double> DEFAULT_ATTRIBUTES = ImmutableMap.<Attribute, Double>builder()
            .put(Attribute.MOVEMENT_SPEED, 0.1)
            .put(Attribute.ATTACK_SPEED, 4.0)
            .put(Attribute.GRAVITY, 0.08)
            .put(Attribute.JUMP_STRENGTH, 0.41999998688697815)
            .put(Attribute.KNOCKBACK_RESISTANCE, 0.0)
            .put(Attribute.SCALE, 1.0)
            .put(Attribute.STEP_HEIGHT, 0.6)
            .build();

    public CXService service;

    public AttributeManager(CXService service) {
        this.service = service;
    }

    /**
     * @param attribute Attribute to check against
     * @return True if we support interpolation for the provided attribute
     */
    public boolean isSupported(Attribute attribute) {
        return DEFAULT_ATTRIBUTES.containsKey(attribute);
    }

    /**
     * Reset a player to the default attribute values
     * @param player Player
     */
    public void resetToDefaults(Player player) {
        DEFAULT_ATTRIBUTES.forEach((attribute, defaultValue) -> Objects.requireNonNull(player.getAttribute(attribute)).setBaseValue(defaultValue));
    }

    /**
     * @param attribute Attribute
     * @return Default value for that attribute
     */
    public double getDefaultBaseValue(Attribute attribute) {
        if (!isSupported(attribute)) {
            service.getPlugin().getAresLogger().warn("getDefaultBaseValue returned a default value of 0.0");
            return 0.0;
        }

        return DEFAULT_ATTRIBUTES.get(attribute);
    }

    /**
     * Scale a player's attributes using interpolation
     * @param player Player
     * @param value Target Value
     * @param durationTicks Time (in ticks) to scale
     * @param modifyMovementSpeed If true we will modify the base movement speed value too
     */
    public void scale(Player player, double value, long durationTicks, boolean modifyMovementSpeed) {
        final List<Attribute> toModify = List.of(Attribute.MOVEMENT_SPEED, Attribute.GRAVITY, Attribute.JUMP_STRENGTH, Attribute.SCALE);

        for (Attribute attribute : toModify) {
            if (!DEFAULT_ATTRIBUTES.containsKey(attribute)) {
                continue;
            }

            if (!modifyMovementSpeed && attribute.equals(Attribute.MOVEMENT_SPEED)) {
                continue;
            }

            final double target = DEFAULT_ATTRIBUTES.get(attribute) * value;
            setAttribute(player, attribute, target, durationTicks);
        }
    }

    /**
     * Set a specific attribute and interpolate to it
     * @param player Player
     * @param attribute Attribute
     * @param target Target Value
     * @param durationTicks Time (in ticks) to update
     */
    public void setAttribute(Player player, Attribute attribute, double target, long durationTicks) {
        if (!isSupported(attribute)) {
            throw new UnsupportedOperationException("Unsupported Attribute: " + attribute.getKey().getKey());
        }

        if (player.getAttribute(attribute) == null) {
            throw new NullPointerException("Null Attribute: " + attribute.getKey().getKey());
        }

        double initialValue = player.getAttribute(attribute).getBaseValue();
        double diff = target - initialValue;

        new BukkitRunnable() {
            private long ticks = 0;

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    player.getAttribute(attribute).setBaseValue(target);
                    this.cancel();
                    return;
                }

                double newValue = initialValue + (diff * ticks / durationTicks);
                player.getAttribute(attribute).setBaseValue(newValue);
                ticks++;
            }
        }.runTaskTimer(service.getPlugin(), 0L, 1L);
    }
}
