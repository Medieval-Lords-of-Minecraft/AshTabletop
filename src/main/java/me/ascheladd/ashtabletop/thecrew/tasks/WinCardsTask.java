package me.ascheladd.ashtabletop.thecrew.tasks;

import java.util.ArrayList;
import java.util.HashMap;

import me.neoblade298.neocore.bukkit.util.Util;
import me.ascheladd.ashtabletop.thecrew.TheCrewCard;
import me.ascheladd.ashtabletop.thecrew.TheCrewCardInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;

import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.shared.io.Section;

public class WinCardsTask extends TheCrewTask {
	protected boolean negate = false;
	protected HashMap<CardMatcher, Integer> negateRemaining = new HashMap<CardMatcher, Integer>();
	protected ArrayList<CardMatcher> cards = new ArrayList<CardMatcher>(), completed = new ArrayList<CardMatcher>();
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "cards: " + cards);
		Util.msgRaw(s, "completed: " + completed);
		Util.msgRaw(s, "negate: " + negate);
	}
	
	public WinCardsTask(Section cfg) {
		super(cfg);
		for (String str : cfg.getStringList("cards")) {
			cards.add(new CardMatcher(str));
		}
		negate = cfg.getBoolean("negate", false);
		
		Builder b = Component.text().content(negate ? "Don't win " : "Win ");
		for (int i = 0; i < cards.size(); i++) {
			if (i != 0) {
				b.append(Component.text(", "));
			}
			b.append(cards.get(i).getDisplay());
		}
		display = b.build();
	}
	
	public WinCardsTask(TheCrewPlayer owner, WinCardsTask src, TheCrewInstance inst) {
		super(owner, src, inst);
		
		for (CardMatcher card : src.cards) {
			this.cards.add(card);
		}
		this.negate = src.negate;
		reset();
	}

	@Override
	public WinCardsTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinCardsTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		for (TheCrewCardInstance card : pile) {
			for (int i = 0; i < cards.size(); i++) {
				CardMatcher matcher = cards.get(i);
				if (matcher.match(card)) {
					if (!winner.getUniqueId().equals(owner.getUniqueId()) ^ negate) {
						// 1: Winner isn't player and negate is false (true xor false)
						// 2: winner is player and negate is true (false xor true)
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		ArrayList<Integer> toComplete = new ArrayList<Integer>(cards.size());
		for (TheCrewCard card : pile) {
			for (int i = 0; i < cards.size(); i++) {
				CardMatcher matcher = cards.get(i);
				if (matcher.match(card)) {
					if (!negate) {
						if (winner.getUniqueId().equals(owner.getUniqueId())) {
							toComplete.add(i);
						}
					}
					else {
						negateRemaining.put(matcher, negateRemaining.get(matcher) - 1);
						boolean succeed = true;
						for (Integer j : negateRemaining.values()) {
							if (j != 0) {
								succeed = false;
								break;
							}
						}
						if (succeed) return true;
					}
				}
			}
		}
		
		for (int i : toComplete) {
			completed.add(cards.remove(i));
		}

		return cards.isEmpty();
	}

	@Override
	public void reset() {
		cards.addAll(completed);
		completed.clear();
		for (CardMatcher card : cards) {
			negateRemaining.put(card, card.getTotalCardsMatching());
		}
	}
}
