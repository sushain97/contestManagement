$(document).ready(function () {
	EnableSubmit();
	CalcCost();
	
	$('.span2').change(CalcCost);
	
	$('#schoolType1,#schoolType2').change(function() {
		$('#mid').toggle();
		$('#hi').toggle();
		CalcCost();
	});
	
	$('#delete').change(function() {
		$('#info').toggle('fast');
	});
});
		
function EnableSubmit() {
	$('#submit').prop('disabled', false)
	$('#reset').prop('disabled', false)
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
	
	for(; i <= end; i++)
	{
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

	$('#cost').val(sum)
}