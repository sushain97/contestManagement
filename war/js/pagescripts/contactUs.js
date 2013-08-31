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

	$('#captcha').on('keyup', AuthCaptcha);
	$('#submit').on('click', AuthCaptcha);
});

function AuthCaptcha() {
	if ($('#nocaptcha').val() === 'true') {
		$('#submit').prop('disabled', false)
		$('#answerCaptcha').hide()
	} else {
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
}

function EnableSubmit() {
	$('#submit').prop('disabled', false)
	$('#answerCaptcha').html('');
}