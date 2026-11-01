package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;
import java.util.HashMap;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import net.kyori.adventure.text.Component;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import org.bukkit.command.CommandSender;

public class WinTrickTotalCompareTask extends TheCrewTask {
	protected HashMap<Integer, Integer> totals = new HashMap<Integer, Integer>();
	protected boolean more; // Either more or less
	protected int total;
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "more: " + more + ", total: " + total);
	}
	
	public WinTrickTotalCompareTask(Section cfg) {
		super(cfg);
		
		Section totalSec = cfg.getSection("total");
		for (String key : totalSec.getKeys()) {
			int ikey = Integer.parseInt(key);
			totals.put(ikey, totalSec.getInt(key));
		}

		more = cfg.getString("comparator").equals(">");
		String s = "Win a trick with a total value " + (more ? "greater" : "less") + " than " + totals.get(3) +
				" (3-player), " + totals.get(4) + " (4-player), " + totals.get(5) + " (5-player). *No subs";
		display = Component.text(s);
		
	}
	
	public WinTrickTotalCompareTask(TheCrewPlayer owner, WinTrickTotalCompareTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.total = inst.getPlayers().size();
		display = SharedUtil.color("Win a trick with a total value " + (more ? "greater" : "less") + " than <yellow>" + total + "</yellow>. *No subs");
		this.total = src.total;
		this.more = src.more;
	}

	@Override
	public WinTrickTotalCompareTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinTrickTotalCompareTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int total = 0;
		boolean containsSub = false;
		if (winner.equals(owner)) {
			for (TheCrewCard card : pile) {
				if (card.getType() == CardType.SUB) {
					containsSub = true;
					break;
				}
				
				total += card.getValue();
			}
		}
		if (more ? total > this.total : total < this.total && !containsSub) return false; // Succeeded
		
		int potential = 0;
		for (TheCrewPlayer p : inst.getPlayers().values()) {
			potential += (more ? p.getMaxCard(false) : p.getMinCard(false));
		}
		return more ? potential <= this.total : potential >= this.total;
	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		int total = 0;
		if (winner.equals(owner)) {
			for (TheCrewCard card : pile) {
				if (card.getType() == CardType.SUB) return false;
				
				total += card.getValue();
			}
		}
		
		return more ? total > this.total : total < this.total;
	}

	@Override
	public void reset() {}
}
