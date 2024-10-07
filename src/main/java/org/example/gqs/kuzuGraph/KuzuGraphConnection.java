package org.example.gqs.kuzuGraph;

import com.kuzudb.KuzuConnection;
import com.kuzudb.KuzuDatabase;
import com.kuzudb.KuzuQueryResult;
import org.apache.commons.io.FileUtils;
import org.example.gqs.Main;
import org.example.gqs.MainOptions;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherConnection;
import org.example.gqs.exceptions.MustRestartDatabaseException;

import java.io.File;
import java.util.Arrays;
import java.util.List;


public class KuzuGraphConnection extends CypherConnection {

    private KuzuConnection conn;
    private KuzuGraphOptions options;
    public KuzuDatabase database = null;
    public KuzuGraphConnection(KuzuDatabase db, KuzuConnection driver, KuzuGraphOptions options){
        this.database = db;
        this.conn = driver;
        this.options = options;
    }


    @Override
    public String getDatabaseVersion() throws Exception {
        return "memgraph";
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void executeStatement(String arg) throws Exception{
        try {
            if (arg.charAt(arg.length() - 1) != ';') {
                arg = arg + ";";
            }
            KuzuQueryResult r1 = conn.query(arg);
        } catch (Exception e){
            e.printStackTrace();
            try{
                KuzuQueryResult r1 = conn.query("MATCH (n) RETURN n;");
            } catch(Exception f) {
                throw new MustRestartDatabaseException(e);
            }
        }
    }




    @Override
    public List<GQSResultSet> executeStatementAndGet(String arg) throws Exception{
        try
        {
            if(arg.charAt(arg.length()-1) != ';'){
                arg = arg + ";";
            }
            KuzuQueryResult r1 = conn.query(arg);
            if (r1.isSuccess() == false)
            {
                throw new RuntimeException("Failed execution of the query: " + arg);
            }
            return Arrays.asList(new GQSResultSet(r1));
        } catch (Exception e){
            e.printStackTrace();
            try{
                KuzuQueryResult r1 = conn.query("MATCH (n) RETURN n;");
                if(r1.isSuccess() == false)
                {
                    throw new RuntimeException("db crashed");
                }
                String r = "[{\"a1\":\"ProblematicQuery\"}]";
                return Arrays.asList(new GQSResultSet(r));
            } catch(Exception f) {
                String r = "[{\"Crash\":\"CrashQuery\"}]";
                return Arrays.asList(new GQSResultSet(r));
            }
        }

    }

    public void reproduce(List<String> queries) {
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        long port = options.getPort();
        Main.rebootDatabase(port - 20000, "", "", MainOptions.deleteFileKuzuDB, "", MainOptions.mode + "database");
        try {

            String databasePath = "/home/auroraeth/kuzugraph/" + Long.toString(port);
            FileUtils.deleteDirectory(new File(databasePath));
            database = new KuzuDatabase(databasePath);
            conn = new KuzuConnection(database);
            executeStatement("MATCH (n) RETURN n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String query : queries) {
            try {
                executeStatement(query);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
