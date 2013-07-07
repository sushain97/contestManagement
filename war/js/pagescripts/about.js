$(document).ready(function() {
	$('h1 button').on('click', function() {
		$('.accordion-body').addClass('in');
		$('button').hide();
		window.print();
		$('.accordion-body').removeClass('in');
		$('button').show();
	});
});