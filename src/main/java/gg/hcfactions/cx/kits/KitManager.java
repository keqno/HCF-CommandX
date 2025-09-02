package gg.hcfactions.cx.kits;

import com.google.common.collect.Lists;
import gg.hcfactions.cx.CXService;
import gg.hcfactions.cx.kits.impl.Kit;
import gg.hcfactions.cx.kits.impl.KitExecutor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class KitManager {
    public static final String KIT_SIGN_IDENTIFIER = "[Kit]";
    public static final String FORMATTED_KIT_SIGN_IDENTIFIER = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "Kit" + ChatColor.DARK_GRAY + "]";

    @Getter public final CXService service;
    @Getter public final KitExecutor executor;
    @Getter public final List<Kit> kitRepository;

    public KitManager(CXService service) {
        this.service = service;
        this.executor = new KitExecutor(this);
        this.kitRepository = Lists.newArrayList();
    }

    public Optional<Kit> getKitByName(String kitName) {
        return kitRepository.stream().filter(k -> k.getName().equalsIgnoreCase(kitName)).findAny();
    }

    public void loadKits() {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("kits");

        if (!kitRepository.isEmpty()) {
            kitRepository.clear();
        }

        if (conf.getConfigurationSection("data") == null) {
            service.getPlugin().getAresLogger().warn("no kits found. skipping...");
            return;
        }

        for (String kitName : Objects.requireNonNull(conf.getConfigurationSection("data")).getKeys(false)) {
            final String key = "data." + kitName + ".";
            final List<ItemStack> contents = (List<ItemStack>)conf.getList(key + "contents");
            final List<ItemStack> armor = (List<ItemStack>)conf.getList(key + "armor");
            final Kit kit = new Kit(kitName, contents, armor);

            kitRepository.add(kit);
        }

        service.getPlugin().getAresLogger().info("loaded " + kitRepository.size() + " kits");
    }

    public void saveKit(Kit kit) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("kits");
        final String key = "data." + kit.getName() + ".";

        conf.set(key + "contents", kit.getContents());
        conf.set(key + "armor", kit.getArmor());

        service.getPlugin().saveConfiguration("kits", conf);
    }

    public void deleteKit(Kit kit) {
        final YamlConfiguration conf = service.getPlugin().loadConfiguration("kits");
        conf.set("data." + kit.getName(), null);
        service.getPlugin().saveConfiguration("kits", conf);
    }
}
