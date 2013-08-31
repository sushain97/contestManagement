/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

$(document).ready(function() {
	$('button').on('click', function() {
		$('.accordion-body').addClass('in');
		$('button').hide();
		window.print();
		$('.accordion-body').removeClass('in');
		$('button').show();
	});
});

$(window).load(function() {
	initialize();
});

function initialize() {
	google.maps.visualRefresh = true;
	var directionsDisplay = new google.maps.DirectionsRenderer();
	var dulles = new google.maps.LatLng(29.621109,-95.584023);
	var mapOptions = {
	    zoom: 13,
	    center: dulles,
	    mapTypeId: google.maps.MapTypeId.ROADMAP,
	    zoomControl: true,
	    zoomControlOptions: { style: google.maps.ZoomControlStyle.SMALL }
    };
	var map = new google.maps.Map($('#map')[0], mapOptions);
	directionsDisplay.setMap(map);
	
	var marker = new google.maps.Marker({
		position: dulles,
		map: map,
		animation: google.maps.Animation.DROP
	});
	var infoWindow = new google.maps.InfoWindow({
		content: "<div id=\"information\"><h2 class=\"infoWindow\" style=\"font-size: xx-large;\">Dulles High School</h2>" +
				"<div class=\"infoWindow\" style=\"font-size: large;\">550 Dulles Ave, Sugar Land, TX 77478</div><br/>" +
				"<a class=\"infoWindow\" style=\"font-size: small;\" target=\"_newtab\" href=\"https://maps.google.com/maps?f=d&hl=en&q=John+Foster+Dulles+High+School,+550+Dulles+Ave,+Sugar+Land,+Fort+Bend,+Texas+77478-3746&aq=&sll=31.168934,-100.076842&sspn=13.309797,23.269043&t=m&ie=UTF8&split=0&daddr=John+Foster+Dulles+High+School,+550+Dulles+Ave,+Sugar+Land,+TX+77478\">Directions</a></div>"
	});
	infoWindow.open(map, marker);
	
	google.maps.event.addListener(marker, 'click', function() {
		infoWindow.open(map,marker);
	});
}