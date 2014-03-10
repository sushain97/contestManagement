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
	$('h1 button').on('click', function() {
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

	$('table#middleReg, table#highReg').tablesorter({
		theme : 'bootstrap',
		headerTemplate : '{content} {icon}',
		widgets : ['uitheme', 'filter', 'zebra'],
		filter_reset : 'button.reset',
		widgetOptions : {
			filter_hideFilters : true,
			filter_saveFilters : true,
			filter_reset : '.reset',
			zebra : ["even", "odd"],
			filter_functions : {
				4: {
					"Coach": function(e, n, f, i, $r) { return n === 'coach'; },
					"Student": function(e, n, f, i, $r) { return n === 'student'; }
				},
				6: {
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
			}
		},
		headers: {
			0: {filter: false},
			1: {sorter: 'shortDate'}
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

	$('tbody td:not(.uneditable)').editable(function(value, settings) {
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

	$('.division').editable(function(value, settings) {
		sendAJAXReq(this, value);
	}, {
		data: "{'1A':'1A','2A':'2A','3A':'3A','4A':'4A','5A':'5A'}",
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
});

function sendAJAXReq(elem, value)
{
	$.ajax({
		'url': '/editRegistration',
		'type': 'post',
		'data': {
			'key': $(elem).parent().data('eid'),
			'ajax': '1',
			'account': $(elem).siblings('.account').html(),
			'newValue': value,
			'modified': $(elem).data('type')
		}
	}).done(function() {
		window.location = '/data?choice=registrations&updated=1';
	});
}
