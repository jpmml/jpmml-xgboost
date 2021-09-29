/*
 * Copyright (c) 2019 Villu Ruusmann
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

import java.util.List;

import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.Schema;

public class LambdaMART extends Regression {

	public LambdaMART(String name){
		super(name);
	}

	@Override
	public MiningModel encodeMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, boolean numeric, Schema schema){
		MiningModel miningModel = createMiningModel(trees, weights, base_score, ntreeLimit, numeric, schema);

		return miningModel;
	}
}