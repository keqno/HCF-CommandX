package gg.hcfactions.cx.modules;

import gg.hcfactions.cx.CXService;
import gg.hcfactions.libs.bukkit.AresPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

public interface ICXModule {
    CXService getService();
    String getKey();
    boolean isEnabled();

    void onEnable();
    void onDisable();
    default void onReload() {}

    void setEnabled(boolean b);

    default AresPlugin getPlugin() {
        return getService().getPlugin();
    }

    default YamlConfiguration getConfig() {
        return getPlugin().loadConfiguration("commandx");
    }
}
