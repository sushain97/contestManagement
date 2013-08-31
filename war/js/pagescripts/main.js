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
	var activeNum = Math.floor((Math.random() * $('.item').length));
	$('.item').eq(activeNum).addClass('active');
	$('.carousel-indicators li').eq(activeNum).addClass('active');
});

$(window).load(function() {
	$('.loading-gif').hide();
	$('.carousel-indicators, .carousel-control, .carousel-inner').show();
	$('.carousel').carousel({interval: 3000});
	$('.carousel').carousel('cycle');
});