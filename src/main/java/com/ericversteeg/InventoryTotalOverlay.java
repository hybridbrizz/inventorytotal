package com.ericversteeg;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class InventoryTotalOverlay extends Overlay
{
	private static final int TEXT_Y_OFFSET = 17;
	private static final String PROFIT_LOSS_TIME_FORMAT = "%02d:%02d:%02d";
	private static final String PROFIT_LOSS_TIME_NO_HOURS_FORMAT = "%02d:%02d";
	private static final int HORIZONTAL_PADDING = 10;
	private static final int BANK_CLOSE_DELAY = 1200;
	private static final Color LEDGER_BACKGROUND_COLOR = new Color(27, 27, 27, 202);
	static final int COINS = ItemID.COINS_995;

	private final Client client;
	private final InventoryTotalPlugin plugin;
	private final InventoryTotalConfig config;

	private final ItemManager itemManager;

	private Widget inventoryWidget;
	private ItemContainer inventoryItemContainer;
	private ItemContainer equipmentItemContainer;

	private boolean onceBank = false;

	private boolean showInterstitial = false;

	private boolean postNewRun = false;
	private long newRunTime = 0;

	private int invX = -1;
	private int invY = -1;
	private int invW = -1;
	private int invH = -1;

	private boolean lastRenderShowingTooltip = false;

	@Inject
	private InventoryTotalOverlay(Client client, InventoryTotalPlugin plugin, InventoryTotalConfig config, ItemManager itemManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setAboveWidgets(config.isAlwaysAboveWidgets());

		this.client = client;
		this.plugin = plugin;
		this.config = config;

		this.itemManager = itemManager;
	}

	void updatePluginState()
	{
		inventoryWidget = client.getWidget(ComponentID.INVENTORY_CONTAINER);

		inventoryItemContainer = client.getItemContainer(InventoryID.INVENTORY);
		equipmentItemContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (plugin.getPLToggleOverride() == null)
		{
			if (config.enableProfitLoss())
			{
				plugin.setMode(InventoryTotalMode.PROFIT_LOSS);
			}
			else
			{
				plugin.setMode(InventoryTotalMode.TOTAL);
			}
		}
		else if (plugin.getPLToggleOverride() == InventoryTotalMode.PROFIT_LOSS)
		{
			plugin.setMode(InventoryTotalMode.PROFIT_LOSS);
		}
		else if (plugin.getPLToggleOverride() == InventoryTotalMode.TOTAL)
		{
			plugin.setMode(InventoryTotalMode.TOTAL);
		}

		boolean isBank = false;

		if (inventoryWidget == null || inventoryWidget.getCanvasLocation().getX() < 0 || inventoryWidget.isHidden())
		{
			Widget [] altInventoryWidgets = new Widget[]
			{
				client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER),
				client.getWidget(ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER)
			};

			for (Widget altInventoryWidget: altInventoryWidgets)
			{
				inventoryWidget = altInventoryWidget;
				if (inventoryWidget != null && !inventoryWidget.isHidden())
				{
					isBank = true;
					if (!onceBank)
					{
						onceBank = true;
					}

					break;
				}
			}
		}

		if (isBank)
		{
			plugin.setState(InventoryTotalState.BANK);
		}
		else
		{
			plugin.setState(InventoryTotalState.RUN);
		}

		// before totals
		boolean newRun = (plugin.getPreviousState() == InventoryTotalState.BANK
				&& plugin.getState() == InventoryTotalState.RUN
				&& config.newRunAfterBanking()) || plugin.isManualNewRun();
		plugin.getRunData().itemQtys.clear();

		// totals
		int [] inventoryTotals = plugin.getInventoryTotals(false);
		int [] equipmentTotals = plugin.getEquipmentTotals(false);

		int inventoryTotal = inventoryTotals[InventoryTotalPlugin.TOTAL_GP_GE_INDEX];
		int equipmentTotal = equipmentTotals[0];

		int inventoryTotalHA = inventoryTotals[InventoryTotalPlugin.TOTAL_GP_HA_INDEX];
		int equipmentTotalHA = equipmentTotals[1];

		int inventoryQty = inventoryTotals[InventoryTotalPlugin.TOTAL_QTY_INDEX];

		int totalGp = 0;
		if (config.priceType() == InventoryTotalPriceType.GRAND_EXCHANGE)
		{
			totalGp += inventoryTotal;
		}
		else
		{
			totalGp += inventoryTotalHA;
		}

		if ((plugin.getState() == InventoryTotalState.RUN || !config.newRunAfterBanking())
				&& plugin.getMode() == InventoryTotalMode.PROFIT_LOSS)
		{
			if (config.priceType() == InventoryTotalPriceType.GRAND_EXCHANGE)
			{
				totalGp += equipmentTotal;
			}
			else
			{
				totalGp += equipmentTotalHA;
			}
		}

		plugin.setTotalGp(totalGp);
		plugin.setTotalQty(inventoryQty);

		// after totals
		if (newRun)
		{
			plugin.onNewRun();

			postNewRun = true;
			newRunTime = Instant.now().toEpochMilli();
		}
		else if (plugin.getPreviousState() == InventoryTotalState.RUN && plugin.getState() == InventoryTotalState.BANK)
		{
			plugin.onBank();
		}

		// check post new run
		if (postNewRun && (Instant.now().toEpochMilli() - newRunTime) > BANK_CLOSE_DELAY)
		{
			plugin.postNewRun();
			postNewRun = false;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		updatePluginState();

		if (inventoryWidget != null)
		{
			invX = inventoryWidget.getCanvasLocation().getX();
			invY = inventoryWidget.getCanvasLocation().getY();
			invW = inventoryWidget.getWidth();
			invH = inventoryWidget.getHeight();
		}

		if (invX < 0 || invY < 0 || invW < 0 || invH < 0)
		{
			return null;
		}

		if (config.hideWithInventory() && (inventoryWidget == null || inventoryWidget.isHidden()))
		{
			return null;
		}

		int height = 20;

		String totalText = getTotalText(plugin.getProfitGp());

		String formattedRunTime = getFormattedRunTime();
		String runTimeText = null;

		if (formattedRunTime != null)
		{
			runTimeText = " (" + formattedRunTime + ")";
		}

		long total = plugin.getProfitGp();

		if (showInterstitial)
		{
			total = 0;

			if (plugin.getMode() == InventoryTotalMode.PROFIT_LOSS)
			{
				totalText = "0";
			}
			else
			{
				totalText = getTotalText(plugin.getProfitGp());
			}
		}

		renderTotal(config, graphics, plugin,
				plugin.getTotalQty(), total, totalText, runTimeText, height);

		return null;
	}

	private void renderTotal(InventoryTotalConfig config, Graphics2D graphics, InventoryTotalPlugin plugin,
							 long totalQty, long total, String totalText,
							 String runTimeText, int height) {
		int imageSize = 15;
		boolean showCoinStack = config.showCoinStack();
		int numCoins;
		if (total > Integer.MAX_VALUE)
		{
			numCoins = Integer.MAX_VALUE;
		}
		else if (total < Integer.MIN_VALUE)
		{
			numCoins = Integer.MIN_VALUE;
		}
		else
		{
			numCoins = (int) total;
			if (numCoins == 0)
			{
				numCoins = 1000000;
			}
		}
		numCoins = Math.abs(numCoins);

		if ((totalQty == 0 && !config.showOnEmpty()) || (plugin.getState() == InventoryTotalState.BANK && !config.showWhileBanking())) {
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		final int totalWidth = graphics.getFontMetrics().stringWidth(totalText);

		int fixedRunTimeWidth = 0;
		int actualRunTimeWidth = 0;
		int imageWidthWithPadding = 0;

		if (runTimeText != null && runTimeText.length() >= 2) {
			fixedRunTimeWidth = 5 * (runTimeText.length() - 2) + (3 * 2) + 5;
			actualRunTimeWidth = graphics.getFontMetrics().stringWidth(runTimeText);
		}

		if (showCoinStack)
		{
			imageWidthWithPadding = imageSize + 3;
		}

		int width = totalWidth + fixedRunTimeWidth + imageWidthWithPadding + HORIZONTAL_PADDING * 2;

		int x = (invX + invW / 2) - (width / 2);
		switch (config.horizontalAlignment())
		{
			case CENTER:
				break;

			case LEFT:
				x = invX;
				break;

			case RIGHT:
				x = invX + invW - width;
				break;
		}

		int xOffset = config.inventoryXOffset();
		if (config.isInventoryXOffsetNegative())
		{
			xOffset *= -1;
		}
		x += xOffset;

		int yOffset = config.inventoryYOffset();
		if (config.isInventoryYOffsetNegative())
		{
			yOffset *= -1;
		}
		int y = invY - height - yOffset;

		Color backgroundColor;
		Color borderColor;
		Color textColor;

		if ((plugin.getState() == InventoryTotalState.BANK && config.newRunAfterBanking())
				|| plugin.getMode() == InventoryTotalMode.TOTAL) {
			backgroundColor = config.totalColor();
			borderColor = config.borderColor();
			textColor = config.textColor();
		}
		else if (total >= 0) {
			backgroundColor = config.profitColor();
			borderColor = config.profitBorderColor();
			textColor = config.profitTextColor();
		}
		else {
			backgroundColor = config.lossColor();
			borderColor = config.lossBorderColor();
			textColor = config.lossTextColor();
		}

		int cornerRadius = config.cornerRadius();
		if (!config.roundCorners())
		{
			cornerRadius = 0;
		}

		int containerAlpha = backgroundColor.getAlpha();

		if (containerAlpha > 0) {
			graphics.setColor(borderColor);
			graphics.drawRoundRect(x, y, width + 1, height + 1, cornerRadius, cornerRadius);
		}

		graphics.setColor(backgroundColor);

		graphics.fillRoundRect(x + 1, y + 1, width, height, cornerRadius, cornerRadius);

		TextComponent textComponent = new TextComponent();

		textComponent.setColor(textColor);
		textComponent.setText(totalText);
		textComponent.setPosition(new Point(x + HORIZONTAL_PADDING, y + TEXT_Y_OFFSET));
		textComponent.render(graphics);

		if (runTimeText != null)
		{
			textComponent = new TextComponent();

			textComponent.setColor(textColor);
			textComponent.setText(runTimeText);
			textComponent.setPosition(new Point((x + width) - HORIZONTAL_PADDING - actualRunTimeWidth - imageWidthWithPadding, y + TEXT_Y_OFFSET));
			textComponent.render(graphics);
		}

		if (showCoinStack)
		{
			int imageOffset = 4;

			BufferedImage coinsImage = itemManager.getImage(ItemID.COINS_995, numCoins, false);
			coinsImage = ImageUtil.resizeImage(coinsImage, imageSize, imageSize);
			graphics.drawImage(coinsImage, (x + width) - HORIZONTAL_PADDING - imageSize + imageOffset, y + 3, null);
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		boolean isShowingTooltip = false;

		RoundRectangle2D roundRectangle2D = new RoundRectangle2D.Double(x, y, width + 1, height + 1, cornerRadius, cornerRadius);
		if (roundRectangle2D.contains(mouseX, mouseY) && (plugin.getState() != InventoryTotalState.BANK || !config.newRunAfterBanking())
				&& (Instant.now().toEpochMilli() - newRunTime) > (BANK_CLOSE_DELAY + 500) && config.showTooltip())
		{
			if (plugin.getMode() == InventoryTotalMode.PROFIT_LOSS)
			{
				renderProfitLossLedger(graphics);
			}
			else
			{
				renderLedger(graphics);
			}

			isShowingTooltip = true;
		}

		// display UI above widgets only when showing the tooltip (or the setting is enabled)
		if (lastRenderShowingTooltip != isShowingTooltip)
		{
			plugin.setOverlayAboveWidgets(isShowingTooltip);
		}
		lastRenderShowingTooltip = isShowingTooltip;
	}

	private void renderLedger(Graphics2D graphics)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();

		java.util.List<InventoryTotalLedgerItem> ledger = plugin.getInventoryLedger().stream()
				.filter(item -> item.getQty() != 0).collect(Collectors.toList());

		if (ledger.isEmpty())
		{
			return;
		}

		ledger = ledger.stream().sorted(Comparator.comparingInt(o ->
				-(o.getQty() * o.getAmount()))
		).collect(Collectors.toList());

		int total = ledger.stream().mapToInt(item -> item.getQty() * item.getAmount()).sum();

		ledger.add(new InventoryTotalLedgerItem("Total", 1, total));

		String [] descriptions = ledger.stream().map(item -> {
			String desc = item.getDescription();
			if (item.getQty() != 0 && Math.abs(item.getQty()) != 1
					&& !item.getDescription().contains("Total") && !item.getDescription().contains("Coins"))
			{
				desc = NumberFormat.getInstance(Locale.ENGLISH).format(Math.abs(item.getQty())) + " " + desc;
			}
			return desc;
		}).toArray(String[]::new);
		Integer [] prices = ledger.stream().map(item -> item.getQty() * item.getAmount()).toArray(Integer[]::new);

		String [] formattedPrices = Arrays.stream(prices).map(
				p -> NumberFormat.getInstance(Locale.ENGLISH).format(p)
		).toArray(String[]::new);

		Integer [] rowWidths = IntStream.range(0, descriptions.length).mapToObj(
				i -> fontMetrics.stringWidth(descriptions[i])
						+ fontMetrics.stringWidth(formattedPrices[i])
		).toArray(Integer[]::new);

		Arrays.sort(rowWidths);

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		int sectionPadding = 5;

		int rowW = rowWidths[rowWidths.length - 1] + 20 + HORIZONTAL_PADDING + 2;
		int rowH = fontMetrics.getHeight();

		int h = descriptions.length * rowH + TEXT_Y_OFFSET / 2 + sectionPadding + 1;

		int x = mouseX - rowW - 10;
		int y = mouseY - h / 2;

		int cornerRadius = 0;

		graphics.setColor(LEDGER_BACKGROUND_COLOR);
		graphics.fillRoundRect(x, y, rowW, h, cornerRadius, cornerRadius);

		int borderWidth = 1;

		graphics.setColor(Color.decode("#0b0b0b"));
		graphics.setStroke(new BasicStroke(borderWidth));
		graphics.drawRoundRect(x - borderWidth / 2, y - borderWidth / 2,
				rowW + borderWidth / 2, h + borderWidth / 2, cornerRadius, cornerRadius);

		if (descriptions.length == prices.length)
		{
			int yOffset = 0;
			String prevDesc = "";
			for (int i = 0; i < descriptions.length; i++)
			{
				String desc = descriptions[i];

				if (!prevDesc.contains("Total") && desc.contains("Total"))
				{
					yOffset += sectionPadding;
				}
				else if (i > 0 && prices[i - 1] >= 0 && prices[i] < 0 && !prevDesc.contains("Total"))
				{
					yOffset += sectionPadding;
				}

				int textX = x + HORIZONTAL_PADDING / 2 + 2;
				int textY = y + rowH * i + TEXT_Y_OFFSET + yOffset;

				TextComponent textComponent = new TextComponent();

				if (desc.contains("Total") && desc.length() == 5)
				{
					textComponent.setColor(Color.ORANGE);
				}
				else if (desc.contains("Total"))
				{
					textComponent.setColor(Color.YELLOW);
				}
				else
				{
					textComponent.setColor(Color.decode("#FFF7E3"));
				}

				textComponent.setText(desc);

				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);

				prevDesc = desc;

				int price = prices[i];

				String formattedPrice = NumberFormat.getInstance(Locale.ENGLISH).format(price);

				int textW = fontMetrics.stringWidth(formattedPrice);
				textX = x + rowW - HORIZONTAL_PADDING / 2 - textW;
				textY = y + rowH * i + TEXT_Y_OFFSET + yOffset;

				textComponent = new TextComponent();
				if (price > 0)
				{
					textComponent.setColor(Color.GREEN);
				}
				else
				{
					textComponent.setColor(Color.WHITE);
				}

				textComponent.setText(formattedPrice);

				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);
			}
		}
	}

	private void renderProfitLossLedger(Graphics2D graphics)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();

		java.util.List<InventoryTotalLedgerItem> ledger = plugin.getProfitLossLedger().stream()
				.filter(item -> item.getQty() != 0).collect(Collectors.toList());

		java.util.List<InventoryTotalLedgerItem> gain = ledger.stream().filter(item -> item.getQty() > 0)
				.collect(Collectors.toList());

		java.util.List<InventoryTotalLedgerItem> loss = ledger.stream().filter(item -> item.getQty() < 0)
				.collect(Collectors.toList());

		gain = gain.stream().sorted(Comparator.comparingInt(o -> -(o.getQty() * o.getAmount()))).collect(Collectors.toList());
		loss = loss.stream().sorted(Comparator.comparingInt(o -> (o.getQty() * o.getAmount()))).collect(Collectors.toList());

		ledger = new LinkedList<>();
		ledger.addAll(gain);
		ledger.addAll(loss);

		if (ledger.isEmpty())
		{
			return;
		}

		int totalGain = gain.stream().mapToInt(item -> item.getQty() * item.getAmount()).sum();
		int totalLoss = loss.stream().mapToInt(item -> item.getQty() * item.getAmount()).sum();
		int total = ledger.stream().mapToInt(item -> item.getQty() * item.getAmount()).sum();

		ledger.add(new InventoryTotalLedgerItem("Total Gain", 1, totalGain));
		ledger.add(new InventoryTotalLedgerItem("Total Loss", 1, totalLoss));
		ledger.add(new InventoryTotalLedgerItem("Total", 1, total));

		String [] descriptions = ledger.stream().map(item -> {
			String desc = item.getDescription();
			if (item.getQty() != 0 && Math.abs(item.getQty()) != 1
					&& !item.getDescription().contains("Total") && !item.getDescription().contains("Coins"))
			{
				desc = NumberFormat.getInstance(Locale.ENGLISH).format(Math.abs(item.getQty())) + " " + desc;
			}
			return desc;
		}).toArray(String[]::new);
		Integer [] prices = ledger.stream().map(item -> item.getQty() * item.getAmount()).toArray(Integer[]::new);

		String [] formattedPrices = Arrays.stream(prices).map(
				p -> NumberFormat.getInstance(Locale.ENGLISH).format(p)
		).toArray(String[]::new);

		Integer [] rowWidths = IntStream.range(0, descriptions.length).mapToObj(
				i -> fontMetrics.stringWidth(descriptions[i])
						+ fontMetrics.stringWidth(formattedPrices[i])
		).toArray(Integer[]::new);

		Arrays.sort(rowWidths);

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		int sectionPadding = 5;

		int rowW = rowWidths[rowWidths.length - 1] + 20 + HORIZONTAL_PADDING + 2;
		int rowH = fontMetrics.getHeight();

		int sectionPaddingTotal = sectionPadding;
		if (!gain.isEmpty() && !loss.isEmpty())
		{
			sectionPaddingTotal += sectionPadding;
		}

		int h = descriptions.length * rowH + TEXT_Y_OFFSET / 2 + sectionPaddingTotal + 1;

		int x = mouseX - rowW - 10;
		int y = mouseY - h / 2;

		int cornerRadius = 0;

		graphics.setColor(LEDGER_BACKGROUND_COLOR);
		graphics.fillRoundRect(x, y, rowW, h, cornerRadius, cornerRadius);

		int borderWidth = 1;

		graphics.setColor(Color.decode("#0b0b0b"));
		graphics.setStroke(new BasicStroke(borderWidth));
		graphics.drawRoundRect(x - borderWidth / 2, y - borderWidth / 2,
				rowW + borderWidth / 2, h + borderWidth / 2, cornerRadius, cornerRadius);

		if (descriptions.length == prices.length)
		{
			int yOffset = 0;
			String prevDesc = "";
			for (int i = 0; i < descriptions.length; i++)
			{
				String desc = descriptions[i];

				if (!prevDesc.contains("Total") && desc.contains("Total"))
				{
					yOffset += sectionPadding;
				}
				else if (i > 0 && prices[i - 1] >= 0 && prices[i] < 0 && !prevDesc.contains("Total"))
				{
					yOffset += sectionPadding;
				}

				int textX = x + HORIZONTAL_PADDING / 2 + 2;
				int textY = y + rowH * i + TEXT_Y_OFFSET + yOffset;

				TextComponent textComponent = new TextComponent();

				if (desc.contains("Total") && desc.length() == 5)
				{
					textComponent.setColor(Color.ORANGE);
				}
				else if (desc.contains("Total"))
				{
					textComponent.setColor(Color.YELLOW);
				}
				else
				{
					textComponent.setColor(Color.decode("#FFF7E3"));
				}

				textComponent.setText(desc);

				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);

				prevDesc = desc;

				int price = prices[i];

				String formattedPrice = NumberFormat.getInstance(Locale.ENGLISH).format(price);

				int textW = fontMetrics.stringWidth(formattedPrice);
				textX = x + rowW - HORIZONTAL_PADDING / 2 - textW;
				textY = y + rowH * i + TEXT_Y_OFFSET + yOffset;

				textComponent = new TextComponent();

				if (price > 0)
				{
					textComponent.setColor(Color.GREEN);
				}
				else if (price < 0)
				{
					textComponent.setColor(Color.RED);
				}

				textComponent.setText(formattedPrice);

				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);
			}
		}
	}

	private String getTotalText(long total)
	{
		if (config.showExactGp())
		{
			return getExactFormattedGp(total);
		}
		else
		{
			String totalText = getFormattedGp(total);
			return totalText.replace(".0", "");
		}
	}

	private String getFormattedGp(long total)
	{
		if (total >= 1000000000 || total <= -1000000000)
		{
			double bTotal = total / 1000000000.0;
			return getTruncatedTotal(bTotal) + "B";
		}
		else
		{
			if (total >= 1000000 || total <= -1000000)
			{
				double mTotal = total / 1000000.0;
				return getTruncatedTotal(mTotal) + "M";
			}
			else
			{
				if (total >= 1000 || total <= -1000)
				{
					double kTotal = total / 1000.0;
					return getTruncatedTotal(kTotal) + "K";
				}
				else
				{
					return getExactFormattedGp(total);
				}
			}
		}
	}

	private String getTruncatedTotal(double total)
	{
		String totalString = Double.toString(total);

		int dotIndex = totalString.indexOf('.');
		if (dotIndex < totalString.length() - 1)
		{
			return totalString.substring(0, dotIndex + 2);
		}

		return totalString;
	}

	private String getExactFormattedGp(long total)
	{
		return NumberFormat.getInstance(Locale.ENGLISH).format(total);
	}

	private String getFormattedRunTime()
	{
		long runTime = plugin.elapsedRunTime();

		if (runTime == InventoryTotalPlugin.NO_PROFIT_LOSS_TIME)
		{
			return null;
		}

		long totalSecs = runTime / 1000;
		long totalMins = totalSecs / 60;

		long hrs = totalMins / 60;
		long mins = totalMins % 60;
		long secs = totalSecs % 60;

		if (hrs > 0)
		{
			return String.format(PROFIT_LOSS_TIME_FORMAT, hrs, mins, secs);
		}
		else
		{
			return String.format(PROFIT_LOSS_TIME_NO_HOURS_FORMAT, mins, secs);
		}
	}

	public ItemContainer getInventoryItemContainer()
	{
		return inventoryItemContainer;
	}

	public ItemContainer getEquipmentItemContainer()
	{
		return equipmentItemContainer;
	}

	public void showInterstitial()
	{
		showInterstitial = true;
	}

	public void hideInterstitial()
	{
		showInterstitial = false;
	}

	public void setAboveWidgets(boolean isAboveWidgets)
	{
		if (isAboveWidgets)
		{
			setLayer(OverlayLayer.ABOVE_WIDGETS);
		}
		else
		{
			setLayer(OverlayLayer.UNDER_WIDGETS);
		}
	}
}
