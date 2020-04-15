package xyz.jallier.simplewarpgate;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage the state of all the gates in the worlds
 */
public class GateManager {
    private static GateManager gateManager = null;
    private final List<Gate> gates;

    private GateManager() {
        gates = new ArrayList<>();
    }

    public static GateManager getInstance() {
        if (gateManager == null) {
            gateManager = new GateManager();
        }
        return gateManager;
    }

    public void addNewGate(Gate gate) {
        if (!gates.contains(gate)) {
            gates.add(gate);
        }
    }

    /**
     * Return a new list (to prevent modifying the managers list state) from the active gates currently stored
     *
     * @return list of gates
     */
    public List<Gate> getActiveGates() {
        return new ArrayList<>(gates);
    }
}
