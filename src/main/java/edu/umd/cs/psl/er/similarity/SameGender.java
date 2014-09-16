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

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.function.ExternalFunction;

/**
 * Returns 1 if the genders are the same (ignoring case and order), 
 * and 0 otherwise.
 */
class SameGender implements ExternalFunction
{
  @Override
  public int getArity() 
  {
    return 2;
  }

  @Override
  public ArgumentType[] getArgumentTypes() 
  {
    return new ArgumentType[] { ArgumentType.String, ArgumentType.String };
  }

  @Override
  public double getValue(ReadOnlyDatabase db, GroundTerm... args) 
  {
    String gender1 = args[0].toString();
    String gender2 = args[1].toString();

    if (gender1.equals(gender2))
      return 1.0;

    return 0.0;
  }
}
