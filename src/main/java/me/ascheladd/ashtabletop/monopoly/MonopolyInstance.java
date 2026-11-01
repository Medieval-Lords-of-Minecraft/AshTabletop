package me.ascheladd.ashtabletop.monopoly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;

import me.ascheladd.ashtabletop.AshTabletop;
import me.ascheladd.ashtabletop.ChatGameInstance;
import me.ascheladd.ashtabletop.GameLobby;
import me.ascheladd.ashtabletop.GamePlayer;
import me.ascheladd.ashtabletop.GameManager;
import me.ascheladd.ashtabletop.monopoly.Monopoly.EventCard;
import me.ascheladd.ashtabletop.monopoly.MonopolyPlayer.Debt;
import me.ascheladd.ashtabletop.monopoly.MonopolySpace.Type;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MonopolyInstance extends ChatGameInstance<MonopolyPlayer> {
	private enum Action { ORDER, ROLL, JAIL, BUY, UTILITY, NONE }

	private final List<MonopolySpace> board;
	private final List<MonopolyPlayer> order = new ArrayList<>();
	private final Map<MonopolyPlayer, Integer> orderRolls = new HashMap<>();
	private final EventDeck chance, chest;
	private int turn, doubles, houses = 32, hotels = 12;
	private Action action = Action.ORDER;
	private boolean decidingOrder = true;
	private boolean rollAgain;
	private Auction auction;
	private Trade trade;

	private static class EventDeck {
		final List<EventCard> cards;
		int next;

		EventDeck(List<EventCard> source) {
			cards = new ArrayList<>(source);
			Collections.shuffle(cards);
		}

		EventCard draw() {
			if (next == cards.size()) {
				Collections.shuffle(cards);
				next = 0;
			}
			return cards.get(next++);
		}
	}

	private static class Auction {
		final MonopolySpace property;
		final Set<MonopolyPlayer> bidders;
		MonopolyPlayer topBidder;
		long bid;

		Auction(MonopolySpace property, List<MonopolyPlayer> players) {
			this.property = property;
			bidders = new LinkedHashSet<>(players);
		}
	}

	private static class Offer {
		final Set<MonopolySpace> properties = new LinkedHashSet<>();
		int money, jailFree;
	}

	private static class Trade {
		final MonopolyPlayer a, b;
		final Offer offerA = new Offer(), offerB = new Offer();
		boolean confirmA, confirmB;

		Trade(MonopolyPlayer a, MonopolyPlayer b) {
			this.a = a;
			this.b = b;
		}

		Offer offer(MonopolyPlayer player) { return player == a ? offerA : offerB; }
		MonopolyPlayer other(MonopolyPlayer player) { return player == a ? b : a; }
		boolean contains(MonopolyPlayer player) { return player == a || player == b; }
	}

	public MonopolyInstance(MonopolyLobby lobby) {
		super(lobby);
		board = Monopoly.inst().createBoard();
		chance = new EventDeck(Monopoly.inst().chance());
		chest = new EventDeck(Monopoly.inst().chest());
		int money = (Integer) params.get("money").get();
		char token = 'a';
		for (UUID uuid : lobby.getPlayers()) {
			MonopolyPlayer gp = new MonopolyPlayer(AshTabletop.findPlayer(uuid).orElseThrow(), money, token++);
			players.put(gp.getName().toLowerCase(Locale.ROOT), gp);
			order.add(gp);
		}
		runLater(() -> {
			started = true;
			broadcast(Component.text("Roll to decide Monopoly turn order. " + current() + " rolls first.", NamedTextColor.YELLOW));
			displayInfo(current().getPlayer(), current().getPlayer());
		}, 1);
	}

	private MonopolyPlayer current() { return order.get(turn); }

	@Override
	protected String waitingFor() {
		if (!order.isEmpty() && !current().getPlayer().isOnline()) return current().getName();
		for (MonopolyPlayer gp : order) {
			if (!gp.debts.isEmpty() && !gp.getPlayer().isOnline()) return gp.getName();
		}
		if (auction != null) {
			for (MonopolyPlayer gp : auction.bidders) if (!gp.getPlayer().isOnline()) return gp.getName();
		}
		if (trade != null) {
			if (!trade.a.getPlayer().isOnline()) return trade.a.getName();
			if (!trade.b.getPlayer().isOnline()) return trade.b.getName();
		}
		return null;
	}

	@Override
	public List<String> commands() {
		return List.of("roll", "end", "buy", "auction", "bid", "build", "destroy", "destroyhotel",
				"mortgage", "unmortgage", "paybills", "payjail", "jailfree", "bankrupt", "trade",
				"offer", "request", "confirm", "unconfirm", "cancel",
				"summary", "list", "view", "color", "pos", "map", "checkbuild", "menu", "quit");
	}

	@Override
	public void execute(Player player, String[] args) {
		String command = args.length == 0 ? "summary" : args[0].toLowerCase(Locale.ROOT);
		if (List.of("offer", "request", "confirm", "unconfirm", "cancel").contains(command)) {
			String[] tradeArgs = new String[args.length + 1];
			tradeArgs[0] = "trade";
			System.arraycopy(args, 0, tradeArgs, 1, args.length);
			execute(player, tradeArgs);
			return;
		}
		String value = String.join(" ", Arrays.copyOfRange(args, Math.min(1, args.length), args.length));
		switch (command) {
			case "summary", "list" -> {
				MonopolyPlayer target = value.isEmpty() ? players.get(player.getName().toLowerCase(Locale.ROOT))
						: players.get(value.toLowerCase(Locale.ROOT));
				if (target == null && value.isEmpty()) displayInfo(player, player);
				else if (target == null) Util.displayError(player, "That player is not in this game.");
				else summary(player, target);
				return;
			}
			case "pos" -> { positions(player); return; }
			case "map" -> { map(player); return; }
			case "checkbuild" -> { message(player, houses + " houses and " + hotels + " hotels remain in the bank."); return; }
			case "view" -> {
				MonopolySpace space = findSpace(player, value);
				if (space != null) message(player, space.details());
				return;
			}
			case "color" -> { showColor(player, args); return; }
			case "quit" -> {
				if (args.length != 1) { help(player); return; }
				leavePlayer(player);
				return;
			}
		}
		if (command.equals("trade") && args.length == 2 && args[1].equalsIgnoreCase("view")) {
			showTrade(player);
			return;
		}
		if (command.equals("auction") && args.length == 2 && args[1].equalsIgnoreCase("view")) {
			showAuction(player);
			return;
		}
		MonopolyPlayer gp = participant(player);
		if (gp == null || !ready(player)) return;
		if (gp.bankrupt) { Util.displayError(player, "You are bankrupt and can only spectate or leave."); return; }
		if (command.equals("menu") && args.length == 1) { menu(player, gp); return; }
		if (trade != null && !command.equals("trade") && !command.equals("bankrupt")) {
			Util.displayError(player, "Resolve or cancel the active trade first.");
			return;
		}
		if (auction != null && !List.of("auction", "bid", "bankrupt").contains(command)) {
			Util.displayError(player, "Resolve the active auction first.");
			return;
		}
		switch (command) {
			case "roll" -> {
				if (args.length == 1 && ownTurn(player, gp)) roll(gp);
				else if (args.length != 1) help(player);
			}
			case "end" -> {
				if (args.length != 1) { help(player); return; }
				if (!ownTurn(player, gp)) return;
				if (action != Action.NONE || hasDebts()) {
					Util.displayError(player, "Resolve all required actions and outstanding bills before ending your turn.");
					return;
				}
				revision++;
				turn = (turn + 1) % order.size();
				startTurn();
			}
			case "buy" -> {
				if (args.length != 1) { help(player); return; }
				if (!ownTurn(player, gp) || !required(player, Action.BUY)) return;
				MonopolySpace property = board.get(gp.position);
				if (!afford(player, gp, property.price)) return;
				revision++;
				gp.money -= property.price;
				property.owner = gp;
				action = rollAgain ? Action.ROLL : Action.NONE;
				broadcast(Component.text(gp + " buys " + property.name + " for $" + property.price + "."));
				displayInfo(player, player);
			}
			case "auction" -> {
				if (args.length == 2 && args[1].equalsIgnoreCase("leave")) {
					leaveAuction(player, gp);
					return;
				}
				if (args.length != 1) { help(player); return; }
				if (!ownTurn(player, gp) || !required(player, Action.BUY)) return;
				revision++;
				auction = new Auction(board.get(gp.position), order);
				broadcast(Component.text("Auctioning " + auction.property.name + ". Bid with /mono bid <amount>; withdraw with /mono auction leave."));
			}
			case "bid" -> {
				if (args.length != 2) { help(player); return; }
				Integer bid = amount(player, args[1], Integer.MAX_VALUE);
				if (bid != null) bid(player, gp, bid);
			}
			case "build", "destroy", "destroyhotel", "mortgage", "unmortgage" -> {
				if (value.isEmpty()) { help(player); return; }
				if (!gp.debts.isEmpty() && !List.of("destroy", "destroyhotel", "mortgage").contains(command)) {
					Util.displayError(player, "Pay your bills before spending money.");
					return;
				}
				if (gp.debts.isEmpty() && !ownTurn(player, gp)) return;
				MonopolySpace property = findSpace(player, value);
				if (property == null) return;
				if (property.owner != gp) { Util.displayError(player, "You do not own that property."); return; }
				manageProperty(player, gp, property, command);
			}
			case "paybills" -> {
				if (args.length != 1) { help(player); return; }
				if (gp.debts.isEmpty()) { Util.displayError(player, "You have no outstanding bills."); return; }
				if (!afford(player, gp, gp.bills())) return;
				revision++;
				for (Debt debt : gp.debts) {
					gp.money -= debt.amount();
					if (debt.creditor() != null && !debt.creditor().bankrupt) debt.creditor().money += debt.amount();
				}
				gp.debts.clear();
				broadcast(Component.text(gp + " pays their outstanding bills."));
				displayInfo(player, player);
			}
			case "payjail", "jailfree" -> {
				if (args.length != 1) { help(player); return; }
				if (!ownTurn(player, gp) || !required(player, Action.JAIL)) return;
				if (command.equals("payjail") && !afford(player, gp, 50)) return;
				if (command.equals("jailfree") && gp.jailFree == 0) {
					Util.displayError(player, "You have no get out of jail free cards.");
					return;
				}
				revision++;
				if (command.equals("payjail")) gp.money -= 50;
				else gp.jailFree--;
				gp.jailed = false;
				gp.jailTurns = 0;
				action = Action.ROLL;
				broadcast(Component.text(gp + " leaves jail and can roll to move."));
			}
			case "bankrupt" -> {
				if (args.length != 1) { help(player); return; }
				if (!gp.debts.isEmpty() || ownTurn(player, gp)) {
					revision++;
					bankrupt(gp);
				}
			}
			case "trade" -> trade(player, gp, args);
			default -> help(player);
		}
	}

	private void help(Player player) {
		message(player, "/mono roll, end, buy, auction [view|leave], bid <money>, paybills, payjail, jailfree, bankrupt, quit");
		message(player, "/mono build|destroy|destroyhotel|mortgage|unmortgage|view <property name or board index>");
		message(player, "/mono summary|list [player], color <color> [player], pos, map, checkbuild, menu");
		message(player, "/mono trade <player>, trade offer|request money|jailfree <amount>, trade offer|request property <name>, trade confirm|cancel|view");
	}

	private boolean ownTurn(Player player, MonopolyPlayer gp) {
		if (gp == current()) return true;
		Util.displayError(player, "It is " + current() + "'s turn.");
		return false;
	}

	private boolean required(Player player, Action expected) {
		if (action == expected && !hasDebts()) return true;
		Util.displayError(player, "Required action: " + action + (hasDebts() ? "; outstanding bills must be paid first." : "."));
		return false;
	}

	private boolean hasDebts() { return order.stream().anyMatch(gp -> !gp.debts.isEmpty()); }

	private boolean afford(Player player, MonopolyPlayer gp, long cost) {
		if (gp.money >= cost) return true;
		Util.displayError(player, "You need $" + cost + " but have $" + gp.money + ".");
		return false;
	}

	private void startTurn() {
		doubles = 0;
		rollAgain = false;
		action = decidingOrder ? Action.ORDER : current().jailed ? Action.JAIL : Action.ROLL;
		broadcast(Component.text("It is now " + current() + "'s turn. Required action: " + action + ".", NamedTextColor.YELLOW));
		if (current().getPlayer().isOnline()) {
			displayInfo(current().getPlayer(), current().getPlayer());
			if (action == Action.JAIL) menu(current().getPlayer(), current());
		}
		else broadcast(Component.text("Waiting for " + current() + " to reconnect.", NamedTextColor.RED));
	}

	private void roll(MonopolyPlayer gp) {
		if (hasDebts() || !List.of(Action.ORDER, Action.ROLL, Action.JAIL, Action.UTILITY).contains(action)) {
			Util.displayError(gp.getPlayer(), "Resolve " + action + " and outstanding bills before rolling.");
			return;
		}
		revision++;
		int a = ThreadLocalRandom.current().nextInt(1, 7), b = ThreadLocalRandom.current().nextInt(1, 7);
		int total = a + b;
		broadcast(Component.text(gp + " rolls " + a + " + " + b + " = " + total + (a == b ? " (doubles)" : "") + "."));
		if (action == Action.ORDER) {
			orderRolls.put(gp, total);
			if (orderRolls.size() == order.size()) {
				order.sort(Comparator.comparingInt((MonopolyPlayer p) -> orderRolls.get(p)).reversed());
				decidingOrder = false;
				turn = 0;
				broadcast(Component.text("Turn order: " + String.join(", ", order.stream().map(MonopolyPlayer::getName).toList())));
			}
			else turn = (turn + 1) % order.size();
			startTurn();
			return;
		}
		if (action == Action.UTILITY) {
			action = rollAgain ? Action.ROLL : Action.NONE;
			bill(gp, total * 10L, board.get(gp.position).owner);
		}
		else if (action == Action.JAIL) {
			action = Action.NONE;
			if (a == b) {
				gp.jailed = false;
				gp.jailTurns = 0;
				move(gp, total, true, false, 0);
			}
			else if (++gp.jailTurns >= 3) {
				gp.jailed = false;
				gp.jailTurns = 0;
				bill(gp, 100, null);
				broadcast(Component.text(gp + " pays the third-failed-roll fine of $100 and leaves jail. End the turn after resolving bills."));
			}
		}
		else {
			if (a == b && ++doubles >= 3) { jail(gp); return; }
			rollAgain = a == b;
			action = a == b ? Action.ROLL : Action.NONE;
			move(gp, total, true, false, 0);
		}
		if (started && gp.getPlayer().isOnline()) displayInfo(gp.getPlayer(), gp.getPlayer());
	}

	private void move(MonopolyPlayer gp, int distance, boolean passGo, boolean special, int depth) {
		int absolute = gp.position + distance;
		if (passGo && distance > 0 && absolute >= board.size()) {
			gp.money += 200;
			broadcast(Component.text(gp + " passes Go and collects $200."));
		}
		gp.position = Math.floorMod(absolute, board.size());
		land(gp, distance, special, depth);
	}

	private void moveTo(MonopolyPlayer gp, int position, boolean special, int depth) {
		if (position <= gp.position) {
			gp.money += 200;
			broadcast(Component.text(gp + " passes Go and collects $200."));
		}
		gp.position = position;
		land(gp, 0, special, depth);
	}

	private void land(MonopolyPlayer gp, int dice, boolean special, int depth) {
		if (depth > 40) {
			AshTabletop.logger().severe("Monopoly configuration creates a non-terminating movement-card chain in " + name);
			started = false;
			revision++;
			GameManager.disbandInstance(this, "an invalid movement-card chain in the Monopoly configuration");
			return;
		}
		MonopolySpace space = board.get(gp.position);
		broadcast(Component.text(gp + " lands on ").append(space.display()));
		switch (space.type) {
			case PROPERTY, RAILROAD, UTILITY -> {
				if (space.owner == null) {
					action = Action.BUY;
					if (gp.getPlayer().isOnline()) menu(gp.getPlayer(), gp);
				}
				else if (space.owner != gp && !space.mortgaged) {
					if (special && space.type == Type.UTILITY) action = Action.UTILITY;
					else bill(gp, rent(space, dice) * (special && space.type == Type.RAILROAD ? 2L : 1L), space.owner);
				}
			}
			case TAX -> bill(gp, space.price, null);
			case GOTOJAIL -> jail(gp);
			case CHANCE -> draw(gp, chance, "Chance", depth + 1);
			case COMMUNITYCHEST -> draw(gp, chest, "Community Chest", depth + 1);
			default -> { }
		}
	}

	private long rent(MonopolySpace space, int dice) {
		if (space.mortgaged || space.owner == null) return 0;
		return switch (space.type) {
			case PROPERTY -> space.rents.get(space.buildings) * (space.buildings == 0 && monopoly(space) ? 2L : 1L);
			case RAILROAD -> space.rents.get((int) board.stream().filter(s -> s.type == Type.RAILROAD && s.owner == space.owner).count() - 1);
			case UTILITY -> (long) dice * space.rents.get((int) board.stream().filter(s -> s.type == Type.UTILITY && s.owner == space.owner).count() - 1);
			default -> 0;
		};
	}

	private List<MonopolySpace> group(MonopolySpace space) {
		return board.stream().filter(s -> s.type == Type.PROPERTY && s.group.equals(space.group)).toList();
	}

	private boolean monopoly(MonopolySpace space) {
		return space.owner != null && group(space).stream().allMatch(s -> s.owner == space.owner);
	}

	private void bill(MonopolyPlayer payer, long amount, MonopolyPlayer creditor) {
		long paid = Math.min(payer.money, amount);
		payer.money -= paid;
		if (creditor != null) creditor.money += paid;
		broadcast(Component.text(payer + " pays $" + paid + " to " + (creditor == null ? "the bank" : creditor) + "."));
		if (paid < amount) {
			payer.debts.add(new Debt(amount - paid, creditor));
			broadcast(Component.text(payer + " still owes $" + (amount - paid)
					+ ". Sell buildings, mortgage or trade, then /mono paybills; or /mono bankrupt.", NamedTextColor.RED));
		}
	}

	private void jail(MonopolyPlayer gp) {
		gp.position = 10;
		gp.jailed = true;
		gp.jailTurns = 0;
		doubles = 0;
		rollAgain = false;
		action = Action.NONE;
		broadcast(Component.text(gp + " is sent to jail. End your turn with /mono end.", NamedTextColor.RED));
	}

	private void draw(MonopolyPlayer gp, EventDeck deck, String source, int depth) {
		EventCard card = deck.draw();
		broadcast(Component.text(source + ": " + card.name(), NamedTextColor.YELLOW));
		switch (card.type()) {
			case GAINMONEY -> gp.money += card.amount();
			case LOSEMONEY -> bill(gp, card.amount(), null);
			case JAILFREE -> gp.jailFree++;
			case JAIL -> jail(gp);
			case MOVE -> moveTo(gp, card.move(), false, depth);
			case MOVERELATIVE -> move(gp, card.move(), true, false, depth);
			case NEARESTRAILROAD, NEARESTUTILITY -> {
				Type target = card.type() == Monopoly.CardType.NEARESTRAILROAD ? Type.RAILROAD : Type.UTILITY;
				for (int i = 1; i <= 40; i++) {
					if (board.get((gp.position + i) % 40).type == target) {
						move(gp, i, true, true, depth);
						break;
					}
				}
			}
			case BUILDINGTAX -> {
				long cost = board.stream().filter(s -> s.owner == gp).mapToLong(s ->
						s.buildings == 5 ? card.hotel() : (long) s.buildings * card.house()).sum();
				bill(gp, cost, null);
			}
			case PAYMONEY -> {
				for (MonopolyPlayer other : order) if (other != gp) bill(gp, card.amount(), other);
			}
			case TAKEMONEY -> {
				for (MonopolyPlayer other : order) if (other != gp) bill(other, card.amount(), gp);
			}
		}
	}

	private MonopolySpace findSpace(Player player, String prefix) {
		if (prefix.isBlank()) { Util.displayError(player, "Specify a property name or board index."); return null; }
		List<MonopolySpace> matches = board.stream().filter(s -> Integer.toString(s.position).equals(prefix)
				|| s.name.equalsIgnoreCase(prefix)).toList();
		if (matches.size() == 1) return matches.get(0);
		matches = board.stream().filter(s -> s.name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
		if (matches.size() == 1) return matches.get(0);
		Util.displayError(player, matches.isEmpty() ? "No matching property." : "That name is ambiguous; use the full name or board index.");
		return null;
	}

	private void manageProperty(Player player, MonopolyPlayer gp, MonopolySpace property, String command) {
		if (command.equals("mortgage") || command.equals("unmortgage")) {
			boolean mortgage = command.equals("mortgage");
			if (property.mortgaged == mortgage || group(property).stream().anyMatch(s -> s.buildings > 0)) {
				Util.displayError(player, "The mortgage status is already set, or the color group still has buildings.");
				return;
			}
			long cost = property.price;
			if (!mortgage && !afford(player, gp, cost)) return;
			revision++;
			property.mortgaged = mortgage;
			gp.money += mortgage ? property.price / 2 : -cost;
		}
		else {
			if (property.type != Type.PROPERTY || property.mortgaged) {
				Util.displayError(player, "Buildings require an unmortgaged colored property.");
				return;
			}
			List<MonopolySpace> group = group(property);
			if (command.equals("build")) {
				if (!monopoly(property) || group.stream().anyMatch(s -> s.mortgaged || s.buildings < property.buildings)) {
					Util.displayError(player, "Own the whole unmortgaged color group and build evenly.");
					return;
				}
				if (property.buildings == 5 || (property.buildings == 4 ? hotels == 0 : houses == 0)) {
					Util.displayError(player, "The property is fully built or the bank has no required buildings.");
					return;
				}
				if (!afford(player, gp, property.buildingPrice)) return;
				revision++;
				if (property.buildings == 4) { houses += 4; hotels--; }
				else houses--;
				property.buildings++;
				gp.money -= property.buildingPrice;
			}
			else {
				int target = command.equals("destroyhotel") ? 0 : property.buildings - 1;
				if (property.buildings == 0 || command.equals("destroyhotel") && property.buildings != 5
						|| !command.equals("destroyhotel") && group.stream().anyMatch(s -> s.buildings > target + 1)) {
					Util.displayError(player, "Sell buildings evenly; destroyhotel requires a hotel.");
					return;
				}
				if (property.buildings == 5 && target == 4 && houses < 4) {
					Util.displayError(player, "Four houses are needed to downgrade a hotel. Sell buildings elsewhere or use /mono destroyhotel when even-selling allows it.");
					return;
				}
				revision++;
				int sold = property.buildings - target;
				if (property.buildings == 5) {
					hotels++;
					if (target == 4) houses -= 4;
				}
				else houses++;
				property.buildings = target;
				gp.money += (long) property.buildingPrice * sold / 2;
			}
		}
		broadcast(Component.text(gp + " uses " + command + " on " + property.name + ". Cash: $" + gp.money));
	}

	private void bid(Player player, MonopolyPlayer gp, int bid) {
		if (auction == null || !auction.bidders.contains(gp)) {
			Util.displayError(player, "You are not participating in an auction.");
			return;
		}
		if (bid <= auction.bid || !gp.debts.isEmpty()) {
			Util.displayError(player, "Bid higher than $" + auction.bid + " and pay your bills first.");
			return;
		}
		if (!afford(player, gp, bid)) return;
		revision++;
		auction.bid = bid;
		auction.topBidder = gp;
		broadcast(Component.text(gp + " bids $" + bid + " for " + auction.property.name + "."));
		finishAuction();
	}

	private void leaveAuction(Player player, MonopolyPlayer gp) {
		if (auction == null || !auction.bidders.contains(gp) || auction.topBidder == gp) {
			Util.displayError(player, "You cannot withdraw as the highest bidder or when you are not in an auction.");
			return;
		}
		revision++;
		auction.bidders.remove(gp);
		broadcast(Component.text(gp + " withdraws from the auction."));
		finishAuction();
	}

	private void finishAuction() {
		if (auction == null || auction.bidders.size() > 1) return;
		if (auction.topBidder != null) {
			auction.topBidder.money -= auction.bid;
			auction.property.owner = auction.topBidder;
			broadcast(Component.text(auction.topBidder + " wins " + auction.property.name + " for $" + auction.bid + "."));
			auction = null;
			action = rollAgain ? Action.ROLL : Action.NONE;
		}
		else if (auction.bidders.isEmpty()) {
			broadcast(Component.text("No bids. The property remains unowned."));
			auction = null;
			action = rollAgain ? Action.ROLL : Action.NONE;
		}
		else broadcast(Component.text("The remaining bidder must bid or withdraw."));
	}

	private void showAuction(Player player) {
		if (auction == null) { Util.displayError(player, "There is no active auction."); return; }
		message(player, "Auction: " + auction.property.name + "; highest bid $" + auction.bid
				+ (auction.topBidder == null ? " (no bids)" : " by " + auction.topBidder)
				+ "; participants: " + String.join(", ", auction.bidders.stream().map(MonopolyPlayer::getName).toList()));
	}

	private boolean tradable(MonopolySpace property, MonopolyPlayer owner) {
		return property.owner == owner && !property.mortgaged && group(property).stream().noneMatch(s -> s.buildings > 0);
	}

	private void trade(Player player, MonopolyPlayer gp, String[] args) {
		if (args.length == 2 && !List.of("confirm", "unconfirm", "accept", "cancel", "view").contains(args[1].toLowerCase(Locale.ROOT))) {
			MonopolyPlayer other = players.get(args[1].toLowerCase(Locale.ROOT));
			if (trade != null || other == null || other == gp || other.bankrupt || decidingOrder) {
				Util.displayError(player, "Choose another active player, with no trade already pending, after turn order is decided.");
				return;
			}
			if (!ownTurn(player, gp) && gp.debts.isEmpty()) return;
			if (!other.getPlayer().isOnline()) { Util.displayError(player, "The other player must be online."); return; }
			revision++;
			trade = new Trade(gp, other);
			broadcast(Component.text(gp + " starts a trade with " + other + ". Use /mono trade offer|request property|money|jailfree <value>."));
			return;
		}
		if (trade == null || !trade.contains(gp)) {
			Util.displayError(player, "You are not in a trade. Start one with /mono trade <player>.");
			return;
		}
		if (args.length == 2 && args[1].equalsIgnoreCase("unconfirm")) {
			revision++;
			if (gp == trade.a) trade.confirmA = false;
			else trade.confirmB = false;
			broadcast(Component.text(gp + " withdraws their trade confirmation."));
			return;
		}
		if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) {
			revision++;
			trade = null;
			broadcast(Component.text(gp + " cancels the trade."));
			return;
		}
		if (args.length == 2 && (args[1].equalsIgnoreCase("confirm") || args[1].equalsIgnoreCase("accept"))) {
			if (!validOffer(trade.a, trade.offerA) || !validOffer(trade.b, trade.offerB)) {
				Util.displayError(player, "The trade assets or money have changed. Update the offers.");
				return;
			}
			revision++;
			if (gp == trade.a) trade.confirmA = true;
			else trade.confirmB = true;
			if (trade.confirmA && trade.confirmB) {
				transfer(trade.a, trade.b, trade.offerA);
				transfer(trade.b, trade.a, trade.offerB);
				broadcast(Component.text("Trade completed between " + trade.a + " and " + trade.b + ".", NamedTextColor.GREEN));
				trade = null;
			}
			else {
				showTrade(player);
				MonopolyPlayer other = trade.other(gp);
				message(other.getPlayer(), gp + " confirms the trade. Review with /mono trade view or /mono menu.");
				menu(other.getPlayer(), other);
			}
			return;
		}
		if (args.length < 4 || !(args[1].equalsIgnoreCase("offer") || args[1].equalsIgnoreCase("request"))) {
			help(player);
			return;
		}
		MonopolyPlayer offering = args[1].equalsIgnoreCase("offer") ? gp : trade.other(gp);
		Offer offer = trade.offer(offering);
		String type = args[2].toLowerCase(Locale.ROOT), value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
		if (type.equals("property")) {
			MonopolySpace property = findSpace(player, value);
			if (property == null) return;
			if (!tradable(property, offering)) {
				Util.displayError(player, "Only owned, unmortgaged properties with no buildings in their color group can be traded.");
				return;
			}
			revision++;
			if (!offer.properties.remove(property)) offer.properties.add(property);
		}
		else if ((type.equals("money") || type.equals("jailfree")) && args.length == 4) {
			Integer amount = amount(player, value, Integer.MAX_VALUE);
			if (amount == null) return;
			if (type.equals("money") && amount > offering.money || type.equals("jailfree") && amount > offering.jailFree) {
				Util.displayError(player, "That player does not have enough of the offered asset.");
				return;
			}
			revision++;
			if (type.equals("money")) offer.money = amount;
			else offer.jailFree = amount;
		}
		else { help(player); return; }
		trade.confirmA = false;
		trade.confirmB = false;
		broadcast(Component.text(gp + " updates the trade. Both players must confirm the new offer."));
		showTrade(player);
	}

	private boolean validOffer(MonopolyPlayer player, Offer offer) {
		return !player.bankrupt && player.money >= offer.money && player.jailFree >= offer.jailFree
				&& offer.properties.stream().allMatch(p -> tradable(p, player));
	}

	private void transfer(MonopolyPlayer from, MonopolyPlayer to, Offer offer) {
		from.money -= offer.money;
		to.money += offer.money;
		from.jailFree -= offer.jailFree;
		to.jailFree += offer.jailFree;
		for (MonopolySpace property : offer.properties) property.owner = to;
	}

	private String offerText(Offer offer) {
		return "$" + offer.money + ", " + offer.jailFree + " jail-free cards, properties: "
				+ String.join(", ", offer.properties.stream().map(p -> p.name).toList());
	}

	private void showTrade(Player player) {
		if (trade == null) { Util.displayError(player, "There is no active trade."); return; }
		message(player, trade.a + " offers " + offerText(trade.offerA) + " (confirmed: " + trade.confirmA + ")");
		message(player, trade.b + " offers " + offerText(trade.offerB) + " (confirmed: " + trade.confirmB + ")");
	}

	private void menu(Player player, MonopolyPlayer gp) {
		if (trade != null && trade.contains(gp)) {
			choices(player, "Review Monopoly trade", trade.a + ": " + offerText(trade.offerA) + "\n"
					+ trade.b + ": " + offerText(trade.offerB),
					new String[][] {{"Confirm trade", "trade confirm"}, {"Cancel trade", "trade cancel"}});
		}
		else if (auction != null) {
			showAuction(player);
			choices(player, "Monopoly auction", "Use /mono bid <amount> to bid. Highest bid: $" + auction.bid,
					new String[][] {{"Withdraw", "auction leave"}, {"View", "auction view"}});
		}
		else if (gp != current()) message(player, "It is " + current() + "'s turn.");
		else if (action == Action.BUY) {
			MonopolySpace property = board.get(gp.position);
			choices(player, "Unowned property", property.details() + "\nYour cash: $" + gp.money,
					new String[][] {{"Buy for $" + property.price, "buy"}, {"Auction", "auction"}});
		}
		else if (action == Action.JAIL) {
			choices(player, "Leave jail", "Roll doubles, pay $50 or use a jail-free card. A third failed roll costs $100.",
					new String[][] {{"Roll", "roll"}, {"Pay $50", "payjail"}, {"Use jail-free card", "jailfree"}});
		}
		else {
			choices(player, "Monopoly turn", "Required action: " + action + ". Outstanding bills: $" + gp.bills(),
					new String[][] {{"Roll", "roll"}, {"Pay bills", "paybills"}, {"End turn", "end"}});
		}
	}

	private void summary(Player player, MonopolyPlayer target) {
		message(player, target + ": $" + target.money + ", bills $" + target.bills() + ", jail-free cards " + target.jailFree
				+ (target.bankrupt ? " (bankrupt)" : target.jailed ? " (jailed)" : ""));
		for (MonopolySpace property : board) {
			if (property.owner != target) continue;
			player.sendMessage(property.display().append(Component.text(" [" + property.position + "] "
					+ (property.mortgaged ? "mortgaged" : "rent $" + rent(property, 1)) + "; buildings " + property.buildings))
					.clickEvent(ClickEvent.runCommand("/mono view " + property.position))
					.hoverEvent(HoverEvent.showText(Component.text(property.details()))));
		}
	}

	private void positions(Player player) {
		for (MonopolyPlayer gp : order) message(player, gp.token + ": " + gp + " at " + board.get(gp.position).name
				+ (gp.getPlayer().isOnline() ? "" : " (offline)"));
	}

	private void showColor(Player player, String[] args) {
		if (args.length < 2 || args.length > 3) { help(player); return; }
		String color = args[1].toLowerCase(Locale.ROOT);
		if (!Monopoly.GROUPS.containsValue(color)) { Util.displayError(player, "Unknown color group."); return; }
		MonopolyPlayer target = players.get((args.length == 3 ? args[2] : player.getName()).toLowerCase(Locale.ROOT));
		if (target == null) { Util.displayError(player, "Specify a player in this game."); return; }
		List<MonopolySpace> missing = board.stream().filter(s -> Monopoly.GROUPS.getOrDefault(s.group, "").equals(color)
				&& s.owner != target).toList();
		message(player, missing.isEmpty() ? target + " owns all " + color + " properties." : target + " is missing: "
				+ String.join(", ", missing.stream().map(s -> s.name).toList()));
	}

	private void map(Player player) {
		for (int row = 0; row <= 10; row++) {
			Component line = Component.empty();
			for (int column = 0; column <= 10; column++) {
				int position;
				if (column == 0) position = 10 - row;
				else if (column == 10) position = 20 + row;
				else if (row == 0) position = 10 + column;
				else if (row == 10) position = 40 - column;
				else { line = line.append(Component.text(" .", NamedTextColor.DARK_GRAY)); continue; }
				MonopolySpace space = board.get(position);
				List<MonopolyPlayer> occupants = order.stream().filter(gp -> gp.position == space.position).toList();
				String token = occupants.size() > 1 ? "@" : occupants.size() == 1 ? Character.toString(occupants.get(0).token)
						: space.owner != null ? Character.toString(space.owner.token).toUpperCase(Locale.ROOT)
						: space.isProperty() ? "x" : switch (space.type) {
							case JAIL -> "J"; case GOTOJAIL -> "/"; case TAX -> "t";
							case CHANCE, COMMUNITYCHEST -> "o"; case GO -> "G"; default -> "-";
						};
				Component hover = Component.text(space.details() + "\nPlayers: " + String.join(", ", occupants.stream().map(MonopolyPlayer::getName).toList()));
				line = line.append(Component.text(" " + token, space.color).hoverEvent(HoverEvent.showText(hover))
						.clickEvent(ClickEvent.runCommand("/mono view " + position)));
			}
			player.sendMessage(line);
		}
		positions(player);
		message(player, "Lowercase: player; uppercase: owner; @: multiple players; x: unowned; o: cards; J: jail; /: go to jail; t: tax.");
	}

	@Override
	public void displayInfo(Player viewer, Player viewed) {
		if (!started || order.isEmpty()) { message(viewer, "Monopoly is starting or has ended."); return; }
		message(viewer, "Monopoly: " + current() + "'s turn; required action " + action
				+ (hasDebts() ? "; outstanding bills" : "") + (waitingFor() == null ? "" : "; waiting for " + waitingFor()));
		MonopolyPlayer target = players.get(viewed.getName().toLowerCase(Locale.ROOT));
		if (target != null) summary(viewer, target);
		positions(viewer);
		viewer.sendMessage(button("Roll", "/mono roll").append(button("Map", "/mono map"))
				.append(button("End turn", "/mono end")).append(button("Dialog", "/mono menu")));
	}

	private void bankrupt(MonopolyPlayer gp) {
		if (gp.bankrupt) return;
		MonopolyPlayer oldCurrent = order.isEmpty() ? null : current();
		int oldIndex = order.indexOf(gp);
		gp.bankrupt = true;
		gp.debts.clear();
		for (MonopolyPlayer other : order) {
			other.debts.replaceAll(debt -> debt.creditor() == gp ? new Debt(debt.amount(), null) : debt);
		}
		for (MonopolySpace property : board) {
			if (property.owner != gp) continue;
			if (property.buildings == 5) hotels++;
			else houses += property.buildings;
			property.buildings = 0;
			property.owner = null;
			property.mortgaged = false;
		}
		if (trade != null && trade.contains(gp)) {
			trade = null;
			broadcast(Component.text("Trade cancelled because a participant left or became bankrupt."));
		}
		if (auction != null) {
			if (gp == oldCurrent) {
				auction = null;
				broadcast(Component.text("Auction cancelled because the current player left."));
			}
			else {
				auction.bidders.remove(gp);
				if (auction.topBidder == gp) {
					auction.topBidder = null;
					auction.bid = 0;
					broadcast(Component.text("The highest bidder left. Remaining bidders must bid again."));
				}
				finishAuction();
			}
		}
		order.remove(gp);
		orderRolls.remove(gp);
		broadcast(Component.text(gp + " is bankrupt. Their properties return to the bank. They may spectate or /tt leave.", NamedTextColor.RED));
		if (order.size() < 2) {
			if (!order.isEmpty()) broadcast(Component.text(order.get(0) + " wins Monopoly!", NamedTextColor.GREEN));
			endGame();
			return;
		}
		turn = gp == oldCurrent ? oldIndex % order.size() : order.indexOf(oldCurrent);
		if (decidingOrder && orderRolls.size() == order.size()) {
			order.sort(Comparator.comparingInt((MonopolyPlayer p) -> orderRolls.get(p)).reversed());
			decidingOrder = false;
			turn = 0;
			startTurn();
		}
		else if (gp == oldCurrent) startTurn();
	}

	@Override
	public void handleLeave(GamePlayer player) {
		revision++;
		bankrupt((MonopolyPlayer) player);
	}

	@Override
	public GameLobby<MonopolyPlayer> onEnd() {
		revision++;
		started = false;
		return new MonopolyLobby(this);
	}
}
