package josie.diamonddestroyer;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;

public class TNTPrimeHandler implements Listener {
    private final JosieDiamondDestroyer plugin;

    public TNTPrimeHandler(JosieDiamondDestroyer pl) {
        this.plugin = pl;
    }

    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {
        if (event.getCause() != TNTPrimeEvent.PrimeCause.REDSTONE) return;

        var block = event.getBlock();

        var chunkX = block.getX() >> 4;
        var chunkZ = block.getZ() >> 4;
        var chunkKey = Chunk.getChunkKey(chunkX, chunkZ);

        if (plugin.alreadyChecked.contains(chunkKey)) return;
        plugin.alreadyChecked.add(chunkKey);

        if (isTntFromDuping(block)) {
            var location = block.getLocation();
            plugin.getLogger()
                    .info("Found TNT duping at " + location.toString()
                            + ", performing block replacements to ensure fairness");

            var world = block.getWorld();
            var worldMinHeight = world.getMinHeight();
            var worldMaxHeight = world.getMaxHeight();

            Map<Material, Integer> replacementCounts = new EnumMap<>(Material.class);

            for (var checkingZ = chunkZ - plugin.clearingRadius;
                    checkingZ <= chunkZ + plugin.clearingRadius;
                    checkingZ++) {
                for (var checkingX = chunkX - plugin.clearingRadius;
                        checkingX <= chunkX + plugin.clearingRadius;
                        checkingX++) {
                    var checkingChunkKey = Chunk.getChunkKey(checkingX, checkingZ);

                    if (plugin.alreadyCleaned.contains(checkingChunkKey)) continue;

                    var chunk = world.getChunkAt(checkingChunkKey);

                    for (var y = worldMinHeight; y < worldMaxHeight; y++) {
                        for (var x = 0; x < 16; x++) {
                            for (var z = 0; z < 16; z++) {
                                var originalBlock = chunk.getBlock(x, y, z);
                                var blockType = originalBlock.getType();

                                if (plugin.blockReplacements.containsKey(blockType)) {
                                    replaceBlockAndEmitChange(
                                            world, originalBlock, replacementCounts, plugin.blockReplacements);
                                } else if (plugin.strictBlockReplacements.containsKey(blockType)
                                        && isSolidlyEncased(originalBlock)) {
                                    replaceBlockAndEmitChange(
                                            world, originalBlock, replacementCounts, plugin.strictBlockReplacements);
                                }
                            }
                        }
                    }

                    plugin.alreadyCleaned.add(checkingChunkKey);
                    chunk.getPersistentDataContainer().set(plugin.persistentStateKey, PersistentDataType.BOOLEAN, true);
                }
            }

            if (!replacementCounts.isEmpty()) {
                var message = plugin.getConfig().getString("message-start");
                var first = true;
                for (var entry : replacementCounts.entrySet()) {
                    if (!first) message += ", ";
                    first = false;
                    message += entry.getValue().toString();
                    message += " ";
                    message += entry.getKey().name();
                }
                message += plugin.getConfig().getString("message-end");

                for (var entity : world.getNearbyPlayers(location, plugin.alertRadius, 320, plugin.alertRadius)) {
                    entity.sendMessage(plugin.mm.deserialize(message));
                }
            }
        }
    }

    private static boolean isTntFromDuping(Block block) {
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

    private static boolean isSolidlyEncased(Block block) {
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

    private static void replaceBlockAndEmitChange(
            World world, Block block, Map<Material, Integer> counter, Map<Material, Material> replacementMap) {
        var blockType = block.getType();
        var replacement = replacementMap.get(blockType);
        new EntityChangeBlockEvent(getFirstEnderman(world), block, Bukkit.createBlockData(replacement)).callEvent();
        counter.put(blockType, counter.getOrDefault(blockType, 0) + 1);
        block.setType(replacementMap.get(blockType));
    }

    public static Enderman getFirstEnderman(World world) {
        for (var entity : world.getEntities()) {
            if (entity instanceof Enderman) {
                return (Enderman) entity;
            }
        }
        return null;
    }
}
