package me.neoblade298.ashtabletop.commands;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.neoblade298.ashtabletop.Game;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CmdTabletopCreate extends Subcommand {

	// /tt create [game] [name] {public/private}
	public CmdTabletopCreate(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		Arg ga = new Arg("game");
		ga.setTabOptions(new ArrayList<String>(GameManager.getGames().keySet()));
		args.add(ga);
		args.add(new Arg("name"));
		args.add(new Arg("public/private", false));
	}

	@Override
	public void run(CommandSender s, String[] args) {
		String game = args[0].toLowerCase();
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		boolean isPublic = false;
		
		if (args.length > 2) {
			if (args[2].equalsIgnoreCase("public")) {
				isPublic = true;
			}
		}
		
		// Check if the name exists already, or player is already in a game
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess != null) {
			String sessionType = sess instanceof GameLobby ? "lobby" : "instance";
			Util.msg(s, SharedUtil
					.color("<red>You're already in " + sessionType + " <yellow>" + sess.getName() 
					+ " </yellow>for game <yellow>" + sess.getGame().getName() + "</yellow>!"));
			return;
		}
		else if (GameManager.sessionExists(args[1])) {
			Util.msg(s, SharedUtil.color("<red>The name <yellow>" + args[1] + "</yellow> is already taken!"));
			return;
		}
		Game g = GameManager.getGame(game);
		if (g == null) {
			Util.msg(s, Component.text("That game doesn't exist! Try using /tt games", NamedTextColor.RED));
			return;
		}

		GameManager.createLobby(args[1], p, g, isPublic);
	}

}
