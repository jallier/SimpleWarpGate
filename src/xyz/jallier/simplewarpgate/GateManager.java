package xyz.jallier.simplewarpgate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Manage the state of all the gates in the worlds
 */
public class GateManager {
    private static GateManager gateManager = null;
    private final List<Gate> gates;
    private final String dataFilename = "SimpleWarpGate.dat";

    private GateManager() {
        gates = new ArrayList<>();
    }

    public static GateManager getInstance() {
        if (gateManager == null) {
            gateManager = new GateManager();
        }
        return gateManager;
    }

    /**
     * Prevent a gate from having the same name as one already in the list
     *
     * @param name name to check
     * @return if the name is unique
     */
    private boolean validateGateName(String name) {
        for (Gate gate : gates) {
            if (gate.getName().equals(name)) {
                return false;
            }
        }
        return true;
    }

    public boolean addNewGate(Gate gate) {
        if (!gates.contains(gate) && validateGateName(gate.getName())) {
            gates.add(gate);
            return true;
        }
        return false;
    }

    /**
     * Return a new list (to prevent modifying the managers list state) from the active gates currently stored
     *
     * @return list of gates
     */
    public List<Gate> getActiveGates() {
        return new ArrayList<>(gates);
    }

    /**
     * Return a new list (to prevent modifying the managers list state) from the active gates currently stored
     * Also check to see if the self gate should be removed from the returned list
     *
     * @param removeSelf if the self gate should be removed from the returned list
     * @param self       the gate to be removed from the list
     * @return list of active gates
     */
    public List<Gate> getActiveGates(boolean removeSelf, Gate self) {
        List<Gate> gates = new ArrayList<>(this.gates);
        if (removeSelf) {
            gates.remove(self);
        }
        return gates;
    }

    public void loadStateFromFile() {
        Scanner scanner = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(dataFilename);
            scanner = new Scanner(fileInputStream);

            while (scanner.hasNextLine()) {
                String info = scanner.nextLine();

                String[] gateData = info.split("::");
                String[] xyz = gateData[2].split(",");
                String name = gateData[0];
                Location location = unserializeLocation(gateData[1], xyz[0], xyz[1], xyz[2]);
                Block startBlock = Bukkit.getWorld(gateData[1]).getBlockAt(location);
                BlockFace direction = BlockFace.valueOf(gateData[3]);

                Gate gate = Gate.createGate(startBlock, direction, name);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        // Once the gates are loaded, refresh their signs
        for (Gate gate : gates) {
            gate.setInitialSignState(gate.getSignBlock());
        }
    }

    private Location unserializeLocation(String worldName, String x, String y, String z) {
        World world = Bukkit.getWorld(worldName);
        int intX = Integer.parseInt(x);
        int intY = Integer.parseInt(y);
        int intZ = Integer.parseInt(z);
        return new Location(world, intX, intY, intZ);
    }

    public void writeStateToFile() {
        String newlineChar = System.getProperty("line.separator");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(dataFilename));
            for (Gate gate : gates) {
                String output = gate.toString();
                writer.write(output + newlineChar);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
