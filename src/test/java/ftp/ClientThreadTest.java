/**
 * 
 */
package ftp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.sql.SQLException;

import org.junit.Test;

import ftp.Connector;
import database.DB;
import ftp.ConnectionException;

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
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Test
	public void testAcceptingClient() throws UnknownHostException, IOException, ConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		DB database = new DB();
		Server s = new Server(3021, database);
		new Thread(s).start();
		try {
			Connector c = new Connector();
			c.connectToServer("localhost",3021,"Kuba","kuba");
		} catch (ConnectionException e) {
			fail();
			throw e;
		} finally {
			s.stop();
		}
	}

	@Test(expected=ConnectionException.class)
	public void testServerBadUsername() throws UnknownHostException, IOException, ConnectionException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		DB database = new DB();
		Server s = new Server(3021, database);
		new Thread(s).start();
		try {
			Connector c = new Connector();
			c.connectToServer("localhost", 3021, "nieistniejacy uzytkownik", "pass");
			fail();
		} catch (ConnectionException e) {
			throw e;
		} finally {
			s.stop();
		}
	}

		@Test(expected=ConnectionException.class)
		public void testServerBadPassword() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException, ConnectionException  {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
			try {
				Connector c = new Connector();
				c.connectToServer("localhost",3021,"Kuba","trel");
				fail();
			} catch (ConnectionException e) {
				throw e;
			} finally {
				s.stop();
			}
		}

		@Test
		public void testPwd() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
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
					s.stop();
				} catch (IOException | ConnectionException e) {
					e.printStackTrace();
					fail();
				}
			}
		}


		@Test
		public void testList() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
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
					s.stop();
				} catch (IOException | ConnectionException e) {
					e.printStackTrace();
					fail();
				}
			}
		}
		
		@Test
		public void fileOperation1() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
			Connector c = new Connector();
			try {
				c.connectToServer("localhost",3021,"Kuba","kuba");
				File f = new File("tmp");
				f.createNewFile();
				PrintWriter out = new PrintWriter("tmp");
				out.println("Test 1 2 3");
				out.close();
				c.sendFile(f, true);
				f.delete();
			} catch (ConnectionException e) {
				fail();
				e.printStackTrace();
			} catch (UnknownHostException e) {
				fail();
				e.printStackTrace();
			} catch (IOException e) {
				fail();
				e.printStackTrace();
			} finally {
				s.stop();
			}
		}
		
		@Test
		public void fileOperation2() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
			Connector c = new Connector();
			try {
				c.connectToServer("localhost",3021,"Kuba","kuba");
				File f = new File("tmp");
				c.getFile(f, "tmp");
				if (f.length() == 0) fail();
				f.delete();
			} catch (ConnectionException e) {
				fail();
				e.printStackTrace();
			} catch (UnknownHostException e) {
				fail();
				e.printStackTrace();
			} catch (IOException e) {
				fail();
				e.printStackTrace();
			} finally {
				s.stop();
			}
		}
		
		@Test(expected=ConnectionException.class)
		public void fileOperation3() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException, ConnectionException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
			Connector c = new Connector();
			try {
				c.connectToServer("localhost",3021,"test","test");
				if (c.removeFile("tmp")) fail();
			} catch (UnknownHostException e) {
				fail();
				e.printStackTrace();
			} catch (IOException e) {
				fail();
				e.printStackTrace();
			} finally {
				s.stop();
			}
		}
		
		@Test
		public void fileOperation4() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
			Connector c = new Connector();
			try {
				c.connectToServer("localhost",3021,"Kuba","kuba");
				c.changeRights("tmp", "777");
			} catch (UnknownHostException e) {
				fail();
				e.printStackTrace();
			} catch (IOException e) {
				fail();
				e.printStackTrace();
			} catch (ConnectionException e) {
				fail();
				e.printStackTrace();
			} finally {
				s.stop();
			}
		}
		
		@Test
		public void fileOperation5() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
			DB database = new DB();
			Server s = new Server(3021, database);
			new Thread(s).start();
			Connector c = new Connector();
			try {
				c.connectToServer("localhost",3021,"test","test");
				if (!c.removeFile("tmp")) fail();
			} catch (UnknownHostException e) {
				fail();
				e.printStackTrace();
			} catch (IOException e) {
				fail();
				e.printStackTrace();
			} catch (ConnectionException e) {
				fail();
				e.printStackTrace();
			} finally {
				s.stop();
			}
		}
	}
