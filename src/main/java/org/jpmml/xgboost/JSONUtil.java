/*
 * Copyright (c) 2021 Villu Ruusmann
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

import com.google.gson.JsonArray;

public class JSONUtil {

	private JSONUtil(){
	}

	static
	public boolean[] toBooleanArray(JsonArray array){
		boolean[] result = new boolean[array.size()];

		for(int i = 0; i < result.length; i++){
			result[i] = (array.get(i)).getAsBoolean();
		}

		return result;
	}

	static
	public int[] toIntArray(JsonArray array){
		int[] result = new int[array.size()];

		for(int i = 0; i < result.length; i++){
			result[i] = (array.get(i)).getAsInt();
		}

		return result;
	}

	static
	public float[] toFloatArray(JsonArray array){
		float[] result = new float[array.size()];

		for(int i = 0; i < result.length; i++){
			result[i] = (array.get(i)).getAsFloat();
		}

		return result;
	}
}