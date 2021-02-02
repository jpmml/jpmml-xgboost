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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BaseNFeature;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueFeature;
import org.jpmml.converter.Schema;
import org.jpmml.converter.visitors.NaNAsMissingDecorator;
import org.jpmml.xgboost.visitors.TreeModelCompactor;

public class Learner implements Loadable {

	private float base_score;

	private int num_features;

	private int num_class;

	private int contain_extra_attrs;

	private int contain_eval_metrics;

	private int major_version;

	private int minor_version;

	private ObjFunction obj;

	private GBTree gbtree;

	private Map<String, String> attributes = null;

	private String[] metrics = null;


	public Learner(){
	}

	@Override
	public void load(XGBoostDataInput input) throws IOException {
		this.base_score = input.readFloat();
		this.num_features = input.readInt();
		this.num_class = input.readInt();
		this.contain_extra_attrs = input.readInt();
		this.contain_eval_metrics = input.readInt();

		this.major_version = input.readInt();
		this.minor_version = input.readInt();

		if(this.major_version < 0 || this.major_version > 1){
			throw new IllegalArgumentException(this.major_version + "." + this.minor_version);
		}

		input.readReserved(27);

		String name_obj = input.readString();
		switch(name_obj){
			case "reg:linear":
			case "reg:squarederror":
			case "reg:squaredlogerror":
				this.obj = new LinearRegression();
				break;
			case "reg:logistic":
				this.obj = new LogisticRegression();
				break;
			case "reg:gamma":
			case "reg:tweedie":
				this.obj = new GeneralizedLinearRegression();
				break;
			case "count:poisson":
				this.obj = new PoissonRegression();
				break;
			case "binary:hinge":
				this.obj = new HingeClassification();
				break;
			case "binary:logistic":
				this.obj = new BinomialLogisticRegression();
				break;
			case "rank:map":
			case "rank:ndcg":
			case "rank:pairwise":
				this.obj = new LambdaMART();
				break;
			case "multi:softmax":
			case "multi:softprob":
				this.obj = new MultinomialLogisticRegression(this.num_class);
				break;
			default:
				throw new IllegalArgumentException(name_obj);
		}

		// Starting from 1.0.0, the base score is saved as an untransformed value
		if(this.major_version >= 1){
			this.base_score = this.obj.probToMargin(this.base_score) + 0f;
		} else

		{
			this.base_score = this.base_score;
		}

		String name_gbm = input.readString();
		switch(name_gbm){
			case "gbtree":
				this.gbtree = new GBTree();
				break;
			case "dart":
				this.gbtree = new Dart();
				break;
			default:
				throw new IllegalArgumentException(name_gbm);
		}

		this.gbtree.load(input);

		if(this.contain_extra_attrs != 0){
			this.attributes = input.readStringMap();
		} // End if

		if(this.major_version >= 1){
			return;
		} // End if

		if(this.obj instanceof PoissonRegression){
			String max_delta_step;

			try {
				max_delta_step = input.readString();
			} catch(EOFException eofe){
				// Ignored
			}
		} // End if

		if(this.contain_eval_metrics != 0){
			this.metrics = input.readStringVector();
		}
	}

	public Schema encodeSchema(FieldName targetField, List<String> targetCategories, FeatureMap featureMap, XGBoostEncoder encoder){

		if(targetField == null){
			targetField = FieldName.create("_target");
		}

		Label label = this.obj.encodeLabel(targetField, targetCategories, encoder);

		List<Feature> features = featureMap.encodeFeatures(encoder);

		return new Schema(encoder, label, features);
	}

	public Schema toXGBoostSchema(Schema schema){
		Function<Feature, Feature> function = new Function<Feature, Feature>(){

			@Override
			public Feature apply(Feature feature){

				if(feature instanceof BaseNFeature){
					BaseNFeature baseFeature = (BaseNFeature)feature;

					return baseFeature;
				} else

				if(feature instanceof BinaryFeature){
					BinaryFeature binaryFeature = (BinaryFeature)feature;

					return binaryFeature;
				} else

				if(feature instanceof MissingValueFeature){
					MissingValueFeature missingValueFeature = (MissingValueFeature)feature;

					return missingValueFeature;
				} else

				{
					ContinuousFeature continuousFeature = feature.toContinuousFeature();

					DataType dataType = continuousFeature.getDataType();
					switch(dataType){
						case INTEGER:
						case FLOAT:
							break;
						case DOUBLE:
							continuousFeature = continuousFeature.toContinuousFeature(DataType.FLOAT);
							break;
						default:
							throw new IllegalArgumentException("Expected integer, float or double data type for continuous feature " + continuousFeature.getName() + ", got " + dataType.value() + " data type");
					}

					return continuousFeature;
				}
			}
		};

		return schema.toTransformedSchema(function);
	}

	public PMML encodePMML(Map<String, ?> options, FieldName targetField, List<String> targetCategories, FeatureMap featureMap){
		XGBoostEncoder encoder = new XGBoostEncoder();

		Boolean nanAsMissing = (Boolean)options.get(HasXGBoostOptions.OPTION_NAN_AS_MISSING);

		Schema schema = encodeSchema(targetField, targetCategories, featureMap, encoder);

		MiningModel miningModel = encodeMiningModel(options, schema);

		PMML pmml = encoder.encodePMML(miningModel);

		if((Boolean.TRUE).equals(nanAsMissing)){
 			Visitor visitor = new NaNAsMissingDecorator();

 			visitor.applyTo(pmml);
 		}

		return pmml;
	}

	public MiningModel encodeMiningModel(Map<String, ?> options, Schema schema){
		Boolean compact = (Boolean)options.get(HasXGBoostOptions.OPTION_COMPACT);
		Integer ntreeLimit = (Integer)options.get(HasXGBoostOptions.OPTION_NTREE_LIMIT);

		MiningModel miningModel = this.gbtree.encodeMiningModel(this.obj, this.base_score, ntreeLimit, schema)
			.setAlgorithmName("XGBoost (" + this.gbtree.getAlgorithmName() + ")");

		if((Boolean.TRUE).equals(compact)){
			Visitor visitor = new TreeModelCompactor();

			visitor.applyTo(miningModel);
		}

		return miningModel;
	}

	public int num_features(){
		return this.num_features;
	}

	public int num_class(){
		return this.num_class;
	}

	public ObjFunction obj(){
		return this.obj;
	}
}