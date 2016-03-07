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
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.Segmentation;

abstract
public class ObjFunction {

	private DataField dataField = null;


	public ObjFunction(DataField dataField){
		setDataField(dataField);
	}

	abstract
	public MiningModel encodeMiningModel(Segmentation segmentation, float base_score, FeatureMap featureMap);

	public DataField getDataField(){
		return this.dataField;
	}

	private void setDataField(DataField dataField){
		this.dataField = dataField;
	}
}