package me.neoblade298.ashtabletop.uno;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public record UnoCard(int number, Color color) {
	public enum Color {
		RED("r", NamedTextColor.RED), BLUE("b", NamedTextColor.BLUE),
		GREEN("g", NamedTextColor.DARK_GREEN), YELLOW("y", NamedTextColor.YELLOW),
		WILD("", NamedTextColor.WHITE);

		final String alias;
		final NamedTextColor display;

		Color(String alias, NamedTextColor display) {
			this.alias = alias;
			this.display = display;
		}

		static Color parse(String text) {
			for (Color color : values()) {
				if (color != WILD && (color.alias.equalsIgnoreCase(text) || color.name().equalsIgnoreCase(text))) {
					return color;
				}
			}
			return null;
		}
	}

	public static final int SKIP = 10, REVERSE = 11, DRAW_TWO = 12, WILD = 13, DRAW_FOUR = 14;

	public String label() {
		return switch (number) {
			case SKIP -> "Skip";
			case REVERSE -> "Reverse";
			case DRAW_TWO -> "+2";
			case WILD -> "Wild";
			case DRAW_FOUR -> "+4";
			default -> Integer.toString(number);
		};
	}

	public String alias() {
		return label().toLowerCase(java.util.Locale.ROOT) + color.alias;
	}

	public Component display() {
		return Component.text(label(), color.display);
	}

	public int points() {
		return number <= 9 ? number : 10;
	}

	public boolean addsDraws() {
		return number == DRAW_TWO || number == DRAW_FOUR;
	}
}
