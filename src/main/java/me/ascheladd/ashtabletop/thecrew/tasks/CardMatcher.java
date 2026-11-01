package me.ascheladd.ashtabletop.thecrew.tasks;

import me.ascheladd.ashtabletop.thecrew.TheCrewCard;
import me.ascheladd.ashtabletop.thecrew.TheCrewCard.CardType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CardMatcher {
	private int value;
	private CardType type;
	private String base;
	private Component display;
	private int totalCardsMatching;
	
	public CardMatcher(TheCrewCard card) {
		this(card.getValue() + "" + card.getType().name().charAt(0));
	}
	public CardMatcher(String matcher) {
		if (matcher == null) {
			value = -1;
			type = null;
			return;
		}
		
		this.base = matcher;
		char num = matcher.charAt(0);
		if (num == '*') {
			value = -1;
		}
		else {
			value = Integer.parseInt("" + num);
		}
		
		char t = matcher.charAt(1);
		if (t == '*') {
			type = null;
		}
		else {
			switch (t) {
			case 'R': type = CardType.RED;
			break;
			case 'G': type = CardType.GREEN;
			break;
			case 'Y': type = CardType.YELLOW;
			break;
			case 'B': type = CardType.BLUE;
			break;
			case 'S': type = CardType.SUB;
			break;
			}
		}
		
		Component start = Component.text("[", NamedTextColor.DARK_GRAY);
		Component end = Component.text("]");
		// Calculate display
		if (value == -1 && type == null) {
			display = start.append(Component.text("Any Card", NamedTextColor.GRAY)).append(end);
			totalCardsMatching = 40;
		}
		else if (value == -1) {
			display = start.append(Component.text("Any " + type.getDisplay() + " Card", type.getColor())).append(end);
			totalCardsMatching = (type == CardType.SUB ? 4 : 9);
		}
		else if (type == null) {
			display = start.append(Component.text("Any " + value + " Card", NamedTextColor.GRAY)).append(end);
			totalCardsMatching = 4;
		}
		else {
			display = start.append(Component.text(type.getDisplay() + " " + value, type.getColor())).append(end);
			totalCardsMatching = 1;
		}
	}
	
	public boolean match(TheCrewCard card) {
		if (value != -1 && card.getValue() != value) {
			return false;
		}
		if (type != null && card.getType() != type) {
			return false;
		}
		return true;
	}
	
	public CardType getType() {
		return type;
	}
	
	public Component getDisplay() {
		return display;
	}
	
	public int getTotalCardsMatching() {
		return totalCardsMatching;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof CardMatcher) {
			CardMatcher cm = (CardMatcher) o;
			return cm.getType() == type && cm.value == value;
		}
		return false;
	}
	
	public String toString() {
		return base;
	}
}
