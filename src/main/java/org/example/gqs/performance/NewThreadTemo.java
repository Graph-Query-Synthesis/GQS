package org.example.gqs.performance;

public class NewThreadTemo{
    public static void main(String[] args) {

        TreeNode tree=new TreeNode();
        tree.totValue=0;
        tree.nVisits=0;
        long n=0;
        while(n++<1000) {
            tree.selectAction();
        }
        System.out.println(tree.select().totValue+"/"+tree.select().nVisits);
    }
}