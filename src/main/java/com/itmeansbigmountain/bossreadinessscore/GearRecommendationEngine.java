package com.itmeansbigmountain.bossreadinessscore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class GearRecommendationEngine
{
	private static final List<GearItem> ITEMS = buildItems();

	private GearRecommendationEngine()
	{
	}

	public static SetupRecommendation recommend(BossProfile boss, CombatStyle requestedStyle, BudgetTier budget, PlayerStats stats)
	{
		return recommend(BossTarget.fromProfile(boss), requestedStyle, budget, stats, ITEMS);
	}

	public static SetupRecommendation recommend(BossTarget boss, CombatStyle requestedStyle, BudgetTier budget, PlayerStats stats, List<GearItem> liveItems)
	{
		List<GearItem> candidateItems = liveItems == null || liveItems.isEmpty() ? ITEMS : liveItems;
		List<CombatStyle> styles = requestedStyle == CombatStyle.AUTO
			? Arrays.asList(CombatStyle.MAGIC, CombatStyle.RANGED, CombatStyle.STAB, CombatStyle.SLASH, CombatStyle.CRUSH)
			: Collections.singletonList(requestedStyle);

		List<SetupRecommendation> scored = styles.stream()
			.map(style -> recommendSingleStyle(boss, style, budget, stats, candidateItems))
			.sorted(Comparator.comparingDouble(SetupRecommendation::getEstimatedDps).reversed())
			.collect(Collectors.toList());

		SetupRecommendation best = scored.get(0);
		List<SetupRecommendation> alternatives = scored.stream()
			.filter(rec -> rec != best)
			.collect(Collectors.toList());

		return new SetupRecommendation(best.getBossName(), best.getStyle(), best.getItems(), best.getEstimatedDps(),
			best.getHitChance(), best.getMaxHit(), best.getReadinessScore(), best.getWarnings(), alternatives);
	}

	private static SetupRecommendation recommendSingleStyle(BossTarget boss, CombatStyle style, BudgetTier budget, PlayerStats stats, List<GearItem> items)
	{
		Map<GearSlot, GearItem> selected = new EnumMap<>(GearSlot.class);
		List<String> warnings = new ArrayList<>();

		for (GearSlot slot : GearSlot.values())
		{
			items.stream()
				.filter(item -> item.getSlot() == slot)
				.filter(item -> item.supports(style))
				.filter(item -> item.getPrice() <= budget.getMaxItemPrice())
				.filter(item -> item.meetsRequirements(stats))
				.max(Comparator.comparingInt(GearItem::scoreValue))
				.ifPresent(item -> selected.put(slot, item));
		}

		addRequirementWarnings(style, stats, warnings);
		if (!selected.containsKey(GearSlot.WEAPON))
		{
			warnings.add("No usable " + style + " weapon found for your stats/budget.");
		}

		int gearScore = selected.values().stream().mapToInt(GearItem::scoreValue).sum();
		double styleLevel = styleLevel(style, stats);
		double targetPressure = targetPressure(boss, style);
		double hitChance = clamp(0.35D + (styleLevel / 140.0D) + (gearScore / 900.0D) - targetPressure, 0.05D, 0.98D);
		int maxHit = Math.max(1, (int) Math.round((styleDamageLevel(style, stats) / 8.0D) + selected.values().stream().mapToInt(GearItem::getStrengthBonus).sum() / 10.0D));
		double attackSpeed = style == CombatStyle.MAGIC ? 2.4D : style == CombatStyle.RANGED ? 3.0D : 3.6D;
		double dps = round(((maxHit * hitChance / 2.0D) / attackSpeed) * styleMultiplier(boss, style));
		int readiness = calculateReadiness(boss, style, stats, selected, warnings);

		return new SetupRecommendation(boss.getLabel(), style, selected, dps, hitChance, maxHit, readiness, warnings, Collections.emptyList());
	}

	private static void addRequirementWarnings(CombatStyle style, PlayerStats stats, List<String> warnings)
	{
		if (style == CombatStyle.MAGIC && stats.getMagic() < 75)
		{
			warnings.add("Magic level is below 75, so top-tier magic gear is filtered out.");
		}
		if (style == CombatStyle.RANGED && stats.getRanged() < 75)
		{
			warnings.add("Ranged level is below 75, so top-tier ranged gear is filtered out.");
		}
		if ((style == CombatStyle.STAB || style == CombatStyle.SLASH || style == CombatStyle.CRUSH)
			&& (stats.getAttack() < 75 || stats.getStrength() < 75))
		{
			warnings.add("Melee stats are below 75, so top-tier melee gear is filtered out.");
		}
	}

	private static int calculateReadiness(BossTarget boss, CombatStyle style, PlayerStats stats, Map<GearSlot, GearItem> items, List<String> warnings)
	{
		int core = basicReadinessScore(boss.getTargetCombat(), stats.getHitpoints(), stats.getPrayer(), stats.getDefence(), boss.getTargetCombat(), 43);
		double styleRatio;
		if (style == CombatStyle.MAGIC)
		{
			styleRatio = stats.getMagic() / (double) boss.getTargetMagic();
		}
		else if (style == CombatStyle.RANGED)
		{
			styleRatio = stats.getRanged() / (double) boss.getTargetRanged();
		}
		else
		{
			styleRatio = Math.min(stats.getAttack() / (double) boss.getTargetAttack(), stats.getStrength() / (double) boss.getTargetStrength());
		}
		int gearSlots = items.size();
		int score = (int) Math.round(core * 0.45D + clamp(styleRatio, 0.0D, 1.0D) * 40.0D + Math.min(gearSlots, 10) * 1.5D);
		if (!warnings.isEmpty())
		{
			score -= 5;
		}
		return Math.max(0, Math.min(100, score));
	}

	private static double styleLevel(CombatStyle style, PlayerStats stats)
	{
		switch (style)
		{
			case MAGIC:
				return stats.getMagic();
			case RANGED:
				return stats.getRanged();
			default:
				return (stats.getAttack() + stats.getStrength()) / 2.0D;
		}
	}

	private static double styleDamageLevel(CombatStyle style, PlayerStats stats)
	{
		switch (style)
		{
			case MAGIC:
				return stats.getMagic();
			case RANGED:
				return stats.getRanged();
			default:
				return stats.getStrength();
		}
	}

	private static double targetPressure(BossTarget boss, CombatStyle style)
	{
		int defence;
		switch (style)
		{
			case MAGIC:
				defence = boss.getDefMagic();
				break;
			case RANGED:
				defence = boss.getDefRanged();
				break;
			case STAB:
				defence = boss.getDefStab();
				break;
			case SLASH:
				defence = boss.getDefSlash();
				break;
			case CRUSH:
			default:
				defence = boss.getDefCrush();
				break;
		}
		return clamp(defence / 700.0D, 0.0D, 0.45D);
	}

	private static double styleMultiplier(BossTarget boss, CombatStyle style)
	{
		String name = boss.getLabel().toLowerCase();
		if (name.contains("zulrah") && style == CombatStyle.MAGIC)
		{
			return 1.45D;
		}
		if (name.contains("zulrah") && style == CombatStyle.RANGED)
		{
			return 0.85D;
		}
		if (name.contains("vorkath") && style == CombatStyle.RANGED)
		{
			return 1.25D;
		}
		if (boss.getAttributes().contains("melee immune") && (style == CombatStyle.STAB || style == CombatStyle.SLASH || style == CombatStyle.CRUSH))
		{
			return 0.25D;
		}
		return 1.0D;
	}

	private static double clamp(double value, double min, double max)
	{
		return Math.max(min, Math.min(max, value));
	}

	private static int basicReadinessScore(int combatLevel, int hitpointsLevel, int prayerLevel, int defenceLevel,
		int targetCombatLevel, int targetPrayerLevel)
	{
		double combatScore = ratioScore(combatLevel, Math.max(1, targetCombatLevel)) * 0.55D;
		double hitpointsScore = ratioScore(hitpointsLevel, 99) * 0.20D;
		double prayerScore = ratioScore(prayerLevel, Math.max(1, targetPrayerLevel)) * 0.15D;
		double defenceScore = ratioScore(defenceLevel, 99) * 0.10D;
		return Math.max(0, Math.min(100, (int) Math.round(combatScore + hitpointsScore + prayerScore + defenceScore)));
	}

	private static double ratioScore(int actual, int target)
	{
		return Math.min(1.0D, Math.max(0.0D, actual / (double) target)) * 100.0D;
	}

	private static double round(double value)
	{
		return Math.round(value * 100.0D) / 100.0D;
	}

	private static List<GearItem> buildItems()
	{
		List<GearItem> items = new ArrayList<>();
		addMagic(items);
		addRanged(items);
		addMelee(items);
		return items;
	}

	private static void addMagic(List<GearItem> items)
	{
		Set<CombatStyle> magic = EnumSet.of(CombatStyle.MAGIC);
		items.add(item(GearSlot.WEAPON, "trident of the seas", magic, 0, 0, 0, 75, 0, 0, 25, 10, 45_000, "Baseline powered staff."));
		items.add(item(GearSlot.WEAPON, "trident of the swamp", magic, 0, 0, 0, 75, 0, 0, 32, 16, 2_500_000, "Strong practical Zulrah staff."));
		items.add(item(GearSlot.WEAPON, "sanguinesti staff", magic, 0, 0, 0, 82, 0, 0, 40, 24, 85_000_000, "High-end sustain option."));
		items.add(item(GearSlot.WEAPON, "eye of ayak", magic, 0, 0, 0, 90, 0, 0, 48, 30, 55_000_000, "GearScape-inspired high DPS option."));
		items.add(item(GearSlot.HEAD, "mystic hat", magic, 0, 0, 20, 40, 0, 0, 4, 0, 25_000, "Budget magic."));
		items.add(item(GearSlot.HEAD, "ahrim's hood", magic, 0, 0, 70, 70, 0, 0, 6, 1, 500_000, "Midgame magic."));
		items.add(item(GearSlot.HEAD, "ancestral hat", magic, 0, 0, 65, 75, 0, 0, 8, 3, 58_000_000, "BiS-style magic."));
		items.add(item(GearSlot.BODY, "mystic robe top", magic, 0, 0, 20, 40, 0, 0, 12, 0, 75_000, "Budget magic."));
		items.add(item(GearSlot.BODY, "ahrim's robetop", magic, 0, 0, 70, 70, 0, 0, 30, 2, 4_500_000, "Midgame magic."));
		items.add(item(GearSlot.BODY, "ancestral robe top", magic, 0, 0, 65, 75, 0, 0, 35, 3, 127_000_000, "BiS-style magic."));
		items.add(item(GearSlot.LEGS, "ahrim's robeskirt", magic, 0, 0, 70, 70, 0, 0, 24, 2, 3_500_000, "Midgame magic."));
		items.add(item(GearSlot.LEGS, "ancestral robe bottom", magic, 0, 0, 65, 75, 0, 0, 26, 3, 88_000_000, "BiS-style magic."));
		items.add(item(GearSlot.NECK, "occult necklace", magic, 0, 0, 1, 70, 0, 0, 12, 5, 450_000, "Huge cheap magic upgrade."));
		items.add(item(GearSlot.CAPE, "imbued god cape", magic, 0, 0, 1, 75, 0, 0, 15, 2, 0, "Mage Arena II cape."));
		items.add(item(GearSlot.SHIELD, "elidinis' ward", magic, 0, 0, 1, 80, 0, 0, 25, 5, 8_500_000, "Strong magic offhand."));
		items.add(item(GearSlot.HANDS, "barrows gloves", magic, 0, 0, 1, 1, 1, 1, 6, 0, 130_000, "Quest glove fallback."));
		items.add(item(GearSlot.HANDS, "tormented bracelet", magic, 0, 0, 75, 75, 0, 0, 10, 5, 16_000_000, "High-impact magic glove."));
		items.add(item(GearSlot.FEET, "eternal boots", magic, 0, 0, 75, 75, 0, 0, 8, 0, 5_000_000, "Magic boots."));
		items.add(item(GearSlot.RING, "magus ring", magic, 0, 0, 1, 75, 0, 0, 15, 2, 30_000_000, "High-end magic ring."));
	}

	private static void addRanged(List<GearItem> items)
	{
		Set<CombatStyle> ranged = EnumSet.of(CombatStyle.RANGED);
		items.add(item(GearSlot.WEAPON, "rune crossbow", ranged, 0, 0, 1, 0, 61, 0, 90, 60, 10_000, "Budget crossbow."));
		items.add(item(GearSlot.WEAPON, "toxic blowpipe", ranged, 0, 0, 1, 0, 75, 0, 75, 70, 2_500_000, "Fast practical ranged."));
		items.add(item(GearSlot.WEAPON, "bow of faerdhinen", ranged, 0, 0, 70, 0, 80, 0, 128, 106, 130_000_000, "High-end bow."));
		items.add(item(GearSlot.WEAPON, "twisted bow", ranged, 0, 0, 1, 0, 75, 0, 140, 120, 1_600_000_000, "Theoretical high-end ranged."));
		items.add(item(GearSlot.HEAD, "black d'hide coif", ranged, 0, 0, 40, 0, 70, 0, 10, 0, 8_000, "Budget ranged."));
		items.add(item(GearSlot.HEAD, "armadyl helmet", ranged, 0, 0, 70, 0, 70, 0, 15, 1, 8_000_000, "Mid/high ranged."));
		items.add(item(GearSlot.HEAD, "masori mask (f)", ranged, 0, 0, 80, 0, 80, 0, 20, 2, 22_000_000, "BiS-style ranged."));
		items.add(item(GearSlot.BODY, "black d'hide body", ranged, 0, 0, 40, 0, 70, 0, 30, 0, 9_000, "Budget ranged."));
		items.add(item(GearSlot.BODY, "armadyl chestplate", ranged, 0, 0, 70, 0, 70, 0, 33, 1, 35_000_000, "High ranged."));
		items.add(item(GearSlot.BODY, "masori body (f)", ranged, 0, 0, 80, 0, 80, 0, 43, 4, 82_000_000, "BiS-style ranged."));
		items.add(item(GearSlot.LEGS, "black d'hide chaps", ranged, 0, 0, 40, 0, 70, 0, 17, 0, 5_000, "Budget ranged."));
		items.add(item(GearSlot.LEGS, "masori chaps (f)", ranged, 0, 0, 80, 0, 80, 0, 27, 2, 57_000_000, "BiS-style ranged."));
		items.add(item(GearSlot.NECK, "necklace of anguish", ranged, 0, 0, 1, 0, 75, 0, 15, 5, 21_000_000, "Best practical ranged amulet."));
		items.add(item(GearSlot.CAPE, "ava's assembler", ranged, 0, 0, 1, 0, 70, 0, 8, 2, 0, "Vorkath cape upgrade."));
		items.add(item(GearSlot.HANDS, "barrows gloves", ranged, 0, 0, 1, 1, 1, 1, 12, 0, 130_000, "Quest glove fallback."));
		items.add(item(GearSlot.HANDS, "zaryte vambraces", ranged, 0, 0, 80, 0, 80, 0, 18, 2, 80_000_000, "High-end ranged gloves."));
		items.add(item(GearSlot.FEET, "pegasian boots", ranged, 0, 0, 75, 0, 75, 0, 12, 0, 35_000_000, "Ranged boots."));
		items.add(item(GearSlot.RING, "archers ring (i)", ranged, 0, 0, 1, 0, 70, 0, 8, 0, 4_000_000, "Ranged ring."));
		items.add(item(GearSlot.AMMUNITION, "dragon arrows / bolts", ranged, 0, 0, 1, 0, 70, 0, 0, 60, 2_000, "Ammo placeholder."));
	}

	private static void addMelee(List<GearItem> items)
	{
		Set<CombatStyle> melee = EnumSet.of(CombatStyle.STAB, CombatStyle.SLASH, CombatStyle.CRUSH);
		items.add(item(GearSlot.WEAPON, "dragon scimitar", melee, 60, 60, 1, 0, 0, 0, 67, 66, 60_000, "Budget melee."));
		items.add(item(GearSlot.WEAPON, "abyssal whip", melee, 70, 70, 1, 0, 0, 0, 82, 82, 1_700_000, "Midgame melee."));
		items.add(item(GearSlot.WEAPON, "fang / lance equivalent", melee, 82, 75, 1, 0, 0, 0, 105, 103, 18_000_000, "Bossing melee option."));
		items.add(item(GearSlot.WEAPON, "noxious halberd", melee, 80, 80, 1, 0, 0, 0, 132, 142, 50_000_000, "High-end melee inspiration."));
		items.add(item(GearSlot.HEAD, "nezzy helm", melee, 1, 1, 45, 0, 0, 0, 0, 3, 50_000, "Budget melee."));
		items.add(item(GearSlot.HEAD, "faceguard", melee, 1, 1, 70, 0, 0, 0, 0, 6, 20_000_000, "Strong melee helm."));
		items.add(item(GearSlot.BODY, "fighter torso", melee, 1, 1, 40, 0, 0, 0, 0, 4, 0, "Free melee body."));
		items.add(item(GearSlot.BODY, "bandos chestplate", melee, 1, 1, 65, 0, 0, 0, 0, 4, 35_000_000, "Strength body."));
		items.add(item(GearSlot.LEGS, "obsidian platelegs", melee, 1, 1, 60, 0, 0, 0, 0, 1, 900_000, "Budget legs."));
		items.add(item(GearSlot.LEGS, "bandos tassets", melee, 1, 1, 65, 0, 0, 0, 0, 2, 22_000_000, "Strength legs."));
		items.add(item(GearSlot.NECK, "amulet of fury", melee, 1, 1, 1, 0, 0, 0, 10, 8, 2_000_000, "All-round amulet."));
		items.add(item(GearSlot.NECK, "amulet of rancour", melee, 80, 80, 1, 0, 0, 0, 25, 12, 60_000_000, "High-end melee amulet."));
		items.add(item(GearSlot.CAPE, "fire cape", melee, 1, 1, 1, 0, 0, 0, 1, 4, 0, "Fight Caves cape."));
		items.add(item(GearSlot.CAPE, "infernal cape", melee, 1, 1, 1, 0, 0, 0, 4, 8, 0, "Best melee cape."));
		items.add(item(GearSlot.HANDS, "barrows gloves", melee, 1, 1, 1, 1, 1, 1, 12, 12, 130_000, "Quest gloves."));
		items.add(item(GearSlot.HANDS, "ferocious gloves", melee, 1, 1, 80, 0, 0, 0, 16, 14, 13_000_000, "Melee gloves."));
		items.add(item(GearSlot.FEET, "dragon boots", melee, 1, 60, 60, 0, 0, 0, 0, 4, 200_000, "Budget boots."));
		items.add(item(GearSlot.FEET, "primordial boots", melee, 75, 75, 75, 0, 0, 0, 2, 5, 35_000_000, "Melee boots."));
		items.add(item(GearSlot.RING, "berserker ring (i)", melee, 1, 1, 1, 0, 0, 0, 0, 8, 4_000_000, "Strength ring."));
	}

	private static GearItem item(GearSlot slot, String name, Set<CombatStyle> styles, int attackReq, int strengthReq, int defenceReq,
		int magicReq, int rangedReq, int prayerReq, int attackBonus, int strengthBonus, long price, String note)
	{
		return new GearItem(slot, name, styles, attackReq, strengthReq, defenceReq, magicReq, rangedReq, prayerReq, attackBonus, strengthBonus, price, note);
	}
}
