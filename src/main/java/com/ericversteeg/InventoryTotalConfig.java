package com.ericversteeg;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup(InventoryTotalConfig.GROUP)
public interface InventoryTotalConfig extends Config
{
	String GROUP = "inventorytotal";

	@ConfigItem(
			position = 0,
			keyName = "enableProfitLoss",
			name = "Profit / Loss",
			description = "Configures whether or not the total runs in profit / loss."
	)
	default boolean enableProfitLoss()
	{
		return false;
	}

	@ConfigItem(
			position = 1,
			keyName = "totalBackgroundColor",
			name = "Background Color",
			description = "Configures the background color of the total box."
	)
	default Color totalColor()
	{
		return Color.decode("#939393");
	}

	@ConfigItem(
			position = 2,
			keyName = "profitBackgroundColor",
			name = "Profit Color",
			description = "Configures the background color of the total box when gaining gp in profit / loss."
	)
	default Color profitColor()
	{
		return Color.decode("#42834C");
	}

	@ConfigItem(
			position = 3,
			keyName = "lossBackgroundColor",
			name = "Loss Color",
			description = "Configures the background color of the total box when losing gp in profit / loss."
	)
	default Color lossColor()
	{
		return Color.decode("#912A2A");
	}

	@ConfigItem(
			position = 4,
			keyName = "opaqueBackground",
			name = "Opaque Background",
			description = "Configures whether or not the total box background is opaque."
	)
	default boolean opaqueBackground()
	{
		return true;
	}

	@ConfigItem(
			position = 5,
			keyName = "roundedCorners",
			name = "Rounded Corners",
			description = "Configures whether or not the total box has rounded corners."
	)
	default boolean roundCorners()
	{
		return false;
	}

	@ConfigItem(
			position = 6,
			keyName = "alignment",
			name = "Alignment",
			description = "Configures the alignment of the total box."
	)
	default InventoryTotalAlignment horizontalAlignment()
	{
		return InventoryTotalAlignment.CENTER;
	}

	@ConfigItem(
			position = 7,
			keyName = "inventoryOffsetX",
			name = "Inventory Offset X",
			description = "Configures where the total box x-axis is located relative to the inventory."
	)
	default int inventoryXOffset()
	{
		return 0;
	}

	@ConfigItem(
			position = 8,
			keyName = "inventoryOffsetXNegative",
			name = "Inventory Offset X Negative",
			description = "Configures whether or not the total box y-axis offset is a negative number."
	)
	default boolean isInventoryXOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 9,
			keyName = "inventoryOffsetY",
			name = "Inventory Offset Y",
			description = "Configures where the total box x-axis is located relative to the inventory."
	)
	default int inventoryYOffset()
	{
		return 42;
	}

	@ConfigItem(
			position = 10,
			keyName = "inventoryOffsetYNegative",
			name = "Inventory Offset Y Negative",
			description = "Configures whether or not the total box y-axis offset is a negative number."
	)
	default boolean isInventoryYOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 11,
			keyName = "showRunTime",
			name = "Show Run Time",
			description = "Configures whether or not the run time is shown when available."
	)
	default boolean showRunTime()
	{
		return false;
	}

	@ConfigItem(
			position = 11,
			keyName = "showExactGp",
			name = "Show Exact Gp",
			description = "Configures whether or not the exact gp total is shown in the total box."
	)
	default boolean showExactGp()
	{
		return false;
	}

	@ConfigItem(
			position = 11,
			keyName = "showCoinStack",
			name = "Show Coin Stack",
			description = "Configures whether or not the coin stack is displayed."
	)
	default boolean showCoinStack()
	{
		return true;
	}

	@ConfigItem(
			position = 11,
			keyName = "showWhileBanking",
			name = "Show While Banking",
			description = "Configures whether or not the total box is shown while banking."
	)
	default boolean showWhileBanking()
	{
		return true;
	}
}
