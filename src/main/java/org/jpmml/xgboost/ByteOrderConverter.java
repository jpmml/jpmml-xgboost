/*
 * Copyright (c) 2018 Villu Ruusmann
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

import java.nio.ByteOrder;

import com.beust.jcommander.IStringConverter;

public class ByteOrderConverter implements IStringConverter<ByteOrder> {

	@Override
	public ByteOrder convert(String value){

		if(("BIG_ENDIAN").equalsIgnoreCase(value) || ("BE").equalsIgnoreCase(value)){
			return ByteOrder.BIG_ENDIAN;
		} else

		if(("LITTLE_ENDIAN").equalsIgnoreCase(value) || ("LE").equalsIgnoreCase(value)){
			return ByteOrder.LITTLE_ENDIAN;
		} else

		{
			throw new IllegalArgumentException(value);
		}
	}
}