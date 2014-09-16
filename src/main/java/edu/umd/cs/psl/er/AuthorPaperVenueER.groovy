package edu.umd.cs.psl.er

import java.io.FileReader;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.application.inference.LazyMPEInference;
import edu.umd.cs.psl.database.rdbms.driver.DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.groovy.*
import edu.umd.cs.psl.groovy.experiments.ontology.*
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.evaluation.resultui.UIFullInferenceResult
import edu.umd.cs.psl.evaluation.result.FullInferenceResult;

import edu.umd.cs.psl.er.evaluation.ModelEvaluation
import edu.umd.cs.psl.er.similarity.DiceSimilarity;
import edu.umd.cs.psl.er.similarity.JaroWinklerSimilarity;
import edu.umd.cs.psl.er.similarity.SameInitials
import edu.umd.cs.psl.er.similarity.SameNumTokens
import edu.umd.cs.psl.er.evaluation.FileAtomPrintStream;
import edu.umd.cs.psl.model.argument.ArgumentType;

/*
 * Start and end times for timing information.
 */
def startTime;
def endTime;

/*
 * First, we'll parse the command line arguments.
 */
if (args.size() < 1) {
	println "\nUsage: AuthorPaperER <data_dir> [ -l ]\n";
	return 1;
}
def datadir = args[0];
if (!datadir[datadir.size()-1].equals("/"))
	datadir += "/";
boolean learnWeights = false;
if (args.size() >= 2) {
	learnWeights = args[1..(args.size()-1)].contains("-l");
}
println "\n*** PSL ER EXAMPLE ***\n"
println "Data directory  : " + datadir;
println "Weight learning : " + (learnWeights ? "ON" : "OFF");


/*
 * We'll use the ConfigManager to access configurable properties.
 * This file is usually in <project>/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager();
ConfigBundle cb = cm.getBundle("er-venue");

/*** LOAD DATA ***/
println "Creating a new DB and loading data:"

/*
 * We'll create a new relational DB.
 */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = cb.getString("dbpath", 
			     defaultPath + File.separator + "er-venue")

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

m.add predicate: "paperVenue" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "venueName" , 
  types: [ArgumentType.UniqueID, ArgumentType.String]

m.add predicate: "authorOf" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add function:  "simName"    ,   implementation: new LevenshteinSimilarity(0.2);
m.add function:  "simTitle"   ,   implementation: new LevenshteinSimilarity(0.2);
m.add function:  "sameInitials",  implementation: new SameInitials();

m.add predicate: "sameAuthor" , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "samePaper"  , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

m.add predicate: "sameVenue"  , 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]

/*
 * Now we can put everything together by defining some rules for our model.
 */



/*
 * Here are some basic rules.
 */
// similar names => same author
m.add rule : (authorName(A1,N1) & authorName(A2,N2) & simName(N1,N2)) >> sameAuthor(A1,A2), weight : 1.0;
// similar titles => same paper
m.add rule : (paperTitle(P1,T1) & paperTitle(P2,T2) & simTitle(T1,T2)) >> samePaper(P1,P2),  weight : 1.0;
// similar venues => same venue
m.add rule : (venueName(V1,T1) & venueName(V2,T2) & simTitle(T1,T2)) >> sameVenue(V1,V2), weight : 1.0;

/*
 * Here are some relational rules.
 * To see the benefit of the relational rules, comment this section 
 * out and re-run the script.
 */

// If two papers are the same, their authors are the same
m.add rule : (authorOf(A1,P1)   & authorOf(A2,P2)   & samePaper(P1,P2)) >> sameAuthor(A1,A2), weight : 1.0;
// If two papers are the same, their venues are the same
m.add rule : (paperVenue(P1,V1) & paperVenue(P2,V2) & samePaper(P1,P2)) >> sameVenue(V1,V2), weight : 1.0;


/* 
 * Now we'll add a prior to the open predicates.
 */
m.add rule: ~sameAuthor(A,B), weight: 1E-6;
m.add rule: ~samePaper(A,B), weight: 1E-6;
m.add rule: ~sameVenue(A,B), weight: 1E-6;

println "done!"

/*** LOAD DATA ***/
println "Creating a new DB and loading data:"
for (testingFold = 0 ; testingFold < 4; testingFold++) {

/*
 * These are just some constants that we'll use to reference data files and DB partitions.
 * To change the dataset (e.g. big, medium, small, tiny), change the dir variable.
 */

  Partition evidenceTestingPartition = new Partition(1);
  Partition targetTestingPartition = new Partition(2);

/* 
 * Now we'll load some data from tab-delimited files into the DB.
 * Note that the second parameter to each call to loadFromFile() determines the DB partition.
 */
def sep = java.io.File.separator;
def insert;

/* 
 * We start by reading in the non-target (i.e. evidence) predicate data.
 */
for (Predicate p1 : [authorName,paperTitle,authorOf,paperVenue,venueName])
{
  String testFile = datadir + p1.getName() + "." + testingFold + ".txt";
  print "  Reading " + testFile + " ... ";
  insert = data.getInserter(p1,evidenceTestingPartition);
  InserterUtils.loadDelimitedData(insert, testFile);
  println "done!"
}
/* 
 * Now we read the target predicate data.
 */
for (Predicate p3 : [sameAuthor,samePaper,sameVenue])
{
  //testing data
  String testFile = datadir + p3.getName() + "." + testingFold + ".txt";
  print "  Reading " + testFile + " ... ";
  insert = data.getInserter(p3,targetTestingPartition);
  InserterUtils.loadDelimitedData(insert, testFile);
  println "done!"
}

/*** INFERENCE ***/

/*
 * Evalute inference on the testing set.
 */
print "\nStarting inference on the testing fold ... ";
startTime = System.nanoTime();
Database db1 = data.getDatabase(evidenceTrainingPartition, 
				[authorName,paperTitle,authorOf,paperVenue,venueName] as Set);
LazyMPEInference trainingInference = new LazyMPEInference(m, db1, cb);
FullInferenceResult result1 = trainingInference.mpeInference();

endTime = System.nanoTime();
println "done!";
println "  Elapsed time: " + TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";

def writer = new FileAtomPrintStream("coraAuthor"+testingFold+".txt", " ")
  UIFullInferenceResult uiresult1 = new UIFullInferenceResult(db1, result1);
  uiresult1.printAtoms(sameAuthor, writer);

writer = new FileAtomPrintStream("coraPaper"+testingFold+".txt", " ")
  UIFullInferenceResult uiresult2 = new UIFullInferenceResult(db1, result1);
  uiresult2.printAtoms(samePaper, writer);

writer = new FileAtomPrintStream("coraVenue"+testingFold+".txt", " ")
  UIFullInferenceResult uiresult3 = new UIFullInferenceResult(db1, result1);
  uiresult3.printAtoms(sameVenue, writer);
}
