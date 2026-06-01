package com.itmeansbigmountain.bossreadinessscore;

public enum CombatStyle
{
	AUTO("Auto"),
	MELEE("Melee"),
	STAB("Stab"),
	SLASH("Slash"),
	CRUSH("Crush"),
	RANGED("Ranged"),
	MAGIC("Magic");

	private final String label;

	CombatStyle(String label)
	{
		this.label = label;
	}

	public boolean isMelee()
	{
		return this == MELEE || this == STAB || this == SLASH || this == CRUSH;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
