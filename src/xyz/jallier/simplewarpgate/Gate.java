package xyz.jallier.simplewarpgate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import java.util.logging.Level;

public class Gate {
    private Block startBlock; // This is the block that the sign is placed against. Should be middle right
    private BlockFace direction;
    private String name;

    public Gate(Block startBlock, BlockFace direction, String name) {
        this.startBlock = startBlock;
        this.direction = direction;
        this.name = name;
    }

    public Block getStartBlock() {
        return startBlock;
    }

    public BlockFace getDirection() {
        return direction;
    }

    /**
     * Instantiate a new gate, and set its various properties
     *
     * @param startBlock The block the sign was placed against
     * @return the newly created Gate
     */
    public static Gate createGate(Block startBlock, BlockFace direction, String name) {
        // TODO validate the gate
        // TODO validate the gate name
        Gate gate = new Gate(startBlock, direction, name);

        gate.clearMiddleBlocks();
        gate.addButton(gate.getSignBlock(), direction);
        gate.setSignState(gate.getSignBlock());

        return gate;
    }

    private void setSignState(Block signBlock) {
        BlockState state = signBlock.getState();
        if (!(state instanceof Sign)) {
            Bukkit.getLogger().log(Level.INFO, "Block passed is not an instance of Sign");
            return;
        }
        Sign sign = (Sign) state;
        sign.setLine(0, "§n" + name);
        // TODO get the other connected gates at this point
        sign.setLine(1, "");
        sign.setLine(2, "");
        sign.setLine(3, "Test");
        boolean updated = sign.update(true, true);

        Bukkit.getLogger().log(Level.INFO, "Updated the sign: " + updated);
        Bukkit.getLogger().log(Level.INFO, "Set gate name to " + name);
    }

    private void clearMiddleBlocks() {
        Block[] middleBlocks = getMiddleBlocks();
        for (Block block : middleBlocks) {
            block.setType(Material.AIR);
        }
    }

    private Block[] getMiddleBlocks() {
        int[][] directionIndices = getDirectionIndices(direction);
        Block[] middleBlocks = new Block[6];
        Block nextBlock = getStartBlock().getRelative(BlockFace.UP);
        middleBlocks[0] = nextBlock.getRelative(directionIndices[0][0], 0, directionIndices[1][0]);
        middleBlocks[1] = nextBlock.getRelative(directionIndices[0][1], 0, directionIndices[1][1]);
        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
        middleBlocks[2] = nextBlock.getRelative(directionIndices[0][0], 0, directionIndices[1][0]);
        middleBlocks[3] = nextBlock.getRelative(directionIndices[0][1], 0, directionIndices[1][1]);
        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
        middleBlocks[4] = nextBlock.getRelative(directionIndices[0][0], 0, directionIndices[1][0]);
        middleBlocks[5] = nextBlock.getRelative(directionIndices[0][1], 0, directionIndices[1][1]);

        return middleBlocks;
    }

    private Block getSignBlock() {
        return startBlock.getRelative(direction);
    }

    /**
     * Get how many blocks to move in each direction, depending on the direction
     *
     * @return 2d array, where first item is array of x indices and second is z indices
     */
    private static int[][] getDirectionIndices(BlockFace direction) {
        // assume north facing as default
        int[] xIndex = new int[]{1, 2, 3};
        int[] zIndex = new int[]{1, 2, 3};
        if (direction == BlockFace.NORTH) {
            zIndex[0] = 0;
            zIndex[1] = 0;
            zIndex[2] = 0;
        }
        if (direction == BlockFace.SOUTH) {
            xIndex[0] = -1;
            xIndex[1] = -2;
            xIndex[2] = -3;
            zIndex[0] = 0;
            zIndex[1] = 0;
            zIndex[2] = 0;
        }
        if (direction == BlockFace.EAST) {
            xIndex[0] = 0;
            xIndex[1] = 0;
            xIndex[2] = 0;
        }
        if (direction == BlockFace.WEST) {
            xIndex[0] = 0;
            xIndex[1] = 0;
            xIndex[2] = 0;
            zIndex[0] = -1;
            zIndex[1] = -2;
            zIndex[2] = -3;
        }
        return new int[][]{
                xIndex,
                zIndex,
        };
    }

    private void addButton(Block signBlock, BlockFace direction) {
        int[][] directionIndices = getDirectionIndices(direction);

        Block buttonBlock = signBlock.getRelative(directionIndices[0][2], 0, directionIndices[1][2]);
        buttonBlock.setType(Material.STONE_BUTTON);
    }

    /**
     * Check if the blocks can form a gate, starting from the center right block where the sign was placed
     *
     * @param startBlock block the sign was placed against
     * @param direction  direction the gate is facing
     * @return if the blocks can form a gate
     */
    public static boolean checkBlocksAreValid(Block startBlock, BlockFace direction) {
        Block topRight = startBlock.getRelative(0, 2, 0);
        return checkGate(topRight, direction);
    }

    // assuming base block is top right corner
    private static boolean checkGate(Block baseBlock, BlockFace direction) {
        Block nextBlock = baseBlock;

        // This should be a loop lol
        boolean topLine = checkGateLine(nextBlock, direction, true);
        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
        boolean firstLine = checkGateLine(nextBlock, direction, false);
        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
        boolean secondLine = checkGateLine(nextBlock, direction, false); // aka middle
        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
        boolean thirdLine = checkGateLine(nextBlock, direction, false);
        nextBlock = nextBlock.getRelative(BlockFace.DOWN);
        boolean bottomLine = checkGateLine(nextBlock, direction, true);

        return topLine && firstLine && secondLine && thirdLine && bottomLine;
    }

    private static boolean checkGateLine(Block baseBlock, BlockFace direction, boolean isEndLine) {
        // assume north facing as default
        int[][] directionIndices = getDirectionIndices(direction);
        Block mid1 = baseBlock.getRelative(directionIndices[0][0], 0, directionIndices[1][0]);
        Block mid2 = baseBlock.getRelative(directionIndices[0][1], 0, directionIndices[1][1]);
        Block opposite = baseBlock.getRelative(directionIndices[0][2], 0, directionIndices[1][2]);
        // Check block positions are correct
        if (isEndLine) {
            if (mid1.getType() != Material.OBSIDIAN) {
                return false;
            }
            if (mid2.getType() != Material.OBSIDIAN) {
                return false;
            }
        } else {
            if (baseBlock.getType() != Material.OBSIDIAN) {
                return false;
            }
            if (mid1.getType() == Material.OBSIDIAN) {
                return false;
            }
            if (mid2.getType() == Material.OBSIDIAN) {
                return false;
            }
            if (opposite.getType() != Material.OBSIDIAN) {
                return false;
            }
        }

        return true;
    }
}
