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
	Boolean isListenMode = false;

	public ServerThread(int port) throws IOException {
		runPeerServer(port);

	}

	public void setConnection() throws IOException {
		Scanner scan = new Scanner(System.in);
		System.out.println("To Which port ?:");
		int port = Integer.parseInt(scan.nextLine());
		System.out.println("To Which host ?:");
		String host = scan.nextLine();
		runThread(port, host);
	}

	public void runThread(int portDestination, String host) throws IOException {
		int i;
		Boolean added = false;
		Socket socket = new Socket(host, portDestination);
		for (i = 0; i < output.length && !added; i++) {
			if (output[i] == null) {
				output[i] = new ObjectOutputStream(socket.getOutputStream());
				System.out.println("Waiting for connection..");
				socket = this.serverSocket.accept();
				added = true;
			}
		}
		connections.add(socket);
		System.out.println("You are now connected with: " + socket.getInetAddress() + " on port " + socket.getPort());

		for (int j = 0; j < input.length; j++) {
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
			serverSocket = new ServerSocket(port, 5);// 3 Peer allowed to connect to this server
			System.out.println("Listening on:" + serverSocket.getLocalPort());
			parser();
		} catch (Exception e) {
			// TODO: handle exception
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

	public void readCommand(String inp) throws IOException {
		switch (inp) {
		case "set":
			setConnection();
			break;
		case "list":
			printPeers();
			break;
		case "help":
			System.out.println(
					"write 'set' to set a connection with a peer | 'list' to print the peers | 'Q' to quit | 'chat' to send message to all connected peers");
			break;
		case "chat":
			startChat();
			break;
		case "Q":
			System.out.println("BYE !");
			System.exit(1);
			break;
		default:
			System.err.print("Command not recognized\n");
		}

	}

	public void parser() throws IOException {
		Scanner scan = new Scanner(System.in);
		String inp = "";
		while (true) {
			System.out.print("->");
			inp = scan.nextLine();
			readCommand(inp);
		}

	}

	public void startChat() throws IOException {
		String outMsg = "";
		Scanner scan = new Scanner(System.in);
		readMsg();
		while (outMsg != "STOP") {
			System.out.print(">>");
			outMsg = scan.nextLine();
			try {
				sendMsg(outMsg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;

	}

	public void sendMsg(String outMsg) throws IOException {

		Boolean bounded = false;
		if (!isListenMode) {// If Listen mode, we prevent from writting messages
			for (int i = 0; i != output.length && !bounded; i++) {

				output[i].writeUTF(outMsg);
				output[i].flush();
				if (output[i + 1] == null) { // If the next stream element is empty, just stop the loop
					bounded = true;
				}
			}
		} else {
			System.out.println("You cant send messages, you are in Listen mode");
		}
	}

	public void readMsg() throws IOException {
		Thread t[] = new Thread[input.length];// Created an empty array of thread
		int x = 0;
		while (input[x] != null) {
			int i = x;// variable needed to passe it as a local variable in the new Thread ran
			t[x] = new Thread(new Runnable() {// Start the Thread for that input socket

				@Override
				public void run() {
					String msg = "";
					while (true) {
						try {
							msg = input[i].readUTF();
							System.out.println(msg);
							if (msg.equals("STOP")) {// If a Thread reads STOP, it will go back in the parser in Listen mode
								System.out.println("Listen mode activated !");
								isListenMode = true;
							}
						} catch (IOException e) {
							e.printStackTrace();
							t[i].stop();// If the connection is lost, we stop the thread
							try {
								parser();// Then go back to parser
							} catch (IOException e1) {
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
