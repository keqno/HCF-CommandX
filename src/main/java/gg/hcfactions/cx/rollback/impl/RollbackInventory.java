package gg.hcfactions.cx.rollback.impl;

import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.utils.Players;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public final class RollbackInventory {
    @Getter public final UUID uniqueId;
    @Getter public final String username;
    @Getter public List<ItemStack> contents;
    @Getter public List<ItemStack> armor;
    @Getter @Setter public long expire;

    public boolean isExpired() {
        return expire <= Time.now();
    }

    public void give(Player player) {
        Players.resetHealth(player);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        int cursor = 0;
        for (ItemStack item : contents) {
            if (item != null && !item.getType().equals(Material.AIR)) {
                player.getInventory().setItem(cursor, item);
            }

            cursor += 1;
        }

        ItemStack[] armorContents = new ItemStack[armor.size()];
        armorContents = armor.toArray(armorContents);
        player.getInventory().setArmorContents(armorContents);
    }
}
