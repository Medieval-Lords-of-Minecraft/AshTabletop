package me.neoblade298.ashtabletop.commands;

import java.util.Optional;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

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

public class CmdTabletopAdminKick extends Subcommand {

	// /tta kick [player]
	public CmdTabletopAdminKick(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player"));
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Optional<Player> opt = BungeeCore.proxy().getPlayer(args[0]);
		
		if (opt.isEmpty()) {
			Util.msg(s, Component.text("That player isn't online!", NamedTextColor.RED));
			return;
		}
		
		// Check if the name exists already, or player is already in a game
		UUID uuid = opt.get().getUniqueId();
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess == null) {
			Util.msg(s, Component.text("That player isn't in a game session!", NamedTextColor.RED));
			return;
		}

		sess.adminKickPlayer(s, opt.get().getUsername());
	}

}
