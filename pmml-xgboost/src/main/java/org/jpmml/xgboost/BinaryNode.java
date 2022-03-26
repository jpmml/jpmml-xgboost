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

public class BinaryNode extends Node implements BinaryLoadable {

	private int parent;

	private int cleft;

	private int cright;

	private int sindex;

	private int info;


	public BinaryNode(){
	}

	@Override
	public void loadBinary(XGBoostDataInput input) throws IOException {
		this.parent = input.readInt();
		this.cleft = input.readInt();
		this.cright = input.readInt();
		this.sindex = input.readInt();
		this.info = input.readInt();
	}

	@Override
	public boolean is_leaf(){
		return (this.cleft == -1);
	}

	@Override
	public int left_child(){
		return this.cleft;
	}

	@Override
	public int right_child(){
		return this.cright;
	}

	@Override
	public boolean default_left(){
		return (this.sindex >> 31) != 0;
	}

	@Override
	public int split_index(){
		return (int)(this.sindex & ((1L << 31) - 1L));
	}

	@Override
	public int split_type(){
		return 0;
	}

	@Override
	public int split_cond(){
		return this.info;
	}

	@Override
	public float leaf_value(){
		return Float.intBitsToFloat(this.info);
	}
}