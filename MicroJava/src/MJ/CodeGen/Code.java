/* MicroJava Code Generator  (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
========================
This class holds the code buffer with its access primitives get* and put*.
It also holds methods to load operands and to generate complex instructions
such as assignments and jumps.
 */
package MJ.CodeGen;

import java.io.*;
import MJ.*;
import MJ.SymTab.*;

public class Code {

    public static final int // instruction codes
            load = 1,
            load0 = 2,
            load1 = 3,
            load2 = 4,
            load3 = 5,
            store = 6,
            store0 = 7,
            store1 = 8,
            store2 = 9,
            store3 = 10,
            getstatic = 11,
            putstatic = 12,
            getfield = 13,
            putfield = 14,
            const0 = 15,
            const1 = 16,
            const2 = 17,
            const3 = 18,
            const4 = 19,
            const5 = 20,
            const_m1 = 21,
            const_ = 22,
            add = 23,
            sub = 24,
            mul = 25,
            div = 26,
            rem = 27,
            neg = 28,
            shl = 29,
            shr = 30,
            new_ = 31,
            newarray = 32,
            aload = 33,
            astore = 34,
            baload = 35,
            bastore = 36,
            arraylength = 37,
            pop = 38,
            jmp = 39,
            jeq = 40,
            jne = 41,
            jlt = 42,
            jle = 43,
            jgt = 44,
            jge = 45,
            call = 46,
            return_ = 47,
            enter = 48,
            exit = 49,
            read = 50,
            print = 51,
            bread = 52,
            bprint = 53,
            trap = 54;
    public static final int // compare operators
            eq = 0,
            ne = 1,
            lt = 2,
            le = 3,
            gt = 4,
            ge = 5;
    private static int[] inverse = {ne, eq, ge, gt, le, lt};
    private static final int bufSize = 8192;
    public static byte[] buf;
    public static int pc;	// next free byte in code buffer
    public static int mainPc;	// pc of main function (set by parser)
    public static int dataSize;	// length of static data in words (set by parser)
    public static final Operand noOp = new Operand(Operand.None, 0, Tab.noType);

    //--------------- code buffer access ----------------------
    public static void put(int x) {
        if (pc >= bufSize) {
            if (pc == bufSize) {
                Parser.error("program too large");
            }
            pc++;
        } else {
            buf[pc++] = (byte) x;
        }
    }

    public static void put2(int x) {
        put(x >> 8);
        put(x);
    }

    public static void put2(int pos, int x) {
        int oldpc = pc;
        pc = pos;
        put2(x);
        pc = oldpc;
    }

    public static void put4(int x) {
        put2(x >> 16);
        put2(x);
    }

    public static int get(int pos) {
        return buf[pos];
    }

    //----------------- instruction generation --------------
    /**
     * Load an operand on the estack regardless its type.
     *
     * @param x The operand to load
     */
    public static void load(Operand x) {
        switch (x.kind) {
            case Operand.Con:
                if (x.type == Tab.nullType) {
                    put(const0);
                } else if (0 <= x.val && x.val <= 5) {
                    put(const0 + x.val);
                } else if (x.val == -1) {
                    put(const_m1);
                } else {
                    put(const_);
                    put4(x.val);
                }
                break;
            case Operand.Elem:
                // for arrays, the address and index are already o the stack
                if (x.type.kind == Struct.Char) {
                    put(baload);
                } else {
                    put(aload);
                }
                break;
            case Operand.Fld:
                // we do already have the address of the object, so
                // put the field x on the stack
                put(getfield);
                put2(x.adr);
                break;
            case Operand.Local:
                if (0 <= x.adr && x.adr <= 3) {
                    put(load0 + x.adr);
                } else {
                    put(load);
                    put(x.adr);
                }
                break;
            case Operand.Static:
                put(getstatic);
                put2(x.adr);
                break;
            case Operand.Stack:
                break;
            default:
                Parser.error("cannot load this value");
        }

        x.kind = Operand.Stack; // change the type to the stack, to mark it as loaded.
    }

    // Generate an assignment x = y
    public static void assign(Operand x, Operand y) {
        load(y);
        switch (x.kind) {
            case Operand.Static:
                put(putstatic);
                put2(x.adr);
                break;
            case Operand.Local:
                put(store);
                put(x.adr);
                break;
            case Operand.Fld:
                // if it is a field, the object address and the value are already on the stack
                put(putfield);
                put2(x.adr);
                break;
            case Operand.Elem:
                // if it is an element, the address, index and value are on the stack
                // we also have to check if we are dealing with strings
                if (y.type.kind == Struct.Char)
                    put(bastore);
                else
                    put(astore);
                break;
            default:
                Parser.error("assignation is not supported with this operand");
        }
    }

    /**
     * Unconditional jump
     *
     * @param adr
     * @return the address to potentially fixup
     */
    public static int putJump(int adr) {
        put(jmp);
        int fixup = pc;
        put2(adr);
        
        return fixup;
    }

    /**
     * Conditional jump if op is false
     * 
     * @param op
     * @param adr
     * @return the address to potentially fixup
     */
    public static int putFalseJump(int op, int adr) {
        put(jeq + inverse[op]);
        int fixup = pc;
        put2(adr);
        
        return fixup;
    }

    // patch jump target at adr so that it jumps to the current pc
    public static void fixup(int adr) {
        // store the actual value of pc
        put2(adr, pc);
    }

    //------------------------------------
    // initialize code buffer
    public static void init() {
        buf = new byte[bufSize];
        pc = 0;
        mainPc = -1;
        dataSize = 0;
    }

    // Write the code buffer to the output stream
    public static void write(OutputStream s) {
        int codeSize;
        try {
            codeSize = pc;
            Decoder.decode(buf, 0, codeSize);
            put('M');
            put('J');
            put4(codeSize);
            put4(dataSize);
            put4(mainPc);
            s.write(buf, codeSize, pc - codeSize);	// header
            s.write(buf, 0, codeSize);			// code
            s.close();
        } catch (IOException e) {
            Parser.error("cannot write code file");
        }
    }

        public static String typeName(Operand op){
            if (op.type == null){
                return "Null";
            }
            return Struct.kindNames[op.type.kind];
        }

        public static String typeName(Obj o){
            return Struct.kindNames[o.kind];
        }
}
