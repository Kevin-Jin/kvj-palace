package kvj.shithead.frontend.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import javax.swing.JComponent;

import kvj.shithead.backend.Card;

public class ShitheadPanel extends JComponent {
	private static final long serialVersionUID = -6335150580109791737L;

	private static final int WIDTH = 1280, HEIGHT = 800;
	private static final int TABLE_DIAMETER = 800;

	private GuiGame model;
	private ShitheadController input;
	private ImageCache cardImages;

	public ShitheadPanel(GuiGame model) {
		this.model = model;
		input =  new ShitheadController(model);
		addMouseListener(input);
		addMouseMotionListener(input);
		setFocusable(true);
		requestFocusInWindow();

		cardImages = new ImageCache();
		cardImages.populate();
	}

	public void updateState() {
		
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
	}

	private void drawPlayerBox(Graphics2D g2d, GuiPlayer p) {
		final int OPEN_HAND_SPACING = 2;
		final int CLOSED_HAND_SPACING = 4;
		//closest we can get without 5 players colliding
		final int DISTANCE_FROM_CENTER = 160;

		int i = 0;
		int left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * 3 / 2;
		for (i = p.getFaceDown().size() - 1; i >= 0; i--)
			g2d.drawImage(cardImages.getBack(), AffineTransform.getTranslateInstance(i * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER), null);
		i = 0;
		left += CLOSED_HAND_SPACING;
		for (Card c : p.getFaceUp())
			g2d.drawImage(cardImages.getFront(c.getSuit(), c.getRank()), AffineTransform.getTranslateInstance(i++ * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER), null);

		boolean isLocal = p.getPlayerId() == model.getLocalPlayerNumber();
		if (p.isThinking()) {
			i = 0;
			left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * p.getHand().size() / 2;
			for (Card c : p.getHand())
				g2d.drawImage(isLocal ? cardImages.getFront(c.getSuit(), c.getRank()) : cardImages.getBack(), AffineTransform.getTranslateInstance(i++ * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1), null);
		} else {
			i = 0;
			left = -(cardImages.getCardWidth() + CLOSED_HAND_SPACING * (p.getHand().size() - 1)) / 2;
			for (Card c : p.getHand())
				g2d.drawImage(isLocal ? cardImages.getFront(c.getSuit(), c.getRank()) : cardImages.getBack(), AffineTransform.getTranslateInstance(i++ * CLOSED_HAND_SPACING + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1), null);
		}
	}

	private void drawCenter(Graphics2D g2d) {
		final int CLOSED_HAND_SPACING = 2;
		int i = 0;
		//the draw pile will never get bigger, but the discard pile
		//can get over 50 cards tall. this is the farthest left we
		//can bring the draw pile without it hitting a player (3
		//players after dealing) and with 5 players, the discard
		//pile can get to 52 cards without hitting a player and
		//the draw pile will start small enough that it doesn't
		//hit anyone after dealing
		int right = -cardImages.getCardWidth() - 30;
		for (i = 0; i < model.remainingDrawCards(); i++)
			g2d.drawImage(cardImages.getBack(), AffineTransform.getTranslateInstance(right - i * CLOSED_HAND_SPACING, -cardImages.getCardHeight() / 2), null);
		int left = -28;
		i = 0;
		for (Card card : model.getDiscardPile())
			g2d.drawImage(cardImages.getFront(card.getSuit(), card.getRank()), AffineTransform.getTranslateInstance(i++ * CLOSED_HAND_SPACING + left, -cardImages.getCardHeight() / 2), null);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawOval(WIDTH / 2 - TABLE_DIAMETER / 2, HEIGHT / 2 - TABLE_DIAMETER / 2, TABLE_DIAMETER, TABLE_DIAMETER);
		for (int i = 0; i < model.getPlayerCount(); i++) {
			AffineTransform old = g2d.getTransform();
			AffineTransform t = AffineTransform.getTranslateInstance(WIDTH / 2, HEIGHT / 2);
			t.concatenate(AffineTransform.getRotateInstance(Math.PI * 2 * (i - model.getLocalPlayerNumber()) / model.getPlayerCount()));
			g2d.setTransform(t);
			drawPlayerBox(g2d, (GuiPlayer) model.getPlayer(i));
			g2d.setTransform(old);
		}
		AffineTransform old = g2d.getTransform();
		AffineTransform t = AffineTransform.getTranslateInstance(WIDTH / 2, HEIGHT / 2);
		g2d.setTransform(t);
		drawCenter(g2d);
		g2d.setTransform(old);
	}
}
