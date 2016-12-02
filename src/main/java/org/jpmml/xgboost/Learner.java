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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.DataDictionary;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Value;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.model.visitors.DataDictionaryCleaner;
import org.jpmml.model.visitors.MiningSchemaCleaner;

public class Learner {

	private float base_score;

	private int num_features;

	private int num_class;

	private int contain_extra_attrs;

	private ObjFunction obj;

	private GBTree gbtree;

	private Map<String, String> attributes = null;


	public Learner(){
	}

	public void load(XGBoostDataInput input) throws IOException {
		this.base_score = input.readFloat();
		this.num_features = input.readInt();
		this.num_class = input.readInt();
		this.contain_extra_attrs = input.readInt();

		input.readReserved(30);

		String name_obj = input.readString();
		switch(name_obj){
			case "reg:linear":
				this.obj = new LinearRegression();
				break;
			case "reg:logistic":
				this.obj = new LogisticRegression();
				break;
			case "count:poisson":
				this.obj = new PoissonRegression();
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

		if(this.contain_extra_attrs != 0){
			this.attributes = input.readStringMap();
		}
	}

	public PMML encodePMML(FieldName targetField, List<String> targetCategories, FeatureMap featureMap){

		if(targetField == null){
			targetField = FieldName.create("_target");
		}

		DataField dataField = new DataField(targetField, this.obj.getOpType(), this.obj.getDataType());

		targetCategories = this.obj.prepareTargetCategories(targetCategories);
		if(targetCategories != null && targetCategories.size() > 0){
			List<Value> values = dataField.getValues();

			values.addAll(PMMLUtil.createValues(targetCategories));
		}

		Schema schema = featureMap.createSchema(targetField, targetCategories);

		MiningModel miningModel = encodeMiningModel(schema);

		List<DataField> dataFields = new ArrayList<>();
		dataFields.add(dataField);
		dataFields.addAll(featureMap.getDataFields());

		DataDictionary dataDictionary = new DataDictionary(dataFields);

		PMML pmml = new PMML("4.3", PMMLUtil.createHeader(Learner.class), dataDictionary)
			.addModels(miningModel);

		List<? extends Visitor> visitors = Arrays.asList(new MiningSchemaCleaner(), new DataDictionaryCleaner());
		for(Visitor visitor : visitors){
			visitor.applyTo(pmml);
		}

		return pmml;
	}

	public MiningModel encodeMiningModel(Schema schema){
		return this.gbtree.encodeMiningModel(this.obj, this.base_score, schema);
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
}