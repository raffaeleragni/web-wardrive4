/*
 *   wardrive4-web - android wardriving application (web side)
 *   Copyright (C) 2012 Raffaele Ragni
 *   https://github.com/raffaeleragni/web-wardrive4
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ki.wardrive4.web.utils;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Application constants.
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class ConnectionUtils
{
    private static final String JNDIJDBC = "java:comp/env/jdbc/wardrive4";

    public static Connection getConnection() throws NamingException, SQLException
    {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup(JNDIJDBC);
        return ds.getConnection();
    }
}
