package org.example.gqs.cypher.gen.query;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.gen.GraphManager;
import org.example.gqs.cypher.oracle.DifferentialNonEmptyBranchOracle;
import org.example.gqs.cypher.schema.CypherSchema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlidingQueryGenerator<S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> extends GraphGuidedQueryGenerator<S,G> {

    public static boolean[] totalCoverage = new boolean[DifferentialNonEmptyBranchOracle.BRANCH_SIZE];
    public static boolean[] totalNonEmptyCoverage = new boolean[DifferentialNonEmptyBranchOracle.BRANCH_SIZE];

    public static File coverageFile;

    public static FileOutputStream outputStream;

    public static long coverageNum = 0;
    public static long nonEmptyCoverageNum = 0;

    static {

    }

    public static class Seed{
        IClauseSequence sequence;
        boolean bugDetected;
        long resultLength;

        byte[] branchInfo;
        byte[] branchPairInfo;

        public Seed(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo){
            this.sequence = sequence;
            this.bugDetected = bugDetected;
            this.resultLength = resultLength;
            this.branchInfo = branchInfo;
            this.branchPairInfo = branchPairInfo;
        }
    }

    protected List<Seed> seeds = new ArrayList<>();


    public SlidingQueryGenerator(GraphManager graphManager) {
        super(graphManager);
    }

    @Override
    public boolean shouldDoMutation(G globalState) {
        return false;
    }

    public static void writeInfoln(String info){
    }


    public static boolean checkAndRecordUncoveredBranch(byte[] branchInfo){
        boolean found = false;
        for(int i = 0; i < DifferentialNonEmptyBranchOracle.BRANCH_SIZE; i++){
            if(branchInfo[i] != 0 && !totalCoverage[i]){
                found = true;
                totalCoverage[i] = true;
                coverageNum++;
            }
        }
        return found;
    }

    public static boolean checkAndRecordUncoveredNonEmptyBranch(byte[] branchInfo){
        boolean found = false;
        for(int i = 0; i < DifferentialNonEmptyBranchOracle.BRANCH_SIZE; i++){
            if(branchInfo[i] != 0 && !totalNonEmptyCoverage[i]){
                found = true;
                totalNonEmptyCoverage[i] = true;
                nonEmptyCoverageNum++;
            }
        }

        return found;
    }

    public static boolean checkPossibleUncoveredNonEmptyBranch(byte[] branchInfo){
        boolean found = false;
        long coverageNum = 0;
        for(int i = 0; i < DifferentialNonEmptyBranchOracle.BRANCH_SIZE; i++){
            if(branchInfo[i] != 0 && !totalNonEmptyCoverage[i]){
                found = true;
            }
            if(totalNonEmptyCoverage[i]){
                coverageNum++;
            }
        }
        return found;
    }

    public void reduceSeeds(){
        List<Seed> deletedSeeds = new ArrayList<>();
        for(Seed seed : seeds){
            if(!checkPossibleUncoveredNonEmptyBranch(seed.branchInfo)){
                deletedSeeds.add(seed);
            }
        }
        seeds.removeAll(deletedSeeds);
    }

    @Override
    public void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo){
    }
}
