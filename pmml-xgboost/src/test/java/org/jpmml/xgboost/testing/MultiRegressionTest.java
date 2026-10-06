/*
 * Copyright (c) 2024 Villu Ruusmann
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
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.jupiter.api.Test;

public class MultiRegressionTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, XGBoostFormats, XGBoostLearningRates {

	public MultiRegressionTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			{
				String algorithm = getAlgorithm();
				String dataset = getDataset();

				if(Objects.equals(AUDIT, dataset) || Objects.equals(AUDIT_NA, dataset)){

					if(algorithm.startsWith(SCALAR_LEAF)){
						setEta(ETA_AUDIT);
					}
				} else

				if(Objects.equals(AUTO, dataset) || Objects.equals(AUTO_NA, dataset)){

					if(Objects.equals(algorithm, SCALAR_LEAF + RANDOM_FOREST)){
						setEta(ETA_AUTO / 37);
						setRecordCount(325);
					} else

					if(algorithm.startsWith(SCALAR_LEAF)){
						setEta(ETA_AUTO);
						setRecordCount(392);
					}
				}
			}

			@Override
			public MultiRegressionTest getArchiveBatchTest(){
				return MultiRegressionTest.this;
			}

			@Override
			public String getFeatureMapPath(){
				return "/csv/Multi" + truncate(getDataset()) + ".fmap";
			}

			@Override
			public Table getInput() throws IOException {
				String algorithm = getAlgorithm();

				return super.getInput();
			}

			@Override
			public Table getOutput() throws IOException {
				String algorithm = getAlgorithm();

				return super.getOutput();
			}
		};

		return result;
	}

	@Test
	public void evaluateScalarLeafDTAuto() throws Exception {
		evaluate(SCALAR_LEAF + DECISION_TREE, AUTO);
	}

	@Test
	public void evaluateScalarLeafDTAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + DECISION_TREE, AUTO_NA);
	}

	@Test
	public void evaluateScalarLeafGammaAuto() throws Exception {
		evaluate(SCALAR_LEAF + GAMMA_REGRESSION, AUTO, new FloatEquivalence(21));
	}

	@Test
	public void evaluateScalarLeafGammaAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + GAMMA_REGRESSION, AUTO_NA, new FloatEquivalence(18));
	}

	@Test
	public void evaluateScalarLeafLinearAuto() throws Exception {
		evaluate(SCALAR_LEAF + LINEAR_REGRESSION, AUTO, new FloatEquivalence(4 + 9));
	}

	@Test
	public void evaluateScalarLeafLinearAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + LINEAR_REGRESSION, AUTO_NA, new FloatEquivalence(6 + 8));
	}

	@Test
	public void evaluateScalarLeafLogisticAudit() throws Exception {
		evaluate(SCALAR_LEAF + LOGISTIC_REGRESSION, AUDIT, new FloatEquivalence(13));
	}

	@Test
	public void evaluateScalarLeafLogisticAuditNA() throws Exception {
		evaluate(SCALAR_LEAF + LOGISTIC_REGRESSION, AUDIT_NA, new FloatEquivalence(23));
	}

	@Test
	public void evaluateScalarLeafRFAuto() throws Exception {
		evaluate(SCALAR_LEAF + RANDOM_FOREST, AUTO, new FloatEquivalence(7 + 12));
	}

	@Test
	public void evaluateScalarLeafRFAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + RANDOM_FOREST, AUTO_NA, new FloatEquivalence(7 + 4));
	}

	@Test
	public void evaluateVectorLeafDTAuto() throws Exception {
		evaluate(VECTOR_LEAF + DECISION_TREE, AUTO);
	}

	@Test
	public void evaluateVectorLeafDTAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + DECISION_TREE, AUTO_NA);
	}

	@Test
	public void evaluateVectorLeafGammaAuto() throws Exception {
		evaluate(VECTOR_LEAF + GAMMA_REGRESSION, AUTO, new FloatEquivalence(17));
	}

	@Test
	public void evaluateVectorLeafGammaAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + GAMMA_REGRESSION, AUTO_NA, new FloatEquivalence(22));
	}

	@Test
	public void evaluateVectorLeafLinearAuto() throws Exception {
		evaluate(VECTOR_LEAF + LINEAR_REGRESSION, AUTO, new FloatEquivalence(6));
	}

	@Test
	public void evaluateVectorLeafLinearAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + LINEAR_REGRESSION, AUTO_NA, new FloatEquivalence(6));
	}

	@Test
	public void evaluateVectorLeafLogisticAudit() throws Exception {
		evaluate(VECTOR_LEAF + LOGISTIC_REGRESSION, AUDIT, new FloatEquivalence(26));
	}

	@Test
	public void evaluateVectorLeafLogisticAuditNA() throws Exception {
		evaluate(VECTOR_LEAF + LOGISTIC_REGRESSION, AUDIT_NA, new FloatEquivalence(19));
	}

	@Test
	public void evaluateVectorLeafRFAuto() throws Exception {
		evaluate(VECTOR_LEAF + RANDOM_FOREST, AUTO, new FloatEquivalence(12));
	}

	@Test
	public void evaluateVectorLeafRFAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + RANDOM_FOREST, AUTO_NA, new FloatEquivalence(15));
	}

	@Test
	public void evaluateVectorLeafSquaredLogAuto() throws Exception {
		evaluate(VECTOR_LEAF + SQUARED_LOG_REGRESSION, AUTO);
	}

	@Test
	public void evaluateVectorLeafSquaredLogAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + SQUARED_LOG_REGRESSION, AUTO_NA);
	}

	private static final Table DATASET_EMPTY = new Table(0);
}