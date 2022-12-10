package com.ericversteeg;

import net.runelite.client.config.Alpha;
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

	@Alpha
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
			keyName = "totalTextColor",
			name = "Text Color",
			description = "Configures the text color of the total box."
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
			position = 3,
			keyName = "totalBorderColor",
			name = "Border Color",
			description = "Configures the border color of the total box."
	)
	default Color borderColor()
	{
		return Color.BLACK;
	}

	@Alpha
	@ConfigItem(
			position = 4,
			keyName = "profitBackgroundColor",
			name = "Profit Color",
			description = "Configures the background color of the total box when gaining gp in profit / loss."
	)
	default Color profitColor()
	{
		return Color.decode("#42834C");
	}

	@ConfigItem(
			position = 5,
			keyName = "profitTextColor",
			name = "Profit Text Color",
			description = "Configures the text color of the total box when gaining gp in profit / loss."
	)
	default Color profitTextColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
			position = 6,
			keyName = "profitBorderColor",
			name = "Profit Border Color",
			description = "Configures the border color of the total box when gaining gp in profit / loss."
	)
	default Color profitBorderColor()
	{
		return Color.BLACK;
	}

	@Alpha
	@ConfigItem(
			position = 7,
			keyName = "lossBackgroundColor",
			name = "Loss Color",
			description = "Configures the background color of the total box when losing gp in profit / loss."
	)
	default Color lossColor()
	{
		return Color.decode("#912A2A");
	}

	@ConfigItem(
			position = 8,
			keyName = "lossTextColor",
			name = "Loss Text Color",
			description = "Configures the text color of the total box when losing gp in profit / loss."
	)
	default Color lossTextColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
			position = 9,
			keyName = "lossBorderColor",
			name = "Loss Border Color",
			description = "Configures the border color of the total box when losing gp in profit / loss."
	)
	default Color lossBorderColor()
	{
		return Color.BLACK;
	}

	@ConfigItem(
			position = 11,
			keyName = "roundedCorners",
			name = "Rounded Corners",
			description = "Configures whether or not the total box has rounded corners."
	)
	default boolean roundCorners()
	{
		return false;
	}

	@ConfigItem(
			position = 12,
			keyName = "cornerRadius",
			name = "Corner Radius",
			description = "Configures the corner radius for rounded corners."
	)
	default int cornerRadius()
	{
		return 10;
	}

	@ConfigItem(
			position = 13,
			keyName = "alignment",
			name = "Alignment",
			description = "Configures the alignment of the total box."
	)
	default InventoryTotalAlignment horizontalAlignment()
	{
		return InventoryTotalAlignment.CENTER;
	}

	@ConfigItem(
			position = 14,
			keyName = "inventoryOffsetX",
			name = "Inventory Offset X",
			description = "Configures where the total box x-axis is located relative to the inventory."
	)
	default int inventoryXOffset()
	{
		return 0;
	}

	@ConfigItem(
			position = 15,
			keyName = "inventoryOffsetXNegative",
			name = "Inventory Offset X Negative",
			description = "Configures whether or not the total box y-axis offset is a negative number."
	)
	default boolean isInventoryXOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 16,
			keyName = "inventoryOffsetY",
			name = "Inventory Offset Y",
			description = "Configures where the total box x-axis is located relative to the inventory."
	)
	default int inventoryYOffset()
	{
		return 42;
	}

	@ConfigItem(
			position = 17,
			keyName = "inventoryOffsetYNegative",
			name = "Inventory Offset Y Negative",
			description = "Configures whether or not the total box y-axis offset is a negative number."
	)
	default boolean isInventoryYOffsetNegative()
	{
		return false;
	}

	@ConfigItem(
			position = 18,
			keyName = "showLapTime",
			name = "Show Lap Time",
			description = "Configures whether or not a lap time is shown when available."
	)
	default boolean showRunTime()
	{
		return false;
	}

	@ConfigItem(
			position = 19,
			keyName = "showExactGp",
			name = "Show Exact Gp",
			description = "Configures whether or not the exact gp total is shown in the total box."
	)
	default boolean showExactGp()
	{
		return false;
	}

	@ConfigItem(
			position = 20,
			keyName = "showCoinStack",
			name = "Show Coin Stack",
			description = "Configures whether or not the coin stack is displayed."
	)
	default boolean showCoinStack()
	{
		return true;
	}

	@ConfigItem(
			position = 21,
			keyName = "showWhileBanking",
			name = "Show While Banking",
			description = "Configures whether or not the total box is shown while banking."
	)
	default boolean showWhileBanking()
	{
		return true;
	}
}
