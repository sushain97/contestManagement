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
	ChangeActive()
	$('button').on('click', function() {
		$('button').hide();
		window.print();
		$('button').show();
	});
	$("table").tablesorter({sortList: [[0,0]]});
	
	$('td').hover(function() {
	    var t = parseInt($(this).index()) + 1;
	    $('td:nth-child(' + t + ')', $(this).closest('table')).addClass('highlighted');
	}, function() {
	    var t = parseInt($(this).index()) + 1;
	    $('td:nth-child(' + t + ')', $(this).closest('table')).removeClass('highlighted');
	});
});

function ChangeActive() {
	var type = $.url().param('type');
	if (type == undefined)
		$("#avail").addClass('active');
	else {
		$("#" + type).closest('.dropdown').addClass('active');
		$('#' + type).addClass('active');
	}
}