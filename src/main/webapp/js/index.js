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


// Delay between subsequent sync calls as long as there is data.
var SYNCDATA_PROCWAIT = 2500;
// Time to wait for checking data to be synced
var SYNCDATA_REFRESHDELAY = 300000;
// First 12 slots backwards form the beginning are months.
// From that on, it's a year per slot.
// Afer 10 years it becomes "all"
// Total = 22
var SLIDER_MAX = 22;
// Month names
var MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
// Max WiFi visible at once
var MAX_WIFI = 250;


// -----------------------------------------------------------------------------


var map;
var db;
var mapMoveTimeout = null;
var markersArray = [];
var mapLoaded = false;
var timelineFilterStart = null, timelineFilterStop = null; // null = no filter


// -----------------------------------------------------------------------------


function addMarker(m)
{
    var marker = new google.maps.Marker(m);
    google.maps.event.addListener(marker, 'click', function()
    {
        new google.maps.InfoWindow( {content: m.infoText} ).open(map, marker);
    });
    marker.setMap(map);
    markersArray.push(marker);
}

function clearMarkers()
{
    if (markersArray)
        for (i in markersArray)
            markersArray[i].setMap(null);
}

function sqlError(err)
{
    console.log(err);
}

function createDB()
{
    db.transaction(function (t)
    {
        t.executeSql("create table if not exists wifi ("
            + "_id text primary key,"
            + "bssid text,"
            + "ssid text,"
            + "capabilities text,"
            + "security integer,"
            + "level integer,"
            + "frequency integer,"
            + "lat real,"
            + "lon real,"
            + "alt real,"
            + "geohash text,"
            + "timestamp integer)");
        t.executeSql("create index if not exists wifisearch_1 on wifi (lat, lon)");
        t.executeSql("create index if not exists wifisearch_2 on wifi (lat, lon, timestamp)");
    });
}

function sessionCheck()
{
    // Attempt a check for session.
    // Necessary because the app accesses the user's data.
    // If no session available, redirect to the login page.
    $.ajax(
    {
        async: false,
        url: "session-ok",
        statusCode: {401: function() {location.href = "loginform.html";}}
    });
    // Repeat this check each 5 minutes, so that the session is kept alive as
    // long as the app is up.
    setTimeout(sessionCheck, 300000);
}

// Synchronize 
function syncData()
{
    db.transaction(function (t2)
    {
        t2.executeSql("select coalesce(max(timestamp), 0) as t from wifi", [], function (t2, r)
        {
            var tstamp = 0;
            if (r.rows.length > 0)
                tstamp = r.rows.item(0)['t'];
            // May get strange results from above...
            if (isNaN(tstamp))
                tstamp = 0;

            $.ajax(
            {
                url: "ajaxsync",
                data:
                {
                    action: "fetch",
                    mark: tstamp
                },
                dataType: "xml",
                success: function(d)
                {
                    db.transaction(function (t)
                    {
                        var wifis = d == null ? null : $(d).find("wifi");
                        // No data found, sleep the long time
                        if (wifis == null || wifis.length == 0)
                        {
                            setTimeout(syncData, SYNCDATA_REFRESHDELAY);
                            return;
                        }

                        wifis.each(function ()
                        {
                            var id = $(this).attr("id");
                            var parameters =
                            [
                                $(this).find("bssid").text(),
                                $(this).find("ssid").text(),
                                $(this).find("capabilities").text(),
                                $(this).find("security").text(),
                                $(this).find("level").text(),
                                $(this).find("frequency").text(),
                                $(this).find("lat").text(),
                                $(this).find("lon").text(),
                                $(this).find("alt").text(),
                                $(this).find("geohash").text(),
                                $(this).find("timestamp").text(),
                                id
                            ];
                            t.executeSql("select timestamp from wifi where _id = ?", [id], function (t, r)
                            {
                                if (r.rows.length > 0)
                                {
                                    var rec_tstamp = parseInt(r.rows.item(0)['timestamp']);
                                    var upd_tstamp = parseInt($(this).find("timestamp").text());
                                    // Make sure the new data is actually new (should be)
                                    if (upd_tstamp > rec_tstamp)
                                        t.executeSql("update wifi set "
                                            + " bssid = ?,"
                                            + " ssid = ?,"
                                            + " capabilities = ?,"
                                            + " security = ?,"
                                            + " level = ?,"
                                            + " frequency = ?,"
                                            + " lat = ?,"
                                            + " lon = ?,"
                                            + " alt = ?,"
                                            + " geohash = ?,"
                                            + " timestamp = ?"
                                            + " where _id = ?",
                                            parameters);
                                }
                                else
                                    t.executeSql("insert into wifi (bssid, ssid, capabilities, security, level, frequency, lat, lon, alt, geohash, timestamp, _id) "
                                        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", parameters);
                            });
                        });
                        // Data was found, just wait a minimum to requery again
                        setTimeout(syncData, SYNCDATA_PROCWAIT);
                        // Also reload data since it's changed.
                        reloadData();
                    },
                    sqlError);
                }
            });
        });
    },
    sqlError);
}

function reloadData()
{
    if (map == null || map.getBounds() == null)
        return;
    
    var lat1 = map.getBounds().getNorthEast().lat();
    var lon1 = map.getBounds().getNorthEast().lng();
    var lat2 = map.getBounds().getSouthWest().lat();
    var lon2 = map.getBounds().getSouthWest().lng();
    
    // Make sure which one is the greatest
    var tmp;
    if (lat1 > lat2)
    {
        tmp = lat1;
        lat1 = lat2;
        lat2 = tmp;
    }
    if (lon1 > lon2)
    {
        tmp = lon1;
        lon1 = lon2;
        lon2 = tmp;
    }
    // TODO: add a loading icon in a corner that will be active as long as AJAX
    // requests are sent to server and there is data to process.
    // No loading should appear if a single request gives 0 updates at first shot.
    
    var params = [lat1, lat2, lon1, lon2];
    var timelineCondition = "";
    if (timelineFilterStart != null && timelineFilterStop != null)
    {
        timelineCondition = " and timestamp between ? and ? ";
        params.push(timelineFilterStart);
        params.push(timelineFilterStop);
    }
    else if (timelineFilterStart != null)
    {
        timelineCondition = " and timestamp > ?";
        params.push(timelineFilterStart);
    }
    else if (timelineFilterStop != null)
    {
        timelineCondition = " and timestamp < ?";
        params.push(timelineFilterStop);
    }
    
    // Select rawly the available data and display it (no-delay).
    db.transaction(function (t)
    {
        t.executeSql(
            "select * from wifi where lat between ? and ? and lon between ? and ? "
                + timelineCondition
                +" order by timestamp desc limit " + MAX_WIFI,
            params,
            function(t, r)
            {
                clearMarkers();
                updateVisibleWiFiCount(r.rows.length);
                for (var i=0; i<r.rows.length; i++)
                {
                    var item = r.rows.item(i);
                    var color = "#000000";
                    switch (item['security'])
                    {
                        case 0:
                            color = "#00c800";
                            break;
                        case 1:
                            color = "#eba017";
                            break;
                        case 2:
                            color = "#FF0000";
                            break;
                    }
                    addMarker(
                    {
                        id: item['id'],
                        ssid: item['ssid'],
                        title: item['ssid'],
                        clickable: true,
                        flat: true,
                        draggable: false,
                        position: new google.maps.LatLng(item['lat'], item['lon']),
                        icon:
                        {
                            path: google.maps.SymbolPath.CIRCLE,
                            scale: 7,
                            fillOpacity: 0.75,
                            strokeOpacity: 0.75,
                            strokeWeight: 1,
                            strokeColor: color,
                            fillColor: color
                        },
                        infoText: "<span style=\"font-size: bigger\"><strong>"+item['ssid']+"</strong></span>"
                            + "<br/><span style=\"font-size: smaller\">" + "<em >"+item['bssid']+"</em>"
                            + "<br/>" + item['capabilities']
                            + "<br/>" + item['level'] + " dBm"
                            + "<br/>" + item['frequency'] + " Ghz"
                            + "</span>"
                    });
                }
            });
    },
    sqlError);
}

function onTimelineSlide(e, ui)
{
    var v1 = ui.values[0];
    var v2 = ui.values[1];
    
    var v1leftperc = v1 / SLIDER_MAX * 100;
    var v2leftperc = v2 / SLIDER_MAX * 100;
    var centerperc = (v2leftperc - v1leftperc) / 2 + v1leftperc;
    $("#timeline-tips-btn1").css({left: v1leftperc+"%"});
    $("#timeline-tips-btn2").css({left: v2leftperc+"%"});
    $("#timeline-tips-desc").css({left: centerperc+"%"});
    
    var date1 = getTimelineTimestamp(v1);
    var date2 = getTimelineTimestamp(v2);
    var tstamp1desc = v1 == 0 ? "EVER" : getTimelineTimestampDescription(date1, SLIDER_MAX-v1 <= 12);
    var tstamp2desc = v2 == SLIDER_MAX ? "NOW" : getTimelineTimestampDescription(date2, SLIDER_MAX-v2 <= 12);
    
    $("#timeline-tips-btn1").text(tstamp1desc);
    $("#timeline-tips-btn2").text(tstamp2desc);
    
    if (v1 > 0)
        timelineFilterStart = date1.valueOf();
    else
        timelineFilterStart = null;
    if (v2 < SLIDER_MAX)
        timelineFilterStop = date2.valueOf();
    else
        timelineFilterStop = null;
    reloadData();
}

function getTimelineTimestamp(value)
{
    var delta = SLIDER_MAX - value;
    var now = new Date();
    var result = new Date();
    var m = now.getMonth();
    var y = now.getFullYear();
    if (delta < 12)
    {
        // Now - delta months
        m -= delta;
        if (m < 0)
        {
            y --;
            m = 12 + m;
        }
        result.setFullYear(y);
        result.setMonth(m);
        result.setDate(1);
        result.setHours(0);
        result.setMinutes(0);
        result.setSeconds(0);
        result.setMilliseconds(0);
        return result;
    }
    else
    {
        delta -= 12;
        // each 1 after 12 subtracts a year.
        // Altough an exact "12" (which becomes "0") would mean the beginning of
        // the year of today - 12 months, meaning the last one.
        // Result: increment delta by 1.
        delta++;
        y -= delta;
        result.setFullYear(y);
        result.setMonth(0);
        result.setDate(1);
        result.setHours(0);
        result.setMinutes(0);
        result.setSeconds(0);
        result.setMilliseconds(0);
        return result;
    }
}

function updateVisibleWiFiCount(count)
{
    $("#timeline-tips-desc").text("");
    $("#timeline-tips-desc").append($("<span>WiFi visible: "+count+"</span><br/>"));
    if (count >= MAX_WIFI)
        $("#timeline-tips-desc").append($("<span style=\"color: red\"><em>Limited</em></span>"));
}

function getTimelineTimestampDescription(date, showMonth)
{
    if (showMonth)
        return MONTH_NAMES[date.getMonth()] + " " + date.getFullYear();
    else
        return date.getFullYear();
}

$().ready(function ()
{
    // Init the database
    db = openDatabase("wifi", "1.0", "WiFi", 10 * 1024 * 1024);
    createDB();
    // Check if session server-side is OK
    sessionCheck();
    // Call syncData() once, it will start synchronizing the data and continue.
    syncData();
    // Toggle the menu action
    $("#wardrive_menu").click(function ()
    {
        $("#wardrive_menu_block").toggle();
        $("#wardrive_menu").toggleClass("open");
    });
    // Init timeline slider
    $("#timeline").slider(
    {
        range: true,
        min: 0,
        max: SLIDER_MAX,
        step: 1,
        values: [20, 22],
        slide: onTimelineSlide
    });
    onTimelineSlide(null, {values: [20, 22]});
    // Init the google maps
    map = new google.maps.Map(document.getElementById("map_canvas"),
    {
        zoom: 4,
        center: new google.maps.LatLng(43, 13),
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });
    // Callback to map movement, to call a delayed reloadData()
    google.maps.event.addListener(map, 'center_changed', function()
    {
        // less than a second after the center of the map has changed,
        // reload data
        if (mapMoveTimeout != null)
            clearTimeout(mapMoveTimeout);
        mapMoveTimeout = setTimeout(function ()
        {
            reloadData();
            mapMoveTimeout = null;
        },
        750);
    });
    // Watch for when the map is available
    google.maps.event.addListener(map, 'idle', function()
    {
        if(!mapLoaded)
            reloadData();
        mapLoaded=true; 
    });
});