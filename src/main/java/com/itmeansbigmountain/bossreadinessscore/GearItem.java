package com.itmeansbigmountain.bossreadinessscore;

import java.util.EnumSet;
import java.util.Set;

public class GearItem
{
	private final GearSlot slot;
	private final int itemId;
	private final String name;
	private final Set<CombatStyle> styles;
	private final int attackReq;
	private final int strengthReq;
	private final int defenceReq;
	private final int magicReq;
	private final int rangedReq;
	private final int prayerReq;
	private final int attackBonus;
	private final int strengthBonus;
	private final long price;
	private final String note;
	private final String iconBase64;
	private final String wikiUrl;
	private final boolean twoHanded;

	public GearItem(GearSlot slot, String name, Set<CombatStyle> styles, int attackReq, int strengthReq, int defenceReq,
		int magicReq, int rangedReq, int prayerReq, int attackBonus, int strengthBonus, long price, String note)
	{
		this(slot, -1, name, styles, attackReq, strengthReq, defenceReq, magicReq, rangedReq, prayerReq, attackBonus, strengthBonus, price, note);
	}

	public GearItem(GearSlot slot, int itemId, String name, Set<CombatStyle> styles, int attackReq, int strengthReq, int defenceReq,
		int magicReq, int rangedReq, int prayerReq, int attackBonus, int strengthBonus, long price, String note)
	{
		this(slot, itemId, name, styles, attackReq, strengthReq, defenceReq, magicReq, rangedReq, prayerReq, attackBonus, strengthBonus, price, note, null);
	}

	public GearItem(GearSlot slot, int itemId, String name, Set<CombatStyle> styles, int attackReq, int strengthReq, int defenceReq,
		int magicReq, int rangedReq, int prayerReq, int attackBonus, int strengthBonus, long price, String note, String iconBase64)
	{
		this(slot, itemId, name, styles, attackReq, strengthReq, defenceReq, magicReq, rangedReq, prayerReq, attackBonus, strengthBonus, price, note, iconBase64, OsrsWikiApiClient.pageUrl(name));
	}

	public GearItem(GearSlot slot, int itemId, String name, Set<CombatStyle> styles, int attackReq, int strengthReq, int defenceReq,
		int magicReq, int rangedReq, int prayerReq, int attackBonus, int strengthBonus, long price, String note, String iconBase64, String wikiUrl)
	{
		this(slot, itemId, name, styles, attackReq, strengthReq, defenceReq, magicReq, rangedReq, prayerReq, attackBonus, strengthBonus, price, note, iconBase64, wikiUrl, isKnownTwoHanded(slot, name));
	}

	public GearItem(GearSlot slot, int itemId, String name, Set<CombatStyle> styles, int attackReq, int strengthReq, int defenceReq,
		int magicReq, int rangedReq, int prayerReq, int attackBonus, int strengthBonus, long price, String note, String iconBase64, String wikiUrl, boolean twoHanded)
	{
		this.slot = slot;
		this.itemId = itemId;
		this.name = name;
		this.styles = EnumSet.copyOf(styles);
		this.attackReq = attackReq;
		this.strengthReq = strengthReq;
		this.defenceReq = defenceReq;
		this.magicReq = magicReq;
		this.rangedReq = rangedReq;
		this.prayerReq = prayerReq;
		this.attackBonus = attackBonus;
		this.strengthBonus = strengthBonus;
		this.price = price;
		this.note = note;
		this.iconBase64 = iconBase64;
		this.wikiUrl = wikiUrl == null || wikiUrl.trim().isEmpty() ? OsrsWikiApiClient.pageUrl(name) : wikiUrl;
		this.twoHanded = twoHanded;
	}

	public GearSlot getSlot()
	{
		return slot;
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getName()
	{
		return name;
	}

	public Set<CombatStyle> getStyles()
	{
		return styles;
	}

	public int getAttackReq()
	{
		return attackReq;
	}

	public int getStrengthReq()
	{
		return strengthReq;
	}

	public int getDefenceReq()
	{
		return defenceReq;
	}

	public int getMagicReq()
	{
		return magicReq;
	}

	public int getRangedReq()
	{
		return rangedReq;
	}

	public int getPrayerReq()
	{
		return prayerReq;
	}

	public int getAttackBonus()
	{
		return attackBonus;
	}

	public int getStrengthBonus()
	{
		return strengthBonus;
	}

	public long getPrice()
	{
		return price;
	}

	public String getNote()
	{
		return note;
	}

	public String getIconBase64()
	{
		return iconBase64;
	}

	public String getWikiUrl()
	{
		return wikiUrl;
	}

	public boolean isTwoHanded()
	{
		return twoHanded;
	}

	private static boolean isKnownTwoHanded(GearSlot slot, String name)
	{
		if (slot != GearSlot.WEAPON || name == null)
		{
			return false;
		}
		String normalized = name.toLowerCase(java.util.Locale.ROOT);
		return normalized.contains("tumeken's shadow")
			|| normalized.contains("twisted bow")
			|| normalized.contains("bow of faerdhinen")
			|| normalized.contains("toxic blowpipe")
			|| normalized.contains("noxious halberd")
			|| normalized.contains("scythe")
			|| normalized.contains("halberd")
			|| normalized.contains("longbow")
			|| normalized.contains("shortbow");
	}

	public boolean supports(CombatStyle style)
	{
		if (style == CombatStyle.MELEE)
		{
			return styles.contains(CombatStyle.STAB) || styles.contains(CombatStyle.SLASH) || styles.contains(CombatStyle.CRUSH);
		}
		return styles.contains(style);
	}

	public boolean meetsRequirements(PlayerStats stats)
	{
		return stats.getAttack() >= attackReq
			&& stats.getStrength() >= strengthReq
			&& stats.getDefence() >= defenceReq
			&& stats.getMagic() >= magicReq
			&& stats.getRanged() >= rangedReq
			&& stats.getPrayer() >= prayerReq;
	}

	public int scoreValue()
	{
		return attackBonus + strengthBonus * 2;
	}
}
