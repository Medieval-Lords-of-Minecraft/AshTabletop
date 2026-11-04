package me.ascheladd.ashtabletop.commands;

import java.util.UUID;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.ascheladd.ashtabletop.GameInstance;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTabletopSpectate extends Subcommand {

	// /tt spectate [player]
	public CmdTabletopSpectate(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("player"));
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
			Util.msg(s, Component.text("That player isn't in an active game! Maybe the game is still in lobby phase?", NamedTextColor.RED));
			return;
		}
		
		inst.addSpectator(p);
	}

}
