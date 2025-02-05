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
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.FieldNames;
import org.jpmml.converter.testing.Fields;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.Test;

public class MultiClassificationTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, XGBoostFormats, Fields {

	public MultiClassificationTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			{
				setFormats(new String[]{JSON, UBJSON});
			}

			@Override
			public MultiClassificationTest getArchiveBatchTest(){
				return MultiClassificationTest.this;
			}

			@Override
			public String getFeatureMapPath(){
				return "/csv/Multi" + truncate(getDataset()) + ".fmap";
			}
		};

		return result;
	}

	@Test
	public void evaluateMultiBinomialAudit() throws Exception {
		evaluate("Multi" + BINOMIAL_CLASSIFICATION, AUDIT, excludeFields(AUDIT_GENDER_PROBABILITY_FALSE, AUDIT_ADJUSTED_PROBABILITY_FALSE), new FloatEquivalence(24 + 12));
	}

	@Test
	public void evaluateMultiBinomialAuditLimit() throws Exception {
		evaluate("Multi" + BINOMIAL_CLASSIFICATION, AUDIT_LIMIT, excludeFields(AUDIT_GENDER_PROBABILITY_FALSE, AUDIT_ADJUSTED_PROBABILITY_FALSE), new FloatEquivalence(20 + 4));
	}

	@Test
	public void evaluateMultiBinomialAuditNA() throws Exception {
		evaluate("Multi" + BINOMIAL_CLASSIFICATION, AUDIT_NA, excludeFields(AUDIT_GENDER_PROBABILITY_FALSE, AUDIT_ADJUSTED_PROBABILITY_FALSE), new FloatEquivalence(24 + 12));
	}

	@Test
	public void evaluateBinomialAuditNALimit() throws Exception {
		evaluate("Multi" + BINOMIAL_CLASSIFICATION, AUDIT_NA_LIMIT, excludeFields(AUDIT_GENDER_PROBABILITY_FALSE, AUDIT_ADJUSTED_PROBABILITY_FALSE), new FloatEquivalence(20));
	}

	// XXX
	private static final String AUDIT_GENDER_PROBABILITY_FALSE = FieldNameUtil.create(FieldNames.PROBABILITY, "_target1", 0);
	private static final String AUDIT_ADJUSTED_PROBABILITY_FALSE = FieldNameUtil.create(FieldNames.PROBABILITY, "_target2", 0);
}