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
import org.jpmml.converter.testing.Fields;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.Test;

public class ClassificationTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, Fields {

	public ClassificationTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public ClassificationTest getArchiveBatchTest(){
				return ClassificationTest.this;
			}

			@Override
			public List<? extends Map<String, ?>> getInput() throws IOException {
				List<? extends Map<String, ?>> table = super.getInput();

				String dataset = truncate(getDataset());

				// XXX
				if((AUDIT_NA).equals(dataset)){
					String income = "Income";

 					for(Map<String, ?> row : table){
 						Object value = row.get(income);

 						if(value == null){
 							((Map)row).put(income, "NaN");
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
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT_LIMIT, excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(8));
	}

	@Test
	public void evaluateBinomialAuditNA() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT_NA, excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(12));
	}

	@Test
	public void evaluateBinomialAuditNALimit() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT_NA_LIMIT, excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(8));
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
		evaluate(MULTINOMIAL_CLASSIFICATION, AUDIT_NA, new FloatEquivalence(28));
	}

	@Test
	public void evaluateMultinomialIris() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS, new FloatEquivalence(16));
	}

	@Test
	public void evaluateMultinomialIrisLimit() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS_LIMIT, new FloatEquivalence(12));
	}

	@Test
	public void evaluateMultinomialIrisNA() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS_NA, new FloatEquivalence(20));
	}

	@Test
	public void evaluateMultinomialIrisNALimit() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS_NA_LIMIT, new FloatEquivalence(12));
	}
}