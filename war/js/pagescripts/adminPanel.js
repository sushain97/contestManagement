$(document).ready(function() {
	CheckPassChange();
	CheckUpdate();
	
	$('.date').datepicker();
	
	$('.price').slider({
		formater: function(value) {
		  return '$' + value;
		}
    });
	$('.price').slider().on('slide', function(ev) {
		$('#priceText').html('$' + ev.value);
	});
	
	$('#update').change(function() {
		$('#updateScores').toggle('fast');
		CheckUpdate();
	});
	
	$('#changePass').change(function() {
		$('#changePassword').toggle('slow');
		CheckPassChange();
	});
});

function CheckUpdate() {
	var update = $('#update').prop('checked');
	$('#docAccount').prop('required', update);
	$('#docPassword').prop('required', update);
	$('#docHigh').prop('required', update);
	$('#docMiddle').prop('required', update);
	if(update)
		$('#updateScores').show();
}

function CheckPassChange() {
	var change = $('#changePass').prop('checked');
	$('#curPassword').prop('required', change);
	$('#newPassword').prop('required', change);
	$('#confPassword').prop('required', change);
	$('#password').prop('required', change);
	$('#confPassword').prop('required', change);
	if(change)
		$('#changePassword').show();
}