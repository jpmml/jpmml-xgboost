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
import org.jpmml.converter.testing.ModelEncoderBatchTest;
import org.jpmml.evaluator.ResultField;

public class XGBoostEncoderBatchTest extends ModelEncoderBatchTest {

	public XGBoostEncoderBatchTest(Equivalence<Object> equivalence){
		super(equivalence);
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			@Override
			public XGBoostEncoderBatchTest getArchiveBatchTest(){
				return XGBoostEncoderBatchTest.this;
			}
		};

		return result;
	}
}