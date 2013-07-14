$(document).ready(function() {
	EnableSubmit();
	CalcCost();
	CheckAccount();
	
	$('.span2').change(CalcCost);
	$('#captcha').on('keyup', AuthCaptcha);
	$('#submit').on('click', AuthCaptcha);
	$('#regType1,#regType2').change(function() {
		$('#aliases').toggle('slow');
	});
	
	$('#schoolType1,#schoolType2').change(function() {
		$('#mid').toggle();
		$('#hi').toggle();
		CalcCost();
	});
	
	$('#account').change(function() {
		$('#acc').toggle('slow');
		CheckAccount();
	});
});

function CheckAccount() {
	var account = $('#account').prop('checked');
	$('#password').prop('required', account);
	$('#confPassword').prop('required', account);
	if(account)
		$('#acc').show();
}

function AuthCaptcha() {
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

function EnableSubmit() {
	if ($('#regError').val() === '') {
		$('#submit').prop('disabled', false)
		$('#reset').prop('disabled', false)
	}
	$('#answerCaptcha').html('');
}

function CalcCost() {
	sum = 0;
	var i;
	var end;
	var price = parseInt($('#price').val());

	if ($('#schoolType1').is(':checked')) {
		i = 6;
		end = 8;
	} else {
		i = 9;
		end = 12;
	}

	for (; i <= end; i++) {
		var start = "[name=" + "'" + i;
		if (parseInt($(start + "n']").get(0).value) < 0)
			$(start + "n']").get(0).value = 0;
		if (parseInt($(start + "c']").get(0).value) < 0)
			$(start + "c']").get(0).value = 0;
		if (parseInt($(start + "s']").get(0).value) < 0)
			$(start + "s']").get(0).value = 0;
		if (parseInt($(start + "m']").get(0).value) < 0)
			$(start + "m']").get(0).value = 0;
		sum += price * parseInt($(start + "n']").get(0).value);
		sum += price * parseInt($(start + "c']").get(0).value);
		sum += price * parseInt($(start + "s']").get(0).value);
		sum += price * parseInt($(start + "m']").get(0).value);
	}
	if(isNaN(sum)) {
		$('#cost').closest('.control-group').addClass('error');
		if($('#cost').closest('.controls').find('.help-inline').length === 0)
			$('#cost').closest('.controls').append('<span class="help-inline">Please enter only <b>numeric</b> values above</span>');
	}
	else
		$('#cost').val(sum)
}