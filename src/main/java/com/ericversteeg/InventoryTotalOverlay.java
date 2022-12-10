package com.ericversteeg;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Locale;

class InventoryTotalOverlay extends Overlay
{
	private static final int CORNER_RADIUS = 10;
	private static final int TEXT_Y_OFFSET = 17;
	private static final String PROFIT_LOSS_TIME_FORMAT = "%02d:%02d:%02d";
	private static final String PROFIT_LOSS_TIME_NO_HOURS_FORMAT = "%02d:%02d";
	private static final int HORIZONTAL_PADDING = 10;

	private final Client client;
	private final InventoryTotalPlugin plugin;
	private final InventoryTotalConfig config;

	private final ItemManager itemManager;

	private Widget inventoryWidget;
	private ItemContainer inventoryItemContainer;
	private ItemContainer equipmentItemContainer;

	private boolean onceBank = false;

	private boolean showInterstitial = false;

	@Inject
	private InventoryTotalOverlay(Client client, InventoryTotalPlugin plugin, InventoryTotalConfig config, ItemManager itemManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);

		this.client = client;
		this.plugin = plugin;
		this.config = config;

		this.itemManager = itemManager;
	}

	void updatePluginState()
	{
		inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);

		inventoryItemContainer = client.getItemContainer(InventoryID.INVENTORY);
		equipmentItemContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (config.enableProfitLoss())
		{
			plugin.setMode(InventoryTotalMode.PROFIT_LOSS);
		}
		else
		{
			plugin.setMode(InventoryTotalMode.TOTAL);
		}

		boolean isBank = false;

		if (inventoryWidget == null || inventoryWidget.getCanvasLocation().getX() < 0 || inventoryWidget.isHidden())
		{
			inventoryWidget = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
			if (inventoryWidget != null && !inventoryWidget.isHidden())
			{
				isBank = true;
				if (!onceBank)
				{
					onceBank = true;
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
		boolean newRun = plugin.getPreviousState() == InventoryTotalState.BANK && plugin.getState() == InventoryTotalState.RUN;

		// totals
		int [] inventoryTotals = plugin.getInventoryTotals();

		int inventoryTotal = inventoryTotals[InventoryTotalPlugin.TOTAL_GP_INDEX];
		int equipmentTotal = plugin.getEquipmentTotal();

		int inventoryQty = inventoryTotals[InventoryTotalPlugin.TOTAL_QTY_INDEX];

		int totalGp = inventoryTotal;
		if (plugin.getState() == InventoryTotalState.RUN && plugin.getMode() == InventoryTotalMode.PROFIT_LOSS)
		{
			totalGp += equipmentTotal;
		}

		plugin.setTotalGp(totalGp);
		plugin.setTotalQty(inventoryQty);

		// after totals
		if (newRun)
		{
			plugin.onNewRun();
		}
		else if (plugin.getPreviousState() == InventoryTotalState.RUN && plugin.getState() == InventoryTotalState.BANK)
		{
			plugin.onBank();
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		updatePluginState();

		boolean isInvHidden = inventoryWidget == null || inventoryWidget.isHidden();
		if (isInvHidden || inventoryItemContainer == null)
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
				totalText = getTotalText(plugin.getTotalGp());
			}
		}

		renderTotal(config, graphics, plugin, inventoryWidget,
				plugin.getTotalQty(), total, totalText, runTimeText, height);

		return null;
	}

	private void renderTotal(InventoryTotalConfig config, Graphics2D graphics, InventoryTotalPlugin plugin,
							 Widget inventoryWidget, long totalQty, long total, String totalText,
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

		if (totalQty == 0 || (plugin.getState() == InventoryTotalState.BANK && !config.showWhileBanking())) {
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

		int x = (inventoryWidget.getCanvasLocation().getX() + inventoryWidget.getWidth() / 2) - (width / 2);
		switch (config.horizontalAlignment())
		{
			case CENTER:
				break;

			case LEFT:
				x = inventoryWidget.getCanvasLocation().getX();
				break;

			case RIGHT:
				x = inventoryWidget.getCanvasLocation().getX() + inventoryWidget.getWidth() - width;
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
		int y = inventoryWidget.getCanvasLocation().getY() - height - yOffset;

		Color backgroundColor;
		Color borderColor;
		Color textColor;

		if (plugin.getState() == InventoryTotalState.BANK || plugin.getMode() == InventoryTotalMode.TOTAL) {
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
}
