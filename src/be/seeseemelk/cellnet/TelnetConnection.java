package be.seeseemelk.cellnet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class TelnetConnection
{
	private BufferedInputStream input;
	private BufferedOutputStream output;
	
	private Byte lastByte = null;
	
	private EnumMap<TelnetOption, Boolean> clientWillMap = new EnumMap<TelnetOption, Boolean>(TelnetOption.class);
	private EnumMap<TelnetOption, Boolean> clientDoesMap = new EnumMap<TelnetOption, Boolean>(TelnetOption.class);
	private EnumMap<TelnetOption, Boolean> serverWillMap = new EnumMap<TelnetOption, Boolean>(TelnetOption.class);
	private EnumMap<TelnetOption, Boolean> serverDoesMap = new EnumMap<TelnetOption, Boolean>(TelnetOption.class);
	private EnumMap<TelnetOption, Boolean> hasNegotiatedMap = new EnumMap<TelnetOption, Boolean>(TelnetOption.class);
	
	private int lastScreenWidth = -1;
	private int lastScreenHeight = -1;
	private int screenWidth = -1;
	private int screenHeight = -1;
	
	private Socket socket;
	
	public TelnetConnection(Socket socket) throws IOException
	{
		this.socket = socket;
		socket.setKeepAlive(false);
		input = new BufferedInputStream(socket.getInputStream());
		output = new BufferedOutputStream(socket.getOutputStream());
	}
	
	protected boolean get(EnumMap<TelnetOption, Boolean> map, TelnetOption option)
	{
		if (map.containsKey(option))
			return map.get(option);
		else
			return false;
	}
	
	protected void set(EnumMap<TelnetOption, Boolean> map, TelnetOption option, boolean state)
	{
		map.put(option, state);
	}
	
	public void write(byte[] bytes) throws IOException
	{
		output.write(bytes);
	}
	
	public void write(char[] chars) throws IOException
	{
		byte[] bytes = new byte[chars.length];
		for (int i = 0; i < chars.length; i++)
			bytes[i] = (byte) chars[i];
		write(bytes);
	}
	
	public void write(TelnetOption[] options) throws IOException
	{
		byte[] bytes = new byte[options.length];
		for (int i = 0; i < options.length; i++)
			bytes[i] = (byte) options[i].asChar();
		write(bytes);
		update();
	}
	
	public void write(String data) throws IOException
	{
		output.write(data.getBytes());
	}
	
	public void write(String str, Object... values) throws IOException
	{
		String newStr = String.format(str, values);
		write(newStr);
	}
	
	public void flush() throws IOException
	{
		output.flush();
	}
	
	public String readString() throws IOException
	{
		StringBuffer buffer = new StringBuffer();
		char lastChar = '\0';
		boolean receivedCR = false;
		
		do
		{
			lastChar = read();
			if (lastChar == TelnetOption.CR.asChar())
				receivedCR = true;
			else if (lastChar != TelnetOption.LF.asChar())
				buffer.append(lastChar);
		}
		while (!(receivedCR && lastChar == TelnetOption.LF.asChar()));
		
		update();
		return buffer.toString();
	}
	
	public char read() throws IOException
	{
		char c = rawRead();
		while (c == TelnetOption.IAC.asChar())
		{
			readTelnetOption();
			c = rawRead();
		}
		return c;
	}
	
	public void update() throws IOException
	{
		int c = peek();
		if (c == -1)
		{
			return;
		}
		
		while (((char)c) == TelnetOption.IAC.asChar())
		{
			rawRead();
			readTelnetOption();
			lastByte = null;
			
			c = peek();
			if (c == -1)
				return;
		}
	}
	
	private char rawRead() throws IOException
	{
		if (lastByte != null)
		{
			byte b = lastByte;
			lastByte = null;
			return (char) b;
		}
		else
		{
			return (char) input.read();
		}
	}
	
	public int peek() throws IOException
	{
		if (lastByte != null)
		{
			return Byte.toUnsignedInt(lastByte);
		}
		else
		{
			if (input.available() > 0)
			{
				lastByte = (byte) input.read();
				return Byte.toUnsignedInt(lastByte);
			}
			else
			{
				return -1;
			}
		}
	}
	
	public Character peekCharacter() throws IOException
	{
		int t = peek();
		if (t >= 0 && t != 255)
			return (char) t;
		else
			return null;
	}
	
	private void sendPair(TelnetOption a, TelnetOption b) throws IOException
	{
		TelnetOption[] options = new TelnetOption[3];
		options[0] = TelnetOption.IAC;
		options[1] = a;
		options[2] = b;
		write(options);
		flush();
	}
	
	public void sendWill(TelnetOption option) throws IOException
	{
		set(serverWillMap, option, true);
		sendPair(TelnetOption.WILL, option);
	}
	
	public void sendWont(TelnetOption option) throws IOException
	{
		set(serverWillMap, option, false);
		set(serverDoesMap, option, true);
		sendPair(TelnetOption.WONT, option);
	}
	
	public void sendDo(TelnetOption option) throws IOException
	{
		System.out.println("Sending do");
		set(clientWillMap, option, true);
		sendPair(TelnetOption.DO, option);
	}
	
	public void sendDont(TelnetOption option) throws IOException
	{
		set(clientWillMap, option, false);
		set(clientDoesMap, option, false);
		sendPair(TelnetOption.DONT, option);
	}
	
	public void sendSubnegotation(TelnetOption option, byte[] data) throws IOException
	{
		TelnetOption[] options = new TelnetOption[3];
		options[0] = TelnetOption.IAC;
		options[1] = TelnetOption.SB;
		options[2] = option;
		write(options);
		write(data);
		
		options = new TelnetOption[2];
		options[0] = TelnetOption.IAC;
		options[1] = TelnetOption.SE;
		write(options);
		update();
	}
	
	protected byte[] readSubnegotiation() throws IOException
	{
		List<Byte> buf = new ArrayList<>();
		char c;
		boolean readIAC = false;
		
		do
		{
			c = rawRead();
			if (c == TelnetOption.IAC.asChar())
			{
				if (readIAC)
				{
					buf.add((byte) c);
					readIAC = false;
				}
				else
					readIAC = true;
			}
			else
				buf.add((byte) c);
		}
		while (!(readIAC == true && c == TelnetOption.SE.asChar()));
		
		byte[] arrb = new byte[buf.size()];
		
		for (int i = 0; i < buf.size(); i++)
			arrb[i] = buf.get(i);
		
		return arrb;
	}
	
	public void readTelnetOption() throws IOException
	{
		System.out.println("Reading telnet option");
		TelnetOption wants = TelnetOption.fromChar(rawRead());
		char optionType = rawRead();
		TelnetOption option = TelnetOption.fromChar(optionType);
		
		if (option == null)
		{
			System.err.println("Received unknown option number " + (int) optionType);
			
			char[] options = new char[3];
			options[0] = TelnetOption.IAC.asChar();
			options[2] = optionType;
			
			if (wants == TelnetOption.WILL)
				options[1] = TelnetOption.DONT.asChar();
			else if (wants == TelnetOption.DO)
				options[1] = TelnetOption.WONT.asChar();
			
			write(options);
			flush();
			return;
		}
		
		System.out.println("WANTS = " + wants.name());
		System.out.println("OPTION = " + option.name());
		
		if (wants == TelnetOption.WILL)
		{
			// The client is going to do something
			if (option == TelnetOption.NAWS)
			{
				if (get(clientWillMap, option))
				{
					System.out.println("NAWS enabled");
					set(clientDoesMap, option, true);
					set(hasNegotiatedMap, option, true);
				}
			}
			else
				sendDont(option);
		}
		else if (wants == TelnetOption.WONT)
		{
			// The client is not going to do something
			if (option == TelnetOption.NAWS)
			{
				if (get(clientWillMap, option))
				{
					set(clientDoesMap, option, false);
					set(hasNegotiatedMap, option, true);
				}
			}
		}
		else if (wants == TelnetOption.DO)
		{
			// The client wants us to do something
			if (!get(hasNegotiatedMap, option))
			{
				if (get(serverWillMap, option) == true)
					sendWill(option);
				else
					sendWont(option);
			}
			else
				return;
		}

		else if (wants == TelnetOption.DONT)
		{
			if (option == TelnetOption.ECHO)
			{
				if (get(serverWillMap, option) == true)
				{
					System.out.println("Client wants us to not to ECHO, but we do");
					if (get(hasNegotiatedMap, option) == false)
						sendWill(option);
				}
			}
			else
			{
				if (!get(hasNegotiatedMap, option))
				{
					set(hasNegotiatedMap, option, true);
					sendWont(option);
				}
			}
		}
		else if (wants == TelnetOption.SB)
		{
			byte[] buf = readSubnegotiation();
			if (option == TelnetOption.NAWS)
			{
				screenWidth = Byte.toUnsignedInt(buf[0]) << 8 | Byte.toUnsignedInt(buf[1]);
				screenHeight = Byte.toUnsignedInt(buf[2]) << 8 | Byte.toUnsignedInt(buf[3]);
				System.out.println("New window size = " + screenWidth + " x " + screenHeight);
			}
		}
		else
		{
			System.err.println("Unknown WANTS type " + wants.name());
		}
	}

	public void setWindowSizeOption(boolean enabled) throws IOException
	{
		set(hasNegotiatedMap, TelnetOption.NAWS, false);
		if (enabled)
			sendDo(TelnetOption.NAWS);
		else
			sendDont(TelnetOption.NAWS);
	}
	
	public int getScreenWidth()
	{
		lastScreenWidth = screenWidth;
		return screenWidth;
	}
	
	public int getScreenHeight()
	{
		lastScreenHeight = screenHeight;
		return screenHeight;
	}
	
	public boolean screenSizeChanged()
	{
		if (lastScreenWidth == screenWidth && lastScreenHeight == screenHeight)
		{
			return false;
		}
		else
		{
			lastScreenWidth = screenWidth;
			lastScreenHeight = screenHeight;
			return true;
		}
	}
	
	public void enableServerSideEcho(boolean state) throws IOException
	{
		set(hasNegotiatedMap, TelnetOption.ECHO, false);
		if (state)
		{
			System.out.println("Sending WILL ECHO");
			sendWill(TelnetOption.ECHO);
		}
		else
			sendWont(TelnetOption.ECHO);
	}
	
	public void enableSurpressGoAhead(boolean state) throws IOException
	{
		set(hasNegotiatedMap, TelnetOption.SGA, false);
		if (state)
			sendWill(TelnetOption.SGA);
		else
			sendWont(TelnetOption.SGA);
	}
	
	public void enableLinemode(boolean state) throws IOException
	{
		set(hasNegotiatedMap, TelnetOption.LINEMODE, false);
		if (state)
			sendWill(TelnetOption.LINEMODE);
		else
			sendWont(TelnetOption.LINEMODE);
	}
	
	public void enableCharacterMode(boolean state) throws IOException
	{
		if (state)
		{
			enableServerSideEcho(true);
			enableSurpressGoAhead(true);
			enableLinemode(false);
		}
		else
		{
			enableServerSideEcho(false);
			enableSurpressGoAhead(false);
			enableLinemode(true);
		}
	}
	
	public String getId()
	{
		return socket.getInetAddress().toString() + ":" + socket.getPort();
	}
	
	public void close()
	{
		try
		{
			if (!socket.isClosed())
				socket.close();
		}
		catch (IOException e)
		{
			System.err.println("Failed to close socket for client " + getId());
			System.err.println("Reason: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public boolean isClosed()
	{
		return socket.isClosed();
	}
}


















