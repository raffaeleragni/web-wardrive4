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
package ki.wardrive4.web.servlet.ajax;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import ki.wardrive4.web.data.WiFi;
import ki.wardrive4.web.data.WiFis;
import ki.wardrive4.web.servlet.LoginServlet;
import ki.wardrive4.web.sync.Sync;
import ki.wardrive4.web.utils.ConnectionUtils;

/**
 * Handles the android sync interface.
 * 
 * @author Raffaele Ragni <raffaele.ragni@gmail.com>
 */
public class SyncServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        String username = (String) request.getSession().getAttribute(LoginServlet.ATT_USERNAME);
        
        // Not logged in
        if (username == null)
        {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        WiFis wifis;
        try (Connection c = ConnectionUtils.getConnection())
        {
            switch (request.getParameter("action"))
            {
                case "fetch":
                    long mark = Long.parseLong(request.getParameter("mark"));
                    // Dump out the JSON
                    List<WiFi> out = Sync.fetch(c, username, mark);
                    wifis = new WiFis();
                    wifis.wifi = out;
                    response.setContentType("text/xml");
                    marshaller.marshal(wifis, response.getWriter());
                    break;

                case "push":
                    String data = request.getParameter("data");
                    wifis = unmarshaller
                        .unmarshal(new StreamSource(new StringReader((data))), WiFis.class)
                        .getValue();
                    Sync.push(c, username, wifis.wifi);
                    break;
            }
        }
    }
    
    // Keep them always ready.
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static
    {
        try
        {
            JAXBContext ctx = JAXBContext.newInstance(WiFis.class, WiFi.class);
            marshaller = ctx.createMarshaller();
            unmarshaller = ctx.createUnmarshaller();
        }
        catch (JAXBException ex)
        {
            Logger.getLogger(SyncServlet.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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
