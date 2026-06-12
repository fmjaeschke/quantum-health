package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import org.dbunit.dataset.ITable;
import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.DataType;
import org.dbunit.dataset.datatype.DataTypeException;
import org.dbunit.dataset.datatype.TypeCastException;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@SuppressWarnings("unused")
public class CustomPostgresqlDataTypeFactory extends PostgresqlDataTypeFactory {

    @Override
    public DataType createDataType(int sqlType, String sqlTypeName) throws DataTypeException {
        if ("jsonb".equals(sqlTypeName)) {
            return new AbstractDataType("jsonb", Types.OTHER, String.class, false) {
                @Override
                public Object typeCast(Object value) {
                    if (value == null || value == ITable.NO_VALUE) {
                        return null;
                    }
                    return value.toString();
                }

                @Override
                public void setSqlValue(Object value, int column, PreparedStatement ps)
                        throws SQLException, TypeCastException {
                    if (value == null || value == ITable.NO_VALUE) {
                        ps.setNull(column, Types.OTHER);
                        return;
                    }
                    var pgObject = new PGobject();
                    pgObject.setType("jsonb");
                    pgObject.setValue(value.toString());
                    ps.setObject(column, pgObject);
                }

                @Override
                public Object getSqlValue(int column, ResultSet rs) throws SQLException {
                    String value = rs.getString(column);
                    return (value == null || rs.wasNull()) ? null : value;
                }
            };
        }
        if ("timestamptz".equals(sqlTypeName)) {
            return new AbstractDataType("timestamptz", Types.OTHER, OffsetDateTime.class, false) {

                @Override
                public Object typeCast(Object value) throws TypeCastException {
                    if (value == null || value == ITable.NO_VALUE) {
                        return null;
                    }
                    String strValue = value.toString();
                    try {
                        return OffsetDateTime.parse(strValue);
                    } catch (DateTimeParseException _) {
                        // fallback for values without offset — assume UTC
                        try {
                            return LocalDateTime.parse(strValue).atOffset(ZoneOffset.UTC);
                        } catch (DateTimeParseException ex) {
                            throw new TypeCastException("Cannot cast value to OffsetDateTime: " + strValue, ex);
                        }
                    }
                }

                @Override
                public void setSqlValue(Object value, int column, PreparedStatement ps)
                        throws SQLException, TypeCastException {
                    if (value == null || value == ITable.NO_VALUE) {
                        ps.setNull(column, Types.OTHER);
                        return;
                    }
                    ps.setObject(column, typeCast(value));
                }

                @Override
                public Object getSqlValue(int column, ResultSet rs)
                        throws SQLException {
                    Object value = rs.getObject(column, OffsetDateTime.class);
                    if (value == null || rs.wasNull()) {
                        return null;
                    }
                    return value;
                }
            };
        }
        return super.createDataType(sqlType, sqlTypeName);
    }
}
