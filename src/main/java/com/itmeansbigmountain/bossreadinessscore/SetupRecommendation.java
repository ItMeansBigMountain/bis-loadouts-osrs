package com.itmeansbigmountain.bossreadinessscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SetupRecommendation
{
	private final String bossName;
	private final CombatStyle style;
	private final Map<GearSlot, GearItem> items;
	private final Map<GearSlot, List<GearItem>> slotAlternatives;
	private final double estimatedDps;
	private final double hitChance;
	private final int maxHit;
	private final int readinessScore;
	private final List<String> warnings;
	private final List<SetupRecommendation> alternatives;

	public SetupRecommendation(String bossName, CombatStyle style, Map<GearSlot, GearItem> items, double estimatedDps,
		double hitChance, int maxHit, int readinessScore, List<String> warnings, List<SetupRecommendation> alternatives)
	{
		this(bossName, style, items, Collections.emptyMap(), estimatedDps, hitChance, maxHit, readinessScore, warnings, alternatives);
	}

	public SetupRecommendation(String bossName, CombatStyle style, Map<GearSlot, GearItem> items, Map<GearSlot, List<GearItem>> slotAlternatives,
		double estimatedDps, double hitChance, int maxHit, int readinessScore, List<String> warnings, List<SetupRecommendation> alternatives)
	{
		this.bossName = bossName;
		this.style = style;
		this.items = Collections.unmodifiableMap(new EnumMap<>(items));
		Map<GearSlot, List<GearItem>> copiedAlternatives = new EnumMap<>(GearSlot.class);
		for (Map.Entry<GearSlot, List<GearItem>> entry : slotAlternatives.entrySet())
		{
			copiedAlternatives.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
		}
		this.slotAlternatives = Collections.unmodifiableMap(copiedAlternatives);
		this.estimatedDps = estimatedDps;
		this.hitChance = hitChance;
		this.maxHit = maxHit;
		this.readinessScore = readinessScore;
		this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
		this.alternatives = Collections.unmodifiableList(new ArrayList<>(alternatives));
	}

	public String getBossName() { return bossName; }
	public CombatStyle getStyle() { return style; }
	public Map<GearSlot, GearItem> getItems() { return items; }
	public Map<GearSlot, List<GearItem>> getSlotAlternatives() { return slotAlternatives; }
	public List<GearItem> getAlternativesForSlot(GearSlot slot) { return slotAlternatives.getOrDefault(slot, Collections.emptyList()); }
	public GearItem getItem(GearSlot slot) { return items.get(slot); }
	public double getEstimatedDps() { return estimatedDps; }
	public double getHitChance() { return hitChance; }
	public int getMaxHit() { return maxHit; }
	public int getReadinessScore() { return readinessScore; }
	public List<String> getWarnings() { return warnings; }
	public List<SetupRecommendation> getAlternatives() { return alternatives; }
}
