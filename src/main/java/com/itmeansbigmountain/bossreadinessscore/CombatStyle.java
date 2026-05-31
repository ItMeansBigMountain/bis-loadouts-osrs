package com.itmeansbigmountain.bossreadinessscore;

public enum CombatStyle
{
	AUTO("Auto"),
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

	@Override
	public String toString()
	{
		return label;
	}
}
