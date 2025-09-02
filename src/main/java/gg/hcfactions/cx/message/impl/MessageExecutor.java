package gg.hcfactions.cx.message.impl;

import gg.hcfactions.cx.CXPermissions;
import gg.hcfactions.cx.message.IMessageExecutor;
import gg.hcfactions.cx.message.MessageManager;
import gg.hcfactions.libs.base.consumer.Promise;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import gg.hcfactions.libs.bukkit.services.impl.account.AccountService;
import gg.hcfactions.libs.bukkit.services.impl.account.model.AresAccount;
import gg.hcfactions.libs.bukkit.services.impl.punishments.PunishmentService;
import gg.hcfactions.libs.bukkit.services.impl.punishments.model.EPunishmentType;
import gg.hcfactions.libs.bukkit.services.impl.punishments.model.impl.Punishment;
import gg.hcfactions.libs.bukkit.services.impl.ranks.RankService;
import gg.hcfactions.libs.bukkit.services.impl.ranks.model.impl.AresRank;
import gg.hcfactions.libs.bukkit.utils.Players;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public record MessageExecutor(@Getter MessageManager manager) implements IMessageExecutor {
    @Override
    public void sendMessage(Player sender, Player receiver, String message, Promise promise) {
        final AccountService acs = (AccountService)manager.getService().getPlugin().getService(AccountService.class);
        final PunishmentService ps = (PunishmentService)manager.getService().getPlugin().getService(PunishmentService.class);
        final RankService rs = (RankService)manager.getService().getPlugin().getService(RankService.class);
        final boolean admin = sender.hasPermission(CXPermissions.CX_MOD);

        if (acs == null) {
            promise.reject("Failed to obtain Account Service");
            return;
        }

        if (ps == null) {
            promise.reject("Failed to obtain Punishment Service");
            return;
        }

        if (rs == null) {
            promise.reject("Failed to obtain Rank Service");
            return;
        }

        if (receiver == null) {
            promise.reject("Player not found");
            return;
        }

        if (receiver.getUniqueId().equals(sender.getUniqueId())) {
            promise.reject("You can not message yourself");
            return;
        }

        final AresAccount senderAccount = acs.getCachedAccount(sender.getUniqueId());
        final AresAccount receiverAccount = acs.getCachedAccount(receiver.getUniqueId());

        if (senderAccount == null) {
            promise.reject("Failed to obtain your account");
            return;
        }

        if (receiverAccount == null) {
            promise.reject("Player not found");
            return;
        }

        if (!senderAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED)) {
            promise.reject("You have disabled private messages");
            return;
        }

        if (senderAccount.getSettings().isIgnoring(receiver)) {
            promise.reject("You are ignoring this player");
            return;
        }

        if (!admin && (receiverAccount.getSettings().isIgnoring(sender.getUniqueId()) || !receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED))) {
            promise.reject("This player has private messages disabled");
            return;
        }

        new Scheduler(manager.getService().getPlugin()).async(() -> {
            final Punishment activeMute = ps.getActivePunishmentByType(sender.getUniqueId(), EPunishmentType.MUTE);

            new Scheduler(manager.getService().getPlugin()).sync(() -> {
                if (activeMute != null) {
                    promise.reject("You can not perform this action while muted");
                    return;
                }

                final AresRank senderRank = rs.getHighestRank(sender);
                final AresRank receiverRank = rs.getHighestRank(receiver);
                final String senderName = (senderRank != null) ? net.md_5.bungee.api.ChatColor.of(senderRank.getColorCode()) + sender.getName() : ChatColor.RESET + sender.getName();
                final String receiverName = (receiverRank != null) ? net.md_5.bungee.api.ChatColor.of(receiverRank.getColorCode()) + receiver.getName() : ChatColor.RESET + receiver.getName();

                receiver.sendMessage(ChatColor.GRAY + "(From " + senderName + ChatColor.GRAY + "): " + ChatColor.RESET + message);
                sender.sendMessage(ChatColor.GRAY + "(To " + receiverName + ChatColor.GRAY + "): " + ChatColor.RESET + message);

                if (receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_PING_ENABLED)) {
                    Players.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_PLING);
                }

                manager.setRecentlyMessaged(sender, receiver);
                promise.resolve();
            }).run();
        }).run();
    }

    @Override
    public void sendReply(Player sender, String message, Promise promise) {
        final AccountService acs = (AccountService)manager.getService().getPlugin().getService(AccountService.class);
        final PunishmentService ps = (PunishmentService)manager.getService().getPlugin().getService(PunishmentService.class);
        final RankService rs = (RankService)manager.getService().getPlugin().getService(RankService.class);
        final Optional<UUID> replyId = manager.getRecentlyMessaged(sender);

        if (acs == null) {
            promise.reject("Failed to obtain Account Service");
            return;
        }

        if (ps == null) {
            promise.reject("Failed to obtain Punishment Service");
            return;
        }

        if (rs == null) {
            promise.reject("Failed to obtain Rank Service");
            return;
        }

        if (replyId.isEmpty()) {
            promise.reject("Nobody has recently messaged you");
            return;
        }

        final Player receiver = Bukkit.getPlayer(replyId.get());
        final boolean admin = sender.hasPermission(CXPermissions.CX_MOD);

        if (receiver == null) {
            promise.reject("Player not found");
            return;
        }

        final AresAccount senderAccount = acs.getCachedAccount(sender.getUniqueId());
        final AresAccount receiverAccount = acs.getCachedAccount(receiver.getUniqueId());

        if (senderAccount == null) {
            promise.reject("Failed to obtain your account");
            return;
        }

        if (receiverAccount == null) {
            promise.reject("Player not found");
            return;
        }

        if (!senderAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED)) {
            promise.reject("You have disabled private messages");
            return;
        }

        if (senderAccount.getSettings().isIgnoring(receiver.getUniqueId())) {
            promise.reject("You are ignoring this player");
            return;
        }

        if (!admin && (receiverAccount.getSettings().isIgnoring(sender.getUniqueId()) || !receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_ENABLED))) {
            promise.reject("This player has private messages disabled");
            return;
        }

        new Scheduler(manager.getService().getPlugin()).async(() -> {
            final Punishment activeMute = ps.getActivePunishmentByType(sender.getUniqueId(), EPunishmentType.MUTE);

            new Scheduler(manager.getService().getPlugin()).sync(() -> {
                if (activeMute != null) {
                    promise.reject("You can not perform this action while muted");
                    return;
                }

                final AresRank senderRank = rs.getHighestRank(sender);
                final AresRank receiverRank = rs.getHighestRank(receiver);
                final String senderName = (senderRank != null) ? net.md_5.bungee.api.ChatColor.of(senderRank.getColorCode()) + sender.getName() : ChatColor.RESET + sender.getName();
                final String receiverName = (receiverRank != null) ? net.md_5.bungee.api.ChatColor.of(receiverRank.getColorCode()) + receiver.getName() : ChatColor.RESET + receiver.getName();

                receiver.sendMessage(ChatColor.GRAY + "(From " + senderName + ChatColor.GRAY + "): " + ChatColor.RESET + message);
                sender.sendMessage(ChatColor.GRAY + "(To " + receiverName + ChatColor.GRAY + "): " + ChatColor.RESET + message);

                if (receiverAccount.getSettings().isEnabled(AresAccount.Settings.SettingValue.PRIVATE_MESSAGES_PING_ENABLED)) {
                    Players.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_PLING);
                }

                manager.setRecentlyMessaged(sender, receiver);
                promise.resolve();
            }).run();
        }).run();
    }
}
