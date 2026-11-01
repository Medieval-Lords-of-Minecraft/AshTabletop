package me.neoblade298.ashtabletop.thecrew.commands;

import java.util.UUID;

import me.neoblade298.neocore.bungee.BungeeCore;
import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.GameSession;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.proxy.Player;

public class CmdTheCrewMod extends Subcommand {
	private static final Component mod;
	
	static {
		Component redo = BungeeCore.miniMessage().deserialize("<dark_gray>[<red>Click to redo round</red>]");
		redo = redo.clickEvent(ClickEvent.runCommand("/thecrew restartround"));
		redo = redo.hoverEvent(Component.text("Click here!"));
		
		Component restart = BungeeCore.miniMessage().deserialize("<dark_gray>[<red>Click to restart from round 1</red>]");
		redo = redo.clickEvent(ClickEvent.runCommand("/thecrew restartgame"));
		redo = redo.hoverEvent(Component.text("Click here!"));
		
		Component lobby = BungeeCore.miniMessage().deserialize("<dark_gray>[<red>Click to return to lobby</red>]");
		redo = redo.clickEvent(ClickEvent.runCommand("/tt return"));
		redo = redo.hoverEvent(Component.text("Click here!"));
		mod = redo.append(restart.append(lobby));
	}

	// /tt kicklist
	public CmdTheCrewMod(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		hidden = true;
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess == null || !(sess instanceof GameInstance)) {
			Util.msgRaw(p, Component.text("You're not in a game instance!", NamedTextColor.RED));
			return;
		}
		
		if (!sess.getHost().equals(uuid)) {
			Util.msgRaw(p, Component.text("Only the host may access the kick list!", NamedTextColor.RED));
			return;
		}

		GameInstance<? extends GamePlayer> inst = (GameInstance<? extends GamePlayer>) sess;
		inst.displayKickList(s);
		s.sendMessage(mod);
	}

}
