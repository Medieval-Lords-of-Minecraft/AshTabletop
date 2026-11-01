package me.neoblade298.ashtabletop.thecrew;

import me.neoblade298.ashtabletop.thecrew.tasks.CardMatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class TheCrewCard {
	protected CardType type;
	protected int value;
	protected Component display;
	
	public TheCrewCard(CardType type, int value) {
		this.type = type;
		this.value = value;
		display = Component.text(value, type.color);
		if (this.type == CardType.SUB) display.decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED);
	}
	
	@Override
	public boolean equals(Object o) {
		if ((o instanceof TheCrewCard)) {
			return false;
		}
		TheCrewCard c = (TheCrewCard) o;
		
		return c.type == type && c.value == value;
	}
	
	public CardType getType() {
		return type;
	}
	public int getValue() {
		return value;
	}
	
	public Component getDisplay() {
		return display;
	}
	
	public boolean isSimilar(TheCrewCard comp) {
		return comp.getType() == type;
	}
	
	public enum CardType {
		RED(1, NamedTextColor.RED, "Red"),
		BLUE(2, NamedTextColor.BLUE, "Blue"),
		GREEN(3, NamedTextColor.DARK_GREEN, "Green"),
		YELLOW(4, NamedTextColor.YELLOW, "Yellow"),
		SUB(5, NamedTextColor.DARK_GRAY, "Sub");
		
		private final int sort;
		private final NamedTextColor color;
		private final String display;
		private CardType(final int sort, final NamedTextColor color, final String display) {
			this.sort = sort;
			this.color = color;
			this.display = display;
		}
		public int getSortPriority() {
			return sort;
		}
		public NamedTextColor getColor() {
			return color;
		}
		public String getDisplay() {
			return display;
		}
	}
	
	public CardMatcher toCardMatcher() {
		return new CardMatcher(this);
	}
	
	public TheCrewCardInstance createInstance() {
		return new TheCrewCardInstance(this);
	}

	
	public enum SonarType {
		MAX("highest"),
		MIN("lowest"),
		ONLY("only");
		
		private final String display;
		private SonarType(String display) {
			this.display = display;
		}
		
		public String getDisplay() {
			return display;
		}
	}
}
