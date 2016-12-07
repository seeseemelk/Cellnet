package be.seeseemelk.cellnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class Image
{
	private List<String> data = new ArrayList<>();
	private int longest = 0;
	
	public Image(File file)
	{
		String data = FileLoader.loadFile(file);
		BufferedReader reader = new BufferedReader(new StringReader(data));
		
		try
		{
			String str;
			while ((str = reader.readLine()) != null)
			{
				if (str.length() > longest)
					longest = str.length();
				this.data.add(str);
			}
		}
		catch (IOException e)
		{
			System.err.println("Somehow an IOException occured in a place where it should be impossible");
			e.printStackTrace();
		}
	}
	
	public int getWidth()
	{
		return longest;
	}
	
	public int getHeight()
	{
		return data.size();
	}
	
	public void draw(Client client, int x, int y) throws IOException
	{
		for (int line = 0; line < data.size(); line++)
		{
			String str = data.get(line);
			client.getANSI().setCursorPosition(x, y+line);
			client.getConnection().write(str);
		}
		client.getConnection().flush();
	}
}






















