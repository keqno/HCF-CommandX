package gg.hcfactions.cx.hologram;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.hologram.impl.Hologram;
import gg.hcfactions.cx.hologram.impl.HologramExecutor;
import gg.hcfactions.libs.bukkit.location.impl.PLocatable;
import gg.hcfactions.libs.bukkit.utils.Configs;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
public final class HologramManager {
    public CXService service;
    public HologramExecutor executor;
    public Set<Hologram> hologramRepository;
    public AtomicInteger nextId;

    public HologramManager(CXService service) {
        this.service = service;
        this.executor = new HologramExecutor(this);
        this.hologramRepository = Sets.newHashSet();
    }

    public Optional<Hologram> getHologram(Predicate<Hologram> pred) {
        return hologramRepository.stream().filter(pred).findFirst();
    }

    public List<Hologram> getHolograms(Predicate<Hologram> pred) {
        return hologramRepository.stream().filter(pred).collect(Collectors.toList());
    }

    /**
     * Loads all holograms in to memory from file
     */
    public void loadHolograms() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("holograms");

        nextId = new AtomicInteger(conf.getInt("next_id"));

        if (conf.get("data") == null) {
            service.getPlugin().getAresLogger().warn("holograms.yml is empty. skipping...");
            return;
        }

        for (String hid : Objects.requireNonNull(conf.getConfigurationSection("data")).getKeys(false)) {
            final int hologramId = Integer.parseInt(hid);
            final List<Component> formatted = Lists.newArrayList();
            final List<String> hologramLines = conf.getStringList("data." + hid + ".text");
            final PLocatable location = Configs.parsePlayerLocation(conf, "data." + hid + ".location");
            final EHologramOrder order = EHologramOrder.valueOf(conf.getString("data." + hid + ".order"));

            hologramLines.forEach(line -> formatted.add(service.getPlugin().getMiniMessage().deserialize(line)));

            final Hologram hologram = new Hologram(service, hologramId, formatted, location, order);
            hologramRepository.add(hologram);
        }

        service.getPlugin().getAresLogger().info("Loaded {} Holograms", hologramRepository.size());
    }

    /**
     * Save all holograms in memory to file
     */
    public void saveHolograms() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("holograms");

        conf.set("next_id", nextId.intValue());

        for (Hologram holo : hologramRepository) {
            final List<String> serialized = Lists.newArrayList();
            holo.getText().forEach(component -> serialized.add(service.getPlugin().getMiniMessage().serialize(component)));

            conf.set("data." + holo.getId() + ".text", serialized);
            conf.set("data." + holo.getId() + ".order", holo.getOrder().name());
            Configs.writePlayerLocation(conf, "data." + holo.getId() + ".location", holo.getOrigin());
        }

        service.getPlugin().saveConfiguration("holograms", conf);
        service.getPlugin().getAresLogger().info("Saved {} holograms", hologramRepository.size());
    }

    /**
     * Delete the provided hologram from file
     * @param hologram Hologram
     */
    public void deleteHologram(Hologram hologram) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("holograms");
        conf.set("data." + hologram.getId(), null);
        service.getPlugin().saveConfiguration("holograms", conf);
        service.getPlugin().getAresLogger().info("Deleted hologram ({})", hologram.getId());
    }

    /**
     * Despawns all holograms, reloads data from file and spawns all holograms
     */
    public void reloadHolograms() {
        despawnHolograms();
        loadHolograms();
        spawnHolograms();
    }

    /**
     * Spawns all holograms in memory
     */
    public void spawnHolograms() {
        service.getPlugin().getAresLogger().info("Spawning {} holograms", hologramRepository.size());
        hologramRepository.forEach(Hologram::spawn);
    }

    /**
     * Despawns all holograms in memory
     */
    public void despawnHolograms() {
        service.getPlugin().getAresLogger().info("Despawning {} holograms", hologramRepository.size());

        hologramRepository.forEach(Hologram::despawn);
        hologramRepository.clear();
    }
}
