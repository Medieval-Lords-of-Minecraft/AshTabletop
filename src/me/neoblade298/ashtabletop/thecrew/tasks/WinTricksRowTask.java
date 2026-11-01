package me.neoblade298.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.ashtabletop.thecrew.TheCrewCardInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewInstance;
import me.neoblade298.ashtabletop.thecrew.TheCrewPlayer;
import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.neocore.shared.util.SharedUtil;

public class WinTricksRowTask extends TheCrewTask {
	protected boolean completedRow = false;
	protected int consecutive = 0;
	
	protected boolean exclusive, negate;
	private int wins;
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "wins: " + wins + ", exclusive: " + exclusive + ", negate: " + negate);
		Util.msgRaw(s, "consecutive: " + consecutive + ", completedRow: " + completedRow);
	}
	
	public WinTricksRowTask(Section cfg) {
		super(cfg);

		wins = cfg.getInt("wins");
		exclusive = cfg.getBoolean("exclusive");
		negate = cfg.getBoolean("negate");
		display = SharedUtil.color("Win " + (exclusive ? "only " : "") + "<yellow>" + wins + " </yellow>tricks in a row");
	}
	
	public WinTricksRowTask(TheCrewPlayer owner, WinTricksRowTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.negate = src.negate;
		this.wins = src.wins;
		this.exclusive = src.exclusive;
	}

	@Override
	public WinTricksRowTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinTricksRowTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (winner.equals(owner)) {
			if (negate && !completedRow) { // Exclusive is never with negate
				return true;
			}
			else if (exclusive && completedRow) {
				return true;
			}
		}
		return false;
 	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		if (winner.equals(owner) == negate) {
			consecutive = 0;
			return false;
		}
		else {
			if (exclusive && !completedRow) {
				completedRow = true;
			}
			else if (!exclusive) {
				return ++consecutive >= wins;
			}
		}
		
		return inst.getRoundsLeft() == 0 && completedRow;
	}

	@Override
	public void reset() {}
}
