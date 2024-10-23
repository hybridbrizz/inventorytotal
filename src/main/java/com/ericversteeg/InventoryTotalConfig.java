/*
 * Copyright (c) 2024, E. Versteeg <https://github.com/hybridbrizz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ericversteeg;

import net.runelite.client.config.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ConfigGroup(InventoryTotalConfig.GROUP)
public interface InventoryTotalConfig extends Config
{
	String GROUP = "inventorytotal";

	@ConfigItem(
			position = -2,
			keyName = "itemPricesKey",
			name = "Prices",
			description = "Configures the price type."
	)
	default InventoryTotalPriceType priceType()
	{
		return InventoryTotalPriceType.GRAND_EXCHANGE;
	}

	@ConfigItem(
			position = -2,
			keyName = "enableProfitLoss",
			name = "Profit / Loss",
			description = "Configures whether or not current total is relative to start amount."
	)
	default boolean enableProfitLoss()
	{
		return false;
	}

	@ConfigItem(
			position = -2,
			keyName = "profitLossToggleKey",
			name = "Switch Mode Key",
			description = "Switch between Total & Profit / Loss modes."
	)
	default Keybind profitLossToggleKey()
	{
		return new Keybind(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			position = -1,
			keyName = "newRunKey",
			name = "Reset Key",
			description = "Resets the ledger and timer."
	)
	default Keybind newRunKey()
	{
		return new Keybind(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK);
	}

	@Alpha
	@ConfigItem(
			position = 1,
			keyName = "totalBackgroundColor",
			name = "Background Color",
			description = "Configures the background color."
	)
	default Color totalColor()
	{
		return new Color(255,255,255,0);
	}

	@Alpha
	@ConfigItem(
			position = 3,
			keyName = "totalBorderColor",
			name = "Border Color",
			description = "Configures the border color."
	)
	default Color borderColor()
	{
		return new Color(255,255,255,0);
	}

	@Alpha
	@ConfigItem(
			position = 4,
			keyName = "profitBackgroundColor",
			name = "Profit Color",
			description = "Configures profit background color."
	)
	default Color profitColor()
	{
		return new Color(255,255,255,0);
	}

	@Alpha
	@ConfigItem(
			position = 6,
			keyName = "profitBorderColor",
			name = "Profit Border Color",
			description = "Configures profit border color."
	)
	default Color profitBorderColor()
	{
		return new Color(255,255,255,0);
	}

	@Alpha
	@ConfigItem(
			position = 7,
			keyName = "lossBackgroundColor",
			name = "Loss Color",
			description = "Configures loss background color."
	)
	default Color lossColor()
	{
		return new Color(255,255,255,0);
	}

	@Alpha
	@ConfigItem(
			position = 9,
			keyName = "lossBorderColor",
			name = "Loss Border Color",
			description = "Configures loss border color."
	)
	default Color lossBorderColor()
	{
		return new Color(255,255,255,0);
	}

	@ConfigItem(
			position = 11,
			keyName = "roundedCorners",
			name = "Rounded Corners",
			description = "Toggle rounded corners."
	)
	default boolean roundCorners()
	{
		return false;
	}

	@ConfigItem(
			position = 12,
			keyName = "cornerRadius",
			name = "Corner Radius",
			description = "Configures the corner radius."
	)
	default int cornerRadius()
	{
		return 10;
	}

	@ConfigItem(
			position = 13,
			keyName = "alignment",
			name = "Alignment",
			description = "Configures alignment."
	)
	default InventoryTotalAlignment horizontalAlignment()
	{
		return InventoryTotalAlignment.RIGHT;
	}

	@ConfigItem(
			position = 14,
			keyName = "inventoryOffsetX",
			name = "Inventory Offset X",
			description = "Configures x-axis offset."
	)
	default int inventoryXOffset()
	{
		return 26;
	}

	@ConfigItem(
			position = 15,
			keyName = "inventoryOffsetXNegative",
			name = "Inventory Offset X Negative",
			description = "Configures whether or not the y-axis offset is a negative number."
	)
	default boolean isInventoryXOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 16,
			keyName = "inventoryOffsetY",
			name = "Inventory Offset Y",
			description = "Configures y-axis offset."
	)
	default int inventoryYOffset()
	{
		return 36;
	}

	@ConfigItem(
			position = 17,
			keyName = "inventoryOffsetYNegative",
			name = "Inventory Offset Y Negative",
			description = "Configures whether or not the y-axis offset is a negative number."
	)
	default boolean isInventoryYOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 18,
			keyName = "showLapTime",
			name = "Show Run Time",
			description = "Configures whether or not the run time is visible."
	)
	default boolean showRunTime()
	{
		return false;
	}

	@ConfigItem(
			position = 19,
			keyName = "showExactGp",
			name = "Show Exact Gp",
			description = "Configures whether or not the exact gp value is visible."
	)
	default boolean showExactGp()
	{
		return false;
	}

	@ConfigItem(
			position = 20,
			keyName = "showCoinStack",
			name = "Show Coin Stack",
			description = "Configures whether or not the coin stack image is visible."
	)
	default boolean showCoinStack()
	{
		return true;
	}

	@ConfigItem(
			position = 21,
			keyName = "showWhileBanking",
			name = "Show While Banking",
			description = "Configures whether or not the total is visible while banking."
	)
	default boolean showWhileBanking()
	{
		return true;
	}

	@ConfigItem(
			position = 22,
			keyName = "showOnEmpty",
			name = "Show On Empty",
			description = "Configures whether or not to show the total when inventory is empty."
	)
	default boolean showOnEmpty()
	{
		return true;
	}

	@ConfigItem(
			position = 22,
			keyName = "showTooltip",
			name = "Show Ledger",
			description = "Configures whether or not ledger info is shown when mousing over."
	)
	default boolean showTooltip()
	{
		return true;
	}

	@ConfigItem(
			position = 23,
			keyName = "hideWithInventory",
			name = "Hide With Inventory",
			description = "Configures whether or not to hide the tooltip when the inventory is hidden."
	)
	default boolean hideWithInventory()
	{
		return true;
	}

	@ConfigItem(
			position = 23,
			keyName = "newRunAfterBanking",
			name = "Reset After Banking",
			description = "Resets the ledger and timer after banking."
	)
	default boolean newRunAfterBanking() { return true; }

	@ConfigItem(
			position = 24,
			keyName = "ignoredItems",
			name = "Ignored Items",
			description = "Ignore these items in your inventory."
	)
	default String ignoredItems() {
		return "Cannon barrels, Cannon base, Cannon furnace, Cannon stand";
	}
}
