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
	ChangeActive();
	$('button#printButton').on('click', function() {
		window.print();
	});

	$.extend($.tablesorter.themes.bootstrap, {
		table: 'table'
	});

	$("table:not(#statusTable)").tablesorter({
		theme : 'bootstrap',
		headerTemplate : '{content} {icon}',
		widgets : ['uitheme'],
		sortList: [[0,0]]
	});

	$('.table-hover td').hover(function() {
		var t = parseInt($(this).index()) + 1;
		$('td:nth-child(' + t + ')', $(this).closest('table')).addClass('highlighted');
	}, function() {
		var t = parseInt($(this).index()) + 1;
		$('td:nth-child(' + t + ')', $(this).closest('table')).removeClass('highlighted');
	});

	$('.popovers').popover({
		html: true,
		trigger: 'manual',
		placement: 'right'
	}).click(function(e) {
		$(this).popover('toggle');
		e.stopPropagation();
	});
	$('html').click(function(e) {
		$('.popovers').popover('hide');
	});

	$('[data-toggle="offcanvas"]').click(function () {
		$('.row-offcanvas').toggleClass('active');
		$('.sidebar-offcanvas').toggleClass('hidden-xs');
	});

	if($('#testsGradedColumnGraph').length > 0) {
		$('#testsGradedColumnGraph').highcharts({
			chart: {
				type: 'column'
			},
			title: {
				text: 'Test Grading Progress'
			},
			xAxis: {
				categories: grades
			},
			yAxis: {
				min: 0,
				max: 100,
				title: {
					text: 'Percent Completion'
				},
				labels: {
					format: '{value}%'
				}
			},
			tooltip: {
				headerFormat: '<span style="font-size:10px">{point.key}</span><table>',
				pointFormat: '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
					'<td style="padding:0"><b>{point.y:.1f}%</b></td></tr>',
				footerFormat: '</table>',
				shared: true,
				useHTML: true
			},
			plotOptions: {
				column: {
					pointPadding: 0.2,
					borderWidth: 0
				}
			},
			series: testsGradedSeries
		});
	}

	if(!$('.hidden-xs').is(':hidden')) {
		var affixElements = $('[data-spy="affix"]');
		affixElements.width(affixElements.parent().width());
	}

	if (($(window).height() + 100) < $(document).height()) {
		$('a#backToTop').show().click(function() {
			$('html, body').animate({scrollTop: 0}, 'slow');
			$(this).blur();
			return false;
		});
	}
});

function ChangeActive() {
	var type = $('meta[name="type"]').prop('content');
	$('li[data-type="' + type + '"]').closest('.dropdown').addClass('active');
	$('li[data-type="' + type + '"]').addClass('active');

	if(type.indexOf("school_") !== -1) {
		var hash = hashCode(type.substring(type.indexOf('school_') + 7));
		$("#school_" + hash).closest('.dropdown').addClass('active');
		$('#school_' + hash).addClass('active');
	}

	if(type.indexOf("qualifying_") !== -1) {
		var hash = hashCode(type.substring(type.indexOf('qualifying_') + 11));
		$("#qualifying_" + hash).closest('.dropdown').addClass('active');
		$('#qualifying_' + hash).addClass('active');
	}
}

hashCode = function(s) {
	var hash = 0, i, c;
	if (s.length == 0) return hash;
	for (i = 0; i < s.length; i++) {
		c = s.charCodeAt(i);
		hash = ((hash << 5) - hash) + c;
		hash = hash & hash; // Convert to 32bit integer
	}
	return hash;
};
