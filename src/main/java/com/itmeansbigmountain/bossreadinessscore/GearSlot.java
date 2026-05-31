package com.itmeansbigmountain.bossreadinessscore;

public enum GearSlot
{
	HEAD("Head"),
	CAPE("Cape"),
	NECK("Neck"),
	WEAPON("Weapon"),
	SHIELD("Shield"),
	BODY("Body"),
	LEGS("Legs"),
	HANDS("Hands"),
	FEET("Feet"),
	RING("Ring"),
	AMMUNITION("Ammo");

	private final String label;

	GearSlot(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
