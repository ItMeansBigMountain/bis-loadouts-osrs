package com.itmeansbigmountain.bisloadouts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class ElementalSpellScaling
{
	private static final int[] ELEMENT_INDEX_REQUIREMENTS = {0, 1, 2, 3};
	private static final SpellTier[] TIERS = {
		new SpellTier("Surge", new int[]{81, 85, 90, 95}, new int[]{21, 22, 23, 24}),
		new SpellTier("Wave", new int[]{62, 65, 70, 75}, new int[]{17, 18, 19, 20}),
		new SpellTier("Blast", new int[]{41, 47, 53, 59}, new int[]{13, 14, 15, 16}),
		new SpellTier("Bolt", new int[]{17, 23, 29, 35}, new int[]{9, 10, 11, 12}),
		new SpellTier("Strike", new int[]{1, 5, 9, 13}, new int[]{2, 4, 6, 8})
	};
	private static final Set<String> NON_ELEMENTAL_POWERED_STAVES = new HashSet<>(Arrays.asList(
		"trident of the seas", "trident of the swamp", "sanguinesti staff", "tumeken's shadow", "eye of ayak",
		"warped sceptre", "bone staff", "thammaron's sceptre", "accursed sceptre", "starter staff", "dawnbringer",
		"crystal staff"
	));

	private ElementalSpellScaling()
	{
	}

	static int bestBaseMaxHit(ElementalType element, int magicLevel)
	{
		SpellTier tier = bestTier(element, magicLevel);
		return tier == null ? 0 : tier.scaledBaseHit(magicLevel);
	}

	static String bestSpellName(ElementalType element, int magicLevel)
	{
		SpellTier tier = bestTier(element, magicLevel);
		return tier == null ? "" : element.displayName() + " " + tier.name;
	}

	static int maxHit(int baseMaxHit, int visibleMagicDamagePercent, int weaknessPercent)
	{
		int base = Math.max(0, baseMaxHit);
		int ordinary = (int) Math.floor(base * (1.0D + Math.max(0, visibleMagicDamagePercent) / 100.0D));
		int weakness = (int) Math.floor(base * Math.max(0, weaknessPercent) / 100.0D);
		return ordinary + weakness;
	}

	static double accuracyRollMultiplier(int weaknessPercent)
	{
		return 1.0D + Math.max(0, weaknessPercent) / 100.0D;
	}

	static double applyAccuracyRollMultiplier(double baseHitChance, int bonusPercent)
	{
		double chance = Math.max(0.0D, Math.min(1.0D, baseHitChance));
		double multiplier = accuracyRollMultiplier(bonusPercent);
		if (chance < 0.5D)
		{
			double adjustedRollRatio = chance * 2.0D * multiplier;
			return adjustedRollRatio <= 1.0D
				? adjustedRollRatio / 2.0D
				: 1.0D - 1.0D / (2.0D * adjustedRollRatio);
		}
		return 1.0D - (1.0D - chance) / multiplier;
	}

	static long magicAttackRoll(int visibleMagicLevel, int magicAttackBonus, int conditionalAccuracyPercent)
	{
		long ordinaryRoll = (long) (Math.max(1, visibleMagicLevel) + 8) * (Math.max(-63, magicAttackBonus) + 64L);
		return ordinaryRoll * (100L + Math.max(0, conditionalAccuracyPercent)) / 100L;
	}

	static long npcMagicDefenceRoll(int npcMagicLevel, int magicDefenceBonus)
	{
		return (long) (Math.max(0, npcMagicLevel) + 9) * (Math.max(-63, magicDefenceBonus) + 64L);
	}

	static double hitChance(long attackRoll, long defenceRoll)
	{
		if (attackRoll > defenceRoll)
		{
			return 1.0D - (defenceRoll + 2.0D) / (2.0D * (attackRoll + 1.0D));
		}
		return attackRoll / (2.0D * (defenceRoll + 1.0D));
	}

	static boolean canCastElementalSpell(String weaponName)
	{
		String name = weaponName == null ? "" : weaponName.toLowerCase(Locale.ROOT).trim();
		if (!(name.contains("staff") || name.contains("wand")))
		{
			return false;
		}
		for (String powered : NON_ELEMENTAL_POWERED_STAVES)
		{
			if (name.contains(powered))
			{
				return false;
			}
		}
		return !name.contains("ancient") && !name.contains("sceptre");
	}

	private static SpellTier bestTier(ElementalType element, int magicLevel)
	{
		int elementIndex = elementIndex(element);
		if (elementIndex < 0)
		{
			return null;
		}
		for (SpellTier tier : TIERS)
		{
			if (magicLevel >= tier.requirements[elementIndex])
			{
				return tier;
			}
		}
		return null;
	}

	private static int elementIndex(ElementalType element)
	{
		if (element == null || element == ElementalType.NONE)
		{
			return -1;
		}
		return element.ordinal() - 1;
	}

	private static final class SpellTier
	{
		private final String name;
		private final int[] requirements;
		private final int[] baseHits;

		private SpellTier(String name, int[] requirements, int[] baseHits)
		{
			this.name = name;
			this.requirements = requirements;
			this.baseHits = baseHits;
		}

		private int scaledBaseHit(int magicLevel)
		{
			int unlocked = 0;
			for (int i : ELEMENT_INDEX_REQUIREMENTS)
			{
				if (magicLevel >= requirements[i])
				{
					unlocked = i;
				}
			}
			return baseHits[unlocked];
		}
	}
}
