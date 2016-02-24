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

public class XGBoostUtil {

	private XGBoostUtil(){
	}

	static
	public Learner loadLearner(InputStream is) throws IOException {
		XGBoostDataInput input = new XGBoostDataInput(is);

		Learner learner = new Learner();
		learner.load(input);

		int eof = is.read();
		if(eof != -1){
			throw new IOException();
		}

		return learner;
	}

	static
	public FeatureMap loadFeatureMap(InputStream is) throws IOException {
		FeatureMap featureMap = new FeatureMap();
		featureMap.load(is);

		return featureMap;
	}
}