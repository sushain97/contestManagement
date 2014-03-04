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
	
	var price = parseInt($('#price').val());

	$.each($('.registrationInfo'), function() {
		var studentData = JSON.parse($(this).text());
		var registrationRow = $(this).parent(), table = registrationRow.parents('table');
		$.each(studentData, function() {
			var student = this;
			var subjects = ['C', 'M', 'N', 'S'];
			$.each(subjects, function() {
				if(student[this]) {
					var test = (student["grade"] + this).toLowerCase();
					var cell = $('td[data-type=' + test + '] a', registrationRow);
					cell.text(parseInt(cell.text()) + 1);
					
					var tooltip = (cell.attr('data-original-title') ? cell.attr('data-original-title') + ", " : "") + student["name"];
					cell.attr('data-original-title', tooltip).popover('fixTitle');
					
					var totalCell = table.find('.total[data-type=' + test + ']');
					totalCell.text(parseInt(totalCell.text()) + 1);
					
					var totalCostCell = table.find('.totalCost');
					totalCostCell.text(parseInt(totalCostCell.text()) + price);
				}
			});
				
		});
	});
	
	$.extend($.tablesorter.themes.bootstrap, {
		table: 'table'
	});
	
	$('table#middleReg, table#highReg').tablesorter({
		theme : 'bootstrap',
		headerTemplate : '{content} {icon}',
	    widgets : ['uitheme'],
		headers: {
			0: {sorter: false},
			1: {sorter: 'shortDate'}
		}
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