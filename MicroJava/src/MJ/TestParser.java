/* MicroJava Parser Tester used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
   ========================
   Place this file in a subdirectory MJ
   Compile with
     javac MJ\Scanner.java MJ\Parser.java MJ\TestParser.java
   Run with
     java MJ.TestParser <inputFileName>
*/
package MJ;

import java.io.*;
import MJ.CodeGen.*;
import MJ.SymTab.Tab;

public class TestParser {

	// Main method of the parser tester
	public static void main(String args[]) {
            Code.init();
            Tab.init();
            Tab.printSymbolTable = true;
            Parser.printErrors = false;
            Token t;
		if (args.length > 0) {
			String source = args[0];
			try {
				Scanner.init(new InputStreamReader(new FileInputStream(source)));
				Parser.parse();
				System.out.println(Parser.errors + " errors detected");
			} catch (IOException e) {
				System.out.println("-- cannot open input file " + source);
			}
		} else System.out.println("-- synopsis: java MJ.TestParser <inputfileName>");
	}

}