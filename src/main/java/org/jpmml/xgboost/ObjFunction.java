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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MathContext;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;
import org.jpmml.converter.mining.MiningModelUtil;

abstract
public class ObjFunction {

	abstract
	public Label encodeLabel(FieldName targetField, List<?> targetCategories, PMMLEncoder encoder);

	abstract
	public MiningModel encodeMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, Schema schema);

	public float probToMargin(float value){
		return value;
	}

	static
	protected MiningModel createMiningModel(List<RegTree> trees, List<Float> weights, float base_score, Integer ntreeLimit, Schema schema){

		if(weights != null){

			if(trees.size() != weights.size()){
				throw new IllegalArgumentException();
			}
		} // End if

		if(ntreeLimit != null){

			if(ntreeLimit > trees.size()){
				throw new IllegalArgumentException("Tree limit " + ntreeLimit + " is greater than the number of trees");
			}

			trees = trees.subList(0, ntreeLimit);

			if(weights != null){
				weights = weights.subList(0, ntreeLimit);
			}
		} // End if

		if(weights != null){
			weights = new ArrayList<>(weights);
		}

		ContinuousLabel continuousLabel = (ContinuousLabel)schema.getLabel();

		Schema segmentSchema = schema.toAnonymousSchema();

		PredicateManager predicateManager = new PredicateManager();

		List<TreeModel> treeModels = new ArrayList<>();

		boolean equalWeights = true;

		Iterator<RegTree> treeIt = trees.iterator();
		Iterator<Float> weightIt = (weights != null ? weights.iterator() : null);

		while(treeIt.hasNext()){
			RegTree tree = treeIt.next();
			Float weight = (weightIt != null ? weightIt.next() : null);

			if(tree.isEmpty()){
				weightIt.remove();

				continue;
			} // End if

			if(weight != null){
				equalWeights &= ValueUtil.isOne(weight);
			}

			TreeModel treeModel = tree.encodeTreeModel(predicateManager, segmentSchema);

			treeModels.add(treeModel);
		}

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
			.setMathContext(MathContext.FLOAT)
			.setSegmentation(MiningModelUtil.createSegmentation(equalWeights ? Segmentation.MultipleModelMethod.SUM : Segmentation.MultipleModelMethod.WEIGHTED_SUM, treeModels, weights))
			.setTargets(ModelUtil.createRescaleTargets(null, base_score, continuousLabel));

		return miningModel;
	}

	static
	protected float inverseLogit(float value){
		return (float)-Math.log((1f / value) - 1f);
	}

	static
	protected float inverseExp(float value){
		return (float)Math.log(value);
	}
}