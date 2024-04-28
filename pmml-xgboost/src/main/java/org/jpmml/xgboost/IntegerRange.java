/*
 * Copyright (c) 2024 Villu Ruusmann
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

import java.util.AbstractList;

class IntegerRange extends AbstractList<Integer> {

	private int size;


	IntegerRange(int size){

		if(size < 0){
			throw new IllegalArgumentException();
		}

		this.size = size;
	}

	@Override
	public boolean isEmpty(){
		return (this.size == 0);
	}

	@Override
	public int size(){
		return this.size;
	}

	@Override
	public Integer get(int i){

		if(i < 0 || i >= this.size){
			throw new IndexOutOfBoundsException();
		}

		return i;
	}
}