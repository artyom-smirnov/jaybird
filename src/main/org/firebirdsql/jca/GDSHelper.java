/*
 * Firebird Open Source J2ee connector - jdbc driver
 *
 * Distributable under LGPL license.
 * You may obtain a copy of the License at http://www.gnu.org/copyleft/lgpl.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * LGPL License for more details.
 *
 * This file was created by members of the firebird development team.
 * All individual contributions remain the Copyright (C) of those
 * individuals.  Contributors to this file are either listed here or
 * can be obtained from a CVS history command.
 *
 * All rights reserved.
 */

package org.firebirdsql.jca;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.resource.ResourceException;

import org.firebirdsql.encodings.Encoding;
import org.firebirdsql.encodings.EncodingFactory;
import org.firebirdsql.gds.BlobParameterBuffer;
import org.firebirdsql.gds.DatabaseParameterBuffer;
import org.firebirdsql.gds.GDS;
import org.firebirdsql.gds.GDSException;
import org.firebirdsql.gds.ISCConstants;
import org.firebirdsql.gds.XSQLDA;
import org.firebirdsql.gds.isc_blob_handle;
import org.firebirdsql.gds.isc_db_handle;
import org.firebirdsql.gds.isc_stmt_handle;
import org.firebirdsql.gds.isc_tr_handle;
import org.firebirdsql.jdbc.FBConnectionDefaults;
import org.firebirdsql.logging.Logger;
import org.firebirdsql.logging.LoggerFactory;


/**
 * Helper class for all GDS-related operations.
 */
public class GDSHelper {

    private final Logger log = LoggerFactory.getLogger(getClass(), false);

    private GDS gds;
    private isc_db_handle currentDbHandle;
    private isc_tr_handle currentTr;
    private int timeout = 0;
    /**
     * Needed from mcf when killing a db handle when a new tx cannot be started.
     */
    protected DatabaseParameterBuffer dpb;
    public boolean autoCommit = true;
    private final FBTpb tpb;
    
    /**
     * 
     */
    public GDSHelper(GDS gds, DatabaseParameterBuffer dpb, FBTpb tpb, isc_db_handle dbHandle) {
        super();
        this.gds = gds;
        this.dpb = dpb;
        this.tpb = tpb;
        this.currentDbHandle = dbHandle;
    }

    public isc_tr_handle getCurrentTrHandle() {
        return currentTr;
    }
    
    public void setCurrentTrHandle(isc_tr_handle currentTr) {
        this.currentTr = currentTr;
    }
    
    public isc_db_handle getCurrentDbHandle() {
        return currentDbHandle;
    }
    
    // FB public methods. Could be package if packages reorganized.
    
    /**
     * Retrieve a newly allocated statment handle with the current connection.
     * 
     * @return The new statement handle
     * @throws GDSException
     *             if a database access error occurs
     */
    public isc_stmt_handle getAllocatedStatement() throws GDSException {
        // Should we test for dbhandle?
        if (currentTr == null) { throw new GDSException(
                "No transaction started for allocate statement"); }
        isc_stmt_handle stmt = gds.get_new_isc_stmt_handle();
        try {
            gds.isc_dsql_allocate_statement(currentTr.getDbHandle(), stmt);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
        return stmt;
    }

    /**
     * Retrieve whether this connection is currently involved in a transaction
     * 
     * @return <code>true</code> if this connection is currently in a
     *         transaction, <code>false</code> otherwise.
     */
    public boolean inTransaction() {
        return currentTr != null;
    }

    /**
     * Prepare an SQL string for execution (within the database server) in the
     * context of a statement handle.
     * 
     * @param stmt
     *            The statement handle within which the SQL statement will be
     *            prepared
     * @param sql
     *            The SQL statement to be prepared
     * @param describeBind
     *            Send bind data to the database server
     * @throws GDSException
     *             if a Firebird-specific error occurs
     * @throws SQLException
     *             if a database access error occurs
     */
    public void prepareSQL(isc_stmt_handle stmt, String sql,
            boolean describeBind) throws GDSException, SQLException {
        if (log != null) log.debug("preparing sql: " + sql);
        // Should we test for dbhandle?
    
        String localEncoding = dpb.getArgumentAsString(ISCConstants.isc_dpb_local_encoding);
        String mappingPath = dpb.getArgumentAsString(ISCConstants.isc_dpb_mapping_path);
    
        Encoding encoding = EncodingFactory.getEncoding(localEncoding,
            mappingPath);
    
        int dialect = ISCConstants.SQL_DIALECT_CURRENT;
        if (dpb.hasArgument(ISCConstants.isc_dpb_sql_dialect))
            dialect = dpb.getArgumentAsInt(ISCConstants.isc_dpb_sql_dialect);
    
        try {
            XSQLDA out = gds.isc_dsql_prepare(currentTr, stmt, encoding
                    .encodeToCharset(sql), dialect);
            if (out.sqld != out.sqln) { throw new GDSException(
                    "Not all columns returned"); }
            if (describeBind) {
                gds.isc_dsql_describe_bind(stmt,
                    ISCConstants.SQLDA_VERSION1);
            }
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    }

    /**
     * Execute a statement in the database.
     * 
     * @param stmt
     *            The handle to the statement to be executed
     * @param sendOutSqlda
     *            Determines if the XSQLDA structure should be sent to the
     *            database
     * @throws GDSException
     *             if a Firebird-specific error occurs
     */
    public void executeStatement(isc_stmt_handle stmt, boolean sendOutSqlda)
            throws GDSException {
        try {
            gds.isc_dsql_execute2(currentTr, stmt,
                ISCConstants.SQLDA_VERSION1, stmt.getInSqlda(),
                (sendOutSqlda) ? stmt.getOutSqlda() : null);
    
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    }

    /**
     * Execute a SQL statement directly with the current connection.
     * 
     * @param statement
     *            The SQL statement to execute
     * @throws GDSException
     *             if a Firebird-specific error occurs
     */
    public void executeImmediate(String statement) throws GDSException {
        try {
            gds.isc_dsql_exec_immed2(getIscDBHandle(), currentTr,
                statement, 3, null, null);
        } catch (GDSException ex) {
            throw ex;
        }
    }

    /**
     * Fetch data from a statement in the database.
     * 
     * @param stmt
     *            handle to the statement from which data will be fetched
     * @param fetchSize
     *            The number of records to fetch
     * @throws GDSException
     *             if a Firebird-specific error occurs
     */
    public void fetch(isc_stmt_handle stmt, int fetchSize) throws GDSException {
        try {
            gds.isc_dsql_fetch(stmt, ISCConstants.SQLDA_VERSION1, stmt
                    .getOutSqlda(), fetchSize);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Set the cursor name for a statement.
     * 
     * @param stmt
     *            handle to statement for which the cursor name will be set
     * @param cursorName
     *            the name for the cursor
     * @throws GDSException
     *             if a Firebird-specific database access error occurs
     */
    public void setCursorName(isc_stmt_handle stmt, String cursorName)
            throws GDSException {
        try {
            gds.isc_dsql_set_cursor_name(stmt, cursorName, 0); // type is
                                                                    // reserved
                                                                    // for
                                                                    // future
                                                                    // use
        } catch (GDSException ge) {
            throw ge;
        }
    }

    /**
     * Close a statement that is allocated in the database. The statement can be
     * optionally deallocated.
     * 
     * @param stmt
     *            handle to the statement to be closed
     * @param deallocate
     *            if <code>true</code>, the statement will be deallocated,
     *            otherwise it will not be deallocated
     * @throws GDSException
     *             if a Firebird-specific database access error occurs
     */
    public void closeStatement(isc_stmt_handle stmt, boolean deallocate)
            throws GDSException {
        try {
            gds.isc_dsql_free_statement(stmt,
                        (deallocate) ? ISCConstants.DSQL_drop
                                : ISCConstants.DSQL_close);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Register a statement with the current transaction. There must be a
     * currently-active transaction to complete this operation.
     * 
     * @param fbStatement
     *            handle to the statement to be registered
     */
    public void registerStatement(isc_stmt_handle fbStatement) {
        if (currentTr == null) { throw new java.lang.IllegalStateException(
                "registerStatement called with no transaction"); }
    
        currentTr.registerStatementWithTransaction(fbStatement);
    }

    /**
     * Open a handle to a new blob within the current transaction with the given
     * id.
     * 
     * @param blob_id
     *            The identifier to be given to the blob
     * @param segmented
     *            If <code>true</code>, the blob will be segmented, otherwise
     *            is will be streamed
     * @throws GDSException
     *             if a Firebird-specific database error occurs
     */
    public isc_blob_handle openBlobHandle(long blob_id, boolean segmented)
            throws GDSException {
        try {
            isc_blob_handle blob = gds.get_new_isc_blob_handle();
            blob.setBlob_id(blob_id);
    
            final BlobParameterBuffer blobParameterBuffer = gds
                    .newBlobParameterBuffer();
    
            blobParameterBuffer.addArgument(BlobParameterBuffer.type,
                segmented ? BlobParameterBuffer.type_segmented
                        : BlobParameterBuffer.type_stream);
    
            gds.isc_open_blob2(currentDbHandle, currentTr, blob,
                blobParameterBuffer);
    
            return blob;
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Create a new blob within the current transaction.
     * 
     * @param segmented
     *            If <code>true</code> the blob will be segmented, otherwise
     *            it will be streamed
     * @throws GDSException
     *             if a Firebird-specific database error occurs
     */
    public isc_blob_handle createBlobHandle(boolean segmented)
            throws GDSException {
        try {
            isc_blob_handle blob = gds.get_new_isc_blob_handle();
    
            final BlobParameterBuffer blobParameterBuffer = gds
                    .newBlobParameterBuffer();
    
            blobParameterBuffer.addArgument(BlobParameterBuffer.type,
                segmented ? BlobParameterBuffer.type_segmented
                        : BlobParameterBuffer.type_stream);
    
            gds.isc_create_blob2(currentDbHandle, currentTr, blob,
                blobParameterBuffer);
    
            return blob;
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Get a segment from a blob.
     * 
     * @param blob
     *            Handle to the blob from which the segment is to be fetched
     * @param len
     *            The maximum length to fetch
     * @throws GDSException
     *             if a Firebird-specific database access error occurs
     */
    public byte[] getBlobSegment(isc_blob_handle blob, int len)
            throws GDSException {
        try {
            return gds.isc_get_segment(blob, len);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Close a blob that has been opened within the database.
     * 
     * @param blob
     *            Handle to the blob to be closed
     * @throws GDSException
     *             if a Firebird-specific database access error occurs
     */
    public void closeBlob(isc_blob_handle blob) throws GDSException {
        try {
            gds.isc_close_blob(blob);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Write a segment of data to a blob.
     * 
     * @param blob
     *            handle to the blob to which data will be written
     * @param buf
     *            segment of data to be written to the blob
     * @throws GDSException
     *             if a Firebird-specific database access error occurs
     */
    public void putBlobSegment(isc_blob_handle blob, byte[] buf)
            throws GDSException {
        try {
            gds.isc_put_segment(blob, buf);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    /**
     * Fetch the count information for a statement handle. The count information
     * that is updated includes the counts for update, insert, delete and
     * select, and it is set in the handle itself.
     * 
     * @param stmt
     *            handle to the statement for which counts will be fetched
     * @throws GDSException
     *             if a Firebird-specific database access error occurs
     */
    public void getSqlCounts(isc_stmt_handle stmt) throws GDSException {
        try {
            gds.getSqlCounts(stmt);
        } catch (GDSException ge) {
            throw ge;
        } // end of catch
    
    }

    // for DatabaseMetaData.
    
    /**
     * Get the name of the database product that we're connected to.
     * 
     * @return The database product name (i.e. Firebird or Interbase)
     */
    public String getDatabaseProductName() {
        /** @todo add check if mc is not null */
        return currentDbHandle.getDatabaseProductName();
    }

    /**
     * Get the version of the the database that we're connected to
     * 
     * @return the database product version
     */
    public String getDatabaseProductVersion() {
        /** @todo add check if mc is not null */
        return currentDbHandle.getDatabaseProductVersion();
    }

    /**
     * Get the major version number of the database that we're connected to.
     * 
     * @return The major version number of the database
     */
    public int getDatabaseProductMajorVersion() {
        /** @todo add check if mc is not null */
        return currentDbHandle.getDatabaseProductMajorVersion();
    }

    /**
     * Get the minor version number of the database that we're connected to.
     * 
     * @return The minor version number of the database
     */
    public int getDatabaseProductMinorVersion() {
        /** @todo add check if mc is not null */
        return currentDbHandle.getDatabaseProductMinorVersion();
    }

    /**
     * Get the name of the database that we're connected to.
     * 
     * @return The name of the database involved in this connection
     */
//    public String getDatabase() {
//        return mcf.getDatabase();
//    }

    /**
     * Get the database login name of the user that we're connected as.
     * 
     * @return The username of the current database user
     */
    public String getUserName() {
        return dpb.getArgumentAsString(ISCConstants.isc_dpb_user);
    }

    /**
     * Get the transaction isolation level of this connection. The level is one
     * of the static final fields of <code>java.sql.Connection</code> (i.e.
     * <code>TRANSACTION_READ_COMMITTED</code>,
     * <code>TRANSACTION_READ_UNCOMMITTED</code>,
     * <code>TRANSACTION_REPEATABLE_READ</code>,
     * <code>TRANSACTION_SERIALIZABLE</code>.
     * 
     * @see java.sql.Connection
     * @see setTransactionIsolation
     * @return Value representing a transaction isolation level defined in
     *         {@link java.sql.Connection}.
     * @throws ResourceException
     *             If the transaction level cannot be retrieved
     */
    public int getTransactionIsolation() throws ResourceException {
        return tpb.getTransactionIsolation();
    }

    /**
     * Set the transaction level for this connection. The level is one of the
     * static final fields of <code>java.sql.Connection</code> (i.e.
     * <code>TRANSACTION_READ_COMMITTED</code>,
     * <code>TRANSACTION_READ_UNCOMMITTED</code>,
     * <code>TRANSACTION_REPEATABLE_READ</code>,
     * <code>TRANSACTION_SERIALIZABLE</code>.
     * 
     * @see java.sql.Connection
     * @see getTransactionIsolation
     * @param isolation
     *            Value representing a transaction isolation level defined in
     *            {@link java.sql.Connection}.
     * @throws ResourceException
     *             If the transaction level cannot be retrieved
     */
    public void setTransactionIsolation(int isolation) throws ResourceException {
        tpb.setTransactionIsolation(isolation);
    }

    /**
     * Get the name of the current transaction isolation level for this
     * connection.
     * 
     * @see getTransactionIsolation
     * @see setTransactionIsolationName
     * @return The name of the current transaction isolation level
     * @throws ResourceException
     *             If the transaction level cannot be retrieved
     */
    public String getTransactionIsolationName() throws ResourceException {
        return tpb.getTransactionIsolationName();
    }

    /**
     * Set the current transaction isolation level for this connection by name
     * of the level. The transaction isolation name must be one of the
     * TRANSACTION_* static final fields in {@link org.firebirdsql.jca.FBTpb}.
     * 
     * @see getTransactionIsolationName
     * @see FBTpb
     * @param isolation
     *            The name of the transaction isolation level to be set
     * @throws ResourceException
     *             if the transaction isolation level cannot be set to the
     *             requested level, or if <code>isolation</code> is not a
     *             valid transaction isolation name
     */
    public void setTransactionIsolationName(String isolation)
            throws ResourceException {
        tpb.setTransactionIsolationName(isolation);
    }

    /**
     * @deprecated you should not use internal transaction isolation levels
     *             directrly.
     */
    public int getIscTransactionIsolation() throws ResourceException {
        return tpb.getIscTransactionIsolation();
    }

    /**
     * @deprecated you should not use internal transaction isolation levels
     *             directrly.
     */
    public void setIscTransactionIsolation(int isolation)
            throws ResourceException {
        tpb.setIscTransactionIsolation(isolation);
    }

    /**
     * Set whether this connection is to be readonly
     * 
     * @param readOnly
     *            If <code>true</code>, the connection will be set read-only,
     *            otherwise it will be writable
     */
    public void setReadOnly(boolean readOnly) {
        tpb.setReadOnly(readOnly);
    }

    /**
     * Retrieve whether this connection is readonly.
     * 
     * @return <code>true</code> if this connection is readonly,
     *         <code>false</code> otherwise
     */
    public boolean isReadOnly() {
        return tpb.isReadOnly();
    }

    /**
     * Get the buffer length for blobs for this connection.
     * 
     * @return The length of blob buffers
     */
    public int getBlobBufferLength() {
        if (dpb.hasArgument(ISCConstants.isc_dpb_blob_buffer_size))
            return dpb.getArgumentAsInt(ISCConstants.isc_dpb_blob_buffer_size);
        else
            return FBConnectionDefaults.DEFAULT_BLOB_BUFFER_SIZE;
    }

    /**
     * Get the encoding used for this connection.
     * 
     * @return The name of the encoding used
     */
    public String getIscEncoding() {
        try {
            String result = dpb.getArgumentAsString(ISCConstants.isc_dpb_lc_ctype);
            if (result == null) result = "NONE";
            return result;
        } catch (NullPointerException ex) {
            return "NONE";
        }
    }

    /**
     * Get all warnings associated with current connection.
     * 
     * @return list of {@link GDSException}instances representing warnings for
     *         this database connection.
     */
    public List getWarnings() {
        if (currentDbHandle == null)
            return Collections.EMPTY_LIST;
        else
            return currentDbHandle.getWarnings();
    }

    /**
     * Clear warnings for this database connection.
     */
    public void clearWarnings() {
        if (currentDbHandle != null) currentDbHandle.clearWarnings();
    }

    /**
     * Get connection handle for direct Firebird API access
     * 
     * @return internal handle for connection
     */
    public isc_db_handle getIscDBHandle() throws GDSException {
        return currentDbHandle;
    }

    /**
     * Get Firebird API handler (sockets/native/embeded/etc)
     * 
     * @return handler object for internal API calls
     */
    public GDS getInternalAPIHandler() {
        return gds;
    }

}
