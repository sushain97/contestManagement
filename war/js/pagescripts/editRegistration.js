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

$(document).ready(function () {
	if(studentData && studentData.length) {
		var subjects = ['N', 'C', 'M', 'S'];
		if(studentData.length > 0)
			$('.student').remove();

		$.each(studentData, function() {
			var tr = $('<tr class="student"></tr>');
			tr.append($('<td class="text-center"><span class="btn btn-xs btn-default tableBtn deleteBtn"><i class="glyphicon glyphicon-remove"></i></span></td>'));

			var td = $('<td class="text-center"></td>');
			td.append($('<input type="text" class="form-control input-sm" value required></td>').val(this["name"]));
			tr.append(td);

			var td = $('<td class="text-center"></td>');
			td.append($('<select class="elemGrades"><option value="4">4</option><option value="5">5</option>').val(this["grade"]));
			td.append($('<select class="midGrades"><option value="6">6</option><option value="7">7</option><option value="8">8</option></select>').val(this["grade"]));
			td.append($('<select class="highGrades"><option value="9">9</option><option value="10">10</option><option value="11">11</option><option value="12">12</option></select>').val(this["grade"]));
			tr.append(td);

			for(var j = 0; j < 4; j++) {
				var td = $('<td class="text-center"></td>');
				td.append($('<input type="checkbox" class="testCheckbox">').prop('checked', this[subjects[j]]))
				tr.append(td);
			}

			tr.append('<td class="text-center visible-lg"><span class="btn btn-xs btn-default allBtn">All</span> <span class="btn btn-xs btn-default noneBtn">None</span></td>');

			$('#registrations tbody').append(tr);
		});
	}

	enableSubmit();
	calcCost();
	checkAccount();
	adjustGradeSelect();

	$.extend($.tablesorter.themes.bootstrap, {
		table: 'table'
	});

	$('#registrations').tablesorter({
		theme : 'bootstrap',
		headerTemplate : '{content} {icon}',
		widgets : ['uitheme'],
		headers: {
			0: {sorter: false},
			1: {sorter: 'inputs'},
			2: {sorter: 'selectNum'},
			3: {sorter: 'checkbox'},
			4: {sorter: 'checkbox'},
			5: {sorter: 'checkbox'},
			6: {sorter: 'checkbox'},
			7: {sorter: false }
		}
	});

	$('#regType1, #regType2').change(checkAccount);
	$('#account').change(checkAccount);

	$('input[name=schoolLevel]').change(adjustGradeSelect);

	$('#delete').change(function() {
		$('#info').toggle('fast');
		$('form').attr('novalidate', !$('form').attr('novalidate'));
	});
});

function checkAccount() {
	var account = $('#account').prop('checked') && $('#regType1').prop('checked');
	$('#password').prop('required', account);
	$('#confPassword').prop('required', account);
	if(account) {
		$('#accountCreds').show('fast');
		$('#account').prop('checked', true);
	}
	else {
		$('#accountCreds').hide('fast');
		$('#account').prop('checked', false);
	}

	if($('#regType2').prop('checked'))
		$('#makeAccount').hide('fast');
	else
		$('#makeAccount').show('fast');
}

function adjustGradeSelect() {
	$('#gradeSelects').remove();
	if($('#schoolTypeElementary').prop('checked'))
		$('<style id="gradeSelects"> .elemGrades { display: block; } .midGrades { display: none; } .highGrades { display: none; }</style>').appendTo('head');
	else if($('#schoolTypeMiddle').prop('checked'))
		$('<style id="gradeSelects"> .elemGrades { display: none; } .midGrades { display: block; } .highGrades { display: none; }</style>').appendTo('head');
	else if($('#schoolTypeHigh').prop('checked'))
		$('<style id="gradeSelects"> .elemGrades { display: none; } .midGrades { display: none; } .highGrades { display: block; }</style>').appendTo('head');
}

function enableSubmit() {
	$('#submit').prop('disabled', false);
	$('#reset').prop('disabled', false);
	$('#answerCaptcha').html('');
}

function calcCost() {
	$('#cost').val($('table input[type=checkbox]:checked').length * price);
}
