package com.itmeansbigmountain.bisloadouts;

public enum BudgetTier
{
	BUDGET("Budget", 1_000_000L),
	MIDGAME("Midgame", 20_000_000L),
	RICH("Rich", 250_000_000L),
	NO_LIMIT("No limit", Long.MAX_VALUE);

	private final String label;
	private final long maxItemPrice;

	BudgetTier(String label, long maxItemPrice)
	{
		this.label = label;
		this.maxItemPrice = maxItemPrice;
	}

	public long getMaxItemPrice()
	{
		return maxItemPrice;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
