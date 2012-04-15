package kvj.shithead.backend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Card {
	public enum Rank {
		TWO,
		THREE,
		FOUR,
		FIVE,
		SIX,
		SEVEN,
		EIGHT,
		NINE,
		TEN,
		JACK,
		QUEEN,
		KING,
		ACE;

		public static Rank getRankByText(String name) {
			Rank c;
			if (name.equals("2") || name.equalsIgnoreCase("TWO"))
				c = Rank.TWO;
			else if (name.equals("3") || name.equalsIgnoreCase("THREE"))
				c = Rank.THREE;
			else if (name.equals("4") || name.equalsIgnoreCase("FOUR"))
				c = Rank.FOUR;
			else if (name.equals("5") || name.equalsIgnoreCase("FIVE"))
				c = Rank.FIVE;
			else if (name.equals("6") || name.equalsIgnoreCase("SIX"))
				c = Rank.SIX;
			else if (name.equals("7") || name.equalsIgnoreCase("SEVEN"))
				c = Rank.SEVEN;
			else if (name.equals("8") || name.equalsIgnoreCase("EIGHT"))
				c = Rank.EIGHT;
			else if (name.equals("9") || name.equalsIgnoreCase("NINE"))
				c = Rank.NINE;
			else if (name.equals("10") || name.equalsIgnoreCase("TEN"))
				c = Rank.TEN;
			else if (name.equalsIgnoreCase("J") || name.equalsIgnoreCase("JACK"))
				c = Rank.JACK;
			else if (name.equalsIgnoreCase("Q") || name.equalsIgnoreCase("QUEEN"))
				c = Rank.QUEEN;
			else if (name.equalsIgnoreCase("K") || name.equalsIgnoreCase("KING"))
				c = Rank.KING;
			else if (name.equalsIgnoreCase("A") || name.equalsIgnoreCase("ACE"))
				c = Rank.ACE;
			else
				c = null;
			return c;
		}
	}

	public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }

	private static final Map<Suit, Map<Rank, Card>> cache;

	static {
		HashMap<Suit, Map<Rank, Card>> suits = new HashMap<Suit, Map<Rank, Card>>();
		for (Suit s : Suit.values()) {
			HashMap<Rank, Card> cards = new HashMap<Rank, Card>();
			for (Rank r : Rank.values())
				cards.put(r, new Card(s, r));
			suits.put(s, Collections.unmodifiableMap(cards));
		}
		cache = Collections.unmodifiableMap(suits);
	}

	public static Card valueOf(Suit suit, Rank rank) {
		if (suit == null || rank == null)
			return null;
		return cache.get(suit).get(rank);
	}

	private final Suit suit;
	private final Rank rank;

	private Card(Suit suit, Rank rank) {
		this.suit = suit;
		this.rank = rank;
	}

	public Suit getSuit() {
		return suit;
	}

	public Rank getRank() {
		return rank;
	}
}
