package net.kiigo.web.apps.utils.jspformatter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * 统计行号的输出流
 * @author Coollf
 *
 */
public class NewLineCompressedBufferedWriter extends BufferedWriter
{
  private int newLines = 0;
  private static String lineSep = null;

  public NewLineCompressedBufferedWriter(Writer writer) {
    super(writer);
    lineSep = System.getProperty("line.separator");
  }

  public void write(String line) throws IOException {
    super.write(line);
    if (!line.equals(lineSep))
      this.newLines = 0;
  }

  public void newLine() throws IOException
  {
    if (this.newLines < 2) {
      super.newLine();
      this.newLines += 1;
    }
  }
}