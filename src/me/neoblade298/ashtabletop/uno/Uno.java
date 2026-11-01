package me.neoblade298.ashtabletop.uno;

import java.io.File;
import java.util.UUID;

import me.neoblade298.ashtabletop.AshTabletop;
import me.neoblade298.ashtabletop.Game;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GamePlayer;

public class Uno extends Game {
	private static Uno instance;

	public Uno() {
		super(new File(AshTabletop.folder(), "Uno"));
		if (!key.equals("uno") || minPlayers < 2 || maxPlayers > 8) {
			throw new IllegalArgumentException("Uno requires key uno and player limits between 2 and 8.");
		}
		instance = this;
	}

	public static Uno inst() {
		return instance;
	}

	@Override
	public GameLobby<? extends GamePlayer> createLobby(String name, UUID host, boolean isPublic) {
		return new UnoLobby(name, host, isPublic);
	}
}
