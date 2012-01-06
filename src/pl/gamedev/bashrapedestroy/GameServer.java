package pl.gamedev.bashrapedestroy;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;

import pl.gamedev.bashrapedestroy.Serializer.DataComb;

import sun.misc.IOUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Prosty serwer WWW napisany w Javie, posiada graficzny interfejs(applet),
 * mo¿na go uruchomiæ na dowolnym systemie.
 * 
 * Dla wiêkszej liczby graczy takie rozwi¹zanie bêdzie niewystarczaj¹ce.
 * 
 */
public class GameServer extends Applet {
	/**
	 * Pliki statyczne s¹ ma³e i dlatego s¹ trzymane w pamiêci.
	 */
	private final HashMap<String, byte[]> fileCache = new HashMap<String, byte[]>();

	/**
	 * Obs³uga zapytañ
	 */
	private final class Handler implements HttpHandler {

		/**
		 * Zwróæ liniê tekstu
		 */
		public void serveString(String line, HttpExchange request) throws IOException {
			request.sendResponseHeaders(200, line.length());
			OutputStream os = request.getResponseBody();
			os.write(line.getBytes());
			os.close();
		}

		/**
		 * Zwróæ zawartoœæ pliku
		 */
		private void serveFile(String name, HttpExchange request) throws IOException {
			if (fileCache.containsKey(name)) {
				OutputStream os = request.getResponseBody();
				Headers headers = request.getResponseHeaders();
				headers.add("Content-Type", name.endsWith(".html") ? "text/html" : name.endsWith(".js") ? "text/javascript" : name.endsWith(".png") ? "image/png" : null);
				request.sendResponseHeaders(200, fileCache.get(name).length);
				os.write(fileCache.get(name));
				os.close();
			} else {
				request.sendResponseHeaders(404, 0);
				request.getResponseBody().close();
			}
		}

		/**
		 * 
		 * Zwróæ obraz gry
		 * 
		 */
		private void serveImage(long timestamp, HttpExchange request) throws IOException {
			OutputStream os = request.getResponseBody();
			DataComb data = serializer.getDataComb(timestamp);
			if (data != null) {
				request.sendResponseHeaders(200, data.bytes.length);
				os.write(data.bytes);
				os.close();
			} else {
				request.sendResponseHeaders(404, 0);
				os.close();
			}

		}

		/**
		 * Obs³uga zapytania
		 * 
		 */
		public void handle(HttpExchange request) throws IOException {
			long t = System.currentTimeMillis();
			String uri = request.getRequestURI().getPath();
			if (uri.startsWith("/im")) {
				serveImage(Long.parseLong(uri.substring(3)), request);
			} else if ("/input".equalsIgnoreCase(uri)) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(request.getRequestBody()));
				String[] line = reader.readLine().split("&");
				reader.close();
				int c = 0;
				long ticket = Long.parseLong(line[c++].split("=")[1]);
				float[] input = new float[8];
				for (int i = 0; i < 8; i++)
					try {
						input[i] = Float.parseFloat(line[c++].split("=")[1]);
					} catch (Exception e) {
						input[i] = 0;
					}

				gameInstance.input(ticket, System.currentTimeMillis(), input);
				serveString("", request);
			} else if ("/join".equalsIgnoreCase(uri)) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(request.getRequestBody()));
				try {
					long ts = Long.parseLong(reader.readLine().split("=")[1]);
					reader.close();
					serveString("{t: " + gameInstance.getTicket() + ", ts: " + (t - ts) + "}", request);
				} catch (Exception e) {
					serveString("error", request);
				}
			} else if (fileCache.containsKey(uri.substring(1))) {
				serveFile(uri.substring(1), request);
			} else if ("/".equalsIgnoreCase(uri)) {
				serveFile("index.html", request);
			} else {
				request.sendResponseHeaders(404, 0);
				request.getResponseBody().close();
			}

			// System.out.println(System.nanoTime() - t + " " + uri);
		}
	}

	private GameInstance gameInstance;
	private GameImage image;
	private Serializer serializer;
	private float frameRate = 0;

	private String address;
	private int port;
	private File workDir;

	public GameServer(String address, int port, String workDir) {
		this.address = address;
		this.port = port;
		this.workDir = new File(workDir);
	}

	private void cacheFile(String path) throws FileNotFoundException, IOException {
		byte[] bytes = IOUtils.readFully(new FileInputStream(new File(workDir, path)), -1, true);
		fileCache.put(path, bytes);
	}

	public void start() {

		try {
			cacheFile("dojo.js");
			cacheFile("index.html");
			cacheFile("main.js");
			cacheFile("sprites.png");
			cacheFile("sprites2.png");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		serializer = new Serializer(1000);
		serializer.start();

		gameInstance = new GameInstance(System.currentTimeMillis());

		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(address, port), 128);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		server.createContext("/", new Handler());
		server.setExecutor(null); // creates a default executor
		server.start();

		new Thread() {

			public void run() {
				long t = System.currentTimeMillis();
				long c = 0;
				while (true) {
					image = gameInstance.evaluate(System.currentTimeMillis());
					serializer.pushImage(image);
					frameRate = c++ / ((System.currentTimeMillis() - t + 1) / 1000.0f);
					repaint();
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						break;
					}
				}
			};
		}.start();
	}

	final Color background = new Color(87, 116, 82);

	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setPaint(background);
		g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
		g2d.setPaint(Color.BLACK);
		for (GameObject obj : image.getObjects())
			g2d.fillRect((int) obj.x - 10, (int) obj.y - 10, 10, 10);
		g2d.setColor(Color.BLACK);
		g2d.drawString("frameRate: " + frameRate, 10, 10);
		super.paint(g);
	}

	public static void main(String[] args) throws IOException {
		if (args == null || args.length < 3 || !args[0].matches("^([0-9]{1,3}).([0-9]{1,3}).([0-9]{1,3}).([0-9]{1,3})$") || !args[1].matches("^([0-9]+)$") || !new File(args[2]).exists()
				|| !new File(args[2]).isDirectory()) {
			System.out.println("[LISTEN IP] [PORT] [DIR]");
			return;
		}
		GameServer server = new GameServer(args[0], Integer.parseInt(args[1]), args[2]);
		server.start();

	}
}
