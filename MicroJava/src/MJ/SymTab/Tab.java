/* MicroJava Symbol Table  (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
======================
This class manages scopes and inserts and retrieves objects.
 */
package MJ.SymTab;

import MJ.*;

public class Tab {

    public static Scope curScope;       // current scope
    public static int curLevel;         // nesting level of current scope
    public static boolean isClassScope; // Flag to know if in a class scope

    // predefined types
    public static Struct intType;
    public static Struct charType;
    public static Struct nullType;
    public static Struct noType;

    // predefined objects
    public static Obj chrObj;
    public static Obj ordObj;
    public static Obj lenObj;
    public static Obj noObj;

    public static boolean printSymbolTable = false;

    private static void error(String msg) {
        Parser.error(msg);
    }

    private static void print(String msg) {
        if (printSymbolTable){
            System.out.println(msg);
        }
    }

    /**
     * Each time we need to go in a nested scope, keep track of
     * the previous one and increase the coresponding level variable.
     *
     * This method create a new scope node.
     */
    public static void openScope() {
        Scope newScope = new Scope();

        newScope.outer = curScope;
        curScope = newScope;

        curLevel++;
    }

    /**
     * Close the current scope, and restore the outer one.
     */
    public static void closeScope() {
        curScope = curScope.outer;
        curLevel--;
    }

    /**
     * Create a new object with the given information (kind, name and type).
     *
     * @param kind The structure's kind. Can be Con, Var, Type, Meth, Prog
     * @param name The identifier
     * @param type the structure (Var, Con, Type, Meth, Prog). Matches object kind.
     * @return Obj the newly created object
     */
    public static Obj insert(int kind, String name, Struct type) {
        // create the object node in the current scope

        Obj newObj = new Obj(kind, name, type);

        // Limit number of the scope's variables or constants
        // defined later
        int numberLimit = 0;

        // if it's a var or a const then we should manage its address and level.
        if (kind == Obj.Var || kind == Obj.Con) {
 
            // Ensure the maximum number of variables per scope is not reached
            // It would cause the variables or constants to be stored all
            // at the address once over the address capacity
            if(curLevel == 1) { // Global scope
                // Is a global
                numberLimit = 32768;
            } else if (curLevel == 2) { // Local or field scope
                if (isClassScope) {
                    // Is a field
                    numberLimit = 32768;
                } else {
                    // Is a local
                    numberLimit = 128;
                }
            }

            // Actually check the limit. We check this limit only for known maximum numbers
            if (numberLimit != 0 && curScope.nVars > numberLimit) {
                error("Maximum number of variables or constants exceeded (" + numberLimit + ")");
            }

            // Set the address and level
            newObj.adr = curScope.nVars;
            curScope.nVars++;
            newObj.level = curLevel;
        }

        // append the object node
        // find the last object, and append the object node to it.
        Obj lastObj = curScope.locals;
        if (lastObj == null) {
            curScope.locals = newObj;
        } else {
            while (lastObj.next != null) {
                lastObj = lastObj.next;
            }
            lastObj.next = newObj;
        }

        print("Inserted " + newObj);
        
        return newObj;
    }

    /**
     * Browse the different scopes and try to find the given name
     * If an object with the name is found, return it, return a dummy
     * object otherwise (noObj).
     * 
     * @param name the name to find
     */
    public static Obj find(String name) {
        // iter over the scopes, from the current to the root.y
        for (Scope scope = curScope; scope != null; scope = scope.outer) {
            for (Obj obj = scope.locals; obj != null; obj = obj.next) {
                if (obj.name.equals(name)) {

                    if (printSymbolTable)
                        print("Found " + obj);
                    return obj; // found !
                }
            }
        }
        error(name + " is not defined for this scope.");
        return noObj; // object not found
    }

    /*
     * Retrieve a class field with the given name from the fields of "type"
     * 
     * @param String the name of the member
     * @param Struct the class structure to search fields into
     */
    public static Obj findField(String name, Struct type) {

        if(type.kind == Struct.Class){
            // look for the field "name"
            for (Obj field = type.fields; field != null; field = field.next){
                if (field.name.equals(name)){
                    if (printSymbolTable)
                        print("Found field " + field);
                    return field;
                }
            }
            error("no member named " + name);
        } else {
            error("You can't access a field of a non class");
        }
        return noObj; // object not found
    }

    //---------------- methods for dumping the symbol table --------------
    public static void dumpStruct(Struct type) {
        String kind;
        switch (type.kind) {
            case Struct.Int:
                kind = "Int  ";
                break;
            case Struct.Char:
                kind = "Char ";
                break;
            case Struct.Arr:
                kind = "Arr  ";
                break;
            case Struct.Class:
                kind = "Class";
                break;
            default:
                kind = "None";
        }
        System.out.print(kind + " ");
        if (type.kind == Struct.Arr) {
            System.out.print(type.nFields + " (");
            dumpStruct(type.elemType);
            System.out.print(")");
        }
        if (type.kind == Struct.Class) {
            System.out.println(type.nFields + "<<");
            for (Obj o = type.fields; o != null; o = o.next) {
                dumpObj(o);
            }
            System.out.print(">>");
        }
    }

    public static void dumpObj(Obj o) {
        String kind;
        switch (o.kind) {
            case Obj.Con:
                kind = "Con ";
                break;
            case Obj.Var:
                kind = "Var ";
                break;
            case Obj.Type:
                kind = "Type";
                break;
            case Obj.Meth:
                kind = "Meth";
                break;
            default:
                kind = "None";
        }
        System.out.print(kind + " " + o.name + " " + o.val + " " + o.adr + " " + o.level + " " + o.nPars + " (");
        dumpStruct(o.type);
        System.out.println(")");
    }

    public static void dumpScope(Obj head) {
        System.out.println("--------------");
        for (Obj o = head; o != null; o = o.next) {
            dumpObj(o);
        }
        for (Obj o = head; o != null; o = o.next) {
            if (o.kind == Obj.Meth || o.kind == Obj.Prog) {
                dumpScope(o.locals);
            }
        }
    }

    /**
     * Initialise the symbol table and create the universe scope.
     */
    public static void init() {
        curScope = new Scope();
        curScope.outer = null;
        curLevel = -1;

        // create predeclared types
        intType = new Struct(Struct.Int);
        charType = new Struct(Struct.Char);
        nullType = new Struct(Struct.Class);
        noType = new Struct(Struct.None);
        noObj = new Obj(Obj.Var, "???", noType);

        // create predeclared objects
        insert(Obj.Type, "int", intType);
        insert(Obj.Type, "char", charType);
        insert(Obj.Con, "null", nullType);
        chrObj = insert(Obj.Meth, "chr", charType);
        chrObj.locals = new Obj(Obj.Var, "i", intType);
        chrObj.nPars = 1;
        ordObj = insert(Obj.Meth, "ord", intType);
        ordObj.locals = new Obj(Obj.Var, "ch", charType);
        ordObj.nPars = 1;
        lenObj = insert(Obj.Meth, "len", intType);
        lenObj.locals = new Obj(Obj.Var, "a", new Struct(Struct.Arr, noType));
        lenObj.nPars = 1;
    }
}
