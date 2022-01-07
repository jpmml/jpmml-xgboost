/*
 * Copyright (c) 2020 Villu Ruusmann
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
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.Header;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Timestamp;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.IntegrationTestBatch;
import org.jpmml.model.ReflectionUtil;
import org.jpmml.xgboost.FeatureMap;
import org.jpmml.xgboost.HasXGBoostOptions;
import org.jpmml.xgboost.Learner;
import org.jpmml.xgboost.XGBoostUtil;

import static org.junit.Assert.assertTrue;

abstract
public class XGBoostTestBatch extends IntegrationTestBatch {

	private String format = System.getProperty(XGBoostTestBatch.class.getName() + ".format", "model,json");


	public XGBoostTestBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		super(name, dataset, predicate, equivalence);
	}

	@Override
	abstract
	public XGBoostTest getIntegrationTest();

	public Map<String, Object> getOptions(){
		String[] dataset = parseDataset();

		Integer ntreeLimit = null;
		if(dataset.length > 1){
			ntreeLimit = new Integer(dataset[1]);
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasXGBoostOptions.OPTION_COMPACT, ntreeLimit != null);
		options.put(HasXGBoostOptions.OPTION_PRUNE, true);
		options.put(HasXGBoostOptions.OPTION_NAN_AS_MISSING, true);
		options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, ntreeLimit);

		return options;
	}

	@Override
	public PMML getPMML() throws Exception {
		PMML result = null;

		String[] dataset = parseDataset();

		String[] extensions = this.format.split(",");
		for(String extension : extensions){
			String learnerPath = "/xgboost/" + getName() + dataset[0] + "." + extension;
			String featureMapPath = "/csv/" + dataset[0] + ".fmap";

			PMML pmml = loadPMML(learnerPath, featureMapPath);

			if(result != null){
				assertEquals(result, pmml);
			}

			result = pmml;
		}

		return result;
	}

	@Override
	public List<Map<String, String>> getInput() throws IOException {
		String[] dataset = parseDataset();

		return loadRecords("/csv/" + dataset[0] + ".csv");
	}

	@Override
	public List<Map<String, String>> getOutput() throws IOException {
		return loadRecords("/csv/" + (getName() + getDataset()) + ".csv");
	}

	protected PMML loadPMML(String learnerPath, String featureMapPath) throws Exception {
		Learner learner;

		try(InputStream is = open(learnerPath)){
			learner = XGBoostUtil.loadLearner(is);
		}

		FeatureMap featureMap;

		try(InputStream is = open(featureMapPath)){
			featureMap = XGBoostUtil.loadFeatureMap(is);
		}

		Map<String, ?> options = getOptions();

		PMML pmml = learner.encodePMML(options, null, null, featureMap);

		validatePMML(pmml);

		return pmml;
	}

	protected String[] parseDataset(){
		String dataset = getDataset();

		int index = dataset.indexOf('@');
		if(index > -1){
			return new String[]{dataset.substring(0, index), dataset.substring(index + 1)};
		}

		return new String[]{dataset};
	}

	private void assertEquals(PMML left, PMML right){
		Header leftHeader = left.requireHeader();
		Header rightHeader = right.requireHeader();

		Timestamp leftTimestamp = leftHeader.getTimestamp();
		Timestamp rightTimestamp = rightHeader.getTimestamp();

		try {
			leftHeader.setTimestamp(null);
			rightHeader.setTimestamp(null);

			assertTrue(ReflectionUtil.equals(left, right));
		} finally {
			leftHeader.setTimestamp(leftTimestamp);
			rightHeader.setTimestamp(rightTimestamp);
		}
	}
}