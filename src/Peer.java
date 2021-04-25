import java.io.Serializable;

/**
 * The class that represents a Peer
 * @author azibit
 *
 */
public class Peer implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The username of the peer
	 */
	private String username;
	
	/**
	 * The port of the peer
	 */
	private int portNumber;
	
	/**
	 * Last time peer was updated
	 */
	private long lastUpTime;
	
	/**
	 * The constructor for a peer
	 * @param username
	 * @param portNumber
	 */
	public Peer(String username, int portNumber) {
		this.username = username;
		this.portNumber = portNumber;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	
	public long getLastUpTime() {
		return lastUpTime;
	}

	public void setLastUpTime(long lastUpTime) {
		this.lastUpTime = lastUpTime;
	}
	
	public boolean isActive() {
		return System.currentTimeMillis() - lastUpTime < 12000;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + portNumber;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Peer other = (Peer) obj;
		if (portNumber != other.portNumber)
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Peer [username=" + username + ", portNumber=" + portNumber + "]";
	}
	
	
	
}
