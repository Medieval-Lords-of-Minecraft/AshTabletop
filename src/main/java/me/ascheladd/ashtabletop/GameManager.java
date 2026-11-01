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
	private static HashMap<String, GameLobby<? extends GamePlayer>> lobbies = new HashMap<String, GameLobby<? extends GamePlayer>>();
	private static HashMap<String, GameInstance<? extends GamePlayer>> instances = new HashMap<String, GameInstance<? extends GamePlayer>>();
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

    public static GameSession<? extends GamePlayer> getSession(String name) {
		name = name.toLowerCase(Locale.ROOT);
    	if (lobbies.containsKey(name)) return lobbies.get(name);
    	return instances.get(name);
    }

	public static GameSession<? extends GamePlayer> getPlayerSession(String playerName) {
		for (GameInstance<?> instance : instances.values()) {
			if (instance.getPlayers().containsKey(playerName.toLowerCase(Locale.ROOT))) return instance;
		}
		return null;
	}
    
    public static GameSession<? extends GamePlayer> getSession(UUID uuid) {
    	return inSession.get(uuid);
    }
    
    public static GameLobby<? extends GamePlayer> getLobby(String name) {
    	return lobbies.get(name.toLowerCase());
    }
    
    public static GameInstance<? extends GamePlayer> getInstance(String name) {
    	return instances.get(name.toLowerCase());
    }
    
    // Creates a lobby
	public static void createLobby(String name, Player sender, Game game, boolean isPublic) {
		UUID uuid = sender.getUniqueId();
		GameLobby<? extends GamePlayer> lobby = game.createLobby(name, uuid, isPublic);
		lobbies.put(name.toLowerCase(), lobby);
		inSession.put(sender.getUniqueId(), lobby);
		Util.msg(sender, NeoCore.miniMessage().deserialize("Successfully created lobby <yellow>" + lobby.getName() + "</yellow>!"));
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
		lob.broadcast("Due to the host leaving, lobby <yellow>" + lob.getName() + " </yellow>was disbanded.");
		lobbies.remove(lob.getName().toLowerCase());
	}
	
	public static void disbandInstance(GameInstance<? extends GamePlayer> inst, String reason) {
		for (GamePlayer gp : inst.getPlayers().values()) {
			inSession.remove(gp.getUniqueId());
		}
		inst.broadcast("Due to " + reason + ", instance <yellow>" + inst.getName() + " </yellow>was disbanded.");
		inst.clearSpectators();
		instances.remove(inst.getName().toLowerCase());
	}
	
	public static void removeFromSession(UUID uuid) {
		inSession.remove(uuid);
	}

	public static void addToInstance(Player player, GameInstance<?> instance) {
		inSession.put(player.getUniqueId(), instance);
	}
	
	public static boolean sessionExists(String name) {
		return lobbies.containsKey(name.toLowerCase()) || instances.containsKey(name.toLowerCase());
	}
	
	public static HashMap<String, GameLobby<? extends GamePlayer>> getLobbies() {
		return lobbies;
	}
	
	public static HashMap<String, GameInstance<? extends GamePlayer>> getInstances() {
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

		lobbies.remove(lob.getName().toLowerCase(Locale.ROOT));
		instances.put(inst.getName().toLowerCase(Locale.ROOT), inst);
		for (UUID uuid : lob.getPlayers()) {
			inSession.put(uuid, inst);
		}
	}
	
	public static void endGame(GameLobby<? extends GamePlayer> lob, GameInstance<? extends GamePlayer> inst) {
		inst.broadcast("The game has ended and you've all been returned to the lobby.");
		instances.remove(inst.getName().toLowerCase(Locale.ROOT));
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
		lobbies.put(lob.getName().toLowerCase(Locale.ROOT), lob);
		for (GamePlayer gp : inst.getPlayers().values()) {
			if (gp.getPlayer().isOnline()) lob.displayInfo(gp.getPlayer(), gp.getPlayer());
		}
	}
}
