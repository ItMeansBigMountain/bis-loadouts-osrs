package com.itmeansbigmountain.bossreadinessscore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearRecommendationEngineTest
{
	@Test
	public void recommendsBestAffordableGearForSelectedBossStyleAndStats()
	{
		PlayerStats stats = new PlayerStats(70, 70, 70, 70, 75, 70, 60);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.MAGIC,
			BudgetTier.MIDGAME,
			stats
		);

		assertEquals("Zulrah", recommendation.getBossName());
		assertEquals(CombatStyle.MAGIC, recommendation.getStyle());
		assertEquals("trident of the swamp", recommendation.getItem(GearSlot.WEAPON).getName());
		assertEquals("ahrim's robetop", recommendation.getItem(GearSlot.BODY).getName());
		assertTrue(recommendation.getEstimatedDps() > 0.0D);
		assertTrue(recommendation.getReadinessScore() > 0);
	}

	@Test
	public void blocksItemsWhenPlayerMissingRequirements()
	{
		PlayerStats stats = new PlayerStats(60, 60, 60, 60, 50, 60, 43);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.MAGIC,
			BudgetTier.NO_LIMIT,
			stats
		);

		assertFalse(recommendation.getItems().values().stream().anyMatch(item -> item.getName().contains("ancestral")));
		assertTrue(recommendation.getWarnings().stream().anyMatch(warning -> warning.contains("Magic level")));
	}

	@Test
	public void autoStyleChoosesHighestScoringSetup()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.AUTO,
			BudgetTier.NO_LIMIT,
			stats
		);

		assertEquals(CombatStyle.MAGIC, recommendation.getStyle());
		assertTrue(recommendation.getAlternatives().stream().anyMatch(alt -> alt.getStyle() == CombatStyle.RANGED));
	}
}
