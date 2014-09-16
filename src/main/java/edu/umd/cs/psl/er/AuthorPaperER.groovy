package edu.umd.cs.psl.er

import java.io.FileReader
import java.util.concurrent.TimeUnit

import edu.umd.cs.psl.application.inference.LazyMPEInference;
import edu.umd.cs.psl.application.inference.MPEInference;
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
import edu.umd.cs.psl.er.similarity.SameInitials
import edu.umd.cs.psl.er.similarity.SameNumTokens
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
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.VotedPerceptron;
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;

/*
 * Start and end times for timing information.
 */
def startTime;
def endTime;

/*
 * First, we'll parse the command line arguments.
 */
if (args.size() < 1) 
{
  println "\nUsage: AuthorPaperER <data_dir> [ -learn | -mlmpe | -mpeinf ]\n";
  return 1;
}

def datadir = args[0];
if (!datadir[datadir.size()-1].equals("/"))
  datadir += "/";

boolean learnWeights = false;
boolean lazyinf = true;
boolean lazympe = true;

if (args.size() >= 2) 
{
  for (String arg : args)
  {
    if (arg.equals("-learn"))
      learnWeights = true;
    else if (arg.equals("-mlmpe"))
      lazympe = false;
    else if (arg.equals("-mpeinf"))
      lazyinf = false;
  }
}

println "\n*** PSL ER EXAMPLE ***\n"
println "Data directory          : " + datadir;
println "Weight learning         : " + (learnWeights ? "ON" : "OFF");
println "Lazy MPE Inference      : " + (lazyinf ? "ON" : "OFF");
println "Lazy Max Likelihood MPE : " + (lazympe ? "ON" : "OFF");

/*
 * We'll use the ConfigManager to access configurable properties.
 * This file is usually in <project>/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager();
ConfigBundle cb = cm.getBundle("er-example");

/*** LOAD DATA ***/
println "Creating a new DB and loading data:"

/*
 * We'll create a new relational DB.
 */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = cb.getString("dbpath", 
                 defaultPath + File.separator + "er-example")

DataStore data = new RDBMSDataStore(
  new H2DatabaseDriver(Type.Disk, dbpath, true), cb)

/*** MODEL DEFINITION ***/
print "Creating the ER model ... "
PSLModel m = new PSLModel(this, data);

/*
 * These are the predicates for our model.
 * Predicates are precomputed.
 * Functions are computed online.
 * "Open" predicates are ones that must be inferred.
 */
m.add predicate: "authorName" , 
  types: [ArgumentType.UniqueID, ArgumentType.String]

m.add predicate: "paperTitle" , 
  types: [ArgumentType.UniqueID, ArgumentType.String]

m.add predicate: "authorOf" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

// m.add predicate: "authorTfIdf" , 
//   types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

// m.add predicate: "titleTfIdf" , 
//   types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

//m.add function:  "simName"    ,   implementation: new LevenshteinSimilarity(0.5);
m.add function:  "simName"    ,   implementation: new JaroWinklerSimilarity(0.75);
m.add function:  "simTitle"   ,   implementation: new JaccardSimilarity(0.75);
m.add function:  "sameInitials",  implementation: new SameInitials();
m.add function:  "sameNumTokens", implementation: new SameNumTokens();

m.add predicate: "sameAuthor" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "samePaper"  , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

/*
 * Set comparison functions operate on sets and return a scalar.
 */
m.add setcomparison: "sameAuthorSet" , 
  using: SetComparison.CrossEquality, on : sameAuthor;

/*
 * Now we can put everything together by defining some rules for our model.
 */

/*
 * Here are some basic rules.
 */
// similar names => same author
m.add rule : (authorName(A1,N1) & authorName(A2,N2) & simName(N1,N2)) >> sameAuthor(A1,A2), weight : 1.0;

// similar titles => same paper
m.add rule : (paperTitle(P1,T1) & paperTitle(P2,T2) & simTitle(T1,T2) ) >> samePaper(P1,P2),  weight : 1.0;

// similar names => same author (tf-idf)
// m.add rule : (authorTfIdf(A1,A2) & authorName(A1,N1) & authorName(A2,N2) & simName(N1,N2)) >> sameAuthor(A1,A2), weight : 1.0;

// similar titles => same paper (tf-idf)
// m.add rule : (titleTfIdf(P1,P2) & paperTitle(P1,T1) & paperTitle(P2,T2) & simTitle(T1,T2) ) >> samePaper(P1,P2),  weight : 1.0;

/*
 * Here are some relational rules.
 * To see the benefit of the relational rules, comment this section out 
 * and re-run the script.
 */

//
// If two references share a common publication, and have the same initials,
// then => same author
//
// m.add rule : (authorOf(A1,P1)   & authorOf(A2,P2)   & samePaper(P1,P2) &
//               authorName(A1,N1) & authorName(A2,N2) & authorTfIdf(A1,A2) &
// 	      sameInitials(N1,N2)) >> sameAuthor(A1,A2), weight : 1.0;

m.add rule : (authorOf(A1,P1)   & authorOf(A2,P2)   & samePaper(P1,P2) &
              authorName(A1,N1) & authorName(A2,N2) & 
	      sameInitials(N1,N2)) >> sameAuthor(A1,A2), weight : 1.0;

//
// If two papers have a common set of authors, and the same number of tokens 
// in the title, then => same paper
//
// m.add rule : (sameAuthorSet({P1.authorOf(inv)},{P2.authorOf(inv)}) & 
// 	      paperTitle(P1,T1) & paperTitle(P2,T2) & titleTfIdf(P1,P2) & 
//              sameNumTokens(T1,T2)) >> samePaper(P1,P2),  weight : 1.0;

m.add rule : (sameAuthorSet({P1.authorOf(inv)},{P2.authorOf(inv)}) & 
	      paperTitle(P1,T1) & paperTitle(P2,T2) & 
             sameNumTokens(T1,T2)) >> samePaper(P1,P2),  weight : 1.0;

/* 
 * Now we'll add a prior to the open predicates.
 */
m.add rule: ~sameAuthor(A,B), weight: 1E-6;
m.add rule: ~samePaper(A,B), weight: 1E-6;

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
 * We start by reading in the non-target (i.e. evidence) predicate data.
 */
for (Predicate p1 : [authorName,paperTitle,authorOf,/*authorTfIdf,titleTfIdf*/])
{
  String trainFile = datadir + p1.getName() + "." + trainingFold + ".txt";
  print "  Reading " + trainFile + " ... ";

  if (learnWeights)
  {
    insert = data.getInserter(p1,evidenceTrainingPartition);
    InserterUtils.loadDelimitedData(insert, trainFile);
  }
  // insert = data.getInserter(p1,evidenceTestingPartition);
  // InserterUtils.loadDelimitedData(insert, trainFile);
  println "done!";

  String testFile = datadir + p1.getName() + "." + testingFold + ".txt";
  print "  Reading " + testFile + " ... ";
  insert = data.getInserter(p1,evidenceTestingPartition);
  InserterUtils.loadDelimitedData(insert, testFile);
  println "done!";
}

/* 
 * Now we read the target predicate data.
 */
for (Predicate p3 : [sameAuthor,samePaper])
{
  String trainFile = datadir + p3.getName() + "." + trainingFold + ".txt";
  print "  Reading " + trainFile + " ... ";

  if (learnWeights)
  {
    insert = data.getInserter(p3,targetTrainingPartition);
    InserterUtils.loadDelimitedDataTruth(insert, trainFile);
  }
  // insert = data.getInserter(p3,targetTestingPartition);
  // InserterUtils.loadDelimitedDataTruth(insert, trainFile);
  println "done!";
    
  //testing data
  String testFile = datadir + p3.getName() + "." + testingFold + ".txt";
  print "  Reading " + testFile + " ... ";
  insert = data.getInserter(p3,targetTestingPartition);
  InserterUtils.loadDelimitedDataTruth(insert, testFile);
  println "done!"
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
  // sameAuthor/samePaper into trainWrite
  Database trainDB =
    data.getDatabase(trainWrite,
		     [authorName, paperTitle, authorOf] as Set,
		     evidenceTrainingPartition);
//The truthDB is a bit different than I described. The problem with
//the commented code below is that it assumes a closed world - that
//every true sameAuthor and samePaper link in the real world are
//contained in targetTrainingPartition. I don't think that's true in
//your case (but you should check, in case the testing and training
//data are disjoint networks). So instead of closing the sameAuthor
//and samePaper predicates, we close no predicates. This will allow
//lazy weight learning to assign the unseen sameAuthor and samePaper
//variables to their MPE state (conditioned on the training data). 
/* CLOSED WORLD ASSUMPTION */
/*
 Database truthDB =
   data.getDatabase(truthWrite,
    [sameAuthor, samePaper] as Set, targetTrainingPartition,
evidenceTrainingPartition);
*/
  //OPEN WORLD ASSUMPTION
  Database truthDB = data.getDatabase(truthWrite,
				      [ ] as Set, targetTrainingPartition,
				      evidenceTrainingPartition);

  //LazyMaxLikelihoodMPE weightLearning =
  //  new LazyMaxLikelihoodMPE(m, trainDB, truthDB, cb);
  VotedPerceptron weightLearning;

  if (lazympe)
    weightLearning = new LazyMaxLikelihoodMPE(m, trainDB, truthDB, cb);
  else
    weightLearning = new MaxLikelihoodMPE(m, trainDB, truthDB, cb);

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
 * author/paper reference files, counting the number of lines.
 */
int[] authorCnt = new int[2];
int[] paperCnt = new int[2];
FileReader rdr = null;
for (int i = 0; i < 2; i++) 
{
  rdr = new FileReader(datadir + "AUTHORNAME." + i + ".txt");
  while (rdr.readLine() != null) authorCnt[i]++;
  println "Authors fold " + i + ": " + authorCnt[i];

  rdr = new FileReader(datadir + "PAPERTITLE." + i + ".txt");
  while (rdr.readLine() != null) paperCnt[i]++;
  println "Papers  fold " + i + ": " + paperCnt[i];
}

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

//
// Training partition
//
if (learnWeights)
{
  Database inferenceTrainingDB = 
    data.getDatabase(inferenceTrainingWrite, 
		     [authorName, paperTitle, authorOf] as Set,
		     evidenceTrainingPartition);

  def trainingInference;
  if (lazyinf)
  {
    trainingInference = 
      new LazyMPEInference(m, inferenceTrainingDB, cb);
  }
  else
  {
    trainingInference = 
      new MPEInference(m, inferenceTrainingDB, cb);
  }

  FullInferenceResult result1 = trainingInference.mpeInference();

  endTime = System.nanoTime();
  trainingInference.close();
  println "done!";
  println "  Elapsed time: " + 
    TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";

  eval.evaluateModel(new UIFullInferenceResult(inferenceTrainingDB, result1),
		     [sameAuthor, samePaper],
		     targetTrainingPartition, evalTrainingWrite,
		     [authorCnt[0]*(authorCnt[0]-1), paperCnt[0]*(paperCnt[0]-1)]);
}

//
// testing partition
//
print "\nStarting inference on the testing fold ... ";
startTime = System.nanoTime();

Database inferenceTestingDB = 
  data.getDatabase(inferenceTestingWrite, 
		   [authorName, paperTitle, authorOf] as Set,
		   evidenceTestingPartition);
def testingInference;
testingInference = new LazyMPEInference(m, inferenceTestingDB, cb);

FullInferenceResult result2 = testingInference.mpeInference();

endTime = System.nanoTime();
testingInference.close();
println "done!";
println "  Elapsed time: " + 
    TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";

eval.evaluateModel(new UIFullInferenceResult(inferenceTestingDB, result2),
		   [sameAuthor, samePaper],
		   targetTestingPartition, evalTestingWrite,
		   [authorCnt[1]*(authorCnt[1]-1), paperCnt[1]*(paperCnt[1]-1)]);
