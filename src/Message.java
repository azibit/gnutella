import java.io.Serializable;

/**
 * A Message
 * 
 * @author azibit
 *
 */
public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Sender username
	 */
	private Peer sender;

	/**
	 * Receiver's Username
	 */
	private Peer receiver;

	/**
	 * Get the message type
	 */
	private MessageType messageType;

	/**
	 * If the message has a body
	 */
	private String messageBody;

	/**
	 * Time to live for each message
	 */
	private int ttl = 3;

	/**
	 * Constructor
	 * 
	 * @param senderUsername
	 * @param senderPortNo
	 * @param receiverUserName
	 * @param receiverPortNo
	 * @param messageType
	 */
	public Message(Peer sender, Peer receiver, MessageType messageType, String messageBody) {
		super();
		this.sender = sender;
		this.receiver = receiver;
		this.messageType = messageType;
		this.messageBody = messageBody;
	}

	public Peer getSender() {
		return sender;
	}

	public void setSender(Peer sender) {
		this.sender = sender;
	}

	public Peer getReceiver() {
		return receiver;
	}

	public void setReceiver(Peer receiver) {
		this.receiver = receiver;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	public void updateTimeToLive() {
		ttl -= 1;
	}


	/**
	 * Checks if a message can still be forwarded based on TTL
	 * 
	 * @return
	 */
	public boolean canForward() {
		return ttl > 0;
	}

	/**
	 * Check if sender and receiver are the same
	 * 
	 * @return true or false
	 */
	public boolean isRoundLoop() {
		return sender.getPortNumber() == receiver.getPortNumber();
	}
}
