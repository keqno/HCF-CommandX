package gg.warfine.cx.modules;

import gg.warfine.cx.CXService;
import gg.warfine.libs.bukkit.AresPlugin;
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
