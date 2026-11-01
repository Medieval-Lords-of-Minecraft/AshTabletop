package me.ascheladd.ashtabletop.monopoly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ascheladd.ashtabletop.AshTabletop;
import me.ascheladd.ashtabletop.Game;
import me.ascheladd.ashtabletop.GameLobby;
import me.ascheladd.ashtabletop.GamePlayer;
import net.kyori.adventure.text.format.NamedTextColor;

public class Monopoly extends Game {
	enum CardType {
		BUILDINGTAX, GAINMONEY, LOSEMONEY, JAIL, JAILFREE, MOVE, MOVERELATIVE,
		NEARESTRAILROAD, NEARESTUTILITY, PAYMONEY, TAKEMONEY
	}

	record EventCard(CardType type, String name, int amount, int move, int house, int hotel) {}

	private static Monopoly instance;
	private final List<MonopolySpace> board = new ArrayList<>();
	private final List<EventCard> chance, chest;
	static final Map<String, String> GROUPS = Map.of("d", "pink", "3", "cyan", "5", "purple", "6", "orange",
			"4", "red", "e", "yellow", "2", "green", "9", "blue", "7", "railroad", "8", "utility");

	public Monopoly() {
		super(new File(AshTabletop.folder(), "Monopoly"));
		if (!key.equals("monopoly") || minPlayers < 2 || maxPlayers > 8) {
			throw new IllegalArgumentException("Monopoly requires key monopoly and player limits between 2 and 8.");
		}
		YamlConfiguration cfg = new YamlConfiguration();
		try {
			cfg.load(new File(new File(AshTabletop.folder(), "Monopoly"), "config.yml"));
		}
		catch (IOException | InvalidConfigurationException e) {
			throw new IllegalArgumentException("Unable to load Monopoly/config.yml", e);
		}
		ConfigurationSection spaces = section(cfg, "spaces");
		if (spaces.getKeys(false).size() != 40) throw new IllegalArgumentException("Monopoly board must contain 40 spaces.");
		for (int i = 0; i < 40; i++) {
			ConfigurationSection space = section(spaces, Integer.toString(i));
			MonopolySpace.Type type = MonopolySpace.Type.valueOf(text(space, "type").toUpperCase(Locale.ROOT));
			boolean property = List.of(MonopolySpace.Type.PROPERTY, MonopolySpace.Type.RAILROAD, MonopolySpace.Type.UTILITY).contains(type);
			String name = property || type == MonopolySpace.Type.TAX ? text(space, "name") : switch (type) {
				case GO -> "Go";
				case FREE -> "Free Parking";
				case GOTOJAIL -> "Go To Jail";
				case JAIL -> "Jail";
				case COMMUNITYCHEST -> "Community Chest";
				case CHANCE -> "Chance";
				default -> throw new IllegalArgumentException("Missing space name.");
			};
			int price = property || type == MonopolySpace.Type.TAX ? number(space, "price", 0, 1000000) : 0;
			int buildingPrice = type == MonopolySpace.Type.PROPERTY ? number(space, "houseprice", 1, 1000000) : 0;
			String group = property ? text(space, "color").toLowerCase(Locale.ROOT) : "";
			if (property && !GROUPS.containsKey(group)) throw new IllegalArgumentException("Unsupported Monopoly color: " + group);
			if (type == MonopolySpace.Type.RAILROAD && !group.equals("7")
					|| type == MonopolySpace.Type.UTILITY && !group.equals("8")
					|| type == MonopolySpace.Type.PROPERTY && (group.equals("7") || group.equals("8"))) {
				throw new IllegalArgumentException("Invalid color group for " + name);
			}
			List<Integer> rents = new ArrayList<>();
			if (property) {
				for (String rent : text(space, "rent").split(",")) {
					int value = Integer.parseInt(rent.trim());
					if (value < 0 || value > 1000000) throw new IllegalArgumentException("Invalid rent for " + name);
					rents.add(value);
				}
				int expected = switch (type) { case PROPERTY -> 6; case RAILROAD -> 4; default -> 2; };
				if (rents.size() != expected) throw new IllegalArgumentException("Expected " + expected + " rents for " + name);
			}
			board.add(new MonopolySpace(i, type, name, price, buildingPrice, group, List.copyOf(rents), color(group)));
		}
		if (board.get(0).type != MonopolySpace.Type.GO || board.get(10).type != MonopolySpace.Type.JAIL) {
			throw new IllegalArgumentException("Monopoly requires Go at position 0 and Jail at position 10.");
		}
		if (board.stream().filter(s -> s.type == MonopolySpace.Type.RAILROAD).count() > 4
				|| board.stream().filter(s -> s.type == MonopolySpace.Type.UTILITY).count() > 2) {
			throw new IllegalArgumentException("Monopoly supports at most four railroads and two utilities.");
		}
		chance = loadCards(section(cfg, "chance"));
		chest = loadCards(section(cfg, "communitychest"));
		if (chance.stream().anyMatch(c -> c.type == CardType.NEARESTRAILROAD)
				&& board.stream().noneMatch(s -> s.type == MonopolySpace.Type.RAILROAD)
				|| chance.stream().anyMatch(c -> c.type == CardType.NEARESTUTILITY)
				&& board.stream().noneMatch(s -> s.type == MonopolySpace.Type.UTILITY)
				|| chest.stream().anyMatch(c -> c.type == CardType.NEARESTRAILROAD)
				&& board.stream().noneMatch(s -> s.type == MonopolySpace.Type.RAILROAD)
				|| chest.stream().anyMatch(c -> c.type == CardType.NEARESTUTILITY)
				&& board.stream().noneMatch(s -> s.type == MonopolySpace.Type.UTILITY)) {
			throw new IllegalArgumentException("Nearest-space cards require matching board spaces.");
		}
		instance = this;
	}

	private static List<EventCard> loadCards(ConfigurationSection section) {
		List<EventCard> cards = new ArrayList<>();
		for (String key : section.getKeys(false)) {
			ConfigurationSection cfg = section(section, key);
			CardType type = CardType.valueOf(text(cfg, "type").toUpperCase(Locale.ROOT));
			int amount = switch (type) {
				case GAINMONEY, LOSEMONEY, PAYMONEY, TAKEMONEY -> number(cfg, "amount", 0, 1000000);
				default -> 0;
			};
			int move = switch (type) {
				case MOVE -> number(cfg, "move", 0, 39);
				case MOVERELATIVE -> number(cfg, "move", -39, 39);
				default -> 0;
			};
			int house = type == CardType.BUILDINGTAX ? number(cfg, "house", 0, 1000000) : 0;
			int hotel = type == CardType.BUILDINGTAX ? number(cfg, "hotel", 0, 1000000) : 0;
			cards.add(new EventCard(type, text(cfg, "name"), amount, move, house, hotel));
		}
		if (cards.isEmpty()) throw new IllegalArgumentException("Monopoly card decks cannot be empty.");
		return List.copyOf(cards);
	}

	private static ConfigurationSection section(ConfigurationSection cfg, String key) {
		ConfigurationSection result = cfg.getConfigurationSection(key);
		if (result == null) throw new IllegalArgumentException("Missing section: " + cfg.getCurrentPath() + "." + key);
		return result;
	}

	private static String text(ConfigurationSection cfg, String key) {
		String value = cfg.getString(key);
		if (!cfg.isString(key) || value == null || value.isBlank()) throw new IllegalArgumentException("Missing string: " + key);
		return value;
	}

	private static int number(ConfigurationSection cfg, String key, int min, int max) {
		if (!cfg.isInt(key) || cfg.getInt(key) < min || cfg.getInt(key) > max) {
			throw new IllegalArgumentException("Invalid integer: " + cfg.getCurrentPath() + "." + key);
		}
		return cfg.getInt(key);
	}

	private static NamedTextColor color(String group) {
		return switch (group) {
			case "d" -> NamedTextColor.LIGHT_PURPLE;
			case "3" -> NamedTextColor.DARK_AQUA;
			case "5" -> NamedTextColor.DARK_PURPLE;
			case "6" -> NamedTextColor.GOLD;
			case "4" -> NamedTextColor.DARK_RED;
			case "e" -> NamedTextColor.YELLOW;
			case "2" -> NamedTextColor.DARK_GREEN;
			case "9" -> NamedTextColor.BLUE;
			case "8" -> NamedTextColor.DARK_GRAY;
			default -> NamedTextColor.GRAY;
		};
	}

	List<MonopolySpace> createBoard() {
		return board.stream().map(MonopolySpace::copy).toList();
	}

	List<EventCard> chance() { return chance; }
	List<EventCard> chest() { return chest; }
	public static Monopoly inst() { return instance; }

	@Override
	public GameLobby<? extends GamePlayer> createLobby(String name, UUID host, boolean isPublic) {
		return new MonopolyLobby(name, host, isPublic);
	}
}
