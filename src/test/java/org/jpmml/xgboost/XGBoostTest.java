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
package org.jpmml.xgboost;

import java.io.InputStream;
import java.util.Set;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.IntegrationTestBatch;

public class XGBoostTest extends IntegrationTest {

	@Override
	protected ArchiveBatch createBatch(String name, String dataset){
		ArchiveBatch result = new IntegrationTestBatch(name, dataset){

			@Override
			public IntegrationTest getIntegrationTest(){
				return XGBoostTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				Learner learner;

				try(InputStream is = open("/xgboost/" + getName() + getDataset() + ".model")){
					learner = XGBoostUtil.loadLearner(is);
				}

				FeatureMap featureMap;

				try(InputStream is = open("/csv/" + getDataset() + ".fmap")){
					featureMap = XGBoostUtil.loadFeatureMap(is);
				}

				PMML pmml = learner.encodePMML(null, null, featureMap);

				ensureValidity(pmml);

				return pmml;
			}
		};

		return result;
	}

	@Override
	public void evaluate(Batch batch, Set<FieldName> ignoredFields) throws Exception {
		evaluate(batch, ignoredFields, 1e-6, 1e-6);
	}
}