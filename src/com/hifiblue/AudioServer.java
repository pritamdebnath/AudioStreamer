package com.hifiblue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioServer {

	public static void main(String[] args) throws IOException {

		ServerSocket ss = new ServerSocket(9090);
		ExecutorService executorService = Executors.newCachedThreadPool();

		while (true) {
			Socket s = ss.accept();
			// new Thread(()->processData(ss)).start();try later implementing
			// with lambda
			executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					processData(s);
					return null;
				}
			});
		}

	}

	private static void processData(Socket s) throws IOException {
		InputStream is = null;

		try (InputStream sis = s.getInputStream();
				OutputStream os = s.getOutputStream();) {

			byte songbuffer[] = new byte[20];
			sis.read(songbuffer);
			String songname = new String(songbuffer);

			is = AudioServer.class.getResourceAsStream(songname.toLowerCase()
					.trim() + ".wav");
			byte buffer[] = new byte[2048];
			int count;
			while ((count = is.read(buffer)) != -1) {
				os.write(buffer, 0, count);
				System.out.println("Streaming audio to client-->" + buffer[0]
						+ "--" + buffer[1]);
			}

			System.out.println("Streaming Completed");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			is.close();
		}
	}
}
