package me.neoblade298.ashtabletop.thecrew.commands;

import java.util.UUID;

import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.GameSession;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public class CmdTheCrewPassTask extends Subcommand {

	// /thecrew passtask
	public CmdTheCrewPassTask(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		hidden = true;
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess == null || !(sess instanceof TheCrewInstance)) {
			Util.msgRaw(p, Component.text("You're not in a game instance of The Crew!", NamedTextColor.RED));
			return;
		}

		TheCrewInstance inst = (TheCrewInstance) sess;
		inst.passTask(p);
	}

}
