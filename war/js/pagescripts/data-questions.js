$(document).ready(function() {
	$('td').hover(function() {
	    var t = parseInt($(this).index()) + 1;
	    $('td:nth-child(' + t + '):not(tfoot td)', $(this).closest('table')).addClass('highlighted');
	}, function() {
	    var t = parseInt($(this).index()) + 1;
	    $('td:nth-child(' + t + '):not(tfoot td)', $(this).closest('table')).removeClass('highlighted');
	});
});