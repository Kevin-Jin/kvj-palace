package kvj.shithead.backend;

import java.util.List;

public class PacketMaker {
	public static final byte //op codes
		ADD_PLAYER = 0x01,
		DECK = 0x02,
		SELECT_CARD = 0x03
	;

	public static byte[] serializedDeck(List<Card> cards) {
		byte[] message = new byte[1 + cards.size()];
		assert message.length == 53;
		int i = 0;
		message[i++] = PacketMaker.DECK;
		for (Card card : cards)
			message[i++] = Card.serialize(card);
		return message;
	}

	public static byte[] selectCard(Card selected) {
		return new byte[] { SELECT_CARD, Card.serialize(selected) };
	}
}
