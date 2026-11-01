package me.ascheladd.ashtabletop.thecrew.tasks;

import java.util.ArrayList;

import me.neoblade298.neocore.bukkit.util.Util;
import me.ascheladd.ashtabletop.thecrew.TheCrewCardInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewInstance;
import me.ascheladd.ashtabletop.thecrew.TheCrewPlayer;
import org.bukkit.command.CommandSender;
import me.neoblade298.neocore.shared.io.Section;
import me.neoblade298.neocore.shared.util.SharedUtil;

public class WinFirstTricksTask extends TheCrewTask {
	protected boolean negate;
	protected int amount;
	
	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "amount: " + amount + ", negate: " + negate);
	}
	
	public WinFirstTricksTask(Section cfg) {
		super(cfg);

		negate = cfg.getBoolean("negate");
		amount = cfg.getInt("amount");
		
		display = SharedUtil.color((negate ? "Do not win" : "Win") + " the first <yellow>" + amount + " </yellow>tricks");
	}
	
	public WinFirstTricksTask(TheCrewPlayer owner, WinFirstTricksTask src, TheCrewInstance inst) {
		super(owner, src, inst);

		this.negate = src.negate;
		this.amount = src.amount;
	}

	@Override
	public WinFirstTricksTask clone(TheCrewPlayer owner, TheCrewInstance inst) {
		return new WinFirstTricksTask(owner, this, inst);
	}
	
	@Override
	public boolean hasFailed(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		// Since this isn't checked when the task is completed, we just need to check if the player wins (or loses if negated)
		return !winner.equals(owner) ^ negate;
 	}

	@Override
	public boolean update(TheCrewInstance inst, TheCrewPlayer winner, ArrayList<TheCrewCardInstance> pile) {
		// Just need to check if we've reached the round without losing
		return inst.getRound() == amount;
	}

	@Override
	public void reset() {}
}
