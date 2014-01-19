/**
 * 
 */
package ftp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;

import org.junit.Test;

import client.Connector;
import database.DB;
import exception.ConnectionException;

/**
 * @author Jakub Fortunka
 *
 */
public class ClientThreadTest {


	/**
	 * Test method for {@link ftp.ClientThread} (connection accept)
	 * @throws ConnectionException 
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	@Test
	public void testAcceptingClient() throws UnknownHostException, IOException, ConnectionException {
		DB database = new DB();
		Server s = new Server(3021, database);
		Connector c = new Connector();
		c.connectToServer("localhost",3021,"Kuba","kuba");
	}
	
	@Test(expected=ConnectionException.class)
	public void testServerBadUsername() throws UnknownHostException, IOException, ConnectionException {
		DB database = new DB();
		Server s = new Server(3021, database);
		Connector c = new Connector();
		c.connectToServer("localhost", 3021, "nieistniejacy uzytkownik", "pass");
		fail();
	}
	
	@Test(expected=ConnectionException.class)
	public void testServerBadPassword() throws UnknownHostException, IOException, ConnectionException {
		DB database = new DB();
		Server s = new Server(3021, database);
		Connector c = new Connector();
		c.connectToServer("localhost",3021,"Kuba","trel");
		fail();
	}
	
	@Test
	public void testPwd() {
		DB database = new DB();
		Server s = new Server(3021, database);
		Connector c = new Connector();
		try {
			c.connectToServer("localhost",3021,"Kuba","kuba");
			assertNotNull(c.pwd());
		} catch (IOException | ConnectionException e) {
			e.printStackTrace();
			fail();
		} finally {
			try {
				c.disconnect();
			} catch (IOException | ConnectionException e) {
				e.printStackTrace();
				fail();
			}
		}
	}
	
	
	@Test
	public void testList() {
		DB database = new DB();
		Server s = new Server(3021, database);
		Connector c = new Connector();
		try {
			c.connectToServer("localhost",3021,"Kuba","kuba");
			c.list();
		} catch (IOException | ConnectionException e) {
			e.printStackTrace();
			fail();
		} finally {
			try {
				c.disconnect();
			} catch (IOException | ConnectionException e) {
				e.printStackTrace();
				fail();
			}
		}
	}
}
