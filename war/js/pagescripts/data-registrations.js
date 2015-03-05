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

	$('button#printButton').on('click', function() {
		window.print();
	});

	$(document).on('click', 'a', function(e) {
		if($(this).attr('href') === '#')
			e.preventDefault();
	});

	$('.popovers').popover();

	$.extend($.tablesorter.themes.bootstrap, {
		table: 'table'
	});

	var filter_functions;
	if(classificationQuestion === 'no')
		filter_functions = {
			4: {
				"Coach": function(e, n, f, i, $r) { return n === 'coach'; },
				"Student": function(e, n, f, i, $r) { return n === 'student'; }
			},
			6: {
				"Yes": function(e, n, f, i, $r) { return n === 'yes'; },
				"No": function(e, n, f, i, $r) { return n === 'no'; }
			}
		};
	else
		filter_functions = {
			4: {
				"Coach": function(e, n, f, i, $r) { return n === 'coach'; },
				"Student": function(e, n, f, i, $r) { return n === 'student'; }
			},
			6: {
				"6A": function(e, n, f, i, $r) { return n === '6a'; },
				"5A": function(e, n, f, i, $r) { return n === '5a'; },
				"4A": function(e, n, f, i, $r) { return n === '4a'; },
				"3A": function(e, n, f, i, $r) { return n === '3a'; },
				"2A": function(e, n, f, i, $r) { return n === '2a'; },
				"1A": function(e, n, f, i, $r) { return n === '1a'; }
			},
			7: {
				"Yes": function(e, n, f, i, $r) { return n === 'yes'; },
				"No": function(e, n, f, i, $r) { return n === 'no'; }
			}
		};

	$('table.regTable').tablesorter({
		theme : 'bootstrap',
		headerTemplate : '{content} {icon}',
		widgets : ['uitheme', 'filter', 'zebra'],
		filter_reset : 'button.reset',
		widgetOptions : {
			filter_hideFilters : true,
			filter_saveFilters : true,
			filter_reset : '.reset',
			zebra : ["even", "odd"],
			filter_functions : filter_functions
		},
		headers: {
			0: {sorter: 'shortDate'}
		}
	});

	var disabled = [0, 3];
	for(var i = 0; i < disabled.length; i++)
		$('.tablesorter-filter[data-column=' + disabled[i] + ']').prop('disabled', true).addClass('disabled');

	$('button.reset').click(function() {
		$($(this).parent().next('table')[0]).trigger('filterReset');
	});

	$('input:radio:checked').data('chk', true);
	$('input:radio').click(function() {
		$("input[name='" + $(this).attr('name') + "']:radio").not(this).removeData('chk');
		$(this).data('chk', !$(this).data('chk'));
		$(this).prop('checked', $(this).data('chk'));
	});

	$('table:not(.overviewTable) tbody td:not(.uneditable)').editable(function(value, settings) {
		sendAJAXReq(this, value);
	}, {
		type: 'text',
		tooltip: 'Click to edit...',
		placeholder: '',
	});

	$('.regType').editable(function(value, settings) {
		sendAJAXReq(this, value);
	}, {
		data: "{'student':'student','coach':'coach'}",
		type: 'select',
		submit: 'OK'
	});

	$('.classification').editable(function(value, settings) {
		sendAJAXReq(this, value);
	}, {
		data: "{'1A':'1A','2A':'2A','3A':'3A','4A':'4A','5A':'5A','6A':'6A'}",
		type: 'select',
		submit: 'OK'
	});

	$('td').hover(function() {
		var t = parseInt($(this).index()) + 1;
		$('td:nth-child(' + t + '):not(tfoot td)', $(this).closest('table')).addClass('highlighted');
	}, function() {
		var t = parseInt($(this).index()) + 1;
		$('td:nth-child(' + t + '):not(tfoot td)', $(this).closest('table')).removeClass('highlighted');
	});

	$('.tablesorter-filter-row td:odd').slice(4).hide();
	$('.testsTakenColspan').attr('colspan', 1);
	$('.testsTaken').hide();

	$('button#testsTaken').click(function() {
		var parent = $(this).parents('.tab-pane');
		if($(this).hasClass('active')) {
			$('.tablesorter-filter-row td:odd', parent).slice(4).hide();
			$('.testsTakenColspan', parent).attr('colspan', 1);
			$('.testsTaken', parent).hide();
			$(this).removeClass('active');
		}
		else {
			$('.tablesorter-filter-row td:odd', parent).slice(4).show();
			$('.testsTakenColspan', parent).attr('colspan', 2);
			$('.testsTaken', parent).show();
			$(this).addClass('active');
		}
	});

	if (($(window).height() + 100) < $(document).height()) {
		$('a#backToTop').show().click(function() {
			$('html, body').animate({scrollTop: 0}, 'slow');
			$(this).blur();
			return false;
		});
	}
});

function sendAJAXReq(elem, value) {
	$.ajax({
		'url': '/editRegistration',
		'type': 'post',
		'data': {
			'key': $(elem).parent().data('eid'),
			'ajax': '1',
			'account': $(elem).siblings('.account').text().trim(),
			'newValue': value,
			'modified': $(elem).data('type')
		}
	}).done(function() {
		window.location = '/data/registrations?updated=1';
	});
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
