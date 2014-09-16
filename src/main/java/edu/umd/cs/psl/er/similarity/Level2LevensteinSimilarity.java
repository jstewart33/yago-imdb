/*
 * This file is part of the PSL software.
 * Copyright 2011 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.umd.cs.psl.er.similarity;

import com.wcohen.ss.BasicStringWrapper;
import com.wcohen.ss.Level2Levenstein;

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.function.ExternalFunction;
// import edu.umd.cs.psl.model.function.AttributeSimilarityFunction;

/**
 * This external function computes the similarity between two
 * paper titles using the dice similarity.
 * If the similarity is below 0.5, it returns 0; otherwise,
 * it returns the dice similarity value.
 */
class Level2LevensteinSimilarity implements ExternalFunction {
	// similarity threshold (default=0.5)
	private double simThresh;

	// constructors
	public Level2LevensteinSimilarity() {
		this.simThresh = 0.5;
	}
	public Level2LevensteinSimilarity(double simThresh) {
		this.simThresh = simThresh;
	}
	
	@Override
	public int getArity() {
		return 2;
	}

	@Override
	public ArgumentType[] getArgumentTypes() {
		return new ArgumentType[] { ArgumentType.String, ArgumentType.String };
	}

	// similarity function
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		double sim = 0.0;
		BasicStringWrapper aWrapped = new BasicStringWrapper(args[0].toString());
		BasicStringWrapper bWrapped = new BasicStringWrapper(args[1].toString());
		
		Level2Levenstein leven = new Level2Levenstein();
		sim = leven.score(aWrapped, bWrapped);
		
		if (sim < simThresh) 
			return 0.0;
		else 
			return sim;
    }
}
