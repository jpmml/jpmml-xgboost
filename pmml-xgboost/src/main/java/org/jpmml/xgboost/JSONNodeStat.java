/*
 * Copyright (c) 2026 Villu Ruusmann
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

import com.devsmart.ubjson.GsonUtil;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBValue;
import com.google.gson.JsonObject;

public class JSONNodeStat extends NodeStat implements JSONLoadable, UBJSONLoadable {

	private float loss_chg;

	private float sum_hess;

	private float base_weight;


	public JSONNodeStat(){
	}

	@Override
	public void loadJSON(JsonObject node){
		UBValue value = GsonUtil.toUBValue(node);

		loadUBJSON(value.asObject());
	}

	@Override
	public void loadUBJSON(UBObject node){
		this.loss_chg = node.get("loss_chg").asFloat32();
		this.sum_hess = node.get("sum_hess").asFloat32();
		this.base_weight = node.get("base_weight").asFloat32();
	}

	@Override
	public boolean hasShrunkenBaseWeight(){
		return true;
	}

	@Override
	public float base_weight(){
		return this.base_weight;
	}
}