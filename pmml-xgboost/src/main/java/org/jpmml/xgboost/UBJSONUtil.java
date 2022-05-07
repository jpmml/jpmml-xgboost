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

import com.devsmart.ubjson.UBArray;
import com.devsmart.ubjson.UBValue;

public class UBJSONUtil {

	private UBJSONUtil(){
	}

	static
	public boolean[] toBooleanArray(UBValue value){
		UBArray array = value.asArray();

		if(array.isBool()){
			return array.asBoolArray();
		}

		boolean[] result = new boolean[array.size()];

		for(int i = 0; i < result.length; i++){
			UBValue element = array.get(i);

			if(element.isBool()){
				result[i] = element.asBool();
			} else

			if(element.isNumber()){
				result[i] = (element.asInt() == 1);
			} else

			{
				throw new IllegalArgumentException();
			}
		}

		return result;
	}

	static
	public int[] toIntArray(UBValue value){
		UBArray array = value.asArray();

		if(array.isInteger()){
			return array.asInt32Array();
		}

		int[] result = new int[array.size()];

		for(int i = 0; i < result.length; i++){
			UBValue element = array.get(i);

			result[i] = element.asInt();
		}

		return result;
	}

	static
	public float[] toFloatArray(UBValue value){
		UBArray array = value.asArray();

		if(array.isNumber()){
			return array.asFloat32Array();
		}

		float[] result = new float[array.size()];

		for(int i = 0; i < result.length; i++){
			UBValue element = array.get(i);

			result[i] = element.asFloat32();
		}

		return result;
	}

	static
	public String[] toStringArray(UBValue value){
		UBArray array = value.asArray();

		if(array.isString()){
			return array.asStringArray();
		}

		String[] result = new String[array.size()];

		for(int i = 0; i < result.length; i++){
			UBValue element = array.get(i);

			result[i] = element.asString();
		}

		return result;
	}
}