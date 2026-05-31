package com.itmeansbigmountain.bossreadinessscore;

public enum BossProfile
{
	GENERAL_PVM("General PvM", 85, 60, 60, 60, 60, 60),
	SCURRIUS("Scurrius", 70, 45, 45, 40, 35, 35),
	GIANT_MOLE("Giant Mole", 75, 60, 60, 50, 45, 45),
	BARROWS("Barrows", 80, 65, 65, 55, 55, 50),
	VORKATH("Vorkath", 95, 80, 75, 70, 75, 70),
	ZULRAH("Zulrah", 95, 75, 70, 70, 80, 75),
	FIGHT_CAVES("Fight Caves", 85, 70, 65, 60, 70, 60);

	private final String label;
	private final int targetCombat;
	private final int targetAttack;
	private final int targetStrength;
	private final int targetDefence;
	private final int targetRanged;
	private final int targetMagic;

	BossProfile(String label, int targetCombat, int targetAttack, int targetStrength, int targetDefence, int targetRanged, int targetMagic)
	{
		this.label = label;
		this.targetCombat = targetCombat;
		this.targetAttack = targetAttack;
		this.targetStrength = targetStrength;
		this.targetDefence = targetDefence;
		this.targetRanged = targetRanged;
		this.targetMagic = targetMagic;
	}

	public String getLabel()
	{
		return label;
	}

	public int getTargetCombat()
	{
		return targetCombat;
	}

	public int getTargetAttack()
	{
		return targetAttack;
	}

	public int getTargetStrength()
	{
		return targetStrength;
	}

	public int getTargetDefence()
	{
		return targetDefence;
	}

	public int getTargetRanged()
	{
		return targetRanged;
	}

	public int getTargetMagic()
	{
		return targetMagic;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
