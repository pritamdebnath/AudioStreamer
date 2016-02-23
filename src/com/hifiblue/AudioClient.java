package com.hifiblue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioClient {

	public static void main(String[] args) throws IOException {

		try {
			Socket socket = new Socket(args[0], 9090);
			System.out.println("server connected...");
			socket.getOutputStream().write(args[1].getBytes());
			AudioInputStream ais = AudioSystem
					.getAudioInputStream(new BufferedInputStream(socket
							.getInputStream()));
			AudioFormat format = ais.getFormat();

			int bufferSize = format.getFrameSize()
					* (int) format.getSampleRate();
			byte[] buffer = new byte[bufferSize];
			int count = 0;

			BlockingQueue<byte[]> bufferQueue = new LinkedBlockingQueue<byte[]>();
			playSongInOtherThread(bufferQueue, format);

			while ((count = ais.read(buffer)) > 0) {
				if (count > 0) {
					bufferQueue.add(buffer);
					System.out.println("getting audio from server-->"
							+ buffer[0] + "---" + buffer[1]);
				}
			}

		} catch (UnknownHostException e) {
			System.out.println("Unknown host: localhost");
		} catch (IOException e) {
			System.out.println("No I/O");
		} catch (UnsupportedAudioFileException e) {
			System.out.println("Unsupported Audio File");
			e.printStackTrace();
		}
	}

	private static void playSongInOtherThread(
			BlockingQueue<byte[]> bufferQueue, AudioFormat format) {
		ExecutorService executorService = Executors.newCachedThreadPool();
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		SourceDataLine line;
		try {
			line = (SourceDataLine) AudioSystem.getLine(info);
			executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					line.open(format);
					line.start();
					while (true) {
						byte[] data = bufferQueue.take();
						if (data != null) {
							line.write(data, 0, data.length);
						} else {
							break;
						}
						// line.write(buffer, 0, count);
					}
					line.drain();
					line.close();
					return null;
				}
			});

		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}

	}
}
