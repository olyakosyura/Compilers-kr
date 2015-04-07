/**
 * Reprent a node of a binary tree. 
 *
 * A node can even be blank or have two childs
 */
class Node{
    public Node left;
    public Node right;
    public String value;

    /**
     * Constructor for blank node
     */
    public void Node(){
        this.left = null;
        this.right = null;
        this.value = "empty";
    }

}
