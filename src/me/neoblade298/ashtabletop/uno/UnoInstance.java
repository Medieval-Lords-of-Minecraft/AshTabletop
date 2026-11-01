package me.neoblade298.ashtabletop.uno;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.entity.Player;

import me.neoblade298.ashtabletop.AshTabletop;
import me.neoblade298.ashtabletop.ChatGameInstance;
import me.neoblade298.ashtabletop.GameLobby;
import me.neoblade298.ashtabletop.GamePlayer;
import me.neoblade298.ashtabletop.uno.UnoCard.Color;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class UnoInstance extends ChatGameInstance<UnoPlayer> {
	private final List<UnoPlayer> order = new ArrayList<>();
	private final List<UnoCard> deck = new ArrayList<>(), discard = new ArrayList<>();
	private final int winningScore;
	private int turn, direction = 1, pendingDraw;
	private UnoCard top;
	private Color color;
	private boolean choosingColor;
	private int matchingNumber;

	public UnoInstance(UnoLobby lobby) {
		super(lobby);
		winningScore = (Integer) params.get("points").get();
		for (UUID uuid : lobby.getPlayers()) {
			Player player = AshTabletop.findPlayer(uuid).orElseThrow();
			UnoPlayer gp = new UnoPlayer(player);
			players.put(gp.getName().toLowerCase(Locale.ROOT), gp);
			order.add(gp);
		}
		runLater(this::startRound, 1);
	}

	private UnoPlayer current() {
		return order.get(turn);
	}

	@Override
	protected String waitingFor() {
		return !order.isEmpty() && !current().getPlayer().isOnline() ? current().getName() : null;
	}

	private void startRound() {
		revision++;
		deck.clear();
		discard.clear();
		initializeDeck();
		Collections.shuffle(order);
		turn = 0;
		direction = 1;
		pendingDraw = 0;
		choosingColor = false;
		for (UnoPlayer gp : order) {
			gp.hand.clear();
			gp.calledUno = false;
			for (int i = 0; i < 7; i++) gp.hand.add(drawOne());
		}
		top = drawOne();
		color = top.color();
		matchingNumber = top.number();
		started = true;
		broadcast(Component.text("A new Uno round has started!", NamedTextColor.YELLOW));
		showHands();
	}

	private void initializeDeck() {
		for (Color clr : Color.values()) {
			if (clr == Color.WILD) continue;
			for (int n = 0; n <= UnoCard.DRAW_TWO; n++) {
				deck.add(new UnoCard(n, clr));
				if (n != 0) deck.add(new UnoCard(n, clr));
			}
		}
		for (int i = 0; i < 4; i++) {
			deck.add(new UnoCard(UnoCard.WILD, Color.WILD));
			deck.add(new UnoCard(UnoCard.DRAW_FOUR, Color.WILD));
		}
		Collections.shuffle(deck);
	}

	private UnoCard drawOne() {
		if (deck.isEmpty()) {
			deck.addAll(discard);
			discard.clear();
			Collections.shuffle(deck);
		}
		if (deck.isEmpty()) {
			initializeDeck();
			broadcast(Component.text("All drawable Uno cards are exhausted. Adding a shuffled deck, following NeoUno's replenishment rule."));
		}
		return deck.remove(deck.size() - 1);
	}

	private boolean matches(UnoCard card) {
		return (color == Color.WILD || card.color() == Color.WILD || card.color() == color
				|| card.number() == matchingNumber) && (pendingDraw == 0 || card.addsDraws());
	}

	private void next(int count) {
		turn = Math.floorMod(turn + count * direction, order.size());
		current().calledUno = false;
		showHands();
	}

	private void showHands() {
		for (UnoPlayer gp : order) {
			if (gp.getPlayer().isOnline()) displayInfo(gp.getPlayer(), gp.getPlayer());
		}
		if (waitingFor() != null) broadcast(Component.text("Waiting for " + waitingFor() + " to reconnect.", NamedTextColor.RED));
	}

	@Override
	public List<String> commands() {
		return List.of("play", "draw", "color", "uno", "challenge", "hand", "showhand", "players", "menu", "quit");
	}

	@Override
	public void execute(Player player, String[] args) {
		String action = args.length == 0 ? "hand" : args[0].toLowerCase(Locale.ROOT);
		if (List.of("hand", "showhand", "players").contains(action) && args.length <= 1) {
			displayInfo(player, player);
			return;
		}
		if (action.equals("quit") && args.length == 1) {
			leavePlayer(player);
			return;
		}
		UnoPlayer gp = participant(player);
		if (gp == null || !ready(player)) return;
		if (action.equals("uno") && args.length == 1) {
			if (gp.calledUno || !(gp.hand.size() == 1 || (gp == current() && gp.hand.size() == 2))) {
				Util.displayError(player, "Call Uno with one card, or before playing your second-to-last card.");
				return;
			}
			revision++;
			gp.calledUno = true;
			broadcast(Component.text(gp + " calls Uno!", NamedTextColor.YELLOW));
			return;
		}
		if (action.equals("challenge") && args.length == 2) {
			UnoPlayer target = players.get(args[1].toLowerCase(Locale.ROOT));
			if (target == null || target == gp || target == current() || target.hand.size() != 1 || target.calledUno) {
				Util.displayError(player, "Challenge another player with one card who has finished their turn without calling Uno.");
				return;
			}
			revision++;
			target.hand.add(drawOne());
			target.hand.add(drawOne());
			broadcast(Component.text(gp + " caught " + target + " without Uno. They draw two cards."));
			showHands();
			return;
		}
		if (gp != current()) {
			Util.displayError(player, "It is " + current() + "'s turn.");
			return;
		}
		if (action.equals("menu") && args.length == 1) {
			menu(player);
			return;
		}
		if (action.equals("color") && args.length == 2) {
			Color selected = Color.parse(args[1]);
			if (!choosingColor || selected == null) {
				Util.displayError(player, "Choose red, blue, green or yellow after playing a wild card.");
				return;
			}
			revision++;
			color = selected;
			choosingColor = false;
			broadcast(Component.text(gp + " chose " + selected.name().toLowerCase(Locale.ROOT) + ".", selected.display));
			next(1);
			return;
		}
		if (choosingColor) {
			Util.displayError(player, "Choose a color with /uno color <r|b|g|y> or /uno menu.");
			return;
		}
		if (action.equals("play") && args.length == 2) {
			UnoCard card = gp.hand.stream().filter(c -> c.alias().equalsIgnoreCase(args[1])
					|| (c.number() == UnoCard.WILD && args[1].equalsIgnoreCase("wildcard"))).findFirst().orElse(null);
			if (card == null || !matches(card)) {
				Util.displayError(player, "You must own a matching card. Pending draws must be drawn or stacked with a matching +2 or +4.");
				return;
			}
			revision++;
			gp.hand.remove(card);
			play(gp, card);
			return;
		}
		if (action.equals("draw") && args.length == 1) {
			int count = pendingDraw > 0 ? pendingDraw : 1;
			revision++;
			if (pendingDraw > 0) {
				for (int i = 0; i < count; i++) gp.hand.add(drawOne());
				pendingDraw = 0;
				gp.calledUno = false;
				broadcast(Component.text(gp + " draws " + count + " cards."));
				next(1);
			}
			else {
				UnoCard card = drawOne();
				if (matches(card)) {
					broadcast(Component.text(gp + " draws and automatically plays a card."));
					play(gp, card);
				}
				else {
					gp.hand.add(card);
					gp.calledUno = false;
					broadcast(Component.text(gp + " draws a card."));
					next(1);
				}
			}
			return;
		}
		Util.displayError(player, "Commands: /uno play <7r|skipb|reverseg|+2y|wild|+4>, draw, color <r|b|g|y>, uno, challenge <player>, hand, players, menu, quit.");
	}

	private void play(UnoPlayer gp, UnoCard card) {
		discard.add(top);
		int previousNumber = matchingNumber;
		top = card;
		color = card.color();
		matchingNumber = color == Color.WILD ? previousNumber : card.number();
		broadcast(Component.text(gp + " plays ").append(card.display()).append(Component.text("; " + gp.hand.size() + " cards left.")));
		if (gp.hand.size() > 1) gp.calledUno = false;
		if (card.number() == UnoCard.DRAW_TWO) pendingDraw += 2;
		if (card.number() == UnoCard.DRAW_FOUR) pendingDraw += 4;
		if (gp.hand.isEmpty()) {
			int points = order.stream().flatMap(p -> p.hand.stream()).mapToInt(UnoCard::points).sum();
			gp.points += points;
			broadcast(Component.text(gp + " wins the round and gains " + points + " points!", NamedTextColor.GREEN));
			for (UnoPlayer p : order) broadcast(Component.text(p + ": " + p.points + " points"));
			started = false;
			if (winningScore > 0 && gp.points >= winningScore) {
				broadcast(Component.text(gp + " wins the game!", NamedTextColor.GREEN));
				endGame();
			}
			else runLater(this::startRound, 3);
			return;
		}
		if (card.color() == Color.WILD) {
			choosingColor = true;
			displayInfo(gp.getPlayer(), gp.getPlayer());
			menu(gp.getPlayer());
		}
		else if (card.number() == UnoCard.SKIP) next(2);
		else if (card.number() == UnoCard.REVERSE) {
			direction = -direction;
			next(1);
		}
		else next(1);
	}

	private void menu(Player player) {
		if (choosingColor) {
			choices(player, "Choose Uno color", "Cancel leaves the color pending. Reopen with /uno menu.",
					new String[][] {{"Red", "color r"}, {"Blue", "color b"}, {"Green", "color g"}, {"Yellow", "color y"}});
			return;
		}
		List<String[]> options = new ArrayList<>();
		for (UnoCard card : current().hand.stream().distinct().toList()) {
			if (matches(card)) options.add(new String[] {card.alias(), "play " + card.alias()});
		}
		options.add(new String[] {"Draw", "draw"});
		options.add(new String[] {"Call Uno", "uno"});
		choices(player, "Uno hand", "Top card: " + top.alias() + ". Current color: " + color + ".",
				options.toArray(String[][]::new));
	}

	@Override
	public void displayInfo(Player viewer, Player viewed) {
		if (!started || top == null) {
			message(viewer, "The next Uno round is starting.");
			return;
		}
		message(viewer, "Uno: " + current() + "'s turn; color " + color + "; pending draws " + pendingDraw);
		viewer.sendMessage(Component.text("Top card: ").append(top.display()));
		for (UnoPlayer gp : order) {
			message(viewer, gp + ": " + gp.hand.size() + " cards, " + gp.points + " points"
					+ (gp.getPlayer().isOnline() ? "" : " (offline)"));
		}
		UnoPlayer owner = players.get(viewer.getName().toLowerCase(Locale.ROOT));
		if (owner == null) return;
		Component hand = Component.text("Your hand: ", NamedTextColor.GRAY);
		for (UnoCard card : owner.hand) {
			hand = hand.append(card.display().clickEvent(ClickEvent.runCommand("/uno play " + card.alias())))
					.append(Component.space());
		}
		viewer.sendMessage(hand.append(button("Draw", "/uno draw")).append(button("Uno", "/uno uno"))
				.append(button("Dialog", "/uno menu")));
	}

	@Override
	public void handleLeave(GamePlayer player) {
		revision++;
		UnoPlayer gp = (UnoPlayer) player;
		UnoPlayer oldCurrent = order.isEmpty() ? null : current();
		int index = order.indexOf(gp);
		discard.addAll(gp.hand);
		order.remove(gp);
		if (order.size() < 2) {
			if (!order.isEmpty()) broadcast(Component.text(order.get(0) + " wins by default!", NamedTextColor.GREEN));
			endGame();
			return;
		}
		if (gp == oldCurrent) {
			turn = Math.floorMod(direction == 1 ? index : index - 1, order.size());
			current().calledUno = false;
			if (choosingColor) broadcast(Component.text("The new current player must choose the pending wild color with /uno menu."));
		}
		else turn = order.indexOf(oldCurrent);
		showHands();
	}

	@Override
	public GameLobby<UnoPlayer> onEnd() {
		revision++;
		started = false;
		return new UnoLobby(this);
	}
}
