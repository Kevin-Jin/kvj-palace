package kvj.shithead.frontend.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JComponent;

import kvj.shithead.backend.Card;

public class ShitheadPanel extends JComponent {
	private static final long serialVersionUID = -6335150580109791737L;

	private static final int WIDTH = 1280, HEIGHT = 800;
	private static final int TABLE_DIAMETER = 800;

	private List<CardEntity> cards;
	private Set<CardEntity> tempDrawOver;
	private CardEntity dragged;
	private GuiGame model;
	private ShitheadController input;
	private ImageCache cardImages;
	private NavigableMap<Integer, MutableRange> handIndices;

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
		tempDrawOver = new LinkedHashSet<CardEntity>();
		handIndices = new TreeMap<Integer, MutableRange>();

		for (int i = 0; i < model.getPlayerCount(); i++)
			init((GuiPlayer) model.getPlayer(i));
		initCenter();
	}

	private CardEntity make(Card c, double rotation, Point2D playerBoxPosition, boolean show) {
		AffineTransform t = AffineTransform.getTranslateInstance(WIDTH / 2, HEIGHT / 2);
		t.concatenate(AffineTransform.getRotateInstance(rotation));
		Point2D pos = new Point2D.Double();
		AffineTransform.getTranslateInstance(cardImages.getCardWidth() / 2, cardImages.getCardHeight() / 2).transform(playerBoxPosition, pos);
		t.transform(pos, pos);
		return new CardEntity(c, show, pos, rotation);
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

		handIndices.put(Integer.valueOf(p.getPlayerId()), new MutableRange(cards.size(), p.getHand().size()));

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

	private Rectangle getDiscardPileBounds() {
		//TODO: this needs to share code with initCenter() and make(...)
		final int CLOSED_HAND_SPACING = 2;
		int left = -28;
		return new Rectangle(WIDTH / 2 + left, HEIGHT / 2 - cardImages.getCardHeight() / 2, model.getDiscardPile().size() * CLOSED_HAND_SPACING + cardImages.getCardWidth(), cardImages.getCardHeight());
	}

	private Point getDiscardPileLocation() {
		//TODO: this needs to share code with initCenter() and make(...)
		final int CLOSED_HAND_SPACING = 2;
		int left = -28;
		return new Point(WIDTH / 2 + left + model.getDiscardPile().size() * CLOSED_HAND_SPACING + cardImages.getCardWidth() / 2, HEIGHT / 2);
	}

	public void updateState(double tDelta) {
		boolean findCardToDrag = false;
		if (dragged != null) {
			if (!input.mouseDown()) {
				if (getDiscardPileBounds().contains(input.getCursor())) {
					//assert dragged is from current player's face down, face up, or hand
					dragged.mark(getDiscardPileLocation(), 0, 1);
					//permanantly paint dragged last so it's on top of the discard pile
					cards.remove(dragged);
					cards.add(dragged);
					boolean fromHand = model.getPlayer(model.getCurrentPlayer()).getHand().contains(dragged.getValue());
					if (fromHand)
						handIndices.get(Integer.valueOf(model.getCurrentPlayer())).decrementLength();
					//if we take dragged from our face up or face down, also decrement
					//our own hand index
					for (MutableRange i : handIndices.tailMap(Integer.valueOf(model.getCurrentPlayer()), !fromHand).values())
						i.decrementStart();
					//TODO: update model/player
				}
				dragged.reset();
				dragged = null;
			} else {
				dragged.manualMove(input.getCursor());
			}
		} else {
			if (input.mouseDown())
				findCardToDrag = true;
		}
		for (Iterator<CardEntity> iter = tempDrawOver.iterator(); iter.hasNext(); )
			if (iter.next().stopTempDrawingOver())
				iter.remove();
		//go in reverse so card that was painted last
		//will be the first candidate for manipulation
		for (ListIterator<CardEntity> iter = cards.listIterator(cards.size()); iter.hasPrevious(); ) {
			CardEntity card = iter.previous();
			card.update(tDelta);
			if (findCardToDrag && card.isPointInCard(input.getCursor(), cardImages.getCardWidth(), cardImages.getCardHeight())) {
				card.mark();
				dragged = card;
				tempDrawOver.add(dragged);
				findCardToDrag = false;
			}
		}
	}

	//TODO: have GuiPlayer.cardsPickedUp(TurnContext cx) call this if !cx.pickedUp.isEmpty()
	public void playerPickedUpCards(GuiPlayer p) {
		MutableRange handRange = handIndices.get(Integer.valueOf(p.getPlayerId()));
		List<Card> hand = p.getHand();
		List<CardEntity> moved = new ArrayList<CardEntity>(hand.size());
		//all cards in updated hand should have come from old hand and draw deck or discard pile
		//skip all the cards before our hand, and cards from draw deck and discard pile should
		//come after all hands
		for (ListIterator<CardEntity> iter = cards.listIterator(handRange.getStart()); iter.hasNext() && moved.size() < hand.size(); ) {
			//assert ent is in deck or discard pile, so we don't need to update handIndices
			CardEntity ent = iter.next();
			if (hand.contains(ent.getValue())) {
				iter.remove();
				moved.add(ent);
			}
		}
		assert moved.size() == hand.size();
		cards.addAll(handRange.getStart(), moved);
		for (MutableRange i : handIndices.tailMap(Integer.valueOf(p.getPlayerId()), false).values())
			i.addToStart(moved.size() - handRange.getLength());
		handRange.setLength(moved.size());

		//TODO: this needs to share code with init(GuiPlayer p)
		double rot = Math.PI * 2 * (p.getPlayerId() - model.getLocalPlayerNumber()) / model.getPlayerCount();
		final int OPEN_HAND_SPACING = 2;
		final int CLOSED_HAND_SPACING = 4;
		//closest we can get without 5 players colliding
		final int DISTANCE_FROM_CENTER = 160;
		int i;
		int left;
		if (p.isThinking()) {
			i = 0;
			left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * p.getHand().size() / 2;
			for (i = 0; i < handRange.getLength(); i++)
				cards.get(handRange.getStart() + i).autoMove(rot, new Point(i * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1), 1);
		} else {
			i = 0;
			left = -(cardImages.getCardWidth() + CLOSED_HAND_SPACING * (p.getHand().size() - 1)) / 2;
			for (i = 0; i < handRange.getLength(); i++)
				cards.get(handRange.getStart() + i).autoMove(rot, new Point(i * CLOSED_HAND_SPACING + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1), 1);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(WIDTH, HEIGHT);
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
			if (!tempDrawOver.contains(card))
				draw(card, g2d);
		for (CardEntity card : tempDrawOver)
			draw(card, g2d);
	}
}
