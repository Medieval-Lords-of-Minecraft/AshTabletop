package me.ascheladd.ashtabletop.thecrew.commands;

import java.util.UUID;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.Arg;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.GameSession;
import me.ascheladd.ashtabletop.thecrew.TheCrewCard.SonarType;
import me.ascheladd.ashtabletop.thecrew.TheCrewInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewPlayer;
import me.ascheladd.ashtabletop.thecrew.tasks.CardMatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdTheCrewSonar extends Subcommand {

	// /thecrew sonar [card matcher] [min/max/only]
	public CmdTheCrewSonar(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
		args.add(new Arg("card matcher"));
		args.add(new Arg("sonar type"));
		hidden = true;
	}

	@Override
	public void run(CommandSender s, String[] args) {
		Player p = (Player) s;
		UUID uuid = p.getUniqueId();
		GameSession<? extends GamePlayer> sess = GameManager.getSession(uuid);
		if (sess == null || !(sess instanceof TheCrewInstance)) {
			Util.msgRaw(p, Component.text("You're not in a game instance of The Crew!", NamedTextColor.RED));
			return;
		}

		TheCrewInstance inst = (TheCrewInstance) sess;
		TheCrewPlayer tcp = inst.getPlayers().get(p.getName().toLowerCase());
		tcp.useSonarToken(new CardMatcher(args[0]), SonarType.valueOf(args[1]));
	}

}
