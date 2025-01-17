package org.example.gqs.cypher.algorithm;

import org.example.gqs.*;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.cypher.CypherGlobalState;
import org.example.gqs.cypher.CypherProviderAdapter;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.gen.*;
import org.example.gqs.cypher.gen.graph.SlidingGraphGenerator;
import org.example.gqs.cypher.gen.query.SlidingQueryGenerator;
import org.example.gqs.cypher.oracle.DifferentialNonEmptyBranchOracle;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.exceptions.DatabaseCrashException;
import org.example.gqs.exceptions.MustRestartDatabaseException;
import org.example.gqs.cypher.gen.GraphManager;
import org.neo4j.driver.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.min;

public class Compared3AlgorithmNew<S extends CypherSchema<G,?>, G extends CypherGlobalState<O, S>,
        O extends DBMSSpecificOptions<? extends OracleFactory<G>>, C extends CypherConnection> extends CypherTestingAlgorithm<S,G,O,C>{
    private GraphManager graphManager;


    public Compared3AlgorithmNew(CypherProviderAdapter<G, S, O> provider) {
        super(provider);
    }



    public List<CypherQueryAdapter> generateDiffQueries(GraphManager old, GraphManager newGraphManager) throws Exception{


        List<CypherQueryAdapter> query = new ArrayList<>();
        long dropcnt = (long) Randomly.getNotCachedInteger(0, (long)(MainOptions.labelNum*0.1));

        for(int i = 0; i < dropcnt; i++)
        {
            if(MainOptions.mode == "neo4j")
            {
                query.add(new CypherQueryAdapter("DROP INDEX i"+i+" IF EXISTS"));
            }
            else if (MainOptions.mode == "memgraph" || MainOptions.mode == "falkordb")
            {
                List<AbstractNode> curNodes = old.getNodes();
                int index = (int) Randomly.getNotCachedInteger(0, curNodes.size());
                AbstractNode curNode = curNodes.get(index);
                String randomProperty = "";
                while(randomProperty.equals(""))
                {
                    int propertyIndex = (int) Randomly.getNotCachedInteger(0, curNode.getProperties().size());
                    List<String> randomProp = new ArrayList<>(curNode.getProperties().keySet());
                    randomProperty = randomProp.get(propertyIndex);
                    if(curNode.getProperties().get(randomProperty) instanceof Boolean)
                    {
                        randomProperty = "";
                    }
                }
                if(curNode.getLabels().size() > 0)
                    query.add(new CypherQueryAdapter("DROP INDEX ON :"+curNode.getLabels().get(0).getName()+"("+randomProperty+")"));
            }
            else
                throw new Exception("undefined mode!");
        }
        Set<Integer> deletedNodes = new HashSet<>();
        if(old.getNodes().size() > newGraphManager.getNodes().size()){

            for(int i = newGraphManager.getNodes().size(); i < old.getNodes().size(); i++){
                query.addAll(old.generateDeleteNodeQueries(old.getNodes().get(i)));

                deletedNodes.add(old.getNodes().get(i).getId());
            }
        }
        else if(old.getNodes().size() < newGraphManager.getNodes().size()){

            for(int i = old.getNodes().size(); i < newGraphManager.getNodes().size(); i++){
                query.addAll(newGraphManager.generateCreateNodeQueries(newGraphManager.getNodes().get(i)));
            }
        }

        for(int i = 0; i <  min(newGraphManager.getNodes().size(), old.getNodes().size()); i++)
        {
            query.addAll(newGraphManager.generateAlterNodeQueries(newGraphManager.getNodes().get(i), old.getNodes().get(i)));
        }

        for(int i = 0; i < newGraphManager.getNodes().size(); i++)
        {
            for (int j = 0; j < newGraphManager.getNodes().size(); j++)
            {
                Set<Integer> newEdges = newGraphManager.MatrixRep.Matrix.get(i).get(j);
                if(i < old.getNodes().size() && j < old.getNodes().size())
                {
                    Set<Integer> oldEdges = old.MatrixRep.Matrix.get(i).get(j);
                    for (Integer oldEdge : oldEdges)
                    {
                        if(newEdges.contains(oldEdge))
                        {
                            AbstractRelationship oldRelation = ((AbstractRelationship) old.MatrixRep.idMap.get(oldEdge)).getCopy();
                            AbstractRelationship newRelation = (AbstractRelationship) newGraphManager.MatrixRep.idMap.get(oldEdge);
                            if(oldRelation.getFrom().getId() == newRelation.getFrom().getId() && oldRelation.getTo().getId() == newRelation.getTo().getId() && oldRelation.getType().equals(newRelation.getType()))
                            {
                                oldRelation.setFrom(newGraphManager.getNodes().get(i));
                                oldRelation.setTo(newGraphManager.getNodes().get(j));

                                query.addAll(newGraphManager.generateAlterEdgeQueries(oldRelation, newRelation));
                            }
                            else
                            {
                                if(!deletedNodes.contains(oldRelation.getFrom().getId()) && !deletedNodes.contains(oldRelation.getTo().getId()))
                                {
                                    oldRelation.setFrom(newGraphManager.getNodes().get(i));
                                    oldRelation.setTo(newGraphManager.getNodes().get(j));

                                    query.addAll(old.generateDeleteCreateEdgeQueries(oldRelation, newRelation));
                                }
                                else
                                {

                                    query.addAll(newGraphManager.generateCreateEdgeQueries(newRelation));
                                }
                            }
                        }
                        else
                        {

                            AbstractRelationship oldRelation = ((AbstractRelationship) old.MatrixRep.idMap.get(oldEdge)).getCopy();
                            if(deletedNodes.contains(oldRelation.getFrom().getId()) || deletedNodes.contains(oldRelation.getTo().getId()))
                                continue;
                            oldRelation.setFrom(newGraphManager.getNodes().get(i));
                            oldRelation.setTo(newGraphManager.getNodes().get(j));
                            query.addAll(old.generateDeleteEdgeQueries(oldRelation));
                        }
                    }
                    for (Integer newEdge : newEdges)
                    {
                        if(!oldEdges.contains(newEdge))
                        {

                            query.addAll(newGraphManager.generateCreateEdgeQueries((AbstractRelationship) newGraphManager.MatrixRep.idMap.get(newEdge)));
                        }
                    }
                }
                else
                {

                    for(Integer newEdge : newEdges)
                    {
                        query.addAll(newGraphManager.generateCreateEdgeQueries((AbstractRelationship) newGraphManager.MatrixRep.idMap.get(newEdge)));
                    }
                }
            }
        }
        return query;
    }

    @Override
    public void generateAndTestDatabase(G globalState) throws Exception {
        try {

            for(int mutationCnt = 0; mutationCnt < MainOptions.mutationCnt; mutationCnt++) {
                if(mutationCnt == 0)
                    generateDatabase(globalState, true);
                else
                {


                    GraphManager graphManagerBackup = graphManager.clone();

                    generateDatabase(globalState, false);

                    List<CypherQueryAdapter> queries = generateDiffQueries(graphManagerBackup, graphManager);
                    for(CypherQueryAdapter query : queries){
                        globalState.executeStatement(query);
                        globalState.getState().logCreateStatement(query);
                    }

                    if(MainOptions.debug != -1 && MainOptions.sidePort != -1) {
                        File file = new File("MutationProbe");
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        else
                        {
                            file.delete();
                            file.createNewFile();
                        }
                        FileOutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(("MATCH (n)-[r]-(m) return properties(n), properties(r), properties(m), labels(n), type(r), labels(m) order by n.id, r.id, m.id desc").getBytes());
                        outputStream.close();
                        String[] mvnCommand = {"/bin/bash", "-c", "java -jar ignorestring.jar " + MainOptions.mode + " " + (Integer.parseInt(globalState.getState().getDatabaseName().split("-")[2]) + 20000) + " " + "MutationProbe"};
                        Main.executeCommand(mvnCommand);
                        String currentStructure = "";
                        File reader = new File("./result.txt");
                        if (reader.exists()) {
                            BufferedReader br = new BufferedReader(new FileReader(reader));
                            String line;
                            while ((line = br.readLine()) != null) {
                                currentStructure += line + "\n";
                            }
                            br.close();
                        } else {
                            System.out.println("No result.txt");
                        }
                        reader.delete();
                        if (MainOptions.standard.equals(currentStructure)) {
                            System.out.println("Mutation " + mutationCnt + " is effective");
                            continue;
                        }
                        else
                        {
                            System.out.println("Mutation " + mutationCnt + " is uneffective");
                        }
                    }
                }
                globalState.getManager().incrementCreateDatabase();

            TestOracle oracle = null;
            oracle = new DifferentialNonEmptyBranchOracle<G, S>(globalState, new SlidingQueryGenerator<>(graphManager));
            MainOptions.assistSkipMatch = false;
            for (int i = 0; i < globalState.getOptions().getNrQueries(); i++) {


                try (StateToReproduce.OracleRunReproductionState localState = globalState.getState().createLocalState()) {
                    assert localState != null;
                    try {
                        if(MainOptions.skipMatch !=-1 && i >= MainOptions.skipMatch)
                            MainOptions.assistSkipMatch = true;
                        oracle.check();
                        globalState.getManager().incrementSelectQueryCount();
                    } catch (IgnoreMeException e) {
                    } catch (MustRestartDatabaseException e){
                        throw e;
                    } catch (DatabaseCrashException e){
                        if(e.getCause() instanceof MustRestartDatabaseException){
                            throw new MustRestartDatabaseException(e);
                        }
                        assert localState != null;
                        localState.executedWithoutError();
                    }
                }
            }

        } }finally {
            globalState.getConnection().close();
        }
    }


    public void generateDatabase(G globalState, boolean executeQueries) throws Exception{
        SlidingGraphGenerator<G,S> generator = new SlidingGraphGenerator<>(globalState);
        this.graphManager = generator.getGraphManager();
        List<CypherQueryAdapter> queries = generator.createGraph(globalState);



        if(executeQueries) {
            for (CypherQueryAdapter query : queries) {
                globalState.executeStatement(query);
                globalState.getState().logCreateStatement(query);
            }
        }
        else {
            System.out.println("---These queries will not be executed---");
            globalState.getState().logStatement("// --- These queries will not be executed. It responsible for creating the database: "+globalState.getDatabaseName()+" ---");
            for (CypherQueryAdapter query : queries) {
                System.out.println(query.getQueryString());
                globalState.getState().logStatement("// "+query.getQueryString());
            }
            if(MainOptions.debug != -1 && MainOptions.sidePort != -1)
            {
                File file = new File("ConstructQueries");
                if (!file.exists()) {
                    file.createNewFile();
                }
                else
                {
                    file.delete();
                    file.createNewFile();
                }
                FileOutputStream outputStream = new FileOutputStream(file);
                queries.add(new CypherQueryAdapter("MATCH (n)-[r]-(m) return properties(n), properties(r), properties(m), labels(n), type(r), labels(m) order by n.id, r.id, m.id desc"));
                for (CypherQueryAdapter query : queries) {

                        outputStream.write((query.getQueryString()+"\n").getBytes());
                }
                outputStream.close();

                if(MainOptions.useEmbeddedNeo4j == false) {
                    String[] stopCommand = {"/bin/bash", "-c", MainOptions.stopCommand.replace("THREAD_WEB", Integer.toString((int) (MainOptions.sidePort + 20000)))};
                    Main.executeCommand(stopCommand);
                    String[] deleteCommand = {"/bin/bash", "-c", MainOptions.deleteCommand.replace("THREAD_FOLDER", Integer.toString((int) MainOptions.sidePort))};
                    Main.executeCommand(deleteCommand);
                    String[] startCommand = {"/bin/bash", "-c", MainOptions.startCommand.replace("THREAD_FOLDER", Integer.toString((int) MainOptions.sidePort)).replace("THREAD_WEB", Integer.toString((int) (MainOptions.sidePort + 20000))).replace("LOG_DIRECTORY", "/home/auroraeth/test")};
                    Main.executeCommand(startCommand);

                    String uri = "bolt://localhost:" + (20000 + MainOptions.sidePort);
                    String user = "neo4j";
                    String password = "testtest";
                    boolean flag = false;

                    long cnt = 0;
                    while (!flag) {
                        flag = false;
                        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
                             Session session = driver.session()) {
                            String query = "MATCH (n) RETURN count(n) AS count";
                            Result result = session.run(query);
                            flag = true;
                            break;


                        } catch (Exception e) {
                            System.out.println("wait for another moment");
                            flag = false;
                            cnt++;
                            try {
                                Thread.sleep(1000);
                            } catch (Exception f) {
                                f.printStackTrace();
                            }
                        }

                    }
                }

                String[] mvnCommand = {"/bin/bash", "-c", "java -jar ignorestring.jar "+MainOptions.mode+" "+Integer.toString((int) (MainOptions.sidePort+20000))+" "+"ConstructQueries"};
                Main.executeCommand(mvnCommand);



                File reader = new File("./result.txt");
                MainOptions.standard = "";
                if(reader.exists())
                {
                    BufferedReader br = new BufferedReader(new FileReader(reader));
                    String line;
                    while ((line = br.readLine()) != null) {
                        MainOptions.standard+=line+"\n";
                    }
                    br.close();
                }
                else
                {
                    System.out.println("No result.txt");
                }
                reader.delete();
            }
            globalState.getState().logStatement("// --- Creating queries end ---");
            System.out.println("---These queries will not be executed---");
        }
    }
}
