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
	EnableSubmit();
	CalcCost();
	CheckAccount();
	
	$('.span2').change(CalcCost);
	$('#captcha').on('keyup', AuthCaptcha);
	$('#submit').on('click', AuthCaptcha);
	$('#regType1,#regType2').change(function() {
		$('#aliases').toggle('slow');
	});
	
	$('#schoolType1,#schoolType2').change(function() {
		$('#mid').toggle();
		$('#hi').toggle();
		CalcCost();
	});
	
	$('#account').change(function() {
		$('#acc').toggle('slow');
		CheckAccount();
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
			$('#passStrength').data('tooltip').options.title = title;
			$('#passStrength').tooltip('fixTitle').tooltip('show');
		}
	});
	passwordElem.blur(function() {
		$('#passStrength').tooltip('hide');
	});
});

function CheckAccount() {
	var account = $('#account').prop('checked');
	$('#password').prop('required', account);
	$('#confPassword').prop('required', account);
	if(account)
		$('#acc').show();
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
	sum = 0;
	var i;
	var end;
	var price = parseInt($('#price').val());

	if ($('#schoolType1').is(':checked')) {
		i = 6;
		end = 8;
	} else {
		i = 9;
		end = 12;
	}

	for (; i <= end; i++) {
		var start = "[name=" + "'" + i;
		if (parseInt($(start + "n']").get(0).value) < 0)
			$(start + "n']").get(0).value = 0;
		if (parseInt($(start + "c']").get(0).value) < 0)
			$(start + "c']").get(0).value = 0;
		if (parseInt($(start + "s']").get(0).value) < 0)
			$(start + "s']").get(0).value = 0;
		if (parseInt($(start + "m']").get(0).value) < 0)
			$(start + "m']").get(0).value = 0;
		sum += price * parseInt($(start + "n']").get(0).value);
		sum += price * parseInt($(start + "c']").get(0).value);
		sum += price * parseInt($(start + "s']").get(0).value);
		sum += price * parseInt($(start + "m']").get(0).value);
	}
	if(isNaN(sum)) {
		$('#cost').closest('.control-group').addClass('error');
		if($('#cost').closest('.controls').find('.help-inline').length === 0)
			$('#cost').closest('.controls').append('<span class="help-inline">Please enter only <b>numeric</b> values above</span>');
	}
	else {
		$('#cost').val(sum);
		$('#cost').closest('.control-group').removeClass('error');
		$('#cost').closest('.controls').children('.help-inline').remove();
	}
}