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
	CheckPassChange();
	CheckUpdate();

	$('.input-daterange').datepicker();
	$('.glyphicon-question-sign').tooltip();

	$('.price').slider({
		formater: function(value) {
		return '$' + value;
		}
	});
	$('.price').slider().on('slide', function(ev) {
		$('#priceText').html('$' + ev.value);
	});

	$('#update').change(function() {
		$('#updateScores').toggle('fast');
		CheckUpdate();
	});

	$('#changePass').change(function() {
		$('#changePassword').toggle('fast');
		CheckPassChange();
	});

	$('#middleDocButton').click(function() {
		$('#middleDocButton').addClass('blurred');
		$.get('/createSpreadsheet', {
			'docMiddle': $('input[name=docMiddle]').val()
		}).done(function() {
			$('#middleDocButton').removeClass('blurred');
		});
	});

	$('#highDocButton').click(function() {
		$('#highDocButton').addClass('blurred');
		$.get('/createSpreadsheet', {
			'docHigh': $('input[name=docHigh]').val()
		}).done(function() {
			$('#middleDocButton').removeClass('blurred');
		});
	});
});

function CheckUpdate() {
	var update = $('#update').prop('checked');
	$('#docAccount').prop('required', update);
	$('#docPassword').prop('required', update);
	$('#docHigh').prop('required', update);
	$('#docMiddle').prop('required', update);
	if(update)
		$('#updateScores').show();
}

function CheckPassChange() {
	var change = $('#changePass').prop('checked');
	$('#curPassword').prop('required', change);
	$('#newPassword').prop('required', change);
	$('#confPassword').prop('required', change);
	$('#password').prop('required', change);
	$('#confPassword').prop('required', change);
	if(change)
		$('#changePassword').show();
}

function signInCallback(authResult) {
	if(authResult['code']) {
		$('#signinButton').hide();

		$.ajax({
			type: 'POST',
			url: '/authToken',
			data: authResult['code'],
			processData: false,
			contentType: 'application/octet-stream; charset=utf-8',
			success: function() {
				$('#oAuthResult').html('<strong class="text-success">You are signed in, submit the form to update online scores.</strong>');
				$('.docButton').show();
			}
		});
	}
	else if(authResult['error']) {
		console.log('There was an error: ' + authResult['error']);
		$('#oAuthResult').html('<strong class="text-danger">Failed to sign you in, try again.</strong>');
	}
}
