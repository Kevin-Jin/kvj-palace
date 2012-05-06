package kvj.shithead.frontend.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class ShitheadController implements MouseListener, MouseMotionListener {
	private Point pt;
	private boolean drag;
	private boolean flag;

	public ShitheadController() {
		pt = new Point(0, 0);
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		pt = arg0.getPoint();
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		pt = arg0.getPoint();
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		drag = true;
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		drag = false;
	}

	public Point getCursor() {
		return pt;
	}

	public boolean mouseDown() {
		return drag;
	}

	public void flag() {
		flag = true;
	}

	public boolean isFlagged() {
		return flag;
	}

	public void unflag() {
		flag = false;
	}
}
