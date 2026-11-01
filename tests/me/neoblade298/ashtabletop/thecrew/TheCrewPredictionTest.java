package me.neoblade298.ashtabletop.thecrew;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryBuilderFactory;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.InlinedRegistryBuilderProvider;
import io.papermc.paper.registry.data.dialog.DialogInstancesProvider;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import me.neoblade298.ashtabletop.AshTabletop;
import me.neoblade298.ashtabletop.GameInstance;
import me.neoblade298.ashtabletop.GameManager;
import me.neoblade298.ashtabletop.GameSession;
import me.neoblade298.ashtabletop.thecrew.tasks.TheCrewTask;
import me.neoblade298.ashtabletop.thecrew.tasks.WinTricksPredictTask;
import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.util.Util;
import net.kyori.adventure.text.minimessage.MiniMessage;

class TheCrewPredictionTest {
	private final ArrayList<AutoCloseable> mocks = new ArrayList<>();
	private TheCrewInstance game;
	private Player player;
	private TheCrewPlayer owner;
	private WinTricksPredictTask task;
	private ArrayList<TheCrewTask> tasks;
	private DialogInstancesProvider dialogProvider;
	private Dialog dialog;
	private BukkitScheduler scheduler;
	private AshTabletop plugin;
	private final AtomicReference<DialogActionCallback> callback = new AtomicReference<>();
	private MockedStatic<Util> util;

	@BeforeEach
	void setup() throws Exception {
		MockedStatic<RegistryAccess> registries = mockStatic(RegistryAccess.class);
		mocks.add(registries);
		RegistryAccess access = mock(RegistryAccess.class, RETURNS_MOCKS);
		registries.when(RegistryAccess::registryAccess).thenReturn(access);
		@SuppressWarnings("unchecked")
		Registry<Dialog> registry = mock(Registry.class);
		when(access.getRegistry(RegistryKey.DIALOG)).thenReturn(registry);
		dialog = mock(Dialog.class);
		MockedStatic<DialogInstancesProvider> dialogs = mockStatic(DialogInstancesProvider.class);
		mocks.add(dialogs);
		dialogProvider = mock(DialogInstancesProvider.class, RETURNS_DEEP_STUBS);
		dialogs.when(DialogInstancesProvider::instance).thenReturn(dialogProvider);
		when(dialogProvider.register(any(), any())).thenAnswer(invocation -> {
			callback.set(invocation.getArgument(0));
			return mock(io.papermc.paper.registry.data.dialog.action.DialogAction.CustomClickAction.class);
		});
		MockedStatic<InlinedRegistryBuilderProvider> builders = mockStatic(InlinedRegistryBuilderProvider.class);
		mocks.add(builders);
		InlinedRegistryBuilderProvider builderProvider = mock(InlinedRegistryBuilderProvider.class);
		builders.when(InlinedRegistryBuilderProvider::instance).thenReturn(builderProvider);
		when(builderProvider.createDialog(any())).thenAnswer(invocation -> {
			Consumer<RegistryBuilderFactory<Dialog, ? extends DialogRegistryEntry.Builder>> build =
					invocation.getArgument(0);
			@SuppressWarnings("unchecked")
			RegistryBuilderFactory<Dialog, DialogRegistryEntry.Builder> factory =
					mock(RegistryBuilderFactory.class);
			when(factory.empty()).thenReturn(mock(DialogRegistryEntry.Builder.class, RETURNS_SELF));
			build.accept(factory);
			return dialog;
		});
		MockedStatic<NeoCore> core = mockStatic(NeoCore.class);
		mocks.add(core);
		core.when(NeoCore::miniMessage).thenReturn(MiniMessage.miniMessage());
		util = mockStatic(Util.class);
		mocks.add(util);
		MockedStatic<AshTabletop> tabletop = mockStatic(AshTabletop.class);
		mocks.add(tabletop);
		scheduler = mock(BukkitScheduler.class);
		plugin = mock(AshTabletop.class);
		tabletop.when(AshTabletop::scheduler).thenReturn(scheduler);
		tabletop.when(AshTabletop::inst).thenReturn(plugin);

		game = mock(TheCrewInstance.class, CALLS_REAL_METHODS);
		player = mock(Player.class);
		UUID uuid = UUID.randomUUID();
		when(player.getUniqueId()).thenReturn(uuid);
		when(player.getName()).thenReturn("Player");
		when(player.isOnline()).thenReturn(true);
		owner = new TheCrewPlayer(uuid, player, game);
		task = mock(WinTricksPredictTask.class);
		tasks = new ArrayList<>();
		tasks.add(task);
		tasks.add(mock(TheCrewTask.class));
		Object selectionPhase = java.util.Arrays.stream(
				TheCrewInstance.class.getDeclaredField("phase").getType().getEnumConstants())
				.filter(value -> value.toString().equals("SELECT_TASKS")).findFirst().orElseThrow();
		setField(TheCrewInstance.class, "phase", selectionPhase);
		setField(TheCrewInstance.class, "tasks", tasks);
		setField(TheCrewInstance.class, "turnOrder", new ArrayList<>(java.util.List.of(owner)));
		setField(TheCrewInstance.class, "totalRounds", 10);
		setField(GameInstance.class, "players", new HashMap<>(java.util.Map.of("player", owner)));
		setField(GameInstance.class, "spectators", new HashSet<Player>());
		setField(GameSession.class, "name", "test");
		MockedStatic<GameManager> sessions = mockStatic(GameManager.class, CALLS_REAL_METHODS);
		mocks.add(sessions);
		sessions.when(() -> GameManager.getSession(uuid)).thenReturn(game);
	}

	private void setField(Class<?> type, String name, Object value) throws Exception {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(game, value);
	}

	@AfterEach
	void cleanup() throws Exception {
		for (int i = mocks.size() - 1; i >= 0; i--) {
			mocks.get(i).close();
		}
		GameManager.clear();
	}

	@Test
	void selectingPredictionShowsBoundedDialogWithoutAssigningTask() {
		game.acceptTask(player, 0);
		verify(player).showDialog(dialog);
		verify(dialogProvider).numberRangeBuilder(eq("tricks"), any(), eq(0.0F), eq(10.0F));
		verify(dialogProvider.numberRangeBuilder("tricks",
				net.kyori.adventure.text.Component.text("Tricks won"), 0, 10).initial(0.0F)).step(1.0F);
		assertTrue(tasks.contains(task));
		assertTrue(owner.getTasks().isEmpty());
		verify(scheduler, never()).runTaskLater(any(), any(Runnable.class), anyLong());
	}

	@ParameterizedTest
	@ValueSource(floats = {0, 3, 10})
	void acceptedPredictionStoresActualAmountAndAdvancesOnce(float amount) {
		WinTricksPredictTask accepted = mock(WinTricksPredictTask.class);
		when(task.clone(owner, game, (int) amount)).thenReturn(accepted);
		when(accepted.getDisplay()).thenReturn(net.kyori.adventure.text.Component.text("Prediction"));
		game.acceptPrediction(player, task, amount);
		assertEquals(java.util.List.of(accepted), owner.getTasks());
		assertFalse(tasks.contains(task));
		verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(20L));
		game.acceptPrediction(player, task, amount);
		assertEquals(1, owner.getTasks().size());
		verify(task).clone(owner, game, (int) amount);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(floats = {-1, 11, 2.5F, Float.NaN, Float.POSITIVE_INFINITY})
	void invalidPredictionKeepsTaskAndReopensDialog(Float amount) {
		game.acceptPrediction(player, task, amount);
		assertTrue(tasks.contains(task));
		assertTrue(owner.getTasks().isEmpty());
		verify(player).showDialog(dialog);
		util.verify(() -> Util.displayError(eq(player), contains("whole number")));
	}

	@Test
	void callbackRunsOnServerThreadAndRejectsDifferentPlayer() {
		game.acceptTask(player, 0);
		DialogResponseView response = mock(DialogResponseView.class);
		when(response.getFloat("tricks")).thenReturn(3.0F);
		Player other = mock(Player.class);
		when(other.getUniqueId()).thenReturn(UUID.randomUUID());
		callback.get().accept(response, other);
		verify(scheduler, never()).runTask(any(), any(Runnable.class));
		AtomicReference<Runnable> queued = new AtomicReference<>();
		when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
			queued.set(invocation.getArgument(1));
			return null;
		});
		callback.get().accept(response, player);
		assertTrue(owner.getTasks().isEmpty());
		when(player.isOnline()).thenReturn(false);
		queued.get().run();
		assertTrue(owner.getTasks().isEmpty());
		assertTrue(tasks.contains(task));
	}
}
