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
		var grades = $("#level").val() == "middle" ? [6, 7, 8] : [9, 10, 11, 12];

		if($('#regEditClosed').val() === 'true')
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

		if($('#regEditClosed').val() === 'true')
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
					2: {sorter: 'select'},
					3: {sorter: 'checkbox'},
					4: {sorter: 'checkbox'},
					5: {sorter: 'checkbox'},
					6: {sorter: 'checkbox'}
				}
			});

		CalcCost();

		$('.addStudentBtn').click(function() {
			var numStudents = $(this).attr('data-numStudents');
			for(var i = 0; i < numStudents; i++)
				addStudent('', grades[0], [false, false, false, false]);
		});

		$(document).on('click', '.deleteBtn', function() {
			var tr = $(this).parents('tr');
			tr.hide('fast', function() {
				tr.remove();
				$('#registrations').trigger('update');
				CalcCost();
			});
		});

		$('input[type="number"]').change(CalcCost);
		$(document).on('change', 'table input[type=checkbox]', function() {
			CalcCost();
		});

		$('form').submit(function(ev) {
			var students = [];
			$.each($('.student'), function() {
				var td = $('td', this);
				students.push({
					"name": $(td[1]).find('input').val(),
					"grade": parseInt($(td[2]).find('select:visible').val()),
					"N": $(td[3]).find('input').prop('checked'),
					"C": $(td[4]).find('input').prop('checked'),
					"M": $(td[5]).find('input').prop('checked'),
					"S": $(td[6]).find('input').prop('checked')
				});
			});
			$('input[name=studentData]').remove();
			$(this).append($('<input>').attr('type', 'hidden').attr('name', 'studentData').val(JSON.stringify(students)));
		});

		$('button#import').click(function() {
			var students = $('textarea#importData').val().split('\n');
			var seperator = $('input#seperator').val();

			$.each(students, function() {
				var fields = this.split(seperator);
				if(fields.length == 6 && parseInt(fields[1].trim()))
					addStudent(fields[0].trim(), fields[1].trim(), [!!fields[2].trim().length, !!fields[3].trim().length, !!fields[4].trim().length, !!fields[5].trim().length]);
			});
		});

		var refreshTable = function() {
			var students = $('textarea#importData').val().split('\n');
			var seperator = $('input#seperator').val();

			var tableBody = $('table#importTable tbody');
			tableBody.empty();

			$.each(students, function() {
				var fields = this.split(seperator);
				var tr = $('<tr></tr>');

				if(fields.length < 6 || !parseInt(fields[1].trim())) {
					tr.addClass("danger");
					tr.append($('<td></td>').text('Failed'));
				}
				else {
					tr.append($('<td></td>').text(fields[0].trim()));
					tr.append($('<td></td>').text(fields[1].trim()));


					var checked = '<i class="glyphicon glyphicon-ok"></i>', unchecked = '<i class="glyphicon glyphicon-remove"></i>';
					tr.append($('<td></td>').html(fields[2].trim().length ? checked : unchecked).addClass(fields[2].trim().length ? "success" : "danger"));
					tr.append($('<td></td>').html(fields[3].trim().length ? checked : unchecked).addClass(fields[3].trim().length ? "success" : "danger"));
					tr.append($('<td></td>').html(fields[4].trim().length ? checked : unchecked).addClass(fields[4].trim().length ? "success" : "danger"));
					tr.append($('<td></td>').html(fields[5].trim().length ? checked : unchecked).addClass(fields[5].trim().length ? "success" : "danger"));
				}

				tableBody.append(tr);
			});
		};

		$('textarea#importData').on('propertychange keyup input paste', refreshTable);
		$('input#seperator').on('input', refreshTable);
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
	tr.append($('<td class="text-center"><span class="btn btn-xs btn-default tableBtn deleteBtn"><i class="glyphicon glyphicon-remove"></i></span></td>'));

	var td = $('<td class="text-center"></td>');
	td.append($('<input type="text" class="form-control input-sm" value required></td>').val(name));
	tr.append(td);

	var td = $('<td class="text-center"></td>');
	if(grade >= 6 && grade <= 8)
		td.append($('<select><option value="6">6</option><option value="7">7</option><option value="8">8</option></select>').val(grade));
	else
		td.append($('<select><option value="9">9</option><option value="10">10</option><option value="11">11</option><option value="12">12</option></select>').val(grade));
	tr.append(td);

	for(var j = 0; j < 4; j++) {
		var td = $('<td class="text-center"></td>');
		td.append($('<input type="checkbox" class="testCheckbox">').prop('checked', subjects[j]));
		tr.append(td);
	}

	$('#registrations tbody').append(tr);
	$('#registrations').trigger('update');
	CalcCost();
}

function addFrozenStudent(name, grade, subjects) {
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

function addStudentRow(name, grade, subjects) {
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

function CalcCost() {
	var price = parseInt($('#price').val());
	if($('table input[type=checkbox]:checked').length)
		$('#cost').text(($('table input[type=checkbox]:checked').length * price).toFixed(2));
	else if($('.student .glyphicon-ok').length)
		$('#cost').text(($('.student .glyphicon-ok').length * price).toFixed(2));
}
