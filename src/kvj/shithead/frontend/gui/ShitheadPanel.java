package kvj.shithead.frontend.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JComponent;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;

//TODO: animate mouseDown, mouseOver for deck
//TODO: animate mouseOver (pulsating outline?), mouseDown (solid outline?) for selectable cards,
//and special animation when mouseDown over discard pile
//TODO: outline group of selectable cards (TurnContext.currentPlayable)
public class ShitheadPanel extends JComponent {
	private static final long serialVersionUID = -6335150580109791737L;

	private static final int WIDTH = 1280, HEIGHT = 800;
	private static final int TABLE_DIAMETER = 800;

	//will fit 5 players without having face down/face up collide
	private static final int DISTANCE_FROM_CENTER = 160;
	private static final int FACE_DOWN_SEQUENCE_MARGIN = 2;
	private static final int FACE_UP_OFFSET_FROM_FACE_DOWN = 3;
	private static final int DRAW_CARDS_SEQUENCE_OFFSET = 2;
	private static final int DISCARDED_CARDS_SEQUENCE_OFFSET = 2;
	//will fit a 52 card discard pile with 5 players and draw deck
	//after dealing any amount of players
	private static final int DISCARD_PILE_OFFSET_FROM_CENTER = -28;
	private static final int DRAW_DECK_OFFSET_FROM_DISCARD_PILE = -2;
	private static final int HAND_SEQUENCE_OFFSET = 4;
	private static final int HAND_SEQUENCE_MARGIN = 2;

	private List<CardEntity> cards;
	private Lock cardsReadLock, cardsWriteLock;
	private Set<CardEntity> tempDrawOver;
	private CardEntity dragged;
	private GuiGame model;
	private ShitheadController input;
	private ImageCache cardImages;
	private PlayerCardsRange[] playerIndices;

	public ShitheadPanel() {
		input = new ShitheadController();
		addMouseListener(input);
		addMouseMotionListener(input);
		setFocusable(true);
		requestFocusInWindow();

		cardImages = new ImageCache();
		cardImages.populate();
		cards = new ArrayList<CardEntity>();
		tempDrawOver = new LinkedHashSet<CardEntity>();
		ReadWriteLock locks = new ReentrantReadWriteLock();
		cardsReadLock = locks.readLock();
		cardsWriteLock = locks.writeLock();
	}

	public void setModel(GuiGame model) {
		this.model = model;
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
		int i = 0;
		int right = -cardImages.getCardWidth() + DISCARD_PILE_OFFSET_FROM_CENTER + DRAW_DECK_OFFSET_FROM_DISCARD_PILE;
		cardsWriteLock.lock();
		try {
			synchronized (model.getDeckCards()) {
				for (Card card : model.getDeckCards())
					cards.add(new CardEntity(card, false, transform(0, new Point(right - i++ * DRAW_CARDS_SEQUENCE_OFFSET, -cardImages.getCardHeight() / 2)), 0));
			}
		} finally {
			cardsWriteLock.unlock();
		}
	}

	private Rectangle getDrawPileBounds(int i) {
		//TODO: share with makeDrawDeckEntities
		int right = -cardImages.getCardWidth() + DISCARD_PILE_OFFSET_FROM_CENTER + DRAW_DECK_OFFSET_FROM_DISCARD_PILE;
		return new Rectangle(WIDTH / 2 + right - i * DRAW_CARDS_SEQUENCE_OFFSET, HEIGHT / 2 - cardImages.getCardHeight() / 2, i * DRAW_CARDS_SEQUENCE_OFFSET + cardImages.getCardWidth(), cardImages.getCardHeight());
	}

	private Point getDrawDeckLocation(int i) {
		//TODO: share with getDrawPileBounds
		int right = -cardImages.getCardWidth() + DISCARD_PILE_OFFSET_FROM_CENTER + DRAW_DECK_OFFSET_FROM_DISCARD_PILE;
		return new Point(WIDTH / 2 + right - i * DRAW_CARDS_SEQUENCE_OFFSET, HEIGHT / 2 + cardImages.getCardHeight() / 2);
	}

	private Rectangle getDiscardPileBounds(int i) {
		return new Rectangle(WIDTH / 2 + DISCARD_PILE_OFFSET_FROM_CENTER, HEIGHT / 2 - cardImages.getCardHeight() / 2, i * DISCARDED_CARDS_SEQUENCE_OFFSET + cardImages.getCardWidth(), cardImages.getCardHeight());
	}

	private Point getDiscardPileLocation(int i) {
		//TODO: share with getDiscardPileBounds
		return new Point(WIDTH / 2 + DISCARD_PILE_OFFSET_FROM_CENTER + i * DISCARDED_CARDS_SEQUENCE_OFFSET + cardImages.getCardWidth() / 2, HEIGHT / 2);
	}

	private Rectangle getLocalFaceUpCardBounds(int i) {
		int left = -((FACE_DOWN_SEQUENCE_MARGIN - 1) + cardImages.getCardWidth()) * 3 / 2 + FACE_UP_OFFSET_FROM_FACE_DOWN;
		return new Rectangle(i * (cardImages.getCardWidth() + FACE_DOWN_SEQUENCE_MARGIN) + left + WIDTH / 2, DISTANCE_FROM_CENTER + HEIGHT / 2, cardImages.getCardWidth(), cardImages.getCardHeight());
	}

	private Point2D getFaceUpCardLocation(Player p, int i) {
		//TODO: share with getLocalFaceUpCardBounds
		int left = -((FACE_DOWN_SEQUENCE_MARGIN - 1) + cardImages.getCardWidth()) * 3 / 2 + FACE_UP_OFFSET_FROM_FACE_DOWN;
		Point2D pt = transform(getRotation(p), new Point(i * (cardImages.getCardWidth() + FACE_DOWN_SEQUENCE_MARGIN) + left, DISTANCE_FROM_CENTER));
		return pt;
	}

	private void removeCardFromHandAndPutOnFaceUp(Player p, CardEntity card) {
		PlayerCardsRange curCardRanges = playerIndices[p.getPlayerId()];

		cardsWriteLock.lock();
		try {
			cards.remove(card);
			//this should put it after the other face up cards
			cards.add(curCardRanges.getHandStart(), card);
		} finally {
			cardsWriteLock.unlock();
		}

		curCardRanges.handLengthened(-1);
		curCardRanges.faceUpLengthened(1);
	}

	private void removeCardFromPlayerAndPutOnDiscardPile(TurnContext cx, Player p, CardEntity card) {
		cardsWriteLock.lock();
		try {
			cards.remove(card);
			//permanantly paint dragged last so it's on top of the discard pile
			cards.add(card);
		} finally {
			cardsWriteLock.unlock();
		}

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
		Player p = model.getPlayer(model.getCurrentPlayer());
		TurnContext cx = p.getCurrentContext();
		boolean localPlayer = cx != null && (model.getCurrentPlayer() == model.getLocalPlayerNumber());
		boolean findCardToDrag = false;
		if (dragged != null) {
			if (!input.mouseDown() || !localPlayer) {
				if (localPlayer) {
					if (cx.choosingFaceUp) {
						int faceUpSize;
						synchronized (p.getFaceUp()) {
							faceUpSize = p.getFaceUp().size();
						}
						if (getLocalFaceUpCardBounds(faceUpSize).contains(input.getCursor())) {
							dragged.mark(getFaceUpCardLocation(p, faceUpSize), 0, 1);
							removeCardFromHandAndPutOnFaceUp(p, dragged);
							((GuiLocalPlayer) p).cardChosen(dragged.getValue());
						} else {
							for (int i = faceUpSize; i < 3; i++) {
								if (getLocalFaceUpCardBounds(i).contains(input.getCursor())) {
									drawHint("Please put your selection on the leftmost available location.");
									break;
								}
							}
						}
					//if cx.blind, we already flipped the face down card, so we must
					//choose current selection no matter where we drop it
					} else if (cx.blind || getDiscardPileBounds(model.getDiscardPileSize()).contains(input.getCursor())) {
						if (((GuiLocalPlayer) p).moveLegal(dragged.getValue())) {
							//assert dragged is from current player's face down, face up, or hand
							dragged.mark(getDiscardPileLocation(model.getDiscardPileSize()), 0, 1);
							removeCardFromPlayerAndPutOnDiscardPile(cx, p, dragged);
							((GuiLocalPlayer) p).cardChosen(dragged.getValue());
						} else {
							drawHint("You may not put a " + dragged.getValue().getRank() + " on top of a " + cx.g.getTopCardRank() + ".");
						}
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
		cardsReadLock.lock();
		try {
			for (ListIterator<CardEntity> iter = cards.listIterator(cards.size()); iter.hasPrevious(); ) {
				CardEntity card = iter.previous();
				card.update(tDelta);
				if (findCardToDrag && card.isPointInCard(input.getCursor(), cardImages.getCardWidth(), cardImages.getCardHeight())) {
					synchronized (cx.currentPlayable) {
						if (cx.currentPlayable.contains(card.getValue())) {
							if (!cx.blind)
								card.mark();
							else
								card.mark(getDiscardPileLocation(model.getDiscardPileSize()), 0, 1);
							dragged = card;
							dragged.setShow(true);
							tempDrawOver.add(dragged);
						}
					}
					findCardToDrag = false;
				}
			}
		} finally {
			cardsReadLock.unlock();
		}

		//do this after the find card to drag routine in case
		//there are cards that are drawn over the draw deck that
		//could be selected first
		if (localPlayer && dragged == null) {
			if (input.mouseDown()) {
				if (input.getMark() == null)
					input.mark();
			} else {
				Point mark = input.getMark();
				if (mark != null) {
					input.unmark();
					if (((GuiLocalPlayer) p).canEndTurn()) {
						Rectangle deckBounds = getDrawPileBounds(Math.max(model.getDrawDeckSize() - 1, 0));
						if (deckBounds.contains(input.getCursor()) && deckBounds.contains(mark))
							((GuiLocalPlayer) p).cardChosen(null);
					}
				}
			}
		}
	}

	private double getRotation(Player p) {
		return Math.PI * 2 * (p.getPlayerId() - model.getLocalPlayerNumber()) / model.getPlayerCount();
	}

	/**
	 * Must be called from the game loop thread to ensure thread-safety.
	 * @param p
	 */
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
		int delta;
		cardsWriteLock.lock();
		try {
			for (ListIterator<CardEntity> iter = cards.listIterator(curCardRanges.getFaceDownStart()); iter.hasNext() && moved.size() < faceDown.size(); ) {
				CardEntity ent = iter.next();
				if (faceDown.contains(ent.getValue())) {
					iter.remove();
					moved.add(ent);
				}
			}
			assert moved.size() == faceDown.size();
			delta = moved.size() - curCardRanges.getFaceDownLength();

			cards.addAll(curCardRanges.getFaceDownStart(), moved);
		} finally {
			cardsWriteLock.unlock();
		}
		curCardRanges.faceDownLengthened(delta);
		for (int i = p.getPlayerId() + 1; i < playerIndices.length; i++)
			playerIndices[i].shifted(delta);

		double rot = getRotation(p);
		int i = 0;
		int left = -((FACE_DOWN_SEQUENCE_MARGIN - 1) + cardImages.getCardWidth()) * 3 / 2;
		cardsReadLock.lock();
		try {
			for (i = 0; i < curCardRanges.getFaceDownLength(); i++) {
				CardEntity c = cards.get(curCardRanges.getFaceDownStart() + i);
				c.mark(transform(rot, new Point(i * (cardImages.getCardWidth() + FACE_DOWN_SEQUENCE_MARGIN) + left, DISTANCE_FROM_CENTER)), rot, 1);
				c.reset();
				c.setShow(false);
			}
		} finally {
			cardsReadLock.unlock();
		}
	}

	/**
	 * Must be called from the game loop thread to ensure thread-safety.
	 * @param p
	 */
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
		int delta;
		cardsWriteLock.lock();
		try {
			for (ListIterator<CardEntity> iter = cards.listIterator(curCardRanges.getHandStart()); iter.hasNext() && moved.size() < hand.size(); ) {
				CardEntity ent = iter.next();
				if (hand.contains(ent.getValue())) {
					iter.remove();
					moved.add(ent);
				}
			}
			assert moved.size() == hand.size();
			delta = moved.size() - curCardRanges.getHandLength();
			Collections.sort(moved, new Comparator<CardEntity>() {
				@Override
				public int compare(CardEntity ent1, CardEntity ent2) {
					return ent1.getValue().getRank().compareTo(ent2.getValue().getRank());
				}
			});

			cards.addAll(curCardRanges.getHandStart(), moved);
		} finally {
			cardsWriteLock.unlock();
		}
		curCardRanges.handLengthened(delta);
		for (int i = p.getPlayerId() + 1; i < playerIndices.length; i++)
			playerIndices[i].shifted(delta);

		double rot = getRotation(p);
		final int SEQUENCE_OFFSET = ((GuiPlayer) p).isThinking() ? (curCardRanges.getHandLength() <= 1 ? 0 : Math.min(HAND_SEQUENCE_MARGIN + cardImages.getCardWidth(), (WIDTH - cardImages.getCardWidth()) / (curCardRanges.getHandLength() - 1))) : HAND_SEQUENCE_OFFSET;
		int i = 0;
		int left = -(cardImages.getCardWidth() + SEQUENCE_OFFSET * (p.getHand().size() - 1)) / 2;
		cardsReadLock.lock();
		try {
			for (i = 0; i < curCardRanges.getHandLength(); i++) {
				CardEntity c = cards.get(curCardRanges.getHandStart() + i);
				c.mark(transform(rot, new Point(i * SEQUENCE_OFFSET + left, DISTANCE_FROM_CENTER + cardImages.getCardHeight() + 1)), rot, 1);
				c.reset();
				c.setShow(p.getPlayerId() == model.getLocalPlayerNumber());
			}
		} finally {
			cardsReadLock.unlock();
		}
	}

	public void drawHint(String message) {
		//TODO: implement
	}

	public void dealtCards(Player p) {
		repositionFaceDown(p);
		repositionHand(p);
	}

	public void playerClearedDiscardPile(Player p) {
		cardsReadLock.lock();
		try {
			synchronized (model.getDeckCards()) {
				//this will start with the cards in the draw deck,
				//skipping all the player cards
				for (ListIterator<CardEntity> iter = cards.listIterator(playerIndices[playerIndices.length - 1].getEndIndexPlusOne()); iter.hasNext(); ) {
					CardEntity ent = iter.next();
					//if it's not a draw deck card, it has to be from the old discard pile
					if (!model.getDeckCards().contains(ent.getValue())) {
						//spin it twice just to convey that it's going down,
						//and have it eventually shrink into nothing as it
						//approaches the top left corner
						ent.mark(new Point(0, 0), 4 * Math.PI, 0);
						ent.reset();
					}
				}
			}
		} finally {
			cardsReadLock.unlock();
		}
	}

	public void remotePlayerPutCard(Player p, Card c) {
		TurnContext cx = p.getCurrentContext();
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
		cardsReadLock.lock();
		try {
			for (ListIterator<CardEntity> iter = cards.listIterator(startIndex); iter.hasNext() && !found && remaining > 0; remaining--) {
				ent = iter.next();
				if (ent.getValue() == c)
					found = true;
			}
		} finally {
			cardsReadLock.unlock();
		}
		if (found) {
			ent.setShow(true);
			if (cx.choosingFaceUp) {
				removeCardFromHandAndPutOnFaceUp(p, ent);
				ent.mark(getFaceUpCardLocation(p, p.getFaceUp().size() - 1), getRotation(p), 1);
				ent.reset();
			} else {
				removeCardFromPlayerAndPutOnDiscardPile(cx, p, ent);
				ent.mark(getDiscardPileLocation(model.getDiscardPileSize() - 1), 0, 1);
				ent.reset();
			}
		}
	}

	public void playerPickedUpCards(Player p) {
		repositionHand(p);
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

	private void drawWrappedString(Graphics2D g2d, Point pos, String s, float width) {
		LineBreakMeasurer measurer = new LineBreakMeasurer(new AttributedString(s).getIterator(), g2d.getFontRenderContext());
		while (measurer.getPosition() < s.length()) {
			TextLayout layout = measurer.nextLayout(width);
			pos.y += layout.getAscent();
			layout.draw(g2d, pos.x, pos.y);
			pos.y += layout.getDescent() + layout.getLeading();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawOval(WIDTH / 2 - TABLE_DIAMETER / 2, HEIGHT / 2 - TABLE_DIAMETER / 2, TABLE_DIAMETER, TABLE_DIAMETER);

		Rectangle discardPile = getDiscardPileBounds(0);
		g2d.drawRect(discardPile.x, discardPile.y, discardPile.width - 1, discardPile.height - 1);
		Rectangle drawPile = getDrawPileBounds(0);
		g2d.drawRect(drawPile.x, drawPile.y, drawPile.width - 1, drawPile.height - 1);
		if (model.getCurrentPlayer() == model.getLocalPlayerNumber()) {
			TurnContext cx = model.getPlayer(model.getCurrentPlayer()).getCurrentContext();
			if (cx != null && !cx.choosingFaceUp) {
				if (model.getDiscardPileSize() == 0)
					drawWrappedString(g2d, new Point(discardPile.x + 2, discardPile.y), "Drop card here", discardPile.width);
				else
					g2d.drawString("Drop card on previously played cards", discardPile.x, discardPile.y - 1);

				int drawDeckSize = model.getDrawDeckSize();
				if (drawDeckSize == 0) {
					drawWrappedString(g2d, new Point(drawPile.x + 2, drawPile.y), "Click here to end turn", discardPile.width);
				} else {
					Point pt = getDrawDeckLocation(drawDeckSize - 1);
					g2d.drawString("Click the deck to end your turn", pt.x, pt.y + g2d.getFontMetrics().getAscent());
				}
			}
		}

		cardsReadLock.lock();
		try {
			for (CardEntity card : cards)
				if (!tempDrawOver.contains(card))
					draw(card, g2d);
		} finally {
			cardsReadLock.unlock();
		}
		for (CardEntity card : tempDrawOver)
			draw(card, g2d);
	}
}
