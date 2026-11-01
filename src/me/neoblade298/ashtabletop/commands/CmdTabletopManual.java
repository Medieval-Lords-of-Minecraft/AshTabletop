package me.neoblade298.ashtabletop.commands;

import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.neoblade298.ashtabletop.Game;
import me.neoblade298.ashtabletop.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public class CmdTabletopManual extends Subcommand {

	// /tt manual [game] {page}
	public CmdTabletopManual(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("game"));
		args.add(new Arg("page", false));
		hidden = true;
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Game g = GameManager.getGame(args[0]);
		if (g == null) {
			Util.msg(s, Component.text("That game doesn't exist! Try using /tt games to see a full list!", NamedTextColor.RED));
			return;
		}

		if (args.length > 1) {
			if (!SharedUtil.isNumeric(args[1])) {
				Util.msg(s, Component.text("Page number must be an integer!", NamedTextColor.RED));
				return;
			}
			g.displayManual((Player) s, Integer.parseInt(args[1]));
		}
		else {
			g.displayManual((Player) s);
		}
	}
}
