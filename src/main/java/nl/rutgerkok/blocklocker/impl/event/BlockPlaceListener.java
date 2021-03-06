package nl.rutgerkok.blocklocker.impl.event;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import nl.rutgerkok.blocklocker.BlockLockerPlugin;
import nl.rutgerkok.blocklocker.Permissions;
import nl.rutgerkok.blocklocker.Translator.Translation;
import nl.rutgerkok.blocklocker.impl.blockfinder.BlockFinder;
import nl.rutgerkok.blocklocker.location.IllegalLocationException;

public final class BlockPlaceListener extends EventListener {

    public BlockPlaceListener(BlockLockerPlugin plugin) {
        super(plugin);
    }

    private boolean isExistingChestNearby(Block chestBlock) {
        for (BlockFace blockFace : BlockFinder.CARDINAL_FACES) {
            if (chestBlock.getRelative(blockFace).getType() == Material.CHEST) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends a message that the player can protect a chest.
     *
     * @param event
     *            The block place event.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (event.getBlockPlaced().getType() != Material.CHEST) {
            return;
        }

        if (!player.hasPermission(Permissions.CAN_PROTECT)) {
            return;
        }

        if (isExistingChestNearby(event.getBlockPlaced())) {
            return;
        }

        try {
            plugin.getLocationCheckers().checkLocationAndPermission(player, event.getBlockPlaced());
        } catch (IllegalLocationException e) {
            return; // Cannot place protection here, so don't show hint
        }

        String message = plugin.getTranslator().get(Translation.PROTECTION_CHEST_HINT);
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }

}
