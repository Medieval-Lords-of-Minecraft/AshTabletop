package me.ascheladd.ashtabletop.commands;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.ascheladd.ashtabletop.Game;
import me.ascheladd.ashtabletop.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTabletopManual extends Subcommand {

	// /tt manual [game] {page}
	public CmdTabletopManual(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("game"));
		args.add(new Arg("page", false));
		hidden = true;
	}

	@Override
	public void run(CommandSender s, String[] args) {
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
