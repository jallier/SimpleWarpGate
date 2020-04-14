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
    // TODO Handle breaking sign deactivating gate

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
    public void onSignChange(SignChangeEvent signChangeEvent) {
        // Return early if sign is wrong type
        Block sign = signChangeEvent.getBlock();
        if (sign.getType() != Material.OAK_WALL_SIGN) {
            return;
        }

        // Get the sign that changed, then get the block behind it
        WallSign wallSign = (WallSign) signChangeEvent.getBlock().getBlockData();
        BlockFace signFaceDirection = wallSign.getFacing();
        Block placedAgainst = sign.getRelative(signFaceDirection.getOppositeFace());
        if (placedAgainst.getType() != Material.OBSIDIAN) {
            return;
        }
        logger.log(Level.INFO, "Sign placed against valid portal material; checking");

        // Check the blocks around the sign for portal pattern
        // Assuming sign is placed on the right for now
        boolean gateIsValid = Gate.checkBlocksAreValid(placedAgainst, signFaceDirection);
        if (!gateIsValid) {
            logger.log(Level.INFO, "Gate is not valid");
            return;
        }

        // Cancel the event to make the sign text update correctly
        signChangeEvent.setCancelled(true);
        String gateName = signChangeEvent.getLine(0);
        if (gateName == null || gateName.equals("")) {
            logger.log(Level.INFO, "All gates require a name in the top line of the sign");
            return;
        }

        logger.log(Level.INFO, "Creating new gate: " + gateName);
        Gate newGate = Gate.createGate(placedAgainst, signFaceDirection, gateName);
        // TODO Add save here once GateManager implemented
        logger.log(Level.INFO, "Created new gate " + gateName);
    }
}
