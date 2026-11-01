package me.neoblade298.ashtabletop.commands;

import java.util.UUID;

import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public class CmdTabletopReturn extends Subcommand {

	// /tt return
	public CmdTabletopReturn(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		hidden = true;
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess == null && sess instanceof GameInstance) {
			Util.msg(s, Component.text("You're not in a game!", NamedTextColor.RED));
			return;
		}
		
		if (!sess.getHost().equals(uuid)) {
			Util.msg(s, Component.text("Only the host may return everyone to lobby!", NamedTextColor.RED));
			return;
		}

		GameInstance<? extends GamePlayer> inst = (GameInstance<? extends GamePlayer>) sess;
		inst.broadcast("The host chose to return everyone to the lobby!");
		inst.endGame();
	}

}
