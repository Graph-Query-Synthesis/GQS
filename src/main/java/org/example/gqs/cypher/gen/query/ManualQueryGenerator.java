package org.example.gqs.cypher.gen.query;

import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.ManualClauseSequence;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class ManualQueryGenerator <S extends CypherSchema<G,?>,G extends CypherGlobalState<?,S>> implements IQueryGenerator<S, G> {

    private long current = 0;
    private String filePath;
    public List<String> queries;

    public ManualQueryGenerator(){

    }

    public void loadFile(String filePath){
        this.filePath = filePath;
        File file = new File(filePath);
        if(!file.exists()){
            throw new RuntimeException();
        }

        try(FileReader fileReader = new FileReader(file)){
            try (BufferedReader bufferedReader = new BufferedReader(fileReader)){
                queries = bufferedReader.lines().collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public IClauseSequence generateQuery(G globalState) {
        if (current < queries.size()) {
            current++;
            return new ManualClauseSequence(queries.get((int) (current - 1)));
        }
        System.exit(0);
        return null;
    }

    @Override
    public void addExecutionRecord(IClauseSequence clauseSequence, boolean isBugDetected, long resultSize) {

    }

    @Override
    public void addNewRecord(IClauseSequence sequence, boolean bugDetected, long resultLength, byte[] branchInfo, byte[] branchPairInfo) {

    }
}
