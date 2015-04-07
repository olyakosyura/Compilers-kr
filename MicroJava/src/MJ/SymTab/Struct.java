/* MicroJava Type Structures  (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
=========================
A type structure stores the type attributes of a declared object.
 */
package MJ.SymTab;

public class Struct {

    public static final int // structure kinds
            None = 0,
            Int = 1,
            Char = 2,
            Arr = 3,
            Class = 4;

    public static String[] kindNames = {"none", "int", "char", "arr", "class"};

    /**
     * kind can be Struct. {None, Int, Char, Arr, Class}
     */
    public int kind;
    public Struct elemType; // Arr: element type
    public int nFields;     // Class: number of fields
    public Obj fields;      // Class: fields

    public Struct(int kind) {
        this.kind = kind;
    }

    public Struct(int kind, Struct elemType) {
        this.kind = kind;
        this.elemType = elemType;
    }

    // Checks if this is a reference type
    public boolean isRefType() {
        return kind == Class || kind == Arr;
    }

    // Checks if two types are equal
    public boolean equals(Struct other) {
        if (kind == Arr) {
            return other.kind == Arr && other.elemType == elemType;
        } else {
            return other == this;
        }
    }

    // Checks if two types are compatible (e.g. in a comparison)
    public boolean compatibleWith(Struct other) {
        return this.equals(other)
                || this == Tab.nullType && other.isRefType()
                || other == Tab.nullType && this.isRefType();
    }

    // Checks if an object with type "this" can be assigned to an object with type "dest"
    public boolean assignableTo(Struct dest) {
        if (dest == null){
            return false;
        }
        return this.equals(dest)
                || this == Tab.nullType && dest.isRefType()
                || this.kind == Arr && dest.kind == Arr && dest.elemType == Tab.noType;
    }

    @Override
    public String toString(){
        return new Integer(this.kind).toString();
    }
}