package com.itmeansbigmountain.bossreadinessscore;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BossReadinessScorePluginTest
{
	@Test
	public void calculateReadinessScoreCapsAtOneHundred()
	{
		int score = BossReadinessScorePlugin.calculateReadinessScore(126, 99, 99, 99, 85, 43);

		assertEquals(100, score);
	}

	@Test
	public void calculateReadinessScoreRewardsCoreCombatStats()
	{
		int lowAccountScore = BossReadinessScorePlugin.calculateReadinessScore(50, 45, 25, 40, 85, 43);
		int readyAccountScore = BossReadinessScorePlugin.calculateReadinessScore(90, 80, 70, 75, 85, 43);

		assertTrue(readyAccountScore > lowAccountScore);
		assertTrue(readyAccountScore >= 80);
		assertTrue(lowAccountScore < 70);
	}

	@Test
	public void buildSummaryMessageIncludesBossProfileScoreAndRecommendation()
	{
		String message = BossReadinessScorePlugin.buildSummaryMessage("Vorkath", 65, 70);

		assertTrue(message.contains("Vorkath"));
		assertTrue(message.contains("65/100"));
		assertTrue(message.contains("caution"));
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BossReadinessScorePlugin.class);
		RuneLite.main(args);
	}
}
