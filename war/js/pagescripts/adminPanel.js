$(document).ready(function() {
	$('.date').datepicker();
	$('.price').slider({
		formater: function(value) {
		  return '$' + value;
		}
    });
	$('.price').slider().on('slideStop', function(ev) {
		$('#priceText').html('$' + ev.value);
	});
	ChangeForm();
	$('.radio').change(ChangeForm);
});

function ChangeForm() {
	var change2 = $('#change2').is(':checked');
	$('#curPassword').prop('required', !change2);
	$('#newPassword').prop('required', !change2);
	$('#confPassword').prop('required', !change2);

	if (change2)
		$('#changePass').hide();
	else
		$('#changePass').show();

	var update2 = $('#update2').is(':checked');
	$('#docAccount').prop('required', !update2);
	$('#docPassword').prop('required', !update2);
	$('#docHigh').prop('required', !update2);
	$('#docMiddle').prop('required', !update2);

	if (update2)
		$('#updateScores').hide();
	else
		$('#updateScores').show();
}