package edu.umd.cs.psl.er

import java.io.FileReader
import java.util.concurrent.TimeUnit

import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.application.inference.LazyMPEInference;
import edu.umd.cs.psl.evaluation.resultui.UIFullInferenceResult;
import edu.umd.cs.psl.evaluation.result.FullInferenceResult;
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.rdbms.driver.DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.er.evaluation.ModelEvaluation
import edu.umd.cs.psl.er.similarity.DiceSimilarity
import edu.umd.cs.psl.er.similarity.JaroWinklerSimilarity
import edu.umd.cs.psl.er.similarity.JaccardSimilarity
import edu.umd.cs.psl.er.similarity.JaroSimilarity
import edu.umd.cs.psl.er.similarity.Level2JaroWinklerSimilarity
import edu.umd.cs.psl.er.similarity.Level2LevensteinSimilarity
import edu.umd.cs.psl.er.similarity.Level2MongeElkanSimilarity
import edu.umd.cs.psl.er.similarity.SameInitials
import edu.umd.cs.psl.er.similarity.SameNumTokens
import edu.umd.cs.psl.groovy.*
import edu.umd.cs.psl.groovy.experiments.ontology.*
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.predicate.Predicate
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.er.evaluation.FileAtomPrintStream;
import edu.umd.cs.psl.ui.loading.InserterUtils;

/*
 * Start and end times for timing information.
 */
def startTime;
def endTime;

/*
 * First, we'll parse the command line arguments.
 */
if (args.size() < 2) {
  println "\nUsage: AuthorPaperER <data_dir> [ -l ] -m <model>\n";
  return 1;
}

def datadir = args[0];
if (!datadir[datadir.size()-1].equals("/"))
  datadir += "/";

boolean learnWeights = false;
if (args.size() >= 2) 
{
  for (int i = 1; i < args.length; i++)
  {
    if (args[i].equals("-l"))
      learnWeights = true;
    else if (args[i].equals("-m"))
    {
      i++;
      int modelIndex = Integer.parseInt(args[i]);
      switch (modelIndex)
      {
	case 1:
	  model = "attribute"; break;
	case 2:
	  model = "relational"; break;
	case 3:
	  model = "relational_coauthor"; break;
	default:
	  println "\nUsage: AuthorPaperER <data_dir> [ -l ] -m <model>\n";
	  return 1;
      }
    }
    else
    {
      println "\nUsage: AuthorPaperER <data_dir> [ -l ] -m <model>\n";
      return 1;
    }
  }
}
println "\n*** PSL ER EXAMPLE ***\n"
println "Data directory  : " + datadir;
println "Weight learning : " + (learnWeights ? "ON" : "OFF");


/*
 * We'll use the ConfigManager to access configurable properties.
 * This file is usually in <project>/src/main/resources/psl.properties
 */
ConfigManager cm = ConfigManager.getManager();
ConfigBundle cb = cm.getBundle("er-example");


/*** MODEL DEFINITION ***/
print "Creating the ER model ... ";

/*
 * We'll create a new relational DB.
 */
def defaultPath = System.getProperty("java.io.tmpdir");
String dbpath = cb.getString("dbpath", 
			     defaultPath + File.separator + "er-notrain");
H2DatabaseDriver h2 =
  new H2DatabaseDriver(Type.Disk, dbpath, true);
RDBMSDataStore data = new RDBMSDataStore(h2, cb);

PSLModel m = new PSLModel(this, data);

/*
 * These are the predicates for our model.
 * Predicates are precomputed.
 * Functions are computed online.
 * "Open" predicates are ones that must be inferred.
 */
m.add predicate: "authorName",
  types: [ArgumentType.UniqueID, ArgumentType.String];
m.add predicate: "paperTitle" , 
  types: [ArgumentType.UniqueID, ArgumentType.String];
m.add predicate: "authorOf", 
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID];

/* 
 * Here we define the first similarity function. We declare the 
 * implementation as an external class, which takes in its
 * constructor a threshold value. All similarity scores below the 
 * threshold value are clipped to have similarity 0.0, which reduces
 * the number of active variables during inference. Setting this
 * threshold too low can activate too many variables, but setting it
 * too high can clip too many possibly relevant pairs. 
 */
m.add function:  "simName", implementation: new LevenshteinSimilarity(0.5);
//m.add function:  "simName", implementation: new JaroSimilarity(0.5);
//m.add function:  "simName", implementation: new Level2MongeElkanSimilarity(0.5);
m.add function:  "simTitle", implementation: new DiceSimilarity(0.5);
m.add function:  "sameInitials", implementation: new SameInitials();
m.add function:  "sameNumTokens", implementation: new SameNumTokens();
m.add predicate: "sameAuthor",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID];
m.add predicate: "samePaper",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID];
m.add predicate: "coauthor",
  types: [ArgumentType.UniqueID, ArgumentType.UniqueID];

/*
 * Set comparison functions operate on sets and return a scalar.
 */
m.add setcomparison: "sameAuthorSet",
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


/* We now iterate through all models. To run all three models, uncomment 
 * the all models line for now, we only run the attribute and basic 
 * relational model adding the coauthor logic creates many more variables 
 * in the optimization so can be considerably more expensive.
*/
//for (String model : ["attribute", "relational", "relational_coauthor"]) {
// for (String model : ["attribute", "relational"]) 
// {
  if (model.equals("relational") || model.equals("relational_coauthor")) 
  {
    /*
     * Here are some relational rules.
     * To see the benefit of the relational rules, comment this section 
     * out and re-run the script.
     */
    //
    // If two references share a common publication, and have the same initials, 
    // then => same author
    //
    m.add rule : (authorOf(A1,P1)   & authorOf(A2,P2)   & samePaper(P1,P2) &
		  authorName(A1,N1) & authorName(A2,N2) & sameInitials(N1,N2)) >> sameAuthor(A1,A2), weight : 1.0;

    //
    // If two papers have a common set of authors, and the same number of 
    // tokens in the title, then => same paper.
    //
    // Note the usage of set operations for the sameAuthorSet set similarity.
    // The inv operator represents the set of instances related via the
    // authorOf relation. 
    //
    m.add rule : (sameAuthorSet({P1.authorOf(inv)},{P2.authorOf(inv)}) & paperTitle(P1,T1) & paperTitle(P2,T2) & 
		  sameNumTokens(T1,T2)) >> samePaper(P1,P2),  weight : 1.0;
  }

  if (model.equals("relational_coauthor")) 
  {
    m.add rule: ~coauthor(A,B), weight: 1E-6;

    m.add rule : (authorOf(A1,P) & authorOf(A2,P)) >> 
                  coauthor(A1,A2), weight : 1.0;
    m.add rule : (coauthor(A1,A2) & sameAuthor(A2,A3) & 
		  authorName(A2,N2) & authorName(A3,N3) & 
		  sameInitials(N2,N3)) >> coauthor(A1,A3), weight: 1.0;
    m.add rule : (coauthor(A1,A2) & coauthor(A2,A3) & 
		  authorName(A1,N1) & authorName(A3,N3) & 
		  sameInitials(N1,N3)) >> sameAuthor(A1,A3), weight: 1.0;
  }

  /* 
   * Now we'll add a prior to the open predicates.
   */
  m.add rule: ~sameAuthor(A,B), weight: 1E-6;
  m.add rule: ~sameAuthor(A,B), weight: 1E-6;

  println "done!";

  /*** LOAD DATA ***/
  println "Creating a new DB and loading data:";

  /*
   * The setup command instructs the DB to use the H2 driver.
   * It can also tell it to use memory as its backing store, or alternately a
   * specific directory in the file system. If neither is specified, the default
   * location is a file in the project root directory.
   * NOTE: In our experiments, we have found that using the hard drive performed
   * better than using main memory, though this may vary from system to system.
   */
  //data.setup db: DatabaseDriver.H2;
  //data.setup db: DatabaseDriver.H2, type: "memory";
  //data.setup db: DatabaseDriver.H2, folder: "/tmp/";

  /*
   * These are just some constants that we'll use to reference data 
   * files and DB partitions.
   * To change the dataset (e.g. big, medium, small, tiny), change the
   * dir variable.
   */
  int testingFold = 0;
  Partition evidenceTestingPartition = new Partition(1);
  Partition targetTestingPartition = new Partition(2);

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
  for (Predicate p1 : [authorName,paperTitle,authorOf])
  {
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
    //testing data
    String testFile = datadir + p3.getName() + "." + testingFold + ".txt";
    print "  Reading " + testFile + " ... ";
    insert = data.getInserter(p3,targetTestingPartition);
    InserterUtils.loadDelimitedDataTruth(insert, testFile);
    println "done!";
  }


  /*** INFERENCE ***/

  /*
   * Note: to run evaluation of ER inference, we need to specify the total 
   * number of pairwise combinations of authors and papers, which we pass 
   * to evaluateModel() in an array.
   * This is for memory efficiency, since we don't want to actually load 
   * truth data for all possible pairs (though one could).
   *
   * To get the total number of possible combinations, we'll scan the 
   * author/paper reference files, counting the number of lines.
   */
  int[] authorCnt = new int[1];
  int[] paperCnt = new int[1];
  FileReader rdr = null;
  for (int i = 0; i < 1; i++) 
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

  /*
   * Now evaluate inference on the testing set (to check model generalization).
   */
  print "\nStarting inference on the testing fold ... ";
  startTime = System.nanoTime();

  Database db1 = data.getDatabase(evidenceTestingPartition, 
				  [authorName, paperTitle, authorOf] as Set);
  LazyMPEInference trainingInference = new LazyMPEInference(m, db1, cb);
  FullInferenceResult result1 = trainingInference.mpeInference();

  endTime = System.nanoTime();
  println "done!";
  println "  Elapsed time: " + TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) + " secs";
  //eval.evaluateModel(testingInference, [sameAuthor, samePaper], targetTestingPartition, [authorCnt[0]*(authorCnt[0]-1), paperCnt[1]*(paperCnt[1]-1)]);

  /*****
	Output predictions to file
  ******/
  def writer = new FileAtomPrintStream(model+".txt", " ");
  UIFullInferenceResult uiresult = new UIFullInferenceResult(db1, result1);
  uiresult.printAtoms(sameAuthor, writer);
// }
