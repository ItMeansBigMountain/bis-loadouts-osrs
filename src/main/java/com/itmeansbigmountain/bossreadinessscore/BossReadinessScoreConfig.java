package com.itmeansbigmountain.bossreadinessscore;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("bossreadinessscore")
public interface BossReadinessScoreConfig extends Config
{
	@ConfigItem(
		keyName = "bossProfile",
		name = "Boss Profile",
		description = "Boss or PvM activity to score and recommend gear for",
		position = 0
	)
	default BossProfile bossProfile()
	{
		return BossProfile.GENERAL_PVM;
	}

	@ConfigItem(
		keyName = "bossName",
		name = "Boss Name",
		description = "Optional live boss lookup. Type any OSRS boss name; data comes from OSRS Wiki/GearScape so new bosses do not need plugin updates.",
		position = 1
	)
	default String bossName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "combatStyle",
		name = "Combat Style",
		description = "Combat style to recommend gear for. Auto compares supported styles.",
		position = 2
	)
	default CombatStyle combatStyle()
	{
		return CombatStyle.AUTO;
	}

	@ConfigItem(
		keyName = "budgetTier",
		name = "Budget Tier",
		description = "Filters recommendations to practical item price tiers.",
		position = 3
	)
	default BudgetTier budgetTier()
	{
		return BudgetTier.MIDGAME;
	}

	@Range(min = 1, max = 126)
	@ConfigItem(
		keyName = "targetCombatLevel",
		name = "Target Combat Level",
		description = "Combat level considered fully ready for the selected boss profile",
		position = 4
	)
	default int targetCombatLevel()
	{
		return 85;
	}

	@Range(min = 1, max = 99)
	@ConfigItem(
		keyName = "targetPrayerLevel",
		name = "Target Prayer Level",
		description = "Prayer level considered fully ready for the selected boss profile",
		position = 5
	)
	default int targetPrayerLevel()
	{
		return 43;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "warningThreshold",
		name = "Warning Threshold",
		description = "Show a caution message when the readiness score is below this value",
		position = 6
	)
	default int warningThreshold()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "showLoginSummary",
		name = "Show Login Summary",
		description = "Print the readiness score to chat after login",
		position = 7
	)
	default boolean showLoginSummary()
	{
		return true;
	}
}
