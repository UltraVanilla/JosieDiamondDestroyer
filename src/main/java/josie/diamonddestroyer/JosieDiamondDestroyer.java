package josie.diamonddestroyer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class JosieDiamondDestroyer extends JavaPlugin {
    public final MiniMessage mm = MiniMessage.miniMessage();

    public final Map<Material, Material> blockReplacements = new HashMap<>();
    public final Map<Material, Material> strictBlockReplacements = new HashMap<>();

    public final LongOpenHashSet alreadyChecked = new LongOpenHashSet();

    public final LongOpenHashSet alreadyCleaned = new LongOpenHashSet();

    public NamespacedKey persistentStateKey;

    public int clearingRadius = 2;

    public int alertRadius = 96;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        persistentStateKey = new NamespacedKey(this, "state_v1");
        getServer().getPluginManager().registerEvents(new TNTPrimeHandler(this), this);
    }

    private void loadConfig() {
        blockReplacements.clear();

        var config = getConfig();

        for (var key : config.getConfigurationSection("block-replacements").getKeys(false)) {
            try {
                var from = Material.valueOf(key.toUpperCase());
                var to = Material.valueOf(
                        config.getString("block-replacements." + key).toUpperCase());
                blockReplacements.put(from, to);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material in config.yml: " + key);
            }
        }

        for (var key :
                config.getConfigurationSection("strict-block-replacements").getKeys(false)) {
            try {
                var from = Material.valueOf(key.toUpperCase());
                var to = Material.valueOf(
                        config.getString("strict-block-replacements." + key).toUpperCase());
                strictBlockReplacements.put(from, to);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material in config.yml: " + key);
            }
        }

        clearingRadius = config.getInt("clearing-radius");
        alertRadius = config.getInt("alert-radius") * 16;
    }

    @Override
    public void onDisable() {}
}
