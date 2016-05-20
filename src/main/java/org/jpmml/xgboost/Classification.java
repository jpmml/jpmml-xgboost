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

import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;

abstract
public class Classification extends ObjFunction {

	private int num_class;


	public Classification(int num_class){
		this.num_class = num_class;
	}

	@Override
	public DataType getDataType(){
		return DataType.STRING;
	}

	@Override
	public OpType getOpType(){
		return OpType.CATEGORICAL;
	}

	public int getNumClass(){
		return this.num_class;
	}
}