/* MicroJava Scanner (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
=================
 */
package MJ;

import java.io.*;
import java.util.Arrays;

public class Scanner {

    private static final char eofCh = '\u0080';
    private static final char eol = '\n';
    private static final int // token codes
            none = 0,
            ident = 1,
            number = 2,
            charCon = 3,
            plus = 4,
            minus = 5,
            times = 6,
            slash = 7,
            rem = 8,
            eql = 9,
            neq = 10,
            lss = 11,
            leq = 12,
            gtr = 13,
            geq = 14,
            assign = 15,
            semicolon = 16,
            comma = 17,
            period = 18,
            lpar = 19,
            rpar = 20,
            lbrack = 21,
            rbrack = 22,
            lbrace = 23,
            rbrace = 24,
            class_ = 25,
            else_ = 26,
            final_ = 27,
            if_ = 28,
            new_ = 29,
            print_ = 30,
            program_ = 31,
            read_ = 32,
            return_ = 33,
            void_ = 34,
            while_ = 35,
            eof = 36;
    private static final String key[] = { // sorted list of keywords
        "class", "else", "final", "if", "new", "print",
        "program", "read", "return", "void", "while"
    };
    private static final int keyVal[] = {
        class_, else_, final_, if_, new_, print_,
        program_, read_, return_, void_, while_
    };
    private static char ch;			// lookahead character
    public static int col;			// current column
    public static int line;		// current line
    private static int pos;			// current position from start of source file
    private static Reader in;  	// source file reader
    private static char[] lex;	// current lexeme (token string)

    //----- ch = next input character
    private static void nextCh() {
        try {
            ch = (char) in.read();
            col++;
            pos++;
            if (ch == eol) {
                line++;
                col = 0;
            } else if (ch == '\uffff') {
                ch = eofCh;
            }
        } catch (IOException e) {
            ch = eofCh;
        }
    }

    //--------- Initialize scanner
    public static void init(Reader r) {
        in = new BufferedReader(r);
        lex = new char[64];
        line = 1;
        col = 0;
        nextCh();
    }

    //---------- Return next input token
    public static Token next() {
        while (ch <= ' ') {
            nextCh(); // skip blanks, tabs, eols
        }
        Token t = new Token();
        t.line = line;
        t.col = col;
        switch (ch) {
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
                readName(t);
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                readNumber(t);
                break;
            case ';':
                nextCh();
                t.kind = semicolon;
                break;
            case '.':
                nextCh();
                t.kind = period;
                break;
            case eofCh:
                t.kind = eof;
                break; // no nextCh() any more
            case '=': // Can be an assignment or a comparaison
                nextCh();
                if (ch == '=') {
                    nextCh();
                    t.kind = eql;
                } else {
                    t.kind = assign;
                }
                break;
            case '/': // Can be a division or a comment
                nextCh();
                if (ch == '/') {
                    do {
                        nextCh();
                    } while (ch != '\n' && ch != eofCh);
                    t = next(); // call scanner recursively
                } else {
                    t.kind = slash;
                }
                break;
            case '+':
                nextCh();
                t.kind = plus;
                break;
            case '-':
                nextCh();
                t.kind = minus;
                break;
            case '*':
                nextCh();
                t.kind = times;
                break;
            case '%':
                nextCh();
                t.kind = rem;
                break;
            case ',':
                nextCh();
                t.kind = comma;
                break;
            case '(':
                nextCh();
                t.kind = lpar;
                break;
            case ')':
                nextCh();
                t.kind = rpar;
                break;
            case '[':
                nextCh();
                t.kind = lbrack;
                break;
            case ']':
                nextCh();
                t.kind = rbrack;
                break;
            case '{':
                nextCh();
                t.kind = lbrace;
                break;
            case '}':
                nextCh();
                t.kind = rbrace;
                break;
            case '!': // Begining of !=
                nextCh();
                if (ch == '=') {
                    nextCh();
                    t.kind = neq;
                } else {
                    t.kind = none; // error exclamation mark cannot be alone
                }
                break;
            case '<': // Can be < or <=
                nextCh();
                if (ch == '=') {
                    nextCh();
                    t.kind = leq;
                } else {
                    t.kind = lss;
                }
                break;
            case '>': // Can be > or >=
                nextCh();
                if (ch == '=') {
                    nextCh();
                    t.kind = geq;
                } else {
                    t.kind = gtr;
                }
                break;
            case '\'':
                readConstChar(t);
                break;
            default:
                nextCh();
                t.kind = none;
                break;
        }
        return t;
    } // ch holds the next character that is still unprocessed

    /*
     * Read the name of an ident or of a keyword
     */
    private static void readName(Token t) {

        // Append the character to a string that will be assigned to the token
        StringBuilder sb = new StringBuilder();
        sb.append(ch);

        nextCh();

        while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
            // Then append it to the string
            sb.append(ch);

            // Load next character
            nextCh();
        }

        // Flush the string builder to a string in the string value of the token
        t.string = sb.toString();

        // Recognise keywords and set type according to it

        int kw_index = Arrays.binarySearch(key, t.string);
        if (kw_index >= 0) {
            // Is a keyword
            t.kind = keyVal[kw_index];

        } else {
            // Not a keyword so it is an ident
            t.kind = ident;
        }
    }

    /*
     * Read a numeric value
     */
    private static void readNumber(Token t) {
        // Append the character to a string that will be assigned to the token
        StringBuilder sb = new StringBuilder();
        sb.append(ch);

        for (;;) {
            // Load next character
            nextCh();

            // Check if it is a simple ascii letter or digit
            if (ch >= '0' && ch <= '9') {
                // Then append it to the string
                sb.append(ch);
            } else {
                break;
            }
        }
        // If the number is too large to be handled per java, return none
        try {
            t.val = Integer.parseInt(sb.toString());
            t.kind = number;
        } catch(NumberFormatException e){
            t.kind = none;
        }
    }

    private static void readConstChar(Token t) {
        // Append the character to a string that will be assigned to the token
        StringBuilder sb = new StringBuilder();
        sb.append(ch); // quote
        nextCh(); // read the second char.


        while (ch != '\'' && ch != '\n' && ch != eof) {
            sb.append(ch);
            nextCh();
        }
        sb.append(ch); // string should be of the form '[\]x'

        if (ch == '\'') {

            t.string = sb.toString();

            if (t.string.length() == 3) { // it's a normal char

                t.val = t.string.charAt(1);
                t.kind = charCon;

            } else if (t.string.length() == 4) { // it's a special char
                // should begin with a backslash
                if (t.string.charAt(1) == '\\') {
                    switch (t.string.charAt(2)) {
                        case 'r':
                            t.val = '\r';
                            t.kind = charCon;
                            break;
                        case 't':
                            t.val = '\t';
                            t.kind = charCon;
                            break;
                        case 'n':
                            t.val = '\n';
                            t.kind = charCon;
                            break;
                        default:
                            t.kind = none;

                    }
                }
            } else {
                t.kind = none; // a char should be either 3 or 4 len
            }

        } else {
            t.kind = none; // a char should be closed.
        }

        nextCh();
    }
}
