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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jpmml.converter.HasNativeConfiguration;
import org.jpmml.converter.HasOptions;

public interface HasXGBoostOptions extends HasOptions, HasNativeConfiguration {

	String OPTION_BYTE_ORDER = "byte_order";

	String OPTION_CHARSET = "charset";

	String OPTION_COMPACT = "compact";

	String OPTION_MISSING = "missing";

	String OPTION_NTREE_LIMIT = "ntree_limit";

	String OPTION_NUMERIC = "numeric";

	String OPTION_PRUNE = "prune";

	@Override
	default
	public Map<String, ?> getNativeConfiguration(){
		Map<String, Object> result = new LinkedHashMap<>();
		result.put(HasXGBoostOptions.OPTION_COMPACT, Boolean.FALSE);

		return result;
	}
}