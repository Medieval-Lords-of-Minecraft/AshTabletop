package me.ascheladd.ashtabletop.commands;

import me.ascheladd.ashtabletop.AshTabletop;

import java.util.Optional;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CmdTabletopAdminKick extends Subcommand {

	// /tta kick [player]
	public CmdTabletopAdminKick(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player"));
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Optional<Player> opt = AshTabletop.findPlayer(args[0]);
		
		// Check if the name exists already, or player is already in a game
		GameSession<? extends GamePlayer> sess = opt.isPresent()
				? GameManager.getSession(opt.get().getUniqueId()) : GameManager.getPlayerSession(args[0]);
		if (sess == null) {
			Util.msg(s, Component.text("That player isn't in a game session!", NamedTextColor.RED));
			return;
		}

		sess.adminKickPlayer(s, args[0]);
	}

}
