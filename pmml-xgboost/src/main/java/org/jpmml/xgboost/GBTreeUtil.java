/*
 * Copyright (c) 2026 Villu Ruusmann
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
import java.util.List;

import com.google.common.math.Quantiles;

public class GBTreeUtil {

	private GBTreeUtil(){
	}

	static
	public Float estimateEta(GBTree gbtree){
		RegTree[] trees = gbtree.trees();

		if(trees == null){
			return null;
		}

		Boolean rawLeafValues = hasRawLeafValues(trees);

		if(rawLeafValues == null){
			return null;
		} // End if

		if(rawLeafValues.booleanValue()){
			return estimateEtaDirect(trees);
		} else

		{
			return estimateEtaIndirect(trees);
		}
	}

	static
	private Boolean hasRawLeafValues(RegTree[] trees){
		int shrunken = 0;
		int raw = 0;

		for(RegTree tree : trees){
			Node[] nodes = tree.nodes();
			NodeStat[] stats = tree.stats();

			for(int i = GBTreeUtil.INDEX_FIRST_NON_ROOT; i < nodes.length; i++){
				Node node = nodes[i];
				NodeStat stat = stats[i];

				if(!node.is_leaf()){
					continue;
				} // End if

				if(node.leaf_value() == stat.base_weight()){
					shrunken++;
				} else

				{
					raw++;
				}
			}
		}

		if(shrunken > 0 && raw == 0){
			return Boolean.FALSE;
		} // End if

		if(raw > 0 && shrunken == 0){
			return Boolean.TRUE;
		}

		return null;
	}

	static
	private Float estimateEtaDirect(RegTree[] trees){
		List<Double> etas = new ArrayList<>();

		for(RegTree tree : trees){
			Node[] nodes = tree.nodes();
			NodeStat[] stats = tree.stats();

			for(int i = GBTreeUtil.INDEX_FIRST_NON_ROOT; i < nodes.length; i++){
				Node node = nodes[i];
				NodeStat stat = stats[i];

				if(node.is_leaf() && Math.abs(stat.base_weight()) > GBTreeUtil.EPSILON){
					Double eta = ((double)node.leaf_value()) / ((double)stat.base_weight());

					etas.add(eta);
				}
			}
		}

		return summarize(etas);
	}

	static
	private Float estimateEtaIndirect(RegTree[] trees){
		List<double[][]> triplets = collectTriplets(trees);

		if(triplets.size() < GBTreeUtil.MIN_TRIPLETS){
			return null;
		}

		Double lambda = solveLambda(triplets);
		if(lambda == null){
			return null;
		}

		List<Double> etas = estimateEtas(triplets, lambda);

		return summarize(etas);
	}

	static
	private Double solveLambda(List<double[][]> triplets){
		int size = triplets.size();

		List<Double> lambdas = new ArrayList<>();

		// Pair up triplets that are far apart, because neighbouring triplets tend to be near-collinear
		int offset = size / 2;

		for(int i = 0; i < size; i++){
			solveLambda(triplets.get(i), triplets.get((i + offset) % size), lambdas);
		}

		if(lambdas.size() < GBTreeUtil.MIN_TRIPLETS){
			return null;
		}

		return GBTreeUtil.MEDIAN.compute(lambdas);
	}

	static
	private void solveLambda(double[][] left, double[][] right, List<Double> lambdas){
		// Every triplet expresses its eta value as a ratio of two linear functions of lambda
		double[][] leftCoefficients = coefficients(left);
		double[][] rightCoefficients = coefficients(right);

		double leftNumeratorConstant = leftCoefficients[0][0];
		double leftNumeratorSlope = leftCoefficients[0][1];
		double leftDenominatorConstant = leftCoefficients[1][0];
		double leftDenominatorSlope = leftCoefficients[1][1];

		double rightNumeratorConstant = rightCoefficients[0][0];
		double rightNumeratorSlope = rightCoefficients[0][1];
		double rightDenominatorConstant = rightCoefficients[1][0];
		double rightDenominatorSlope = rightCoefficients[1][1];

		// Requiring the two triplets to yield the same eta value, and cross-multiplying, gives a quadratic equation in lambda
		double a = leftNumeratorSlope * rightDenominatorSlope - rightNumeratorSlope * leftDenominatorSlope;
		double b = leftNumeratorConstant * rightDenominatorSlope + leftNumeratorSlope * rightDenominatorConstant - rightNumeratorConstant * leftDenominatorSlope - rightNumeratorSlope * leftDenominatorConstant;
		double c = leftNumeratorConstant * rightDenominatorConstant - rightNumeratorConstant * leftDenominatorConstant;

		if(Math.abs(a) < GBTreeUtil.EPSILON){

			// The two triplets are collinear, and the equation degenerates to a linear one
			if(Math.abs(b) > GBTreeUtil.EPSILON){
				collectLambda(-c / b, left, lambdas);
			}

			return;
		}

		double discriminant = b * b - 4d * a * c;
		if(discriminant < 0d){
			return;
		}

		double root = Math.sqrt(discriminant);

		collectLambda((-b + root) / (2d * a), left, lambdas);
		collectLambda((-b - root) / (2d * a), left, lambdas);
	}

	static
	private void collectLambda(double lambda, double[][] triplet, List<Double> lambdas){

		if(lambda < 0d || !Double.isFinite(lambda)){
			return;
		}

		Double eta = estimateEta(triplet, lambda);

		// A quadratic equation has two roots, one of which is spurious
		if(eta != null && eta.doubleValue() > 0d){
			lambdas.add(lambda);
		}
	}

	static
	private List<Double> estimateEtas(List<double[][]> triplets, double lambda){
		List<Double> result = new ArrayList<>();

		for(double[][] triplet : triplets){
			Double eta = estimateEta(triplet, lambda);

			if(eta != null){
				result.add(eta);
			}
		}

		return result;
	}

	static
	private Double estimateEta(double[][] triplet, double lambda){
		double[][] coefficients = coefficients(triplet);

		double denominator = coefficients[1][0] + coefficients[1][1] * lambda;

		if(Math.abs(denominator) < GBTreeUtil.EPSILON){
			return null;
		}

		return (coefficients[0][0] + coefficients[0][1] * lambda) / denominator;
	}

	static
	private double[][] coefficients(double[][] triplet){
		double weight = triplet[0][0];
		double hess = triplet[0][1];
		double leftWeight = triplet[1][0];
		double leftHess = triplet[1][1];
		double rightWeight = triplet[2][0];
		double rightHess = triplet[2][1];

		return new double[][]{
			{leftWeight * leftHess + rightWeight * rightHess, leftWeight + rightWeight},
			{weight * hess, weight}
		};
	}

	static
	private List<double[][]> collectTriplets(RegTree[] trees){
		List<double[][]> result = new ArrayList<>();

		for(RegTree tree : trees){
			Node[] nodes = tree.nodes();
			NodeStat[] stats = tree.stats();

			for(int i = GBTreeUtil.INDEX_ROOT; i < nodes.length; i++){
				Node node = nodes[i];
				NodeStat stat = stats[i];

				if(node.is_leaf()){
					continue;
				}

				Node leftChild = nodes[node.left_child()];
				NodeStat leftStat = stats[node.left_child()];

				Node rightChild = nodes[node.right_child()];
				NodeStat rightStat = stats[node.right_child()];

				if(!leftChild.is_leaf() || !rightChild.is_leaf()){
					continue;
				} // End if

				if(Math.abs(stat.base_weight()) < GBTreeUtil.EPSILON){
					continue;
				}

				double[][] triplet = {
					{stat.base_weight(), stat.sum_hess()},
					{leftStat.base_weight(), leftStat.sum_hess()},
					{rightStat.base_weight(), rightStat.sum_hess()}
				};

				result.add(triplet);
			}
		}

		return result;
	}

	static
	private Float summarize(List<Double> etas){

		if(dispersion(etas) > GBTreeUtil.MAX_DISPERSION){
			return null;
		}

		return (float)GBTreeUtil.MEDIAN.compute(etas);
	}

	static
	private double dispersion(List<Double> etas){

		if(etas.size() < GBTreeUtil.MIN_TRIPLETS){
			return Double.MAX_VALUE;
		}

		double median = GBTreeUtil.MEDIAN.compute(etas);

		if(Math.abs(median) < GBTreeUtil.EPSILON){
			return Double.MAX_VALUE;
		}

		List<Double> deviations = new ArrayList<>();

		for(Double eta : etas){
			deviations.add(Math.abs(eta - median));
		}

		return GBTreeUtil.MEDIAN.compute(deviations) / Math.abs(median);
	}

	private static final Quantiles.ScaleAndIndex MEDIAN = Quantiles.median();

	private static final int INDEX_ROOT = 0;
	private static final int INDEX_FIRST_NON_ROOT = 1;

	private static final double EPSILON = 1e-6d;

	private static final int MIN_TRIPLETS = 8;

	private static final double MAX_DISPERSION = 1e-5d;
}