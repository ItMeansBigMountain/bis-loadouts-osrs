package com.itmeansbigmountain.bossreadinessscore;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bossreadinessscore")
public interface BossReadinessScoreConfig extends Config
{
	@ConfigItem(
		keyName = "budgetTier",
		name = "Budget Tier",
		description = "Filters recommendations to practical item price tiers. Boss and style are selected in the side panel.",
		position = 0
	)
	default BudgetTier budgetTier()
	{
		return BudgetTier.NO_LIMIT;
	}
}
