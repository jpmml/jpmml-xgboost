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
package org.jpmml.xgboost;

abstract
public class Node {

	abstract
	public boolean is_leaf();

	abstract
	public float leaf_value();

	abstract
	public int split_index();

	abstract
	public int split_type();

	abstract
	public int split_cond();

	abstract
	public int left_child();

	abstract
	public int right_child();

	abstract
	public boolean default_left();

	public static final int SPLIT_NUMERICAL = 0;
	public static final int SPLIT_CATEGORICAL = 1;
}