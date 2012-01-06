package pl.gamedev.bashrapedestroy;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class GameImage implements Cloneable {

	private Vector<GameObject> objects = new Vector<GameObject>();

	public long getTimestamp() {
		return System.currentTimeMillis();
	}

	public Vector<GameObject> getObjects() {
		// TODO Auto-generated method stub
		return objects;
	}

	public void addObject(GameObject object) {
		objects.add(object);
	}

}
