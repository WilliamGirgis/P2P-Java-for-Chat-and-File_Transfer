package server;
// server.java

import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import java.io.*;

public class ServerThread extends Thread {
	ServerSocket serverSocket;
	ArrayList<Socket> connections = new ArrayList<Socket>();
	ObjectInputStream input[] = new ObjectInputStream[5];
	ObjectOutputStream output[] = new ObjectOutputStream[5];
	FileInputStream fileIn;
	FileOutputStream fileOut;
	File file = null;
	Boolean isListenMode = false;
	boolean isWriteActivated = false;

	public ServerThread(int port) throws IOException {
		runPeerServer(port);

	}

	public void runThread(int portDestination, String host) throws IOException {
		int i;
		boolean added = false;
		Socket socket = new Socket(host, portDestination);
		for (i = 0; i < output.length && !added; i++) {// Run through output[] and place the current new socket output
														// stream into the new emplacement
			if (output[i] == null) {
				output[i] = new ObjectOutputStream(socket.getOutputStream());
				System.out.println("Waiting for connection..");
				socket = this.serverSocket.accept();
				added = true;
			}
		}
		connections.add(socket);
		System.out.println("You are now connected with: " + socket.getInetAddress() + " on port " + socket.getPort());

		for (int j = 0; j < input.length; j++) { // Run through input[] and place the current new socket input stream
													// into the new emplacement
			if (input[j] == null) {
				this.input[j] = new ObjectInputStream(socket.getInputStream());
				break;
			}
		}
		parser();
	}

	public void runPeerServer(int port) {
		String text;
		String text2;
		try {
			serverSocket = new ServerSocket(port, 5);// 5 Peer allowed to connect to this server
			System.out.println("Listening on:" + serverSocket.getLocalPort());
			parser();
		} catch (Exception e) {
		}

	}

	public void printPeers() {
		if (connections.isEmpty()) {
			System.out.println("No connections yet.");
			return;
		}

		for (int i = 0; i < connections.size(); i++) {
			System.out.println("Peer n°" + (i + 1) + ": " + connections.get(i).getInetAddress() + " Port: "
					+ connections.get(i).getPort());
		}
	}

	public void readCommand(String[] inp) throws IOException {
		switch (inp[0]) {
		case "set":
			runThread(Integer.parseInt(inp[1]), inp[2]);
			break;
		case "list":
			printPeers();
			break;
		case "help":
			System.out.println(
					"Write :\n| 'set' + <port> + <host> to set a connection with a peer \n| 'list' to print the peers \n| 'Q' to quit \n| 'chat' to send message to all connected peers \n| 'file' to send a file to a specific peer");
			break;
		case "chat":
			chatMode();
			break;
		case "file":
			fileMode();
			break;
		case "Q":
			System.out.println("BYE !");
			System.exit(1);
			break;
		default:
			System.err.println("Command not recognized\n");
		}

	}

	public void parser() throws IOException {
		Scanner scan = new Scanner(System.in);
		String inp[] = {};
		while (true) {
			System.out.print("->");
			inp = scan.nextLine().split(" ", 4);
			readCommand(inp);
		}

	}

	public void fileMode() throws NumberFormatException, IOException {
		System.out.println("File mode\n<File localtion> + <index connection*>");
		Scanner scan = new Scanner(System.in);
		String in[] = {};
		int socketIndex = 0;
		while (true) {
			System.out.print("FM->");
			in = scan.nextLine().split(" ", 3);// Split input to creat a kind of 'arguments parser'
			socketIndex = Integer.parseInt(in[1]);
			if (in[0].equals("DL")) {// Download mode -> the peer will wait for incoming data
				writeFile(connections.get(socketIndex).getInputStream());
			} else {
				readFile(in[0], Integer.parseInt(in[1]));
			}
		}
	}

	public void readFile(String fileLocation, int socketIndex) throws IOException {
		file = new File(fileLocation);
		FileInputStream fis = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		while (fis.read(buffer) != -1) {
			System.out.println(buffer.length);
			output[socketIndex].write(buffer);
		}
		System.out.println("Done");
		fis.close();
		System.out.println("FileOutputStream closed");
		output[socketIndex].close();
		System.out.println("OutputStream closed");
	}

	public void writeFile(InputStream is) throws IOException {

		FileOutputStream fos = null;
		long filesize = 600000000;
		byte[] buffer = new byte[(int) filesize];
		int read = 0;
		int totalRead = 0;
		long rest = filesize;

		try {
			fos = new FileOutputStream("Output.txt");
			System.out.println("Waiting for packets..");
			while ((read = is.read(buffer, 0, (int) Math.min(buffer.length, rest))) != -1) {
				totalRead += read;
				rest -= read;
				System.out.println("read " + totalRead + " bytes.");
				fos.write(buffer, 0, read);
			}
		} catch (IOException e) {
			e.printStackTrace();
			fos.close();
			System.out.println("FileOutputStream closed");
			is.close();
			System.out.println("InputStream closed");
		}
	}

	/*
	 * 
	 * 
	 * Chat related methods
	 * 
	 * 
	 */

	public void chatMode() throws IOException {
		String outMsg = "";
		Scanner scan = new Scanner(System.in);
		readMsg();
		while (outMsg != "STOP") {
			System.out.print(">>");
			outMsg = scan.nextLine();
			try {
				if (!isListenMode) {
					sendMsg(outMsg);
				} else {
					System.out.println("You cant send messages, you are in Listen mode");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;

	}

	public void sendMsg(String outMsg) throws IOException {

		Boolean bounded = false;
		for (int i = 0; i != output.length && !bounded; i++) {

			output[i].writeUTF(outMsg);
			output[i].flush();
			if (output[i + 1] == null) { // If the next stream element is empty, just stop the loop
				bounded = true;
			}
		}

	}

	public void readMsg() throws IOException {
		Thread t[] = new Thread[input.length];// Created an empty array of thread
		int x = 0;
		while (input[x] != null) {// While there is a input stream, we keep creating a thread for each peer
									// listening
			int i = x;// variable needed to passe it as a local variable in the new Thread ran
			t[x] = new Thread(new Runnable() {// Start the Thread for that input socket

				@Override
				public void run() {
					String msg = "";
					while (true) {
						try {
							msg = input[i].readUTF();
							System.out.println(msg);
							if (msg.equals("STOP")) {// If a Thread reads STOP, it will go back in the parser in Listen
														// mode
								System.out.println("Listen mode activated !");
								isListenMode = true;
							}
						} catch (IOException e) {
							e.printStackTrace();
							t[i].interrupt();// If the connection is lost, we stop the thread
							try {
								parser();// Then go back to parser
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			});
			t[x].start();
			x++;
		}
	}

	public static void main(String[] args) throws IOException, SocketTimeoutException {
		ServerThread server = new ServerThread(Integer.parseInt(args[0]));

	}
}
