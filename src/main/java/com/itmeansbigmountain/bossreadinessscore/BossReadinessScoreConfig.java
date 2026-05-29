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
		description = "Label for the boss or PvM activity you are checking readiness for",
		position = 0
	)
	default String bossProfile()
	{
		return "General PvM";
	}

	@Range(min = 1, max = 126)
	@ConfigItem(
		keyName = "targetCombatLevel",
		name = "Target Combat Level",
		description = "Combat level considered fully ready for the selected boss profile",
		position = 1
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
		position = 2
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
		position = 3
	)
	default int warningThreshold()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "showLoginSummary",
		name = "Show Login Summary",
		description = "Print the readiness score to chat after login",
		position = 4
	)
	default boolean showLoginSummary()
	{
		return true;
	}
}
