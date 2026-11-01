package me.ascheladd.ashtabletop.thecrew.commands;

import java.util.UUID;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.ascheladd.ashtabletop.GameInstance;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.GameSession;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CmdTheCrewMod extends Subcommand {
	private static final Component mod;
	
	static {
		Component redo = NeoCore.miniMessage().deserialize("<dark_gray>[<red>Click to redo round</red>]");
		redo = redo.clickEvent(ClickEvent.runCommand("/thecrew restartround"));
		redo = redo.hoverEvent(Component.text("Click here!"));
		
		Component restart = NeoCore.miniMessage().deserialize("<dark_gray>[<red>Click to restart from round 1</red>]");
		redo = redo.clickEvent(ClickEvent.runCommand("/thecrew restartgame"));
		redo = redo.hoverEvent(Component.text("Click here!"));
		
		Component lobby = NeoCore.miniMessage().deserialize("<dark_gray>[<red>Click to return to lobby</red>]");
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
	public void run(CommandSender s, String[] args) {
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
