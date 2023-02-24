/*
 *   Copyright 2023 Daniel Pereira Coelho
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

        public static final String AMOUNT = "valort";
        public static final String DATE = "datat";
        public static final String DETAILS = "descric";
        public static final String ID_GROUP = "idgrupo";
        public static final String ID_CATEGORY = "idcat";

        public static final String T_ID = TABLE_NAME + "." + _ID;
        public static final String T_AMOUNT = TABLE_NAME + "." + AMOUNT;
        public static final String T_DATE = TABLE_NAME + "." + DATE;
        public static final String T_DETAILS = TABLE_NAME + "." + DETAILS;
        public static final String T_ID_GROUP = TABLE_NAME + "." + ID_GROUP;
        public static final String T_ID_CATEGORY = TABLE_NAME + "." + ID_CATEGORY;
    }

    public static class Table2 implements BaseColumns {
        public static final String TABLE_NAME = "categorias";

        public static final String CATEGORY_NAME = "ncat";
        public static final String CATEGORY_COLOR = "corcat";

        public static final String T_ID = TABLE_NAME + "." + _ID;
        public static final String T_CATEGORY_NAME = TABLE_NAME + "." + CATEGORY_NAME;
        public static final String T_CATEGORY_COLOR = TABLE_NAME + "." + CATEGORY_COLOR;
    }

    public static class Table3 implements BaseColumns {
        public static final String TABLE_NAME = "grupos";

        public static final String GROUP_NAME = "ngrupo";
        public static final String GROUP_TYPE = "tipo";

        public static final String T_ID = TABLE_NAME + "." + _ID;
        public static final String T_GROUP_NAME = TABLE_NAME + "." + GROUP_NAME;
        public static final String T_GROUP_TYPE = TABLE_NAME + "." + GROUP_TYPE;

        public static final int TYPE_MONTH = 0;
        public static final int TYPE_TOTAL = 1;
    }

    public static class Table4 implements BaseColumns {
        public static final String TABLE_NAME = "metas";

        public static final String ID_GROUP = "idgrupo";
        public static final String ID_CATEGORY = "idcat";
        public static final String AMOUNT = "valor";
        public static final String ALERT = "alerta";

        public static final String T_ID = TABLE_NAME + "." + _ID;
        public static final String T_ID_GROUP = TABLE_NAME + "." + ID_GROUP;
        public static final String T_ID_CATEGORY = TABLE_NAME + "." + ID_CATEGORY;
        public static final String T_AMOUNT = TABLE_NAME + "." + AMOUNT;
        public static final String T_ALERT = TABLE_NAME + "." + ALERT;
    }

    public static class Table5 implements BaseColumns {
        public static final String TABLE_NAME = "config";

        public static final String TAG = "tag";
        public static final String VALUE = "valor";

        public static final String T_ID = TABLE_NAME + "." + _ID;
        public static final String T_TAG = TABLE_NAME + "." + TAG;
        public static final String T_VALUE = TABLE_NAME + "." + VALUE;
    }
}