package org.example.gqs.performance;

import org.example.gqs.Randomly;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TreeNode {
    static Random r = new Random(Randomly.THREAD_SEED.get());
    static int nActions = 5;
    static double epsilon =1e-6;

    TreeNode[] children;
    public long nVisits;
    public long totValue;

    public boolean isLeaf() {
        return children == null;
    }

    public TreeNode select() {
        TreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (TreeNode c : children) {
            double uctValue = c.totValue / (c.nVisits + epsilon) +
                    Math.sqrt(Math.log(nVisits + 1) / (c.nVisits + epsilon)) + r.nextDouble() * epsilon;
            if (uctValue > bestValue) {
                selected = c;
                bestValue = uctValue;
            }
        }
        return selected;
    }

    public void expand() {
        children = new TreeNode[nActions];
        for (int i = 0; i < nActions; i++) {
            children[i] = new TreeNode();
        }
    }


    public void selectAction() {
        List<TreeNode> visited = new LinkedList<>();
        TreeNode cur = this;
        System.out.print("：" + cur.totValue + "/" + cur.nVisits + " \n ");
        visited.add(this);
        while (!cur.isLeaf()) {
            cur = cur.select();
            visited.add(cur);
            System.out.print("" + cur.totValue + "/" + cur.nVisits + "  ");
        }
        System.out.print("\n");
        cur.expand();
        TreeNode newNode = cur.select();
        visited.add(newNode);
        long value = rollOut();
        for (TreeNode node : visited) {
            node.updateState(value);
        }
    }

    public long rollOut() {
        return r.nextInt(2);
    }

    public void updateState(double value){
        nVisits++;
        totValue += value;
    }
    public long arity() {
        return children == null ? 0 : children.length;
    }

}
