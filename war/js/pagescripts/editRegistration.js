/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

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