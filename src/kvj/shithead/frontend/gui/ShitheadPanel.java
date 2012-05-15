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

	private final List<CardEntity> cards;
	private final Lock cardsReadLock, cardsWriteLock;
	private final Set<CardEntity> tempDrawOver;
	private CardEntity dragged;
	private GuiGame model;
	private final ShitheadController input;
	private final ImageCache cardImages;

	private PlayerCardsRange[] playerIndices;
	private CardRange drawDeckEntitiesRange;
	private CardRange clearedPileEntitiesRange;
	private CardRange discardPileEntitiesRange;

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
		playerIndices[0] = new PlayerCardsRange(null);
		for (int i = 1; i < playerIndices.length; i++)
			playerIndices[i] = new PlayerCardsRange(playerIndices[i - 1].getHandRange());
		drawDeckEntitiesRange = new CardRange(playerIndices[playerIndices.length - 1].getHandRange());
		clearedPileEntitiesRange = new CardRange(drawDeckEntitiesRange);
		discardPileEntitiesRange = new CardRange(clearedPileEntitiesRange);
	}

	private Point2D transform(double rotation, Point2D pt) {
		AffineTransform t = AffineTransform.getTranslateInstance(WIDTH / 2, HEIGHT / 2);
		t.concatenate(AffineTransform.getRotateInstance(rotation));
		t.concatenate(AffineTransform.getTranslateInstance(cardImages.getCardWidth() / 2, cardImages.getCardHeight() / 2));
		Point2D pos = new Point2D.Double();
		t.transform(pt, pos);
		return pos;
	}

	/**
	 * Must be called from the game loop thread to ensure thread-safety.
	 */
	public void makeDrawDeckEntities() {
		int i = 0;
		int right = -cardImages.getCardWidth() + DISCARD_PILE_OFFSET_FROM_CENTER + DRAW_DECK_OFFSET_FROM_DISCARD_PILE;
		cardsWriteLock.lock();
		try {
			drawDeckEntitiesRange.lengthen(model.remainingDrawCards());
			for (Card card : model.getDeckCards())
				cards.add(new CardEntity(card, false, transform(0, new Point(right - i++ * DRAW_CARDS_SEQUENCE_OFFSET, -cardImages.getCardHeight() / 2)), 0));
		} finally {
			cardsWriteLock.unlock();
		}
	}

	public Card.Rank topCardRank() {
		cardsReadLock.lock();
		try {
			int discardPileSize = discardPileEntitiesRange.getLength();
			if (discardPileSize == 0)
				return null;
			return cards.get(discardPileEntitiesRange.getStart() + discardPileSize - 1).getValue().getRank();
		} finally {
			cardsReadLock.unlock();
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

	private CardRange getCorrespondingCardRange(List<Card> playable, Player p) {
		PlayerCardsRange r = playerIndices[p.getPlayerId()];
		if (playable == p.getHand())
			return r.getHandRange();
		if (playable == p.getFaceUp())
			return r.getFaceUpRange();
		if (playable == p.getFaceDown())
			return r.getFaceDownRange();
		return null;
	}

	private void removeCardFromHandAndPutOnFaceUp(Player p, CardEntity card) {
		PlayerCardsRange curCardRanges = playerIndices[p.getPlayerId()];

		cardsWriteLock.lock();
		try {
			curCardRanges.getHandRange().lengthen(-1);
			curCardRanges.getFaceUpRange().lengthen(1);

			cards.remove(card);
			cards.add(curCardRanges.getFaceUpRange().getStart() + curCardRanges.getFaceUpRange().getLength() - 1, card);
		} finally {
			cardsWriteLock.unlock();
		}
	}

	private void removeCardFromPlayerAndPutOnDiscardPile(TurnContext cx, Player p, CardEntity card) {
		cardsWriteLock.lock();
		try {
			getCorrespondingCardRange(cx.currentPlayable, p).lengthen(-1);
			discardPileEntitiesRange.lengthen(1);

			cards.remove(card);
			cards.add(card);
		} finally {
			cardsWriteLock.unlock();
		}
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
						cardsReadLock.lock();
						try {
							faceUpSize = playerIndices[p.getPlayerId()].getFaceUpRange().getLength();
						} finally {
							cardsReadLock.unlock();
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
					} else if (cx.blind || getDiscardPileBounds(discardPileEntitiesRange.getLength()).contains(input.getCursor())) {
						if (((GuiLocalPlayer) p).moveLegal(dragged.getValue())) {
							//assert dragged is from current player's face down, face up, or hand
							dragged.mark(getDiscardPileLocation(discardPileEntitiesRange.getLength()), 0, 1);
							removeCardFromPlayerAndPutOnDiscardPile(cx, p, dragged);
							((GuiLocalPlayer) p).cardChosen(dragged.getValue());
						} else {
							cardsReadLock.lock();
							try {
								drawHint("You may not put a " + dragged.getValue().getRank() + " on top of a " + cards.get(cards.size() - 1).getValue().getRank() + ".");
							} finally {
								cardsReadLock.unlock();
							}
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
			int draggableMinIndex = -1, draggableMaxIndex = -1;
			if (findCardToDrag) {
				CardRange draggableRange = getCorrespondingCardRange(cx.currentPlayable, p);
				draggableMinIndex = draggableRange.getStart();
				draggableMaxIndex = draggableMinIndex + draggableRange.getLength();
			}
			int index = cards.size() - 1;
			for (ListIterator<CardEntity> iter = cards.listIterator(cards.size()); iter.hasPrevious(); index--) {
				CardEntity card = iter.previous();
				card.update(tDelta);
				if (findCardToDrag && card.isPointInCard(input.getCursor(), cardImages.getCardWidth(), cardImages.getCardHeight())) {
					if (index >= draggableMinIndex && index < draggableMaxIndex) {
						if (!cx.blind)
							card.mark();
						else
							card.mark(getDiscardPileLocation(discardPileEntitiesRange.getLength()), 0, 1);
						input.unmark();
						dragged = card;
						dragged.setShow(true);
						tempDrawOver.add(dragged);
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
						Rectangle deckBounds = getDrawPileBounds(Math.max(drawDeckEntitiesRange.getLength() - 1, 0));
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
		int delta;
		cardsWriteLock.lock();
		try {
			CardRange[] sublists = { drawDeckEntitiesRange };
			for (int i = 0; i < sublists.length && moved.size() < faceDown.size(); i++) {
				CardRange bounds = sublists[i];
				ListIterator<CardEntity> iter = cards.listIterator(bounds.getStart() + bounds.getLength());
				for (int j = bounds.getLength() - 1; j >= 0 && moved.size() < faceDown.size(); j--) {
					CardEntity ent = iter.previous();
					if (faceDown.contains(ent.getValue())) {
						iter.remove();
						moved.add(ent);
						bounds.lengthen(-1);
					}
				}
			}
			assert moved.size() == faceDown.size();
			delta = moved.size() - curCardRanges.getFaceDownRange().getLength();

			curCardRanges.getFaceDownRange().lengthen(delta);
			cards.addAll(curCardRanges.getFaceDownRange().getStart(), moved);
		} finally {
			cardsWriteLock.unlock();
		}

		double rot = getRotation(p);
		int i = 0;
		int left = -((FACE_DOWN_SEQUENCE_MARGIN - 1) + cardImages.getCardWidth()) * 3 / 2;
		cardsReadLock.lock();
		try {
			for (i = 0; i < curCardRanges.getFaceDownRange().getLength(); i++) {
				CardEntity c = cards.get(curCardRanges.getFaceDownRange().getStart() + i);
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
		int delta;
		cardsWriteLock.lock();
		try {
			CardRange[] sublists = { curCardRanges.getHandRange(), drawDeckEntitiesRange, discardPileEntitiesRange };
			for (int i = 0; i < sublists.length && moved.size() < hand.size(); i++) {
				CardRange bounds = sublists[i];
				ListIterator<CardEntity> iter = cards.listIterator(bounds.getStart() + bounds.getLength());
				for (int j = bounds.getLength() - 1; j >= 0 && moved.size() < hand.size(); j--) {
					CardEntity ent = iter.previous();
					if (hand.contains(ent.getValue())) {
						iter.remove();
						moved.add(ent);
						bounds.lengthen(-1);
					}
				}
			}
			assert moved.size() == hand.size();
			delta = moved.size() - curCardRanges.getHandRange().getLength();
			Collections.sort(moved, new Comparator<CardEntity>() {
				@Override
				public int compare(CardEntity ent1, CardEntity ent2) {
					return ent1.getValue().getRank().compareTo(ent2.getValue().getRank());
				}
			});

			cards.addAll(curCardRanges.getHandRange().getStart(), moved);
		} finally {
			cardsWriteLock.unlock();
		}
		curCardRanges.getHandRange().lengthen(delta);

		double rot = getRotation(p);
		final int SEQUENCE_OFFSET = ((GuiPlayer) p).isThinking() ? (curCardRanges.getHandRange().getLength() <= 1 ? 0 : Math.min(HAND_SEQUENCE_MARGIN + cardImages.getCardWidth(), (WIDTH - cardImages.getCardWidth()) / (curCardRanges.getHandRange().getLength() - 1))) : HAND_SEQUENCE_OFFSET;
		int i = 0;
		int left = -(cardImages.getCardWidth() + SEQUENCE_OFFSET * (hand.size() - 1)) / 2;
		cardsReadLock.lock();
		try {
			for (i = 0; i < curCardRanges.getHandRange().getLength(); i++) {
				CardEntity c = cards.get(curCardRanges.getHandRange().getStart() + i);
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
		cardsWriteLock.lock();
		try {
			for (ListIterator<CardEntity> iter = cards.listIterator(discardPileEntitiesRange.getStart()); iter.hasNext(); clearedPileEntitiesRange.lengthen(1), discardPileEntitiesRange.lengthen(-1)) {
				CardEntity ent = iter.next();
				ent.mark(new Point(0, 0), 4 * Math.PI, 0);
				ent.reset();
			}
		} finally {
			cardsWriteLock.unlock();
		}
	}

	/**
	 * Must be called from the game loop thread to ensure thread-safety.
	 * @param p
	 */
	public void remotePlayerPutCard(Player p, Card c) {
		TurnContext cx = p.getCurrentContext();
		CardRange r = getCorrespondingCardRange(cx.currentPlayable, p);
		CardEntity ent = null;
		boolean found = false;
		cardsReadLock.lock();
		try {
			int remaining = r.getLength();
			for (Iterator<CardEntity> iter = cards.listIterator(r.getStart()); !found && remaining > 0; remaining--) {
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
				//playerIndices[p.getPlayerId()].getFaceUpRange().getLength() would work as well as p.getFaceUp().size()
				//but this way doesn't need cardsReadLock to be locked (remember, we're in the game loop thread)
				ent.mark(getFaceUpCardLocation(p, p.getFaceUp().size() - 1), getRotation(p), 1);
			} else {
				removeCardFromPlayerAndPutOnDiscardPile(cx, p, ent);
				//discardPileEntitiesRange.getLength().getLength() would work as well as p.getFaceUp().size()
				//but this way doesn't need cardsReadLock to be locked (remember, we're in the game loop thread)
				ent.mark(getDiscardPileLocation(model.discardPileSize() - 1), 0, 1);
			}
			ent.reset();
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
				int discardPileSize;
				int drawDeckSize;
				cardsReadLock.lock();
				try {
					discardPileSize = discardPileEntitiesRange.getLength();
					drawDeckSize = drawDeckEntitiesRange.getLength();
				} finally {
					cardsReadLock.unlock();
				}

				if (discardPileSize == 0)
					drawWrappedString(g2d, new Point(discardPile.x + 2, discardPile.y), "Drop card here", discardPile.width);
				else
					g2d.drawString("Drop card on previously played cards", discardPile.x, discardPile.y - 1);

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
