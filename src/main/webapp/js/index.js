
var map;

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

$().ready(function ()
{
    sessionCheck();
    // Toggle the menu action
    $("#wardrive_menu").click(function (){ $("#wardrive_menu_block").toggle(); $("#wardrive_menu").toggleClass("open"); });
    // Init the google maps
    map = new google.maps.Map(document.getElementById("map_canvas"),
    {
        zoom: 4,
        center: new google.maps.LatLng(43, 13),
        mapTypeId: google.maps.MapTypeId.ROADMAP
    });

});