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
package org.jpmml.xgboost;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.IntegrationTestBatch;

abstract
public class XGBoostTestBatch extends IntegrationTestBatch {

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
		options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, ntreeLimit);

		return options;
	}

	protected String[] parseDataset(){
		String dataset = getDataset();

		int index = dataset.indexOf('@');
		if(index > -1){
			return new String[]{dataset.substring(0, index), dataset.substring(index + 1)};
		}

		return new String[]{dataset};
	}
}