package xyz.jallier.simplewarpgate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignListener implements Listener {
    private final Logger logger = Bukkit.getLogger();
    private final String[] signOptions = new String[]{
            "Rotorua",
            "Pepega Castle",
            "Ezyvet",
            "Bradleys Hole",
            "ay lmao",
            "Can't think of other names",
    };

    /**
     * Handle the player entering the portal
     *
     * @param playerPortalEvent event
     */
    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent playerPortalEvent) {
        // Check if the portal belongs to a gate
        // TODO

        logger.log(Level.INFO, "Handling player portal");
        playerPortalEvent.setCancelled(true);
        Player player = playerPortalEvent.getPlayer();

        // Need to check if safe to teleport the user to the location
        Location playerCoords = player.getLocation();
        player.teleport(playerCoords.add(0, 3, -10));

        // Then set the portal material in the portal back to air
    }

    /**
     * Handle the player activating the gate by clicking the button
     *
     * @param playerInteractEvent event
     */
    @EventHandler
    public void onGateButtonClick(PlayerInteractEvent playerInteractEvent) {
        Block clickedBlock = playerInteractEvent.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.STONE_BUTTON) {
            return;
        }
        logger.log(Level.INFO, "Stone button was clicked");

        // Once the gate class is done, check if the button belongs to a gate first
        // TODO

        // Get the destination state from the gate
        // TODO

        // Set inner air blocks to portal material
        Block innerOne = clickedBlock.getRelative(-1, 0, 1);
        if (innerOne.getType() == Material.NETHER_PORTAL) {
            innerOne.setType(Material.AIR);
        } else {
            innerOne.setType(Material.NETHER_PORTAL);
        }
    }

    /**
     * Handle the player clicking the sign to activate the gate
     *
     * @param playerInteractEvent event
     */
    @EventHandler
    public void onSignInteract(PlayerInteractEvent playerInteractEvent) {
        // Make sure the player is trying to use a gate
        // Later we will check the stored gates sign positions, but for now, just check the material
        Block clickedBlock = playerInteractEvent.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.OAK_WALL_SIGN) {
            return;
        }
        // Now check if sign belongs to an active gate
        // TODO

        // Get the list of connected gates and list them in the sign
        // Can't do too much more without proper gate state
        Sign sign = (Sign) clickedBlock.getState();
        String[] connectedGateNames = signOptions;
        for (int i = 1; i < Math.min(connectedGateNames.length, 3 + 1); i++) {
            if (i == 1) {
                sign.setLine(i, "> " + signOptions[i - 1]);
                continue;
            }
            sign.setLine(i, signOptions[i - 1]);
        }
        sign.update();

        // Set the selected destination in the gate state
    }

    /**
     * Handle player placing the sign to create the gate
     *
     * @param signChangeEvent event
     */
    @EventHandler
    public void onSignChanged(SignChangeEvent signChangeEvent) {
        logger.log(Level.INFO, "Sign change event fired");
        logger.log(Level.INFO, "" + signChangeEvent.getBlock().getBlockData());

        // Return early if sign is wrong type
        Block sign = signChangeEvent.getBlock();
        if (sign.getType() != Material.OAK_WALL_SIGN) {
            return;
        }

        // Get the sign that changed, then get the block behind it
        WallSign wallSign = (WallSign) signChangeEvent.getBlock().getBlockData();
        BlockFace signFace = wallSign.getFacing();
        Block placedAgainst = sign.getRelative(signFace.getOppositeFace());
        if (placedAgainst.getType() != Material.OBSIDIAN) {
            return;
        }

        // Check the blocks around the sign for portal pattern
        // Assuming sign is placed on the right for now

        logger.log(Level.INFO, "Sign data " + sign.getBlockData());
        logger.log(Level.INFO, "Sign facing " + signFace);
        logger.log(Level.INFO, "Sign placed against " + placedAgainst);
        logger.log(Level.INFO, "Sign text is " + Arrays.toString(signChangeEvent.getLines()));

        Block top = placedAgainst.getRelative(0, 2, 0);
        boolean gateIsValid = checkGate(top, signFace);
        logger.log(Level.INFO, "Gate is valid: " + gateIsValid);
        if (!gateIsValid) {
            return;
        }

        // Add an activation button
        addButton(placedAgainst, signFace);

        // Test modifying the sign text
        // We will construct a gate object here to handle saving etc, and move this code to that class later
        String signName = signChangeEvent.getLine(0);
        signChangeEvent.setLine(0, "§n" + signName);
        for (int i = 0; i < 3; i++) {
            signChangeEvent.setLine(i + 1, signOptions[i]);
        }
    }

    private void addButton(Block signBlock, BlockFace direction) {
        // pls extract me to my own method
        Block opposite = null;
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

        opposite = signBlock.getRelative(xIndex[2], 0, zIndex[2]);
        Block buttonBlock = opposite.getRelative(direction);
        buttonBlock.setType(Material.STONE_BUTTON);
    }

    // assuming base block is top right corner
    private boolean checkGate(Block baseBlock, BlockFace direction) {
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

    private boolean checkGateLine(Block baseBlock, BlockFace direction, boolean isEndLine) {
        Block opposite = null;
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
        Block air1 = baseBlock.getRelative(xIndex[0], 0, zIndex[0]);
        Block air2 = baseBlock.getRelative(xIndex[1], 0, zIndex[1]);
        opposite = baseBlock.getRelative(xIndex[2], 0, zIndex[2]);
        // Check block positions are correct
        if (isEndLine) {
            if (air1.getType() != Material.OBSIDIAN) {
                return false;
            }
            if (air2.getType() != Material.OBSIDIAN) {
                return false;
            }
        } else {
            if (baseBlock.getType() != Material.OBSIDIAN) {
                return false;
            }
            if (air1.getType() != Material.AIR) {
                return false;
            }
            if (air2.getType() != Material.AIR) {
                return false;
            }
            if (opposite.getType() != Material.OBSIDIAN) {
                return false;
            }
        }

        logger.log(Level.INFO, "Gate line is valid");

        return true;
    }

}
