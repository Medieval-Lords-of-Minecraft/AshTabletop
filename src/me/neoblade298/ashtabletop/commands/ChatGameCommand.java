package me.neoblade298.ashtabletop.commands;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import me.neoblade298.ashtabletop.ChatGameInstance;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChatGameCommand implements TabExecutor {
	private final String game;

	public ChatGameCommand(String game) {
		this.game = game;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			Util.msgRaw(sender, Component.text("Only players can use this command.", NamedTextColor.RED));
			return true;
		}
		if (!player.hasPermission("tabletop.use")) {
			Util.displayError(player, "You do not have permission to play tabletop games.");
			return true;
		}
		if (!(GameManager.getSession(player.getUniqueId()) instanceof ChatGameInstance<?> instance)
				|| !instance.getGame().getKey().equals(game)) {
			Util.displayError(player, "Join a " + game + " game using /tt create " + game + " <lobby>"
					+ ", /tt join <lobby>, and /tt start. Use /tt for lobby management.");
			return true;
		}
		instance.execute(player, args);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player && args.length == 1
				&& GameManager.getSession(player.getUniqueId()) instanceof ChatGameInstance<?> instance
				&& instance.getGame().getKey().equals(game)) {
			String prefix = args[0].toLowerCase(Locale.ROOT);
			return instance.commands().stream().filter(s -> s.startsWith(prefix)).toList();
		}
		return List.of();
	}
}
