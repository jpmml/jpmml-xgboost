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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.DataType;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Schema;

public class XGBoostUtil {

	private XGBoostUtil(){
	}

	static
	public Learner loadLearner(InputStream is) throws IOException {
		XGBoostDataInput input = new XGBoostDataInput(is);

		Learner learner = new Learner();
		learner.load(input);

		ObjFunction obj = learner.getObj();

		if(obj instanceof PoissonRegression){
			String max_delta_step;

			try {
				max_delta_step = input.readString();
			} catch(EOFException eof){
				// Ignored
			}
		}

		int eof = is.read();
		if(eof != -1){
			throw new IOException();
		}

		return learner;
	}

	static
	public FeatureMap loadFeatureMap(InputStream is) throws IOException {
		FeatureMap featureMap = new FeatureMap();
		featureMap.load(is);

		return featureMap;
	}

	static
	public Schema toXGBoostSchema(Schema schema){
		List<Feature> xgbFeatures = new ArrayList<>();

		List<Feature> features = schema.getFeatures();
		for(Feature feature : features){

			if(feature instanceof BinaryFeature){
				BinaryFeature binaryFeature = (BinaryFeature)feature;

				xgbFeatures.add(binaryFeature);
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
						throw new IllegalArgumentException();
				}

				xgbFeatures.add(continuousFeature);
			}
		}

		Schema xgbSchema = new Schema(schema.getLabel(), xgbFeatures);

		return xgbSchema;
	}
}