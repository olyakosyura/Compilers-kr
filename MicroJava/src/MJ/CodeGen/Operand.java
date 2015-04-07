/* MicroJava Code Operands  (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
   =======================
An Operand stores the attributes of a value during code generation.
*/

package MJ.CodeGen;
import MJ.*;
import MJ.SymTab.*;

public class Operand {
	public static final int  // item kinds
	  Con    = 0,
	  Local  = 1,
	  Static = 2,
	  Stack  = 3,
	  Fld    = 4,
	  Elem   = 5,
	  Meth   = 6,
          None   = -1;

        /**
         * the kind of the operand: Con, Local, Static, Stack, Fld, Elem, Meth
         */
	public int    kind;

        /**
         * The item type.
         * 
         * @value Struct
         */
	public Struct type;	// item type

        /**
         * For a method, the related object.
         *
         * @value Obj
         */
	public Obj    obj;

        /**
         * For constant, its value.
         *
         * @value int
         */
	public int    val;

        /**
         * For Local, Static, Method and Fld, the address.
         *
         * @value int
         */
	public int    adr;  // Local, Static, Fld, Meth: address

	public Operand(Obj o) {
		type = o.type; val = o.val; adr = o.adr; kind = Stack; // default
		switch (o.kind) {
			case Obj.Con:
				kind = Con; break;
			case Obj.Var:
				if (o.level == 0) kind = Static; else kind = Local;
				break;
			case Obj.Meth:
				kind = Meth; obj = o; break;
			case Obj.Type: break; // consider it is already on the stack
			default:
				Parser.error("wrong kind of identifier"); break;
		}
	}

	public Operand(int val) {
		kind = Con; this.val = val; type = Tab.intType;
	}

	public Operand(int kind, int val, Struct type) {
		this.kind = kind; this.val = val; this.type = type;
	}

        @Override
        public String toString(){
            return "Operand, of kind " + this.kind + " at address " + this.adr;
        }

}