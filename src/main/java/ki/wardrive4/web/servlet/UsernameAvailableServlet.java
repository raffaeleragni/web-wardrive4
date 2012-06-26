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
package ki.wardrive4.web.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static ki.wardrive4.web.utils.ConnectionUtils.getConnection;

/**
 * Tell if the username requested is available for registration.
 *
 * This Servlet is public.
 * Response is in JSON ("true"|"false").
 *
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class UsernameAvailableServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        String username = request.getParameter("username");
        response.setContentType("text/json");

        if (username == null || username.isEmpty())
        {
            response.getWriter().write("false");
            return;
        }

        try (Connection c = getConnection())
        {
            try (PreparedStatement s = c.prepareStatement(
                "select count(username) from users where username = ?"))
            {
                s.setString(1, username);
                try (ResultSet rs = s.executeQuery())
                {
                    rs.next();
                    // count > 0 = username found = not available = return false
                    if (rs.getInt(1) > 0)
                        response.getWriter().write("false");
                    else
                        response.getWriter().write("true");
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            processRequest(request, response);
        }
        catch (Exception ex)
        {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            processRequest(request, response);
        }
        catch (Exception ex)
        {
            throw new ServletException(ex);
        }
    }// </editor-fold>
}
