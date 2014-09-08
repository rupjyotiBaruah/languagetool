/* LanguageTool, a natural language style checker
 * Copyright (C) 2014 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.rules.patterns;

import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.rules.RuleMatch;

import java.util.*;

/**
 * Evaluates a {@link RuleFilter}.
 * @since 2.7
 */
class RuleFilterEvaluator {

  private final RuleFilter filter;

  RuleFilterEvaluator(RuleFilter filter) {
    this.filter = filter;
  }

  RuleMatch runFilter(String filterArgs, RuleMatch ruleMatch, List<Integer> tokenPositions, AnalyzedTokenReadings[] tokenReadings) {
    Map<String,String> args = getResolvedArguments(filterArgs, tokenReadings, tokenPositions);
    return filter.acceptRuleMatch(ruleMatch, args);
  }

  /**
   * Resolves the backref arguments, e.g. replaces {@code \1} by the value of the first token in the pattern.
   */
  Map<String,String> getResolvedArguments(String filterArgs, AnalyzedTokenReadings[] tokenReadings, List<Integer> tokenPositions) {
    Map<String,String> result = new HashMap<>();
    String[] arguments = filterArgs.split("\\s+");
    for (String arg : arguments) {
      String[] keyVal = arg.split(":");
      if (keyVal.length != 2) {
        throw new RuntimeException("Invalid syntax for key/value, expected 'key:value', got: '" + arg + "'");
      }
      String key = keyVal[0];
      String val = keyVal[1];
      if (val.startsWith("\\")) {
        int refNumber = Integer.parseInt(val.replace("\\", ""));
        if (refNumber > tokenPositions.size()) {
          throw new RuntimeException("Your reference number " + refNumber + " is bigger than the number of tokens: " + tokenPositions.size());
        }
        int correctedRef = getSkipCorrectedReference(tokenPositions, refNumber);
        if (correctedRef >= tokenReadings.length) {
          throw new RuntimeException("Your reference number " + refNumber +
                  " is bigger than number of matching tokens: " + tokenReadings.length);
        }
        if (result.containsKey(key)) {
          throw new RuntimeException("Duplicate key '" + key + "'");
        }
        result.put(key, tokenReadings[correctedRef].getToken());
      } else {
        result.put(key, val);
      }
    }
    return result;
  }

  // when there's a 'skip', we need to adapt the reference number
  private int getSkipCorrectedReference(List<Integer> tokenPositions, int refNumber) {
    int correctedRef = 0;
    int i = 0;
    for (int tokenPosition : tokenPositions) {
      if (i++ >= refNumber) {
        break;
      }
      correctedRef += tokenPosition;
    }
    return correctedRef - 1;
  }

}
