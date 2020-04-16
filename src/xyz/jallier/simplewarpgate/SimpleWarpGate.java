package xyz.jallier.simplewarpgate;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class SimpleWarpGate extends JavaPlugin {
    @Override
    public void onEnable() {
        this.getLogger().log(Level.INFO, "Loading SimpleWarpGate...");
        GateManager.getInstance().loadStateFromFile();
        this.getLogger().log(Level.INFO, "Reading the data storage file");

        GateManager gateManager = GateManager.getInstance();
        // Load the gates or whatever here

        this.getCommand("ping").setExecutor(new CommandPing());
        this.getServer().getPluginManager().registerEvents(new SignListener(), this);

        this.getLogger().log(Level.INFO, "SimpleWarpGate loaded!");
    }

    @Override
    public void onDisable() {
        GateManager.getInstance().writeStateToFile();
        this.getLogger().log(Level.INFO, "Wrote the data storage file");
    }
}
