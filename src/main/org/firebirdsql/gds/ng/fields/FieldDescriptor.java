/*
 * Public Firebird Java API.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *    1. Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright 
 *       notice, this list of conditions and the following disclaimer in the 
 *       documentation and/or other materials provided with the distribution. 
 *    3. The name of the author may not be used to endorse or promote products 
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED 
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO 
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.firebirdsql.gds.ng.fields;

import org.firebirdsql.encodings.EncodingDefinition;
import org.firebirdsql.encodings.IEncodingFactory;
import org.firebirdsql.gds.ISCConstants;
import org.firebirdsql.gds.ng.DatatypeCoder;

import java.util.Objects;

import static org.firebirdsql.gds.ISCConstants.*;

/**
 * The class <code>FieldDescriptor</code> contains the column metadata of the XSQLVAR server
 * data structure used to describe one column for input or output.
 * <p>
 * FieldDescriptor is an immutable type, the value of a field is maintained separately in an instance of {@link
 * FieldValue}.
 * </p>
 *
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @version 3.0
 */
public final class FieldDescriptor {

    private final int position;
    private final DatatypeCoder datatypeCoder;
    private final int type;
    private final int subType;
    private final int scale;
    private final int length;
    private final String fieldName;
    private final String tableAlias;
    private final String originalName;
    private final String originalTableName;
    private final String ownerName;
    private int hash;

    /**
     * Constructor for metadata FieldDescriptor.
     *
     * @param position
     *         Position of this field (0-based), or {@code -1} if position is not known (eg for test code)
     * @param datatypeCoder
     *         Instance of DatatypeCoder to use when decoding column data (note that another instance may be derived
     *         internally, which then will be returned by {@link #getDatatypeCoder()})
     * @param type
     *         Column SQL type
     * @param subType
     *         Column subtype
     * @param scale
     *         Column scale
     * @param length
     *         Column defined length
     * @param fieldName
     *         Column alias name
     * @param tableAlias
     *         Column table alias
     * @param originalName
     *         Column original name
     * @param originalTableName
     *         Column original table
     * @param ownerName
     *         Owner of the column/table
     */
    public FieldDescriptor(final int position, final DatatypeCoder datatypeCoder,
            final int type, final int subType,
            final int scale, int length,
            final String fieldName, final String tableAlias, 
            final String originalName, final String originalTableName,
            final String ownerName) {
        assert datatypeCoder != null : "dataTypeCoder should not be null";
        this.position = position;
        this.datatypeCoder = datatypeCoderForType(datatypeCoder, type, subType, scale);
        this.type = type;
        this.subType = subType;
        this.scale = scale;
        this.length = length;
        this.fieldName = fieldName;
        // Assign null if table alias is empty string
        // TODO May want to do the reverse, or handle this better; see FirebirdResultSetMetaData contract
        this.tableAlias = tableAlias == null || !tableAlias.isEmpty() ? tableAlias : null;
        this.originalName = originalName;
        this.originalTableName = originalTableName;
        this.ownerName = ownerName;
    }

    /**
     * @return The 0-based position of this field (or {@code -1})
     */
    public int getPosition() {
        return position;
    }

    /**
     * @return The {@link org.firebirdsql.gds.ng.DatatypeCoder} to use when decoding field data.
     */
    public DatatypeCoder getDatatypeCoder() {
        return datatypeCoder;
    }

    /**
     * @return The {@link org.firebirdsql.encodings.IEncodingFactory} for the associated connection.
     */
    public IEncodingFactory getEncodingFactory() {
        return datatypeCoder.getEncodingFactory();
    }

    /**
     * @return The Firebird type of this field
     */
    public int getType() {
        return type;
    }

    /**
     * @return The Firebird subtype of this field
     */
    public int getSubType() {
        return subType;
    }

    /**
     * @return The scale of this field
     */
    public int getScale() {
        return scale;
    }

    /**
     * @return The declared (maximum) length of this field
     */
    public int getLength() {
        return length;
    }

    /**
     * @return The (aliased) field name
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * @return The (aliased) table name
     */
    public String getTableAlias() {
        return tableAlias;
    }

    /**
     * @return The original name of the field (eg the column name in the table)
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * @return The original table name
     */
    public String getOriginalTableName() {
        return originalTableName;
    }

    /**
     * @return The owner
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * @return {@code true} if the type is variable length (ie {@link org.firebirdsql.gds.ISCConstants#SQL_VARYING}).
     */
    public boolean isVarying() {
        return isFbType(SQL_VARYING);
    }

    /**
     * Check if the type of this field is the specified Firebird data type.
     * <p>
     * This method assumes the not-nullable data type is passed, on checking the nullable bit of {@link #getType()} is
     * set to {@code 0}.
     * </p>
     *
     * @param fbType
     *         One of the {@code SQL_} data type identifier values
     * @return {@code true} if the type is the same as the type of this field
     */
    public boolean isFbType(int fbType) {
        return (type & ~1) == fbType;
    }

    /**
     * @return {@code true} if this field is nullable.
     */
    public boolean isNullable() {
        return (type & 1) == 1;
    }

    /**
     * Determines if this is a db-key (RDB$DB_KEY) of a table.
     * <p>
     * NOTE: Technically it could also be a normal {@code CHAR CHARACTER SET OCTETS} column called {@code DB_KEY}.
     * </p>
     *
     * @return {@code true} if the field is a RDB$DB_KEY
     * @since 4.0
     */
    public boolean isDbKey() {
        return "DB_KEY".equals(originalName)
                && isFbType(SQL_TEXT)
                && (subType & 0xFF) == CS_BINARY;
    }

    /**
     * The length in characters of this field.
     * <p>
     * This takes into account the max bytes per character of the character set.
     * </p>
     *
     * @return Character length, or {@code -1} for non-character types (including blobs)
     */
    public int getCharacterLength() {
        switch (type & ~1) {
        case SQL_TEXT:
        case SQL_VARYING:
            return length / getDatatypeCoder().getEncodingDefinition().getMaxBytesPerChar();
        default:
            return -1;
        }
    }

    /**
     * Creates a default, uninitialized {@link FieldValue}
     *
     * @return A new {@link FieldValue}
     */
    public FieldValue createDefaultFieldValue() {
        return new FieldValue();
    }

    /**
     * Returns a type-specific coder for this datatype.
     * <p>
     * Primary intent is to handle character set conversion for char, varchar and blob sub_type text.
     * </p>
     *
     * @param datatypeCoder
     *         Datatype coder to use for obtaining the type-specific variant
     * @param type
     *         Firebird type code
     * @param subType
     *         Firebird sub type code
     * @param scale
     *         Scale
     * @return type-specific datatype coder
     */
    private static DatatypeCoder datatypeCoderForType(DatatypeCoder datatypeCoder, int type, int subType, int scale) {
        int characterSetId = getCharacterSetId(type, subType, scale);
        EncodingDefinition encodingDefinition = datatypeCoder.getEncodingFactory()
                .getEncodingDefinitionByCharacterSetId(characterSetId);
        return datatypeCoder.forEncodingDefinition(encodingDefinition);
    }

    /**
     * Determines the character set id (without collation id) for a combination of type, sub type and scale.
     *
     * @param type
     *         Firebird type code
     * @param subType
     *         Firebird sub type code
     * @param scale
     *         Firebird scale
     * @return Character set id for the type, if the type has no character set, than {@link ISCConstants#CS_dynamic}
     * is returned
     */
    private static int getCharacterSetId(int type, int subType, int scale) {
        switch (type & ~1) {
        case SQL_TEXT:
        case SQL_VARYING:
            return subType & 0xFF;
        case SQL_BLOB:
            if (subType == BLOB_SUB_TYPE_TEXT) {
                return scale & 0xFF;
            }
            // Assume binary/octets (instead of NONE)
            return CS_BINARY;
        default:
            // Technically not a character type, but assume connection character set
            return CS_dynamic;
        }
    }

    /**
     * Limited equals that only checks if the data type in the provided field descriptor is the same as this descriptor.
     * <p>
     * The fields checked are:
     * <ul>
     * <li>type</li>
     * <li>subType</li>
     * <li>scale</li>
     * <li>length</li>
     * </ul>
     * </p>
     *
     * @param other
     *         Field descriptor to check
     * @return <code>true</code> when <code>other</code> is not null and has the same type definition as this instance,
     * <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean typeEquals(final FieldDescriptor other) {
        return this == other
                || other != null
                && this.type == other.type
                && this.subType == other.subType
                && this.scale == other.scale
                && this.length == other.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendFieldDescriptor(sb);
        return sb.toString();
    }

    void appendFieldDescriptor(final StringBuilder sb) {
        sb.append("FieldDescriptor:[")
                .append("Position=").append(position)
                .append(",FieldName=").append(fieldName)
                .append(",TableAlias=").append(tableAlias)
                .append(",Type=").append(type)
                .append(",SubType=").append(subType)
                .append(",Scale=").append(scale)
                .append(",Length=").append(length)
                .append(",OriginalName=").append(originalName)
                .append(",OriginalTableName=").append(originalTableName)
                .append(",OwnerName=").append(ownerName)
                .append(']');
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FieldDescriptor)) return false;
        FieldDescriptor other = (FieldDescriptor) obj;
        return this.position == other.position
                && this.type == other.type
                && this.subType == other.subType
                && this.scale == other.scale
                && this.length == other.length
                && Objects.equals(this.fieldName, other.fieldName)
                && Objects.equals(this.tableAlias, other.tableAlias)
                && Objects.equals(this.originalName, other.originalName)
                && Objects.equals(this.originalTableName, other.originalTableName)
                && Objects.equals(this.ownerName, other.ownerName)
                && this.datatypeCoder.equals(other.datatypeCoder);
    }

    @Override
    public int hashCode() {
        // Depend on immutability to cache hashCode
        if (hash == 0) {
            hash = Objects.hash(position, type, subType, scale, length, fieldName, tableAlias, originalName,
                    originalTableName, ownerName);
        }
        return hash;
    }

}
