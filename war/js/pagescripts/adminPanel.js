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
	$('.nav-pills').stickyTabs();

	CheckPassChange();

	$('.input-daterange').datepicker();
	$('.glyphicon-question-sign').tooltip();

	$('input[name=startDate], input[name=endDate], input[name=editStartDate], input[name=editEndDate]').change(function(ev) {
		$($(this).parents('.form-group')[0]).children('.help-block').fadeOut();
	});

	$('.price').slider({
		formater: function(value) {
		return '$' + value;
		}
	});
	$('.price').slider().on('slide', function(ev) {
		$('#priceText').html('$' + ev.value);
	});

	$('#changePass').change(function() {
		$('#changePassword').toggle('fast');
		CheckPassChange();
	});

	$('#elementaryDocButton').click(function() {
		$('#elementaryDocButton').addClass('blurred');
		$.get('/createSpreadsheet', {
			'docElementary': $('input[name=docElementary]').val()
		}).done(function() {
			$('#elementaryDocButton').removeClass('blurred');
		});
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
			$('#highDocButton').removeClass('blurred');
		});
	});

	$('button#enableDeleteScores').click(function() {
		$('#deleteScoresButton').prop('disabled', false);
		$('button#enableDeleteScores').prop('disabled', true);
	});

	$('#deleteScoresButton').click(function() {
		$('#deleteScoresButton').addClass('blurred');
		$.get('/clearScores').done(function() {
			$('#deleteScoresButton').removeClass('blurred');
		});
	});

	$('.tab-pane#tabulation button[type=submit]').click(function() {
		$('<input />').attr('type', 'hidden')
			.attr('name', 'submitType')
			.attr('value', $(this).attr('id'))
			.appendTo($(this).parents('form'));
	});

	$('.subjectColor').colorpicker();
});

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
				$('#oAuthResult').html('<strong class="text-success">You are signed in.</strong>');
				$('.docButton').show();
			}
		});
	}
	else if(authResult['error']) {
		console.log('There was an error: ' + authResult['error']);
		$('#oAuthResult').html('<strong class="text-danger">Failed to sign you in, try again.</strong>');
	}
}

/**
 * jQuery Plugin: Sticky Tabs
 *
 * @author Aidan Lister <aidan@php.net>
 * @version 1.0.0
 */

(function ( $ ) {
	$.fn.stickyTabs = function() {
		context = this

		// Show the tab corresponding with the hash in the URL, or the first tab.
		var showTabFromHash = function() {
			var hash = window.location.hash;
			var selector = hash ? 'a[href="' + hash + '"]' : 'li:first-child a';
			$(selector, context).tab('show');
		}

		// Set the correct tab when the page loads
		showTabFromHash(context)

		// Set the correct tab when a user uses their back/forward button
		window.addEventListener('hashchange', showTabFromHash, false);

		// Change the URL when tabs are clicked
		$('a', context).on('click', function(e) {
			history.pushState(null, null, this.href);
		});

		return this;
	};
}( jQuery ));
