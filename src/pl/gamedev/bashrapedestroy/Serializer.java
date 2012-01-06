package pl.gamedev.bashrapedestroy;

import java.io.DataOutputStream;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Klasa odpowiedzialna za przetwarzanie danych gry do formy gotowej do
 * wys³ania. Dane s¹ przechowywane w pamiêci, ale docelowo bêdzie siê to
 * odbywa³o przez system plików. Pliki bêd¹ zapisywane w okreœlonym katalogu, z
 * którego bêdzie je mo¿na pobraæ poprzez np. apache httpd Takie rozwi¹zanie
 * bêdzie wydajniejsze.
 * 
 * Serializacja odbywa siê w osobnym w¹tku
 * 
 */
public class Serializer extends Thread {

	/**
	 * Plaster danych, zawiera stan gry o danym czasie(timestamp). Dane s¹
	 * przechowywane w formie gotowej do wys³ania.
	 */
	class DataComb {
		public DataComb(long timestamp) {
			this.timestamp = timestamp;
		}

		byte[] bytes;
		long timestamp;
	}

	/**
	 * Trzymamy dane w liœcie o pewnej maksymalnej d³ugoœci
	 */
	private ConcurrentLinkedQueue<DataComb> dataTail = new ConcurrentLinkedQueue<DataComb>();
	private int maxTail;

	public Serializer(int maxTail) {
		this.maxTail = maxTail;
	}

	public DataComb getDataComb(long timestamp) throws IOException {
		Iterator<DataComb> iterator = dataTail.iterator();
		if (iterator.hasNext()) {
			// Zwróæ plaster o mo¿liwie najbli¿szym czasie do zapytania
			DataComb last = iterator.next();
			while (iterator.hasNext()) {
				if (last.timestamp >= timestamp)
					break;
				last = iterator.next();
			}
			return last;
		} else
			return null;
	}

	/**
	 * Zg³oœ obraz gry do serializacji
	 * 
	 * @param image
	 */
	public void pushImage(GameImage image) {
		queue.add(image);
	}

	private ConcurrentLinkedQueue<GameImage> queue = new ConcurrentLinkedQueue<GameImage>();

	@Override
	public void run() {
		while (true) {
			try {
				while (!queue.isEmpty()) {
					GameImage image = queue.poll();
					try {
						dataTail.add(serializeImage(image));
					} catch (Exception e) {
					}
				}

				while (dataTail.size() > maxTail)
					dataTail.poll();
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	private DataComb serializeImage(GameImage image) throws IOException {
		DataComb data = new DataComb((long) Math.floor(image.getTimestamp() / 100));
		CharArrayWriter writer = new CharArrayWriter();
		Vector<GameObject> objects = image.getObjects();
		writer.append(objects.size() + "\n");
		for (GameObject object : objects) {
			writer.append(object.getID() + "\n");
			writer.append(object.getX() + "\n");
			writer.append(object.getY() + "\n");
			writer.append(object.getZ() + "\n");
			writer.append(object.getFX() + "\n");
			writer.append(object.getFY() + "\n");
			writer.append(object.getFZ() + "\n");
			writer.append(object.getH() + "\n");
			writer.append(object.getV() + "\n");

		}
		writer.close();
		data.bytes = String.valueOf(writer.toCharArray()).getBytes("UTF-8");
		return data;
	}

}
