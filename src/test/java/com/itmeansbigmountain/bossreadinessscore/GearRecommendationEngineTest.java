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
		assertEquals(27275, GearRecommendationEngine.fallbackItemId("Tumeken's shadow"));
		assertEquals(20997, GearRecommendationEngine.fallbackItemId("Twisted bow"));
		assertEquals(22325, GearRecommendationEngine.fallbackItemId("Scythe of vitur"));
		assertEquals(26219, GearRecommendationEngine.fallbackItemId("Osmumten's fang"));
		assertEquals(10828, GearRecommendationEngine.fallbackItemId("Helm of neitiznot"));
	}

	@Test
	public void megararesAreRecommendedAndRemoveShieldWhenTwoHanded()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation magic = GearRecommendationEngine.recommend(BossProfile.GENERAL_PVM, CombatStyle.MAGIC, BudgetTier.NO_LIMIT, stats);
		assertEquals("tumeken's shadow", magic.getItem(GearSlot.WEAPON).getName());
		assertTrue(magic.getItem(GearSlot.WEAPON).isTwoHanded());
		assertFalse("2H weapon should not also equip a shield", magic.getItems().containsKey(GearSlot.SHIELD));

		SetupRecommendation ranged = GearRecommendationEngine.recommend(BossProfile.GENERAL_PVM, CombatStyle.RANGED, BudgetTier.NO_LIMIT, stats);
		assertEquals("twisted bow", ranged.getItem(GearSlot.WEAPON).getName());
		assertTrue(ranged.getItem(GearSlot.WEAPON).isTwoHanded());
		assertFalse("2H weapon should not also equip a shield", ranged.getItems().containsKey(GearSlot.SHIELD));
	}

	@Test
	public void localOsrsItemsFillGapsWhenLiveApiIsStale()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);
		java.util.List<GearItem> staleLiveItems = java.util.Collections.singletonList(
			new GearItem(GearSlot.WEAPON, 11907, "trident of the seas", java.util.EnumSet.of(CombatStyle.MAGIC),
				0, 0, 0, 75, 0, 0, 25, 10, 45_000, "stale live api")
		);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossTarget.fromProfile(BossProfile.GENERAL_PVM), CombatStyle.MAGIC, BudgetTier.NO_LIMIT, stats, staleLiveItems);

		assertEquals("tumeken's shadow", recommendation.getItem(GearSlot.WEAPON).getName());
	}

	@Test
	public void recommendedItemsUseOldSchoolWikiLinksAndCanonicalItemNames()
	{
		PlayerStats stats = new PlayerStats(99, 99, 99, 99, 99, 99, 99);

		SetupRecommendation recommendation = GearRecommendationEngine.recommend(
			BossProfile.GENERAL_PVM,
			CombatStyle.MELEE,
			BudgetTier.NO_LIMIT,
			stats
		);

		for (GearItem item : recommendation.getItems().values())
		{
			assertTrue(item.getWikiUrl(), item.getWikiUrl().startsWith("https://oldschool.runescape.wiki/w/"));
			assertFalse("placeholder item names should not be recommended", item.getName().contains(" / "));
			assertFalse("placeholder shorthand should not be recommended", item.getName().equals("nezzy helm"));
			assertFalse("placeholder shorthand should not be recommended", item.getName().equals("faceguard"));
		}
	}
}
