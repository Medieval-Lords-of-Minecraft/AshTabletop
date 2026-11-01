package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bungee.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import com.velocitypowered.api.command.CommandSource;
import me.neoblade298.neocore.shared.io.Section;

public class WinTrickCompareColorsTask extends TheCrewTask {
	protected int cardsRemaining, compsRemaining;
	
	protected CardMatcher card, comp;
	protected boolean more; // Either more or equal
	protected boolean allowZero;
	
	@Override
	public void showDebug(CommandSource s) {
		Util.msgRaw(s, "card: " + card + ", comp: " + comp + ", more: " + more + ", allowZero: " + allowZero);
		Util.msgRaw(s, "cardsRemaining: " + cardsRemaining + ", compsRemaining: " + compsRemaining);
	}
	
	public WinTrickCompareColorsTask(Section cfg) {
		super(cfg);
		
		card = new CardMatcher(cfg.getString("card"));
		comp = new CardMatcher(cfg.getString("comp"));
		more = cfg.getString("comparator").equals(">");
		allowZero = cfg.getBoolean("allow_zero");
		
		cardsRemaining = card.getTotalCardsMatching();
		compsRemaining = card.getTotalCardsMatching();

		CardType t = card.getType();
		CardType tc = comp.getType();
		display = Component.text().content("Win " + (more ? "more " : "exactly as many "))
				.append(Component.text(t.getDisplay() + " cards", t.getColor()))
				.append(Component.text(more ? "than " : "as "))
				.append(Component.text(tc.getDisplay() + " cards", tc.getColor()))
				.append(Component.text(" in one trick")).build();
		
	}
	
	public WinTrickCompareColorsTask(TheCrewPlayer owner, WinTrickCompareColorsTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.card = src.card;
		this.comp = src.comp;
		this.allowZero = src.allowZero;
		this.more = src.more;
		this.cardsRemaining = src.cardsRemaining;
		this.compsRemaining = src.compsRemaining;
	}

	@Override
	public WinTrickCompareColorsTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinTrickCompareColorsTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (succeeds(winner, pile)) return false;
		

		int tempCards = cardsRemaining;
		int tempComps = compsRemaining;
		for (TheCrewCard card : pile) {
			if (this.card.match(card)) tempCards--;
			if (this.comp.match(card)) tempComps--;
		}
		
		if (!allowZero) {
			return (more ? tempCards <= 1 : tempCards == 0) || tempComps == 0;
		}
		else {
			return tempCards == 0;
		}
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (succeeds(winner, pile)) {
			return true;
		}

		for (TheCrewCard card : pile) {
			if (this.card.match(card)) cardsRemaining--;
			if (this.comp.match(card)) compsRemaining--;
		}
		return false;
	}

	@Override
	public void reset() {
		cardsRemaining = card.getTotalCardsMatching();
		compsRemaining = comp.getTotalCardsMatching();
	}
	
	private boolean succeeds(TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (!winner.equals(owner)) return false;
		int cards = 0, comps = 0;
		for (TheCrewCard card : pile) {
			if (this.card.match(card)) cards++;
			if (this.comp.match(card)) comps++;
		}
		
		if (!allowZero && comps == 0) return false;
		
		if (more) {
			return cards > comps;
		}
		else {
			return cards == comps;
		}
	}
}
