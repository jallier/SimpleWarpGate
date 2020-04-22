package xyz.jallier.simplewarpgate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainListener implements Listener {
    private final Logger logger = Bukkit.getLogger();

    /**
     * Handle destroying gates
     *
     * @param blockBreakEvent event
     */
    public void onBlockBreakEvent(BlockBreakEvent blockBreakEvent) {
        Block brokenBlock = blockBreakEvent.getBlock();
        Material blockType = brokenBlock.getType();
        if (!(blockType == Material.OBSIDIAN
                || blockType == Material.OAK_WALL_SIGN
                || blockType == Material.STONE_BUTTON
        )) {
            return; // We only care about signs, stone buttons and obsidian blocks
        }

        // Check if the broken block belonged to a gate
        GateManager gateManager = GateManager.getInstance();
        Gate brokenGate = null;
        List<Gate> gates = gateManager.getActiveGates();
        for (Gate gate : gates) {
            if (Gate.checkBlocksAreValid(gate.getStartBlock(), gate.getDirection())) {
                brokenGate = gate;
            }
        }
        if (brokenGate == null) {
            return;
        }

        gateManager.removeGate(brokenGate);
    }

    /**
     * Handle the player entering the portal
     *
     * @param playerPortalEvent event
     */
    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent playerPortalEvent) {
        logger.log(Level.INFO, "Handling player portal");
        playerPortalEvent.setCancelled(true);
        Player player = playerPortalEvent.getPlayer();
        Location playerLocation = player.getLocation();
        GateManager gateManager = GateManager.getInstance();
        List<Gate> gates = gateManager.getActiveGates();
        logger.log(Level.INFO, player.getLocation() + "");
        Gate activatedGate = null;
        for (Gate gate : gates) {
            Location location = gate.getStartBlock().getLocation();
            double gateX = location.getX();
            double playerX = playerLocation.getX();
            double gateY = location.getY();
            double playerY = playerLocation.getY();
            double gateZ = location.getZ();
            double playerZ = playerLocation.getZ();
            if (
                    (playerX >= gateX - 3 && playerX <= gateX + 3) &&
                            (playerY >= gateY - 1 && playerY <= gateY + 1) &&
                            (playerZ >= gateZ - 3 && playerZ <= gateZ + 3)
            ) {
                logger.log(Level.INFO, "player within the bounds of the portal");
                activatedGate = gate;
            }
        }

        if (activatedGate == null) {
            // portal does not belong to a gate; let it process normally
            playerPortalEvent.setCancelled(false);
            return;
        }

        // Very shoddy code to ensure player is looking same direction as portal on teleport
        Gate destGate = activatedGate.getSelectedDestination();
        Location location = destGate.getStartBlock().getLocation();
        double distance = 1.25;
        switch (destGate.getDirection()) {
            case NORTH:
            case SOUTH:
                int modZ = destGate.getDirection().getModZ();
                location.setYaw(modZ < 0 ? 180F : 0f);
                location.add(modZ < 0 ? (distance + 1.0) : -distance, 0, 0.5);
                break;
            case EAST:
            case WEST:
                int modX = destGate.getDirection().getModX();
                location.setYaw(modX < 0 ? 90F : -90F);
                location.add(0.5, 0, modX < 0 ? -distance : (distance + 1));
        }

        destGate.deactivatePortal();
        player.teleport(location);
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
        GateManager gateManager = GateManager.getInstance();
        List<Gate> gates = gateManager.getActiveGates();
        Gate clickedGate = null;
        for (Gate gate : gates) {
            if (gate.buttonBelongsToGate(clickedBlock)) {
                clickedGate = gate;
                break;
            }
        }
        if (clickedGate == null) {
            // clicked sign did not belong to a gate
            return;
        }

        if (clickedGate.getSelectedDestination() == null) {
            // No destination selected
            return;
        }

        if (!clickedGate.portalIsActive()) {
            clickedGate.activatePortal();
        } else {
            clickedGate.deactivatePortal();
        }
    }

    /**
     * Handle the player clicking the sign to choose a destination
     *
     * @param playerInteractEvent event
     */
    @EventHandler
    public void onSignInteract(PlayerInteractEvent playerInteractEvent) {
        // Make sure the player is trying to use a gate
        Block clickedBlock = playerInteractEvent.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.OAK_WALL_SIGN) {
            return;
        }

        GateManager gateManager = GateManager.getInstance();
        List<Gate> gates = gateManager.getActiveGates();
        Gate clickedGate = null;
        for (Gate gate : gates) {
            if (gate.signBelongsToGate(clickedBlock)) {
                clickedGate = gate;
                break;
            }
        }
        if (clickedGate == null) {
            // clicked sign did not belong to a gate
            return;
        }

        // Now we know the sign belongs to a gate; Ask the gate to set its sign state
        clickedGate.selectDestination();
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
        if (newGate == null) {
            signChangeEvent.getPlayer().sendMessage("Could not create gate " + gateName + ". This name already exists");
            logger.log(Level.INFO, "Could not create new gate: " + gateName + ". This name already exists");
            return;
        }
        logger.log(Level.INFO, "Created new gate " + gateName);
    }
}
