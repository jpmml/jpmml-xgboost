/*
 * Copyright (c) 2016 Villu Ruusmann
 *
 * This file is part of JPMML-XGBoost
 *
 * JPMML-XGBoost is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-XGBoost is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-XGBoost.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.xgboost.testing;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.converter.testing.Datasets;
import org.jpmml.converter.testing.Fields;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.ArchiveBatch;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.Test;

public class ClassificationTest extends XGBoostTest implements Algorithms, Datasets, Fields {

	public ClassificationTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		ArchiveBatch result = new XGBoostTestBatch(name, dataset, predicate, equivalence){

			@Override
			public ClassificationTest getIntegrationTest(){
				return ClassificationTest.this;
			}

			@Override
			public List<Map<String, String>> getInput() throws IOException {
				String[] dataset = parseDataset();

				List<Map<String, String>> table = super.getInput();

				// XXX
				if((AUDIT_NA).equals(dataset[0])){
					String income = "Income";

 					for(Map<String, String> row : table){
 						String value = row.get(income);

 						if(value == null){
 							row.put(income, "NaN");
 						}
 					}
				}

				return table;
			}
		};

		return result;
	}

	@Test
	public void evaluateBinomialAudit() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT, excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(8));
	}

	@Test
	public void evaluateBinomialAuditLimit() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT + "@31", excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(8));
	}

	@Test
	public void evaluateBinomialAuditNA() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT_NA, excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(12));
	}

	@Test
	public void evaluateBinomialAuditNALimit() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT_NA + "@31", excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(8));
	}

	@Test
	public void evaluateHingeAudit() throws Exception {
		evaluate(HINGE_CLASSIFICATION, AUDIT);
	}

	@Test
	public void evaluateHingeAuditNA() throws Exception {
		evaluate(HINGE_CLASSIFICATION, AUDIT_NA);
	}

	@Test
	public void evaluateMultinomialAudit() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, AUDIT, new FloatEquivalence(20));
	}

	@Test
	public void evaluateMultinomialAuditNA() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, AUDIT_NA, new FloatEquivalence(24));
	}

	@Test
	public void evaluateMultinomialIris() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS, new FloatEquivalence(12));
	}

	@Test
	public void evaluateMultinomialIrisLimit() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS + "@11", new FloatEquivalence(8));
	}

	@Test
	public void evaluateMultinomialIrisNA() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS_NA, new FloatEquivalence(20));
	}

	@Test
	public void evaluateMultinomialIrisNALimit() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS_NA + "@11", new FloatEquivalence(12));
	}
}