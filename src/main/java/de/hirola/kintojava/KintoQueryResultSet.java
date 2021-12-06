package de.hirola.kintojava;

import android.database.Cursor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A simple ResultSet to using with Android (Cursor) and JVM (ResultSet).
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public final class KintoQueryResultSet {

    private final ResultSet resultSet;
    private final Cursor cursor;
    private final boolean isRunningOnAndroid;

    public KintoQueryResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
        cursor = null;
        isRunningOnAndroid = false;
    }

    public KintoQueryResultSet(Cursor cursor) {
        this.cursor = cursor;
        resultSet = null;
        isRunningOnAndroid = true;
    }

    public boolean next() throws SQLException {
        if (isRunningOnAndroid) {
            if (cursor != null) {
                return cursor.moveToNext();
            }
            throw new SQLException("Cursor must not be null.");
        } else {
            if (resultSet != null) {
                return resultSet.next();
            }
            throw new SQLException("ResultSet must not be null.");
        }
    }

    public String getString(String columnLabel) throws SQLException {
        if (isRunningOnAndroid) {
            if (cursor != null) {
                return cursor.getString(getColumnIndex(columnLabel));
            }
            throw new SQLException("Cursor must not be null.");
        } else {
            if (resultSet != null) {
                return resultSet.getString(columnLabel);
            }
            throw new SQLException("ResultSet must not be null.");
        }
    }

    public boolean getBoolean(String columnLabel) throws SQLException {
        if (isRunningOnAndroid) {
            if (cursor != null) {
                int value = cursor.getInt(getColumnIndex(columnLabel));
                switch (value) {
                    case 0: return false;
                    case 1: return true;
                    default: throw new SQLException("The column "
                            + columnLabel
                            + " doesn't contain a boolean value.");
                }
            }
            throw new SQLException("Cursor must not be null.");
        } else {
            if (resultSet != null) {
                return resultSet.getBoolean(columnLabel);
            }
            throw new SQLException("ResultSet must not be null.");
        }
    }

    public int getInt(String columnLabel) throws SQLException {
        if (isRunningOnAndroid) {
            if (cursor != null) {
                return cursor.getInt(getColumnIndex(columnLabel));
            }
            throw new SQLException("Cursor must not be null.");
        } else {
            if (resultSet != null) {
                return resultSet.getInt(columnLabel);
            }
            throw new SQLException("ResultSet must not be null.");
        }
    }

    public double getDouble(String columnLabel) throws SQLException {
        if (isRunningOnAndroid) {
            if (cursor != null) {
                return cursor.getDouble(getColumnIndex(columnLabel));
            }
            throw new SQLException("Cursor must not be null.");
        } else {
            if (resultSet != null) {
                return resultSet.getDouble(columnLabel);
            }
            throw new SQLException("ResultSet must not be null.");
        }
    }

    public float getFloat(String columnLabel) throws SQLException {
        if (isRunningOnAndroid) {
            if (cursor != null) {
                return cursor.getFloat(getColumnIndex(columnLabel));
            }
            throw new SQLException("Cursor must not be null.");
        } else {
            if (resultSet != null) {
                return resultSet.getFloat(columnLabel);
            }
            throw new SQLException("ResultSet must not be null.");
        }
    }

    public LocalDate getDate(String columnLabel) throws SQLException {
        // save LocalDate as text in iso format
        String isoLocalDateString;
        if (isRunningOnAndroid) {
            if (cursor != null) {
                isoLocalDateString = cursor.getString(getColumnIndex(columnLabel));
            } else {
                throw new SQLException("Cursor must not be null.");
            }
        } else {
            if (resultSet != null) {
                isoLocalDateString = resultSet.getString(columnLabel);
            } else {
                throw new SQLException("ResultSet must not be null.");
            }
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

    private int getColumnIndex(String columnLabel) throws SQLException {
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndex(columnLabel);
            if (columnIndex == -1) {
                // column doesn't exist
                throw new SQLException("The column with label "
                        + columnLabel
                        + " doesn't exist.");
            }
            return columnIndex;
        }
        throw new SQLException("Cursor must not be null.");
    }
}
