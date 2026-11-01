package me.ascheladd.ashtabletop.thecrew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.UUID;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.thecrew.TheCrewCard.CardType;
import me.ascheladd.ashtabletop.thecrew.TheCrewCard.SonarType;
import me.ascheladd.ashtabletop.thecrew.tasks.CardMatcher;
import me.ascheladd.ashtabletop.thecrew.tasks.TheCrewTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class TheCrewPlayer extends GamePlayer {
	private LinkedList<TheCrewCardInstance> hand = new LinkedList<TheCrewCardInstance>();
	private LinkedList<TheCrewCardInstance> revealedCards = new LinkedList<TheCrewCardInstance>();
	private LinkedList<TheCrewTask> tasks = new LinkedList<TheCrewTask>();
	private ArrayList<TheCrewCardInstance> cardsWon = new ArrayList<TheCrewCardInstance>();
	private TreeSet<Integer> cardValues = new TreeSet<Integer>();
	private TheCrewCardInstance lastPlayed;
	private TheCrewInstance inst;
	private int tricksWon = 0, sonarTokens = 0;

	public TheCrewPlayer(UUID uuid, Player p, TheCrewInstance inst) {
		super(uuid, p);
		this.inst = inst;
	}
	
	public TheCrewInstance getInstance() {
		return inst;
	}
	
	public TheCrewCardInstance getCard(int num) {
		return hand.get(num);
	}
	
	public TheCrewCardInstance playCard(int num) {
		lastPlayed = hand.remove(num);
		
		// If we have another card with same value, don't remove from cardValues
		boolean removeValue = true;
		for (TheCrewCard card : hand) {
			if (card.getValue() == lastPlayed.getValue() && card.getType() != CardType.SUB) {
				removeValue = false;
				break;
			}
		}
		if (removeValue) cardValues.remove(lastPlayed.getValue());
		
		if (lastPlayed.getType() != CardType.SUB) cardValues.add(lastPlayed.getValue());
		return lastPlayed;
	}
	
	public void addCard(TheCrewCardInstance card) {
		hand.add(card);
		if (card.getType() != CardType.SUB) cardValues.add(card.getValue());
	}
	
	public void addTask(TheCrewTask task) {
		tasks.add(task);
	}
	
	public void setSonarTokens(int tokens) {
		this.sonarTokens = tokens;
	}
	
	public int getSonarTokens() {
		return this.sonarTokens;
	}
	
	public void sortHand() {
		Collections.sort(hand, new Comparator<TheCrewCard>() {
			@Override
			public int compare(TheCrewCard c1, TheCrewCard c2) {
				int c1p = c1.getType().getSortPriority();
				int c2p = c2.getType().getSortPriority();
				int comp = Integer.compare(c1p, c2p);
				if (comp != 0) return comp;
				
				return Integer.compare(c1.getValue(), c2.getValue());
			}
		});
	}
	
	public void displayHand(Player viewer) {
		boolean isOwner = viewer.getUniqueId().equals(uuid);
		
		Builder b = Component.text().content((isOwner ? "Hand" : p.getName() + "'s hand") + ": ");
		if (hand.isEmpty()) {
			viewer.sendMessage(b.append(Component.text("Empty!")));
			return;
		}
		
		int pos = 0;
		Iterator<TheCrewCardInstance> iter = hand.iterator();
		
		Component card = iter.next().getDisplay();
		if (isOwner) {
			card = card.clickEvent(ClickEvent.runCommand("/thecrew play " + pos))
					.hoverEvent(HoverEvent.showText(Component.text("Click to play!")));
		}
		b.append(card);
		
		while (iter.hasNext()) {
			b.appendSpace();
			card = iter.next().getDisplay();
			if (isOwner) {
				card.clickEvent(ClickEvent.runCommand("/thecrew play " + ++pos))
					.hoverEvent(HoverEvent.showText(Component.text("Click to play!")));
			}
			b.append(card);
		}
		viewer.sendMessage(b.build());
		if (isOwner) displaySonarButton(viewer);
	}
	
	public void displaySonarButton(Player viewer) {
		if (sonarTokens == 0) return;
		Component c = SharedUtil.color("<dark_gray>[<gray>Click to use a sonar token</gray>]");
		c = c.hoverEvent(HoverEvent.showText(SharedUtil.color("You have <yellow>" + sonarTokens + "</yellow> tokens remaining!")));
		if (sonarTokens > 0) {
			c = c.clickEvent(sonarTokens > 0 ? ClickEvent.runCommand("/thecrew usesonars") : null);
		}
		viewer.sendMessage(c);
	}
	
	public void displaySonarOptions() {
		TheCrewCard curr, prev, next;
		Builder b = Component.text().content("Use sonar token on: ").color(NamedTextColor.GRAY);
		if (hand.size() == 0) return;
		else if (hand.size() == 1) {
			p.sendMessage(buildSonarOption(b, null, hand.getFirst(), null).build());
			return;
		}
		else {
			Iterator<TheCrewCardInstance> iter = hand.iterator();
			curr = iter.next();
			next = iter.next();
			while (iter.hasNext()) {
				prev = curr;
				curr = next;
				next = iter.next();
				
				buildSonarOption(b, prev, curr, next);
			}
			
			prev = curr;
			curr = next;
			next = null;
			buildSonarOption(b, prev, curr, next);
			p.sendMessage(b.build());
		}
		
	}
	
	private Builder buildSonarOption(Builder b, TheCrewCard prev, TheCrewCard curr, TheCrewCard next) {
		boolean prevDif = prev == null || prev.getType() != curr.getType();
		boolean nextDif = next == null || next.getType() != curr.getType();
		
		Component c = curr.getDisplay().appendSpace();
		if (prevDif && nextDif) {
			c = c.clickEvent(ClickEvent.runCommand("/thecrew sonar " + curr.toCardMatcher() + " ONLY"))
					.hoverEvent(HoverEvent.showText(Component.text("Click to use sonar token")));
		}
		else if (prevDif) {
			c = c.clickEvent(ClickEvent.runCommand("/thecrew sonar " + curr.toCardMatcher() + " MIN"))
					.hoverEvent(HoverEvent.showText(Component.text("Click to use sonar token")));
		}
		else if (nextDif) {
			c = c.clickEvent(ClickEvent.runCommand("/thecrew sonar " + curr.toCardMatcher() + " MAX"))
					.hoverEvent(HoverEvent.showText(Component.text("Click to use sonar token")));
		}
		else {
			String s = "<red>You may only use sonar tokens on\n" +
					"cards that are the max, min, or only\n" +
					"card of that color.";
			c = c.hoverEvent(HoverEvent.showText(SharedUtil.color(s)));
		}
		return b.append(c);
	}
	
	public boolean useSonarToken(CardMatcher cm, SonarType stype) {
		if (sonarTokens == 0) {
			Util.displayError(p, "You don't have any sonar tokens!");
			return false;
		}
		
		for (TheCrewCardInstance card : revealedCards) {
			if (cm.match(card)) {
				Util.displayError(p, "You've already revealed this card!");
				return false;
			}
		}
		
		TheCrewCardInstance card = null;
		for (TheCrewCardInstance c : hand) {
			if (cm.match(c)) {
				card = c;
			}
		}
		
		if (card == null) {
			Util.displayError(p, "You don't have this card!");
			return false;
		}
		
		CardType type = card.getType();
		Component c = Component.text()
				.append(SharedUtil.color("<yellow>" + getName() + "</yellow> reveals that "))
				.append(card.getDisplay())
				.append(SharedUtil.color(" is their <yellow>" + stype.getDisplay() + " "))
				.append(Component.text(type.getDisplay(), type.getColor()))
				.append(Component.text(" card.")).build();
		inst.broadcast(c);
		sonarTokens--;
		revealedCards.add(card);
		return true;
	}
	
	// Only for spectators
	public String createHandHoverText() {
		String text = p.getName() + "'s hand:";
		if (hand.isEmpty()) text += " empty!";
		else {
			for (TheCrewCard card : hand) {
				text += " " + card.getDisplay();
			}
		}
		return text;
	}
	
	public void winTrick(ArrayList<TheCrewCardInstance> pile) {
		tricksWon++;
		cardsWon.addAll(pile);
		pile.clear();
	}
	
	public boolean hasSimilarCard(TheCrewCard comp) {
		for (TheCrewCard card : hand) {
			if (card.getType() == comp.getType()) {
				return true;
			}
		}
		return false;
	}
	
	public LinkedList<TheCrewTask> getTasks() {
		return tasks;
	}
	
	public int getWins() {
		return tricksWon;
	}
	
	public ArrayList<TheCrewCardInstance> getCardsWon() {
		return cardsWon;
	}
	
	public LinkedList<TheCrewCardInstance> getHand() {
		return hand;
	}
	
	public TheCrewCardInstance getLastPlayed() {
		return lastPlayed;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof TheCrewPlayer) {
			TheCrewPlayer tcp = (TheCrewPlayer) o;
			return tcp.getUniqueId().equals(uuid);
		}
		return false;
	}
	
	public int getMinCard(boolean includeSubs) {
		return cardValues.first();
	}
	
	public int getMaxCard(boolean includeSubs) {
		return cardValues.last();
	}
	
	// List of available values not including subs
	public TreeSet<Integer> getCardValues() {
		return cardValues;
	}
	
	public void addWin(int num) {
		tricksWon += num;
	}
	
	public LinkedList<TheCrewCardInstance> getRevealedCards() {
		return revealedCards;
	}
}
