package gg.hcfactions.cx.rollback;

import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.rollback.impl.RollbackInventory;
import gg.hcfactions.libs.base.util.Time;
import gg.hcfactions.libs.bukkit.scheduler.Scheduler;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RollbackManager {
    @Getter public final CXService service;
    @Getter public final Set<RollbackInventory> rollbackRepository;
    @Getter public BukkitTask expiryTask;

    public RollbackManager(CXService service) {
        this.service = service;
        this.rollbackRepository = Sets.newConcurrentHashSet();

        service.getPlugin().registerListener(new RollbackListener(this));

        expiryTask = new Scheduler(service.getPlugin()).async(() -> rollbackRepository.removeIf(RollbackInventory::isExpired)).repeat(3600*20L, 3600*20L).run();
    }

    /**
     * Returns a rollback inventory matching the provided UUID
     * @param uuid Generated UUID (not bukkit)
     * @return Optional of RollbackInventory
     */
    public Optional<RollbackInventory> getInventory(UUID uuid) {
        return rollbackRepository.stream().filter(inv -> inv.getUniqueId().equals(uuid)).findFirst();
    }

    /**
     * Returns a rollback inventory matching the provided username
     * @param username Player username
     * @return Optional of RollbackInventory
     */
    public Optional<RollbackInventory> getInventory(String username) {
        return rollbackRepository.stream().filter(inv -> inv.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    /**
     * Create a new rollback inventory instance
     * @param username Bukkit Username
     * @param contents ItemStack Contents
     * @param armor Armor Contents
     * @return RollbackInventory
     */
    public RollbackInventory createInventory(String username, List<ItemStack> contents, List<ItemStack> armor) {
        return new RollbackInventory(UUID.randomUUID(), username, contents, armor, Time.now() + (28800*1000L));
    }
}
