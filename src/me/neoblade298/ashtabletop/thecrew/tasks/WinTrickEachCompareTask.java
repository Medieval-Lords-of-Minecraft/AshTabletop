package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.neocore.shared.util.SharedUtil;

public class WinTrickEachCompareTask extends TheCrewTask {
	protected boolean more; // Either more or less
	protected int value;
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "more: " + more + ", value: " + value);
	}
	
	public WinTrickEachCompareTask(Section cfg) {
		super(cfg);

		value = cfg.getInt("value");
		more = cfg.getString("comparator").equals(">");
		display = SharedUtil.color("Win a trick where each card is " + (more ? "greater" : "less") + " than <yellow>" + value + "</yellow>. *No subs");
		
	}
	
	public WinTrickEachCompareTask(TheCrewPlayer owner, WinTrickEachCompareTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.value = src.value;
		this.more = src.more;
	}

	@Override
	public WinTrickEachCompareTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinTrickEachCompareTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		boolean succeeds = true;
		if (winner.equals(owner)) {
			for (TheCrewCard card : pile) {
				if (card.getType() == CardType.SUB ||
						(more ? card.getValue() <= value : card.getValue() >= value)) {
					succeeds = false;
					break;
				}
			}
		}
		if (succeeds) return false;
		
		boolean canSucceed = false;
		for (TheCrewPlayer p : inst.getPlayers().values()) {
			for (TheCrewCard card : p.getHand()) {
				if (more ? card.getValue() > value : card.getValue() < value) break;
			}
		}
		return !canSucceed;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (winner.equals(owner)) {
			for (TheCrewCard card : pile) {
				if (card.getType() == CardType.SUB ||
						(more ? card.getValue() <= value : card.getValue() >= value)) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void reset() {}
}
