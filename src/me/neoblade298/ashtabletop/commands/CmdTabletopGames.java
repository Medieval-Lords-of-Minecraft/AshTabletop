package me.neoblade298.ashtabletop.commands;

import java.util.Map.Entry;

import com.velocitypowered.api.command.CommandSource;

import me.neoblade298.neocore.bungee.commands.Subcommand;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.ashtabletop.Game;
import me.neoblade298.ashtabletop.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class CmdTabletopGames extends Subcommand {

	// /tt games
	public CmdTabletopGames(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSource s, String[] args) {
		Builder b = Component.text().content("List of games:").color(NamedTextColor.GRAY);
		for (Entry<String, Game> ent : GameManager.getGames().entrySet()) {
			b.appendNewline();
			Component c = Component.text("- ", NamedTextColor.GRAY)
					.append(Component.text(ent.getKey(), NamedTextColor.RED));
			
			Component hover = Component.text(ent.getValue().getName(), NamedTextColor.GOLD)
					.append(Component.text("\nClick for more information!", NamedTextColor.WHITE));
			c = c.hoverEvent(HoverEvent.showText(hover));
			
			c = c.clickEvent(ClickEvent.runCommand("/tt viewgame " + ent.getValue().getKey()));
			b.append(c);
		}
		s.sendMessage(b.build());
	}
}
