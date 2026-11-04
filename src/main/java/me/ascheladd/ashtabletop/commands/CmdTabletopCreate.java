package me.ascheladd.ashtabletop.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.ascheladd.ashtabletop.Game;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CmdTabletopCreate extends Subcommand {

	// /tt create [game] {public/private}
	public CmdTabletopCreate(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		Arg ga = new Arg("game");
		ga.setTabOptions(new ArrayList<String>(GameManager.getGames().keySet()));
		args.add(ga);
		Arg visibility = new Arg("public/private", false);
		visibility.setTabOptions(new ArrayList<String>(List.of("public", "private")));
		args.add(visibility);
	}

	@Override
	public void run(CommandSender s, String[] args) {
		String game = args[0].toLowerCase(Locale.ROOT);
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		
		if (args.length > 2 || (args.length == 2 && !args[1].equalsIgnoreCase("public")
				&& !args[1].equalsIgnoreCase("private"))) {
			Util.displayError(p, "Use /tt create <game> [public|private]. Lobby names are no longer used.");
			return;
		}
		boolean isPublic = args.length == 2 && args[1].equalsIgnoreCase("public");
		
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess != null) {
			Util.msg(s, SharedUtil
					.color("<red>You're already in <yellow>" + sess.getName() + "</yellow>!"));
			return;
		}
		Game g = GameManager.getGame(game);
		if (g == null) {
			Util.msg(s, Component.text("That game doesn't exist! Try using /tt games", NamedTextColor.RED));
			return;
		}

		GameManager.createLobby(p, g, isPublic);
	}

}
