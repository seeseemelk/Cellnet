package be.seeseemelk.cellnet;

import java.io.IOException;
import java.util.Arrays;

public class Box
{
	private int border;
	
	public static final int NONE = 0;
	public static final int SINGLE = 1;
	public static final int DOUBLE = 2;
	
	public Box(int border)
	{
		this.border = border;
	}
	
	public void draw(Client client, int x, int y, int width, int height) throws IOException
	{
		if (border == NONE)
			return;
		
		ANSI ansi = client.getANSI();
		
		if (border == SINGLE)
		{
			char[] buffer = new char[width];
			Arrays.fill(buffer, '-');
			buffer[buffer.length-1] = '\\';
			buffer[0] = '/';
			ansi.saveCursorPosition();
			ansi.setCursorPosition(x, y);
			client.getConnection().write(buffer);
			
			buffer[buffer.length-1] = '/';
			buffer[0] = '\\';
			ansi.setCursorPosition(x, y+height-1);
			client.getConnection().write(buffer);
			
			Arrays.fill(buffer, ' ');
			buffer[buffer.length-1] = '|';
			buffer[0] = '|';
			
			for (int line = 0; line < height-2; line++)
			{
				ansi.setCursorPosition(x, y+line+1);
				client.getConnection().write(buffer);
			}	
		}
	}
}









