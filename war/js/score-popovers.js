$(document).ready(function() {
	$('.popovers').popover({
	}
	    html: true,
	    trigger: 'manual',
	    placement: 'right'
	}).click(function(e) {
	    $(this).popover('toggle');
	    e.stopPropagation();
	    e.preventDefault();
	});
	$('html').click(function(e) {
	    $('.popovers').popover('hide');
	});
});