package me.ascheladd.ashtabletop;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class GameInstance<T extends GamePlayer> extends GameSession<T> {
	protected HashMap<String, T> players = new HashMap<String, T>();
	protected HashSet<Player> spectators = new HashSet<Player>();
	public GameInstance(GameLobby<T> lobby) {
		super(lobby.getName(), lobby.getGame(), lobby.getHost(), lobby.isPublic(), lobby.getParameters());
	}
	
	public abstract void handleLeave(GamePlayer gp);

	protected void runLater(Runnable task, long seconds) {
		AshTabletop.scheduler().runTaskLater(AshTabletop.inst(), () -> {
			if (GameManager.getInstance(name) == this) {
				task.run();
			}
		}, seconds * 20L);
	}

	@Override
	public void adminKickPlayer(CommandSender s, String name) {
		GamePlayer gp = players.remove(name.toLowerCase(Locale.ROOT));
		if (gp == null) {
			Util.msgRaw(s, Component.text("That player isn't in this game!", NamedTextColor.RED));
			return;
		}
		GameManager.removeFromSession(gp.getUniqueId());
		broadcast("<yellow>" + gp.getName() + " <gray>was kicked from the game by an admin!");
		replaceHost(gp);
		handleLeave(gp);
	}
	
	@Override
	public void kickPlayer(Player p, String name) {
		if (!p.getUniqueId().equals(host)) {
			Util.displayError(p, "Only the host may kick other players!");
			return;
		}
		
		if (!players.containsKey(name.toLowerCase())) {
			Util.displayError(p, "That player isn't in your game!");
			return;
		}
		
		GamePlayer gp = players.remove(name.toLowerCase());
		GameManager.removeFromSession(gp.getUniqueId());
		broadcast("<yellow>" + gp.getName() + " <gray>was kicked from the game!");
		replaceHost(gp);
		handleLeave(gp);
	}

	@Override
	public void broadcast(String msg) {
		for (GamePlayer gp : players.values()) {
			if (!gp.getPlayer().isOnline()) continue;
			Util.msgRaw(gp.getPlayer(), NeoCore.miniMessage().deserialize(msg).colorIfAbsent(NamedTextColor.GRAY));
		}
		for (Player p : spectators) {
			if (!p.isOnline()) continue;
			Util.msgRaw(p, NeoCore.miniMessage().deserialize(msg).colorIfAbsent(NamedTextColor.GRAY));
		}
	}
	
	public void broadcast(Component bc) {
		for (GamePlayer gp : players.values()) {
			if (!gp.getPlayer().isOnline()) continue;
			gp.getPlayer().sendMessage(bc);
		}
		for (Player p : spectators) {
			if (!p.isOnline()) continue;
			p.sendMessage(bc);
		}
	}

	@Override
	public void leavePlayer(Player p) {
		if (players.containsKey(p.getName().toLowerCase())) {
			GamePlayer gp = players.remove(p.getName().toLowerCase());
			GameManager.removeFromSession(p.getUniqueId());
			broadcast("<yellow>" + p.getName() + " <gray>left the game!");
			
			if (p.getUniqueId().equals(host)) {
				if (players.size() != 0) {
					GamePlayer next = players.values().iterator().next();
					host = next.getUniqueId();
					broadcast("Because the host left, the new host is now <yellow>" + next.getName() + "</yellow>!");
				}
				else {
					
				}
			}
			handleLeave(gp);
		}
		else if (spectators.contains(p)) {
			spectators.remove(p);
			GameManager.removeFromSession(p.getUniqueId());
			broadcast("<yellow>" + p.getName() + " <gray>stopped spectating!");
		}
		else {
			Util.displayError(p, "Something went wrong! You were unable to leave game " + name + ".");
			return;
		}
	}
	
	public HashMap<String, T> getPlayers() {
		return players;
	}
	
	public void addSpectator(Player p) {
		spectators.add(p);
		GameManager.addToInstance(p, this);
		broadcast("<yellow>" + p.getName() + " <gray>began spectating!");
		onSpectate(p);
	}

	public void clearSpectators() {
		for (Player spectator : spectators) {
			GameManager.removeFromSession(spectator.getUniqueId());
		}
		spectators.clear();
	}

	private void replaceHost(GamePlayer removed) {
		if (removed.getUniqueId().equals(host) && !players.isEmpty()) {
			host = players.values().iterator().next().getUniqueId();
			broadcast("The new host is <yellow>" + players.values().iterator().next().getName() + "</yellow>.");
		}
	}

	public void reconnect(Player player) {
		T gp = null;
		for (T candidate : players.values()) {
			if (candidate.getUniqueId().equals(player.getUniqueId())) {
				gp = candidate;
				break;
			}
		}
		if (gp != null) {
			players.remove(gp.getName().toLowerCase(Locale.ROOT));
			gp.reconnect(player);
			players.put(gp.getName().toLowerCase(Locale.ROOT), gp);
		}
		else {
			spectators.removeIf(spectator -> spectator.getUniqueId().equals(player.getUniqueId()));
			spectators.add(player);
		}
		onReconnect(player);
	}

	public void onReconnect(Player player) {
		displayInfo(player, player);
	}
	
	public void endGame() {
		GameManager.endGame(onEnd(), this);
	}
	
	public void displayKickList(CommandSender cmdUser) {
		Player p = (Player) cmdUser;
		Util.msgRaw(cmdUser, Component.text("Players:", NamedTextColor.GRAY));
		String hostName = players.values().stream().filter(gp -> gp.getUniqueId().equals(host))
				.map(GamePlayer::getName).findFirst().orElse("No host");
		Component c = Component.text().content("- ").color(NamedTextColor.GRAY)
				.append(Component.text(hostName, NamedTextColor.RED))
				.append(Component.text(" ("))
				.append(Component.text("Host", NamedTextColor.YELLOW))
				.append(Component.text(")")).build();
		p.sendMessage(c);
		boolean first = true;
		Builder b = Component.text();
		for (GamePlayer gp : players.values()) {
			UUID uuid = gp.getUniqueId();
			if (uuid.equals(host)) continue;
			if (!first) {
				b.appendNewline();
			}
			first = false;
			
			b.append(Component.text("- ", NamedTextColor.GRAY)).append(Component.text(gp.getName(), NamedTextColor.RED));
			b.appendNewline();
			
			Component kick = Component.text().content(" [").color(NamedTextColor.DARK_GRAY)
					.append(Component.text("Click to kick", NamedTextColor.RED))
					.append(Component.text("]")).build();
			kick = kick.hoverEvent(HoverEvent.showText(Component.text("Click to kick " + gp.getName())));
			kick = kick.clickEvent(ClickEvent.runCommand("/tt kick " + gp.getName()));
			Component giveHost = Component.text().content(" [").color(NamedTextColor.DARK_GRAY)
					.append(Component.text("Click to give host", NamedTextColor.RED))
					.append(Component.text("]")).build();
			giveHost = giveHost.hoverEvent(HoverEvent.showText(Component.text("Click to give host to " + gp.getName())));
			giveHost = giveHost.clickEvent(ClickEvent.runCommand("/tt sethost " + gp.getName()));
			
			b.append(kick).append(giveHost);
		}
		cmdUser.sendMessage(b.build());
	}
	
	public abstract void onSpectate(Player p);
	public abstract void showDebug(CommandSender s);
	public abstract GameLobby<T> onEnd();
}
