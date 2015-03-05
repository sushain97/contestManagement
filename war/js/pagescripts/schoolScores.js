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
	$('button#printButton').on('click', function() {
		window.print();
	});

	$.extend($.tablesorter.themes.bootstrap, {
		table: 'table'
	});

	if($('table tr').length > 1)
		$("table").tablesorter({
			theme: 'bootstrap',
			headerTemplate: '{content} {icon}',
			widgets: ['uitheme'],
			sortList: [[1,0]],
			headers: {
				0: {sorter: false}
			}
		});

	$('td').hover(function() {
		var t = parseInt($(this).index()) + 1;
		$('td:nth-child(' + t + '):not(tfoot td)', $(this).closest('table')).addClass('highlighted');
	}, function() {
		var t = parseInt($(this).index()) + 1;
		$('td:nth-child(' + t + '):not(tfoot td)', $(this).closest('table')).removeClass('highlighted');
	});

	if (($(window).height() + 100) < $(document).height()) {
		$('a#backToTop').show().click(function() {
			$('html, body').animate({scrollTop: 0}, 'slow');
			$(this).blur();
			return false;
		});
	}
});
