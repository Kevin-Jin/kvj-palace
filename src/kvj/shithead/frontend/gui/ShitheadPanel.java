package kvj.shithead.frontend.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.swing.JComponent;

import kvj.shithead.backend.Card;

public class ShitheadPanel extends JComponent {
	private static final long serialVersionUID = -6335150580109791737L;

	private static final int WIDTH = 1280, HEIGHT = 800;
	private static final int TABLE_DIAMETER = 800;

	private List<CardEntity> cards;
	private Set<CardEntity> drawOver;
	private CardEntity dragged;
	private GuiGame model;
	private ShitheadController input;
	private ImageCache cardImages;

	public ShitheadPanel(GuiGame model) {
		this.model = model;
		input = new ShitheadController(model);
		addMouseListener(input);
		addMouseMotionListener(input);
		setFocusable(true);
		requestFocusInWindow();

		cardImages = new ImageCache();
		cardImages.populate();
		cards = new ArrayList<CardEntity>();
		drawOver = new LinkedHashSet<CardEntity>();

		for (int i = 0; i < model.getPlayerCount(); i++)
			init((GuiPlayer) model.getPlayer(i));
		initCenter();
		cards.get(0).autoMove(4 * Math.PI, new Point(0, 0), 5);
	}

	private void init(GuiPlayer p) {
		double rot = Math.PI * 2 * (p.getPlayerId() - model.getLocalPlayerNumber()) / model.getPlayerCount();
		final int OPEN_HAND_SPACING = 2;
		final int CLOSED_HAND_SPACING = 4;
		//closest we can get without 5 players colliding
		final int DISTANCE_FROM_CENTER = 160;
		int i = 0;
		int left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * 3 / 2;
		for (Card c : p.getFaceDown())
			cards.add(make(c, rot, new Point(i++ * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER), false));
		i = 0;
		left += CLOSED_HAND_SPACING;
		for (Card c : p.getFaceUp())
			cards.add(make(c, rot, new Point(i++ * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER), true));

		boolean isLocal = p.getPlayerId() == model.getLocalPlayerNumber();
		if (p.isThinking()) {
			i = 0;
			left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * p.getHand().size() / 2;
			for (Card c : p.getHand())
				cards.add(make(c, rot, new Point(i++ * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1), isLocal));
		} else {
			i = 0;
			left = -(cardImages.getCardWidth() + CLOSED_HAND_SPACING * (p.getHand().size() - 1)) / 2;
			for (Card c : p.getHand())
				cards.add(make(c, rot, new Point(i++ * CLOSED_HAND_SPACING + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1), isLocal));
		}
	}

	private void initCenter() {
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
		for (Card card : model.getDeckCards())
			cards.add(make(card, 0, new Point(right - i++ * CLOSED_HAND_SPACING, -cardImages.getCardHeight() / 2), false));
		int left = -28;
		i = 0;
		for (Card card : model.getDiscardPile())
			cards.add(make(card, 0, new Point(i++ * CLOSED_HAND_SPACING + left, -cardImages.getCardHeight() / 2), true));
	}

	public void updateState(double tDelta) {
		boolean findCardToDrag = false;
		if (dragged != null) {
			if (!input.mouseDown()) {
				//TODO: move dragged back to original location,
				//before removing it from drawOver
				drawOver.remove(dragged);
				dragged = null;
			} else {
				dragged.manualMove(input.getCursor());
			}
		} else {
			if (input.mouseDown())
				findCardToDrag = true;
		}
		//go in reverse so card that was painted last
		//will be the first candidate for manipulation
		for (ListIterator<CardEntity> iter = cards.listIterator(cards.size()); iter.hasPrevious(); ) {
			CardEntity card = iter.previous();
			card.update(tDelta);
			if (findCardToDrag && card.isPointInCard(input.getCursor(), cardImages.getCardWidth(), cardImages.getCardHeight())) {
				dragged = card;
				drawOver.add(dragged);
				findCardToDrag = false;
			}
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
	}

	private CardEntity make(Card c, double rotation, Point2D playerBoxPosition, boolean show) {
		AffineTransform t = AffineTransform.getTranslateInstance(WIDTH / 2, HEIGHT / 2);
		t.concatenate(AffineTransform.getRotateInstance(rotation));
		Point2D pos = new Point2D.Double();
		AffineTransform.getTranslateInstance(cardImages.getCardWidth() / 2, cardImages.getCardHeight() / 2).transform(playerBoxPosition, pos);
		t.transform(pos, pos);
		return new CardEntity(c, show, pos, rotation);
	}

	private void draw(CardEntity card, Graphics2D g2d) {
		g2d.drawImage(card.show() ? cardImages.getFront(card.getValue().getSuit(), card.getValue().getRank()) : cardImages.getBack(), card.getTransform(cardImages.getCardWidth(), cardImages.getCardHeight()), null);
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawOval(WIDTH / 2 - TABLE_DIAMETER / 2, HEIGHT / 2 - TABLE_DIAMETER / 2, TABLE_DIAMETER, TABLE_DIAMETER);
		for (CardEntity card : cards)
			if (!drawOver.contains(card))
				draw(card, g2d);
		for (CardEntity card : drawOver)
			draw(card, g2d);
	}
}
