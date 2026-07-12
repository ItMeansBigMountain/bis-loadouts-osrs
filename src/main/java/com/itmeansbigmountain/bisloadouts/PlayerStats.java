package com.itmeansbigmountain.bisloadouts;

public class PlayerStats
{
	private final int attack;
	private final int strength;
	private final int defence;
	private final int hitpoints;
	private final int magic;
	private final int ranged;
	private final int prayer;

	public PlayerStats(int attack, int strength, int defence, int hitpoints, int magic, int ranged, int prayer)
	{
		this.attack = attack;
		this.strength = strength;
		this.defence = defence;
		this.hitpoints = hitpoints;
		this.magic = magic;
		this.ranged = ranged;
		this.prayer = prayer;
	}

	public int getAttack()
	{
		return attack;
	}

	public int getStrength()
	{
		return strength;
	}

	public int getDefence()
	{
		return defence;
	}

	public int getHitpoints()
	{
		return hitpoints;
	}

	public int getMagic()
	{
		return magic;
	}

	public int getRanged()
	{
		return ranged;
	}

	public int getPrayer()
	{
		return prayer;
	}
}
