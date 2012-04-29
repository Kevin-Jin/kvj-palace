package kvj.shithead.frontend.gui;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import kvj.shithead.backend.Card;

public class ImageCache {
	private BufferedImage back;
	private Map<Card.Suit, Map<Card.Rank, BufferedImage>> fronts;
	private int cardWidth, cardHeight;

	public ImageCache() {
		fronts = new HashMap<Card.Suit, Map<Card.Rank, BufferedImage>>();
	}

	private BufferedImage getPngResource(String name) throws IOException {
		return ImageIO.read(new BufferedInputStream(getClass().getResourceAsStream("/resources/" + name + ".png")));
	}

	public void populate() {
		try {
			back = getPngResource("back-blue-75-1");
			for (Card.Suit suit : Card.Suit.values()) {
				Map<Card.Rank, BufferedImage> suitFronts = new HashMap<Card.Rank, BufferedImage>();
				for (Card.Rank rank : Card.Rank.values())
					suitFronts.put(rank, getPngResource(suit.toString().toLowerCase() + "-" + rank.getShorthandNotation().toLowerCase() + "-75"));
				fronts.put(suit, suitFronts);
			}
			cardWidth = back.getWidth();
			cardHeight = back.getHeight();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BufferedImage getBack() {
		return back;
	}

	public BufferedImage getFront(Card.Suit suit, Card.Rank rank) {
		return fronts.get(suit).get(rank);
	}

	public int getCardWidth() {
		return cardWidth;
	}

	public int getCardHeight() {
		return cardHeight;
	}
}
