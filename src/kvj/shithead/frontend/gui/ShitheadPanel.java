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
import java.util.Set;

import javax.swing.JComponent;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;

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
	private PlayerCardsRange[] playerIndices;

	public ShitheadPanel(GuiGame model) {
		this.model = model;
		input = new ShitheadController();
		addMouseListener(input);
		addMouseMotionListener(input);
		setFocusable(true);
		requestFocusInWindow();

		cardImages = new ImageCache();
		cardImages.populate();
		cards = new ArrayList<CardEntity>();
		tempDrawOver = new LinkedHashSet<CardEntity>();
		playerIndices = new PlayerCardsRange[model.getPlayerCount()];
		for (int i = 0; i < playerIndices.length; i++)
			playerIndices[i] = new PlayerCardsRange();
	}

	private Point2D transform(double rotation, Point2D pt) {
		AffineTransform t = AffineTransform.getTranslateInstance(WIDTH / 2, HEIGHT / 2);
		t.concatenate(AffineTransform.getRotateInstance(rotation));
		t.concatenate(AffineTransform.getTranslateInstance(cardImages.getCardWidth() / 2, cardImages.getCardHeight() / 2));
		Point2D pos = new Point2D.Double();
		t.transform(pt, pos);
		return pos;
	}

	public void makeDrawDeckEntities() {
		final int CLOSED_HAND_SPACING = 2;
		int i = 0;
		int right = -cardImages.getCardWidth() - 30;
		for (Card card : model.getDeckCards())
			cards.add(new CardEntity(card, false, transform(0, new Point(right - i++ * CLOSED_HAND_SPACING, -cardImages.getCardHeight() / 2)), 0));
	}

	private Rectangle getDiscardPileBounds() {
		final int CLOSED_HAND_SPACING = 2;
		int left = -28;
		return new Rectangle(WIDTH / 2 + left, HEIGHT / 2 - cardImages.getCardHeight() / 2, model.getDiscardPile().size() * CLOSED_HAND_SPACING + cardImages.getCardWidth(), cardImages.getCardHeight());
	}

	private Point getDiscardPileLocation() {
		final int CLOSED_HAND_SPACING = 2;
		int left = -28;
		return new Point(WIDTH / 2 + left + model.getDiscardPile().size() * CLOSED_HAND_SPACING + cardImages.getCardWidth() / 2, HEIGHT / 2);
	}

	private void removeCardFromPlayerAndPutOnDiscardPile(TurnContext cx, Player p, CardEntity card) {
		//permanantly paint dragged last so it's on top of the discard pile
		cards.remove(card);
		cards.add(card);

		if (cx.currentPlayable == p.getHand())
			playerIndices[p.getPlayerId()].handLengthened(-1);
		else if (cx.currentPlayable == p.getFaceUp())
			playerIndices[p.getPlayerId()].faceUpLengthened(-1);
		else if (cx.currentPlayable == p.getFaceDown())
			playerIndices[p.getPlayerId()].faceDownLengthened(-1);
		for (int i = p.getPlayerId() + 1; i < playerIndices.length; i++)
			playerIndices[i].shifted(-1);
	}

	public void updateState(double tDelta) {
		TurnContext cx = model.getCurrentTurnContext();
		boolean localPlayer = cx != null && (model.getCurrentPlayer() == model.getLocalPlayerNumber());
		boolean findCardToDrag = false;
		if (dragged != null) {
			if (!input.mouseDown() || !localPlayer) {
				if (localPlayer && (cx.blind || getDiscardPileBounds().contains(input.getCursor()))) {
					GuiLocalPlayer p = (GuiLocalPlayer) model.getPlayer(model.getCurrentPlayer());
					if (cx.g.isMoveLegal(dragged.getValue())) {
						//assert dragged is from current player's face down, face up, or hand
						dragged.mark(getDiscardPileLocation(), 0, 1);
						removeCardFromPlayerAndPutOnDiscardPile(cx, p, dragged);
						p.cardChosen(dragged.getValue());
					} else if (cx.blind) {
						p.cardChosen(dragged.getValue());
					} else {
						drawHint("You may not put a " + dragged.getValue().getRank() + " on top of a " + cx.g.getTopCardRank() + ".");
					}
				}
				dragged.reset();
				dragged = null;
			} else {
				dragged.manualMove(input.getCursor());
			}
		} else {
			if (input.mouseDown() && localPlayer)
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
				if (cx.currentPlayable.contains(card.getValue())) {
					if (!cx.blind)
						card.mark();
					else
						card.mark(getDiscardPileLocation(), 0, 1);
					dragged = card;
					tempDrawOver.add(dragged);
					findCardToDrag = false;
				}
			}
		}
	}

	private void repositionFaceDown(Player p) {
		PlayerCardsRange curCardRanges = playerIndices[p.getPlayerId()];
		List<Card> faceDown = p.getFaceDown();
		List<CardEntity> moved = new ArrayList<CardEntity>(faceDown.size());
		//All cards in updated face down should have come from old face down or draw deck
		//so we don't have to worry about taking cards from other players and having to
		//update their PlayerCardsRange.
		//Since we assert that updated face down has cards only from old face down or draw deck,
		//we can skip all the cards before our face down since our own face down comes after them,
		//and cards from draw deck should come after all face downs.
		for (ListIterator<CardEntity> iter = cards.listIterator(curCardRanges.getFaceDownStart()); iter.hasNext() && moved.size() < faceDown.size(); ) {
			CardEntity ent = iter.next();
			if (faceDown.contains(ent.getValue())) {
				iter.remove();
				moved.add(ent);
			}
		}
		assert moved.size() == faceDown.size();
		int delta = moved.size() - curCardRanges.getFaceDownLength();
		cards.addAll(curCardRanges.getFaceDownStart(), moved);
		curCardRanges.faceDownLengthened(delta);
		for (int i = p.getPlayerId() + 1; i < playerIndices.length; i++)
			playerIndices[i].shifted(delta);

		double rot = Math.PI * 2 * (p.getPlayerId() - model.getLocalPlayerNumber()) / model.getPlayerCount();
		final int OPEN_HAND_SPACING = 2;
		//closest we can get without 5 players colliding
		final int DISTANCE_FROM_CENTER = 160;
		int i = 0;
		int left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * 3 / 2;
		for (i = 0; i < curCardRanges.getFaceDownLength(); i++) {
			CardEntity c = cards.get(curCardRanges.getFaceDownStart() + i);
			c.autoMove(rot, transform(rot, new Point(i * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER)), 1);
			c.setShow(false);
		}
	}

	private void repositionFaceUp(Player p) {
		PlayerCardsRange curCardRanges = playerIndices[p.getPlayerId()];
		List<Card> faceUp = p.getFaceUp();
		List<CardEntity> moved = new ArrayList<CardEntity>(faceUp.size());
		//All cards in updated face up should have come from old face up or draw deck
		//so we don't have to worry about taking cards from other players and having to
		//update their PlayerCardsRange.
		//Since we assert that updated face up has cards only from old face up or draw deck,
		//we can skip all the cards before our face up since our own face up comes after them,
		//and cards from draw deck should come after all face ups.
		for (ListIterator<CardEntity> iter = cards.listIterator(curCardRanges.getFaceUpStart()); iter.hasNext() && moved.size() < faceUp.size(); ) {
			CardEntity ent = iter.next();
			if (faceUp.contains(ent.getValue())) {
				iter.remove();
				moved.add(ent);
			}
		}
		assert moved.size() == faceUp.size();
		int delta = moved.size() - curCardRanges.getFaceUpLength();
		cards.addAll(curCardRanges.getFaceUpStart(), moved);
		curCardRanges.faceUpLengthened(delta);
		for (int i = p.getPlayerId() + 1; i < playerIndices.length; i++)
			playerIndices[i].shifted(delta);

		double rot = Math.PI * 2 * (p.getPlayerId() - model.getLocalPlayerNumber()) / model.getPlayerCount();
		final int OPEN_HAND_SPACING = 2;
		final int CLOSED_HAND_SPACING = 4;
		//closest we can get without 5 players colliding
		final int DISTANCE_FROM_CENTER = 160;
		int i = 0;
		int left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * 3 / 2 + CLOSED_HAND_SPACING;
		for (i = 0; i < curCardRanges.getFaceUpLength(); i++) {
			CardEntity c = cards.get(curCardRanges.getFaceUpStart() + i);
			c.autoMove(rot, transform(rot, new Point(i * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER)), 1);
			c.setShow(true);
		}
	}

	private void repositionHand(Player p) {
		PlayerCardsRange curCardRanges = playerIndices[p.getPlayerId()];
		List<Card> hand = p.getHand();
		List<CardEntity> moved = new ArrayList<CardEntity>(hand.size());
		//All cards in updated hand should have come from old hand, draw deck, or discard pile
		//so we don't have to worry about taking cards from other players and having to
		//update their PlayerCardsRange.
		//Since we assert that updated hand has cards only from old hand, draw deck, or discard pile,
		//we can skip all the cards before our hand since our own hand comes after them,
		//and cards from draw deck and discard pile should come after all hands.
		for (ListIterator<CardEntity> iter = cards.listIterator(curCardRanges.getHandStart()); iter.hasNext() && moved.size() < hand.size(); ) {
			CardEntity ent = iter.next();
			if (hand.contains(ent.getValue())) {
				iter.remove();
				moved.add(ent);
			}
		}
		assert moved.size() == hand.size();
		int delta = moved.size() - curCardRanges.getHandLength();
		cards.addAll(curCardRanges.getHandStart(), moved);
		curCardRanges.handLengthened(delta);
		for (int i = p.getPlayerId() + 1; i < playerIndices.length; i++)
			playerIndices[i].shifted(delta);

		double rot = Math.PI * 2 * (p.getPlayerId() - model.getLocalPlayerNumber()) / model.getPlayerCount();
		final int OPEN_HAND_SPACING = 2;
		final int CLOSED_HAND_SPACING = 4;
		//closest we can get without 5 players colliding
		final int DISTANCE_FROM_CENTER = 160;
		int i;
		int left;
		if (p.getPlayerId() == model.getCurrentPlayer()) {
			i = 0;
			left = -((OPEN_HAND_SPACING - 1) + cardImages.getCardWidth()) * p.getHand().size() / 2;
			for (i = 0; i < curCardRanges.getHandLength(); i++) {
				CardEntity c = cards.get(curCardRanges.getHandStart() + i);
				c.autoMove(rot, transform(rot, new Point(i * (cardImages.getCardWidth() + OPEN_HAND_SPACING) + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1)), 1);
				c.setShow(p.getPlayerId() == model.getLocalPlayerNumber());
			}
		} else {
			i = 0;
			left = -(cardImages.getCardWidth() + CLOSED_HAND_SPACING * (p.getHand().size() - 1)) / 2;
			for (i = 0; i < curCardRanges.getHandLength(); i++) {
				CardEntity c = cards.get(curCardRanges.getHandStart() + i);
				c.autoMove(rot, transform(rot, new Point(i * CLOSED_HAND_SPACING + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1)), 1);
				c.setShow(p.getPlayerId() == model.getLocalPlayerNumber());
			}
		}
	}

	public void drawHint(String message) {
		//TODO: implement
	}

	public void dealtCards(Player p) {
		repositionFaceDown(p);
		repositionFaceUp(p);
		repositionHand(p);
	}

	public void playerClearedDiscardPile(Player p) {
		//this will start with the cards in the draw deck,
		//skipping all the player cards
		for (ListIterator<CardEntity> iter = cards.listIterator(playerIndices[playerIndices.length - 1].getEndIndexPlusOne()); iter.hasNext(); ) {
			CardEntity ent = iter.next();
			//if it's not a draw deck card, it has to be from the old discard pile
			if (!model.getDeckCards().contains(ent)) {
				iter.remove();
				ent.autoMove(4 * Math.PI, new Point(0, 0), 0);
			}
		}
	}

	public void remotePlayerPutCard(Player p, Card c) {
		TurnContext cx = model.getCurrentTurnContext();
		PlayerCardsRange curCardRanges = playerIndices[p.getPlayerId()];
		int startIndex = -1;
		int remaining = 0;
		if (cx.currentPlayable == p.getHand()) {
			startIndex = curCardRanges.getHandStart();
			remaining = curCardRanges.getHandLength();
		} else if (cx.currentPlayable == p.getFaceUp()) {
			startIndex = curCardRanges.getFaceUpStart();
			remaining = curCardRanges.getFaceUpLength();
		} else if (cx.currentPlayable == p.getFaceDown()) {
			startIndex = curCardRanges.getFaceDownStart();
			remaining = curCardRanges.getFaceDownLength();
		}
		CardEntity ent = null;
		boolean found = false;
		for (ListIterator<CardEntity> iter = cards.listIterator(startIndex); iter.hasNext() && !found && remaining > 0; remaining--) {
			ent = iter.next();
			if (ent.getValue() == c)
				found = true;
		}
		if (found) {
			removeCardFromPlayerAndPutOnDiscardPile(cx, p, ent);
			ent.autoMove(0, getDiscardPileLocation(), 1);
		}
	}

	public void playerPickedUpCards(Player p, String message) {
		repositionHand(p);
		drawHint(message);
	}

	public void playerEndedTurn(Player p) {
		repositionHand(p);
	}

	public void playerStartedTurn(Player p) {
		repositionHand(p);
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
