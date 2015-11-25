package se.su.dsv.maryam;

public class XY {
	private float X, Y;

	public XY(float x, float y) {
		super();
		X = x;
		Y = y;
	}

	public float getX() {
		return X;
	}

	public void setX(float x) {
		X = x;
	}

	public float getY() {
		return Y;
	}

	public void setY(float y) {
		Y = y;
	}
	
	@Override
	public boolean equals(Object o) {
		if( !(o instanceof XY)) return false;
		XY other = (XY) o;
		return (other.getX() == this.X) && (other.getY() == this.Y);
	}
}
