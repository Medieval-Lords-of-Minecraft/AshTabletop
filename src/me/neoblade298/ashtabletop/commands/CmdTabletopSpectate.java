package me.neoblade298.ashtabletop.commands;

import java.util.UUID;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTabletopSpectate extends Subcommand {

	// /tt spectate [name]
	public CmdTabletopSpectate(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("name"));
		hidden = true;
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		if (GameManager.getSession(uuid) != null) {
			Util.msg(s, Component.text("You're already in a session! Use /tt leave", NamedTextColor.RED));
			return;
		}

		GameInstance<? extends GamePlayer> inst = GameManager.getInstance(args[0]);
		if (inst == null) {
			Util.msg(s, Component.text("Couldn't find that game instance! Maybe the game is still in lobby phase?", NamedTextColor.RED));
			return;
		}
		
		inst.addSpectator(p);
	}

}
