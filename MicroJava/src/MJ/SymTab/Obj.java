/* MicroJava Symbol Table Objects  (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
==============================
Every named object in a program is stored in an Obj node.
Every scope has a list of objects declared in this scope.
 */
package MJ.SymTab;

public class Obj {

    public static final int // object kinds
            Con = 0,
            Var = 1,
            Type = 2,
            Meth = 3,
            Prog = 4;
    public int kind;		// Con, Var, Type, Meth, Prog
    public String name;		// object name
    public Struct type;	 	// object type
    public int val;             // Con: value
    public int adr;             // Var, Meth: address
    public int level;           // Var: declaration level
    public int nPars;           // Meth: number of parameters
    public Obj locals;          // Meth: parameters and local objects
    public Obj next;		// next local object in this scope

    public Obj(int kind, String name, Struct type) {
        this.kind = kind;
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString(){
        return this.name;
        /*
        StringBuilder sb = new StringBuilder();
        sb.append(" name: " + this.name);
        sb.append(" kind: " + this.kind);
        sb.append(" type: " + this.type);

        if (this.kind == Con){
            sb.append(" val: " + this.val);
        }
        if (this.kind == Meth || this.kind == Var){
            sb.append(" address: " + this.adr);
        }
        if (this.kind == Var){
            sb.append(" level: " + this.level);
        }
        if (this.kind == Meth){
            sb.append(" number of parameters: " + this.nPars);
        }

        return sb.toString();
         */
    }
    
}
