package josie.diamonddestroyer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;

public class TNTPrimeHandler implements Listener {
    private final JosieDiamondDestroyer plugin;

    public TNTPrimeHandler(JosieDiamondDestroyer pl) {
        this.plugin = pl;
    }

    private final LongOpenHashSet alreadyChecked = new LongOpenHashSet();

    private final LongOpenHashSet alreadyCleaned = new LongOpenHashSet();

    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {
        if (event.getCause() != TNTPrimeEvent.PrimeCause.REDSTONE) return;

        var block = event.getBlock();

        var chunkX = block.getX() >> 4;
        var chunkZ = block.getZ() >> 4;
        var chunkKey = Chunk.getChunkKey(chunkX, chunkZ);

        if (alreadyChecked.contains(chunkKey)) return;
        alreadyChecked.add(chunkKey);

        if (isTntFromDuping(block)) {
            var location = block.getLocation();
            plugin.getLogger()
                    .info("Found TNT duping at " + location.toString()
                            + ", performing block replacements to ensure fairness");

            var world = block.getWorld();
            var worldMinHeight = world.getMinHeight();
            var worldMaxHeight = world.getMaxHeight();

            var radius = plugin.clearingRadius;

            Map<Material, Integer> replacementCounts = new EnumMap<>(Material.class);

            for (var checkingZ = chunkZ - radius; checkingZ <= chunkZ + radius; checkingZ++) {
                for (var checkingX = chunkX - radius; checkingX <= chunkX + radius; checkingX++) {
                    var checkingChunkKey = Chunk.getChunkKey(checkingX, checkingZ);

                    if (alreadyCleaned.contains(checkingChunkKey)) continue;

                    var chunk = world.getChunkAt(checkingChunkKey);

                    for (int y = worldMinHeight; y < worldMaxHeight; y++) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                var originalBlock = chunk.getBlock(x, y, z);
                                var blockType = originalBlock.getType();

                                if (plugin.blockReplacements.containsKey(blockType)) {
                                    replacementCounts.put(blockType, replacementCounts.getOrDefault(blockType, 0) + 1);
                                    originalBlock.setType(plugin.blockReplacements.get(blockType));
                                } else if (plugin.strictBlockReplacements.containsKey(blockType)
                                        && isSolidlyEncased(originalBlock)) {
                                    replacementCounts.put(blockType, replacementCounts.getOrDefault(blockType, 0) + 1);
                                    originalBlock.setType(plugin.strictBlockReplacements.get(blockType));
                                }
                            }
                        }
                    }

                    alreadyCleaned.add(checkingChunkKey);
                }
            }

            if (replacementCounts.isEmpty()) return;

            var message = plugin.getConfig().getString("message-start");
            var first = true;
            for (Map.Entry<Material, Integer> entry : replacementCounts.entrySet()) {
                if (!first) message += ", ";
                first = false;
                message += entry.getValue().toString();
                message += " ";
                message += entry.getKey().name();
            }
            message += plugin.getConfig().getString("message-end");

            for (Player entity : world.getNearbyPlayers(location, plugin.alertRadius, 320, plugin.alertRadius)) {
                entity.sendMessage(plugin.mm.deserialize(message));
            }
        }
    }

    private boolean isTntFromDuping(Block block) {
        for (var yOff = -1; yOff <= 1; yOff++) {
            for (var zOff = -1; zOff <= 1; zOff++) {
                for (var xOff = -1; xOff <= 1; xOff++) {

                    var otherBlock = block.getRelative(xOff, yOff, zOff);

                    if (otherBlock.getType() == Material.MOVING_PISTON) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSolidlyEncased(Block block) {
        for (var yOff = -1; yOff <= 1; yOff++) {
            for (var zOff = -1; zOff <= 1; zOff++) {
                for (var xOff = -1; xOff <= 1; xOff++) {
                    var otherBlock = block.getRelative(xOff, yOff, zOff);

                    if (otherBlock.getType() == Material.AIR) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
