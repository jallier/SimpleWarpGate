package xyz.jallier.simplewarpgate;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class CommandPing implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player || commandSender instanceof ConsoleCommandSender) {
            commandSender.sendMessage("PONG!");
        }
        return true;
    }
}
