package pl.gamedev.bashrapedestroy;

public class GameObject {

	protected long id;
	protected float x;
	protected float y;
	protected float z;
	protected float fx;
	protected float fy;
	protected float fz;
	protected int h, v;

	public GameObject(long id, float x, float y) {
		this.id = id;
		this.x = x;
		this.y = y;
	}
	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public float getH() {
		return h;
	}

	public float getV() {
		return v;
	}

	public float getFX() {
		return fx;
	}

	public float getFY() {
		return fy;
	}

	public float getFZ() {
		return fz;
	}

	public long getID() {
		return id;
	}

	public void setID(long id) {
		this.id = id;

	}

	public void setX(float x) {
		this.x = x;

	}

	public void setY(float y) {
		this.y = y;
	}

	public void setH(int h) {
		this.h = h;
	}

	public void setV(int v) {
		this.v = v;
	}

}
