package kvj.shithead.backend.adapter;

import kvj.shithead.backend.Card;
import kvj.shithead.backend.PacketMaker;

public abstract class BroadcastAdapter implements PlayerAdapter {
	protected abstract void broadcast(byte[] message);

	@Override
	public void cardChosen(Card selected) {
		broadcast(PacketMaker.selectCard(selected));
	}
}
