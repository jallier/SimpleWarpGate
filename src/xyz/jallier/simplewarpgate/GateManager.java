package xyz.jallier.simplewarpgate;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage the state of all the gates in the worlds
 */
public class GateManager {
    private static GateManager gateManager = null;
    private List<Gate> gates;

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

    public List<Gate> getActiveGates() {
        return gates;
    }
}
