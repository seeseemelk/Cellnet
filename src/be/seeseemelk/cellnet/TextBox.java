package be.seeseemelk.cellnet;

import java.io.IOException;

public class TextBox extends Box
{
	private String text;
	private int x = 0;
	private int y = 0;
	private int width = 0;
	private int height = 0;
	private int cursorPosition = 0;
	private int endOfTextX = -1;
	private int endOfTextY = -1;
	
	public TextBox(String text)
	{
		super(Box.SINGLE);
		this.text = text;
	}
	
	public String getText()
	{
		return text;
	}
	
	public void setText(String text)
	{
		this.text = text;
		cursorPosition = text.length();
	}
	
	public void setCursorPosition(int position)
	{
		if (position >= 0)
			cursorPosition = position;
		else
			cursorPosition = text.length() - position + 1;
	}
	
	@Override
	public void draw(Client client, int x, int y, int width, int height) throws IOException
	{
		super.draw(client, x, y, width, height);
		
		x += 2;
		y += 1;
		width -= 4;
		height -= 2;
		
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		
		ANSI ansi = client.getANSI();
		for (int i = 0; i < height; i++)
		{
			ansi.setCursorPosition(x, y+i);
			
			int start = i*width;
			int end = (i+1)*width;
			
			if (end > text.length())
				end = text.length();
			if (start > end)
			{
				endOfTextX = (end%width) + x;
				endOfTextY = (end/width) + y;
				return;
			}
			
			String str = text.substring(start, end);
			client.getConnection().write(str);
		}
	}
	
	public void updateCursor(Client client) throws IOException
	{
		int px = (cursorPosition % width) + x;
		int py = (cursorPosition / width) + y;
		client.getANSI().setCursorPosition(px, py);
	}
	
	public void addCharacter(Client client, char c) throws IOException
	{
		int location = text.length();
		text += c;
		
		if (location <= width*height)
		{
			endOfTextX = location % width;
			endOfTextY = location / width;
			cursorPosition = location + 1;
			client.getANSI().setCursorPosition(endOfTextX+x, endOfTextY+y);
			client.getConnection().write(Character.toString(c));
			client.getConnection().flush();
		}
	}
}




























