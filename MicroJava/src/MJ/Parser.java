/*  MicroJava Parser (HM 06-12-28) used as base
 *  then modified by Alexis Metaireau and Maxime Hardy (Feb.-Mar. 2011)
================
 */
package MJ;

import java.util.*;
import MJ.SymTab.*;
import MJ.CodeGen.*;

/**
 *
 * 
 */
public class Parser {

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
    private static final String[] name = { // token names for error messages
        "none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
        "==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
        "[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
        "program", "read", "return", "void", "while", "eof"
    };
    private static Token t;     // current token (recently recognized)
    private static Token la;	// lookahead token
    private static int sym;	// always contains la.kind
    public static int errors;   // error counter
    private static int errDist;	// no. of correctly recognized tokens since last error
    private static BitSet exprStart, statStart, statSeqFollow, declStart, declFollow, relopStart, syncStat;
    public static boolean printErrors = true;

    /**
     * Read the next token, and update the readahead token.
     *
     */
    private static void scan() {
        t = la;
        la = Scanner.next();
        sym = la.kind;
        errDist++;
    }

    private static void check(int expected, String message) {
        if (sym == expected) {
            scan();

        } else {
            error(name[expected] + " : " + message);
        }
    }

    private static void check(int expected) {
        if (sym == expected) {
            scan();
        } else {
            error(name[expected] + " expected");
        }
    }

    public static void error(String msg) { // syntactic error at token la
        if (errDist >= 3 && printErrors) {
            System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
            errors++;
        }
        errDist = 0;
    }

    /**
     * Topmost production.
     * 
     *      Program = "program" ident {ConstDecl | VarDecl | ClassDecl} "{" {MethodDecl} "}".
     *
     * Declares a global scope for the program.
     */
    private static void Program() {
        check(program_);
        check(ident);
        Tab.insert(Obj.Prog, t.string, Tab.noType);
        Tab.openScope(); // program scope

        for (;;) {
            if (sym == final_) {
                ConstDecl();
            } else if (sym == ident) {
                VarDecl();
            } else if (sym == class_) {
                ClassDecl();
            } else if (sym == lbrace || sym == eof) {
                break;
            } else {
                // print an error
                error("not a valid begining of a program. Trying to recover.");

                // try to recover
                while (sym != class_ && sym != lbrace && sym != eof) {
                    scan();
                }
            }
        }

        check(lbrace);
        while (sym == ident || sym == void_) {
            MethodDecl();
        }
        check(rbrace);
        // Tab.dumpScope(Tab.curScope.locals);
        Tab.closeScope(); // program scope
    }

    /**
     * Constant declarations.
     *
     *      ConstDecl =
     *      "final" Type ident "="
     *      (number | charConst) ";"
     *
     * Declares all the constants of the program in the symbol table.
     */
    private static void ConstDecl() {
        check(final_);
        Struct type = Type();
        check(ident);
        Obj obj = Tab.insert(Obj.Con, t.string, type);

        check(assign);

        if (sym == number) {
            scan();
            if (obj.type.kind == Struct.Int) {
                // store the int in the object.
                obj.val = t.val;

            } else {
                error(obj.name + " is not compatible with integer");
            }
        } else if (sym == charCon) {
            scan();
            // store the char in the object.Obj obj = Tab.insert(Obj.Con, t.string, type);
            if (obj.type.kind == Struct.Char) {
                obj.val = t.val;
            } else {
                error(obj.name + " is not compatible with char");
            }
        } else {
            error("Expected symbols : " + name[number] + " or " + name[charCon]);
        }

        Code.load(new Operand(obj));
        Code.put(Code.putstatic);
        Code.put2(obj.adr);

        check(semicolon);
    }

    /**
     * Variable declaration
     * 
     *      Type ident {"," ident } ";"
     *
     * Add all the global variables into the global scope symbol table.
     */
    private static void VarDecl() {
        Struct type = Type();
        check(ident);
        Tab.insert(Obj.Var, t.string, type);
        Code.dataSize++;

        while (sym == comma) {
            scan();
            check(ident);
            Tab.insert(Obj.Var, t.string, type);
            Code.dataSize++;
        }
        check(semicolon);
    }

    /**
     * Class declaration
     *
     *      "class" ident "{" {VarDecl} "}"
     *
     * Create a new scope for the class and defines the variables within it.
     */
    private static void ClassDecl() {
        check(class_);
        check(ident);
        Struct c = new Struct(Struct.Class);
        Tab.insert(Obj.Type, t.string, c);

        // We create a new scope here, in order to get all the declared variables
        Tab.openScope();
        Tab.isClassScope = true;
        check(lbrace);
        while (sym == ident) {
            VarDecl();
        }
        c.fields = Tab.curScope.locals; // attach members of the class to its struct
        c.nFields = Tab.curScope.nVars; // put the number of fields in the class struct as well

        //  Tab.dumpScope(Tab.curScope.locals);
        Tab.closeScope();
        Tab.isClassScope = false;
        
        check(rbrace);
    }

    /*
     * Method declaration.
     *
     *      (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block
     *
     * The method declaration creates a new scope and defines all the variables
     * within it.
     *
     * While closing the scope, attach the scope parameters to the method, to
     * use them later.
     */
    private static void MethodDecl() {
        // void return's type is attached to None structure.
        Struct returnType = Tab.noType;
        if (sym == ident) {
            returnType = Type();
        } else if (sym == void_) {
            scan();
        } else {
            error("Expected symbol: " + name[void_] + " or " + name[ident]);
        }
        check(ident);
        // add the method in the Symbol Table
        Obj method = Tab.insert(Obj.Meth, t.string, returnType);

        // store the name of the method for later use
        String methodName = t.string;

        // open a new scope for the method
        Tab.openScope();

        check(lpar);
        int nPars = 0;

        // store the address of the method we are defining
        int methodAdr = Code.pc;

        if (sym == ident) {
            nPars = FormPars();
        }

        if (methodName.equals("main")) {
            // if the main method is detected, define the main pointer to
            // the actual value.
            Code.mainPc = methodAdr;

            // check that the main method is well defined
            if (method.type != Tab.noType) {
                error("The main method must be void");
            }
            if (nPars != 0) {
                error("The main method should not have any argument");
            }

        }

        // define the address of the method as the current address
        method.adr = methodAdr;
        check(rpar);
        while (sym == ident) {
            VarDecl();
        }
        // and then enter the method
        Code.put(Code.enter);
        Code.put(nPars);
        Code.put(Tab.curScope.nVars);

        Block(method);

        /**
         * At the end of a method, we return if the type of the function is void
         * (noType). Otherwise, the method should have already returned, so a trap
         * will be put on the stack in order to raise a runtime error.
         *
         * The lines below will be reached only if the method does not already
         * have returned.
         */
        if (method.type == Tab.noType) {
            // it is a void method
            Code.put(Code.exit);
            Code.put(Code.return_);
        } else {
            // it should already have returned !
            Code.put(Code.trap);
            Code.put(1);
        }

        // Do not drop the scope parameters. Attach them to the method.
        method.locals = Tab.curScope.locals;
        method.nPars = nPars;

        Tab.closeScope();
    }

    /**
     * Parses formal parameters and insert them in the symbol table for later
     * retrieval.
     * 
     *      Type ident {"," Type ident}
     *
     * @return the number of parameters
     */
    private static int FormPars() {
        // there is always at least one parameter
        int count = 1;
        Struct type = Type();
        check(ident);
        Tab.insert(Obj.Var, t.string, type);

        while (sym == comma) {
            scan();
            type = Type();
            check(ident);
            Tab.insert(Obj.Var, t.string, type);
            count++;
        }
        return count;
    }

    /**
     * A block is a block of statements contained within braces
     * 
     *      "{" {Statement} "}"
     *
     * The recovery is done until the end of the statement (closing brace).
     */
    private static void Block(Obj method) {
        check(lbrace);
        for (;;) {
            if (statStart.get(sym)) {
                Statement(method);
            } else if (sym == rbrace || sym == eof) {
                // get out of the loop if no statement detected
                break;
            } else {
                // something is getting bad here, try to recover.
                error("invalid start of statement. Trying to recover.");
                while (!syncStat.get(sym) && sym != rbrace) {
                    scan();
                }
            }
        }
        check(rbrace);
    }

    /**
     * Statelement parses a statement. It can be any of the following:
     * 
     *      Designator ("=" Expr | ActPars) ";"
     *      | "if" "(" Condition ")" Statement ["else" Statement]
     *      | "while" "(" Condition ")" Statement
     *      | "return" [Expr] ";"
     *      | "read" "(" Designator ")" ";"
     *      | "print" "(" Expr ["," number] ")" ";"
     *      | Block
     *      | ";".
     *
     * The current method as an argument here: it allows to check
     * that the method returns the right types in the return statement.
     * 
     * @param Obj method
     */
    private static void Statement(Obj method) {
        // init
        int operator;

        if (!statStart.get(sym)) {
            error("invalid start of statement. Trying to recover.");
            // we check for the end of file, to consider it as a recovery point.
            while (!syncStat.get(sym)) {
                scan(); // skip the token.
            }
        }
        switch (sym) {
            // it's an assignation.
            case ident:
                Operand lOp = Designator(); // get the value of the left operand
                if (sym == assign) {
                    scan();
                    Operand rOp = Expr();

                    if (rOp.type != null && rOp.type.assignableTo(lOp.type)) {
                        Code.assign(lOp, rOp);
                    } else {
                        error(Code.typeName(lOp) + " is not compatible with "
                                + Code.typeName(rOp));
                    }

                } else if (sym == lpar) {
                    // put the parameters on the stack
                    ActPars(lOp);
                    Code.put(Code.call);
                    Code.put2(lOp.adr);
                    if (lOp.type != Tab.noType) {
                        Code.put(Code.pop);
                    }
                }
                check(semicolon);
                break;
            case if_:
                scan();
                check(lpar);
                operator = Condition();
                check(rpar);

                // depending on the operation, do the comparison
                int patchAdrIf = Code.putFalseJump(operator, 0);

                Statement(method);

                if (sym == else_) {
                    int patchAdrElse = Code.putJump(0);
                    Code.fixup(patchAdrIf);
                    scan();
                    Statement(method);
                    Code.fixup(patchAdrElse);
                } else {
                    Code.fixup(patchAdrIf);
                }
                break;
            case while_:
                scan();
                check(lpar);

                int adrWhile = Code.pc;
                operator = Condition();

                check(rpar);

                int fixupAdr = Code.putFalseJump(operator, 0);
                Statement(method);

                // jump to the beginning of the loop
                Code.putJump(adrWhile);

                // patch the jump address
                Code.fixup(fixupAdr);
                break;

            case return_:
                scan();
                if (exprStart.get(sym)) {
                    Operand ret = Expr();

                    // check that the returned operand is compatible with the method type
                    if (ret.type.compatibleWith(method.type)) {
                        // if it is, load it and return
                        Code.load(ret);
                        Code.put(Code.return_);
                    } else {
                        error("The method return type does not match the expected one");
                    }
                } else {
                    // if no expression is returned, the type of the method should be void
                    if (method.type == Tab.noType) {
                        Code.put(Code.exit);
                        Code.put(Code.return_);
                    } else {
                        error("the method should return something.");
                    }
                }
                check(semicolon);
                break;

            case read_:
                scan();
                check(lpar);
                Operand target = Designator();
                Operand value = null;

                if (target.type == Tab.charType) {
                    // call read for a char
                    Code.put(Code.bread);
                    value = new Operand(Operand.Stack, 0, Tab.charType);
                } else if (target.type == Tab.intType) {
                    // call read for an int
                    Code.put(Code.read);
                    value = new Operand(Operand.Stack, 0, Tab.intType);
                } else {
                    error("read can only output an int or a char (" + Code.typeName(target) + " given)");
                    return;
                }
                Code.assign(target, value);

                check(rpar);
                check(semicolon);
                break;

            case print_:
                scan();
                check(lpar);
                Operand var = Expr();
                int width = 1;

                if (sym == comma) {
                    scan();
                    check(number);
                    width = t.val;
                }

                // load the variable to print on the stack
                Code.load(var);
                // load the width on the stack
                Code.load(new Operand(width));

                if (var.type == Tab.charType) {
                    // print a char
                    Code.put(Code.bprint);
                } else if (var.type == Tab.intType) {
                    Code.put(Code.print);
                } else {
                    error("It is not possible to print a different type than char or int (found "
                            + Code.typeName(var) + ")");
                }

                check(rpar);
                check(semicolon);
                break;
            case lbrace:
                Block(method);
                break;
            case semicolon:
                scan();
                break;
            default:
                error(name[sym] + " is not a valid begining for a statement");
        }
    }

    /**
     * The designator matches any thing that can be used as a variable:
     * simple identifiers, object fields and array elements.
     *
     * In the cases of the field and the array element, the address of the
     * container is loaded on the stack and an operand containing the field
     * or element relative address is returned.
     *
     * The kind of the operand we return can be:
     *   - Operand.Fld
     *   - Operand.Elem
     *   - the type of the variable
     *
     * In the case of the simple identifier, an operand containing its
     * information is returned.
     *
     * Grammar:
     *
     *      ident {"." ident | "[" Expr "]"}
     * 
     * @return an operand to be loaded
     */
     private static Operand Designator() {
        check(ident);

        // load the identifier from the symbol table
        Obj obj = Tab.find(t.string);
        Operand op = new Operand(obj);

        for (;;) {
            if (sym == period) {
                // attribute access

                scan();

                // type checking
                if (op.type.kind == Struct.Class) {
                    check(ident);

                    /**
                     * Put the operand on the stack. It can be even a class
                     * or a previously loaded field / array.
                     */
                    Code.load(op);
                    Obj field = Tab.findField(t.string, op.type);

                    // change the operand with informations from the field
                    op.kind = Operand.Fld;
                    op.adr = field.adr;
                    op.type = field.type;

                } else {
                    error(obj.name + " is not a class");
                }

            } else if (sym == lbrack) {
                // array access

                scan();
                if (op.type.kind == Struct.Arr) {
                    Code.load(op); // push the address of the table on the stack
                    Operand expr = Expr(); // retrieve the value of the index

                    // add informations to the operand that will bubble-up
                    // because chars and ints are not used the same way
                    op.kind = Operand.Elem;

                    op.type = op.type.elemType;
                    op.val = expr.val;

                    if (expr.type == null || expr.type.kind != Struct.Int) {
                        // check for null in case the expr was not defined (array[] = xx;)
                        error("array indexes must be of type int");
                    }

                    check(rbrack);
                } else {
                    error(obj.name + " is not an array");
                }

            } else {
                break;
            }
        }
        return op;
    }

    /**
     * Method call.
     *
     * In this method, we do use f and a prefixes. They stands for
     * actual and formal. The formal ones are defined in the symbol
     * table whereas the actual ones are the one that are part of the
     * method call
     *
     * Here is what we are parsing:
     *      
     *      "(" [ Expr {"," Expr} ] ")"
     *
     * ActPars is always relative to a function. which is passed as the
     * first argument.
     *
     * When returning from this function, all the parameters are on the stack
     *
     * @param method the function we are working on.
     */
    private static void ActPars(Operand method) {
        check(lpar);

        // first of all, check that the given operand is a method
        if (method == null || method.obj == null) {
            error("Called object is not a method");
            return;
        } else if (method.kind != Operand.Meth) {
            error("Try to do a method call on an operand which is not a method (found "
                    + Code.typeName(method) + ") in method " + method.obj.name);
            return;
        } else {
            // Get the method argument defined in the symbol table
            Obj fPar = method.obj.locals;
            int fPars = method.obj.nPars;
            int aPars = 0;

            if (exprStart.get(sym)) {
                // iterate on the arguments we are passing to the function
                for (;;) {
                    Operand aPar = Expr();
                    Code.load(aPar);
                    aPars++;

                    // check that the retrieved parameter is compatible with
                    // the one defined in the symbol table

                    if (fPar != null) { // fPar is null at the end of the list

                        if (!aPar.type.assignableTo(fPar.type)) {
                            error("function parameter and definition are not compatible (found "
                                    + Code.typeName(aPar) + ", expected " + Code.typeName(fPar) + ")"
                                    + " in method " + method.obj.name);
                        }
                        fPar = fPar.next;
                    }


                    // loop until there is no more parameter
                    if (sym == comma) {
                        scan();
                    } else {
                        break;
                    }
                }
            }
            // at the end, check that the number of parameters is the expected one
            if (fPars > aPars) {
                error("too few parameters have been defined. " + fPars + " expected, got " + aPars);
            } else if (fPars < aPars) {
                error("too much parameters have been defined. " + fPars + " expected, got " + aPars);
            }
        }
        check(rpar);
    }

    /**
     * Conditions.
     *
     * Load the two operands and return the token code of the condition.
     * 
     * @return the tokenCode of the comparison
     */
    private static int Condition() {
        Operand lOp = Expr();
        int operator = Relop();
        Operand rOp = Expr();

        // load the two operands on the stack
        Code.load(lOp);
        Code.load(rOp);

        if ((lOp.type.kind == Struct.Class || lOp.type.kind == Struct.Arr)
                && !(operator == neq || operator == eql)) {
            // it should only be equality or inequality
            error("Comparison between Classes and arrays can only be equality or inequality");
        }
        return operator;
    }

    /**
     * ["-"] Term {Addop Term}.
     *
     * Returns an operand. The operand can or cannot be loaded, depending the
     * cases, so there is a need to call again Code.load with it.
     */
    private static Operand Expr() {
        Operand leftOp, rightOp;
        int operator;
        boolean isMinus = false;

        // detect if it is a minus sign
        if (sym == minus) {
            scan();
            isMinus = true;
        }

        // load the term
        leftOp = Term();

        // do the negation
        if (isMinus && leftOp.type.kind == Struct.Int) {

            if (leftOp.kind == Operand.Con) {
                // if it is an int, do the negation directly in the compiler
                leftOp.val = -leftOp.val;
            } else {
                // otherwise, load the operator on the stack and do the negation
                Code.load(leftOp);
                Code.put(Code.neg);
            }
        } else if (isMinus) {
            error("operand must be an int");
        } else {

            // if it is a simple operand, load it !
            Code.load(leftOp);
        }
        // at this point, the left operand is loaded on the stack.

        while (sym == minus || sym == plus) {
            operator = Addop();

            rightOp = Term();
            Code.load(rightOp); // add the right operand to the stack

            // add the operator on the stack
            Code.put(operator);
        }
        return leftOp;
    }

    /**
     * Term.
     * 
     *      Factor {Mulop Factor}
     *
     * Load the value of the term on the stack.
     */
    private static Operand Term() {
        Operand op;
        op = Factor();

        // load the factor on the stack
        Code.load(op);

        while (sym == times || sym == slash || sym == rem) {
            // multiplication and division

            Operand rOp;
            int operator = Mulop();
            rOp = Factor();

            // check that the type of both operand is int
            if (op.type == Tab.intType && rOp.type == Tab.intType) {
                // load the two operands on the stack and do an operation on them
                Code.load(rOp);
                Code.put(operator);
                op.kind = Operand.Stack;
            } else {
                error("both operands should be int");
            }
        }
        return op;
    }

    /**
     * A factor can be one of the following.
     * 
     *      Designator [ActPars]
     *      | number
     *      | charConst
     *      | "new" ident ["[" Expr "]"]
     *      | "(" Expr ")".
     *
     * In any case, it returns an operand containing information about what the
     * factor is.
     *
     * It does not *always* load the operand on the stack. So that's still to
     * do in the Factor's caller code.
     * 
     */
    private static Operand Factor() {

        // by default, initialise the operand to a null one
        Operand op = Code.noOp;
        Struct type = Tab.noType;

        switch (sym) {
            // Designator [ActPars]
            case ident:
                op = Designator();

                // parameter call
                if (sym == lpar) {
                    // load the parameter values on the stack
                    ActPars(op);

                    if (op.obj == Tab.chrObj || op.obj == Tab.ordObj); else if (op.obj == Tab.lenObj) {
                        Code.put(Code.arraylength);
                    } else {
                        // and call the function
                        Code.put(Code.call);
                        Code.put2(op.adr);
                    }

                    // now the result is on the stack
                    op.kind = Operand.Stack;
                }
                break;

            // it's an int
            case number:
                scan();
                op = new Operand(t.val); // create the new operand with the token value
                op.type = Tab.intType;
                break;

            // it's a char
            case charCon:
                scan();
                op = new Operand(t.val);
                op.type = Tab.charType;
                break;

            // "new" ident ["[" Expr "]"]
            case new_:
                scan();
                check(ident);

                // load the identifier from the symbol table
                Obj obj = Tab.find(t.string);
                type = obj.type; // store the type of the identifier

                if (sym == lbrack) {
                    // it's an array
                    scan();
                    if (obj.kind == Obj.Type) {
                        op = Expr();
                        if (op.type == Tab.intType) {
                            Code.load(op); // put the size of the array on the stack
                            Code.put(Code.newarray);
                            if (type == Tab.charType) {
                                Code.put(0); // init an array specific to store chars
                            } else {
                                Code.put(1); // Init an array
                            }
                            type = new Struct(Struct.Arr, type);
                            
                            // set the type of the operand to nullType, as it is an address
                            op.type = Tab.nullType;
                        } else {
                            error("Array size must be an integer");
                        }
                        check(rbrack);
                    } else {
                        error("Type expected");
                    }
                } else {
                    // it's a simple new statement for a class
                    if (obj.kind == Obj.Type && obj.type.kind == Struct.Class) {

                        // allocate an area to contain the object.
                        Code.put(Code.new_);
                        Code.put2(type.nFields);

                        // and create an operand saying it is already on the stack
                        op = new Operand(obj);

                    } else {
                        error("must be a class");
                    }
                }
                break;

            // "(" Expr ")".
            case lpar:
                scan();
                op = Expr();
                check(rpar);
                break;
        }
        return op;
    }

    /**
     * Return the code operator to use.
     *
     * @return int (Code.add or Code.sub)
     */
    private static int Addop() {
        int operator = 0; // aouch !
        if (sym == plus) {
            scan();
            operator = Code.add;
        } else if (sym == minus) {
            scan();
            operator = Code.sub;
        } else {
            error(name[plus] + " or " + name[minus] + " expected");
        }
        return operator;
    }
    /**
     * Return the code operator to use.
     *
     * @return int (Code.mul, Code.div or Code.rem)
     */
    private static int Mulop() {
        int operator = 0; // aouch !
        if (sym == times) {
            scan();
            operator = Code.mul;
        } else if (sym == slash) {
            scan();
            operator = Code.div;
        } else if (sym == rem) {
            scan();
            operator = Code.rem;
        } else {
            error(name[times] + " or " + name[slash] + " or " + name[rem] + " expected");
        }
        return operator;
    }

    /**
     * Relop check for conditions operators
     * 
     * @return The token code (int) of the comparison.
     */
    private static int Relop() {

        int tokenCode = none;
        if (relopStart.get(sym)) {
            tokenCode = sym - eql;
            scan();
        } else {
            error("Expected == != < <= > >=");
        }
        return tokenCode;
    }

    /**
     * Type.
     * 
     *      Type = ident ["[" "]"]
     *
     * Parses a type and return information about it
     * 
     * @return Struct the structure containing type information
     */
    private static Struct Type() {
        check(ident);
        Obj obj = Tab.find(t.string);
        if (obj.kind != Obj.Type) {
            error(t.string + " does not refer to a known type. It must be a type.");
        }
        Struct type = obj.type; // store the type
        if (sym == lbrack) {
            scan();
            check(rbrack);
            type = new Struct(Struct.Arr, type); // return an array
        }
        return type;
    }

    public static void parse() {
        // initialize symbol sets
        BitSet s;
        s = new BitSet(64);
        exprStart = s;
        s.set(ident);
        s.set(number);
        s.set(charCon);
        s.set(new_);
        s.set(lpar);
        s.set(minus);

        s = new BitSet(64);
        statStart = s;
        s.set(ident);
        s.set(if_);
        s.set(while_);
        s.set(read_);
        s.set(return_);
        s.set(print_);
        s.set(lbrace);
        s.set(semicolon);

        s = new BitSet(64);
        statSeqFollow = s;
        s.set(rbrace);
        s.set(eof);

        s = new BitSet(64);
        declStart = s;
        s.set(final_);
        s.set(ident);
        s.set(class_);

        s = new BitSet(64);
        declFollow = s;
        s.set(lbrace);
        s.set(void_);
        s.set(eof);

        s = new BitSet(64);
        relopStart = s;
        s.set(eql);
        s.set(neq);
        s.set(gtr);
        s.set(geq);
        s.set(lss);
        s.set(leq);

        s = new BitSet(64);
        syncStat = s;
        s.set(while_);
        s.set(if_);
        s.set(while_);
        s.set(read_);
        s.set(return_);
        s.set(print_);
        s.set(lbrace);
        s.set(semicolon);
        s.set(eof); // we add the eof here to be sure it's not skipped while trying to recover.

        // create the table
        Tab.init();

        // start parsing
        errors = 0;
        errDist = 3;
        scan();
        Program();
        if (sym != eof) {
            error("end of file found before end of program");
        }
    }
}
