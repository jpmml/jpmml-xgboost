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

import java.io.IOException;
import java.util.List;

public class Dart extends GBTree {

	private List<Float> weight_drop;


	public Dart(){
	}

	@Override
	public void load(XGBoostDataInput input) throws IOException {
		super.load(input);

		int num_trees = num_trees();
		if(num_trees > 0){
			this.weight_drop = input.readFloatList();
		}
	}

	@Override
	public List<Float> weight_drop(){
		return this.weight_drop;
	}
}