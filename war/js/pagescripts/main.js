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

	if(studentData) {
		var grades;
		if(level === 'elementary') {
			$('<style id="gradeSelects"> .elemGrades { display: block; } .midGrades { display: none; } .highGrades { display: none; }</style>').appendTo('head');
			grades = [4, 5];
		}
		else if(level === 'middle') {
			$('<style id="gradeSelects"> .elemGrades { display: none; } .midGrades { display: block; } .highGrades { display: none; }</style>').appendTo('head');
			grades = [6, 7, 8];
		}
		else {
			$('<style id="gradeSelects"> .elemGrades { display: none; } .midGrades { display: none; } .highGrades { display: block; }</style>').appendTo('head');
			grades = [9, 10, 11, 12];
		}

		if(regEditClosed)
			$.each(studentData, function() {
				addFrozenStudent(this["name"], this["grade"], [this['N'], this['C'], this['M'], this['S']]);
			});
		else
			$.each(studentData, function() {
				addStudent(this["name"], this["grade"], [this['N'], this['C'], this['M'], this['S']]);
			});

		$.extend($.tablesorter.themes.bootstrap, {
			table: 'table'
		});

		if(regEditClosed)
			$('#registrations').tablesorter({
				theme: 'bootstrap',
				headerTemplate: '{content} {icon}',
				widgets: ['uitheme']
			});
		else
			$('#registrations').tablesorter({
				theme: 'bootstrap',
				headerTemplate: '{content} {icon}',
				widgets: ['uitheme'],
				headers: {
					0: {sorter: false},
					1: {sorter: 'inputs'},
					2: {sorter: 'selectNum'},
					3: {sorter: 'checkbox'},
					4: {sorter: 'checkbox'},
					5: {sorter: 'checkbox'},
					6: {sorter: 'checkbox'},
					7: {sorter: false}
				}
			});

		calcCost();
	}
});

$(window).load(function() {
	$('.loading').hide();
	$('.carousel-indicators, .carousel-control, .carousel-inner').fadeIn();
	$('.carousel').carousel({interval: 3000});
	$('.carousel').carousel('cycle');
});

function calcCost() {
	if($('table input[type=checkbox]:checked').length)
		$('#cost').text(($('table input[type=checkbox]:checked').length * price).toFixed(2));
	else if($('.student .glyphicon-ok').length)
		$('#cost').text(($('.student .glyphicon-ok').length * price).toFixed(2));
}

