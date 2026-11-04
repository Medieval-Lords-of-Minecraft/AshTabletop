package me.ascheladd.ashtabletop.commands;

import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.ascheladd.ashtabletop.GameInstance;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class CmdTabletopInstances extends Subcommand {

	// /tt instances
	public CmdTabletopInstances(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSender s, String[] args) {
		if (GameManager.getInstances().size() == 0) {
			Util.msg(s, Component.text("There are currently no active instances!", NamedTextColor.RED));
			return;
		}
		Builder b = Component.text("List of instances (click to spectate):", NamedTextColor.GRAY).toBuilder();
		for (GameInstance<? extends GamePlayer> inst : GameManager.getInstances().values()) {
			Component c = SharedUtil.color("\n<gray>- <red>" + inst.getName() + "</red>");
			c = c.hoverEvent(HoverEvent.showText(Component.text("Click to spectate this game!")));
			c = c.clickEvent(ClickEvent.runCommand("/tt spectate " + inst.getHostName()));
			b.append(c);
		}
		s.sendMessage(b.build());
	}
}
