package be.seeseemelk.cellnet;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cellnet
{
	private File messageFolder = new File("messages");
	private List<String> messages = new ArrayList<>(); 
	private List<Client> clients = new ArrayList<>();
	
	private Image logo = new Image(new File("logo.txt"));
	private String motd = FileLoader.loadFile(new File("motd.txt"));
	
	public Cellnet()
	{
		System.out.println("Loading messages");
		
		if (!messageFolder.exists())
			messageFolder.mkdir();
		else if (messageFolder.isFile())
			System.err.println("Message folder is file");
		else
		{
			for (File file : messageFolder.listFiles())
			{
				String str = FileLoader.loadFile(file);
				messages.add(str);
			}
		}
	}
	
	public Image getLogo()
	{
		return logo;
	}
	
	public String getMotd()
	{
		return motd;
	}
	
	public int getNumberOfConnectedClients()
	{
		return clients.size();
	}
	
	public List<String> getMessages()
	{
		synchronized (messages)
		{
			return Collections.unmodifiableList(messages);
		}
	}
	
	public void postDisconnectClient(Client client)
	{
		synchronized (clients)
		{
			for (int i = 0; i < clients.size(); i++)
			{
				Client other = clients.get(i);
				if (client.equals(other))
				{
					clients.remove(client);
					return;
				}
			}
		}
	}
	
	public void postMessage(String message)
	{
		File file;
		synchronized (messages)
		{
			messages.add(message);
			file = new File(messageFolder, messages.size() + ".txt");
		}
		FileLoader.saveFile(file, message);
	}
	
	public void start() throws IOException
	{
		try (ServerSocket server = new ServerSocket(27003))
		{
			System.out.println("Server is now listening");
			while (true)
			{
				Socket socket = server.accept();
				Client client = new Client(this, socket);
				clients.add(client);
			}
		}
		catch (IOException e)
		{
			throw e;
		}
	}
	
	public static void main(String[] arg) throws IOException
	{
		System.out.println("Starting server");
		Cellnet cellnet = new Cellnet();
		cellnet.start();
	}
}






















