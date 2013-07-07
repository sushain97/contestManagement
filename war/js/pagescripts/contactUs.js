$(document).ready(function() {
	EnableSubmit();

	$('#captcha').on('keyup', AuthCaptcha);
	$('#submit').on('click', AuthCaptcha);
});

function AuthCaptcha() {
	if ($('#nocaptcha').val() === 'true') {
		$('#submit').prop('disabled', false)
		$('#answerCaptcha').hide()
	} else {
		if (calcMD5($('#salt').val() + $('#captcha').val()) === $('#hash').val()) {
			$('#submit').prop('disabled', false)
			$('#answerCaptcha').hide();
			$('#captcha').closest('.control-group').removeClass('error');
		} else {
			$('#captcha').closest('.control-group').addClass('error');
			$('#submit').prop('disabled', true)
			$('#answerCaptcha').show();
			$('#answerCaptcha').html('Answer the captcha correctly to prove that you are not a robot.');
		}
	}
}

function EnableSubmit() {
	$('#submit').prop('disabled', false)
	$('#answerCaptcha').html('');
}