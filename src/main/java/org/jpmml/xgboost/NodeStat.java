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

public class NodeStat implements BinaryLoadable {

	private float loss_chg;

	private float sum_hess;

	private float base_weight;

	private int leaf_child_cnt;


	public NodeStat(){
	}

	@Override
	public void loadBinary(XGBoostDataInput input) throws IOException {
		this.loss_chg = input.readFloat();
		this.sum_hess = input.readFloat();
		this.base_weight = input.readFloat();
		this.leaf_child_cnt = input.readInt();
	}
}