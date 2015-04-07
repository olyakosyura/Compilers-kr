Language specification and compiler construction
################################################

:Authors: Maxime Hardy, Alexis Metaireau (10094003)
:Date: 11/03/2010

This archive contains:

* A lexer, parser and code generator for microjava.
* An exemple of use of CoCo/R

Run the programs
================

CoCo/R
------

To run the COCO/R program, go on the directory coco/BinaryTree and then compile
and run the program::

    $ java -jar Coco.jar grammar.atg && javac *.java && java MakeBinaryTree input

Which should compile and run the program. You should see this output::

    Coco/R (Nov 16, 2010)
    checking
    parser + scanner generated
    0 errors detected
    + London
      + Brussels
      + Paris
        + Madrid
        + Rome
          + null
          + Vienna
    0
    

µJava compiler
--------------

To run the compiler, you need to compile it, then you can run all the different 
entrypoints via the following lines, from the root directory::

    $ java -cp MicroJava/build/classes/ MJ.Compiler
    $ java -cp MicroJava/build/classes/ MJ.Run
    $ java -cp MicroJava/build/classes/ MJ.TestParser

To ease the process we have defined aliases::

    $ alias parse="java -cp MicroJava/build/classes/ MJ.TestParser"
    $ alias run="java -cp MicroJava/build/classes/ MJ.Run"
    $ alias compile="java -cp MicroJava/build/classes/ MJ.Compiler"

Then just do::

    $ [compile|parse|run] samples/file-to-use.[mj|obj]
