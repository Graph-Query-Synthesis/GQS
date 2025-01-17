package org.example.gqs.composite;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.example.gqs.DBMSSpecificOptions;
import org.example.gqs.IGeneratorFactory;
import org.example.gqs.IGraphGeneratorFactory;
import org.example.gqs.OracleFactory;
import org.example.gqs.composite.gen.CompositeGraphGenerator;
import org.example.gqs.composite.gen.CompositePatternBasedGraphGenerator;
import org.example.gqs.composite.oracle.*;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.cypher.dsl.IGraphGenerator;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.gen.query.AdvancedQueryGenerator;
import org.example.gqs.cypher.gen.graph.ManualGraphGenerator;
import org.example.gqs.cypher.gen.query.ManualQueryGenerator;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Parameters(separators = "=", commandDescription = "Composite (default port: " + CompositeOptions.DEFAULT_PORT
        + ", default host: " + CompositeOptions.DEFAULT_HOST)
public class CompositeOptions implements DBMSSpecificOptions<CompositeOptions.CompositeOracleFactory> {

    public static final String DEFAULT_HOST = "localhost";
    public static final long DEFAULT_PORT = 2424;


    @Parameter(names = "--oracle")
    public List<CompositeOracleFactory> oracles = Arrays.asList(CompositeOracleFactory.DIFFERENTIAL);

    @Parameter(names = "--generator")
    public CompositeGeneratorFactory generator = CompositeGeneratorFactory.RANDOM;

    @Parameter(names = "--graph")
    public CompositeGraphGeneratorFactory graphGenerator = CompositeGraphGeneratorFactory.RANDOM;

    public String getConfigPath() {
        return configPath;
    }

    @Parameter(names = "--config")
    public String configPath = "./config.json";

    @Override
    public List<CompositeOracleFactory> getTestOracleFactory() {
        return oracles;
    }

    @Override
    public IQueryGenerator<CompositeSchema, CompositeGlobalState> getQueryGenerator() {
        return generator.create();
    }

    public enum CompositeGraphGeneratorFactory implements IGraphGeneratorFactory<CompositeGlobalState, IGraphGenerator<CompositeGlobalState>>{
        RANDOM{
            @Override
            public IGraphGenerator<CompositeGlobalState> create(CompositeGlobalState globalState) {
                return new CompositeGraphGenerator(globalState);
            }
        },
        MANUAL{
            @Override
            public IGraphGenerator<CompositeGlobalState> create(CompositeGlobalState globalState) {
                ManualGraphGenerator<CompositeGlobalState> manualGraphGenerator = new ManualGraphGenerator<>();
                manualGraphGenerator.loadFile("./graph.txt");
                return manualGraphGenerator;
            }
        },
        PATTERN_BASED{
            @Override
            public IGraphGenerator<CompositeGlobalState> create(CompositeGlobalState globalState) {
                return new CompositePatternBasedGraphGenerator(globalState);
            }
        }
    }




    public enum CompositeGeneratorFactory implements IGeneratorFactory<IQueryGenerator<CompositeSchema,CompositeGlobalState>>{
        RANDOM {
            @Override
            public IQueryGenerator<CompositeSchema, CompositeGlobalState> create() {
                return new RandomQueryGenerator<>();
            }
        },

        ADVANCED{
            @Override
            public IQueryGenerator<CompositeSchema, CompositeGlobalState> create() {
                return new AdvancedQueryGenerator<>();
            }
        },

        MANUAL{
            @Override
            public IQueryGenerator<CompositeSchema, CompositeGlobalState> create() {
                ManualQueryGenerator<CompositeSchema, CompositeGlobalState> generator = new ManualQueryGenerator<>();
                generator.loadFile("./query.txt");
                return generator;
            }
        }
    }

    public enum CompositeOracleFactory implements OracleFactory<CompositeGlobalState> {

        ALWAYS_TRUE {

            @Override
            public TestOracle create(CompositeGlobalState globalState) throws SQLException {
                return new CompositeAlwaysTrueOracle(globalState);
            }
        },
        DIFFERENTIAL {
            @Override
            public TestOracle create(CompositeGlobalState globalState) throws SQLException{
                return new CompositeDifferentialOracle(globalState);
            }
        },
        PERFORMANCE {
            @Override
            public TestOracle create(CompositeGlobalState globalState) throws SQLException{
                return new CompositePerformanceOracle(globalState);
            }
        },
        PURE_PERFORMANCE{
            @Override
            public TestOracle create(CompositeGlobalState globalState) throws SQLException{
                return new CompositePurePerformanceOracle(globalState);
            }
        },
        MCTS {
            @Override
            public TestOracle create(CompositeGlobalState globalState) throws SQLException{
                return new CompositeMCTSOracle(globalState);
            }
        }
    }
}
