package kvj.shithead.frontend.cli;

import java.util.List;

import kvj.shithead.backend.Card;

/* package-private */ class CliGameUtil {
	/* package-private */ static String listRanks(List<Card> hand) {
		if (hand.isEmpty())
			return "[]";
		StringBuilder sb = new StringBuilder("[");
		for (Card card : hand)
			sb.append(card.getRank().toString()).append(", ");
		return sb.replace(sb.length() - 2, sb.length(), "]").toString();
	}
}
