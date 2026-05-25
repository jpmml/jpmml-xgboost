/*
 * Copyright (c) 2026 Villu Ruusmann
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
import org.jpmml.converter.testing.Fields;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.jupiter.api.Test;

public class LegacyClassificationTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, Fields {

	public LegacyClassificationTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new LegacyXGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public LegacyClassificationTest getArchiveBatchTest(){
				return LegacyClassificationTest.this;
			}
		};

		return result;
	}

	@Test
	public void evaluateBinomialAudit() throws Exception {
		evaluate(BINOMIAL_CLASSIFICATION, AUDIT, excludeFields(AUDIT_PROBABILITY_FALSE), new FloatEquivalence(32 + 16));
	}

	@Test
	public void evaluateHingeAudit() throws Exception {
		evaluate(HINGE_CLASSIFICATION, AUDIT);
	}

	@Test
	public void evaluateMultinomialAudit() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, AUDIT, new FloatEquivalence(16 + 4));
	}

	@Test
	public void evaluateMultinomialIris() throws Exception {
		evaluate(MULTINOMIAL_CLASSIFICATION, IRIS, new FloatEquivalence(8));
	}
}
