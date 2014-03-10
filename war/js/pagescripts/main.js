/* Component of GAE Project for TMSCA Contest Automation
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

	if($('#studentData').val()) {
		var studentData = JSON.parse($('#studentData').val());
		$.each(studentData, function() {
			addStudent(this["name"], this["grade"], [this['N'], this['C'], this['M'], this['S']]);
		});

		$.extend($.tablesorter.themes.bootstrap, {
			table: 'table'
		});

		$('#registrations').tablesorter({
			theme : 'bootstrap',
			headerTemplate : '{content} {icon}',
			widgets : ['uitheme']
		});
	}
});

$(window).load(function() {
	$('.loading').hide();
	$('.carousel-indicators, .carousel-control, .carousel-inner').fadeIn();
	$('.carousel').carousel({interval: 3000});
	$('.carousel').carousel('cycle');
});

function addStudent(name, grade, subjects) {
	var tr = $('<tr class="student"></tr>');

	tr.append($('<td class="text-center"></td>').text(name));
	tr.append($('<td class="text-center"></td>').text(grade));

	for(var j = 0; j < 4; j++) {
		var td = $('<td class="text-center"></td>');
		if(subjects[j]) {
			td.html('<span class="hide">1</span><i class="glyphicon glyphicon-ok"></i>');
			td.addClass('success');
		}
		else {
			td.html('<span class="hide">0</span><i class="glyphicon glyphicon-remove"></i>');
			td.addClass('danger');
		}
		tr.append(td);
	}

	$('#registrations tbody').append(tr);
}
