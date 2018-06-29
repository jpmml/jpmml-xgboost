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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.IntegrationTestBatch;

public class XGBoostTest extends IntegrationTest {

	public XGBoostTest(Equivalence<Object> equivalence){
		super(equivalence);
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<FieldName> predicate){
		ArchiveBatch result = new IntegrationTestBatch(name, dataset, predicate){

			@Override
			public IntegrationTest getIntegrationTest(){
				return XGBoostTest.this;
			}

			@Override
			public PMML getPMML() throws Exception {
				Learner learner;

				String[] dataset = parseDataset();

				try(InputStream is = open("/xgboost/" + getName() + dataset[0] + ".model")){
					learner = XGBoostUtil.loadLearner(is);
				}

				FeatureMap featureMap;

				try(InputStream is = open("/csv/" + dataset[0] + ".fmap")){
					featureMap = XGBoostUtil.loadFeatureMap(is);
				}

				Integer ntreeLimit = null;
				if(dataset.length > 1){
					ntreeLimit = new Integer(dataset[1]);
				}

				Map<String, Object> options = new LinkedHashMap<>();
				options.put(HasXGBoostOptions.OPTION_COMPACT, ntreeLimit != null);
				options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, ntreeLimit);

				PMML pmml = learner.encodePMML(null, null, featureMap, options);

				ensureValidity(pmml);

				return pmml;
			}

			@Override
			public List<Map<FieldName, String>> getInput() throws IOException {
				String[] dataset = parseDataset();

				return loadRecords("/csv/" + dataset[0] + ".csv");
			}

			@Override
			public List<Map<FieldName, String>> getOutput() throws IOException {
				return loadRecords("/csv/" + (getName() + getDataset()) + ".csv");
			}

			private String[] parseDataset(){
				String dataset = getDataset();

				int index = dataset.indexOf('@');
				if(index > -1){
					return new String[]{dataset.substring(0, index), dataset.substring(index + 1)};
				}

				return new String[]{dataset};
			}
		};

		return result;
	}
}