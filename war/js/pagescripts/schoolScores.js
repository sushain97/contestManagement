$(document).ready(function() {
	$('button').on('click', function() {
		$('button').hide();
		window.print();
		$('button').show();
	});
});