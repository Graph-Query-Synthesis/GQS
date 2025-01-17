package org.example.gqs.cypher.gen.graph;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.dsl.IGraphGenerator;
import org.example.gqs.cypher.gen.SubgraphManager;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;

import java.util.List;

public class TreeBasedGraphGenerator<G extends CypherGlobalState<?,S>, S extends CypherSchema<G,?>> implements IGraphGenerator<G> {

    private static long minNumOfNodes = 200;
    private static long maxNumOfNodes = 200;
    private static double percentOfEdges = 0.001;
    private static List<IPattern> INodesPattern;

    private final G globalState;


    private SubgraphManager subgraphManager;


    public TreeBasedGraphGenerator(G globalState){
        this.globalState = globalState;
        subgraphManager = new SubgraphManager(globalState.getSchema(), globalState.getOptions());
    }

    private ConstExpression generatePropertyValue(Randomly r, CypherType type) throws Exception {
        switch (type) {
            case NUMBER:
                return new ConstExpression(r.getInteger());
            case STRING:
                return new ConstExpression(r.getString());
            case BOOLEAN:
                return new ConstExpression(r.getInteger(0, 2) == 0);
            default:
                throw new Exception("undefined type in generator!");
        }
    }

    public SubgraphManager getSubgraphManager(){
        return subgraphManager;
    }


    @Override
    public List<CypherQueryAdapter> createGraph(G globalState) throws Exception {
        List<CypherQueryAdapter> queries = subgraphManager.generateCreateGraphQueries();
        return queries;
    }
}

