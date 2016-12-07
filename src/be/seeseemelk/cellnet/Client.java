package be.seeseemelk.cellnet;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Client
{
	private Cellnet cellnet;
	private Thread thread;

	private TelnetConnection conn;
	private ANSI ansi;
	
	public Client(Cellnet cellnet, Socket socket) throws IOException
	{
		this.cellnet = cellnet;
		
		conn = new TelnetConnection(socket);
		ansi = new ANSI(conn);
		
		System.out.println("Client " + conn.getId() + " connected");
		
		thread = new Thread(new Runnable()
		{
			
			@Override
			public void run()
			{
				try
				{
					runThread();
				}
				catch (IOException e)
				{
					System.out.println("IOException on client " + conn.getId());
				}
				catch (InterruptedException e)
				{
					System.out.println("Client thread interrupt for client " + conn.getId());
				}
				finally
				{
					disconnect();
				}
			}
		});
		
		thread.start();
	}
	
	public void disconnect()
	{
		if (!conn.isClosed())
		{
			try
			{
				ansi.reset();
				conn.write("You have been disconnected from the server :-(\r\n");
				conn.flush();
			}
			catch (IOException e)
			{
				System.err.println("Failed to properly send some data to a client that wasn't quite disconnected but about to be");
				System.err.println("Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		conn.close();
		cellnet.postDisconnectClient(this);
	}
	
	public TelnetConnection getConnection()
	{
		return conn;
	}
	
	public ANSI getANSI()
	{
		return ansi;
	}
	
	private static final int SCREEN_MAIN = 0;
	private static final int SCREEN_READ = 1;
	private static final int SCREEN_WRITE = 2;
	private static final int SCREEN_VIEW = 3;
	private int screen = SCREEN_MAIN;
	private TextBox messageInput = new TextBox("");
	private String viewingMessage;
	
	private void drawMain() throws IOException
	{
		ansi.setColor(ANSI.BRIGHT + ANSI.BLUE, ANSI.BLACK);
		ansi.clearDisplay();
		ansi.setCursorEnabled(false);
		ansi.setBold(true);
		
		Image logo = cellnet.getLogo();
		int x = (conn.getScreenWidth() - logo.getWidth()) / 2;
		int y = 3;
		cellnet.getLogo().draw(this, x, y);
		
		ansi.setCursorPosition(5, 5);
		ansi.setForeground(ANSI.BRIGHT + ANSI.RED);
		conn.write("Long live Father Carl!");
		
		TextBox box = new TextBox(cellnet.getMotd());
		box.draw(this, conn.getScreenWidth()-33, y-4+logo.getHeight(), 30, 6);
		
		ansi.setForeground(ANSI.BRIGHT + ANSI.MAGENTA);
		ansi.setCursorPosition(20, 20);
		int numMessages = cellnet.getMessages().size();
		conn.write("[R]  Read messages [" + numMessages + "]\n");
		ansi.setCursorPosition(20, 21);
		conn.write("[W]  Write a message\n");
		ansi.setCursorPosition(20, 22);
		conn.write("[D]  Disconnect     \n");
		ansi.setCursorPosition(20, 24);
		if (cellnet.getNumberOfConnectedClients() <= 1)
			conn.write("You're the only one who is currently connected");
		else if (cellnet.getNumberOfConnectedClients() == 2)
			conn.write("There is currently one other person connected");
		else
			conn.write("There are currently " + (cellnet.getNumberOfConnectedClients()-1) + " other people connected");
		
		conn.flush();
	}
	
	private void drawWrite() throws IOException
	{
		ansi.setColor(ANSI.BRIGHT + ANSI.BLUE, ANSI.BLACK);
		ansi.clearDisplay();
		ansi.setCursorEnabled(true);
		ansi.setBold(true);
		
		ansi.setForeground(ANSI.BRIGHT + ANSI.MAGENTA);
		ansi.setCursorPosition(2, getConnection().getScreenHeight()-1);
		conn.write("[^B]  Go back     [^P]  Post        (Enters are not supported)");
		
		ansi.setForeground(ANSI.BRIGHT + ANSI.GREEN);
		messageInput.draw(this, 2, 2, getConnection().getScreenWidth()-2, getConnection().getScreenHeight()-4);
		messageInput.updateCursor(this);
	}
	
	private int cursorIndex = 0;
	private void drawRead() throws IOException
	{
		ansi.setColor(ANSI.BRIGHT + ANSI.BLUE, ANSI.BLACK);
		ansi.clearDisplay();
		ansi.setCursorEnabled(false);
		ansi.setBold(true);
		
		ansi.setForeground(ANSI.BRIGHT + ANSI.MAGENTA);
		ansi.setCursorPosition(0, 2);
		conn.write("  [^B]  Go back    [ENTER]  View message\r\n");
		conn.write("  Messages\r\n\r\n");
		
		ansi.setForeground(ANSI.BRIGHT + ANSI.GREEN);
		List<String> messages = cellnet.getMessages();
		for (int i = messages.size()-1; i >= 0; i--)
		{
			String message = messages.get(i);
			
			int length = message.length();
			int width = conn.getScreenWidth() - 5;
			
			String title = "";
			
			if (length <= width)
			{
				title = message; 
			}
			else if (length > width)
			{
				int end = width - 3;
				title = message.substring(0, end) + "...";
			}
			
			title += "\r\n";
			if (messages.size()-cursorIndex-1 == i)
			{
				ansi.setForeground(ANSI.BRIGHT + ANSI.YELLOW);
				conn.write(" > " + title);
				ansi.setForeground(ANSI.BRIGHT + ANSI.GREEN);
			}
			else
				conn.write(" - " + title);
		}
		conn.flush();
	}
	
	public void drawView() throws IOException
	{
		ansi.setColor(ANSI.BRIGHT + ANSI.BLUE, ANSI.BLACK);
		ansi.clearDisplay();
		ansi.setCursorEnabled(false);
		ansi.setBold(true);
		
		ansi.setCursorPosition(0, 0);
		ansi.setForeground(ANSI.BRIGHT + ANSI.GREEN);
		conn.write(viewingMessage);
		
		ansi.setForeground(ANSI.BRIGHT + ANSI.MAGENTA);
		conn.write("\r\n\r\n  [ENTER]  Go back\r\n");
		conn.flush();
	}
	
	public void viewMessage() throws IOException
	{
		List<String> messages = cellnet.getMessages();
		viewingMessage = messages.get(messages.size()-cursorIndex-1);
		screen = SCREEN_VIEW;
		draw();
	}
	
	private void draw() throws IOException
	{
		if (screen == SCREEN_MAIN)
			drawMain();
		else if (screen == SCREEN_WRITE)
			drawWrite();
		else if (screen == SCREEN_READ)
			drawRead();
		else if (screen == SCREEN_VIEW)
			drawView();
	}
	
	char[] allowed = new char[]{'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
			'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
			'0','1','2','3','4','5','6','7','8','9',
			'/','*','-','+','%',',',':','\'','"','&',';','@','(',')','!','?','\\','/',' ','=','$','€','£','µ','.','^','§','{','}','°','_',
			'[',']','~','<','>'};
	
	public boolean isAllowableCharacter(char c)
	{
		for (char cb : allowed)
		{
			if (c == cb)
				return true;
		}
		return false;
	}
	
	private void keyPressed(char key) throws IOException
	{
		switch (screen)
		{
			case SCREEN_MAIN:
				if (key == 'w' || key == 'W')
				{
					screen = SCREEN_WRITE;
					draw();
				}
				else if (key == 'r' || key == 'R')
				{
					screen = SCREEN_READ;
					draw();
				}
				else if (key == 'd' || key == 'D')
				{
					disconnect();
				}
				break;
			case SCREEN_WRITE:
				if (isAllowableCharacter(key))
				{
					//messageInput.setText(messageInput.getText() + key);
					messageInput.addCharacter(this, key);
				}
				else if (key == 127)
				{
					String text = messageInput.getText();
					if (text.length() > 0)
					{
						text = text.substring(0, text.length()-1);
						messageInput.setText(text);
						draw();
					}
				}
				else if (key <= 26)
				{
					key += 96;
					System.out.println("Key = " + key);
					if (key == 'b')
					{
						screen = SCREEN_MAIN;
						draw();
					}
					else if (key == 'p')
					{
						if (messageInput.getText().length() >= 6)
						{
							cellnet.postMessage(messageInput.getText());
							messageInput.setText("");
							
							screen = SCREEN_MAIN;
							draw();
							ansi.setCursorPosition(0, 0);
							ansi.setForeground(ANSI.BRIGHT + ANSI.CYAN);
							conn.write("Your message has been posted!");
							conn.flush();
						}
						else
						{
							screen = SCREEN_MAIN;
							draw();
							ansi.setCursorPosition(0, 0);
							ansi.setForeground(ANSI.BRIGHT + ANSI.CYAN);
							conn.write("RLY dude? Make your message at least 6 characters long for god sake!");
							conn.flush();
						}
					}
				}
				break;
			case SCREEN_READ:
				if (key <= 26)
				{
					key += 96;
					System.out.println("Key = " + key);
					if (key == 'b')
					{
						screen = SCREEN_MAIN;
						draw();
					}
					else if (key == 'm')
					{
						// This is seriously the enter key
						viewMessage();
					}
				}
				else if (key == 65)
				{
					// Arrow up
					List<String> messages = cellnet.getMessages();
					cursorIndex = (cursorIndex - 1) % messages.size();
					if (cursorIndex < 0)
						cursorIndex = messages.size()-1;
					draw();
				}
				else if (key == 66)
				{
					// Arrow down
					cursorIndex = (cursorIndex + 1) % cellnet.getMessages().size();
					draw();
				}
				break;
			case SCREEN_VIEW:
				if (key <= 26)
				{
					key += 96;
					System.out.println("Key = " + key);
					if (key == 'b' || key == 'm')
					{
						screen = SCREEN_READ;
						draw();
					}
				}
				break;
		}
	}
	
	private void runThread() throws IOException, InterruptedException
	{
		//ansi.reset();
		ansi.setCursorEnabled(false);
		conn.enableCharacterMode(true);
		conn.setWindowSizeOption(true);
		
		draw();
		
		while (true)
		{
			if (conn.isClosed())
			{
				disconnect();
				return;
			}
			
			Thread.sleep(50);
			conn.update();
			
			if (conn.isClosed())
			{
				disconnect();
				return;
			}
			
			if (conn.screenSizeChanged())
				draw();
			
			if (conn.peekCharacter() != null)
			{
				char character = conn.read();
				System.out.println("Character = " + (int) character);
				keyPressed(character);
			}
		}
	}
}













