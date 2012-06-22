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

package ki.wardrive4.web.android;

import java.sql.*;
import java.util.List;
import javax.naming.NamingException;
import ki.wardrive4.web.data.WiFi;
import ki.wardrive4.web.utils.JSONBuilder;

/**
 * Sync logic.
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class Sync
{
    // The limit of how much records we will send back to Android at once.
    // Considered it to be used also in 3G situation, better to keep it to a
    // minimum, and increment the frequency of updates.
    private static final int PAGE_LIMIT = 10;
    
    public static String fetch(Connection c, String username, long mark) throws NamingException, SQLException
    {
        // Android will send us the last mark of where the last sync has ended.
        // The last mark is the max value of all the previously synchronized
        // items. We will restart from there (>mark)
        try (PreparedStatement s = c.prepareStatement(
            "select * from wifi where fk_user = ? and t_timestamp > ? order by t_timestamp asc limit "+PAGE_LIMIT))
        {
            s.setString(1, username);
            s.setTimestamp(2, new Timestamp(mark));
            try (ResultSet rs = s.executeQuery())
            {
                // Return all the limited items. Being them ordered, each
                // sequential call to this service, given the right mark,
                // will advance incrementally in the synchronization.
                JSONBuilder jb = new JSONBuilder();
                jb.beginArray();
                ResultSetMetaData rsmd = rs.getMetaData();
                while (rs.next())
                {
                    jb.beginObject();
                    for (int i = 1; i <= rsmd.getColumnCount(); i++)
                        jb.property(rsmd.getColumnLabel(i), rs.getObject(i));
                    jb.endObject();
                }
                jb.endArray();
                return jb.toString();
            }
        }
    }
    
    public static void push(Connection c, String username, List<WiFi> wifis) throws SQLException
    {
        // Consider only the ones that have a greater timestamp. If we
        // are the ones having the most updated data, then just leave
        // it as it is in our DB. In the sequential call to the sync as
        // "fetch" that most updated data will go back to android.
        for (WiFi wifi: wifis)
        {
            // 1: check if the user has this one already
            try (PreparedStatement s = c.prepareStatement(
                "select * from wifi where fk_user = ? and id > ?"))
            {
                s.setString(1, username);
                s.setString(2, wifi.id);
                try (ResultSet rs = s.executeQuery())
                {
                    // Not existing, just create
                    if (!rs.next())
                    {
                        try (PreparedStatement s_insert = c.prepareStatement(
                            "insert into wifi ("
                            + "bssid,"
                            + "ssid,"
                            + "capabilities,"
                            + "security,"
                            + "level,"
                            + "frequency,"
                            + "lat,"
                            + "lon,"
                            + "alt,"
                            + "geohash,"
                            + "t_timestamp,"
                            + "id,"
                            + "fk_user)"
                            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"))
                        {
                            int ct = 1;
                            s_insert.setString(ct++, wifi.bssid);
                            s_insert.setString(ct++, wifi.ssid);
                            s_insert.setString(ct++, wifi.capabilities);
                            s_insert.setInt(ct++, wifi.security);
                            s_insert.setInt(ct++, wifi.level);
                            s_insert.setInt(ct++, wifi.frequency);
                            s_insert.setDouble(ct++, wifi.lat);
                            s_insert.setDouble(ct++, wifi.lon);
                            s_insert.setDouble(ct++, wifi.alt);
                            s_insert.setString(ct++, wifi.geohash);
                            s_insert.setTimestamp(ct++, new Timestamp(wifi.timestamp));
                            s_insert.setString(ct++, wifi.id);
                            s_insert.setString(ct++, username);
                            s_insert.executeUpdate();
                        }
                    }
                    else
                    {
                        // Check if the timestamp is superior
                        if (rs.getTimestamp("timestamp").getTime() < wifi.timestamp)
                        {
                            try (PreparedStatement s_insert = c.prepareStatement(
                                "update wifi set "
                                + "bssid = ?,"
                                + "ssid = ?,"
                                + "capabilities = ?,"
                                + "security = ?,"
                                + "level = ?,"
                                + "frequency = ?,"
                                + "lat = ?,"
                                + "lon = ?,"
                                + "alt = ?,"
                                + "geohash = ?,"
                                + "t_timestamp = ? "
                                + "where id = ? and fk_user = ?"))
                            {
                                int ct = 1;
                                s_insert.setString(ct++, wifi.bssid);
                                s_insert.setString(ct++, wifi.ssid);
                                s_insert.setString(ct++, wifi.capabilities);
                                s_insert.setInt(ct++, wifi.security);
                                s_insert.setInt(ct++, wifi.level);
                                s_insert.setInt(ct++, wifi.frequency);
                                s_insert.setDouble(ct++, wifi.lat);
                                s_insert.setDouble(ct++, wifi.lon);
                                s_insert.setDouble(ct++, wifi.alt);
                                s_insert.setString(ct++, wifi.geohash);
                                s_insert.setTimestamp(ct++, new Timestamp(wifi.timestamp));
                                s_insert.setString(ct++, wifi.id);
                                s_insert.setString(ct++, username);
                                s_insert.executeUpdate();
                            }
                        }
                    }
                }
            }
        }
    }
}
