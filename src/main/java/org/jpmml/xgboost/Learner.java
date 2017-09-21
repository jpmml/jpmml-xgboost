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

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.Schema;
import org.jpmml.xgboost.visitors.TreeModelCompactor;

public class Learner {

	private float base_score;

	private int num_features;

	private int num_class;

	private int contain_extra_attrs;

	private int contain_eval_metrics;

	private ObjFunction obj;

	private GBTree gbtree;

	private Map<String, String> attributes = null;

	private List<String> metrics = null;


	public Learner(){
	}

	public void load(XGBoostDataInput input) throws IOException {
		this.base_score = input.readFloat();
		this.num_features = input.readInt();
		this.num_class = input.readInt();
		this.contain_extra_attrs = input.readInt();
		this.contain_eval_metrics = input.readInt();

		input.readReserved(29);

		String name_obj = input.readString();
		switch(name_obj){
			case "reg:gamma":
				this.obj = new GammaRegression();
				break;
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
				this.obj = new BinomialLogisticRegression();
				break;
			case "multi:softmax":
			case "multi:softprob":
				this.obj = new MultinomialLogisticRegression(this.num_class);
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
		} // End if

		if(this.obj instanceof PoissonRegression){
			String max_delta_step;

			try {
				max_delta_step = input.readString();
			} catch(EOFException eof){
				// Ignored
			}
		} // End if

		if(this.contain_eval_metrics != 0){
			this.metrics = input.readStringList();
		}
	}

	public PMML encodePMML(FieldName targetField, List<String> targetCategories, FeatureMap featureMap, Integer ntreeLimit, boolean transform){
		XGBoostEncoder encoder = new XGBoostEncoder();

		if(targetField == null){
			targetField = FieldName.create("_target");
		}

		Label label = this.obj.encodeLabel(targetField, targetCategories, encoder);

		List<Feature> features = featureMap.encodeFeatures(encoder);

		Schema schema = new Schema(label, features);

		MiningModel miningModel = encodeMiningModel(ntreeLimit, transform, schema);

		PMML pmml = encoder.encodePMML(miningModel);

		return pmml;
	}

	/**
	 * @see XGBoostUtil#toXGBoostSchema(Schema)
	 */
	public MiningModel encodeMiningModel(Integer ntreeLimit, boolean transform, Schema schema){
		MiningModel miningModel = this.gbtree.encodeMiningModel(this.obj, this.base_score, ntreeLimit, schema);

		if(transform){
			List<Visitor> visitors = Arrays.<Visitor>asList(new TreeModelCompactor());

			for(Visitor visitor : visitors){
				visitor.applyTo(miningModel);
			}
		}

		return miningModel;
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