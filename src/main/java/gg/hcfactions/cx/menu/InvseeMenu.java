package gg.hcfactions.cx.menu;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.builder.impl.ItemBuilder;
import gg.hcfactions.libs.bukkit.menu.impl.Clickable;
import gg.hcfactions.libs.bukkit.menu.impl.GenericMenu;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

@Getter
public final class InvseeMenu extends GenericMenu {
    public final CXService service;
    public final Player observed;
    public BukkitTask updateTask;

    public InvseeMenu(CXService service, Player player, Player observed) {
        super(service.getPlugin(), player, observed.getName(), 6);
        this.service = service;
        this.observed = observed;
    }

    private void update() {
        clear();

        if (observed.isDead() || !observed.isOnline()) {
            if (this.updateTask != null) {
                this.updateTask.cancel();
                this.updateTask = null;
            }

            player.closeInventory();
            player.sendMessage(ChatColor.RED + observed.getName() + " is no longer available");

            return;
        }

        final List<String> potionEffects = Lists.newArrayList();

        observed.getActivePotionEffects().forEach(effect -> potionEffects.add(
                ChatColor.DARK_AQUA + StringUtils.capitalize(effect.getType().getName().toLowerCase().replace("_", " ")) + ChatColor.GRAY + ": " +
                        ChatColor.WHITE + Time.convertToHHMMSS((effect.getDuration() / 20) * 1000L)));

        final ItemStack health = new ItemBuilder()
                .setMaterial(Material.GLISTERING_MELON_SLICE)
                .setName(ChatColor.RED + "Health")
                .addLore(ChatColor.YELLOW + "" + String.format("%.1f", (observed.getHealth() / 2)) + ChatColor.GOLD + "/" + ChatColor.YELLOW + "10.0")
                .build();

        final ItemStack food = new ItemBuilder()
                .setMaterial(Material.COOKED_BEEF)
                .setName(ChatColor.GOLD + "Food")
                .addLore(ChatColor.YELLOW + "" + (observed.getFoodLevel() / 2) + ChatColor.GOLD + "/" + ChatColor.YELLOW + "10")
                .build();

        final ItemStack potions = new ItemBuilder()
                .setMaterial(Material.GLASS_BOTTLE)
                .setName(ChatColor.AQUA + "Potions")
                .addLore(potionEffects)
                .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .build();

        for (int i = 0; i < observed.getInventory().getSize(); i++) {
            final ItemStack item = observed.getInventory().getItem(i);

            if (item == null || item.getType().equals(Material.AIR)) {
                continue;
            }

            addItem(new Clickable(item, i, click -> {
                observed.getInventory().removeItem(item);
                player.getInventory().addItem(item);

                observed.sendMessage(ChatColor.BLUE + player.getName() + ChatColor.GOLD + " transferred an item from your inventory to theirs");
                player.sendMessage(ChatColor.BLUE + observed.getName() + ChatColor.GOLD + "'s item has been transferred to your inventory");
            }));
        }

        int cursor = 45;
        for (ItemStack item : observed.getInventory().getArmorContents()) {
            if (item == null || item.getType().equals(Material.AIR)) {
                continue;
            }

            addItem(new Clickable(item, cursor, click -> player.sendMessage(ChatColor.RED + "You can not transfer player armor")));

            cursor += 1;
        }

        addItem(new Clickable(health, 51, click -> {}));
        addItem(new Clickable(food, 52, click -> {}));
        addItem(new Clickable(potions, 53, click -> {}));
    }

    @Override
    public void open() {
        super.open();
        addUpdater(this::update, 5L);
    }
}
