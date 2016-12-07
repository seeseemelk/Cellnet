package be.seeseemelk.cellnet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileLoader
{
	public static String loadFile(File file)
	{
		StringBuffer buffer = new StringBuffer();
		byte[] buf = new byte[1024];
		char[] cbuf = new char[1024];
		
		try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file)))
		{
			int read;
			do
			{
				read = input.read(buf);
			
				if (read > 0)
				{
					for (int i = 0; i < read; i++)
						cbuf[i] = (char) buf[i];
					
					buffer.append(cbuf, 0, read);
				}
			}
			while (read == 1024);
		}
		catch (IOException e)
		{
			System.err.println("IOException while reading from the file " + file.getName());
			e.printStackTrace();
		}
		
		return buffer.toString();
	}

	public static void saveFile(File file, String message)
	{
		try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file)))
		{
			output.write(message.getBytes());
			output.close();
		}
		catch (IOException e)
		{
			System.err.println("IOException while writing file to disk");
			System.err.println("Reason: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

















