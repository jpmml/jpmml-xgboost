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
import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Value;
import org.jpmml.converter.Schema;
import org.jpmml.converter.PMMLUtil;

public class Learner {

	private float base_score;

	private int num_features;

	private int num_class;

	private ObjFunction obj;

	private GBTree gbtree;


	public Learner(){
	}

	public void load(XGBoostDataInput input) throws IOException {
		this.base_score = input.readFloat();
		this.num_features = input.readInt();
		this.num_class = input.readInt();

		input.readReserved(31);

		String name_obj = input.readString();
		switch(name_obj){
			case "reg:linear":
				this.obj = new LinearRegression();
				break;
			case "reg:logistic":
				this.obj = new LogisticRegression();
				break;
			case "binary:logistic":
				this.obj = new LogisticClassification();
				break;
			case "multi:softmax":
			case "multi:softprob":
				this.obj = new SoftMaxClassification(this.num_class);
				break;
			default:
				throw new IllegalArgumentException(name_obj);
		}

		String name_gbm = input.readString();
		switch(name_gbm){
			case "gbtree":
				break;
			default:
				throw new IllegalArgumentException(name_gbm);
		}

		this.gbtree = new GBTree();
		this.gbtree.load(input);
	}

	public PMML encodePMML(FieldName targetField, List<String> targetCategories, FeatureMap featureMap){

		if(targetField == null){
			targetField = FieldName.create("_target");
		} // End if

		if(this.obj instanceof Classification){
			Classification classification = (Classification)this.obj;

			if(targetCategories != null){

				if(targetCategories.size() != classification.getNumClass()){
					throw new IllegalArgumentException();
				}
			} else

			{
				targetCategories = createTargetCategories(classification.getNumClass());
			}
		} else

		{
			if(targetCategories != null){
				throw new IllegalArgumentException();
			}
		}

		DataField dataField = new DataField(targetField, this.obj.getOpType(), this.obj.getDataType());

		if(targetCategories != null){
			List<Value> values = dataField.getValues();

			values.addAll(PMMLUtil.createValues(targetCategories));
		}

		Schema schema = featureMap.createSchema(targetField, targetCategories);

		MiningModel miningModel = this.gbtree.encodeMiningModel(this.obj, this.base_score, schema);

		List<DataField> dataFields = new ArrayList<>();
		dataFields.add(dataField);
		dataFields.addAll(featureMap.getDataFields());

		DataDictionary dataDictionary = new DataDictionary(dataFields);

		PMML pmml = new PMML("4.2", PMMLUtil.createHeader("JPMML-XGBoost", "1.0-SNAPSHOT"), dataDictionary)
			.addModels(miningModel);

		return pmml;
	}

	public float getBaseScore(){
		return this.base_score;
	}

	public int getNumClass(){
		return this.num_class;
	}

	public int getNumFeatures(){
		return this.num_features;
	}

	public ObjFunction getObj(){
		return this.obj;
	}

	public GBTree getGBTree(){
		return this.gbtree;
	}

	static
	private List<String> createTargetCategories(int size){
		List<String> result = new ArrayList<>();

		for(int i = 0; i < size; i++){
			result.add(String.valueOf(i));
		}

		return result;
	}
}