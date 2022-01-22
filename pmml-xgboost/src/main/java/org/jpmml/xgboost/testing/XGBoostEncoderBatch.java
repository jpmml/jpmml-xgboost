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

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.Header;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Timestamp;
import org.jpmml.converter.testing.ModelEncoderBatch;
import org.jpmml.converter.testing.OptionsUtil;
import org.jpmml.evaluator.ResultField;
import org.jpmml.model.ReflectionUtil;
import org.jpmml.xgboost.FeatureMap;
import org.jpmml.xgboost.HasXGBoostOptions;
import org.jpmml.xgboost.Learner;
import org.jpmml.xgboost.XGBoostUtil;

abstract
public class XGBoostEncoderBatch extends ModelEncoderBatch {

	private String format = System.getProperty(XGBoostEncoderBatch.class.getName() + ".format", "model,json");


	public XGBoostEncoderBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);
	}

	@Override
	abstract
	public XGBoostEncoderBatchTest getArchiveBatchTest();

	@Override
	public List<Map<String, Object>> getOptionsMatrix(){
		String dataset = getDataset();

		Integer ntreeLimit = null;

		int index = dataset.indexOf('@');
		if(index > -1){
			ntreeLimit = new Integer(dataset.substring(index + 1));
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasXGBoostOptions.OPTION_COMPACT, new Boolean[]{false, true});
		options.put(HasXGBoostOptions.OPTION_PRUNE, true);
		options.put(HasXGBoostOptions.OPTION_NAN_AS_MISSING, true);
		options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, ntreeLimit);

		return OptionsUtil.generateOptionsMatrix(options);
	}

	public String getLearnerPath(String format){
		return "/xgboost/" + (getAlgorithm() + truncate(getDataset())) + "." + format;
	}

	public String getFeatureMapPath(){
		return "/csv/" + truncate(getDataset()) + ".fmap";
	}

	@Override
	public PMML getPMML() throws Exception {
		PMML result = null;

		String[] formats = this.format.split(",");
		for(String format : formats){
			PMML pmml = loadPMML(getLearnerPath(format), getFeatureMapPath());

			if(result != null){
				assertEquals(result, pmml);
			}

			result = pmml;
		}

		return result;
	}

	@Override
	public String getInputCsvPath(){
		return "/csv/" + truncate(getDataset()) + ".csv";
	}

	@Override
	public String getOutputCsvPath(){
		return super.getOutputCsvPath();
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

	private void assertEquals(PMML left, PMML right){
		Header leftHeader = left.requireHeader();
		Header rightHeader = right.requireHeader();

		Timestamp leftTimestamp = leftHeader.getTimestamp();
		Timestamp rightTimestamp = rightHeader.getTimestamp();

		try {
			leftHeader.setTimestamp(null);
			rightHeader.setTimestamp(null);

			boolean equals = ReflectionUtil.equals(left, right);
			if(!equals){
				throw new AssertionError();
			}
		} finally {
			leftHeader.setTimestamp(leftTimestamp);
			rightHeader.setTimestamp(rightTimestamp);
		}
	}
}