package kvj.shithead.frontend.gui;

import java.util.concurrent.CountDownLatch;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.TurnContext;
import kvj.shithead.backend.adapter.PlayerAdapter;

public class GuiLocalPlayer extends GuiPlayer {
	private volatile CountDownLatch inputWait;
	private volatile boolean sameRankOnly;
	private volatile Card chosen;

	public GuiLocalPlayer(int playerId, PlayerAdapter adapter, GuiGame model) {
		super(playerId, adapter, model);
		inputWait = new CountDownLatch(1);
	}

	@Override
	public Card chooseCard(TurnContext state, String selectText, boolean sameRank, boolean checkDiscardPile) {
		try {
			sameRankOnly = sameRank;
			inputWait = new CountDownLatch(1);
			inputWait.await();
			inputWait = null;
			adapter.cardChosen(chosen);
			return chosen;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean moveLegal(Card c) {
		if (sameRankOnly)
			return (currentCx.selection.getRank() == c.getRank());
		return currentCx.blind || currentCx.g.isMoveLegal(c);
	}

	public void cardChosen(Card value) {
		chosen = value;
		inputWait.countDown();
	}
}
