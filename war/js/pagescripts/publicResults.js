$(document).ready(function() {
	ChangeActive()
	$('button').on('click', function() {
		$('button').hide();
		window.print();
		$('button').show();
	});
	$("table").tablesorter({sortList: [[0,0]]});
	
	$('td').hover(function() {
	    var t = parseInt($(this).index()) + 1;
	    $('td:nth-child(' + t + ')', $(this).closest('table')).addClass('highlighted');
	}, function() {
	    var t = parseInt($(this).index()) + 1;
	    $('td:nth-child(' + t + ')', $(this).closest('table')).removeClass('highlighted');
	});
});

function ChangeActive() {
	var type = $.url().param('type');
	if (type == undefined)
		$("#avail").addClass('active');
	else {
		$("#" + type).closest('.dropdown').addClass('active');
		$('#' + type).addClass('active');
	}
}