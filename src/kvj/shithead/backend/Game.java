package kvj.shithead.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class Game {
	private final Deck drawPile;
	protected final List<Card> discardPile;
	protected final Player[] players;
	protected final Set<Integer> remainingPlayers;
	protected int currentPlayer;
	protected final List<Client> connected;
	protected int connectedCount = 0;

	public Game(int playerCount) {
		drawPile = new Deck();
		discardPile = new ArrayList<Card>(52);
		remainingPlayers = new TreeSet<Integer>();
		players = new Player[playerCount];
		connected = new ArrayList<Client>();
	}

	public void populateDeck() {
		drawPile.populate();
	}

	public void setDeck(List<Card> cards) {
		drawPile.addCards(cards);
	}

	public List<Card> getDeckCards() {
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
			players[i].sortHand();
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

	private Card getLowestCard(Player p) {
		Card lowest = null;
		for (Card card : p.getHand()) {
			Card.Rank rank = card.getRank();
			if (rank != Card.Rank.TWO && rank != Card.Rank.TEN && (lowest == null || rank.compareTo(lowest.getRank()) < 0))
				lowest = card;
		}
		return lowest;
	}

	protected void findStartingPlayer() {
		currentPlayer = 0;
		Card lowestCard = null;

		for (int i = 0; i < players.length; i++) {
			Card playerLowest = getLowestCard(players[i]);
			if (playerLowest != null && (lowestCard == null || playerLowest.getRank().compareTo(lowestCard.getRank()) < 0)) {
				lowestCard = playerLowest;
				currentPlayer = i;
			}
		}

		players[currentPlayer].getHand().remove(lowestCard);
		addToDiscardPile(lowestCard);
		players[currentPlayer].getHand().add(draw());
		players[currentPlayer].sortHand();
	}

	public abstract void run();

	public boolean isMoveLegal(Card attempt) {
		if (discardPile.isEmpty())
			return true;
		Card.Rank attemptRank = attempt.getRank();
		Card.Rank topCardRank = discardPile.get(discardPile.size() - 1).getRank();
		if (attemptRank.compareTo(topCardRank) >= 0)
			return true;
		if (attemptRank == Card.Rank.TWO || attemptRank == Card.Rank.TEN || topCardRank == Card.Rank.TEN)
			return true;
		return false;
	}

	public int getSameRankCount() {
		int count = 0;
		Card.Rank lastCardRank = null;
		for (int i = discardPile.size() - 1; i >= 0; i--) {
			if (lastCardRank == null)
				lastCardRank = discardPile.get(i).getRank();
			if (lastCardRank == discardPile.get(i).getRank())
				count++;
			else
				break;
		}
		return count;
	}

	public Card.Rank getTopCardRank() {
		if (discardPile.isEmpty())
			return null;
		return discardPile.get(discardPile.size() - 1).getRank();
	}

	public void addToDiscardPile(Card card) {
		discardPile.add(card);
	}

	public void transferDiscardPile(List<Card> newLocation) {
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

	public Card draw() {
		return drawPile.pop();
	}

	public int remainingDrawCards() {
		return drawPile.size();
	}
}
