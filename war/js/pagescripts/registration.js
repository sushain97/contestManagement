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
	if($('form').length) {
		EnableSubmit();
		CalcCost();
		CheckAccount();
		adjustGradeSelect();
	}
	
	$('h1 button').on('click', function() {
		window.print();
	});
	
	$('input[type="number"]').change(CalcCost);
	$('#captcha').on('keyup', AuthCaptcha);
	$('#submit').on('click', AuthCaptcha);
	
	$(document).on('change', 'table input[type=checkbox]', function() {
		CalcCost();
	});
	
	$('#regType1,#regType2').change(CheckAccount);
	$('#account').change(CheckAccount);
	
	$('#schoolType1,#schoolType2').change(adjustGradeSelect);
	
	$('.addStudentBtn').click(function() {
		var numStudents = $(this).attr('data-numStudents');
		for(var i = 0; i < numStudents; i++) {
			var tr = $('<tr></tr>');
			tr.append($('<td class="text-center"><span class="btn btn-xs btn-default tableBtn deleteBtn"><i class="glyphicon glyphicon-remove"></i></span></td>'));
			tr.append($('<td><input type="text" class="form-control input-sm" required></td>'));
			tr.append($('<td class="text-center"><select class="midGrades"><option value="6">6</option><option value="7">7</option><option value="8">8</option></select><select class="highGrades"><option value="9">9</option><option value="10">10</option><option value="11">11</option><option value="12">12</option></select></td>'));
			for(var j = 0; j < 4; j++)
				tr.append($('<td class="text-center"><input type="checkbox" class="testCheckbox"></td>'));
			$('tr#addOptions').before(tr);
		}
	});
	
	$(document).on('click', '.deleteBtn', function() {
		var tr = $(this).parents('tr');
		tr.hide('fast', function() { tr.remove(); });
	});
	
	$('#passStrength').tooltip({placement: 'right', html: 'true'});
	
	var passwordElem = $('#password');
	passwordElem.data('oldVal', passwordElem);
	passwordElem.bind("propertychange keyup input paste", function(event) {
		$('#passHelp').show();
		var passwordElem = $('#password');
		if (passwordElem.data('oldVal') != passwordElem.val()) {
			passwordElem.data('oldVal', passwordElem.val());
			
			var userInputs = ['tmsca', 'dulles'];
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

function CheckAccount() {
	var account = $('#account').prop('checked') && $('#regType1').prop('checked');
	$('#password').prop('required', account);
	$('#confPassword').prop('required', account);
	if(account) 
		$('#accountCreds').show('fast');
	else
		$('#accountCreds').hide('fast');
	
	if($('#regType2').prop('checked'))
		$('#makeAccount').hide('fast');
	else
		$('#makeAccount').show('fast');
}

function adjustGradeSelect() {
	$('#gradeSelects').remove();
	if($('#schoolType1').prop('checked'))
		$('<style id="gradeSelects"> .midGrades { display: block; } .highGrades { display: none; }</style>').appendTo('head');
	else
		$('<style id="gradeSelects"> .midGrades { display: none; } .highGrades { display: block; }</style>').appendTo('head');
}

function AuthCaptcha() {
	if (calcMD5($('#salt').val() + $('#captcha').val()) === $('#hash').val()) {
		$('#submit').prop('disabled', false)
		$('#answerCaptcha').hide();
		$('#captcha').closest('.control-group').removeClass('error');
	} else {
		$('#captcha').closest('.control-group').addClass('error');
		$('#submit').prop('disabled', true)
		$('#answerCaptcha').show();
		$('#answerCaptcha').html('Answer the captcha correctly to prove that you are not a robot.');
	}
}

function EnableSubmit() {
	if ($('#regError').val() === '') {
		$('#submit').prop('disabled', false)
		$('#reset').prop('disabled', false)
	}
	$('#answerCaptcha').html('');
}

function CalcCost() {
	var price = parseInt($('#price').val());
	$('#cost').val($('table input[type=checkbox]:checked').length * price);
}