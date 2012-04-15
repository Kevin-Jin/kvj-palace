package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class Game {
	private final Deck drawPile;
	private final List<Card.Rank> discardPile;
	protected final Player[] players;
	protected final Set<Integer> remainingPlayers;
	protected int currentPlayer;
	protected final List<Client> connected;
	protected int connectedCount = 0;

	public Game(int playerCount) {
		drawPile = new Deck();
		discardPile = new ArrayList<Card.Rank>(52);
		remainingPlayers = new TreeSet<Integer>();
		players = new Player[playerCount];
		connected = new ArrayList<Client>();
	}

	public void populateDeck() {
		drawPile.populate();
	}

	public void setDeck(List<Card.Rank> cards) {
		drawPile.addCards(cards);
	}

	public List<Card.Rank> getDeckCards() {
		return drawPile.getList();
	}

	public void deal() {
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < players.length; j++)
				players[j].getFaceDown().add(draw());
		for (int i = 0; i < 6; i++)
			for (int j = 0; j < players.length; j++)
				players[j].getHand().add(draw());

		for (int i = 0; i < players.length; i++)
			Collections.sort(players[i].getHand());
	}

	public Player getPlayer(int playerId) {
		return players[playerId];
	}

	public int occupiedCount() {
		return connectedCount;
	}

	public int maxSize() {
		return players.length;
	}

	public abstract void constructLocalPlayers(int start, int amount, Client hub, boolean host);

	public abstract void constructRemotePlayer(int playerId, Client client, boolean host);

	public void clientConnected(Client connection) {
		connected.add(connection);
	}

	public List<Client> getConnected() {
		return connected;
	}

	private Card.Rank getLowestCard(Player p) {
		Card.Rank lowest = null;
		for (Card.Rank card : p.getHand())
			if (card != Card.Rank.TWO && card != Card.Rank.TEN && (lowest == null || card.compareTo(lowest) < 0))
				lowest = card;
		return lowest;
	}

	protected void findStartingPlayer() {
		currentPlayer = 0;
		Card.Rank lowestCard = null;

		for (int i = 0; i < players.length; i++) {
			Card.Rank playerLowest = getLowestCard(players[i]);
			if (playerLowest != null && (lowestCard == null || playerLowest.compareTo(lowestCard) < 0)) {
				lowestCard = playerLowest;
				currentPlayer = i;
			}
		}

		players[currentPlayer].getHand().remove(lowestCard);
		addToDiscardPile(lowestCard);
		players[currentPlayer].getHand().add(draw());
		Collections.sort(players[currentPlayer].getHand());
	}

	public abstract void run();

	public boolean isMoveLegal(Card.Rank attempt) {
		if (discardPile.isEmpty())
			return true;
		Card.Rank topCard = discardPile.get(discardPile.size() - 1);
		if (attempt.compareTo(topCard) >= 0)
			return true;
		if (attempt == Card.Rank.TWO || attempt == Card.Rank.TEN || topCard == Card.Rank.TEN)
			return true;
		return false;
	}

	public int getSameRankCount() {
		int count = 0;
		Card.Rank lastCard = null;
		for (int i = discardPile.size() - 1; i >= 0; i--) {
			if (lastCard == null)
				lastCard = discardPile.get(i);
			if (lastCard == discardPile.get(i))
				count++;
			else
				break;
		}
		return count;
	}

	public Card.Rank getTopCard() {
		if (discardPile.isEmpty())
			return null;
		return discardPile.get(discardPile.size() - 1);
	}

	public void addToDiscardPile(Card.Rank card) {
		discardPile.add(card);
	}

	public void transferDiscardPile(List<Card.Rank> newLocation) {
		if (newLocation != null)
			newLocation.addAll(discardPile);
		discardPile.clear();
	}

	public int discardPileSize() {
		return discardPile.size();
	}

	public boolean canDraw() {
		return !drawPile.isEmpty();
	}

	public Card.Rank draw() {
		return drawPile.pop();
	}

	public int remainingDrawCards() {
		return drawPile.size();
	}
}
