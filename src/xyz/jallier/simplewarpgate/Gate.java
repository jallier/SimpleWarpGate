package xyz.jallier.simplewarpgate;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;

import java.util.List;
import java.util.logging.Level;

public class Gate {
    /**
     * This is the block the sign is placed against. Should be middle right
     */
    private final Block startBlock;
    private final BlockFace direction;
    private final String name;

    private int cursorIndex;
    private int gateListWindowIndex;
    private Gate selectedDestination;
    private boolean portalActive;

    public Gate(Block startBlock, BlockFace direction, String name) {
        this.startBlock = startBlock;
        this.direction = direction;
        this.name = name;
        cursorIndex = 0;
        gateListWindowIndex = 0;
        selectedDestination = null;
        portalActive = false;
    }

    @Override
    public String toString() {
        String world = startBlock.getWorld().getName();
        int x = startBlock.getX();
        int y = startBlock.getY();
        int z = startBlock.getZ();
        String direction = this.direction.name();
        return String.format("%s::%s::%s,%s,%s::%s", name, world, x, y, z, direction);
    }

    public String getName() {
        return name;
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

        // Gate now created. Get an instance of the manager and add the new gate to it
        GateManager manager = GateManager.getInstance();
        if (!manager.addNewGate(gate)) {
            return null;
        }
        gate.clearMiddleBlocks();
        gate.addButton(gate.getSignBlock(), direction);
        gate.setInitialSignState(gate.getSignBlock());

        return gate;
    }

    public void setInitialSignState(Block signBlock) {
        BlockState state = signBlock.getState();
        if (!(state instanceof Sign)) {
            Bukkit.getLogger().log(Level.INFO, "Block passed is not an instance of Sign");
            return;
        }
        Sign sign = (Sign) state;
        sign.setLine(0, "§n" + name);

        GateManager gateManager = GateManager.getInstance();
        List<Gate> gates = gateManager.getActiveGates(true, this);
        renderDisplay(sign, gates, cursorIndex, gateListWindowIndex);
        sign.update();

        Bukkit.getLogger().log(Level.INFO, "Updated the sign");
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

    public void activatePortal() {
        if (selectedDestination == null) { // Only allow if destination has already been set
            return;
        }
        Block[] middleBlocks = getMiddleBlocks();
        for (Block block : middleBlocks) {
            block.setType(Material.NETHER_PORTAL);
            if (direction.equals(BlockFace.EAST) || direction.equals(BlockFace.WEST)) {
                Orientable orientation = (Orientable) block.getBlockData();
                orientation.setAxis(Axis.Z);
                block.setBlockData(orientation);
            }
        }
        portalActive = true;
    }

    public void deactivatePortal() {
        Block[] middleBlocks = getMiddleBlocks();
        for (Block block : middleBlocks) {
            block.setType(Material.AIR);
        }
        portalActive = false;
    }

    public boolean portalIsActive() {
        return portalActive;
    }

    private Block getButtonBlock() {
        int[][] directionIndices = getDirectionIndices(direction);
        return getSignBlock().getRelative(directionIndices[0][2], 0, directionIndices[1][2]);
    }

    public boolean buttonBelongsToGate(Block button) {
        return getButtonBlock().getLocation().equals(button.getLocation());
    }

    public Block getSignBlock() {
        return startBlock.getRelative(direction);
    }

    private Sign getSignBlockState() {
        return (Sign) getSignBlock().getState();
    }

    /**
     * Check if a passed in sign block belongs to this gate
     *
     * @param sign the sign to check
     * @return if the sign belongs to the gate
     */
    public boolean signBelongsToGate(Block sign) {
        return getSignBlock().getLocation().equals(sign.getLocation());
    }

    /**
     * Cycle the destinations on the sign and set the selected Gate
     */
    public void selectDestination() {
        GateManager gateManager = GateManager.getInstance();
        List<Gate> gates = gateManager.getActiveGates(true, this);
        Sign sign = getSignBlockState();

        int windowEnd = gateListWindowIndex + 2;
        int listEndPos = gates.size() - 1;

        if (gates.size() > cursorIndex && cursorIndex < 3) { // Move the curson down
            cursorIndex++;
            renderDisplay(sign, gates, cursorIndex, gateListWindowIndex);
        } else if (windowEnd < listEndPos) { // Scroll the list up without moving the cursor
            gateListWindowIndex++;
            renderDisplay(sign, gates, cursorIndex, gateListWindowIndex);
        } else { // Reset everything to the top
            cursorIndex = 1;
            gateListWindowIndex = 0;
            renderDisplay(sign, gates, cursorIndex, gateListWindowIndex);
        }

        int gateListIndex = gateListWindowIndex + Math.max(cursorIndex, 1) - 1;
        Gate destinationGate = gates.get(gateListIndex);
        selectedDestination = destinationGate;
        Bukkit.getLogger().log(Level.INFO, "Destination gate set to: " + destinationGate.getName());
    }

    private void renderDisplay(Sign sign, List<Gate> destinations, int cursorPosition, int listPosition) {
        int size = destinations.size();
        if (size == 0) {
            return;
        }
        int index = listPosition;
        for (int i = 1; i < 4; i++) {
//            Bukkit.getLogger().log(Level.INFO, "Index is: " + index);
            String name;
            if (index < size) {
                Gate gate = destinations.get(index);
                name = gate.getName();
            } else {
                name = "";
            }
            if (i == cursorPosition) {
                name = ">" + name;
            }
            sign.setLine(i, name);
            index++;
        }
        sign.update();
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
        Directional data = (Directional) buttonBlock.getBlockData();
        data.setFacing(direction);
        buttonBlock.setBlockData(data);
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

    public Gate getSelectedDestination() {
        return selectedDestination;
    }
}
