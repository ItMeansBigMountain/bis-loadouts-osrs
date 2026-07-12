package com.itmeansbigmountain.bisloadouts;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BisLoadoutsPluginTest
{
	@Test
	public void calculateLoadoutScoreCapsAtOneHundred()
	{
		int score = BisLoadoutsPlugin.calculateLoadoutScore(126, 99, 99, 99, 85, 43);

		assertEquals(100, score);
	}

	@Test
	public void calculateLoadoutScoreRewardsCoreCombatStats()
	{
		int lowAccountScore = BisLoadoutsPlugin.calculateLoadoutScore(50, 45, 25, 40, 85, 43);
		int readyAccountScore = BisLoadoutsPlugin.calculateLoadoutScore(90, 80, 70, 75, 85, 43);

		assertTrue(readyAccountScore > lowAccountScore);
		assertTrue(readyAccountScore >= 80);
		assertTrue(lowAccountScore < 70);
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BisLoadoutsPlugin.class);
		RuneLite.main(args);
	}
}
