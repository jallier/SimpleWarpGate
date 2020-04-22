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

/**
 * Represents a Gate object which is used to check if the shape is correct and manage the state of each individual gate
 */
public class Gate {
    /**
     * This is the block the sign is placed against. Should be middle right
     */
    private final Block startBlock;
    /**
     * Direction of the gate
     */
    private final BlockFace direction;
    /**
     * Name of the gate
     */
    private final String name;

    /**
     * The position of the selected destination gate cursor
     */
    private int cursorIndex;
    /**
     * The start of the 3 displayed destination gates
     */
    private int gateListWindowIndex;
    /**
     * The selected destination
     */
    private Gate selectedDestination;
    /**
     * If the portal should show the portal material
     */
    private boolean portalActive;

    /**
     * Construct a new gate
     *
     * @param startBlock The block that the sign was placed against
     * @param direction  The direction of the gate (and sign)
     * @param name       The name of the gate
     */
    public Gate(Block startBlock, BlockFace direction, String name) {
        this.startBlock = startBlock;
        this.direction = direction;
        this.name = name;
        cursorIndex = 0;
        gateListWindowIndex = 0;
        selectedDestination = null;
        portalActive = false;
    }

    /**
     * Create a string representation of the gate. Used to store its state to file
     *
     * @return string representing the gate state
     */
    @Override
    public String toString() {
        String world = startBlock.getWorld().getName();
        int x = startBlock.getX();
        int y = startBlock.getY();
        int z = startBlock.getZ();
        String direction = this.direction.name();
        return String.format("%s::%s::%s,%s,%s::%s", name, world, x, y, z, direction);
    }

    /**
     * Get name
     *
     * @return gate name
     */
    public String getName() {
        return name;
    }

    /**
     * Get start block
     *
     * @return start block
     */
    public Block getStartBlock() {
        return startBlock;
    }

    /**
     * Get direction
     *
     * @return direction
     */
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

    /**
     * Set the state of the display sign after the gate has been created for the first time
     *
     * @param signBlock The sign block to operate on
     */
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
    }

    /**
     * Set the middle blocks of the gate to air
     */
    private void clearMiddleBlocks() {
        Block[] middleBlocks = getMiddleBlocks();
        for (Block block : middleBlocks) {
            block.setType(Material.AIR);
        }
    }

    /**
     * Find the middle blocks of the gate and return them
     *
     * @return array of the middle blocks of the gate
     */
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

    /**
     * Set the portal to active, and set the middle blocks to portal material
     */
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

    /**
     * Set the portal to inactive and set the middle blocks to air
     */
    public void deactivatePortal() {
        Block[] middleBlocks = getMiddleBlocks();
        for (Block block : middleBlocks) {
            block.setType(Material.AIR);
        }
        portalActive = false;
    }

    /**
     * Return if portal is active
     *
     * @return if portal is active
     */
    public boolean portalIsActive() {
        return portalActive;
    }

    /**
     * Return the button block (the actual button, not the block it is attached to)
     *
     * @return button block
     */
    private Block getButtonBlock() {
        int[][] directionIndices = getDirectionIndices(direction);
        return getSignBlock().getRelative(directionIndices[0][2], 0, directionIndices[1][2]);
    }

    /**
     * Check if a button belongs to the gate
     *
     * @param button the button block to check
     * @return If the button does belong to the gate
     */
    public boolean buttonBelongsToGate(Block button) {
        return getButtonBlock().getLocation().equals(button.getLocation());
    }

    /**
     * Get sign block
     *
     * @return the sign block
     */
    public Block getSignBlock() {
        return startBlock.getRelative(direction);
    }

    /**
     * Get sign block state
     *
     * @return sign block state
     */
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

    /**
     * Render the destinations on the display sign. Since signs can only display 3 lines, we must scroll through the destinations as needed
     *
     * @param sign           the sign to render on
     * @param destinations   the list of total destinations
     * @param cursorPosition the position of the selection cursor
     * @param listPosition   the position of the start of the list of destinations to display
     */
    private void renderDisplay(Sign sign, List<Gate> destinations, int cursorPosition, int listPosition) {
        int size = destinations.size();
        if (size == 0) {
            return;
        }
        int index = listPosition;
        for (int i = 1; i < 4; i++) {
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

    /**
     * Check if a gate is valid. To be valid it must have the same shape and materials as a vanilla nether portal
     *
     * @param baseBlock the start block to check. This must be the top right corner of the gate
     * @param direction the direction the gate is facing
     * @return if the gate is valid or not
     */
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

    /**
     * Check if a line of the gate is valid
     *
     * @param baseBlock the start block. This should be the rightmost block in the line
     * @param direction the direction of the gate
     * @param isEndLine if the line is the top of bottom of the gate
     * @return if the line is valid
     */
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
            if (opposite.getType() != Material.OBSIDIAN) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get selected destination
     *
     * @return selected destionation
     */
    public Gate getSelectedDestination() {
        return selectedDestination;
    }
}
