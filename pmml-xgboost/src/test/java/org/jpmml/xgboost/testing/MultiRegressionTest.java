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

import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.jupiter.api.Test;

public class MultiRegressionTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, XGBoostFormats {

	public MultiRegressionTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			{
				setFormats(new String[]{JSON, UBJSON});
			}

			@Override
			public MultiRegressionTest getArchiveBatchTest(){
				return MultiRegressionTest.this;
			}

			@Override
			public String getFeatureMapPath(){
				return "/csv/Multi" + truncate(getDataset()) + ".fmap";
			}
		};

		return result;
	}

	@Test
	public void evaluateMultiLinearAuto() throws Exception {
		evaluate("Multi" + LINEAR_REGRESSION, AUTO, new FloatEquivalence(8));
	}

	@Test
	public void evaluateMultiLinearAutoNA() throws Exception {
		evaluate("Multi" + LINEAR_REGRESSION, AUTO_NA, new FloatEquivalence(8));
	}

	@Test
	public void evaluateMultiRFAuto() throws Exception {
		evaluate("Multi" + RANDOM_FOREST, AUTO, new FloatEquivalence(8 + 4));
	}

	@Test
	public void evaluateMultiRFAutoNA() throws Exception {
		evaluate("Multi" + RANDOM_FOREST, AUTO_NA, new FloatEquivalence(8));
	}
}