package org.example.gqs.cypher.gen;

import org.example.gqs.Randomly;

import java.util.LinkedList;
import java.util.List;

public class EnumerationSeq {
    private List<Boolean> sequence = new LinkedList<>();
    private long pos = 0;
    private boolean ended = false;

    public boolean isEnded(){
        return ended;
    }

    private Randomly randomly = new Randomly();

    public void printPresent(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < sequence.size(); i++){
            if(sequence.get(i)){
                sb.append(1);
            }
            else {
                sb.append(0);
            }
        }
        System.out.println(sb.toString());
    }

    public void incByAndRefresh(long pos){
        int i = (int)pos;
        boolean carry = true;
        while(i >= 0){
            boolean present = sequence.get(i);
            sequence.set(i, present ^ carry);
            carry = present && carry;
            i--;
            if(!carry){
                break;
            }
        }
        if(carry){
            sequence.add(0, true);
            ended = true;
        }
    }

    public void incAndRefresh(){
        int i = sequence.size() - 1;
        boolean carry = true;
        while(i >= 0){
            boolean present = sequence.get(i);
            sequence.set(i, present ^ carry);
            carry = present && carry;
            i--;
            if(!carry){
                break;
            }
        }
        if(carry){
            sequence.add(0, true);
            ended = true;
        }
        pos = 0;
    }

    public boolean getDecision(){
        pos++;
        if(pos >= sequence.size()){
            sequence.add(false);
        }
        return sequence.get((int) (pos - 1));
    }

    public int getRange(long max){
        long num = 0;
        long maxNum = 1;
        while (maxNum < max) {
            pos++;
            if (pos >= sequence.size()) {
                sequence.add(false);
            }
            num = (num << 1) + (sequence.get((int)pos - 1) ? 1 : 0);
            maxNum <<= 1;
        }
        return (int) (num % max);
    }

    public <T> T getElement(List<T> list){
        return list.get((int)getRange(list.size()));
    }

    public void finish(){
        sequence = sequence.subList(0, (int)pos);
        pos = 0;
    }

    public void randomize(){
        List<Boolean> newSeq = new LinkedList<>();
        for(int i = 0; i < sequence.size() * 2; i++){
            newSeq.add(randomly.getInteger(0, 100) < 50);
        }
        sequence = newSeq;
    }


}
