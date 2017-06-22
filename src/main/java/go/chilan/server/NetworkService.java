package go.chilan.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.graphhopper.chilango.network.EasyCrypt;

class NetworkService implements Runnable {
	private final ServerSocket serverSocket;
	private final ExecutorService pool;
	private final EasyCrypt ec;
	private WorkHandler handler;

	public NetworkService(int port, ExecutorService pool) throws Exception {
		serverSocket = new ServerSocket(port);
		// pool = Executors.newFixedThreadPool(poolSize);//(poolSize);
		this.pool = pool;
		ec = new EasyCrypt(null, EasyCrypt.aes);
		ec.setKey(ec.readKey(new File("aes.key")));
		System.out.println("pool created on port");
	}

	public void run() { // run the service
		// running the confirmation Listener

		try {
			// do{
			System.out.println("waiting for connection :-)");
			handler = new WorkHandler(serverSocket, ec);
			handler.runTask();
			System.out.println("connection accepted");
			// }while(true);

		} finally {
			System.out.println("close down in NetworkService");
			closeDown("from NetworkSize");
		}
	}

	public void closeDown(String origin) {
		System.out.println("close down pool "+origin);
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(60, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
}
