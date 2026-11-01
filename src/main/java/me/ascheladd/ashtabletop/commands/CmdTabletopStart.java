package me.ascheladd.ashtabletop.commands;

import java.util.UUID;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.ascheladd.ashtabletop.GameLobby;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTabletopStart extends Subcommand {

	// /tt start
	public CmdTabletopStart(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		hidden = true;
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
		
		if (!sess.getHost().equals(uuid)) {
			Util.msg(s, Component.text("Only the host may start the game!", NamedTextColor.RED));
			return;
		}
		
		GameLobby<? extends GamePlayer> lob = (GameLobby<? extends GamePlayer>) sess;
		lob.startGame(p);
	}

}
