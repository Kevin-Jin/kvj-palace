package kvj.shithead.frontend.gui;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import kvj.shithead.backend.Card;

public class CardEntity {
	private static final double VELOCITY = 640d;

	private final Card value;
	private boolean show;

	private final Point2D initPos, finalPos, curPos, markedPos;
	private double initRot, finalRot, curRot, markedRot;
	private double initScale, finalScale, curScale, markedScale;
	private double vX, vY;
	private boolean stopped, atHome;

	public CardEntity(Card value, boolean show, Point2D pos, double rotation) {
		this.value = value;
		this.show = show;

		initPos = new Point2D.Double();
		finalPos = new Point2D.Double();
		curPos = new Point2D.Double();

		markedPos = new Point2D.Double();

		curPos.setLocation(pos);
		curRot = rotation;
		curScale = 1;

		stop();
	}

	private void autoMove(double rot, Point2D position, double size) {
		stop();
		stopped = false;
		atHome = false;

		finalPos.setLocation(position);
		finalRot = rot;
		finalScale = size;

		double theta = Math.atan2(finalPos.getY() - initPos.getY(), finalPos.getX() - initPos.getX());
		vX = VELOCITY * Math.cos(theta);
		vY = VELOCITY * Math.sin(theta);
	}

	public void setShow(boolean show) {
		this.show = show;
	}

	public void manualMove(Point2D position) {
		curPos.setLocation(position);
		stop();
		atHome = false;
	}

	private void stop() {
		finalPos.setLocation(curPos);
		initPos.setLocation(curPos);
		initRot = finalRot = curRot;
		initScale = finalScale = curScale;
		vX = vY = 0;
		stopped = true;
		atHome = true;
	}

	public boolean stopTempDrawingOver() {
		return atHome;
	}

	public void update(double tDelta) {
		if (!stopped) {
			double xUnclamped = curPos.getX() + tDelta * vX;
			double yUnclamped = curPos.getY() + tDelta * vY;
			curPos.setLocation(
					vX > 0 ?
						Math.min(xUnclamped, finalPos.getX())
					:
						Math.max(xUnclamped, finalPos.getX()),
					vY > 0 ?
						Math.min(yUnclamped, finalPos.getY())
					:
						Math.max(yUnclamped, finalPos.getY())
			);
			if (curPos.equals(finalPos)) {
				curRot = finalRot;
				curScale = finalScale;
				stop();
			} else {
				double curDistance = curPos.distance(initPos);
				double finalDistance = finalPos.distance(initPos);
				double progress = curDistance / finalDistance;
				curRot = (finalRot - initRot) * progress + initRot;
				curScale = (finalScale - initScale) * progress + initScale;
			}
		}
	}

	public Card getValue() {
		return value;
	}

	public boolean isGone() {
		final double TOLERANCE = 0.0001d;
		return Math.abs(curScale) < TOLERANCE;
	}

	public double getX() {
		return curPos.getX();
	}

	public double getY() {
		return curPos.getY();
	}

	public double getRotation() {
		return curRot;
	}

	public double getScale() {
		return curScale;
	}

	public boolean show() {
		return show;
	}

	public AffineTransform getTransform(int width, int height) {
		AffineTransform t = AffineTransform.getTranslateInstance(getX(), getY());
		t.concatenate(AffineTransform.getRotateInstance(getRotation()));
		t.concatenate(AffineTransform.getScaleInstance(getScale(), getScale()));
		t.concatenate(AffineTransform.getTranslateInstance(-width / 2, -height / 2));
		return t;
	}

	public boolean isPointInCard(Point point, int width, int height) {
		try {
			return new Rectangle2D.Double(0, 0, width, height).contains(getTransform(width, height).inverseTransform(point, null));
		} catch (NoninvertibleTransformException e) {
			return false;
		}
	}

	public void mark() {
		if (atHome) {
			markedPos.setLocation(curPos);
			markedRot = curRot;
			markedScale = curScale;
		}
	}

	public void mark(Point2D pos, double rot, double scale) {
		markedPos.setLocation(pos);
		markedRot = rot;
		markedScale = scale;
	}

	public void reset() {
		autoMove(markedRot, markedPos, markedScale);
	}
}
