package be.seeseemelk.cellnet;

public enum TelnetOption
{
	DO(253), DONT(254),
	WILL(251), WONT(252),
	
	IAC(255), // Data byte 255
	
	IP(244), // Interrupt process
	AO(245), // Abort output
	AYT(246), // Are you there
	EC(247), // Erase character
	EL(248), // Erase line
	BRK(243), // Break
	GA(249), // Go ahead
	
	NULL(0), //NULL
	LF(10), // Line feed
	CR(13), // Carriage return
	BELL(7), // Bing!
	BS(8), // Back space
	HT(9), // Horizontal tab
	VT(11), // Vertical tab
	FF(12), // Form feed
	
	SB(250), // Subnegotiation begin
	SE(240), // Subnegotiation end
	NOP(241), // No operation
	DM(242), // Data mark
	
	NAWS(31), // Negotiate About Window Size
	ECHO(1), // Should echo
	SGA(3), // Surpress go ahead
	LINEMODE(34), // Linemode
	;
	
	private int numVal;
	TelnetOption(int numVal)
	{
		this.numVal = numVal;
	}
	
	public int getNumVal()
	{
		return numVal;
	}
	
	public char asChar()
	{
		return (char) numVal;
	}
	
	public static TelnetOption fromChar(char value)
	{
		for (TelnetOption option : values())
		{
			if (option.asChar() == value)
				return option;
		}
		return null;
	}
	
	/*public static final int WILL;
	public static final int WONT;
	public static final int IP;
	public static */
}
