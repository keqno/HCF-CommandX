package gg.hcfactions.cx.kits.impl;

import gg.hcfactions.cx.kits.IKitExecutor;
import gg.hcfactions.cx.kits.KitManager;
import gg.hcfactions.libs.base.consumer.Promise;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Optional;

public record KitExecutor(@Getter KitManager manager) implements IKitExecutor {
    @Override
    public void giveKit(Player player, String kitName, Promise promise) {
        final Optional<Kit> existing = getManager().getKitByName(kitName);

        if (existing.isEmpty()) {
            promise.reject("Kit not found");
            return;
        }

        existing.get().give(player);
    }

    @Override
    public void giveKit(Player givingPlayer, Player receivingPlayer, String kitName, Promise promise) {
        final Optional<Kit> existing = getManager().getKitByName(kitName);

        if (existing.isEmpty()) {
            promise.reject("Kit not found");
            return;
        }

        existing.get().give(receivingPlayer);
        promise.resolve();
    }

    @Override
    public void createKit(Player player, String kitName, Promise promise) {
        if (getManager().getKitByName(kitName).isPresent()) {
            promise.reject("Kit already exists");
            return;
        }

        final Kit kit = new Kit(kitName, Arrays.asList(player.getInventory().getContents()), Arrays.asList(player.getInventory().getArmorContents()));
        manager.getKitRepository().add(kit);
        manager.saveKit(kit);
        promise.resolve();
    }

    @Override
    public void deleteKit(Player player, String kitName, Promise promise) {
        final Optional<Kit> existing = getManager().getKitByName(kitName);

        if (existing.isEmpty()) {
            promise.reject("Kit not found");
            return;
        }

        manager.getKitRepository().remove(existing.get());
        manager.deleteKit(existing.get());
        promise.resolve();
    }

    @Override
    public void listKits(Player player) {
        if (getManager().getKitRepository().isEmpty()) {
            player.sendMessage(ChatColor.RED + "No kits found");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "Kit List (" + ChatColor.YELLOW + getManager().getKitRepository().size() + " kits" + ChatColor.GOLD + ")");

        manager.getKitRepository().forEach(k -> player.spigot().sendMessage(
                new ComponentBuilder(" ")
                        .color(net.md_5.bungee.api.ChatColor.RESET)
                        .append(" - ")
                        .color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append(k.getName())
                        .color(net.md_5.bungee.api.ChatColor.BLUE)
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kit give " + k.getName()))
                        .create()));
    }
}
