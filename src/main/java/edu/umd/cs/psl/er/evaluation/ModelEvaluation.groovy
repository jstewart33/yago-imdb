package edu.umd.cs.psl.er.evaluation

import java.util.List;

import edu.umd.cs.psl.evaluation.resultui.UIFullInferenceResult;
import edu.umd.cs.psl.evaluation.statistics.ResultComparator;
// import edu.umd.cs.psl.evaluation.statistics.ResultComparison.BinaryClass;
import edu.umd.cs.psl.evaluation.statistics.PredictionComparator;
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionComparator;
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionStatistics;
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionStatistics.BinaryClass;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DataStore;
import edu.umd.cs.psl.model.predicate.Predicate;

/*
 * This class contains code to perform evaluation, specific to our ER example project.
 */
class ModelEvaluation
{
  private DataStore dataStore;
	
  //constructor
  public ModelEvaluation(DataStore newDataStore){
    this.dataStore = newDataStore;
  }

  // data store
  public DataStore getDataStore() {
    return dataStore;
  }

  public void setDataStore(DataStore newDataStore) {
    this.dataStore = newDataStore;
  }
	
  /*
   * This overload requires the *full* ground truth 
   * (both positives and negatives) to exist in the DB. 
   */
  public void evaluateModel(UIFullInferenceResult result, 
			    List<Predicate> openPredicates, 
			    Partition baselinePartition) 
  {
    PredictionComparator comparator = result.compareResults();
    comparator.setBaseline(dataStore.getDatabase(null, baselinePartition));

    println("Model Evaluation: ");

    for (predicate in openPredicates) 
    {
      println("  Predicate: " + predicate.getName());
      for (double tol = 0.1; tol <= 1.0; tol += 0.1) 
      {
	comparator.setThreshold(tol);
	def comparison = comparator.compare(predicate);
	println("    Threshold = " + tol);
	println("      Pos: Prec = " + 
		comparison.getPrecision(BinaryClass.POSITIVE) +
		", Rec = " + comparison.getRecall(BinaryClass.POSITIVE) +
		", F1 = " + comparison.getF1(BinaryClass.POSITIVE));
					
	// println("      Neg: Prec = " + 
	// 	comparison.getPrecision(BinaryClass.NEGATIVE) +
	// 	", Rec = " + comparison.getRecall(BinaryClass.NEGATIVE) +
	// 	", F1 = " + comparison.getF1(BinaryClass.NEGATIVE));
      }
    }	
  }
	
  /*
   * This is the *efficient* evaluation method, in which the number 
   * of pairwise combinations are specified.
   */
  public void evaluateModel(UIFullInferenceResult result, 
  			    List<Predicate> openPredicates, 
   			    Partition baselineReadPartition, 
   			    Partition baselineWritePartition, 
  			    List<Integer> maxBaseAtoms) 
  {
    ResultComparator comparator = result.compareResults();
    comparator.setBaseline(dataStore.getDatabase(baselineWritePartition, 
    						 baselineReadPartition));

    println("Model Evaluation: ");

    for (int i = 0; i < openPredicates.size; i++) 
    {
      def predicate = openPredicates[i];
      println("  Predicate: " + predicate.getName());

      for (double tol = 0.1; tol <= 1.0; tol += 0.1) 
      {
	comparator.setThreshold(tol);
	DiscretePredictionStatistics comparison = 
	  comparator.compare(predicate, maxBaseAtoms[i]);

	println("  Threshold = " + tol);
	println("      Pos: Prec = " + 
		comparison.getPrecision(BinaryClass.POSITIVE) +
		", Rec = " + comparison.getRecall(BinaryClass.POSITIVE) +
		", F1 = " + comparison.getF1(BinaryClass.POSITIVE));

	// println("      Neg: Prec = " + 
	// 	comparison.getPrecision(BinaryClass.NEGATIVE) +
	// 	", Rec = " + comparison.getRecall(BinaryClass.NEGATIVE) +
	// 	", F1 = " + comparison.getF1(BinaryClass.NEGATIVE));
      }
    }
  }

}
