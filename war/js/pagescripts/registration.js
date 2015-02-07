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
	$.extend($.tablesorter.themes.bootstrap, {
		table: 'table'
	});

	if($('form').length) {
		enableSubmit();
		calcCost();
		checkAccount();
		adjustGradeSelect();

		if(studentData) {
			if(studentData.length > 0)
				$('.student').remove();

			$.each(studentData, function() {
				addStudent(this["name"], this["grade"], [this['N'], this['C'], this['M'], this['S']]);
			});
		}

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
				7: {sorter: false}
			}
		});
	}
	else {
		if(studentData) {
			$.each(studentData, function() {
				addFrozenStudent(this["name"], this["grade"], [this['N'], this['C'], this['M'], this['S']]);
			});

			$('#registrations').tablesorter({
				theme: 'bootstrap',
				headerTemplate: '{content} {icon}',
				widgets: ['uitheme']
			});
		}
	}

	$('#printButton').on('click', function() {
		window.print();
	});

	$('#regType1, #regType2').change(checkAccount);
	$('#account').change(checkAccount);

	$('input[name=schoolLevel]').change(adjustGradeSelect);

	$('#passStrength').tooltip({placement: 'right', html: 'true'});

	var passwordElem = $('#password');
	passwordElem.data('oldVal', passwordElem);
	passwordElem.bind('propertychange keyup input paste', function(event) {
		$('#passHelp').show();
		var passwordElem = $('#password');
		if (passwordElem.data('oldVal') != passwordElem.val()) {
			passwordElem.data('oldVal', passwordElem.val());

			var userInputs = ['tmsca'];
			userInputs = userInputs.concat(userInputs, $("#schoolName").val().split(' '));
			userInputs = userInputs.concat(userInputs, $("#name").val().split(' '));
			userInputs.push($("#email").val().split('@')[0]);

			var passEval = zxcvbn($('#password').val(), userInputs);
			$('#passStrength').html(passEval.crack_time_display);
			var title = '<strong>Password Strength: </strong><br/> <strong>Crack Time: </strong>' + passEval.crack_time +
			' seconds</br> <strong>Informational Entropy: </strong>' + passEval.entropy + ' bits<br/>' + '<strong>Matched Sequences: </strong>'
			var sequences = passEval.match_sequence;
			for(var i = 0; i < sequences.length; i++)
				title += '<em>' + sequences[i].pattern + ': </em>"' + sequences[i].token + '"<br/>';
			$('#passStrength').attr('data-original-title', title).tooltip('fixTitle').tooltip('show');
		}
	});
	passwordElem.blur(function() {
		$('#passStrength').tooltip('hide');
	});
});

function checkAccount() {
	var account = ($('#account').prop('checked') && $('#regType1').prop('checked')) || $('input[name="registrationType"]:checked').val() === undefined;
	$('#password').prop('required', account);
	$('#confPassword').prop('required', account);

	if(account) {
		$('#accountCreds').show('fast');
		$('#account').prop('checked', true);
	}
	else {
		$('#accountCreds').hide('fast');
		$('#account').prop('checked', false);
		$('#password').val('');
		$('#confPassword').val('');
	}

	if($('#regType2').prop('checked')) {
		$('#makeAccount').hide('fast');
		$('#registerWarning').show('fast');
	}
	else {
		$('#makeAccount').show('fast');
		$('#registerWarning').hide('fast');
	}
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
	if (regError === '') {
		$('#submit').prop('disabled', false);
		$('#reset').prop('disabled', false);
	}
	$('#answerCaptcha').html('');
}

function calcCost() {
	$('#cost').val($('table input[type=checkbox]:checked').length * price);
}
