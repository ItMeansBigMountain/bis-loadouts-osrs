package com.itmeansbigmountain.bisloadouts;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElementalSpellScalingTest
{
	@Test
	public void lowerElementsScaleWithinTheHighestUnlockedSpellTier()
	{
		assertEquals(8, ElementalSpellScaling.bestBaseMaxHit(ElementalType.AIR, 13));
		assertEquals(12, ElementalSpellScaling.bestBaseMaxHit(ElementalType.WATER, 35));
		assertEquals(19, ElementalSpellScaling.bestBaseMaxHit(ElementalType.EARTH, 70));
		assertEquals(20, ElementalSpellScaling.bestBaseMaxHit(ElementalType.WATER, 81));
		assertEquals(21, ElementalSpellScaling.bestBaseMaxHit(ElementalType.AIR, 81));
		assertEquals(24, ElementalSpellScaling.bestBaseMaxHit(ElementalType.FIRE, 95));
	}

	@Test
	public void unavailableMatchingElementDoesNotProduceAnElementalSpell()
	{
		assertEquals(0, ElementalSpellScaling.bestBaseMaxHit(ElementalType.FIRE, 12));
		assertEquals("", ElementalSpellScaling.bestSpellName(ElementalType.FIRE, 12));
		assertEquals(0, ElementalSpellScaling.bestBaseMaxHit(ElementalType.NONE, 99));
	}

	@Test
	public void weaknessDamageIsAddedToOrdinaryMagicDamageFromTheSameBase()
	{
		assertEquals(43, ElementalSpellScaling.maxHit(24, 30, 50));
		assertEquals(48, ElementalSpellScaling.maxHit(24, 0, 100));
		assertEquals(1.5D, ElementalSpellScaling.accuracyRollMultiplier(50), 0.0001D);
		assertEquals(0.8333D, ElementalSpellScaling.applyAccuracyRollMultiplier(0.75D, 50), 0.0001D);
		assertEquals(0.5833D, ElementalSpellScaling.applyAccuracyRollMultiplier(0.40D, 50), 0.0001D);
		assertEquals(17548L, ElementalSpellScaling.magicAttackRoll(99, 100, 0));
		assertEquals(26322L, ElementalSpellScaling.magicAttackRoll(99, 100, 50));
		assertEquals(17876L, ElementalSpellScaling.npcMagicDefenceRoll(100, 100));
		assertEquals(0.660411D, ElementalSpellScaling.hitChance(26322L, 17876L), 0.000001D);
	}

	@Test
	public void onlyStandardElementalAutocastWeaponsCanExploitWeakness()
	{
		assertTrue(ElementalSpellScaling.canCastElementalSpell("harmonised nightmare staff"));
		assertTrue(ElementalSpellScaling.canCastElementalSpell("smoke battlestaff"));
		assertTrue(ElementalSpellScaling.canCastElementalSpell("purging staff"));
		assertFalse(ElementalSpellScaling.canCastElementalSpell("trident of the swamp"));
		assertFalse(ElementalSpellScaling.canCastElementalSpell("tumeken's shadow"));
		assertFalse(ElementalSpellScaling.canCastElementalSpell("eye of ayak"));
	}
}
