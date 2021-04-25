
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;

/**
 * A class to imitate the working of Gnutella
 * 
 * We have a peer to peer communication
 * 
 * @author azibit
 *
 *         Useful Links:
 *         https://stackoverflow.com/questions/40044453/java-peer-to-peer-using-udp-socket
 */
public class Gnutella {

	/**
	 * Host
	 */
	private InetAddress aHost;

	/**
	 * Neighbors
	 */
	private List<Peer> neighbors = new ArrayList<Peer>();

	/**
	 * Directory to store the files I have
	 */
	private List<String> directory = new ArrayList<String>();

	/**
	 * To Represent myself as a peer
	 */
	private Peer myself;

	/**
	 * Needed number of neighbors
	 */
	private int neededNumberOfNeighbors = 3;

	/**
	 * Time to wait before checking for new neighbors or for removing dead neighbors
	 */
	private int timeToWait = 6000;

	/**
	 * Set up constructor
	 * 
	 * @param aHost
	 */
	public Gnutella(InetAddress aHost) {
		this.aHost = aHost;
	}

	/**
	 * Run the entire code from HERE
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		/**
		 * Scanner for reading input
		 */
		Scanner scanner = new Scanner(System.in);

		/**
		 * 1. Get the details of the Peer we want to start
		 */
		System.out.print("PORT: ");
		int portNo = scanner.nextInt();

		System.out.print("USERNAME: ");
		String username = scanner.next();

		/**
		 * 2. Set up Gnutella
		 */
		InetAddress aHost = InetAddress.getLocalHost();
		Peer peer = new Peer(username, portNo);
		Gnutella gnu = new Gnutella(aHost);
		gnu.setMyself(peer);

		/**
		 * 3. Start my server on this port number
		 */
		gnu.startServer(portNo);

		/**
		 * 4. Get the port number of the host already in the Gnutella Network
		 */
		System.out.print("HOST TO CONNECT TO? ");
		int peerPortNumber = scanner.nextInt();

		/**
		 * 5. A node joins the network with a PING to announce self. It sends a PING to
		 * a Well-known root node if starting from scratch
		 */
		Message message = new Message(peer, new Peer("", peerPortNumber), MessageType.PING, "");
		gnu.sendMessage(message);

		/**
		 * Monitor for dead or unresponsive neighbors Maintaining a Gnutella network
		 */
		gnu.monitorNeighbors();

		/**
		 * Take instructions from terminal
		 */
		int promptValue = 0;
		do {
			System.out.println("Press " + "\n1 to send a message or " + "\n2 to view peers and directory "
					+ "\n3 to add file" + "\n4 to search for file" + "\n5 to end");
			promptValue = scanner.nextInt();

			/**
			 * Send a chat
			 */
			if (promptValue == 1) {
				System.out.println("Enter the message to send: ");
				String chatMessage = scanner.next();

				Message messageToSend = new Message(gnu.getMyself(),
						new Peer("", gnu.getPeerList().get(0).getPortNumber()), MessageType.CHAT, chatMessage);

				gnu.sendMessage(messageToSend);
			}

			/**
			 * View Neighbors and list in directory
			 */
			else if (promptValue == 2) {

				System.out.println("PEERS");
				System.out.println(gnu.getPeerList() + "\n");

				System.out.println("DIRECTORY");
				System.out.println(gnu.getDirectory() + "\n");
			}

			/**
			 * Add file to directory
			 */
			else if (promptValue == 3) {

				System.out.println("Enter the file to be added: ");
				String fileName = scanner.next();

				gnu.addFile(fileName);
			}

			/**
			 * Search for file
			 */
			else if (promptValue == 4) {
				System.out.println("Enter the file name you are looking for: ");
				String searchName = scanner.next();

				Message messageToSend = new Message(gnu.getMyself(),
						new Peer("", gnu.getPeerList().get(0).getPortNumber()), MessageType.QUERY, searchName);

				gnu.sendMessage(messageToSend);
			}

		} while (promptValue != 5);

		scanner.close();

	}

	/**
	 * Return directory
	 * 
	 * @return directory
	 */
	public List<String> getDirectory() {
		return directory;
	}

	/**
	 * Add a file to the directory
	 * 
	 * @param file
	 */
	public void addFile(String file) {
		directory.add(file);
	}

	/**
	 * Sends a message
	 * 
	 * @param message
	 * @throws IOException
	 */
	public void sendMessage(Message message) throws IOException {

		System.out.println("Sending " + message.getMessageType() + " from: " + message.getSender() + " to: "
				+ message.getReceiver());
		/**
		 * If for some reasons, the sender and receiver are the same
		 */
		if (message.isRoundLoop()) {
			System.out.println("Sending TO SAME PERSON: " + message.getMessageType() + " from: " + message.getSender()
					+ " to: " + message.getReceiver());
			return;
		}

		/**
		 * Prepare data packet
		 */
		DatagramSocket socket = null;
		byte[] serializedMessage = serializeObject(message);

		try {
			socket = new DatagramSocket();
		} catch (SocketException ex) {
			ex.printStackTrace();
		}

		DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, aHost,
				message.getReceiver().getPortNumber());

		/**
		 * Send data packet
		 */
		try {
			socket.send(packet);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	/**
	 * Starting the server on the given port number
	 * 
	 * @param portNumber
	 */
	public void startServer(int portNumber) {

		/**
		 * Start a thread for the server
		 */
		Thread serverThread = new Thread() {
			public void run() {
				DatagramSocket socket = null;

				try {
					socket = new DatagramSocket(portNumber);
				} catch (SocketException ex) {
					ex.printStackTrace();
				}

				while (true) {
					try {
						DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
						socket.receive(packet);

						/**
						 * Decode received message
						 */
						Message message = arrayToObject(packet.getData());

						Peer sender = message.getSender();

						/**
						 * If the message received is a PING
						 */

						switch (message.getMessageType()) {

						case PING:

							/**
							 * If I have you in my peer list already, update the up-time
							 */
							if (neighbors.contains(sender)) {

								int indexOf = neighbors.indexOf(sender);

								sender.setLastUpTime(System.currentTimeMillis());

								neighbors.set(indexOf, sender);

							} else {
								/**
								 * If conditions allow to connect, send PONG back and send message also to my
								 * neighbors
								 */
								if (canAcceptPeer(sender)) {

									/**
									 * 1. Prepare a PONG MESSAGE
									 */
									Message pongMessage = new Message(myself, sender, MessageType.PONG, "");

									/**
									 * 2. Send Message back to sender Receivers back-propagate a PONG to announce
									 * self
									 */
									sendMessage(pongMessage);

									/**
									 * 3. Add the Peer to my peer list
									 */
									sender.setLastUpTime(System.currentTimeMillis());
									neighbors.add(sender);

									/**
									 * TESTING
									 */
									System.out.println("Now I have: " + neighbors.toString());
								}

							}

							/**
							 * 4. Send the message to all of your neighbors except the person who sent the
							 * message in the first place
							 * 
							 * Receivers forward the Ping to their neighbors
							 */
							sendMessageToNeighbors(message);
							break;

						case PONG:
							/**
							 * If conditions allow to connect, add Peer to your list of peers
							 */

							if (canAcceptPeer(sender)) {

								/**
								 * 2. Add the Peer to my peer list
								 */
								sender.setLastUpTime(System.currentTimeMillis());
								neighbors.add(sender);

								/**
								 * TESTING
								 */
								System.out.println("Now I have: " + neighbors.toString());
							}
							break;

						case CHAT:
							System.out.println(sender.getUsername() + " said: " + message.getMessageBody());

							/**
							 * Send same message to peers
							 */
							sendMessageToNeighbors(message);
							break;

						case QUERY:
							search(message);
							break;

						case HIT:
							System.out.println("The file titled: " + message.getMessageBody()
									+ " can be downloaded from: " + sender);
						}

					} catch (IOException ex) {
						ex.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		};

		serverThread.start();

	}

	public InetAddress getaHost() {
		return aHost;
	}

	public List<Peer> getPeerList() {
		return neighbors;
	}

	public Peer getMyself() {
		return myself;
	}

	public void setMyself(Peer myself) {
		this.myself = myself;
	}

	/**
	 * Serialize to a byte array
	 * 
	 * @param message
	 * @return
	 * @throws IOException
	 */
	private byte[] serializeObject(Message message) throws IOException {
		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutput oo = new ObjectOutputStream(bStream);
		oo.writeObject(message);
		oo.close();

		byte[] serializedMessage = bStream.toByteArray();

		return serializedMessage;
	}

	/**
	 * Convert a byte array to message
	 * 
	 * @param message
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Message arrayToObject(byte[] message) throws IOException, ClassNotFoundException {
		ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(message));
		Message messageClass = (Message) iStream.readObject();
		iStream.close();

		return messageClass;
	}

	/**
	 * Checks if we can accept more peers
	 * 
	 * @return true or false
	 */
	public boolean canAcceptPeer(Peer peer) {
		return neighbors.size() < neededNumberOfNeighbors && !neighbors.contains(peer);
	}

	/**
	 * Send to the neighbors
	 * 
	 * @param message
	 * @throws IOException
	 */
	private void sendMessageToNeighbors(Message message) throws IOException {

		/**
		 * If message can be forwarded to neighbors based on TTL
		 */
		if (message.canForward()) {
			/**
			 * Update message before sending to my neighbors
			 */
			message.updateTimeToLive();

			for (Peer peers : neighbors) {

				/**
				 * Do not send to your peer who initially sent the message
				 */
				if (!peers.equals(message.getSender())) {
					/**
					 * Update the person to receive the message
					 */
					message.setReceiver(peers);

					/**
					 * Send the message
					 */
					sendMessage(message);
				}

			}
		}
	}

	/**
	 * Search for a file with the given name
	 * 
	 * @param file
	 * @return the peers that have the file
	 * @throws IOException
	 */
	public void search(Message message) throws IOException {

		/**
		 * If you have the file we are
		 */
		if (directory.contains(message.getMessageBody())) {
			/**
			 * Checks local system, if not found
			 */
			if (message.getSender() != myself) {
				message.setMessageType(MessageType.HIT);

				message.setReceiver(message.getSender());
				message.setSender(myself);

				sendMessage(message);
			}

			/**
			 * If local system has file
			 */
			else {
				System.out.println(
						"The file titled: " + message.getMessageBody() + " can be downloaded from my local directory ");
			}
		}

		/**
		 * Else, send to my neighbors for help
		 */
		else {
			sendMessageToNeighbors(message);
		}
	}

	/**
	 * Monitors and removes dead peers PING neighbors periodically
	 * 
	 * @param timeToWait
	 */
	private void monitorNeighbors() {
		new Timer().scheduleAtFixedRate(new java.util.TimerTask() {
			@Override
			public void run() {

				ArrayList<Peer> deadPeers = new ArrayList<Peer>();

				for (Peer peer : neighbors) {

					if (!peer.isActive()) {
						deadPeers.add(peer);
					}
				}

				if (!deadPeers.isEmpty()) {
					neighbors.removeAll(deadPeers);
					System.out.println("ALERT: Removed dead peers: " + deadPeers + "\n");
				}

				if (neighbors.size() < neededNumberOfNeighbors) {
					Message searchForNewPeers = new Message(myself, null, MessageType.PING, "");
					try {
						sendMessageToNeighbors(searchForNewPeers);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}
		}, timeToWait, timeToWait);
	}

}
