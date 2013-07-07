$(document).ready(function() {
			ChangeActive()
			$('button').on('click', function() {
		$('button').hide();
		window.print();
		$('button').show();
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