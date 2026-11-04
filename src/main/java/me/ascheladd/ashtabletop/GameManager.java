package me.ascheladd.ashtabletop;

import java.util.HashMap;
import java.util.UUID;
import java.util.Locale;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;
import me.ascheladd.ashtabletop.thecrew.TheCrew;
import me.ascheladd.ashtabletop.monopoly.Monopoly;
import me.ascheladd.ashtabletop.uno.Uno;

public class GameManager implements Listener {
	private static HashMap<UUID, GameLobby<? extends GamePlayer>> lobbies = new HashMap<UUID, GameLobby<? extends GamePlayer>>();
	private static HashMap<UUID, GameInstance<? extends GamePlayer>> instances = new HashMap<UUID, GameInstance<? extends GamePlayer>>();
	private static HashMap<UUID, GameSession<? extends GamePlayer>> inSession = new HashMap<UUID, GameSession<? extends GamePlayer>>(); // Can be lobby or instance
	private static HashMap<String, Game> games = new HashMap<String, Game>();
	
	public GameManager() {
		// Creating instance of game registers it to games via Game.java
		new TheCrew();
		new Monopoly();
		new Uno();
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		GameSession<? extends GamePlayer> session = inSession.get(player.getUniqueId());
		if (session instanceof GameInstance<?> instance) {
			instance.reconnect(player);
		}
	}
	
	// Kick player out of lobbies on disconnect, disband lobby if host is kicked
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
    	Player p = e.getPlayer();
    	UUID uuid = p.getUniqueId();
    	if (inSession.containsKey(uuid)) {
    		GameSession<? extends GamePlayer> sess = inSession.get(uuid);
    		if (sess instanceof GameLobby) {
    			GameLobby<? extends GamePlayer> lob = (GameLobby<? extends GamePlayer>) sess;
    			inSession.remove(uuid);
    			if (lob.getHost().equals(uuid)) {
    				disbandLobby(lob);
    			}

    			else {
    				lob.leavePlayer(p);
    			}
    		}
    	}
    }
    
	public static void clear() {
		inSession.clear();
		lobbies.clear();
		instances.clear();
		games.clear();
	}

    public static GameSession<? extends GamePlayer> getSession(String playerName) {
		return getPlayerSession(playerName);
    }

	public static GameSession<? extends GamePlayer> getPlayerSession(String playerName) {
		Player player = AshTabletop.findPlayer(playerName).orElse(null);
		if (player != null) {
			return inSession.get(player.getUniqueId());
		}
		for (GameInstance<?> instance : instances.values()) {
			if (instance.getPlayers().containsKey(playerName.toLowerCase(Locale.ROOT))) return instance;
		}
		return null;
	}
    
    public static GameSession<? extends GamePlayer> getSession(UUID uuid) {
    	return inSession.get(uuid);
    }
    
    public static GameLobby<? extends GamePlayer> getLobby(String playerName) {
		GameSession<? extends GamePlayer> session = getSession(playerName);
		return session instanceof GameLobby<?> lobby ? lobby : null;
    }
    
    public static GameInstance<? extends GamePlayer> getInstance(String playerName) {
		GameSession<? extends GamePlayer> session = getSession(playerName);
		return session instanceof GameInstance<?> instance ? instance : null;
    }

	public static GameInstance<? extends GamePlayer> getInstance(UUID id) {
		return instances.get(id);
	}
    
    // Creates a lobby
	public static void createLobby(Player sender, Game game, boolean isPublic) {
		UUID uuid = sender.getUniqueId();
		GameLobby<? extends GamePlayer> lobby = game.createLobby(uuid, isPublic);
		lobbies.put(lobby.getId(), lobby);
		inSession.put(sender.getUniqueId(), lobby);
		Util.msg(sender, NeoCore.miniMessage().deserialize("Successfully created <yellow>" + lobby.getName() + "</yellow>!"));
		lobby.displayInfo(sender, sender);
	}
	
	public static void disbandSession(GameSession<? extends GamePlayer> sess) {
		if (sess instanceof GameLobby) {
			disbandLobby((GameLobby<? extends GamePlayer>) sess);
		}
		else if (sess instanceof GameInstance) {
			disbandInstance((GameInstance<? extends GamePlayer>) sess, "admin override");
		}
	}
    
	public static void disbandLobby(GameLobby<? extends GamePlayer> lob) {
		for (UUID uuid : lob.getPlayers()) {
			inSession.remove(uuid);
		}
		lob.broadcast("Due to the host leaving, <yellow>" + lob.getName() + " </yellow>was disbanded.");
		lobbies.remove(lob.getId());
	}
	
	public static void disbandInstance(GameInstance<? extends GamePlayer> inst, String reason) {
		for (GamePlayer gp : inst.getPlayers().values()) {
			inSession.remove(gp.getUniqueId());
		}
		inst.broadcast("Due to " + reason + ", instance <yellow>" + inst.getName() + " </yellow>was disbanded.");
		inst.clearSpectators();
		instances.remove(inst.getId());
	}
	
	public static void removeFromSession(UUID uuid) {
		inSession.remove(uuid);
	}

	public static void addToInstance(Player player, GameInstance<?> instance) {
		inSession.put(player.getUniqueId(), instance);
	}
	
	public static HashMap<UUID, GameLobby<? extends GamePlayer>> getLobbies() {
		return lobbies;
	}
	
	public static HashMap<UUID, GameInstance<? extends GamePlayer>> getInstances() {
		return instances;
	}
	
	public static void addToLobby(Player p, GameLobby<? extends GamePlayer> lob) {
		inSession.put(p.getUniqueId(), lob);
	}
	
	public static Game getGame(String name) {
		return games.get(name);
	}
	
	public static HashMap<String, Game> getGames() {
		return games;
	}
	
	public static void registerGame(Game g) {
		if (games.containsKey(g.getKey())) {
			throw new IllegalArgumentException("Duplicate game key: " + g.getKey());
		}
		games.put(g.getKey(), g);
	}
	
	public static void startGame(GameLobby<? extends GamePlayer> lob, GameInstance<? extends GamePlayer> inst) {
		lob.broadcast("The game has started!");

		lobbies.remove(lob.getId());
		instances.put(inst.getId(), inst);
		for (UUID uuid : lob.getPlayers()) {
			inSession.put(uuid, inst);
		}
	}
	
	public static void endGame(GameLobby<? extends GamePlayer> lob, GameInstance<? extends GamePlayer> inst) {
		inst.broadcast("The game has ended and you've all been returned to the lobby.");
		instances.remove(inst.getId());
		inst.clearSpectators();
		for (GamePlayer gp : inst.getPlayers().values()) {
			inSession.remove(gp.getUniqueId());
			if (gp.getPlayer().isOnline()) {
				lob.addPlayerForce(gp.getPlayer());
			}
		}
		if (lob.getPlayers().isEmpty()) return;
		if (!lob.getPlayers().contains(lob.getHost())) {
			lob.host = lob.getPlayers().iterator().next();
		}
		lobbies.put(lob.getId(), lob);
		for (GamePlayer gp : inst.getPlayers().values()) {
			if (gp.getPlayer().isOnline()) lob.displayInfo(gp.getPlayer(), gp.getPlayer());
		}
	}
}
