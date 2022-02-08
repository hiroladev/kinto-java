package de.hirola.kintojava;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A simple layer for a ResultSet to using with Android (Cursor) and JVM (ResultSet).
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public final class KintoQueryResultSet {

    private final ResultSet resultSet;

    /**
     * Create an object for using in jvm.
     *
     * @param resultSet to use in this library
     */
    public KintoQueryResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    /**
     * A layer to use result set on jvm and Android.
     *
     * @return A flag to determine, if their more results.
     * @throws SQLException if an error occurred
     */
    public boolean next() throws SQLException {
        if (resultSet != null) {
            return resultSet.next();
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The string value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public String getString(String columnLabel) throws SQLException {
        if (resultSet != null) {
            return resultSet.getString(columnLabel);
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The boolean value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public boolean getBoolean(String columnLabel) throws SQLException {
        if (resultSet != null) {
            return resultSet.getBoolean(columnLabel);
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The integer value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public int getInt(String columnLabel) throws SQLException {
        if (resultSet != null) {
            return resultSet.getInt(columnLabel);
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The long value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public long getLong(String columnLabel) throws SQLException {
        if (resultSet != null) {
            return resultSet.getLong(columnLabel);
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The double value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public double getDouble(String columnLabel) throws SQLException {
        if (resultSet != null) {
            return resultSet.getDouble(columnLabel);
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The float value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public float getFloat(String columnLabel) throws SQLException {
        if (resultSet != null) {
            return resultSet.getFloat(columnLabel);
        }
        throw new SQLException("ResultSet must not be null.");
    }

    /**
     * A layer to get the value for a given column name on jvm and Android.
     *
     * @param columnLabel name of the column
     * @return The date value of the row in column.
     * @throws SQLException if the result set or the cursor is null or
     *                      the column does not exist
     */
    public LocalDate getDate(String columnLabel) throws SQLException {
        // save LocalDate as text in iso format
        String isoLocalDateString;
        if (resultSet != null) {
            isoLocalDateString = resultSet.getString(columnLabel);
        } else {
            throw new SQLException("ResultSet must not be null.");
        }
        try {
            return LocalDate.parse(isoLocalDateString);
        } catch (DateTimeParseException exception) {
            throw new SQLException("The column "
                    + columnLabel
                    + " doesn't contain a validate format: "
                    + exception.getMessage());
        }
    }
}
