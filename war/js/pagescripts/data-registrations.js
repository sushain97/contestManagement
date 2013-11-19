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
	$("input:radio:checked").data("chk", true);
	$("input:radio").click(function() {
		$("input[name='" + $(this).attr("name") + "']:radio").not(this).removeData("chk");
		$(this).data("chk", !$(this).data("chk"));
		$(this).prop("checked", $(this).data("chk"));
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
		data: " {'student':'student','coach':'coach'}",
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
			'modified': $(elem).data('type'),
			'test': $(elem).hasClass('test')
		}
	}).done(function() {
		window.location = '/data?choice=registrations&updated=1';
	});
}