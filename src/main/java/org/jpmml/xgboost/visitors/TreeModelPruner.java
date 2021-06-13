/*
 * Copyright (c) 2021 Villu Ruusmann
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
package org.jpmml.xgboost.visitors;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.dmg.pmml.CompoundPredicate;
import org.dmg.pmml.False;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.tree.Node;
import org.jpmml.converter.visitors.AbstractTreeModelTransformer;

public class TreeModelPruner extends AbstractTreeModelTransformer {

	@Override
	public void enterNode(Node node){
		Object defaultChild = node.getDefaultChild();

		if(node.hasNodes()){
			List<Node> children = node.getNodes();

			for(Iterator<Node> it = children.iterator(); it.hasNext(); ){
				Node child = it.next();

				if(defaultChild != null && equalsNode(defaultChild, child)){
					continue;
				}

				Predicate predicate = child.getPredicate();

				if(predicate instanceof False){
					it.remove();
				}
			}
		}
	}

	@Override
	public void exitNode(Node node){
		Predicate predicate = node.getPredicate();

		if(node.hasNodes()){
			List<Node> children = node.getNodes();

			if(children.size() == 1){
				Node child = children.get(0);

				Predicate childPredicate = child.getPredicate();

				if(equals(predicate, childPredicate)){
					node.setDefaultChild(null);

					initScore(node, child);
					initDefaultChild(node, child);
					replaceChildWithGrandchildren(node, child);
				}
			}
		}
	}

	static
	protected void initDefaultChild(Node parentNode, Node node){
		Object defaultChild = node.getDefaultChild();

		if(parentNode.getDefaultChild() != null){
			throw new IllegalArgumentException();
		}

		parentNode.setDefaultChild(defaultChild);
	}

	static
	private boolean equals(Predicate left, Predicate right){

		if(left instanceof CompoundPredicate && right instanceof CompoundPredicate){
			return equals((CompoundPredicate)left, (CompoundPredicate)right);
		}

		return Objects.equals(left, right);
	}

	static
	private boolean equals(CompoundPredicate left, CompoundPredicate right){

		if(!Objects.equals(left.getBooleanOperator(), right.getBooleanOperator())){
			return false;
		}

		List<Predicate> leftPredicates = left.getPredicates();
		List<Predicate> rightPredicates = right.getPredicates();

		if(leftPredicates.size() != rightPredicates.size()){
			return false;
		}

		for(int i = 0; i < leftPredicates.size(); i++){

			if(!equals(leftPredicates.get(0), rightPredicates.get(0))){
				return false;
			}
		}

		return true;
	}
}