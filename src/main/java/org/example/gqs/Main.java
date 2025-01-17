package org.example.gqs;

import java.io.*;
import java.lang.Boolean;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.JCommander.Builder;

import com.falkordb.FalkorDB;
import com.falkordb.Graph;
import org.example.gqs.PrintGraph.PrintGraphProvider;
import org.example.gqs.arcadeDB.ArcadeDBProvider;
import org.example.gqs.common.log.Loggable;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.common.query.Query;
import org.example.gqs.composite.CompositeProvider;
import org.example.gqs.composite.oracle.CompositeMCTSOracle;
import org.example.gqs.janusGraph.JanusProvider;
import org.example.gqs.kuzuGraph.KuzuGraphProvider;
import org.example.gqs.memGraph.MemGraphProvider;
import org.example.gqs.neo4j.Neo4jProvider;
import org.example.gqs.tinkerGraph.TinkerConnection;
import org.example.gqs.tinkerGraph.TinkerProvider;
import org.example.gqs.agensGraph.AgensGraphProvider;
import org.example.gqs.redisGraph.RedisGraphProvider;
import jxl.Workbook;
import jxl.write.*;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;

import org.apache.tinkerpop.gremlin.driver.Cluster;

public final class Main {

    public static final File LOG_DIRECTORY = new File(MainOptions.getLogPath());
    public static volatile AtomicLong nrQueries = new AtomicLong();
    public static volatile AtomicLong nrDatabases = new AtomicLong();
    public static volatile AtomicLong nrSuccessfulActions = new AtomicLong();
    public static volatile AtomicLong nrUnsuccessfulActions = new AtomicLong();
    static long threadsShutdown;
    static boolean progressMonitorStarted;

    static {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
        if (!LOG_DIRECTORY.exists()) {
            LOG_DIRECTORY.mkdir();
        }
    }

    private Main() {
    }

    public static final class SLog {

        private File loggerFile;

        private long loggerNum = 0;
        private File curFile;
        private FileWriter logFileWriter;
        public FileWriter currentFileWriter;
        private static final List<String> INITIALIZED_PROVIDER_NAMES = new ArrayList<>();
        private final boolean logEachSelect;
        private final DatabaseProvider<?, ?, ?> databaseProvider;

        private File dir;

        private String databaseName;

        private static final class AlsoWriteToConsoleFileWriter extends FileWriter {

            AlsoWriteToConsoleFileWriter(File file) throws IOException {
                super(file);
            }

            @Override
            public Writer append(CharSequence arg0) throws IOException {
                System.err.println(arg0);
                return super.append(arg0);
            }

            @Override
            public void write(String str) throws IOException {
                System.err.println(str);
                super.write(str);
            }
        }

        public SLog(String databaseName, DatabaseProvider<?, ?, ?> provider, MainOptions options) {
            this.databaseName = databaseName;
            dir = new File(LOG_DIRECTORY, provider.getDBMSName());
            if (dir.exists() && !dir.isDirectory()) {
                throw new AssertionError(dir);
            }
            ensureExistsAndIsEmpty(dir, provider);
            loggerFile = new File(dir, databaseName + "-" + loggerNum + ".log");
            loggerNum++;
            logEachSelect = options.logEachSelect();
            if (logEachSelect) {
                curFile = new File(dir, databaseName + "-cur.log");
            }
            this.databaseProvider = provider;
        }

        private void ensureExistsAndIsEmpty(File dir, DatabaseProvider<?, ?, ?> provider) {
            if (INITIALIZED_PROVIDER_NAMES.contains(provider.getDBMSName())) {
                return;
            }
            synchronized (INITIALIZED_PROVIDER_NAMES) {
                if (!dir.exists()) {
                    try {
                        Files.createDirectories(dir.toPath());
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }
                File[] listFiles = dir.listFiles();
                assert listFiles != null : "directory was just created, so it should exist";
                INITIALIZED_PROVIDER_NAMES.add(provider.getDBMSName());
            }
        }

        private FileWriter getLogFileWriter() {
            if (logFileWriter == null) {
                try {
                    logFileWriter = new AlsoWriteToConsoleFileWriter(loggerFile);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
            return logFileWriter;
        }

        public FileWriter getCurrentFileWriter() {
            if (!logEachSelect) {
                throw new UnsupportedOperationException();
            }
            if (currentFileWriter == null) {
                try {
                    currentFileWriter = new FileWriter(curFile, false);
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
            return currentFileWriter;
        }

        public void writeCurrent(StateToReproduce state) {
            if (!logEachSelect) {
                throw new UnsupportedOperationException();
            }
            printState(getCurrentFileWriter(), state);
            try {
                currentFileWriter.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void writeCurrent(String input) {
            write(databaseProvider.getLoggableFactory().createLoggable(input));
        }

        public void writeCurrentNoLineBreak(String input) {
            write(databaseProvider.getLoggableFactory().createLoggableWithNoLinebreak(input));
        }

        private void write(Loggable loggable) {
            if (!logEachSelect) {
                throw new UnsupportedOperationException();
            }
            try {
                getCurrentFileWriter().write(loggable.getLogString());

                currentFileWriter.flush();
            } catch (IOException e) {
                throw new AssertionError();
            }
        }

        public void logException(Throwable reduce, StateToReproduce state) {
            Loggable stackTrace = getStackTrace(reduce);
            FileWriter logFileWriter2 = getLogFileWriter();
            try {
                logFileWriter2.write(stackTrace.getLogString());
                printState(logFileWriter2, state);
            } catch (IOException e) {
                throw new AssertionError(e);
            } finally {
                try {
                    logFileWriter2.flush();
                    logFileWriter2.close();
                    logFileWriter = null;
                    loggerFile = new File(dir, databaseName + "-" + loggerNum + ".log");
                    loggerNum++;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void logExceptionPartial(Throwable reduce, StateToReproduce state, Query<?> query) {
            Loggable stackTrace = getStackTrace(reduce);
            FileWriter logFileWriter2 = getLogFileWriter();
            try {
                logFileWriter2.write(stackTrace.getLogString());
                printCreateState(logFileWriter2, state);

                StringBuilder sb = new StringBuilder();

                sb.append("\n\n");

                sb.append(query.getLogString());
                logFileWriter2.write(sb.toString());
            } catch (IOException e) {
                throw new AssertionError(e);
            } finally {
                try {
                    logFileWriter2.flush();
                    logFileWriter2.close();
                    logFileWriter = null;
                    loggerFile = new File(dir, databaseName + "-" + loggerNum + ".log");
                    loggerNum++;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private Loggable getStackTrace(Throwable e1) {
            return databaseProvider.getLoggableFactory().convertStacktraceToLoggable(e1);
        }

        private void printState(FileWriter writer, StateToReproduce state) {
            StringBuilder sb = new StringBuilder();

            sb.append(databaseProvider.getLoggableFactory()
                    .getInfo(state.getDatabaseName(), state.getDatabaseVersion(), state.getSeedValue()).getLogString());

            for (Query<?> s : state.getStatements()) {
                sb.append(s.getLogString());
                sb.append('\n');
            }
            try {
                writer.write(sb.toString());
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        private void printCreateState(FileWriter writer, StateToReproduce state) {
            StringBuilder sb = new StringBuilder();

            sb.append(databaseProvider.getLoggableFactory()
                    .getInfo(state.getDatabaseName(), state.getDatabaseVersion(), state.getSeedValue()).getLogString());

            for (Query<?> s : state.getCreateStatements()) {
                sb.append(s.getLogString());
                sb.append('\n');
            }
            try {
                writer.write(sb.toString());
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

    }

    public static class QueryManager<C extends GDSmithDBConnection> {

        private final GlobalState<?, ?, C> globalState;

        QueryManager(GlobalState<?, ?, C> globalState) {
            this.globalState = globalState;
        }

        public boolean execute(Query<C> q, String... fills) throws Exception {
            globalState.getState().logStatement(q);
            boolean success;
            success = q.execute(globalState, fills);
            Main.nrSuccessfulActions.addAndGet(1);
            return success;
        }

        public List<GQSResultSet> executeAndGet(Query<C> q, String... fills) throws Exception {
            globalState.getState().logStatement(q);
            List<GQSResultSet> result;
            List<GQSResultSet> finalResult;
            result = q.executeAndGet(globalState, fills);
            if (MainOptions.mode.equals("memgraph")) {
                finalResult = new ArrayList<>();
                if (result.size() > 0 && result.get(0).result != null && result.get(0).result.size() > 0 && !(result.get(0).result.get(0) instanceof HashMap)) {
                    for (int i = 0; i < result.size(); i++) {
                        GQSResultSet currentRes = result.get(i);
                        GQSResultSet convertRes = new GQSResultSet();
                        convertRes.result = new ArrayList<>();
                        for (int j = 0; j < currentRes.result.size(); j++) {
                            convertRes.result.add(new HashMap<>());
                            convertRes.result.get(j).putAll(currentRes.result.get(j));
                        }
                        convertRes.resultRowNum = currentRes.resultRowNum;
                        finalResult.add(convertRes);
                    }
                }
            } else {
                finalResult = result;
            }
            return finalResult;
        }

        public void incrementSelectQueryCount() {
        }

        public void incrementCreateDatabase() {
        }

        public List<Long> executeAndGetTime(Query<C> q, String... fills) throws Exception {
            globalState.getState().logStatement(q);
            List<Long> result;
            result = q.executeAndGetTime(globalState, fills);
            return result;
        }
    }

    public static void main(String[] args) throws IOException {
        System.exit(executeMain(args));
    }

    public static class DBMSExecutor<G extends GlobalState<O, ?, C>, O extends DBMSSpecificOptions<?>, C extends GDSmithDBConnection> {

        private final DatabaseProvider<G, O, C> provider;
        public final MainOptions options;
        private final O command;
        private final String databaseName;
        private SLog logger;
        private StateToReproduce stateToRepro;
        private Randomly r;


        public DBMSExecutor(DatabaseProvider<G, O, C> provider, MainOptions options, O dbmsSpecificOptions,
                String databaseName, Randomly r) {
            this.provider = provider;
            this.options = options;
            this.databaseName = databaseName;
            this.command = dbmsSpecificOptions;
            this.r = r;
        }

        private G createGlobalState() {
            try {
                return provider.getGlobalStateClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        public O getCommand() {
            return command;
        }

        public void testConnection() throws Exception {
            G state = getInitializedGlobalState(options.getRandomSeed());
            try (GDSmithDBConnection con = provider.createDatabase(state)) {
                return;
            }
        }

        public void run() throws Exception {
            System.gc();
            G state = createGlobalState();
            stateToRepro = provider.getStateToReproduce(databaseName);
            stateToRepro.seedValue = r.THREAD_SEED.get();
            state.setState(stateToRepro);
            logger = new SLog(databaseName, provider, options);
            state.setRandomly(r);
            state.setDatabaseName(databaseName);
            System.out.println("Database Name:" + databaseName);
            state.setMainOptions(options);
            state.setDbmsSpecificOptions(command);
            System.gc();
            try (C con = provider.createDatabase(state)) {
                QueryManager<C> manager = new QueryManager<>(state);
                try {
                    stateToRepro.databaseVersion = con.getDatabaseVersion();
                } catch (Exception e) {
                }
                state.setConnection(con);
                state.setStateLogger(logger);
                state.setManager(manager);
                if (options.logEachSelect()) {
                    logger.writeCurrent(state.getState());
                }
                provider.generateAndTestDatabase(state);
                try {
                    logger.getCurrentFileWriter().close();
                    logger.currentFileWriter = null;
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
            }
        }

        private G getInitializedGlobalState(long seed) {
            G state = createGlobalState();
            stateToRepro = provider.getStateToReproduce(databaseName);
            stateToRepro.seedValue = seed;
            state.setState(stateToRepro);
            logger = new SLog(databaseName, provider, options);
            state.setDatabaseName(databaseName);
            state.setMainOptions(options);
            state.setDbmsSpecificOptions(command);
            return state;
        }

        public SLog getLogger() {
            return logger;
        }

        public StateToReproduce getStateToReproduce() {
            return stateToRepro;
        }
    }

    public static class DBMSExecutorFactory<G extends GlobalState<O, ?, C>, O extends DBMSSpecificOptions<?>, C extends GDSmithDBConnection> {

        private final DatabaseProvider<G, O, C> provider;
        private final MainOptions options;
        private final O command;
        public static long seed = 0;

        public DBMSExecutorFactory(DatabaseProvider<G, O, C> provider, MainOptions options) {
            this.provider = provider;
            this.options = options;
            this.command = createCommand();
        }

        private O createCommand() {
            try {
                return provider.getOptionClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        public O getCommand() {
            return command;
        }

        @SuppressWarnings("unchecked")
        public DBMSExecutor<G, O, C> getDBMSExecutor(String databaseName, Randomly r) {
            try {
                return new DBMSExecutor<G, O, C>(provider.getClass().getDeclaredConstructor().newInstance(), options,
                        command, databaseName, r);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        public DatabaseProvider<G, O, C> getProvider() {
            return provider;
        }

    }

    public static boolean rebootDatabase(long i, String stopC, String deleteC, String deleteF, String startC, String prefix) {
        if(MainOptions.useEmbeddedNeo4j == true && MainOptions.mode.equals("neo4j")) {
            String[] stopCommand = {"/bin/bash", "-c", stopC.replace("THREAD_NAME", prefix + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i)))};
            executeCommand(stopCommand);
            return true;
        }
        else if (MainOptions.mode == "kuzu")
        {
            String[] deleteCommand = {"/bin/bash", "-c", deleteC.replace("THREAD_NAME", prefix + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i)};
            executeCommand(deleteCommand);
            return true;
        }
        Long time = System.currentTimeMillis();
        String[] stopCommand = {"/bin/bash", "-c", stopC.replace("THREAD_NAME", prefix + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize) + i)};
        executeCommand(stopCommand);
        if(MainOptions.mode == "memgraph" && MainOptions.exp == "coverage")
        {
            long fileSize = 0;
            long cnt = 0;
            while(fileSize == 0) {
                File file = new File("/home/auroraeth/memgraph/THREAD_FOLDER/default.profraw".replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize) + i));
                if (!file.exists())
                    break;
                fileSize = file.length();
                try {
                    Thread.sleep(1000);
                    cnt++;
                } catch (Exception e) {
                }
                if (cnt > 5)
                    break;
            }
        }
        else if (MainOptions.mode == "neo4j" && MainOptions.exp == "coverage")
        {
            long fileSize = 0;
            long cnt = 0;
            while(fileSize == 0) {
                File file = new File("/home/auroraeth/neo4j/THREAD_FOLDER/jacoco.exec".replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize) + i));
                if (!file.exists())
                    break;
                fileSize = file.length();
                try {
                    Thread.sleep(1000);
                    cnt++;
                } catch (Exception e) {
                }
                if (cnt > 5)
                    break;
            }
        }
        String[] deleteFile = {"/bin/bash", "-c", deleteF.replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i).replace("TIME", time.toString())};
        executeCommand(deleteFile);
        String[] deleteCommand = {"/bin/bash", "-c", deleteC.replace("THREAD_NAME", prefix + i).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i)};
        executeCommand(deleteCommand);

        String[] startCommand = {"/bin/bash", "-c", startC.replace("THREAD_NAME", prefix + i).replace("THREAD_SERVER", Integer.toString((int) (10000 + i))).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode+ Long.toString(MainOptions.maxClauseSize) + i).replace("LOG_DIRECTORY", "./logs/" + MainOptions.mode + "/")};
        executeCommand(startCommand);

        if(MainOptions.mode == "neo4j" || MainOptions.mode == "memgraph")
        {
            String uri = "bolt://127.0.0.1:"+ (20000 + i);
            String user = "neo4j";
            String password = "testtest";
            boolean flag = false;

            long cnt = 0;
            while (!flag) {
                flag = false;
                if (cnt > 20)
                    return false;
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
        else if(MainOptions.mode == "thinker") {
            String uri = "bolt://localhost:" + (20000 + i);
            Cluster cluster;
            try {
                cluster = Cluster.build().port((int) (20000+i)).create();

            }
            catch (Exception e)
            {
                throw new RuntimeException("Config file for thinker not found");
            }

            TinkerConnection con = new TinkerConnection(cluster);
            boolean flag = false;
            long cnt = 0;
            while (!flag) {
                try {
                    flag = false;
                    if (cnt > 20)
                        return false;
                    con.executeStatement("MATCH (n) DETACH DELETE n");
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
        else if (MainOptions.mode == "falkordb")
        {
            boolean flag = false;
            int cnt = 0;
            Graph graph = FalkorDB.driver("127.0.0.1", (int) (20000 + i)).graph("social");
            while (!flag) {
                try {
                    graph.query("MATCH (n) DETACH DELETE n");
                    flag = true;
                    graph.close();
                    break;
                } catch (Exception e) {
                    System.out.println("wait for another moment");
                    flag = false;
                    cnt++;
                    try {
                        Thread.sleep(500);
                    } catch (Exception f) {
                        f.printStackTrace();
                    }
                }
            }
        }
        else if (MainOptions.mode == "kuzu"){}
        else{throw new RuntimeException("Mode not supported");}
        return true;
    }

    public static boolean rebootDatabase(long i, MainOptions options) {
        if(MainOptions.useEmbeddedNeo4j == true && MainOptions.mode.equals("neo4j")) {
            String[] stopCommand = {"/bin/bash", "-c", options.stopCommand.replace("THREAD_NAME", options.getDatabasePrefix() + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i)))};
            executeCommand(stopCommand);
            return true;
        }
        else if (MainOptions.mode == "kuzu")
        {
            String[] deleteCommand = {"/bin/bash", "-c", options.deleteCommand.replace("THREAD_NAME", options.getDatabasePrefix() + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode+ Long.toString(MainOptions.maxClauseSize) + i)};
            executeCommand(deleteCommand);
            return true;
        }
        Long time = System.currentTimeMillis();
        String[] stopCommand = {"/bin/bash", "-c", options.stopCommand.replace("THREAD_NAME", options.getDatabasePrefix() + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i)};
        executeCommand(stopCommand);
        if(MainOptions.mode == "memgraph" && MainOptions.exp == "coverage")
        {
            long fileSize = 0;
            long cnt = 0;
            while(fileSize == 0) {

                File file = new File("/home/auroraeth/memgraph/THREAD_FOLDER/default.profraw".replace("THREAD_FOLDER", MainOptions.mode+ Long.toString(MainOptions.maxClauseSize) + i));
                if(!file.exists())
                    break;
                fileSize = file.length();
                try {
                    Thread.sleep(1000);
                    cnt++;
                } catch (Exception e) {
                }
                if(cnt>5)
                    break;
            }
        }
        if(MainOptions.mode == "neo4j" && MainOptions.exp  == "coverage")
        {
            long fileSize = 0;
            long cnt = 0;
            while(fileSize == 0) {

                File file = new File("/home/auroraeth/neo4j/THREAD_FOLDER/jacoco.exec".replace("THREAD_FOLDER", MainOptions.mode+ Long.toString(MainOptions.maxClauseSize) + i));
                if(!file.exists())
                    break;
                fileSize = file.length();
                try {
                    Thread.sleep(1000);
                    cnt++;
                } catch (Exception e) {
                }
                if(cnt>5)
                    break;
            }
        }
        if(MainOptions.mode == "falkordb" && MainOptions.exp  == "coverage")
        {
            long fileSize = 0;
            long cnt = 0;
            while(fileSize == 0) {

                File file = new File("/home/auroraeth/falkordb/THREAD_FOLDER/dump.rdb".replace("THREAD_FOLDER", MainOptions.mode+ Long.toString(MainOptions.maxClauseSize) + i));
                if(!file.exists())
                    break;
                fileSize = file.length();
                try {
                    Thread.sleep(1000);
                    cnt++;
                } catch (Exception e) {
                }
                if(cnt>5)
                    break;
            }
        }
        String stopCommand2[] = {"/bin/bash", "-c", options.stopCommand.replace("THREAD_NAME", options.getDatabasePrefix() + i).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i).replace("-15", "-9")};
        executeCommand(stopCommand2);
        String[] deleteFile = {"/bin/bash", "-c", options.deleteFile.replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i).replace("TIME", time.toString())};
        executeCommand(deleteFile);
        String[] deleteCommand = {"/bin/bash", "-c", options.deleteCommand.replace("THREAD_NAME", options.getDatabasePrefix() + i).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i)};
        executeCommand(deleteCommand);

        String[] startCommand = {"/bin/bash", "-c", options.startCommand.replace("THREAD_NAME", options.getDatabasePrefix() + i).replace("THREAD_SERVER", Integer.toString((int) (10000 + i))).replace("THREAD_WEB", Integer.toString((int) (20000 + i))).replace("THREAD_FOLDER", MainOptions.mode + Long.toString(MainOptions.maxClauseSize)+ i).replace("LOG_DIRECTORY", "./logs/" + MainOptions.mode + "/")};
        executeCommand(startCommand);

        if(MainOptions.mode == "neo4j" || MainOptions.mode == "memgraph")
        {
            String uri = "bolt://127.0.0.1:" + (20000 + i);
            String user = "neo4j";
            String password = "testtest";
            boolean flag = false;

            long cnt = 0;
            while (!flag) {
                flag = false;
                if (cnt > 20)
                    return false;
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
        else if(MainOptions.mode == "thinker") {
            String uri = "bolt://localhost:" + (20000 + i);
            Cluster cluster;
            try {
                cluster = Cluster.build().port((int) (20000+i)).create();

            }
            catch (Exception e)
            {
                throw new RuntimeException("Config file for thinker not found");
            }

            TinkerConnection con = new TinkerConnection(cluster);
            boolean flag = false;
            long cnt = 0;
            while (!flag) {
                try {
                    flag = false;
                    if (cnt > 20)
                        return false;
                    con.executeStatement("MATCH (n) DETACH DELETE n");
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
        else if (MainOptions.mode == "falkordb")
        {
            boolean flag = false;
            int cnt = 0;
            Graph graph = FalkorDB.driver("127.0.0.1", (int) (20000 + i)).graph("social");
            while (!flag) {
                try {
                    graph.query("MATCH (n) DETACH DELETE n");
                    flag = true;
                    graph.close();
                    break;
                } catch (Exception e) {
                    System.out.println("wait for another moment");
                    flag = false;
                    cnt++;
                    try {
                        Thread.sleep(500);
                    } catch (Exception f) {
                        f.printStackTrace();
                    }
                }
            }
        }

        else if (MainOptions.mode == "kuzu"){}
        else{throw new RuntimeException("Mode not supported");}
        return true;
    }

    public static int executeMain(String... args) throws AssertionError {
        List<DatabaseProvider<?, ?, ?>> providers = getDBMSProviders();
        Map<String, DBMSExecutorFactory<?, ?, ?>> nameToProvider = new HashMap<>();
        MainOptions options = new MainOptions();
        Builder commandBuilder = JCommander.newBuilder().addObject(options);
        for (DatabaseProvider<?, ?, ?> provider : providers) {
            String name = provider.getDBMSName();
            if (!name.toLowerCase().equals(name)) {
                throw new AssertionError(name + " should be in lowercase!");
            }
            DBMSExecutorFactory<?, ?, ?> executorFactory = new DBMSExecutorFactory<>(provider, options);
            commandBuilder = commandBuilder.addCommand(name, executorFactory.getCommand());
            nameToProvider.put(name, executorFactory);
        }
        JCommander jc = commandBuilder.programName("SQLancer").build();
        jc.parse(args);

        if (jc.getParsedCommand() == null || options.isHelp()) {
            jc.usage();
            return (int) options.getErrorExitCode();
        }

        Randomly.initialize(options);
        if (options.printProgressInformation()) {
            startProgressMonitor();
            if (options.printProgressSummary()) {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("Overall execution statistics");
                        System.out.println("============================");
                        System.out.println(formatInteger(nrQueries.get()) + " queries");
                        System.out.println(formatInteger(nrDatabases.get()) + " databases");
                        System.out.println(
                                formatInteger(nrSuccessfulActions.get()) + " successfully-executed statements");
                        System.out.println(
                                formatInteger(nrUnsuccessfulActions.get()) + " unsuccessfuly-executed statements");
                    }

                    private String formatInteger(long intValue) {
                        if (intValue > 1000) {
                            return String.format("%,9dk", intValue / 1000);
                        } else {
                            return String.format("%,10d", intValue);
                        }
                    }
                }));
            }
        }

        ExecutorService execService = Executors.newFixedThreadPool(options.getNumberConcurrentThreads());
        String dbMode = jc.getParsedCommand();
        if(MainOptions.mode == "") MainOptions.mode = dbMode;
        else dbMode = MainOptions.mode;
        
        String startCommand = null, stopCommand = null, deleteCommand = null, deleteFile = null;
        try (BufferedReader br = new BufferedReader(new FileReader("config.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("startCommand=")) {
                    startCommand = line.replace("startCommand=", "").trim();
                }
                else if (line.startsWith("stopCommand=")) {
                    stopCommand = line.replace("stopCommand=", "").trim();
                }
                else if (line.startsWith("resetCommand=")) {
                    deleteCommand = line.replace("resetCommand=", "").trim();
                    deleteFile = deleteCommand;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(startCommand == null || stopCommand == null || deleteCommand == null || deleteFile == null) {
            throw new AssertionError("Incorrect config file! Missing startCommand, stopCommand or resetCommand");
        }
        options.deleteCommand = deleteCommand;
        options.deleteFile = deleteFile;
        options.startCommand = startCommand;
        options.stopCommand = stopCommand;
        if(Objects.equals(dbMode, "falkordb"))
        {
            options.startCommandFalkorDB = startCommand;
            options.stopCommandFalkorDB = stopCommand;
            options.deleteCommandFalkorDB = deleteCommand;
            options.deleteFileFalkorDB = deleteFile;
        }
        if(Objects.equals(dbMode, "kuzu"))
        {
            options.deleteFileKuzuDB = deleteFile;
        }


        DBMSExecutorFactory<?, ?, ?> executorFactory = nameToProvider.get(dbMode);

        if (options.performConnectionTest()) {
            try {
                executorFactory.getDBMSExecutor(options.getDatabasePrefix() + "connectiontest", new Randomly())
                        .testConnection();
            } catch (Exception e) {
                System.err.println(
                        "SQLancer failed creating a test database, indicating that SQLancer might have failed connecting to the DBMS. In order to change the username, password, host and port, you can use the --username, --password, --host and --port options.\n\n");
                e.printStackTrace();
                return (int) options.getErrorExitCode();
            }
        }
        HashMap<Integer, Boolean> databaseStatus = new HashMap<>();

            for (int i = 0; i < options.getNumberConcurrentThreads(); i++) {
                if(MainOptions.needReset && MainOptions.debug == -1) {
                    rebootDatabase(i, options);








































                }
                databaseStatus.put(i, false);
            }




        for (int i = 0; i < options.getTotalNumberTries(); i++) {
            final String databaseName;
            if(MainOptions.debug == -1)
                databaseName = options.getDatabasePrefix() + (i%options.getNumberConcurrentThreads());
            else
                databaseName = options.getDatabasePrefix() + Integer.toString((int) MainOptions.debug);
            execService.execute(new Runnable() {
                long seed = 0;

                @Override
                public void run() {
                    long randomSleep = (new Random()).nextInt(30);
                    try {
                        Thread.sleep(randomSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }





                    String availableDatabase = databaseName.split("-")[0].replace(options.getDatabasePrefix(), "");















                    Thread.currentThread().setName(databaseName + "-" + Long.toString(seed) + "-" + availableDatabase);
                    runThread(databaseName + "-" + Long.toString(seed) + "-" + availableDatabase);
                }

                private void runThread(String databaseName) {
                    String occupiedDatabase = databaseName.split("-")[2];
                    String databasePrefix = databaseName.split("-")[0];
                    databaseStatus.put(Integer.parseInt(occupiedDatabase), true);
                    try {
                        if (options.getMaxGeneratedDatabases() == -1) {

                            boolean continueRunning = true;
                            long counter = 1;
                            long thinkercounter = 0;

                            while (continueRunning){
                                thinkercounter += 1;
                                if(thinkercounter > options.getTotalNumberTries() && MainOptions.mode == "thinker")
                                    break;
                                System.gc();

                                if(MainOptions.mode == "neo4j") {
                                    try{Thread.sleep(new Random().nextInt(1000));}
                                    catch (Exception e){e.printStackTrace();}
                                    if (counter == 1 && MainOptions.needReset != false) {
                                        boolean temp = rebootDatabase(Integer.parseInt(occupiedDatabase), options);
                                        if (!temp)
                                            break;
                                    }
                                }
                                else if (MainOptions.mode == "memgraph")
                                {
                                    if (counter == 1 && MainOptions.needReset != false) {
                                        boolean temp = rebootDatabase(Integer.parseInt(occupiedDatabase), options);
                                        if (!temp)
                                            break;
                                    }


                                }

                                else if (MainOptions.mode == "thinker")
                                {
                                    if (counter == 1 && MainOptions.needReset != false) {
                                        boolean temp = rebootDatabase(Integer.parseInt(occupiedDatabase), options);
                                        if (!temp)
                                            break;
                                    }


                                }
                                else if (MainOptions.mode == "falkordb")
                                {
                                    if (counter == 1 && MainOptions.needReset != false) {
                                        boolean temp = rebootDatabase(Integer.parseInt(occupiedDatabase), options);
                                        if (!temp)
                                            break;
                                    }

                                }
                                else if (MainOptions.mode == "kuzu")
                                {
                                    if (counter == 1 && MainOptions.needReset != false) {
                                        boolean temp = rebootDatabase(Integer.parseInt(occupiedDatabase), options);
                                        if (!temp)
                                            break;
                                    }
                                }
                                else
                                {
                                    throw new RuntimeException("Mode not supported");
                                }


                                if(MainOptions.realRandomSeed==-1)
                                    seed = System.currentTimeMillis();
                                else
                                    seed = MainOptions.realRandomSeed;
                                Randomly r = new Randomly(seed);
                                databaseName = databasePrefix + "-" + Long.toString(seed) + "-" + occupiedDatabase;
                                continueRunning = run(options, execService, executorFactory, r, databaseName);
                            }
                        } else {






                        }
                    } finally {
                        databaseStatus.put(Integer.parseInt(occupiedDatabase), false);
                        threadsShutdown++;
                        if (threadsShutdown == options.getTotalNumberTries()) {
                            execService.shutdown();
                        }
                    }
                }

                private boolean run(MainOptions options, ExecutorService execService,
                        DBMSExecutorFactory<?, ?, ?> executorFactory, Randomly r, final String databaseName) {
                    DBMSExecutor<?, ?, ?> executor = executorFactory.getDBMSExecutor(databaseName, r);
                    try {
                        executor.run();
                        return true;
                    } catch (IgnoreMeException e) {
                        return true;
                    } catch (Throwable reduce) {
                        reduce.printStackTrace();
                        executor.getStateToReproduce().exception = reduce.getMessage();
                        executor.getLogger().logFileWriter = null;
                        executor.getLogger().logException(reduce, executor.getStateToReproduce());
                        return true;
                    } finally {
                        try {
                            if (options.logEachSelect()) {
                                if (executor.getLogger().currentFileWriter != null) {
                                    executor.getLogger().currentFileWriter.close();
                                }
                                executor.getLogger().currentFileWriter = null;
                            }

                            if(MainOptions.mode == "memgraph") {
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        try {
            if (options.getTimeoutSeconds() == -1) {
                execService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } else {
                execService.awaitTermination(options.getTimeoutSeconds(), TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return threadsShutdown == 0 ? 0 : (int) options.getErrorExitCode();
    }

    public static void executeCommand(String[] startCommand) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            Process process = builder.command(startCommand).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                System.err.println(errorLine);
            }
            errorReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static List<DatabaseProvider<?, ?, ?>> providers = new ArrayList<>();
    public static List<DatabaseProvider<?, ?, ?>> getDBMSProviders() {
        if(providers.size()==0){
            providers.add(new Neo4jProvider());
            providers.add(new PrintGraphProvider());
            providers.add(new AgensGraphProvider());
            providers.add(new RedisGraphProvider());
            providers.add(new MemGraphProvider());
            providers.add(new ArcadeDBProvider());
            providers.add(new JanusProvider());
            providers.add(new TinkerProvider());
            providers.add(new CompositeProvider());
            providers.add(new KuzuGraphProvider());
        }
        return providers;
    }

    private static synchronized void startProgressMonitor() {
        if (progressMonitorStarted) {
            
            return;
        } else {
            progressMonitorStarted = true;
        }
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                
                List<String> listTest = Arrays.asList(dateFormat.format(date), String.valueOf(CompositeMCTSOracle.maxDc),
                        String.valueOf(CompositeMCTSOracle.maxSeq), String.valueOf(CompositeMCTSOracle.maxDr), String.valueOf(CompositeMCTSOracle.maxDa),
                        String.valueOf(CompositeMCTSOracle.maxT1),String.valueOf(CompositeMCTSOracle.maxT2),
                        String.valueOf(CompositeMCTSOracle.numofQueries), String.valueOf(CompositeMCTSOracle.numofTimeOut));

                WritableWorkbook wwb = null;
                InputStream io = null;
                Workbook wb = null;








































            }
        }, 20, 60, TimeUnit.SECONDS);
    }

}
