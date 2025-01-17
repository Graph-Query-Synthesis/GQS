package org.example.gqs.cypher.gen.query;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.*;
import org.example.gqs.cypher.gen.alias.RandomAliasGenerator;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.gen.list.RandomListGenerator;
import org.example.gqs.cypher.gen.SubgraphManager;
import org.example.gqs.cypher.gen.pattern.GuidedPatternGenerator;
import org.example.gqs.cypher.mutation.ClauseScissorsMutator;
import org.example.gqs.cypher.mutation.LimitExpandingMutator;
import org.example.gqs.cypher.mutation.OptionalAdditionMutator;
import org.example.gqs.cypher.mutation.WhereRemovalMutator;
import org.example.gqs.cypher.mutation.expression.*;
import org.example.gqs.cypher.oracle.DifferentialNonEmptyBranchOracle;
import org.example.gqs.cypher.schema.CypherSchema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeBasedQueryGeneratorCompared<S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> extends SubgraphGuidedQueryGenerator<S,G> {

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


    public TreeBasedQueryGeneratorCompared(SubgraphManager subgraphManager) {
        super(subgraphManager);
    }

    @Override
    public IPatternGenerator createPatternGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new GuidedPatternGenerator<S>(globalState.getSchema(), varToProperties, subgraphManager, identifierBuilder, false);
    }

    @Override
    public IConditionGenerator createConditionGenerator(G globalState) {
        return new RandomConditionGenerator<>(globalState.getSchema(), false);
    }

    @Override
    public IAliasGenerator createAliasGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new RandomAliasGenerator<>(globalState.getSchema(), identifierBuilder, false);
    }

    @Override
    public IListGenerator createListGenerator(G globalState, IIdentifierBuilder identifierBuilder) {
        return new RandomListGenerator<>(globalState.getSchema(), identifierBuilder, false);
    }
    @Override
    public boolean shouldDoMutation(G globalState) {
        return false;
    }

    @Override
    public IClauseSequence doMutation(G globalState) {
        Randomly r = new Randomly();
        IClauseSequence sequence = null;
        S schema = globalState.getSchema();
        IClauseSequence seedSeq = seeds.get(r.getInteger(0, seeds.size())).sequence;
        IClauseSequence sequenceCopy = seedSeq.getCopy();
        long kind = r.getInteger(0, 9);
        switch ((int) kind){
            case 0:{
                new ClauseScissorsMutator<S>(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 1:{
                new LimitExpandingMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 2:{
                new OptionalAdditionMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 3:{
                new WhereRemovalMutator<>(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 4:{
                new AndRemovalMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 5:{
                new ComparisonReverseMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 6:{
                new ConditionReverseMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 7:{
                new OrRemovalMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            case 8:{
                new StringMatchReductionMutator(sequenceCopy).mutate();
                return sequenceCopy;
            }
            default:
                throw new RuntimeException();
        }
    }

    public static void writeInfoln(String info){
        try {
            outputStream.write((""+System.currentTimeMillis()+"\n"+info+"\n").getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        boolean hasNewBranch = checkAndRecordUncoveredBranch(branchInfo);
        if(resultLength != 0){
            boolean hasPossibleNonEmptyBranch = checkAndRecordUncoveredNonEmptyBranch(branchInfo);
            reduceSeeds();
        }
        else {
            boolean hasPossibleNonEmptyBranch = checkPossibleUncoveredNonEmptyBranch(branchInfo);
            if (hasPossibleNonEmptyBranch) {
            }
        }
        writeInfoln("coverage: "+coverageNum);
        writeInfoln("non_empty_coverage: "+nonEmptyCoverageNum);
        writeInfoln("result_size: "+resultLength);
    }
}
