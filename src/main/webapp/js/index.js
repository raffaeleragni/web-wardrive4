
var map;
var db;

function sessionCheck()
{
    // Attempt a check for session.
    // Necessary because the app accesses the user's data.
    // If no session available, redirect to the login page.
    $.ajax(
    {
        async: false,
        url: "session-ok",
        statusCode: { 401: function() { location.href = "loginform.html"; } }
    });
    // Repeat this check each 5 minutes, so that the session is kept alive as
    // long as the app is up.
    setTimeout(sessionCheck, 300000);
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

function getMaxTimestamp()
{
    db.transaction(function (t)
    {
        t.executeSql(
            "select max(timestamp) as m from wifi", [],
            function(t, r)
            {
                if (r.rows.length == 0)
                    return 0;
                else
                    return r.rows.item(i)["m"];
            });
    });
}

function refreshData(lat1, lon1, lat2, lon2)
{
    // TODO: add a loading icon in a corner that will be active as long as AJAX
    // requests are sent to server and there is data to process.
    // No loading should appear if a single request gives 0 updates at first shot.
    
    // TODO: clear map visually of available points
    
    // Select rawly the available data and display it (no-delay).
    db.transaction(function (t)
    {
        t.executeSql(
            "select * from wifi where lat between ? and ? and lon between ? and ?",
            [lat1, lat2, lon1, lon2],
            function(t, r)
            {
                for (var i=0; i<results.rows.length; i++)
                {
                    var item = results.rows.item(i);
                    // TODO: add points to map
                }
                // Get the max timestamp and query the server for updates.
                var tstamp = getMaxTimestamp();
                // Query the server, if new items were available, refresh the data
                // (recall this function again).
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
                        // refreshData(lat1, lon1, lat2, lon2);
                    }
                });
            });
    });
}

$().ready(function ()
{
    sessionCheck();
    // Init the database
    db = openDatabase("wifi", "1.0", "WiFi", 10 * 1024 * 1024);
    createDB();
    // Toggle the menu action
    $("#wardrive_menu").click(function (){ $("#wardrive_menu_block").toggle(); $("#wardrive_menu").toggleClass("open"); });
    // Init the google maps
    map = new google.maps.Map(document.getElementById("map_canvas"),
    {
        zoom: 4,
        center: new google.maps.LatLng(43, 13),
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });
    // TODO: add callback to map movement, to call refreshData() with map bounds.
});