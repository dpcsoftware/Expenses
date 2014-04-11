/*
 *   Copyright 2013, 2014 Daniel Pereira Coelho
 *   
 *   This file is part of the Expenses Android Application.
 *
 *   Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation in version 3.
 *
 *   Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Expenses.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.dpcsoftware.mn;

import android.provider.BaseColumns;

public class Db {

	public static class Table1 implements BaseColumns {
	    public static final String TABLE_NAME = "gastos";
	    public static final String COLUMN_VALORT = "valort";
	    public static final String COLUMN_DATAT = "datat";
	    public static final String COLUMN_DESCRIC = "descric";
	    public static final String COLUMN_IDGRUPO = "idgrupo";
	    public static final String COLUMN_IDCAT = "idcat";
	}
	
	public static class Table2 implements BaseColumns {
		public static final String TABLE_NAME = "categorias";
		public static final String COLUMN_NCAT = "ncat";
		public static final String COLUMN_CORCAT = "corcat";
	}
	
	public static class Table3 implements BaseColumns {
		public static final String TABLE_NAME = "grupos";
		public static final String COLUMN_NGRUPO = "ngrupo";
	}
}