package edu.umd.cs.psl.er

import java.io.FileReader
import java.util.concurrent.TimeUnit

import edu.umd.cs.psl.application.inference.LazyMPEInference;
import edu.umd.cs.psl.evaluation.resultui.UIFullInferenceResult;
import edu.umd.cs.psl.evaluation.result.FullInferenceResult;
import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.database.rdbms.driver.DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabaseQuery;
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Partition
import edu.umd.cs.psl.database.ResultList
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.er.evaluation.ModelEvaluation
import edu.umd.cs.psl.er.similarity.DiceSimilarity;
import edu.umd.cs.psl.er.similarity.SameInitials;
import edu.umd.cs.psl.er.similarity.SameNumTokens;
import edu.umd.cs.psl.er.similarity.SameDate;
import edu.umd.cs.psl.er.similarity.SameGender;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.IntegerAttribute;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.atom.QueryAtom;
import edu.umd.cs.psl.model.formula.Conjunction;
import edu.umd.cs.psl.model.formula.Formula;
import edu.umd.cs.psl.groovy.PSLModel;
import edu.umd.cs.psl.groovy.*
import edu.umd.cs.psl.groovy.experiments.ontology.*
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.er.evaluation.FileAtomPrintStream;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.LazyMaxLikelihoodMPE;
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;

/*
 * Start and end times for timing information.
 */
def startTime;
def endTime;

def datadir = "/ucsc/psl/er-example/data/YAGO-IMDB";
if (!datadir[datadir.size()-1].equals("/"))
  datadir += "/";

boolean learnWeights = false;
if (args.size() >= 1) 
{
  learnWeights = args[1..(args.size()-1)].contains("-l");
}

println "\n*** ER YAGO-IMDB EXAMPLE ***\n"
println "Data directory  : " + datadir;
println "Weight learning : " + (learnWeights ? "ON" : "OFF");

/*
 * We'll use the ConfigManager to access configurable properties.
 * This file is usually in <project>/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager();
ConfigBundle cb = cm.getBundle("er-yago-example");

/*** LOAD DATA ***/
println "Creating a new DB and loading data:"

/*
 * We'll create a new relational DB.
 */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = cb.getString("dbpath", 
                 defaultPath + File.separator + "er-yago-example")

DataStore data = new RDBMSDataStore(
  new H2DatabaseDriver(Type.Disk, dbpath, true), cb)

/*** MODEL DEFINITION ***/
print "Creating the ER model ... "
PSLModel m = new PSLModel(this, data);

/*
 * Relations:
 *   IMDB                YAGO
 * ========		=======
 * actedIn              actedIn
 * bornIn               wasBornIn
 * deceasedIn           diedIn
 * directorOf           directed
 * producerOf           produced
 * writerOf             created
 * locatedIn            capitalOf
 * 
 * Properties:
 *   IMDB                YAGO
 * ========		=======
 * hasLabel             hasLabel
 * bornOn               wasBornOnDate
 * deceasedOn           diedOnDate
 * firstName            hasGivenName
 * gender               hasGender
 * hasHeight            hasHeight
 * lastName             hasFamilyName
 *                      wasCreatedOnDate
 */

/*
 * These are the predicates for our model.
 * Predicates are precomputed.
 * Functions are computed online.
 * "Open" predicates are ones that must be inferred.
 */

/*
 * IMDB properties
 */
m.add predicate: "Yago_hasLabel",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "bornOn",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "deceasedOn",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "firstName",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "gender",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "lastName",
  types: [ArgumentType.UniqueID, ArgumentType.String]

/*  
 * YAGO properties
 */
m.add predicate: "Imdb_hasLabel",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "wasBornOnDate",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "diedOnDate",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "hasGivenName",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "hasGender",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
m.add predicate: "hasFamilyName",
  types: [ArgumentType.UniqueID, ArgumentType.String]

m.add predicate: "wasCreatedOnDate",
  types: [ArgumentType.UniqueID, ArgumentType.String]
  
/*
 * IMDB relations
 */
m.add predicate: "actedIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "bornIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "deceasedIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "directorOf",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "producerOf",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "writerOf",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "locatedIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]


/*
 * YAGO relations
 */
m.add predicate: "actedIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "wasBornIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "diedIn",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "directed",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "produced",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "created",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]


/*
 * Declare similarity functions
 */
m.add function: "simLabel",  implementation: new JaroWinklerSimilarity(0.75);

m.add function: "simDate",   implementation: new SameDate(0.9);

m.add function: "simName",   implementation: new JaccardSimilarity(0.75);

m.add function: "simGender", implementation: new SameGender(1.0);

m.add predicate: "sameLabel" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "samePerson" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "sameMovie" , 
  types: [ArgumentType.String, ArgumentType.String]


/*
 * Set comparison functions operate on sets and return a scalar.
 */
m.add setcomparison: "sameActorSet" , 
  using: SetComparison.CrossEquality, on : samePerson;

m.add setcomparison: "sameProducerSet" , 
  using: SetComparison.CrossEquality, on : samePerson;

m.add setcomparison: "sameMovieSet" , 
  using: SetComparison.CrossEquality, on : sameMovie;

/*
 * Here are some basic rules.
 */
// similar person attributes => same person
//
// Here we will consider every attribute, as long as all the
// attributes for the compared objects are non-null.
//
m.add rule : (hasLabel_Yago(A1,L1) & hasLabel_Imdb(A2,L2) & simLabel(L1,L2) &
	      firstName(A1,F1) & lastName(A1,L1) &
              hasGivenName(A2,F2) & hasFamilyName(A2,L2) &
	      simName(F1,F2) & simName(L1,L2)) &
	      bornOn(A1,B1) & wasBornOnDate(A2,B2) & simDate(B1,B2) &
	      deceasedOn(A1,B3) & diedOnDate(A2,B4) & simDate(B3,B4) &
	      gender(A1,G1) & hasGender(A2,G2) & simGender(G1,G2) >>
              samePerson(A1,A2), weight : 1.0;

// similar person attributes => same person
//
// label not available
//
m.add rule : (firstName(A1,F1) & lastName(A1,L1) &
              hasGivenName(A2,F2) & hasFamilyName(A2,L2) &
	      simName(F1,F2) & simName(L1,L2)) &
	      bornOn(A1,B1) & wasBornOnDate(A2,B2) & simDate(B1,B2) &
	      deceasedOn(A1,B3) & diedOnDate(A2,B4) & simDate(B3,B4) &
	      gender(A1,G1) & hasGender(A2,G2) & simGender(G1,G2) >>
              samePerson(A1,A2), weight : 1.0;

// similar person attributes => same person
//
// birth & deceased dates not available
//
m.add rule : (hasLabel_Yago(A1,L1) & hasLabel_Imdb(A2,L2) & simLabel(L1,L2) &
	      firstName(A1,F1) & lastName(A1,L1) &
              hasGivenName(A2,F2) & hasFamilyName(A2,L2) &
	      simName(F1,F2) & simName(L1,L2)) &
	      gender(A1,G1) & hasGender(A2,G2) & simGender(G1,G2) >>
              samePerson(A1,A2), weight : 1.0;

// similar person attributes => same person
//
// gender not available
//
m.add rule : (hasLabel_Yago(A1,L1) & hasLabel_Imdb(A2,L2) & simLabel(L1,L2) &
	      firstName(A1,F1) & lastName(A1,L1) &
              hasGivenName(A2,F2) & hasFamilyName(A2,L2) &
	      simName(F1,F2) & simName(L1,L2)) &
	      bornOn(A1,B1) & wasBornOnDate(A2,B2) & simDate(B1,B2) &
	      deceasedOn(A1,B3) & diedOnDate(A2,B4) & simDate(B3,B4) >>
              samePerson(A1,A2), weight : 1.0;

// similar person attributes => same person
//
// gender & label not available
//
m.add rule : (firstName(A1,F1) & lastName(A1,L1) &
              hasGivenName(A2,F2) & hasFamilyName(A2,L2) &
	      simName(F1,F2) & simName(L1,L2)) &
	      bornOn(A1,B1) & wasBornOnDate(A2,B2) & simDate(B1,B2) &
	      deceasedOn(A1,B3) & diedOnDate(A2,B4) & simDate(B3,B4) >>
              samePerson(A1,A2), weight : 1.0;

m.add rule : (sameAuthorSet({P1.authorOf(inv)},{P2.authorOf(inv)}) & 
	      paperTitle(P1,T1) & paperTitle(P2,T2) & sameBib(P1,P2) &
              sameNumTokens(T1,T2)) >> 
              sameTitle(T1,T2),  weight : 1.0;

/*
 * Here are some relational rules for movies.
 */

m.add rule : (sameActorSet({A1.actedIn_yago(inv)},{A2.actedIn_imdb(inv)}) &
	      hasLabel_Yago(A1,L1) & hasLabel_Imdb(A2,L2) & simLabel(L1,L2)) >>
 	      sameMovie(A1,A2), weight : 1.0;

m.add rule : (sameProducerSet({A1.producerOf(inv)},{A2.produced(inv)}) &
	      hasLabel_Yago(A1,L1) & hasLabel_Imdb(A2,L2) & simLabel(L1,L2)) >>
	      sameMovie(A1,A2), weight : 1.0;

m.add rule : (directorOf(A1,M1) & directed(A2,M2) &
	      samePerson(A1,A2)) >> sameMovie(M1,M2), weight : 1.0;

/* 
 * Now we'll add a prior to the open predicates.
 */
m.add rule: ~samePerson(A,B), weight: 1E-6;
m.add rule: ~sameMovie(A,B),  weight: 1E-6;

println "done!"

/*
 * These are just some constants that we'll use to reference data files 
 * and DB partitions.
 *
 * To change the dataset (e.g. big, medium, small, tiny), 
 * change the dir variable.
 */
int trainingFold = 0;
int testingFold = 1;

Partition evidenceTrainingPartition = new Partition(1);
Partition evidenceTestingPartition  = new Partition(2);
Partition targetTrainingPartition   = new Partition(3);
Partition targetTestingPartition    = new Partition(4);

/* 
 * Now we'll load some data from tab-delimited files into the DB.
 * Note that the second parameter to each call to loadFromFile() 
 * determines the DB partition.
 */
def sep = java.io.File.separator;
def insert;

/*
 * define a function to insert data from a specified data set
 */
/*
 * define a function to insert ground truth data
 */
def insertData(dataFile,partition)
{
  print "  Reading " + dataFile + " ... ";
  insert = data.getInserter(p1,partition);
  InserterUtils.loadDelimitedData(insert, dataFile);
  println "done!";
}

/* 
 * First, read the training and testing data from the IMDB data.
 */
for (Predicate p1 : [actedIn_imdb,bornOn,deceasedOn,
		     directorOf,firstName,gender,
		     hasHeight,hasLabel_imdb,lastName,
		     producerOf])
{
  if (learnWeights)
  {
    String dataFile = datadir + "training/imdb" + sep + p1.getName() + ".txt";
    insertData(datafile,evidenceTrainingPartition);
  }

  String dataFile = datadir + "testing/imdb" + sep + p1.getName() + ".txt";
  insertData(dataFile,evidenceTestingPartition);
}

/* 
 * Next, read the training and testing data from the YAGO data.
 */
for (Predicate p1 : [actedIn_yago,diedOnDate,directed,hasFamilyName,
		     hasGender,hasGivenName,hasLabel_yago,produced,
		     wasBornOnDate])
{
  if (learnWeights)
  {
    String dataFile = datadir + "training/yago" + sep + p1.getName() + ".txt";
    insertData(dataFile,evidenceTrainingPartition);
  }

  String dataFile = datadir + "testing/yago" + sep + p1.getName() + ".txt";
  insertData(dataFile,evidenceTestingPartition);
}

/* 
 * Now we read the target predicate data.
 */
for (Predicate p3 : [samePerson,sameMovie])
{
  if (learnWeights)
  {
    String dataFile = datadir + "training" + sep + p3.getName() + ".txt";
    insertData(dataFile,targetTrainingPartition);
  }
    
  String dataFile = datadir + "testing" + sep + p3.getName() + ".txt";
  insertData(dataFile,targetTestingPartition);
}

///Then, for weight learning, you can create empty write partitions.
Partition trainWrite = new Partition(100);
Partition truthWrite = new Partition(101);

/*** WEIGHT LEARNING ***/

/*
* This is how we perform weight learning.
* Note that one must specify the open predicates and the evidence
* and target partitions.
*/
if (learnWeights)
{
  // Now the trainDB will use just the evidence and infer values for
  // samePerson/sameMovie into trainWrite
  Database trainDB =
    data.getDatabase(trainWrite,
		     [actedIn_imdb,bornOn,deceasedOn,directorOf,firstName,
		      gender,hasHeight,hasLabel_imdb,lastName,producerOf,
		      actedIn_yago,diedOnDate,directed,hasFamilyName,
		      hasGender,hasGivenName,hasLabel_yago,produced,
		      wasBornOnDate] as Set,
		     evidenceTrainingPartition);

  //OPEN WORLD ASSUMPTION
  Database truthDB = data.getDatabase(truthWrite,
				      [ ] as Set, targetTrainingPartition,
				      evidenceTrainingPartition);

  LazyMaxLikelihoodMPE weightLearning =
    new LazyMaxLikelihoodMPE(m, trainDB, truthDB, cb);

  /*
   * Now we run the learning algorithm.
   */
  print "\nStarting weight learning ... ";
  startTime = System.nanoTime();
  weightLearning.learn();
  endTime = System.nanoTime();
  println "done!";
  println "  Elapsed time: " + 
    TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";
  weightLearning.close();
    
  /*
   * Now let's print the model to see the learned weights.
   */
  println "Learned model:\n";
  println m;
}

/*** INFERENCE ***/

/*
 * Note: to run evaluation of ER inference, we need to specify 
 * the total number of pairwise combinations of authors and papers,
 * which we pass to evaluateModel() in an array. This is for memory
 * efficiency, since we don't want to actually load truth data for all
 * possible pairs (though one could).
 *
 * To get the total number of possible combinations, we'll scan the 
 * label files, counting the number of lines.
 */
int[] imdbCnt = new int[2];
int[] yagoCnt = new int[2];
FileReader rdr = null;
rdr = new FileReader(datadir + "training/imdb/HASLABEL_IMDB.txt");
while (rdr.readLine() != null) 
  imdbCnt[0]++;
println "IMDB training labels  fold " +  ": " + imdbCnt[0];

rdr = new FileReader(datadir + "testing/imdb/HASLABEL_IMDB.txt");
while (rdr.readLine() != null) 
  imdbCnt[1]++;
println "IMDB testing labels  fold " +  ": " + imdbCnt[1];

rdr = new FileReader(datadir + "training/yago/HASLABEL_YAGO.txt");
while (rdr.readLine() != null) 
  yagoCnt[0]++;
println "YAGO training labels  fold " +  ": " + yagoCnt[0];

rdr = new FileReader(datadir + "testing/yago/HASLABEL_YAGO.txt");
while (rdr.readLine() != null) 
  yagoCnt[1]++;
println "YAGO testing labels  fold " +  ": " + yagoCnt[1];

/*
 * Let's create an instance of our evaluation class.
 */
ModelEvaluation eval = new ModelEvaluation(data);
Partition inferenceTrainingWrite = new Partition(102);
Partition evalTrainingWrite = new Partition(103);
Partition inferenceTestingWrite = new Partition(104);
Partition evalTestingWrite = new Partition(105);

/*
 * Evalute inference on the training set.
 */
print "\nStarting inference on the training fold ... ";
startTime = System.nanoTime();

int imdbFactor;
int yagoFactor;

//
// Training partition
//
if (learnWeights)
{
  Database inferenceTrainingDB = 
    data.getDatabase(inferenceTrainingWrite, 
		     [actedIn_imdb,bornOn,deceasedOn,
		      directorOf,firstName,gender,
		      hasHeight,hasLabel_imdb,lastName,
		      producerOf,actedIn_yago,diedOnDate,directed,hasFamilyName,
		      hasGender,hasGivenName,hasLabel_yago,produced,
		      wasBornOnDate] as Set,
		     evidenceTrainingPartition);

  def trainingInference;
  trainingInference = new LazyMPEInference(m, inferenceTrainingDB, cb);

  FullInferenceResult result1 = trainingInference.mpeInference();

  endTime = System.nanoTime();
  trainingInference.close();
  println "done!";
  println "  Elapsed time: " + 
    TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";

  imdbFactor = (imdbCnt[0] * imdbCnt[0]);
  yagoFactor = (yagoCnt[0] * yagoCnt[0]);

  eval.evaluateModel(new UIFullInferenceResult(inferenceTrainingDB, result1),
		     [samePerson, sameMovie],
		     targetTrainingPartition, evalTrainingWrite,
		     [imdbFactor, yagoFactor]);
}

//
// testing partition
//
print "\nStarting inference on the testing fold ... ";
startTime = System.nanoTime();

Database inferenceTestingDB = 
  data.getDatabase(inferenceTestingWrite, 
		   [actedIn_imdb,bornOn,deceasedOn,
		    directorOf,firstName,gender,
		    hasHeight,hasLabel_imdb,lastName,
		    producerOf,actedIn_yago,diedOnDate,directed,hasFamilyName,
		    hasGender,hasGivenName,hasLabel_yago,produced,
		    wasBornOnDate] as Set,
		   evidenceTestingPartition);
def testingInference;
testingInference = new LazyMPEInference(m, inferenceTestingDB, cb);

FullInferenceResult result2 = testingInference.mpeInference();

endTime = System.nanoTime();
testingInference.close();
println "done!";
println "  Elapsed time: " + 
    TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";

imdbFactor = (imdbCnt[1] * imdbCnt[1]);
yagoFactor = (yagoCnt[1] * yagoCnt[1]);

eval.evaluateModel(new UIFullInferenceResult(inferenceTestingDB, result2),
		   [samePerson, sameMovie],
		   targetTestingPartition, evalTestingWrite,
		   [imdbFactor, yagoFactor]);
