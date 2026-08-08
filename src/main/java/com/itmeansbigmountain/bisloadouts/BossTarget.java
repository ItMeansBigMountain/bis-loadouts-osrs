package com.itmeansbigmountain.bisloadouts;

import java.util.Collections;
import java.util.List;

public class BossTarget
{
	private final String label;
	private final int id;
	private final int targetCombat;
	private final int targetAttack;
	private final int targetStrength;
	private final int targetDefence;
	private final int targetRanged;
	private final int targetMagic;
	private final int hitpoints;
	private final int defStab;
	private final int defSlash;
	private final int defCrush;
	private final int defMagic;
	private final int defRanged;
	private final ElementalType elementalWeakness;
	private final int elementalWeaknessPercent;
	private final List<String> attributes;
	private final String wikiUrl;
	private final String source;

	public BossTarget(String label, int id, int targetCombat, int targetAttack, int targetStrength, int targetDefence,
		int targetRanged, int targetMagic, int hitpoints, int defStab, int defSlash, int defCrush, int defMagic,
		int defRanged, List<String> attributes, String wikiUrl, String source)
	{
		this(label, id, targetCombat, targetAttack, targetStrength, targetDefence, targetRanged, targetMagic, hitpoints,
			defStab, defSlash, defCrush, defMagic, defRanged, ElementalType.NONE, 0, attributes, wikiUrl, source);
	}

	public BossTarget(String label, int id, int targetCombat, int targetAttack, int targetStrength, int targetDefence,
		int targetRanged, int targetMagic, int hitpoints, int defStab, int defSlash, int defCrush, int defMagic,
		int defRanged, ElementalType elementalWeakness, int elementalWeaknessPercent, List<String> attributes,
		String wikiUrl, String source)
	{
		this.label = label;
		this.id = id;
		this.targetCombat = Math.max(1, targetCombat);
		this.targetAttack = Math.max(1, targetAttack);
		this.targetStrength = Math.max(1, targetStrength);
		this.targetDefence = Math.max(1, targetDefence);
		this.targetRanged = Math.max(1, targetRanged);
		this.targetMagic = Math.max(1, targetMagic);
		this.hitpoints = Math.max(1, hitpoints);
		this.defStab = defStab;
		this.defSlash = defSlash;
		this.defCrush = defCrush;
		this.defMagic = defMagic;
		this.defRanged = defRanged;
		this.elementalWeakness = elementalWeakness == null ? ElementalType.NONE : elementalWeakness;
		this.elementalWeaknessPercent = this.elementalWeakness == ElementalType.NONE ? 0 : Math.max(0, elementalWeaknessPercent);
		this.attributes = attributes == null ? Collections.emptyList() : Collections.unmodifiableList(attributes);
		this.wikiUrl = wikiUrl;
		this.source = source;
	}

	public static BossTarget fromProfile(BossProfile profile)
	{
		return new BossTarget(profile.getLabel(), -1, profile.getTargetCombat(), profile.getTargetAttack(), profile.getTargetStrength(),
			profile.getTargetDefence(), profile.getTargetRanged(), profile.getTargetMagic(), 100, 0, 0, 0, 0, 0,
			Collections.emptyList(), OsrsWikiApiClient.pageUrl(profile.getLabel()), "local preset");
	}

	public String getLabel() { return label; }
	public int getId() { return id; }
	public int getTargetCombat() { return targetCombat; }
	public int getTargetAttack() { return targetAttack; }
	public int getTargetStrength() { return targetStrength; }
	public int getTargetDefence() { return targetDefence; }
	public int getTargetRanged() { return targetRanged; }
	public int getTargetMagic() { return targetMagic; }
	public int getHitpoints() { return hitpoints; }
	public int getDefStab() { return defStab; }
	public int getDefSlash() { return defSlash; }
	public int getDefCrush() { return defCrush; }
	public int getDefMagic() { return defMagic; }
	public int getDefRanged() { return defRanged; }
	public ElementalType getElementalWeakness() { return elementalWeakness; }
	public int getElementalWeaknessPercent() { return elementalWeaknessPercent; }
	public boolean hasElementalWeakness() { return elementalWeakness != ElementalType.NONE && elementalWeaknessPercent > 0; }
	public List<String> getAttributes() { return attributes; }
	public String getWikiUrl() { return wikiUrl; }
	public String getSource() { return source; }
}
