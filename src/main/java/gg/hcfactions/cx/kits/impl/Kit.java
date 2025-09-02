package gg.hcfactions.cx.kits.impl;

import gg.hcfactions.libs.bukkit.utils.Players;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@AllArgsConstructor
public final class Kit {
    @Getter public final String name;
    @Getter public final List<ItemStack> contents;
    @Getter public final List<ItemStack> armor;

    public void give(Player player) {
        give(player, true);
    }

    public void give(Player player, boolean message) {
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

        if (message) {
            player.sendMessage(ChatColor.YELLOW + "You have received the " + ChatColor.BLUE + name + ChatColor.YELLOW + " kit");
        }
    }
}
