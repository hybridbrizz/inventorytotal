package com.ericversteeg;

import com.google.gson.Gson;
import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@PluginDescriptor(
	name = "Inventory Total",
	description = "Totals item prices in your inventory."
)

public class InventoryTotalPlugin extends Plugin
{
	static final int COINS = ItemID.COINS_995;
	static final int TOTAL_GP_GE_INDEX = 0;
	static final int TOTAL_GP_HA_INDEX = 1;
	static final int TOTAL_QTY_INDEX = 2;
	static final int NO_PROFIT_LOSS_TIME = -1;
	static final int RUNEPOUCH_ITEM_ID = 12791;
	static final int DIVINE_RUNEPOUCH_ITEM_ID = 27281;

	@Inject
	private InventoryTotalOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private InventoryTotalConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private KeyManager keyManager;

	private String profileKey = "";

	private InventoryTotalRunData runData;

	private InventoryTotalMode mode = InventoryTotalMode.TOTAL;

	private InventoryTotalState state = InventoryTotalState.NONE;
	private InventoryTotalState prevState = InventoryTotalState.NONE;

	private long totalGp = 0;
	private long totalQty = 0;

	private long runStartTime = 0;

	private long lastWriteSaveTime = 0;

	private InventoryTotalMode plToggleOverride = null;
	private KeyListener plToggleKeyListener;
	private KeyListener newRunKeyListener;

	private boolean manualNewRun = false;

	// from ClueScrollPlugin
	private static final int[] RUNEPOUCH_AMOUNT_VARBITS = {
			Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3, Varbits.RUNE_POUCH_AMOUNT4
	};
	private static final int[] RUNEPOUCH_RUNE_VARBITS = {
			Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3, Varbits.RUNE_POUCH_RUNE4
	};

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		runData = new InventoryTotalRunData();

		registerKeys();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);

		unregisterKeys();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		profileKey = configManager.getRSProfileKey();
		if (profileKey != null)
		{
			runData = getSavedData();
		}
	}

	@Provides
	InventoryTotalConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(InventoryTotalConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged config)
	{
		if (config.getGroup().equals(InventoryTotalConfig.GROUP))
		{
			if (config.getKey().equals("enableProfitLoss"))
			{
				plToggleOverride = null;
			}
			else if (config.getKey().equals("profitLossToggleKey"))
			{
				unregisterKeys();
				registerKeys();
			} else if (config.getKey().equals("ignoredItems"))
			{
				// update the runData if it's already initialized
				if (runData != null) {
					runData.ignoredItems = getIgnoredItems();
				}
			}
		}
	}

	private void registerKeys()
	{
		// switch mode
		plToggleKeyListener = new HotkeyListener(() -> config.profitLossToggleKey())
		{
			@Override
			public void hotkeyPressed()
			{
				if (mode == InventoryTotalMode.TOTAL)
				{
					plToggleOverride = InventoryTotalMode.PROFIT_LOSS;
				}
				else
				{
					plToggleOverride = InventoryTotalMode.TOTAL;
				}
			}
		};
		keyManager.registerKeyListener(plToggleKeyListener);

		// new run
		newRunKeyListener = new HotkeyListener(() -> config.newRunKey())
		{
			@Override
			public void hotkeyPressed()
			{
				if (state != InventoryTotalState.BANK)
				{
					manualNewRun = true;
				}
			}
		};
		keyManager.registerKeyListener(newRunKeyListener);
	}

	private void unregisterKeys()
	{
		if (plToggleKeyListener != null)
		{
			keyManager.unregisterKeyListener(plToggleKeyListener);
		}
		if (newRunKeyListener != null)
		{
			keyManager.unregisterKeyListener(newRunKeyListener);
		}
	}

	void onNewRun()
	{
		overlay.showInterstitial();

		runStartTime = Instant.now().toEpochMilli();

		runData.ignoredItems = getIgnoredItems();
	}

	// to handle same tick bank closing
	void postNewRun()
	{
		runData.initialItemQtys.clear();

		int [] inventoryTotals = getInventoryTotals(true);
		int [] equipmentTotals = getEquipmentTotals(true);

		int inventoryTotal = inventoryTotals[InventoryTotalPlugin.TOTAL_GP_GE_INDEX];
		int inventoryTotalHA = inventoryTotals[InventoryTotalPlugin.TOTAL_GP_HA_INDEX];

		int equipmentTotal = equipmentTotals[0];
		int equipmentTotalHA = equipmentTotals[1];

		runData.profitLossInitialGp = inventoryTotal + equipmentTotal;
		runData.profitLossInitialGpHA = inventoryTotalHA + equipmentTotalHA;

		writeSavedData();

		overlay.hideInterstitial();
	}

	void onBank()
	{
		if (!config.newRunAfterBanking())
		{
			return;
		}

		runData.profitLossInitialGp = 0;
		runData.profitLossInitialGpHA = 0;
		runData.itemPrices.clear();

		runStartTime = 0;

		writeSavedData();
	}

	int [] getInventoryTotals(boolean isNewRun)
	{
		final ItemContainer itemContainer = overlay.getInventoryItemContainer();

		if (itemContainer == null)
		{
			return new int [2];
		}

		final Item[] items = itemContainer.getItems();

		final LinkedList<Item> allItems = new LinkedList<>(Arrays.asList(items));
		// only add when the runepouch is in the inventory
		if (allItems.stream().anyMatch(s -> s.getId() == RUNEPOUCH_ITEM_ID || s.getId() == DIVINE_RUNEPOUCH_ITEM_ID))
		{
			allItems.addAll(getRunepouchContents());
		}

		int totalQty = 0;
		int totalGp = 0;
		int totalGpHA = 0;

		for (Item item: allItems)
		{
			int itemId = item.getId();

			final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

			String itemName = itemComposition.getName();
			final boolean ignore = runData.ignoredItems.stream().anyMatch(s -> {
				String lcItemName = itemName.toLowerCase();
				String lcS = s.toLowerCase();
				return lcItemName.contains(lcS);
			});
			if (ignore) { continue; }

			final boolean isNoted = itemComposition.getNote() != -1;
			final int realItemId = isNoted ? itemComposition.getLinkedNoteId() : itemId;

			int totalPrice;
			int totalPriceHA;
			int gePrice;
			int haPrice;

			if (runData.itemPrices.containsKey(realItemId))
			{
				gePrice = runData.itemPrices.get(realItemId);
			}
			else
			{
				gePrice = itemManager.getItemPrice(realItemId);
			}

			if (runData.itemPricesHA.containsKey(realItemId))
			{
				haPrice = runData.itemPricesHA.get(realItemId);
			}
			else
			{
				haPrice = itemComposition.getHaPrice();
			}

			int itemQty = item.getQuantity();

			if (realItemId == COINS)
			{
				totalPrice = itemQty;
			}
			else
			{
				totalPrice = itemQty * gePrice;
			}

			if (realItemId == COINS)
			{
				totalPriceHA = itemQty;
			}
			else
			{
				totalPriceHA = itemQty * haPrice;
			}

			totalGp += totalPrice;
			totalGpHA += totalPriceHA;
			totalQty += itemQty;

			if (realItemId != COINS && !runData.itemPrices.containsKey(realItemId))
			{
				runData.itemPrices.put(realItemId, gePrice);
			}

			if (realItemId != COINS && !runData.itemPricesHA.containsKey(realItemId))
			{
				runData.itemPricesHA.put(realItemId, haPrice);
			}

			if (isNewRun)
			{
				if (runData.initialItemQtys.containsKey(realItemId))
				{
					runData.initialItemQtys.put(realItemId, runData.initialItemQtys.get(realItemId) + itemQty);
				}
				else
				{
					runData.initialItemQtys.put(realItemId, itemQty);
				}
			}

			if (runData.itemQtys.containsKey(realItemId))
			{
				runData.itemQtys.put(realItemId, runData.itemQtys.get(realItemId) + itemQty);
			}
			else
			{
				runData.itemQtys.put(realItemId, itemQty);
			}
		}

		int[] totals = new int[3];

		totals[TOTAL_GP_GE_INDEX] = totalGp;
		totals[TOTAL_GP_HA_INDEX] = totalGpHA;
		totals[TOTAL_QTY_INDEX] = totalQty;

		return totals;
	}

	int [] getEquipmentTotals(boolean isNewRun)
	{
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (itemContainer == null)
		{
			return new int [] {0, 0};
		}

		Item ammo = itemContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());

		List<Integer> eIds = getEquipmentIds();

		int eTotal = 0;
		int eTotalHA = 0;
		for (int itemId: eIds)
		{
			int qty = 1;
			if (ammo != null && itemId == ammo.getId())
			{
				qty = ammo.getQuantity();
			}

			int gePrice;
			int haPrice;

			if (runData.itemPrices.containsKey(itemId))
			{
				gePrice = runData.itemPrices.get(itemId);
			}
			else
			{
				gePrice = itemManager.getItemPrice(itemId);
			}

			if (runData.itemPricesHA.containsKey(itemId))
			{
				haPrice = runData.itemPricesHA.get(itemId);
			}
			else
			{
				ItemComposition itemComposition = itemManager.getItemComposition(itemId);
				haPrice = itemComposition.getHaPrice();
			}

			int totalPrice = qty * gePrice;
			int totalPriceHA = qty * haPrice;

			eTotal += totalPrice;
			eTotalHA += totalPriceHA;

			if (!runData.itemPrices.containsKey(itemId))
			{
				runData.itemPrices.put(itemId, gePrice);
			}

			if (!runData.itemPricesHA.containsKey(itemId))
			{
				runData.itemPricesHA.put(itemId, haPrice);
			}

			if (isNewRun)
			{
				if (runData.initialItemQtys.containsKey(itemId))
				{
					runData.initialItemQtys.put(itemId, runData.initialItemQtys.get(itemId) + qty);
				}
				else
				{
					runData.initialItemQtys.put(itemId, qty);
				}
			}

			if (runData.itemQtys.containsKey(itemId))
			{
				runData.itemQtys.put(itemId, runData.itemQtys.get(itemId) + qty);
			}
			else
			{
				runData.itemQtys.put(itemId, qty);
			}
		}

		return new int [] {eTotal, eTotalHA};
	}

	private List<Integer> getEquipmentIds()
	{
		List<Item> equipment = getEquipment();

		return equipment
				.stream()
				.map(Item::getId)
				.collect(Collectors.toList());
	}

	private List<Item> getEquipment()
	{
		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

		if (equipment == null)
		{
			return new ArrayList<>();
		}

		Item head = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		Item cape = equipment.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
		Item amulet = equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		Item ammo = equipment.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		Item body = equipment.getItem(EquipmentInventorySlot.BODY.getSlotIdx());
		Item shield = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		Item legs = equipment.getItem(EquipmentInventorySlot.LEGS.getSlotIdx());
		Item gloves = equipment.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
		Item boots = equipment.getItem(EquipmentInventorySlot.BOOTS.getSlotIdx());
		Item ring = equipment.getItem(EquipmentInventorySlot.RING.getSlotIdx());

		List<Item> items = new ArrayList<Item>();

		if (head != null)
		{
			items.add(head);
		}

		if (cape != null)
		{
			items.add(cape);
		}

		if (amulet != null)
		{
			items.add(amulet);
		}

		if (ammo != null)
		{
			items.add(ammo);
		}

		if (weapon != null)
		{
			items.add(weapon);
		}

		if (body != null)
		{
			items.add(body);
		}

		if (shield != null)
		{
			items.add(shield);
		}

		if (legs != null)
		{
			items.add(legs);
		}

		if (gloves != null)
		{
			items.add(gloves);
		}

		if (boots != null)
		{
			items.add(boots);
		}

		if (ring != null)
		{
			items.add(ring);
		}

		return items;
	}

	List<InventoryTotalLedgerItem> getInventoryLedger()
	{
		List<InventoryTotalLedgerItem> ledgerItems = new LinkedList<>();

		final ItemContainer itemContainer = overlay.getInventoryItemContainer();

		if (itemContainer == null)
		{
			return new LinkedList<>();
		}

		final Item[] items = itemContainer.getItems();

		final LinkedList<Item> allItems = new LinkedList<>(Arrays.asList(items));
		// only add when the runepouch is in the inventory
		if (allItems.stream().anyMatch(s -> s.getId() == RUNEPOUCH_ITEM_ID || s.getId() == DIVINE_RUNEPOUCH_ITEM_ID))
		{
			allItems.addAll(getRunepouchContents());
		}

		Map<Integer, Integer> qtyMap = new HashMap<>();

		for (Item item: allItems) {
			int itemId = item.getId();

			final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

			String itemName = itemComposition.getName();
			final boolean ignore = runData.ignoredItems.stream().anyMatch(s -> {
				String lcItemName = itemName.toLowerCase();
				String lcS = s.toLowerCase();
				return lcItemName.contains(lcS);
			});
			if (ignore) { continue; }

			final boolean isNoted = itemComposition.getNote() != -1;
			final int realItemId = isNoted ? itemComposition.getLinkedNoteId() : itemId;

			int itemQty = item.getQuantity();

			if (qtyMap.containsKey(realItemId))
			{
				qtyMap.put(realItemId, qtyMap.get(realItemId) + itemQty);
			}
			else
			{
				qtyMap.put(realItemId, itemQty);
			}
		}

		for (Integer itemId: qtyMap.keySet())
		{
			final ItemComposition itemComposition = itemManager.getItemComposition(itemId);

			String itemName = itemComposition.getName();

			Integer qty = qtyMap.get(itemId);

			Integer total;
			if (config.priceType() == InventoryTotalPriceType.GRAND_EXCHANGE)
			{
				total = runData.itemPrices.get(itemId);
			}
			else
			{
				total = runData.itemPricesHA.get(itemId);
			}

			if (itemId == COINS || total == null)
			{
				total = 1;
			}

			ledgerItems.add(new InventoryTotalLedgerItem(itemName, qty, total));
		}

		return ledgerItems;
	}

	List<InventoryTotalLedgerItem> getProfitLossLedger()
	{
		Map<Integer, Integer> prices;
		if (config.priceType() == InventoryTotalPriceType.GRAND_EXCHANGE)
		{
			prices = runData.itemPrices;
		}
		else
		{
			prices = runData.itemPricesHA;
		}

		Map<Integer, Integer> initialQtys = runData.initialItemQtys;
		Map<Integer, Integer> qtys = runData.itemQtys;

		Map<Integer, Integer> qtyDifferences = new HashMap<>();

		HashSet<Integer> combinedQtyKeys = new HashSet<>();
		combinedQtyKeys.addAll(qtys.keySet());
		combinedQtyKeys.addAll(initialQtys.keySet());

		for (Integer itemId: combinedQtyKeys)
		{
			Integer initialQty = initialQtys.get(itemId);
			Integer qty = qtys.get(itemId);

			if (initialQty == null)
			{
				initialQty = 0;
			}

			if (qty == null)
			{
				qty = 0;
			}

			qtyDifferences.put(itemId, qty - initialQty);
		}

		List<InventoryTotalLedgerItem> ledgerItems = new LinkedList<>();

		for (Integer itemId: qtyDifferences.keySet())
		{
			final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
			Integer price = prices.get(itemId);

			if (price == null)
			{
				price = 1;
			}

			Integer qtyDifference = qtyDifferences.get(itemId);

			List<InventoryTotalLedgerItem> filteredList = ledgerItems.stream().filter(
					item -> item.getDescription().equals(itemComposition.getName())).collect(Collectors.toList()
			);

			if (!filteredList.isEmpty())
			{
				filteredList.get(0).addQuantityDifference(qtyDifference);
			}
			else
			{
				if (price > 0)
				{
					ledgerItems.add(new InventoryTotalLedgerItem(itemComposition.getName(), qtyDifference, price));
				}
			}
		}

		return ledgerItems;
	}

	// from ClueScrollPlugin
	private List<Item> getRunepouchContents()
	{
		EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		List<Item> items = new ArrayList<>(RUNEPOUCH_AMOUNT_VARBITS.length);
		for (int i = 0; i < RUNEPOUCH_AMOUNT_VARBITS.length; i++)
		{
			int amount = client.getVarbitValue(RUNEPOUCH_AMOUNT_VARBITS[i]);
			if (amount <= 0)
			{
				continue;
			}

			int runeId = client.getVarbitValue(RUNEPOUCH_RUNE_VARBITS[i]);
			if (runeId == 0)
			{
				continue;
			}

			final int itemId = runepouchEnum.getIntValue(runeId);
			Item item = new Item(itemId, amount);
			items.add(item);
		}
		return items;
	}

	// max invoke rate approximately once per tick
	// mainly so that initially this isn't getting invoked multiple times after item prices are added to the map
	void writeSavedData()
	{
		if (state == InventoryTotalState.BANK || Instant.now().toEpochMilli() - lastWriteSaveTime < 600)
		{
			return;
		}

		String profile = configManager.getRSProfileKey();

		String json = gson.toJson(runData);
		configManager.setConfiguration(InventoryTotalConfig.GROUP, profile, "inventory_total_data", json);

		lastWriteSaveTime = Instant.now().toEpochMilli();
	}

	private InventoryTotalRunData getSavedData()
	{
		String profile = configManager.getRSProfileKey();
		String json = configManager.getConfiguration(InventoryTotalConfig.GROUP, profile, "inventory_total_data");

		InventoryTotalRunData savedData = gson.fromJson(json, InventoryTotalRunData.class);

		if (savedData == null)
		{
			return new InventoryTotalRunData();
		}
		return savedData;
	}

	private LinkedList<String> getIgnoredItems() {
		return new LinkedList<>(
			Arrays.asList(
				config.ignoredItems().split("\\s*,\\s*")
			)
		);
	}

	long elapsedRunTime()
	{
		if (runStartTime == 0 || !config.showRunTime())
		{
			return NO_PROFIT_LOSS_TIME;
		}

		return Instant
				.now()
				.minusMillis(runStartTime)
				.toEpochMilli();
	}

	void setMode(InventoryTotalMode mode)
	{
		this.mode = mode;
	}

	public InventoryTotalMode getMode()
	{
		return mode;
	}

	void setState(InventoryTotalState state)
	{
		this.prevState = this.state;
		this.state = state;
	}

	public InventoryTotalState getState()
	{
		return state;
	}

	public InventoryTotalState getPreviousState()
	{
		return prevState;
	}

	public long getProfitGp()
	{
		if (mode == InventoryTotalMode.TOTAL)
		{
			return totalGp;
		}
		else if (config.priceType() == InventoryTotalPriceType.GRAND_EXCHANGE)
		{
			return totalGp - runData.profitLossInitialGp;
		}
		else
		{
			return totalGp - runData.profitLossInitialGpHA;
		}
	}

	void setTotalGp(long totalGp)
	{
		this.totalGp = totalGp;
	}

	void setTotalQty(long totalQty)
	{
		this.totalQty = totalQty;
	}

	public long getTotalQty()
	{
		return totalQty;
	}

	public InventoryTotalRunData getRunData()
	{
		return runData;
	}

	public InventoryTotalMode getPLToggleOverride()
	{
		return plToggleOverride;
	}

	public boolean isManualNewRun()
	{
		if (manualNewRun)
		{
			manualNewRun = false;
			return true;
		}
		return false;
	}
}
