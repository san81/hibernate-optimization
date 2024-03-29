// $Id: JDBCExceptionHelper.java 9557 2006-03-06 15:16:27Z steve.ebersole@jboss.com $
package org.hibernate.exception;

import org.hibernate.JDBCException;
import org.hibernate.util.JDBCExceptionReporter;

import java.sql.SQLException;

/**
 * Implementation of JDBCExceptionHelper.
 *
 * @author Steve Ebersole
 */
public final class JDBCExceptionHelper {

	private JDBCExceptionHelper() {
	}

	/**
	 * Converts the given SQLException into Hibernate's JDBCException hierarchy, as well as performing
	 * appropriate logging.
	 *
	 * @param converter    The converter to use.
	 * @param sqlException The exception to convert.
	 * @param message      An optional error message.
	 * @return The converted JDBCException.
	 */
	public static JDBCException convert(SQLExceptionConverter converter, SQLException sqlException, String message) {
		return convert( converter, sqlException, message, "???" );
	}

	/**
	 * Converts the given SQLException into Hibernate's JDBCException hierarchy, as well as performing
	 * appropriate logging.
	 *
	 * @param converter    The converter to use.
	 * @param sqlException The exception to convert.
	 * @param message      An optional error message.
	 * @return The converted JDBCException.
	 */
	public static JDBCException convert(SQLExceptionConverter converter, SQLException sqlException, String message, String sql) {
		JDBCExceptionReporter.logExceptions( sqlException, message + " [" + sql + "]" );
		return converter.convert( sqlException, message, sql );
	}

	/**
	 * For the given SQLException, locates the vendor-specific error code.
	 *
	 * @param sqlException The exception from which to extract the SQLState
	 * @return The error code.
	 */
	public static int extractErrorCode(SQLException sqlException) {
		int errorCode = sqlException.getErrorCode();
		SQLException nested = sqlException.getNextException();
		while ( errorCode == 0 && nested != null ) {
			errorCode = nested.getErrorCode();
			nested = nested.getNextException();
		}
		return errorCode;
	}

	/**
	 * For the given SQLException, locates the X/Open-compliant SQLState.
	 *
	 * @param sqlException The exception from which to extract the SQLState
	 * @return The SQLState code, or null.
	 */
	public static String extractSqlState(SQLException sqlException) {
		String sqlState = sqlException.getSQLState();
		SQLException nested = sqlException.getNextException();
		while ( sqlState == null && nested != null ) {
			sqlState = nested.getSQLState();
			nested = nested.getNextException();
		}
		return sqlState;
	}

	/**
	 * For the given SQLException, locates the X/Open-compliant SQLState's class code.
	 *
	 * @param sqlException The exception from which to extract the SQLState class code
	 * @return The SQLState class code, or null.
	 */
	public static String extractSqlStateClassCode(SQLException sqlException) {
		return determineSqlStateClassCode( extractSqlState( sqlException ) );
	}

	public static String determineSqlStateClassCode(String sqlState) {
		if ( sqlState == null || sqlState.length() < 2 ) {
			return sqlState;
		}
		return sqlState.substring( 0, 2 );
	}
}
