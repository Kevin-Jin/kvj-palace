package kvj.shithead.frontend.gui;

import java.util.List;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.Client;
import kvj.shithead.backend.Game;
import kvj.shithead.backend.Player;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.ClientAdapter;
import kvj.shithead.backend.adapter.HostAdapter;
import kvj.shithead.backend.adapter.NoOperationAdapter;

public class GuiGame extends Game {
	private volatile int localPlayer;
	private final ShitheadPanel view;

	public GuiGame(int playerCount, ShitheadPanel view) {
		super(playerCount);
		this.view = view;
	}

	public int getPlayerCount() {
		return players.length;
	}

	public int getLocalPlayerNumber() {
		return localPlayer;
	}

	public int getCurrentPlayer() {
		return currentPlayer;
	}

	public ShitheadPanel getView() {
		return view;
	}

	@Override
	public void populateDeck() {
		synchronized (getDeckCards()) {
			super.populateDeck();
		}
	}

	@Override
	public void setDeck(List<Card> cards) {
		synchronized (getDeckCards()) {
			super.setDeck(cards);
		}
	}

	@Override
	protected void fillFaceDown() {
		synchronized (players[currentPlayer].getFaceDown()) {
			super.fillFaceDown();
		}
	}

	@Override
	protected void fillHand() {
		synchronized (players[currentPlayer].getHand()) {
			super.fillHand();
		}
	}

	@Override
	public void constructLocalPlayers(int start, int amount, Client client, boolean host) {
		for (int i = 0; i < amount; i++) {
			if (host)
				players[start + i] = new GuiLocalPlayer(start + i, new HostAdapter(client, connected), this);
			else
				players[start + i] = new GuiLocalPlayer(start + i, new ClientAdapter(client), this);
			remainingPlayers.add(Integer.valueOf(start + i));
			connectedCount++;
			localPlayer = start + i;
		}
	}

	@Override
	public void constructRemotePlayer(int playerId, Client client, boolean host) {
		if (host)
			players[playerId] = new GuiRemotePlayer(playerId, new HostAdapter(client, connected), client, this);
		else
			players[playerId] = new GuiRemotePlayer(playerId, NoOperationAdapter.getInstance(), client, this);
		remainingPlayers.add(Integer.valueOf(playerId));
		connectedCount++;
	}

	@Override
	protected void replaceCard(Card c) {
		synchronized (players[currentPlayer].getHand()) {
			super.replaceCard(c);
		}
	}

	@Override
	public void run() {
		view.makeDrawDeckEntities();
		deal();
		for (Player p : players)
			view.dealtCards(p);

		for (currentPlayer = 0; currentPlayer < players.length; currentPlayer++)
			players[currentPlayer].chooseFaceUp(this);

		findStartingPlayer();
		((GuiPlayer) players[currentPlayer]).startedGame();
		view.remotePlayerPutCard(players[currentPlayer], discardPile.get(discardPile.size() - 1));
		((GuiPlayer) players[currentPlayer]).putFirstCard();
		view.playerPickedUpCards(players[currentPlayer]);

		for (currentPlayer = (currentPlayer + 1) % players.length; remainingPlayers.size() > 1; currentPlayer = (currentPlayer + 1) % players.length) {
			if (remainingPlayers.contains(Integer.valueOf(currentPlayer))) {
				TurnContext cx = players[currentPlayer].playTurn(this);
				if (cx.won)
					remainingPlayers.remove(Integer.valueOf(currentPlayer));
			}
		}

		for (Integer pId : remainingPlayers)
			view.drawHint("Player " + (pId.intValue() + 1) + " is the shithead!");
	}

	@Override
	public void addToDiscardPile(Card card) {
		synchronized (discardPile) {
			super.addToDiscardPile(card);
		}
	}

	@Override
	public void transferDiscardPile(List<Card> newLocation) {
		synchronized (discardPile) {
			super.transferDiscardPile(newLocation);
		}
	}

	@Override
	public int discardPileSize() {
		synchronized (discardPile) {
			return super.discardPileSize();
		}
	}

	@Override
	public Card draw() {
		synchronized (getDeckCards()) {
			return super.draw();
		}
	}

	@Override
	public int remainingDrawCards() {
		synchronized (getDeckCards()) {
			return super.remainingDrawCards();
		}
	}
}
