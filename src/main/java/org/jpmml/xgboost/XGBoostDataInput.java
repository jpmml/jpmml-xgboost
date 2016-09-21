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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.io.ByteStreams;
import com.google.common.io.LittleEndianDataInputStream;

public class XGBoostDataInput implements Closeable {

	private LittleEndianDataInputStream dis = null;


	public XGBoostDataInput(InputStream is) throws IOException {
		this.dis = new LittleEndianDataInputStream(init(new PushbackInputStream(is, 4)));
	}

	@Override
	public void close() throws IOException {
		this.dis.close();
	}

	public int readInt() throws IOException {
		return this.dis.readInt();
	}

	public float readFloat() throws IOException {
		return this.dis.readFloat();
	}

	public String readString() throws IOException {
		int length = (int)this.dis.readLong();

		byte[] buffer = new byte[length];

		ByteStreams.readFully(this.dis, buffer);

		return new String(buffer);
	}

	public Map<String, String> readStringMap() throws IOException {
		int length = (int)this.dis.readLong();

		Map<String, String> result = new LinkedHashMap<>();

		for(int i = 0; i < length; i++){
			result.put(readString(), readString());
		}

		return result;
	}

	public void readReserved(int length) throws IOException {

		for(int i = 0; i < length; i++){
			int value = this.dis.readInt();

			if(value != 0){
				throw new IOException();
			}
		}
	}

	static
	private InputStream init(PushbackInputStream is) throws IOException {
		byte[] header = new byte[4];

		ByteStreams.readFully(is, header);

		if(!Arrays.equals(XGBoostDataInput.BINF_MAGIC, header)){
			is.unread(header);
		}

		return is;
	}

	private static final byte[] BINF_MAGIC = {'b', 'i', 'n', 'f'};
}