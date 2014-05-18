/*
 * $Id$
 *
 * Firebird Open Source JavaEE Connector - JDBC Driver
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
 * can be obtained from a source control history command.
 *
 * All rights reserved.
 */
package org.firebirdsql.jdbc;

import org.firebirdsql.common.FBJUnit4TestBase;
import org.firebirdsql.common.TestDataGeneration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.*;

import static org.firebirdsql.common.DdlHelper.executeCreateTable;
import static org.firebirdsql.common.FBTestProperties.getConnectionViaDriverManager;
import static org.firebirdsql.common.JdbcResourceHelper.closeQuietly;
import static org.junit.Assert.*;

/**
 * Tests for {@link org.firebirdsql.jdbc.FBBlobInputStream}.
 *
 * @author <a href="mailto:mrotteveel@users.sourceforge.net">Mark Rotteveel</a>
 * @since 3.0
 */
public class TestFBBlobInputStream extends FBJUnit4TestBase {

    private static final String CREATE_TABLE =
            "CREATE TABLE test_blob(" +
            "  id INTEGER, " +
            "  bin_data BLOB " +
            ")";

    private static final String INSERT_BLOB = "INSERT INTO test_blob(id, bin_data) VALUES (?, ?)";

    private static final String SELECT_BLOB = "SELECT bin_data FROM test_blob WHERE id = ?";

    private Connection connection;

    @Before
    public void setUp() throws SQLException {
        connection = getConnectionViaDriverManager();
        executeCreateTable(connection, CREATE_TABLE);
    }

    @After
    public void tearDown() {
        closeQuietly(connection);
    }

    @Test
    public void testGetOwner() throws Exception {
        populateBlob(1, new byte[] { 1, 2, 3, 4, 5 });

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                assertSame("FBBlobInputStream.getBlob() should return owning blob", blob, is.getBlob());
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    @Test
    public void testAvailable_noReads_returns0() throws Exception {
        populateBlob(1, new byte[] { 1, 2, 3, 4, 5 });

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                assertEquals("Available() without initial read should return 0", 0, is.available());
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    @Test
    public void testAvailable_singleRead_returnsRemaining() throws Exception {
        final byte[] bytes = { 1, 2, 3, 4, 5 };
        populateBlob(1, bytes);

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                assertEquals("Expected first blob value of 1", 1, is.read());

                assertEquals("Available() after initial read should return remaining length",
                        bytes.length - 1, is.available());
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    @Test
     public void testAvailable_fullyRead_returns0() throws Exception {
        final byte[] bytes = { 1, 2, 3, 4, 5 };
        populateBlob(1, bytes);

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                byte[] buffer = new byte[bytes.length];
                is.readFully(buffer);

                assertArrayEquals(bytes, buffer);

                assertEquals("Available() after readFully() should return 0", 0, is.available());
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    @Test
    public void testAvailable_singleReadClosed_returns0() throws Exception {
        final byte[] bytes = { 1, 2, 3, 4, 5 };
        populateBlob(1, bytes);

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                assertEquals("Expected first blob value of 1", 1, is.read());
                is.close();

                assertEquals("Available() after close() should return 0", 0, is.available());
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    @Test
    public void testRead_byteArr_moreThanAvailable_returnsAvailable() throws Exception {
        final byte[] bytes = TestDataGeneration.createRandomBytes(128 * 1024);
        populateBlob(1, bytes);

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                assertEquals("Unexpected first byte", bytes[0] & 0xFF, is.read());
                final int available = is.available();
                assertTrue("Value of available() should be larger than 0", available > 0);
                assertTrue("Value of available() should be smaller than 128 * 1024 - 1", available < 128 * 1024 - 1);

                byte[] buffer = new byte[128 * 1024];
                int bytesRead = is.read(buffer, 1, 128 * 1024 - 1);

                assertEquals("Expected to read the number of bytes previously returned by available",
                        available, bytesRead);
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    @Test
    public void testRead_byteArr_moreThanAvailable_returnsAllRead() throws Exception {
        final byte[] bytes = TestDataGeneration.createRandomBytes(128 * 1024);
        populateBlob(1, bytes);

        PreparedStatement pstmt = connection.prepareStatement(SELECT_BLOB);
        try {
            pstmt.setInt(1, 1);
            ResultSet rs = pstmt.executeQuery();
            try {
                assertTrue("Expected a row", rs.next());
                Blob blob = rs.getBlob(1);
                FBBlobInputStream is = (FBBlobInputStream) blob.getBinaryStream();

                byte[] buffer = new byte[128 * 1024];
                int firstValue = is.read();
                assertEquals("Unexpected first byte", bytes[0] & 0xFF, firstValue);
                buffer[0] = (byte) firstValue;

                final int available = is.available();
                assertTrue("Value of available() should be smaller than 128 * 1024 - 1", available < 128 * 1024 - 1);

                is.readFully(buffer, 1, 128 * 1024 - 1);

                assertArrayEquals("Full blob should have been read", bytes, buffer);
            } finally {
                rs.close();
            }
        } finally {
            pstmt.close();
        }
    }

    private void populateBlob(int id, byte[] bytes) throws SQLException {
        PreparedStatement insert = connection.prepareStatement(INSERT_BLOB);
        try {
            insert.setInt(1, id);
            insert.setBytes(2, bytes);
            insert.executeUpdate();
        } finally {
            closeQuietly(insert);
        }
    }
}
