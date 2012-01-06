package pl.gamedev.bashrapedestroy;

import java.util.HashMap;
import java.util.Vector;

public class GameInstance {

	final static int STEP = 5;

	private long time;

	public GameInstance(long time) {
		this.time = time;
	}

	public GameImage evaluate(long newTime) {
		long dt = newTime - time;
		time = newTime;

		// while (dt > 0) {
		// this.evaluate((dt > STEP ? STEP : dt) / 1000f);
		// dt -= STEP;
		// }
		this.evaluate(dt / 1000f);

		GameImage image = new GameImage();
		for (Warrior warrior : warriors)
			image.addObject(warrior);
		return image;
	}

	private void evaluate(float t) {
		for (Warrior warrior : warriors) {
			warrior.update(t);
		}
	}

	public long getTicket() {
		long ticket = (long) (Math.random() * System.currentTimeMillis());
		warriors.add(new Warrior(ticket, 100, 100));
		return ticket;
	}

	public void input(long ticket, long time, float[] data) {
		int c = 0;
		Warrior warrior = warriorMap.get(ticket);
		if (warrior == null)
			return;
		warrior.input(new Input(time, data));
	}

	private HashMap<Long, Warrior> warriorMap = new HashMap<Long, Warrior>();
	private Vector<Warrior> warriors = new Vector<Warrior>() {
		public synchronized boolean add(Warrior e) {
			warriorMap.put(e.id, e);
			return super.add(e);
		};

		public boolean remove(Object o) {
			warriorMap.remove(((Warrior) o).id);
			return super.remove(o);
		};
	};

	class Input {
		long time;
		float[] data;

		public Input(long time, float[] data) {
			this.time = time;
			this.data = data;
		}
	}

	class Warrior extends GameObject {
		public Warrior(long id, float x, float y) {
			super(id, x, y);

		}

		Input lastInput;

		public void input(Input input) {
			if (lastInput != null)
				synchronized (lastInput) {
					if (lastInput != null && lastInput.time > input.time)
						return;
					lastInput = input;
				}
			else
				lastInput = input;

		}

		public void update(float dt) {
			if (lastInput != null)
				synchronized (lastInput) {
					x = lastInput.data[0];
					y = lastInput.data[1];
					z = lastInput.data[2];
					fx = lastInput.data[3];
					fy = lastInput.data[4];
					fz = lastInput.data[5];
					h = (int) lastInput.data[6];
					v = (int) lastInput.data[7];
					lastInput = null;
				}

			if (z > 0)
				fz = Math.max(fz - 100 * dt, -1000);
			else {
				double dir = Math.atan2(v, h);
				if (h != 0 || v != 0) {
					fx = (float) Math.cos(dir);
					fy = (float) Math.sin(dir);
				}
			}

			if (fx != 0)
				fx = fx - fx * dt * 10 / Math.max(z, 1);
			if (fy != 0)
				fy = fy - fy * dt * 10 / Math.max(z, 1);

			if (fz > 0)
				fz = fz - fz * dt;
			z = z + fz * dt;
			if (z < 0) {
				z = 0;
				fz = 0;
			}

			x += fx * dt * 100;
			y += fy * dt * 100;
		}

	}

}
