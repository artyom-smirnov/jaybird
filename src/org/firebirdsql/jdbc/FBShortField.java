/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Contributor(s): David Jencks, Roman Rokytskyy
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Lesser General Public License Version 2.1 or later
 * (the "LGPL"), in which case the provisions of the LGPL are applicable
 * instead of those above.  If you wish to allow use of your
 * version of this file only under the terms of the LGPL and not to
 * allow others to use your version of this file under the MPL,
 * indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by
 * the LGPL.  If you do not delete the provisions above, a recipient
 * may use your version of this file under either the MPL or the
 * LGPL.
 */

package org.firebirdsql.jdbc;

import org.firebirdsql.gds.XSQLVAR;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * This class represents a field of type SHORT and performs all necessary
 * conversions.
 */
class FBShortField extends FBField {

    FBShortField(XSQLVAR field) throws SQLException {
        super(field);
    }


    byte getByte() throws SQLException {
        if (isNull()) return BYTE_NULL_VALUE;

        Short value = (Short)field.sqldata;

        // check if value is withing bounds
        if (value.shortValue() > MAX_BYTE_VALUE ||
            value.shortValue() < MIN_BYTE_VALUE)
                throw (SQLException)createException(
                    BYTE_CONVERSION_ERROR).fillInStackTrace();

        return value.byteValue();
    }
    short getShort() throws SQLException {
        if (isNull()) return SHORT_NULL_VALUE;

        return ((Short)field.sqldata).shortValue();
    }
    int getInt() throws SQLException {
        if (isNull()) return INT_NULL_VALUE;

        return ((Short)field.sqldata).intValue();
    }
    long getLong() throws SQLException {
        if (isNull()) return LONG_NULL_VALUE;

        return ((Short)field.sqldata).longValue();
    }
    float getFloat() throws SQLException {
        if (isNull()) return FLOAT_NULL_VALUE;

        return ((Short)field.sqldata).floatValue();
    }
    double getDouble() throws SQLException {
        if (isNull()) return DOUBLE_NULL_VALUE;

        return ((Short)field.sqldata).doubleValue();
    }
    BigDecimal getBigDecimal() throws SQLException {
        if (isNull()) return BIGDECIMAL_NULL_VALUE;

        return new java.math.BigDecimal(((Short)field.sqldata).doubleValue());
    }
    Object getObject() throws SQLException {
        if (isNull()) return OBJECT_NULL_VALUE;

        return field.sqldata;
    }
    boolean getBoolean() throws java.sql.SQLException {
        if (isNull()) return BOOLEAN_NULL_VALUE;

        return ((Short)field.sqldata).intValue() == 1;
    }
    String getString() throws SQLException {
        if (isNull()) return STRING_NULL_VALUE;

        return ((Short)field.sqldata).toString();
    }

    //--- setXXX methods

    void setString(String value) throws java.sql.SQLException {
        if (value == null) {
            setNull(true);
            return;
        }

        try {
            setShort(Short.parseShort(value));
        } catch(NumberFormatException nfex) {
            throw (SQLException)createException(
                SHORT_CONVERSION_ERROR).fillInStackTrace();
        }
    }
    void setShort(short value) throws java.sql.SQLException {
        field.sqldata = new Short((short)value);
        setNull(false);
    }
    void setBoolean(boolean value) throws java.sql.SQLException {
        setShort((short)(value ? 1 : 0));
    }
    void setFloat(float value) throws java.sql.SQLException {
        // check if value is within bounds
        if (value > MAX_SHORT_VALUE ||
            value < MIN_SHORT_VALUE)
                throw (SQLException)createException(
                    LONG_CONVERSION_ERROR).fillInStackTrace();

        setShort((short)value);
    }
    void setDouble(double value) throws java.sql.SQLException {
        // check if value is within bounds
        if (value > MAX_SHORT_VALUE ||
            value < MIN_SHORT_VALUE)
                throw (SQLException)createException(
                    DOUBLE_CONVERSION_ERROR).fillInStackTrace();

        setShort((short)value);
    }
    void setLong(long value) throws java.sql.SQLException {
        // check if value is within bounds
        if (value > MAX_SHORT_VALUE ||
            value < MIN_SHORT_VALUE)
                throw (SQLException)createException(
                    LONG_CONVERSION_ERROR).fillInStackTrace();

        setShort((short)value);
    }
    void setInteger(int value) throws java.sql.SQLException {
        // check if value is within bounds
        if (value > MAX_SHORT_VALUE ||
            value < MIN_SHORT_VALUE)
                throw (SQLException)createException(
                    LONG_CONVERSION_ERROR).fillInStackTrace();

        setShort((short)value);
    }
    void setByte(byte value) throws java.sql.SQLException {
        setShort((short)value);
    }
    void setBigDecimal(BigDecimal value) throws SQLException {
        if (value == null) {
            setNull(true);
            return;
        }

        // check if value is within bounds
        if (value.compareTo(new BigDecimal(MAX_SHORT_VALUE)) > 0 ||
            value.compareTo(new BigDecimal(MIN_SHORT_VALUE)) < 0)
                throw (SQLException)createException(
                    BIGDECIMAL_CONVERSION_ERROR).fillInStackTrace();

        setShort(value.shortValue());
    }

}