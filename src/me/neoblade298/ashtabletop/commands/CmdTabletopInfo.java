package me.neoblade298.ashtabletop.commands;

import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public class CmdTabletopInfo extends Subcommand {

	// /tt info {player}
	public CmdTabletopInfo(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player", false));
		hidden = true;
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Player p = args.length > 0 ? BungeeCore.proxy().getPlayer(args[0]).get() : (Player) s;
		GameSession<? extends GamePlayer> sess = GameManager.getSession(p.getUniqueId());
		if (sess == null) {
			Util.msg(s, Component.text("Player is not currently in a session!", NamedTextColor.RED));
			return;
		}

		sess.displayInfo((Player) s, p);
	}

}
