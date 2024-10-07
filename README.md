# Testing Graph Databases with Synthesized Queries

## Previously Unknown Bugs Detected

GQS has detected 35 bugs in Neo4j, Memgraph, FalkorDB and Kuzu in total. 25 of them are logic bugs, and 10 of them are other bugs, like database crash or unhandled exception. The bug list is as follows:

| Bug Numer | Database | Type  | Issue                                            |
| --------- | -------- | ----- | ------------------------------------------------ |
| #1        | Neo4j    | Logic | https://github.com/neo4j/neo4j/issues/13359      |
| #2        | Neo4j    | Logic | https://github.com/neo4j/neo4j/issues/13489      |
| #3        | Neo4j    | Other | https://github.com/neo4j/neo4j/issues/13457      |
| #4        | Neo4j    | Other | https://github.com/neo4j/neo4j/issues/13469      |
| #5        | Neo4j    | Other | https://github.com/neo4j/neo4j/issues/13473      |
| #6        | Memgraph | Logic | https://github.com/memgraph/memgraph/issues/1628 |
| #7        | Memgraph | Logic | https://github.com/memgraph/memgraph/issues/1648 |
| #8        | Memgraph | Logic | https://github.com/memgraph/memgraph/issues/1665 |
| #9        | Memgraph | Logic | https://github.com/memgraph/memgraph/issues/1676 |
| #10       | Memgraph | Logic | https://github.com/memgraph/memgraph/issues/1818 |
| #11       | Memgraph | Logic | https://github.com/memgraph/memgraph/issues/2244 |
| #12       | Memgraph | Other | https://github.com/memgraph/memgraph/issues/1822 |
| #13       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/731  |
| #14       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/735  |
| #15       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/741  |
| #16       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/752  |
| #17       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/755  |
| #18       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/762  |
| #19       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/783  |
| #20       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/774  |
| #21       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/775  |
| #22       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/776  |
| #23       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/777  |
| #24       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/778  |
| #25       | FalkorDB | Logic | https://github.com/FalkorDB/FalkorDB/issues/784  |
| #26       | FalkorDB | Other | https://github.com/FalkorDB/FalkorDB/issues/751  |
| #27       | FalkorDB | Other | https://github.com/FalkorDB/FalkorDB/issues/753  |
| #28       | FalkorDB | Other | https://github.com/FalkorDB/FalkorDB/issues/756  |
| #29       | FalkorDB | Other | https://github.com/FalkorDB/FalkorDB/issues/798  |
| #30       | Kuzu     | Logic | https://github.com/kuzudb/kuzu/issues/3890       |
| #31       | Kuzu     | Logic | https://github.com/kuzudb/kuzu/issues/3906       |
| #32       | Kuzu     | Logic | https://github.com/kuzudb/kuzu/issues/3909       |
| #33       | Kuzu     | Logic | https://github.com/kuzudb/kuzu/issues/4189       |
| #34       | Kuzu     | Other | https://github.com/kuzudb/kuzu/issues/3892       |
| #35       | Kuzu     | Other | https://github.com/kuzudb/kuzu/issues/3897       |

The test cases involve 29 functions, including:
```
{'reverse', 'toBoolean', 'upper', 'count', 'e', 'replace', 'toUpper', 'toInteger', 'endNode', 'suffix', 'split', 'min', 'exp', 'lTrim', 'cos', 'starts_with', 'right', 'sum', 'type', 'CAST', 'abs', 'toLower', 'rTrim', 'size', 'substring', 'collect', 'floor', 'acos', 'max'}
```

## GQS Compile
To build GQS, please make sure that you have the following environment installed:
```
Maven 3.9.6
Java-JDK 21.0
```
Then, using the following command to compile the source code:
```
mvn install -DskipTests -T1C
```
You will get the executable JAR file at "targets/"


## GQS Usage

GQS is implemented based on Java.
To run the GQS, please make sure that you have correctly installed `Java 21.0`.
The GQS requires a database mode parameter specifying the database you want to test. Currently, GQS supports the following database mode:

```
neo4j -> Neo4j
memgraph -> Memgraph
falkordb -> FalkorDB
kuzu -> Kùzu
```
Before running GQS, please correctly configure the `config.txt`, which specifies the commands to start, stop and reset the database system. GQS executes these commands everytime a new test cycle starts.
In the following, we describe the configuration process of each database in detail.

### Neo4j
1. Download the Neo4j source code from the Github repository (https://github.com/neo4j/neo4j).
2. Compile and unzip the compiled compression file to get the following folder structure. Please put the `change_port.sh` in our repository into the folder. This script will change the port configuration of Neo4j to make sure GQS can correctly connect to the database under test.
```
├── bin
├── certificates
├── change_port.sh // From our repository
├── conf // Configuration files to adjust
├── data
├── import
├── labs
├── lib
├── licenses
├── LICENSES.txt
├── LICENSE.txt
├── logs
├── NOTICE.txt
├── packaging_info
├── plugins
├── README.txt
├── run
└── UPGRADE.txt
```
3. Edit the configuration `conf/neo4j.conf`. Change the following item:
```
server.bolt.listen_address=127.0.0.1:___PORT___1
server.http.listen_address=127.0.0.1:___PORT___2
```
1. Configure the `config.txt` based on the directory of your Neo4j database. The following commands give you an example when you install the Neo4j database in `neo4j` folder at the current directory, and place testing temporary files in `~/neo4j/`. Please note that `THREAD_FOLDER` is a constant that will be subsititued by GQS to put databases under different testing threads to distinct locations. `THREAD_WEB` and `THREAD_SERVER` will be subsititued by GQS to specify the port.  `LOG_DIRECTORY` will be changed to the corresponding log folder.
```
startCommand=aa=$PWD; mkdir -p ~/neo4j/THREAD_FOLDER; cd ~/neo4j/THREAD_FOLDER; cp -r $aa/neo4j ~/neo4j/THREAD_FOLDER; mkdir -p ./logs/neo4j; cd neo4j; ./change_port.sh conf/neo4j.conf THREAD_WEB THREAD_SERVER; ./bin/neo4j-admin server console 2>&1 &
stopCommand=kill -9 `netstat -tulnp | grep :THREAD_WEB | awk '{print $7}' | cut -d'/' -f1`
resetCommand=rm -rf ~/neo4j/THREAD_FOLDER
```
Please make sure that the port `20000` is not occupied by any other processes. Use the following command to initiate the testing process:
```
java -jar GQS-1.0-SNAPSHOT.jar neo4j
```
You will find the testing log and results in `logs` folder. 

### Memgraph
1. Download the Memgraph source code from the Github repository (https://github.com/memgraph/memgraph).
2. Compile the source code and copy the Memgraph database binary into the current directory. 
3. Configure the `config.txt` based on your needs. The following commands give you an example when you put the Memgraph binary at the current folder, and testing temporary files in `~/memgraph/`. Please note that `THREAD_FOLDER` is a constant that will be subsititued by GQS to put databases under different testing threads to distinct locations. `THREAD_WEB` will be subsititued by GQS to specify the port. 
```
startCommand=aa=$PWD; mkdir -p ~/memgraph/THREAD_FOLDER; cd ~/memgraph/THREAD_FOLDER; mkdir -p ./logs/memgraph; cp $aa/memgraph ~/memgraph/THREAD_FOLDER; cp $aa/libmemgraph_module_support.so  ~/memgraph/THREAD_FOLDER/; ./memgraph --bolt-address 127.0.0.1 --bolt-port THREAD_WEB --storage-properties-on-edges true --query-execution-timeout-sec=30 > LOG_DIRECTORY/THREAD_FOLDER.log 2>&1 &
stopCommand=kill -9 `netstat -tulnp | grep :THREAD_WEB | awk '{print $7}' | cut -d'/' -f1`;rm -rf ~/memgraph/THREAD_FOLDER
resetCommand=kill -9 `netstat -tulnp | grep :THREAD_WEB | awk '{print $7}' | cut -d'/' -f1`;rm -rf ~/memgraph/THREAD_FOLDER
```
Please make sure that the port `20000` is not occupied by any other processes. Use the following command to initiate the testing process:
```
java -jar GQS-1.0-SNAPSHOT.jar memgraph
```
You will find the testing log and results in `logs` folder. 

### FalkorDB
1. Download the FalkorDB source code from the Github repository (https://github.com/FalkorDB/FalkorDB).
2. Compile the source code and copy the suitable `falkordb.so` library into the directory where redis server is installed, configure the redis server to automatically load this module once started.
3. Configure the `config.txt` based on your needs. The following commands give you an example when you put the redis server and the falkordb library into the `falkordb` folder at the current directory, and testing temporary files in `~/falkordb/`. Make sure you also put `change_port.sh` into the redis server folder to enable GQS automatically change the port redis server binds to. Please note that `THREAD_FOLDER` is a constant that will be subsititued by GQS to put databases under different testing threads to distinct locations. `THREAD_WEB` will be subsititued by GQS to specify the port. 
```
startCommand=aa=$PWD/falkordb/; mkdir -p ~/falkordb/THREAD_FOLDER; cd ~/falkordb/THREAD_FOLDER; mkdir -p ./logs/falkordb/; cp $aa/* ~/falkordb/THREAD_FOLDER;  ./change_port.sh redis.conf THREAD_WEB THREAD_WEB; ./redis-server ./redis.conf > LOG_DIRECTORY/THREAD_FOLDER.log 2>&1 &
stopCommand=kill -9 `netstat -tulnp | grep :THREAD_WEB | awk '{print $7}' | cut -d'/' -f1`
resetCommand=rm -rf ~/falkordb/THREAD_FOLDER
```
4. Revise the redis server configuration `redis.conf`, specifically in the following item.
```
port ___PORT___1
```
Please make sure that the port `20000` is not occupied by any other processes. Use the following command to initiate the testing process:
```
java -jar GQS-1.0-SNAPSHOT.jar falkordb
```
You will find the testing log and results in `logs` folder. 

### Kuzu

Kuzu database is already embedded in the GQS. You do not need to download or compile it, but just need to correctly configure the `config.txt`. By default, the temporary testing files will be put at `~/kuzu`.
```
startCommand=random
stopCommand=random
resetCommand=rm -rf ~/kuzu/THREAD_FOLDER
```
Use the following command to initiate the testing process:
```
java -jar GQS-1.0-SNAPSHOT.jar kuzu
```
You will find the testing log and results in `logs` folder. 

