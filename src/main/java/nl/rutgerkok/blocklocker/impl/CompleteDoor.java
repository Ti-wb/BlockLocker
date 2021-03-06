package nl.rutgerkok.blocklocker.impl;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;

import nl.rutgerkok.blocklocker.OpenBlockSound;
import nl.rutgerkok.blocklocker.protection.Protection.SoundCondition;

/**
 * Represents a single or double door. Used to find all blocks of a door, as
 * well to open or close a door.
 *
 */
public final class CompleteDoor {

    private enum Hinge {
        LEFT,
        RIGHT,
        UNKNOWN
    }

    private static Door asDoorMaterialOrNull(Block nullableBlock) {
        if (nullableBlock == null) {
            return null;
        }
        BlockData materialData = nullableBlock.getBlockData();
        if (materialData instanceof Door) {
            return (Door) materialData;
        }
        return null;
    }

    private static BlockFace getFaceToLeftDoor(Block bottomHalfDoorBlock) {
        return getFaceToRightDoor(bottomHalfDoorBlock).getOppositeFace();
    }

    private static BlockFace getFaceToRightDoor(Block bottomHalfDoorBlock) {
        Door door = asDoorMaterialOrNull(bottomHalfDoorBlock);
        if (door == null) {
            throw new RuntimeException("Block " + bottomHalfDoorBlock + " is not a door");
        }

        switch (door.getFacing()) {
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            case NORTH:
                return BlockFace.EAST;
            default:
                throw new RuntimeException("Invalid facing for door: " + door);
        }
    }

    private static Hinge getHinge(Block topHalfDoorBlock) {
        Door door = asDoorMaterialOrNull(topHalfDoorBlock);
        if (door != null) {
            return door.getHinge() == Door.Hinge.LEFT ? Hinge.LEFT : Hinge.RIGHT;
        }
        return Hinge.UNKNOWN;
    }

    private static boolean isTopHalf(Block doorBlock) {
        Door door = asDoorMaterialOrNull(doorBlock);
        if (door != null) {
            return door.getHalf() == Half.TOP;
        }
        return false;
    }

    private final Material doorMaterial;

    /** Top left block, may be null. */
    private final Block topLeftBlock;

    /** Top right block, may be null. */
    private final Block topRightBlock;
    /** Bottom left block, may be null. */
    private final Block bottomLeftBlock;
    /** Bottom right block, may be null. */
    private final Block bottomRightBlock;

    /**
     * Creates a new door. The given block must be part of the door.
     *
     * @param doorBlock
     *            A block that is part of the door.
     */
    public CompleteDoor(Block doorBlock) {
        doorMaterial = doorBlock.getType();

        Block topBlock;
        Block bottomBlock;

        if (isTopHalf(doorBlock)) {
            // Top half
            topBlock = doorBlock;
            bottomBlock = doorBlock.getRelative(BlockFace.DOWN);
        } else {
            // Bottom half
            bottomBlock = doorBlock;
            topBlock = doorBlock.getRelative(BlockFace.UP);
        }

        Hinge hinge = getHinge(topBlock);
        if (hinge == Hinge.LEFT) {
            // Hinge on the left, there may be another door on the right
            topLeftBlock = asDoorBlockOrNull(topBlock);
            bottomLeftBlock = asDoorBlockOrNull(bottomBlock);

            BlockFace faceToRightDoor = getFaceToRightDoor(bottomBlock);
            topRightBlock = asDoorBlockOrNull(topBlock.getRelative(faceToRightDoor));
            bottomRightBlock = asDoorBlockOrNull(bottomBlock.getRelative(faceToRightDoor));
        } else if (hinge == Hinge.RIGHT) {
            // Hinge on the right, there may be another door on the left
            topRightBlock = asDoorBlockOrNull(topBlock);
            bottomRightBlock = asDoorBlockOrNull(bottomBlock);

            BlockFace faceToLeftDoor = getFaceToLeftDoor(bottomBlock);
            topLeftBlock = asDoorBlockOrNull(topBlock.getRelative(faceToLeftDoor));
            bottomLeftBlock = asDoorBlockOrNull(bottomBlock.getRelative(faceToLeftDoor));
        } else {
            topLeftBlock = null;
            topRightBlock = null;
            bottomLeftBlock = null;
            bottomRightBlock = null;
        }
    }

    private Block asDoorBlockOrNull(Block nullableBlock) {
        if (nullableBlock != null && nullableBlock.getType() == doorMaterial) {
            return nullableBlock;
        }
        return null;
    }

    /**
     * Gets a collection of all blocks where attached protection signs are used
     * for this door. This includes the blocks above and below the door, and the
     * blocks on the left and right of the door.
     *
     * @return All blocks that can have protection signs attached.
     */
    public Collection<Block> getBlocksForSigns() {
        BlockFace toLeft = getFaceToLeftDoor();
        BlockFace toRight = toLeft.getOppositeFace();

        ImmutableList.Builder<Block> blocks = ImmutableList.builder();
        if (bottomLeftBlock != null) {
            blocks.add(bottomLeftBlock);
            blocks.add(bottomLeftBlock.getRelative(BlockFace.DOWN));
            blocks.add(bottomLeftBlock.getRelative(toLeft));
            if (bottomRightBlock == null) {
                // Single door, add block to the right too
                blocks.add(bottomLeftBlock.getRelative(toRight));
            }
        }
        if (bottomRightBlock != null) {
            blocks.add(bottomRightBlock);
            blocks.add(bottomRightBlock.getRelative(BlockFace.DOWN));
            blocks.add(bottomRightBlock.getRelative(toRight));
            if (bottomLeftBlock == null) {
                // Single door, add block to the left too
                blocks.add(bottomRightBlock.getRelative(toLeft));
            }
        }
        if (topLeftBlock != null) {
            blocks.add(topLeftBlock);
            blocks.add(topLeftBlock.getRelative(BlockFace.UP));
            blocks.add(topLeftBlock.getRelative(toLeft));
            if (topRightBlock == null) {
                // Single door, add block to the right too
                blocks.add(topLeftBlock.getRelative(toRight));
            }
        }
        if (topRightBlock != null) {
            blocks.add(topRightBlock);
            blocks.add(topRightBlock.getRelative(BlockFace.UP));
            blocks.add(topRightBlock.getRelative(toRight));
            if (topLeftBlock == null) {
                // Single door, add block to the left too
                blocks.add(topRightBlock.getRelative(toLeft));
            }
        }
        return blocks.build();
    }

    private BlockFace getFaceToLeftDoor() {
        Block bottomBlock = asDoorBlockOrNull(this.bottomLeftBlock);
        if (bottomBlock == null) {
            bottomBlock = asDoorBlockOrNull(this.bottomRightBlock);
            if (bottomBlock == null) {
                return BlockFace.SELF;
            }
        }

        return getFaceToLeftDoor(bottomBlock);
    }

    /**
     * Gets whether the door is currently open. The result is undefined if the
     * door is half-open, half-closed.
     *
     * @return True if the door is currently open, false otherwise.
     */
    public boolean isOpen() {
        BlockData materialData = null;
        if (bottomRightBlock != null) {
            materialData = bottomRightBlock.getBlockData();
        }
        if (bottomLeftBlock != null) {
            materialData = bottomLeftBlock.getBlockData();
        }
        return (materialData instanceof Door) && ((Door) materialData).isOpen();
    }

    private void playSound(Block bottomBlock, boolean open, SoundCondition condition) {
        if (condition == SoundCondition.NEVER) {
            return;
        }
        if (open == isOpen()) {
            return;
        }
        boolean ironDoor = bottomBlock.getType() == Material.IRON_DOOR;
        if (condition == SoundCondition.AUTOMATIC && !ironDoor) {
            return;
        }

        Sound sound = OpenBlockSound.get(bottomBlock.getType(), open);

        bottomBlock.getWorld().playSound(bottomBlock.getLocation(), sound, 1f, 0.7f);
    }

    /**
     * Opens or closes the door. If the door has been destroyed after creating
     *
     * @param open
     *            Whether the door must be opened (true) or closed (false).
     * @param soundAction
     *            Whether a sound must be played.
     */
    public void setOpen(boolean open, SoundCondition soundAction) {
        Door leftDoor = asDoorMaterialOrNull(bottomLeftBlock);
        if (leftDoor != null) {
            // Sound effect
            playSound(bottomLeftBlock, open, soundAction);

            // Don't play sound for other half
            soundAction = SoundCondition.NEVER;

            // Door toggle
            leftDoor.setOpen(open);
            bottomLeftBlock.setBlockData(leftDoor);
        }

        Door rightDoor = asDoorMaterialOrNull(bottomRightBlock);
        if (rightDoor != null) {
            // Sound effect
            playSound(bottomRightBlock, open, soundAction);

            // Door toggle
            rightDoor.setOpen(open);
            bottomRightBlock.setBlockData(rightDoor);
        }
    }

}
