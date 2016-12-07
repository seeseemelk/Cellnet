package be.seeseemelk.cellnet;

import java.io.IOException;

public class ANSI
{
	private TelnetConnection conn;
	
	private static final char ESC = (char) 27;
	
	private int foreground = 0;
	private int background = 0;
	
	public static final byte BLACK = 0;
	public static final byte RED = 1;
	public static final byte GREEN = 2;
	public static final byte YELLOW = 3;
	public static final byte BLUE = 4;
	public static final byte MAGENTA = 5;
	public static final byte CYAN = 6;
	public static final byte WHITE = 7;
	public static final byte BRIGHT = 8;
	
	public ANSI(TelnetConnection conn)
	{
		this.conn = conn;
	}
	
	protected void sendSequence(String sequence) throws IOException
	{
		conn.write(ESC + "[");
		conn.write(sequence);
		conn.flush();
	}
	
	public void reset() throws IOException
	{
		conn.write(ESC + "c");
		conn.flush();
	}
	
	public void clearDisplay() throws IOException
	{
		sendSequence("2J");
	}
	
	public void clearLine() throws IOException
	{
		sendSequence("K");
	}
	
	public void saveCursorPosition() throws IOException
	{
		sendSequence("s");
	}
	
	public void restoreCursorPosition() throws IOException
	{
		sendSequence("u");
	}
	
	public void moveCursorUp(int amount) throws IOException
	{
		sendSequence(amount + "A");
	}
	
	public void moveCursorDown(int amount) throws IOException
	{
		sendSequence(amount + "B");
	}
	
	public void moveCursorRight(int amount) throws IOException
	{
		sendSequence(amount + "C");
	}
	
	public void moveCursorLeft(int amount) throws IOException
	{
		sendSequence(amount + "D");
	}
	
	public void setCursorPosition(int x, int y) throws IOException
	{
		x = Math.max(x, 0);
		y = Math.max(y, 0);
		sendSequence(y + ";" + x + "H");
	}
	
	public void setCursorEnabled(boolean show) throws IOException
	{
		if (show)
			sendSequence("?25h");
		else
			sendSequence("?25l");
	}
	
	public void setBold(boolean bold) throws IOException
	{
		if (bold)
			sendSequence("1m");
		else
			sendSequence("2m");
	}
	
	public void setBackground(int color) throws IOException
	{
		background = color;
		color += 40;
		if (color > 47)
			color += 52;
		sendSequence(color + "m");
	}
	
	public void setForeground(int color) throws IOException
	{
		foreground = color;
		color += 30;
		if (color > 37)
			color += 52;
		sendSequence(color + "m");
	}
	
	public void setColor(int foreground, int background) throws IOException
	{
		this.foreground = foreground;
		this.background = background;
		
		foreground += 30;
		background += 40;
		
		if (foreground > 37)
			foreground += 52;
		if (background > 47)
			background += 52;
		
		sendSequence(foreground + ";" + background + "m");
	}
	
	public void resendColors() throws IOException
	{
		setColor(foreground, background);
	}
}





















