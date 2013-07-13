$(document).ready(function() {
	$("input:radio:checked").data("chk", true);
	$("input:radio").click(function() {
		$("input[name='" + $(this).attr("name") + "']:radio").not(this).removeData("chk");
		$(this).data("chk", !$(this).data("chk"));
		$(this).prop("checked", $(this).data("chk"));
	});
	
	$('tbody td:not(.uneditable)').editable(function(value, settings) {
		sendAJAXReq(this, value);
	},
	{ 
		type: 'text',
		tooltip: 'Click to edit...',
		placeholder: '',
	});
	
	$('.regType').editable(function(value, settings) {
		sendAJAXReq(this, value);
	}, 
	{ 
		data: " {'student':'student','coach':'coach'}",
	    type: 'select',
	    submit: 'OK'
	});
});

function sendAJAXReq(elem, value)
{
	$.ajax({
		'url': '/editRegistration',
		'type': 'post',
		'data': {
			'key': $(elem).siblings().first().html(),
			'ajax': '1',
			'account': $(elem).siblings('.account').html(),
			'newValue': value,
			'modified': $(elem).data('type'),
			'test': $(elem).attr('class') === 'test'
		}
	}).done(function() {
		window.location = '/data?choice=registrations&updated=1';
	});
}