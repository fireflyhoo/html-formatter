package net.kiigo.web.apps.html_formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;

import junit.framework.TestCase;
import net.kiigo.web.apps.utils.jspformatter.JSPFormatter;
import net.kiigo.web.apps.utils.jstyle.JSFormatter;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
   public void testHtmlFormatter() throws IOException{
	   String context = laodFileContext("9_qE2yKiRCG1kzgjP90eWA.html");
	   JSPFormatter formatter = new JSPFormatter(context, 4, 120);
	   System.out.println(formatter.format());
   }
   
   
   public void testJsFormatter() throws IOException{
	   JSFormatter formatter  = new JSFormatter();
	   
	   PrintWriter printStream = new PrintWriter("a.js");
	   
	  formatter.format(new BufferedReader(new StringReader(laodFileContext("ueditor.all.min.js"))),printStream);
	   
   }
   
   public String laodFileContext(String path) throws IOException{
		File file = new File(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
		StringBuffer buffer = new StringBuffer();
		String leng = reader.readLine();
		while(leng!=null){
			buffer.append(leng);
			leng = reader.readLine();
		}
		reader.close();
		return buffer.toString();
   }
}
