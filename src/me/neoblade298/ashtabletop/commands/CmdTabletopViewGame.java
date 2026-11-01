package me.neoblade298.ashtabletop.commands;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.Game;
import me.neoblade298.ashtabletop.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTabletopViewGame extends Subcommand {

	// /tt viewgame [game]
	public CmdTabletopViewGame(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("game"));
		hidden = true;
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Game g = GameManager.getGame(args[0]);
		if (g == null) {
			Util.msg(s, Component.text("That game doesn't exist! Try using /tt games", NamedTextColor.RED));
			return;
		}

		g.displayInfo((Player) s);
	}
}
