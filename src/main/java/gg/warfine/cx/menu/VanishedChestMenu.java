package gg.warfine.cx.menu;

import gg.warfine.libs.bukkit.AresPlugin;
import gg.warfine.libs.bukkit.menu.impl.Clickable;
import gg.warfine.libs.bukkit.menu.impl.GenericMenu;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public final class VanishedChestMenu extends GenericMenu {
    private final Inventory inventory;
    private BukkitTask updateTask;

    public VanishedChestMenu(AresPlugin plugin, Player player, Inventory inventory) {
        super(plugin, player, StringUtils.capitalize(inventory.getType().name()), inventory.getSize() / 9);
        this.inventory = inventory;
    }

    private void update() {
        clear();

        if (inventory == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Inventory no longer exists");

            updateTask.cancel();
            updateTask = null;

            return;
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            final ItemStack item = inventory.getItem(i);

            if (item == null || item.getType().equals(Material.AIR)) {
                continue;
            }

            addItem(new Clickable(item, i, click -> {
                inventory.remove(item);
                player.getInventory().addItem(item);
            }));
        }
    }

    @Override
    public void open() {
        super.open();
        addUpdater(this::update, 5L);
    }
}
