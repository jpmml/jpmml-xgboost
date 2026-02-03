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

import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.jupiter.api.Test;

public class RegressionTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, XGBoostFormats {

	public RegressionTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			{
				String dataset = getDataset();

				// XXX
				if(dataset.startsWith(AUDIT) || dataset.startsWith(AUTO) || dataset.startsWith(VISIT)){
					setFormats(new String[]{JSON, UBJSON});
				}
			}

			@Override
			public RegressionTest getArchiveBatchTest(){
				return RegressionTest.this;
			}
		};

		return result;
	}

	@Test
	public void evaluateLinearAuto() throws Exception {
		evaluate(LINEAR_REGRESSION, AUTO, new FloatEquivalence(8));
	}

	@Test
	public void evaluateLinearAutoNA() throws Exception {
		evaluate(LINEAR_REGRESSION, AUTO_NA, new FloatEquivalence(8));
	}

	@Test
	public void evaluateLogisticAudit() throws Exception {
		evaluate(LOGISTIC_REGRESSION, AUDIT, new FloatEquivalence(20 + 8));
	}

	@Test
	public void evaluateLogisticAuditNA() throws Exception {
		evaluate(LOGISTIC_REGRESSION, AUDIT_NA, new FloatEquivalence(16 + 4));
	}

	@Test
	public void evaluateAFTLungNA() throws Exception {
		evaluate(AFT, LUNG_NA, new FloatEquivalence(16 + 2));
	}

	@Test
	public void evaluateGammaVisit() throws Exception {
		evaluate(GAMMA_REGRESSION, VISIT, new FloatEquivalence(16 + 2));
	}

	@Test
	public void evaluateGammaVisitNA() throws Exception {
		evaluate(GAMMA_REGRESSION, VISIT_NA, new FloatEquivalence(16));
	}

	@Test
	public void evaluatePoissonVisit() throws Exception {
		evaluate(POISSON_REGRESSION, VISIT, new FloatEquivalence(16));
	}

	@Test
	public void evaluatePoissonVisitNA() throws Exception {
		evaluate(POISSON_REGRESSION, VISIT_NA, new FloatEquivalence(16 + 4));
	}

	@Test
	public void evaluateTweedieVisit() throws Exception {
		evaluate(TWEEDIE_REGRESSION, VISIT, new FloatEquivalence(12 + 2));
	}

	@Test
	public void evaluateTweedieVisitNA() throws Exception {
		evaluate(TWEEDIE_REGRESSION, VISIT_NA, new FloatEquivalence(12 + 2));
	}
}