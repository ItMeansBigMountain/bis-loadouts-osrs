package com.itmeansbigmountain.bisloadouts;

import java.util.Locale;

public enum ElementalType
{
	NONE,
	AIR,
	WATER,
	EARTH,
	FIRE;

	public static ElementalType fromApi(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return NONE;
		}
		try
		{
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ignored)
		{
			return NONE;
		}
	}

	public String displayName()
	{
		if (this == NONE)
		{
			return "";
		}
		String lower = name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
