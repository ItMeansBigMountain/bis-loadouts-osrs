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

	@Test
	public void recommendationIncludesSlotAlternativesForPanelCycling()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.ZULRAH,
			CombatStyle.MAGIC,
			BudgetTier.NO_LIMIT,
			stats
		);

		assertTrue(recommendation.getAlternativesForSlot(GearSlot.WEAPON).size() > 1);
		assertEquals(recommendation.getItem(GearSlot.WEAPON), recommendation.getAlternativesForSlot(GearSlot.WEAPON).get(0));
	}

	@Test
	public void meleeOnlyStyleProducesMeleeGear()
	{
		PlayerStats stats = new PlayerStats(80, 80, 80, 80, 70, 70, 70);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.GIANT_MOLE,
			CombatStyle.MELEE,
			BudgetTier.MIDGAME,
			stats
		);

		assertTrue(recommendation.getStyle().isMelee());
		assertTrue(recommendation.getItem(GearSlot.WEAPON).supports(CombatStyle.MELEE));
	}

	@Test
	public void knownItemFallbackIdsAreCaseInsensitiveForIconLookup()
	{
		assertEquals(23971, GearRecommendationEngine.fallbackItemId("Crystal helm"));
		assertEquals(23857, GearRecommendationEngine.fallbackItemId("corrupted bow (perfected)"));
		assertEquals(28951, GearRecommendationEngine.fallbackItemId("dizana's quiver"));
	}
}
