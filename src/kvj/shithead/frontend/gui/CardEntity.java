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

	private final Point2D initPos, finalPos, curPos;
	private double initRot, finalRot, curRot;
	private double initScale, finalScale, curScale;
	private double vX, vY;

	public CardEntity(Card value, boolean show, Point2D pos, double rotation) {
		this.value = value;
		this.show = show;

		initPos = new Point2D.Double();
		finalPos = new Point2D.Double();
		curPos = new Point2D.Double();

		curPos.setLocation(pos);
		curRot = rotation;
		curScale = 1;

		stop();
	}

	public void autoMove(double rot, Point position, double size) {
		stop();

		finalPos.setLocation(position);
		finalRot = rot;
		finalScale = size;

		double theta = Math.atan2(finalPos.getY() - initPos.getY(), finalPos.getX() - initPos.getX());
		vX = VELOCITY * Math.cos(theta);
		vY = VELOCITY * Math.sin(theta);
	}

	public void manualMove(Point position) {
		curPos.setLocation(position);
		stop();
	}

	public void stop() {
		finalPos.setLocation(curPos);
		initPos.setLocation(curPos);
		initRot = finalRot = curRot;
		initScale = finalScale = curScale;
		vX = vY = 0;
	}

	public void update(double tDelta) {
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
		double progress;
		if (finalPos.equals(initPos)) {
			//assert curPos.equals(initPos);
			progress = 1;
		} else {
			double curDistance = curPos.distance(initPos);
			double finalDistance = finalPos.distance(initPos);
			progress = curDistance / finalDistance;
		}
		curRot = (finalRot - initRot) * progress + initRot;
		curScale = (finalScale - initScale) * progress + initScale;
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
}
