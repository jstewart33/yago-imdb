/*
 * This file is part of the PSL software.
 * Copyright 2014 University of California, Santa Cruz
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

import java.util.regex.*

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.function.ExternalFunction;

/**
 * Returns 1 if the input dates are the same.
 * Same year => 0.6
 * Same year, day => 0.8
 * Same year, month => 0.9
 * 0 otherwise.
 */
class SameDate implements ExternalFunction
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

  /*
   * Parse the possible date formats:
   *
   * eg. 27 November 1992 or 1983 (IMDB)
   *     eg. 1942-10-11   or 1978-##-##   or -890-##-## (YAGO)
   */
  @Override
  public double getValue(ReadOnlyDatabase db, GroundTerm... args) 
  {
    int[] date1 = extractDate(args[0].toString());
    int[] date2 = extractDate(args[1].toString());

    if (date1[0] == date2[0])
    {
      if (date1[1] == date2[1])
      {
	if (date1[2] == date2[2])
	{
	  return 1.0;
	}
	return 0.9;
      }
      return 0.6;
    }

    return 0.0;
  }

  private int[] extractDate(String date)
  {
    /* parse the date based on format */
    Pattern imdb_pattern1 = Pattern.compile("(\\d{1,2})\\s(\\w+)\\s(\\d{4})");
    Pattern imdb_pattern2 = Pattern.compile("(\\d{4})");

    Pattern yago_pattern1 = Pattern.compile("(\\d{4})-(\\d{2}|##)-(\\d{2}|##)");
    Pattern yago_pattern2 = 
      Pattern.compile("\\-(\\d{3})-(\\d{2}|##)-(\\d{2}|##)");

    /* date array [year,month,day] */
    int[] dateVal = new int[3];

    Matcher m1 = imdb_pattern1.matcher(date);
    if (m1.find())
    {
      dateVal[0] = Integer.parseInt(m1.group(2));
      dateVal[1] = convertMonth(m1.group(1));
      dateVal[2] = Integer.parseInt(m1.group(0));
      return dateVal;
    }

    Matcher m2 = imdb_pattern2.matcher(date);
    if (m2.find())
    {
      dateVal[0] = Integer.parseInt(m1.group(0));
      dateVal[1] = 0;
      dateVal[2] = 0;
      return dateVal;
    }

    Matcher m3 = yago_pattern1.matcher(date);
    if (m3.find())
    {
      dateVal[0] = Integer.parseInt(m1.group(0));

      String monthVal = m1.group(1);
      if (monthVal.matches("##"))
	dateVal[1] = 0;
      else
	dateVal[1] = Integer.parseInt(m1.group(1));

      String dayVal = m2.group(1);
      if (dayVal.matches("##"))
	dateVal[2] = 0;
      else
	dateVal[2] = Integer.parseInt(m1.group(2));
      return dateVal;
    }

    Matcher m4 = yago_pattern2.matcher(date);
    if (m4.find())
    {
      dateVal[0] = Integer.parseInt(m1.group(0));

      String monthVal = m1.group(1);
      if (monthVal.matches("##"))
	dateVal[1] = 0;
      else
	dateVal[1] = Integer.parseInt(m1.group(1));

      String dayVal = m2.group(1);
      if (dayVal.matches("##"))
	dateVal[2] = 0;
      else
	dateVal[2] = Integer.parseInt(m1.group(2));
      return dateVal;
    }
  }

  /*
   * Convert the month string to an integer.
   */
  private int convertMonth(String month)
  {
    if (month.matches("^[Jj]an"))
      return 1;
    if (month.matches("^[Ff]eb"))
      return 2;
    if (month.matches("^[Mm]ar"))
      return 3;
    if (month.matches("^[Aa]pr"))
      return 4;
    if (month.matches("^[M]ay"))
      return 5;
    if (month.matches("^[Jj]un"))
      return 6;
    if (month.matches("^[Jj]ul"))
      return 7;
    if (month.matches("^[Aa]ug"))
      return 8;
    if (month.matches("^[Ss]ep"))
      return 9;
    if (month.matches("^[Oo]ct"))
      return 10;
    if (month.matches("^[Nn]ov"))
      return 11;
    if (month.matches("^[Dd]ec"))
      return 12;

    /* unknown month */
    return 0;
  }
}
