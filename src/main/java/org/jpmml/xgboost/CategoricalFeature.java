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

import org.dmg.pmml.DataField;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;

public class CategoricalFeature extends Feature {

	private String value = null;


	public CategoricalFeature(DataField dataField, String value){
		super(dataField);

		setValue(value);
	}

	@Override
	public boolean isDefaultLeft(Node node){
		return true;
	}

	@Override
	public Predicate encodePredicate(int splitCondition, boolean left){
		Predicate simplePredicate = new SimplePredicate()
			.setField(getName())
			.setOperator(left ? SimplePredicate.Operator.NOT_EQUAL : SimplePredicate.Operator.EQUAL)
			.setValue(getValue());

		return simplePredicate;
	}

	public String getValue(){
		return this.value;
	}

	private void setValue(String value){
		this.value = value;
	}
}