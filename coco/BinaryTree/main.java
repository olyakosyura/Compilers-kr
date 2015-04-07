class MakeBinaryTree{
    public static int nbTab = 0; 
    public static void main (String [] args) {
        String input = args[0];
        Scanner scanner = new Scanner(input);
        Parser parser = new Parser(scanner);

        parser.Parse();
        printTree(parser.rootnode);
        System.out.println(parser.errors.count);
    }
    
    /**
     * Prints a the tree
     */
    public static void printTree(Node root){
        if (root != null){
            print(root.value);
            if (root.left != null && root.right != null){
                nbTab++;
                printTree(root.left);
                printTree(root.right);
                nbTab--;
            }
        }
    }
    
    public static void print(String s){
        for (int i=0; i < nbTab; i++){
            System.out.print("  ");
        }
        System.out.println("+ " + s);
    }
}
