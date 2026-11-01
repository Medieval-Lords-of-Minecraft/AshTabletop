package me.ascheladd.ashtabletop.thecrew;

public class TheCrewCardInstance extends TheCrewCard {
	private TheCrewPlayer player;
	public TheCrewCardInstance(TheCrewCard card) {
		super(card.getType(), card.getValue());
	}
	
	public void setPlayer(TheCrewPlayer p) {
		this.player = p;
	}
	
	public TheCrewPlayer getPlayer() {
		return player;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TheCrewCardInstance) {
			TheCrewCardInstance card = (TheCrewCardInstance) o;
			return card.type == type && card.value == value;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return type + " " + value;
	}
}
