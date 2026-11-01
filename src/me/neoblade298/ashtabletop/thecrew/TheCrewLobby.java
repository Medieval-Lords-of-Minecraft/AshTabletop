package me.neoblade298.ashtabletop.thecrew;

import java.util.UUID;

import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GameParameter;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.IntegerParameterSetter;

public class TheCrewLobby extends GameLobby<TheCrewPlayer> {

	public TheCrewLobby(String name, UUID host, boolean isPublic) {
		super(name, TheCrew.inst(), host, isPublic);
		
		params.put("difficulty", new GameParameter("difficulty", "Difficulty",
				"Affects how many conditions you must pass to win. Can be 1-25.",
				Integer.valueOf(6), new IntegerParameterSetter(1, 25)));
		
		params.put("sonar_tokens", new GameParameter("sonar_tokens", "Sonar Tokens",
				"Number of sonar tokens per user you're allowed to use to communicate. Can be 0-3",
				Integer.valueOf(1), new IntegerParameterSetter(0, 3)));
	}
	
	public TheCrewLobby(GameInstance<TheCrewPlayer> inst) {
		super(inst);
	}

	@Override
	public GameInstance<? extends GamePlayer> onStart() {
		return new TheCrewInstance(this);
	}
	
}
