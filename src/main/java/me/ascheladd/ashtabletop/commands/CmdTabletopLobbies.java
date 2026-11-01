package me.ascheladd.ashtabletop.commands;

import me.ascheladd.ashtabletop.AshTabletop;

import java.util.Map.Entry;
import java.util.UUID;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.commands.Subcommand;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.commands.SubcommandRunner;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.ascheladd.ashtabletop.GameLobby;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.GamePlayer;
import org.bukkit.command.CommandSender;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CmdTabletopLobbies extends Subcommand {

	// /tt lobbies
	public CmdTabletopLobbies(String key, String desc, String perm, SubcommandRunner runner) {
		super(key, desc, perm, runner);
	}

	@Override
	public void run(CommandSender s, String[] args) {
		if (GameManager.getLobbies().size() == 0) {
			Util.msg(s, Component.text("There are currently no active lobbies!", NamedTextColor.RED));
			return;
		}
		Builder b = Component.text("List of Lobbies (click to join):", NamedTextColor.GRAY).toBuilder();
		for (Entry<String, GameLobby<? extends GamePlayer>> ent : GameManager.getLobbies().entrySet()) {
			GameLobby<? extends GamePlayer> lob = ent.getValue();
			Component c = SharedUtil.color("\n<gray>- <red>" + lob.getName() + " </red>(<gold>" +
					lob.getGame().getName() + "</gold>)");
			c = c.hoverEvent(HoverEvent.showText(createHoverText(lob)));
			c = c.clickEvent(ClickEvent.runCommand("/tt join " + lob.getName()));
			b.append(c);
		}
		s.sendMessage(b.build());
	}

	private Component createHoverText(GameLobby<? extends GamePlayer> lob) {
		Player host = AshTabletop.findPlayer(lob.getHost()).get();
		Builder b = Component.text().content(lob.isPublic() ? "Public Lobby" : "Private Lobby")
				.color(lob.isPublic() ? NamedTextColor.GREEN : NamedTextColor.RED)
				.append(Component.text("\nPlayercount: ", NamedTextColor.GRAY))
				.append(Component.text(lob.getPlayers().size(), lob.isFull() ? NamedTextColor.RED : NamedTextColor.GREEN))
				.append(Component.text(" / " + lob.getGame().getMaxPlayers()))
				.append(Component.text("\n- "))
				.append(Component.text(host.getName(), NamedTextColor.RED))
				.append(Component.text(" ("))
				.append(Component.text("Host", NamedTextColor.GOLD))
				.append(Component.text(")"));
		
		for (UUID uuid : lob.getPlayers()) {
			if (uuid.equals(lob.getHost())) continue;
			
			Player p = AshTabletop.findPlayer(uuid).get();
			b.append(Component.text("\n- "))
				.append(Component.text(p.getName(), NamedTextColor.RED));
		}
		return b.build();
	}
}
