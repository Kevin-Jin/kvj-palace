package kvj.shithead.frontend.gui;

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

	//GuiLocalPlayer.moveLegal runs in the EDT, and isMoveLegal uses the discardPile
	//collection, which is not thread-safe. we need an alternative so that we don't need
	//to lock discardPile
	public boolean threadSafeIsMoveLegal(Card attempt) {
		Card.Rank topCardRank = view.topCardRank();
		if (topCardRank == null)
			return true;
		return isMoveLegal(attempt.getRank(), topCardRank);
	}
}
