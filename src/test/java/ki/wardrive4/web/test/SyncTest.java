/*
 *   wardrive4-web - android wardriving application (web side)
 *   Copyright (C) 2012 Raffaele Ragni
 *   https://github.com/raffaeleragni/android-wardrive4
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
package ki.wardrive4.web.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.naming.NamingException;
import javax.xml.bind.JAXBException;
import ki.wardrive4.web.android.Sync;
import ki.wardrive4.web.data.WiFi;
import ki.wardrive4.web.data.WiFiSecurity;
import ki.wardrive4.web.data.WiFis;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class SyncTest
{
    private static final String[] TBL_CREATE = new String[]
    {
        "create table wifi (id varchar(40), fk_user varchar(255), bssid varchar(18), ssid varchar(255), capabilities varchar(255), security integer, level integer, frequency integer, lat double, lon double, alt double, geohash varchar(255), t_timestamp datetime, primary key(id, fk_user))",
        "create table users (username varchar(255), password varchar(255), primary key(username))",
        "insert into users(username, password) values('user', 'user')"
    };
    
    private static Connection connection;
    @BeforeClass
    public static void init() throws ClassNotFoundException, SQLException
    {
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection("jdbc:hsqldb:mem:test");
        connection.setAutoCommit(true);
        for (String sql: TBL_CREATE)
            try (PreparedStatement s = connection.prepareStatement(sql))
            {
                s.executeUpdate();
            }
    }
    
    @AfterClass
    public static void destroy() throws SQLException
    {
        connection.close();
    }
    
    @Test
    public void test() throws NamingException, SQLException, JAXBException
    {
        List<WiFi> wifis = new ArrayList<>();
        WiFi w;
        
        w = new WiFi();
        
        w.id = "XX";
        w.bssid = "YY";
        w.ssid = "ZZ";
        w.capabilities = "";
        w.security = WiFiSecurity.OPEN.ordinal();
        w.level = -30;
        w.frequency = 2345;
        w.lat = 55.6d;
        w.lon = 88.9d;
        w.alt = 11.2d;
        w.geohash = "asdagfarga";
        w.timestamp = new Date().getTime();
        wifis.add(w);
        
        Sync.push(connection, "user", wifis);
        List<WiFi> wifis2 = Sync.fetch(connection, "user", w.timestamp - 1);
        
        assert wifis2.size() > 0;
    }
}
