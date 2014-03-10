$(document).ready(function() {
	$('.popovers').popover({
		html: true,
		trigger: 'click',
		placement: 'right'
	}).click(function(e) {
		e.stopPropagation();
		e.preventDefault();
	});
	$('html').click(function(e) {
		$('.popovers').popover('hide');
	});
});
