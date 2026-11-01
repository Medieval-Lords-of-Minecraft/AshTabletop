package me.neoblade298.ashtabletop.commands;

import java.util.Map.Entry;

import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.command.CommandSource;

public class CmdTabletopInstances extends Subcommand {

	// /tt instances
	public CmdTabletopInstances(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSource s, String[] args) {
		if (GameManager.getInstances().size() == 0) {
			Util.msg(s, Component.text("There are currently no active instances!", NamedTextColor.RED));
			return;
		}
		Builder b = Component.text("List of instances (click to spectate):", NamedTextColor.GRAY).toBuilder();
		for (Entry<String, GameInstance<? extends GamePlayer>> ent : GameManager.getInstances().entrySet()) {
			GameInstance<? extends GamePlayer> inst = ent.getValue();
			Component c = SharedUtil.color("\n<gray>- <red>" + inst.getName() + " </red>(<gold>" +
					inst.getGame().getName() + "</gold>)");
			c = c.hoverEvent(HoverEvent.showText(Component.text("Click to spectate this game!")));
			c = c.clickEvent(ClickEvent.runCommand("/tt spectate " + inst.getName()));
			b.append(c);
		}
		s.sendMessage(b.build());
	}
}
