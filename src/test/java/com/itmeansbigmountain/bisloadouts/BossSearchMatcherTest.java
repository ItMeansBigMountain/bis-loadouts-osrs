package com.itmeansbigmountain.bisloadouts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class BossSearchMatcherTest
{
	private static final List<String> BOSSES = Arrays.asList(
		"Zulrah", "Vorkath", "King Black Dragon", "TzTok-Jad", "General Graardor",
		"Cerberus", "The Hueycoatl", "Phantom Muspah", "TzKal-Zuk", "Demonic Brutus", "Brutus");

	@Test
	public void typoStillFindsTheIntendedBossFirst()
	{
		assertEquals("Vorkath", BossSearchMatcher.rank(BOSSES, "vorkth", 10).get(0));
	}

	@Test
	public void commonBossShorthandFindsCanonicalNames()
	{
		assertEquals("King Black Dragon", BossSearchMatcher.rank(BOSSES, "kbd", 10).get(0));
		assertEquals("TzTok-Jad", BossSearchMatcher.rank(BOSSES, "jad", 10).get(0));
		assertEquals("The Hueycoatl", BossSearchMatcher.rank(BOSSES, "huey", 10).get(0));
	}

	@Test
	public void wordsCanBeTypedInAnyOrder()
	{
		assertEquals("King Black Dragon", BossSearchMatcher.rank(BOSSES, "dragon black king", 10).get(0));
	}

	@Test
	public void exactBossRanksBeforeLongerVariant()
	{
		assertEquals(Arrays.asList("Brutus", "Demonic Brutus"),
			BossSearchMatcher.rank(BOSSES, "brutus", 10));
	}

	@Test
	public void nonsenseDoesNotFillThePopupWithUnrelatedBosses()
	{
		assertTrue(BossSearchMatcher.rank(BOSSES, "zzzzzz", 10).isEmpty());
	}

	@Test
	public void resultLimitKeepsTheSidebarCompactWithoutReorderingBlankSearch()
	{
		assertEquals(BOSSES.subList(0, 3), BossSearchMatcher.rank(BOSSES, "", 3));
	}
}
