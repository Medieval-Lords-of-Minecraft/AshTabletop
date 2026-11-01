package me.neoblade298.ashtabletop.commands;

import java.util.UUID;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTabletopInvite extends Subcommand {

	// /tt invite [player]
	public CmdTabletopInvite(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player"));
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess == null || !(sess instanceof GameLobby)) {
			Util.msg(s, Component.text("You're not in a game lobby!", NamedTextColor.RED));
			return;
		}
		
		GameLobby<? extends GamePlayer> lob = (GameLobby<? extends GamePlayer>) sess;
		
		if (!sess.getHost().equals(uuid)) {
			Util.msg(s, Component.text("Only the host may invite players!", NamedTextColor.RED));
			return;
		}

		lob.invitePlayer(p, args[0]);
	}

}
