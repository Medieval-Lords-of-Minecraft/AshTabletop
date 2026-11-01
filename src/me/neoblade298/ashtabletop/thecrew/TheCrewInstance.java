package me.neoblade298.ashtabletop.thecrew;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;
import me.neoblade298.neocore.shared.util.SharedUtil;
import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.AshTabletop;
import me.neoblade298.ashtabletop.thecrew.TheCrewCard.CardType;
import me.neoblade298.ashtabletop.thecrew.tasks.CompareWinsTask;
import me.neoblade298.ashtabletop.thecrew.tasks.TheCrewTask;
import me.neoblade298.ashtabletop.thecrew.tasks.WinTricksPredictTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TheCrewInstance extends GameInstance<TheCrewPlayer> {
	
	// Metadata
	private int difficulty;
	private GamePhase phase = GamePhase.SETUP;
	private TheCrewPlayer captain;
	private ArrayList<TheCrewPlayer> turnOrder = new ArrayList<TheCrewPlayer>();
	private int turn = 0, round = 1, totalRounds;
	
	// Task Selection
	private ArrayList<TheCrewTask> tasks;
	
	// Play phase
	private ArrayList<TheCrewCardInstance> pile = new ArrayList<TheCrewCardInstance>();
	
	// Lose phase
	private TheCrewTask lossReason;

	public TheCrewInstance(GameLobby<TheCrewPlayer> lobby) {
		super(lobby);
		difficulty = (int) params.get("difficulty").get();
		int sonarTokens = (int) params.get("sonar_tokens").get();
		
		for (UUID uuid : lobby.getPlayers()) {
			Player p = AshTabletop.findPlayer(uuid).get();
			TheCrewPlayer tcp = new TheCrewPlayer(uuid, p, this);
			tcp.setSonarTokens(sonarTokens);
			this.players.put(p.getName().toLowerCase(), tcp);
		}
		
		setupGame();
	}

	@Override
	public void handleLeave(GamePlayer gp) {
		broadcast("<gray>Due to <yellow>" + gp.getName() + " </yellow>leaving, the game will end.");
		endGame();
	}

	@Override
	public void onSpectate(Player p) {
		displaySpectatorInfo(p);
	}

	@Override
	public GameLobby<TheCrewPlayer> onEnd() {
		return new TheCrewLobby(this);
	}

	@Override
	public void displayInfo(Player viewer, Player viewed) {
		if (!players.containsKey(viewer.getName().toLowerCase()) && (phase == GamePhase.LOSE || phase == GamePhase.PLAY)) {
			displaySpectatorInfo(viewer);
			return;
		}
		TheCrewPlayer p = players.get(viewer.getName().toLowerCase());
		boolean isHost = p.getUniqueId().equals(host);
		boolean viewerTurn = turnOrder.get(turn).getUniqueId().equals(viewer.getUniqueId());
		
		if (isHost) {
			Component c = SharedUtil.color("<dark_gray>[<gray>Click for game moderation tools</gray>]")
					.hoverEvent(HoverEvent.showText(Component.text("Click here!")))
					.clickEvent(ClickEvent.runCommand("/thecrew mod"));
			p.getPlayer().sendMessage(c);
		}
		switch (phase) {
		case SETUP: Util.msgRaw(viewer, Component.text("Game is being setup...", NamedTextColor.GRAY));
		break;
		case WAITING: Util.msgRaw(viewer, Component.text("Game is calculating something...", NamedTextColor.GRAY));
		break;
		case ROLL_TASKS: 
			if (viewer.getUniqueId().equals(captain.getUniqueId())) {
				promptPlayer();
			}
			else {
				Util.msgRaw(viewer, Component.text("Captain is deciding whether to reroll the following tasks:", NamedTextColor.GRAY));
				for (TheCrewTask task : tasks) {
					Component c = Component.text("- ", NamedTextColor.GRAY).append(task.getDisplay())
							.append(SharedUtil.color(" (Difficulty: <yellow>" + task.getDifficulty(players.size()) + "</yellow>)"));
					Util.msgRaw(viewer, c);
				}
			}
			break;
		case SELECT_TASKS:
			if (viewerTurn) {
				promptPlayer();
			}
			else {
				TheCrewPlayer tcp = turnOrder.get(turn);
				Component c = SharedUtil.color("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]")
						.hoverEvent(HoverEvent.showText(createTaskHover()))
						.clickEvent(ClickEvent.runCommand("/thecrew viewtasks"));
				viewer.sendMessage(c);
				Util.msgRaw(viewer, "<yellow>" + tcp.getName() + "<gray>'s turn to select a task:");
				for (TheCrewTask task : tasks) {
					Component ct = Component.text("- ", NamedTextColor.GRAY).append(task.getDisplay())
							.append(SharedUtil.color(" (Difficulty: <yellow>" + task.getDifficulty(players.size()) + "</yellow>)"));
					Util.msgRaw(viewer, ct);
				}
			}
			break;
		case PLAY:
			Util.msgRaw(viewer, "<gray>Rounds remaining: <yellow>" + (totalRounds - round));
			Util.msgRaw(viewer, "<gray>Turn Order:");
			int num = 0;
			for (TheCrewPlayer tcp : turnOrder) {
				String text = "<gray>- <red>" + tcp.getName() + " </red><gold>" + tcp.getWins() + "W</gold>";
				if (tcp.getUniqueId().equals(captain.getUniqueId())) {
					text += " (<dark_red>Captain</dark_red>)";
				}
				text += ": ";
				text += pile.size() > num ? pile.get(num).getDisplay() : "???";
				Util.msgRaw(viewer, SharedUtil.color(text));
				num++;
			}
			Component c = SharedUtil.color("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]")
					.hoverEvent(HoverEvent.showText(createTaskHover()))
					.clickEvent(ClickEvent.runCommand("/thecrew viewtasks"));
			viewer.sendMessage(c);
			p.displayHand(viewer);
			break;
		case LOSE:
			if (viewer.getUniqueId().equals(host)) {
				Util.msgRaw(viewer, "<red>You lost!");
				Util.msgRaw(viewer, SharedUtil.color("<red>" + lossReason.getOwner().getName() + " failed to perform task: ")
						.append(lossReason.getDisplay()));
				p.displayHand(viewer);
				Component accepted = SharedUtil.createText("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]",
						createTaskHover(), ClickEvent.runCommand("/thecrew viewtasks"));
				viewer.sendMessage(accepted);
				
				Component redo = SharedUtil.createText("<dark_gray>[<red>Click to redo round</red>]",
						"Click here!", ClickEvent.runCommand("/thecrew restartround"));
				Component restart = SharedUtil.createText(" <dark_gray>[<red>Click to restart from round 1</red>]",
						"Click here!", ClickEvent.runCommand("/thecrew restartgame"));
				Component lobby = SharedUtil.createText("\n<dark_gray>[<red>Click to return to lobby</red>]",
						"Click here!", ClickEvent.runCommand("/tt return"));
				viewer.sendMessage(redo.append(restart).append(lobby));
			}
			else {
				Util.msgRaw(viewer, "<red>You lost! Waiting for host to decide what to do next...");
				Util.msgRaw(viewer, SharedUtil.color("<red>" + lossReason.getOwner().getName() + " failed to perform task: ")
						.append(lossReason.getDisplay()));
				p.displayHand(viewer);
				viewer.sendMessage(SharedUtil.createText("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]",
						createTaskHover(), ClickEvent.runCommand("/thecrew viewtasks")));
			}
			break;
		case PREPLAY:
			break;
		case WIN:
			break;
		default:
			break;
		}
			
	}
	
	public void displaySpectatorInfo(Player viewer) {
		if (phase == GamePhase.PLAY) {
			Util.msgRaw(viewer, SharedUtil.color("<gray>Rounds remaining: <yellow>" + (totalRounds - round)));
			Util.msgRaw(viewer, SharedUtil.color("<gray>Turn Order (Hover or click to view hand):"));
			showAllHands(viewer);
			viewer.sendMessage(SharedUtil.createText("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]",
					createTaskHover(), ClickEvent.runCommand("/thecrew viewtasks")));
		}
		else if (phase == GamePhase.LOSE) {
			Util.msgRaw(viewer, "<red>You lost! Waiting for host to decide what to do next...");
			Util.msgRaw(viewer, SharedUtil.color("<red>" + lossReason.getOwner().getName() + " failed to perform task: ")
					.append(lossReason.getDisplay()));
			showAllHands(viewer);
			viewer.sendMessage(SharedUtil.createText("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]",
					createTaskHover(), ClickEvent.runCommand("/thecrew viewtasks")));
		}
		else {
			displayInfo(viewer, viewer);
		}
	}
	
	public void showAllHands(Player viewer) {
		int num = 0;
		for (TheCrewPlayer tcp : turnOrder) {
			String text = "<gray>- <red>" + tcp.getName() + " </red><gold>" + tcp.getWins() + "W</gold>";
			if (tcp.getUniqueId().equals(captain.getUniqueId())) {
				text += " (<dark_red>Captain</dark_red>)";
			}
			text += ": ";
			text += pile.size() > num ? pile.get(num).getDisplay() : "???";
			viewer.sendMessage(SharedUtil.createText(text,
					tcp.createHandHoverText(), ClickEvent.runCommand("/thecrew viewhand " + tcp.getName())));
		}
		viewer.sendMessage(SharedUtil.createText("<dark_gray>[<gray>Click or hover to view accepted tasks</gray>]",
				createTaskHover(), ClickEvent.runCommand("/thecrew viewtasks")));
	}
	
	public void broadcastInfo() {
		for (GamePlayer gp : players.values()) {
			displayInfo(gp.getPlayer(), gp.getPlayer());
		}
		for (Player spec : spectators) {
			displaySpectatorInfo(spec);
		}
	}
	
	private void setupGame() {
		// Pass out deck
		ArrayList<TheCrewCardInstance> deck = TheCrew.createDeck();
		while (deck.size() >= players.size()) {
			totalRounds++;
			for (TheCrewPlayer p : players.values()) {
				TheCrewCardInstance card = deck.remove(0);
				p.addCard(card);
				card.setPlayer(p);
				if (card.getType() == CardType.SUB && card.getValue() == 4) {
					captain = p;
				}
			}
		}
		turnOrder.addAll(players.values());
		
		// Edge case if captain is null because it was the last card left
		if (captain == null) {
			TheCrewPlayer cpt = turnOrder.get(new Random().nextInt(players.size()));
			cpt.addCard(deck.remove(0));
		}

		// Announce captain and turn order
		Collections.sort(turnOrder, new Comparator<TheCrewPlayer>() {
			@Override
			public int compare(TheCrewPlayer c1, TheCrewPlayer c2) {
				return c1.getName().compareTo(c2.getName());
			}
		});
		setFirst(captain.getUniqueId());
		broadcast("The captain is <yellow>" + captain.getName() + "</yellow>!");

		// Sort and display hands
		for (TheCrewPlayer p : turnOrder) {
			p.sortHand();
			p.displayHand(p.getPlayer());
			for (Player spec : spectators) {
				p.displayHand(spec);
			}
		}

		rollTasks(2);
		phase = GamePhase.WAITING;
	}
	
	public void restartRound(Player p) {
		if (phase != GamePhase.LOSE && phase != GamePhase.PLAY) {
			Util.displayError(p, "You can't do that right now!");
			return;
		}
		
		if (!p.getUniqueId().equals(host)) {
			Util.displayError(p, "Only the host may restart rounds!");
			return;
		}
		
		// Return cards to each player
		for (TheCrewCardInstance card : pile) {
			card.getPlayer().addCard(card);
			card.getPlayer().sortHand();
		}
		pile.clear();
		
		broadcast("The host chose to restart the round!");
		phase = GamePhase.PLAY;
		startRound(this.round);
	}
	
	public void restartGame(Player p) {
		if (phase != GamePhase.LOSE && phase != GamePhase.PLAY) {
			Util.displayError(p, "You can't do that right now!");
			return;
		}
		
		if (!p.getUniqueId().equals(host)) {
			Util.displayError(p, "Only the host may restart games!");
			return;
		}
		
		// Return all cards to each player
		for (TheCrewCardInstance card : pile) {
			card.getPlayer().addCard(card);
		}
		pile.clear();
		for (TheCrewPlayer tcp : turnOrder) {
			ArrayList<TheCrewCardInstance> cardsWon = tcp.getCardsWon();
			for (TheCrewCardInstance card : cardsWon) {
				card.getPlayer().addCard(card);
			}
			cardsWon.clear();
			tcp.sortHand();
			
			for (TheCrewTask task : tcp.getTasks()) {
				task.reset();
			}
		}

		broadcast("The host chose to restart the game from round 1!");
		setFirst(captain.getUniqueId());
		phase = GamePhase.PLAY;
		startRound(1);
	}
	
	public void setFirst(UUID uuid) {
		while (!uuid.equals(turnOrder.get(0).getUniqueId())) {
			turnOrder.add(turnOrder.remove(0)); // Shift right
		}
	}
	
	public void displayHand(String viewed, Player viewer) {
		TheCrewPlayer p = players.get(viewed.toLowerCase());
		if (p == null) {
			Util.displayError(viewer, "That player isn't in this game!");
			return;
		}
	}
	
	public void promptPlayer() {
		if (phase == GamePhase.ROLL_TASKS) {
			for (TheCrewTask task : tasks) {
				Component ct = Component.text("- ", NamedTextColor.GRAY).append(task.getDisplay())
						.append(SharedUtil.color(" (Difficulty: <yellow>" + task.getDifficulty(players.size()) + "</yellow>)"));
				Util.msgRaw(captain.getPlayer(), ct);
			}
			Component c = SharedUtil.createText("<dark_gray>[<green>Accept Tasks</green>] ",
					"Click to accept!", ClickEvent.runCommand("/thecrew accepttasks"));
			c = c.append(SharedUtil.createText("<dark_gray>[<red>Reroll Tasks</red>]",
					"Click to reroll!", ClickEvent.runCommand("/thecrew rerolltasks")));
			captain.getPlayer().sendMessage(c);
		}
		else if (phase == GamePhase.SELECT_TASKS) {
			Player p = turnOrder.get(turn).getPlayer();
			p.sendMessage(SharedUtil.createText("<dark_gray>[<gray>Click or hover to view accepted tasks</gray]",
					createTaskHover(), ClickEvent.runCommand("/thecrew viewtasks")));
			players.get(p.getName().toLowerCase()).displayHand(p);
			Util.msgRaw(p, "<gray>Choose a task:");
			int num = 0;
			for (TheCrewTask task : tasks) {
				Builder b = Component.text().content("- ").color(NamedTextColor.GRAY);
				b.append(task.getDisplay());
				b.append(SharedUtil.createText(" <gray>(Difficulty: <yellow>" + task.getDifficulty(players.size()) + "</yellow>)",
					"Click to accept!", ClickEvent.runCommand("/thecrew accepttask " + num++)));
				p.sendMessage(b.build());
			}

			int remainingPlayers = players.size() - turn;
			
			if (tasks.size() != remainingPlayers) {
				p.sendMessage(SharedUtil.createText("<dark_gray>[<gray>Click to pass</gray>]",
						"This means players after you will\nhave to accept these tasks!",
						ClickEvent.runCommand("/thecrew passtask")));
			}
		}
		else {
			Player p = turnOrder.get(turn).getPlayer();
			displayInfo(p, p);
			
			if (phase == GamePhase.PLAY) {
				for (TheCrewPlayer tcp : players.values()) {
					if (tcp.getUniqueId().equals(p.getUniqueId())) continue;
					
					tcp.displayHand(tcp.getPlayer());
				}
			}
		}
	}
	
	public void play(Player p, int num) {
		if (phase != GamePhase.PLAY) {
			Util.displayError(p, "You can't do that during this phase!");
			return;
		}
		
		if (!turnOrder.get(turn).getPlayer().getUniqueId().equals(p.getUniqueId())) {
			Util.displayError(p, "It's not your turn right now!");
			return;
		}
		
		TheCrewPlayer tcp = turnOrder.get(turn);
		TheCrewCard toPlay = tcp.getCard(num);
		if (!pile.isEmpty()) { // First card can choose any card without restriction
			TheCrewCard topCard = pile.get(0);
			if (!toPlay.isSimilar(topCard) && tcp.hasSimilarCard(pile.get(0))) {
				Util.displayError(p, "You must play a card that's the same type as the top card!");
				return;
			}
		}
		
		pile.add(pile.size(), tcp.playCard(num));
		Component c = SharedUtil.color("<yellow>" + p.getName() + " <gray>plays ");
		broadcast(c.append(toPlay.getDisplay()));

		int time = 1;
		if (pile.size() == players.size()) {
			runLater(() -> {
				calculateTrickWinner();
				advanceTurn();
			}, time);
		}
		else {
			advanceTurn();
		}
	}
	
	public void calculateTrickWinner() {
		TheCrewCard winningCard = pile.get(0);
		int winningPlayer = 0;
		for (int i = 1; i < pile.size(); i++) {
			TheCrewCard comp = pile.get(i);
			// Condition 1: Cards are the same type and comp is higher value
			// Condition 2: Comp is sub and winning card is not
			if (comp.getType() == winningCard.getType() && comp.getValue() > winningCard.getValue()
					|| comp.getType() == CardType.SUB && winningCard.getType() != CardType.SUB) {
				winningCard = comp;
				winningPlayer = i;
			}
		}
		
		TheCrewPlayer tcp = turnOrder.get(winningPlayer);
		
		// Figure out what tasks have completed
		tcp.addWin(1); // Temporarily add win to accurately check task fails
		for (TheCrewPlayer p : turnOrder) {
			for (TheCrewTask task : p.getTasks()) {
				if (task.isComplete()) continue; // Skip completed tasks
				if (task.hasFailed(this, tcp, pile)) {
					tcp.addWin(-1); // Reset win so round restart works
					phase = GamePhase.LOSE;
					lossReason = task;
					broadcastInfo();
					return;
				}
			}
		}
		tcp.addWin(-1); // Reset win because tcp.winTrick adds it later
		
		boolean win = true;
		for (TheCrewPlayer p : turnOrder) {
			for (TheCrewTask task : p.getTasks()) {
				if (task.isComplete()) continue; // Skip completed tasks
				if (task.update(this, tcp, pile)) {
					task.setComplete(true);
					Component c = SharedUtil.color("<yellow>" + p.getName() + " <gray>completed task: ");
					broadcast(c.append(task.getDisplay()));
				}
				else {
					win = false;
				}
			}
		}
		
		tcp.winTrick(pile);
		Component cmp = SharedUtil.color("<yellow>" + tcp.getName() + " <gray>won the round with ");
		broadcast(cmp.append(winningCard.getDisplay()).append(Component.text("!")));
		setFirst(tcp.getUniqueId());
		
		// Remove revealed cards if they were played (after it's not possible to lose)
		for (TheCrewPlayer p : players.values()) {
			Iterator<TheCrewCardInstance> revealed = p.getRevealedCards().iterator();
			while (revealed.hasNext()) {
				TheCrewCard card = revealed.next();
				for (TheCrewCard c : pile) {
					if (c.equals(card)) {
						revealed.remove();
						break;
					}
				}
			}
		}
		
		if (win) {
			phase = GamePhase.WIN;
			runLater(() -> {
				broadcast("<green>You won! Sending you back to lobby in 3 seconds...");
			}, 1);
			
			runLater(() -> {
				endGame();
			}, 4);
		}
	}
	
	public void acceptTask(Player p, int num) {
		if (phase != GamePhase.SELECT_TASKS) {
			Util.displayError(p, "You can't do that during this phase!");
			return;
		}
		
		if (!turnOrder.get(turn).getPlayer().getUniqueId().equals(p.getUniqueId())) {
			Util.displayError(p, "It's not your turn right now!");
			return;
		}
		TheCrewPlayer tcp = players.get(p.getName().toLowerCase());
		
		// Special case for not allowing captain to choose captain comparison tasks
		if (tasks.get(num) instanceof CompareWinsTask) {
			if (((CompareWinsTask) tasks.get(num)).comparesCaptain() && captain.equals(tcp)) {
				Util.displayError(p, "The captain can't choose this task!");
				return;
			}
		}
		
		TheCrewTask task = tasks.get(num);
		
		// Special case for predicting a task
		if (task instanceof WinTricksPredictTask) {
			showPredictionDialog(p, (WinTricksPredictTask) task);
		}
		else {
			tasks.remove(num);
			tcp.addTask(task.clone(tcp, this));
			completeAcceptTask(task, tcp);
		}
	}

	private void showPredictionDialog(Player player, WinTricksPredictTask task) {
		Dialog dialog = Dialog.create(builder -> builder.empty()
				.base(DialogBase.builder(Component.text("Predict tricks won"))
						.canCloseWithEscape(true)
						.pause(false)
						.afterAction(DialogBase.DialogAfterAction.CLOSE)
						.body(java.util.List.of(DialogBody.plainMessage(
								Component.text("Choose exactly how many tricks you'll win. Cancel leaves the task unassigned."))))
						.inputs(java.util.List.of(DialogInput.numberRange(
								"tricks", Component.text("Tricks won"), 0, totalRounds)
								.initial(0.0F).step(1.0F).build()))
						.build())
				.type(DialogType.confirmation(
						ActionButton.builder(Component.text("Accept task"))
								.action(DialogAction.customClick((response, audience) -> {
									if (!(audience instanceof Player)
											|| !((Player) audience).getUniqueId().equals(player.getUniqueId())) {
										audience.sendMessage(Component.text("This prediction belongs to another player.", NamedTextColor.RED));
										return;
									}
									Player respondent = (Player) audience;
									Float amount = response.getFloat("tricks");
									AshTabletop.scheduler().runTask(AshTabletop.inst(),
											() -> acceptPrediction(respondent, task, amount));
								}, ClickCallback.Options.builder().uses(1).build()))
								.build(),
						ActionButton.builder(Component.text("Cancel")).build())));
		player.showDialog(dialog);
	}

	void acceptPrediction(Player player, WinTricksPredictTask task, Float amount) {
		if (!player.isOnline() || GameManager.getSession(player.getUniqueId()) != this
				|| phase != GamePhase.SELECT_TASKS || !tasks.contains(task)
				|| !turnOrder.get(turn).getUniqueId().equals(player.getUniqueId())) {
			Util.displayError(player, "That prediction is no longer available. View the game to choose a task.");
			return;
		}
		if (amount == null || !Float.isFinite(amount) || amount < 0 || amount > totalRounds
				|| amount != Math.floor(amount)) {
			Util.displayError(player, "Your prediction must be a whole number between 0 and " + totalRounds + "!");
			showPredictionDialog(player, task);
			return;
		}
		TheCrewPlayer owner = turnOrder.get(turn);
		WinTricksPredictTask accepted = task.clone(owner, this, amount.intValue());
		owner.addTask(accepted);
		tasks.remove(task);
		completeAcceptTask(accepted, owner);
	}
	
	private void completeAcceptTask(TheCrewTask task, TheCrewPlayer tcp) {
		broadcast(SharedUtil.color("<yellow>" + tcp.getName() + " <gray>has accepted task: ").append(task.getDisplay()));
		
		if (tasks.isEmpty()) {
			broadcast("Task assignment completed! Starting game...");
			phase = GamePhase.PLAY;
			startRound(1);
		}
		else {
			advanceTurn();
		}
	}
	
	public void passTask(Player p) {
		if (phase != GamePhase.SELECT_TASKS) {
			Util.msgRaw(p, "<red>You can't do that during this phase!");
			return;
		}
		
		if (!turnOrder.get(turn).getPlayer().getUniqueId().equals(p.getUniqueId())) {
			Util.msgRaw(p, "<red>It's not your turn right now!");
			return;
		}
		
		int remainingPlayers = players.size() - turn;
		if (tasks.size() == remainingPlayers) {
			Util.msgRaw(p, "<red>All remaining tasks must be accepted this round!");
			return;
		}
		
		broadcast("<yellow>" + p.getName() + " <gray>has chosen not to accept a task.");
		advanceTurn();
	}
	
	public void acceptTasks(Player p) {
		if (phase != GamePhase.ROLL_TASKS) {
			Util.displayError(p, "You can't do that during this phase!");
			return;
		}
		
		if (!captain.getPlayer().getUniqueId().equals(p.getUniqueId())) {
			Util.displayError(p, "Only the captain may reroll tasks!");
			return;
		}
		
		broadcast("The captain chose to <green>accept </green>the tasks!");
		phase = GamePhase.SELECT_TASKS;
		startRound(1);
	}
	
	public void startRound(int round) {
		GamePhase temp = phase;
		phase = GamePhase.WAITING;
		this.round = round;
		turn = 0;
		displayTurn(temp, true);
	}
	
	public void advanceTurn() {
		if (phase == GamePhase.LOSE || phase == GamePhase.WIN) return;
		
		GamePhase temp = phase;
		phase = GamePhase.WAITING;
		turn++;
		if (turn >= players.size()) {
			turn = 0;
			round++;
		}
		displayTurn(temp, turn == 0);
	}
	
	public void displayTurn(GamePhase temp, boolean showRound) {
		int time = 1;
		if (showRound) {
			broadcast("Round " + round + "...");
		}
		runLater(() -> {
			phase = temp;
			promptPlayer();
			broadcast("<yellow>" + turnOrder.get(turn).getName() + "<gray>'s turn");
		}, time);
	}
	
	public void rerollTasks(Player p) {
		if (phase != GamePhase.ROLL_TASKS) {
			Util.msgRaw(p, "<red>You can't do that during this phase!");
			return;
		}
		
		if (!captain.getPlayer().getUniqueId().equals(p.getUniqueId())) {
			Util.msgRaw(p, "<red>Only the captain may reroll tasks!");
			return;
		}
		
		broadcast("The captain chose to <red>reroll </red>the tasks!");
		phase = GamePhase.WAITING;
		rollTasks(1);
	}
	
	// Use time 2 for first setup, time 1 for reroll
	private void rollTasks(int time) {
		runLater(() -> {
			broadcast("Rolling tasks...");
		}, time);

		time += 2;
		tasks = getTasks();
		for (TheCrewTask task : tasks) {
			runLater(() -> {
				Component ct = Component.text("- ", NamedTextColor.GRAY).append(task.getDisplay())
						.append(SharedUtil.color(" (Difficulty: <yellow>" + task.getDifficulty(players.size()) + "</yellow>)"));
				broadcast(ct);
			}, time++);
		}

		runLater(() -> {
			broadcast("The captain is responsible for accepting tasks or rerolling! Note that some tasks "
					+ "may be impossible together. Look carefully before accepting them!");
		}, time++);

		runLater(() -> {
			Component accept = SharedUtil.createText("<dark_gray>[<green>Accept Tasks</green>] ", "Click to accept!",
					ClickEvent.runCommand("/thecrew accepttasks"));
			Component reroll = SharedUtil.createText("<dark_gray>[<red>Reroll Tasks</red>] ", "Click to reroll!",
					ClickEvent.runCommand("/thecrew rerolltasks"));
			
			captain.getPlayer().sendMessage(accept.append(reroll));
			phase = GamePhase.ROLL_TASKS;
		}, time);
	}
	
	private Component createTaskHover() {
		Builder b = Component.text().color(NamedTextColor.GRAY);
		int count = 0;
		for (TheCrewPlayer tcp : turnOrder) {
			b.append(Component.text(tcp.getName(), NamedTextColor.YELLOW))
				.append(Component.text(":"));
			for (TheCrewTask task : tcp.getTasks()) {
				if (task.isComplete()) {
					Component c = Component.text().content("- " + task.getDisplayString())
							.color(NamedTextColor.GRAY)
							.decorate(TextDecoration.ITALIC, TextDecoration.UNDERLINED).build();
					b.append(c);
				}
				else {
					b.appendSpace().append(task.getDisplay());
				}
			}
			if (++count < turnOrder.size()) {
				b.appendNewline();
			}
		}
		return b.build();
	}
	
	public void displayTasks(Player p) {
		for (TheCrewPlayer tcp : turnOrder) {
			Util.msgRaw(p, SharedUtil.color("<yellow>" + tcp.getName() + "</yellow>:"));
			for (TheCrewTask task : tcp.getTasks()) {
				if (task.isComplete()) {
					Component c = Component.text().content("- " + task.getDisplayString())
							.color(NamedTextColor.GRAY)
							.decorate(TextDecoration.ITALIC, TextDecoration.UNDERLINED).build();
					Util.msgRaw(p, c);
				}
				else {
					Util.msgRaw(p, Component.text("- ", NamedTextColor.GRAY).append(task.getDisplay()));
				}
			}
		}
	}
	
	private ArrayList<TheCrewTask> getTasks() {
		int diff = difficulty;
		ArrayList<TheCrewTask> tasks = new ArrayList<TheCrewTask>();
		while (diff > 0) {
			TheCrewTask task = TheCrew.getTask();
			if (task.getDifficulty(players.size()) > diff) continue;
			
			diff -= task.getDifficulty(players.size());
			tasks.add(task);
		}
		return tasks;
	}
	
	public void viewHand(String name, Player viewer) {
		TheCrewPlayer tcp = players.get(name.toLowerCase());
		tcp.displayHand(viewer);
	}
	
	public int getRound() {
		return round;
	}
	
	public int getRoundsLeft() {
		return totalRounds - round;
	}
	
	public ArrayList<TheCrewPlayer> getTurnOrder() {
		return turnOrder;
	}
	
	public TheCrewPlayer getCaptain() {
		return captain;
	}

	private enum GamePhase {
		SETUP,
		WAITING,
		ROLL_TASKS,
		SELECT_TASKS,
		PREPLAY,
		PLAY,
		WIN,
		LOSE;
	}

	@Override
	public void showDebug(CommandSender s) {
		Util.msgRaw(s, "Round: " + round + ", Total Rounds: " + totalRounds + ", Phase: " + phase + ", Turn: " + turn);
		Util.msgRaw(s, "Turn Order: " + turnOrder);
		for (TheCrewPlayer p : turnOrder) {
			Util.msgRaw(s, p.getName() + " tasks, cardValues: " + p.getCardValues());
			for (TheCrewTask task : p.getTasks()) {
				Util.msgRaw(s, "- " + (task.isComplete() ? "(done) " : "") + task.getDisplay());
				task.showDebug(s);
			}
		}
	}
}
