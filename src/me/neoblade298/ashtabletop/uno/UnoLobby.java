package me.neoblade298.ashtabletop.uno;

import java.util.UUID;

import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GameParameter;
import me.neoblade298.ashtabletop.IntegerParameterSetter;

public class UnoLobby extends GameLobby<UnoPlayer> {
	public UnoLobby(String name, UUID host, boolean isPublic) {
		super(name, Uno.inst(), host, isPublic);
		params.put("points", new GameParameter("points", "Winning score",
				"0 plays continuous rounds; otherwise the first player to reach this score wins.",
				0, new IntegerParameterSetter(0, 100000)));
	}

	public UnoLobby(UnoInstance instance) {
		super(instance);
	}

	@Override
	public GameInstance<UnoPlayer> onStart() {
		return new UnoInstance(this);
	}
}
