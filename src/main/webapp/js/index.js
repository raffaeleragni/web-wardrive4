
var map;
$().ready(function ()
{
   $("#wardrive_menu").click(function (){ $("#wardrive_menu_block").toggle();$("#wardrive_menu").toggleClass("open"); });
   
   map = new google.maps.Map(document.getElementById("map_canvas"),
   {
        zoom: 4,
        center: new google.maps.LatLng(43, 13),
        mapTypeId: google.maps.MapTypeId.ROADMAP
   });
   
});

